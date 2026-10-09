package com.ecoamazonas.eco_agua.platform.control.demo;

import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

@Service
public class Matrix26DemoRuntimeService {

    private static final int CONNECT_TIMEOUT_MS = 700;
    private static final int READ_TIMEOUT_MS = 900;
    private static final int STOP_TIMEOUT_SECONDS = 20;
    private static final int LOG_TAIL_LINES = 180;

    public Matrix26DemoRuntimeStatus status(Matrix26DemoPortalDefinition definition) {
        if (definition == null || !definition.runtimeManaged()) {
            return Matrix26DemoRuntimeStatus.notApplicable();
        }

        RuntimePaths paths = paths(definition);
        boolean configured = Files.isRegularFile(paths.configFile());
        Integer configuredPort = configuredPort(definition, paths.configFile());
        boolean portListening = configuredPort != null && isPortListening(configuredPort);
        boolean httpReachable = isHttpReachable(definition.localUrl());
        Optional<ProcessHandle> trackedProcess = trackedProcess(paths.pidFile());
        boolean tracked = trackedProcess.isPresent();
        boolean trackedAlive = trackedProcess.map(ProcessHandle::isAlive).orElse(false);

        boolean canStart = configured && !portListening && !trackedAlive;
        boolean canStop = configured && trackedAlive;
        boolean canRestart = configured && (trackedAlive || !portListening);

        String statusLabel;
        String badgeClass;
        String detail;

        if (!configured) {
            statusLabel = "Config faltante";
            badgeClass = "text-bg-secondary";
            detail = "No se encontró runtime-clients/" + safe(definition.runtimeProfile()) + "/application.properties.";
        } else if (httpReachable) {
            statusLabel = "Activo";
            badgeClass = "text-bg-success";
            detail = trackedAlive
                    ? "El portal responde y fue iniciado desde Demo Center. PID: " + trackedProcess.get().pid() + "."
                    : "El portal responde en el puerto " + configuredPort + ", pero no fue iniciado desde Demo Center en esta sesión.";
        } else if (portListening) {
            statusLabel = "Puerto activo";
            badgeClass = "text-bg-warning";
            detail = "El puerto " + configuredPort + " está ocupado, pero la URL principal no respondió correctamente.";
        } else if (trackedAlive) {
            statusLabel = "Iniciando";
            badgeClass = "text-bg-info";
            detail = "Existe un proceso iniciado desde Demo Center, pero la URL todavía no responde. Revisa el log.";
        } else {
            statusLabel = "Apagado";
            badgeClass = "text-bg-secondary";
            detail = "Runtime listo para iniciar desde este panel. Puerto esperado: " + configuredPort + ".";
        }

        String pidText = trackedProcess.map(process -> Long.toString(process.pid())).orElse("");
        return new Matrix26DemoRuntimeStatus(
                true,
                configured,
                portListening,
                httpReachable,
                tracked,
                trackedAlive,
                canStart,
                canStop,
                canRestart,
                statusLabel,
                badgeClass,
                detail,
                projectRoot().relativize(paths.logFile()).toString().replace('\\', '/'),
                pidText
        );
    }

    public Matrix26DemoRuntimeActionResult start(Matrix26DemoPortalDefinition definition, String actor) {
        RuntimePaths paths = paths(definition);
        if (!definition.runtimeManaged()) {
            return Matrix26DemoRuntimeActionResult.failure("Este portal no usa runtime separado.");
        }
        if (!Files.isRegularFile(paths.configFile())) {
            return Matrix26DemoRuntimeActionResult.failure("No se encontró la configuración del runtime: " + paths.configFile());
        }
        Integer port = configuredPort(definition, paths.configFile());
        if (port != null && isPortListening(port)) {
            return Matrix26DemoRuntimeActionResult.failure("El puerto " + port + " ya está ocupado. Actualiza el estado o abre el portal si ya está activo.");
        }
        Optional<ProcessHandle> running = trackedProcess(paths.pidFile());
        if (running.isPresent() && running.get().isAlive()) {
            return Matrix26DemoRuntimeActionResult.failure("Este runtime ya tiene un proceso administrado activo. PID: " + running.get().pid());
        }

        try {
            Files.createDirectories(paths.logFile().getParent());
            Files.createDirectories(paths.pidFile().getParent());
            appendLog(paths.logFile(), "\n===== START requested by " + safe(actor) + " at " + LocalDateTime.now() + " =====\n");

            List<String> command = startCommand(paths.configFile(), port);
            ProcessBuilder builder = new ProcessBuilder(command);
            builder.directory(projectRoot().toFile());
            builder.redirectErrorStream(true);
            builder.redirectOutput(ProcessBuilder.Redirect.appendTo(paths.logFile().toFile()));
            Process process = builder.start();
            Files.writeString(
                    paths.pidFile(),
                    Long.toString(process.pid()),
                    StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.TRUNCATE_EXISTING
            );
            return Matrix26DemoRuntimeActionResult.success("Runtime iniciado. PID: " + process.pid() + ". Espera unos segundos y actualiza el estado.");
        } catch (IOException ex) {
            return Matrix26DemoRuntimeActionResult.failure("No se pudo iniciar el runtime: " + ex.getMessage());
        }
    }

    public Matrix26DemoRuntimeActionResult stop(Matrix26DemoPortalDefinition definition, String actor) {
        RuntimePaths paths = paths(definition);
        Optional<ProcessHandle> process = trackedProcess(paths.pidFile());
        if (process.isEmpty() || !process.get().isAlive()) {
            Integer port = configuredPort(definition, paths.configFile());
            if (port != null && isPortListening(port)) {
                return Matrix26DemoRuntimeActionResult.failure("El puerto " + port + " está activo, pero no hay PID administrado por Demo Center. No se detendrá para evitar cerrar un proceso ajeno.");
            }
            deleteQuietly(paths.pidFile());
            return Matrix26DemoRuntimeActionResult.success("El runtime ya estaba apagado.");
        }

        try {
            appendLog(paths.logFile(), "\n===== STOP requested by " + safe(actor) + " at " + LocalDateTime.now() + " =====\n");
            ProcessHandle handle = process.get();
            long pid = handle.pid();
            stopProcessTree(handle);
            deleteQuietly(paths.pidFile());
            return Matrix26DemoRuntimeActionResult.success("Runtime detenido. PID anterior: " + pid + ".");
        } catch (Exception ex) {
            return Matrix26DemoRuntimeActionResult.failure("No se pudo detener el runtime: " + ex.getMessage());
        }
    }

    public Matrix26DemoRuntimeActionResult restart(Matrix26DemoPortalDefinition definition, String actor) {
        Matrix26DemoRuntimeActionResult stopResult = stop(definition, actor);
        if (!stopResult.success()) {
            return stopResult;
        }
        return start(definition, actor);
    }

    public Matrix26DemoRuntimeLogView log(Matrix26DemoPortalView portal) {
        Matrix26DemoPortalDefinition definition = portal.definition();
        RuntimePaths paths = paths(definition);
        if (!Files.isRegularFile(paths.logFile())) {
            return new Matrix26DemoRuntimeLogView(
                    portal,
                    projectRoot().relativize(paths.logFile()).toString().replace('\\', '/'),
                    false,
                    "Todavía no existe log para este runtime. Inicia el portal para generarlo.",
                    List.of()
            );
        }
        try {
            return new Matrix26DemoRuntimeLogView(
                    portal,
                    projectRoot().relativize(paths.logFile()).toString().replace('\\', '/'),
                    true,
                    "Últimas " + LOG_TAIL_LINES + " líneas del log local del Demo Center.",
                    tail(paths.logFile(), LOG_TAIL_LINES)
            );
        } catch (IOException ex) {
            return new Matrix26DemoRuntimeLogView(
                    portal,
                    projectRoot().relativize(paths.logFile()).toString().replace('\\', '/'),
                    false,
                    "No se pudo leer el log: " + ex.getMessage(),
                    List.of()
            );
        }
    }


    private void stopProcessTree(ProcessHandle handle) throws Exception {
        if (isWindows()) {
            Process taskkill = new ProcessBuilder(
                    "cmd.exe",
                    "/c",
                    "taskkill",
                    "/PID",
                    Long.toString(handle.pid()),
                    "/T",
                    "/F"
            ).start();
            taskkill.waitFor(STOP_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            return;
        }

        handle.descendants().forEach(ProcessHandle::destroy);
        handle.destroy();
        handle.onExit().get(STOP_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        if (handle.isAlive()) {
            handle.descendants().forEach(ProcessHandle::destroyForcibly);
            handle.destroyForcibly();
            handle.onExit().get(8, TimeUnit.SECONDS);
        }
    }

    private List<String> startCommand(Path configFile, Integer port) {
        String configUri = configFile.toUri().toString();
        StringBuilder args = new StringBuilder("--spring.config.additional-location=").append(configUri);
        if (port != null) {
            args.append(" --server.port=").append(port);
        }

        if (isWindows()) {
            return List.of("cmd.exe", "/c", "mvn", "spring-boot:run", "-Dspring-boot.run.arguments=" + args);
        }
        return List.of("mvn", "spring-boot:run", "-Dspring-boot.run.arguments=" + args);
    }

    private Integer configuredPort(Matrix26DemoPortalDefinition definition, Path configFile) {
        if (definition.port() != null) {
            return definition.port();
        }
        if (!Files.isRegularFile(configFile)) {
            return null;
        }
        Properties properties = new Properties();
        try (var input = Files.newInputStream(configFile)) {
            properties.load(input);
            String value = properties.getProperty("server.port");
            if (value == null || value.isBlank()) {
                return null;
            }
            return Integer.parseInt(value.trim());
        } catch (IOException | NumberFormatException ex) {
            return null;
        }
    }

    private boolean isPortListening(Integer port) {
        if (port == null || port <= 0) {
            return false;
        }
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress("127.0.0.1", port), CONNECT_TIMEOUT_MS);
            return true;
        } catch (IOException ex) {
            return false;
        }
    }

    private boolean isHttpReachable(String urlValue) {
        if (urlValue == null || urlValue.isBlank() || !urlValue.startsWith("http")) {
            return false;
        }
        try {
            URL url = URI.create(urlValue).toURL();
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setConnectTimeout(CONNECT_TIMEOUT_MS);
            connection.setReadTimeout(READ_TIMEOUT_MS);
            connection.setInstanceFollowRedirects(false);
            connection.setRequestMethod("GET");
            int code = connection.getResponseCode();
            return code >= 200 && code < 500;
        } catch (RuntimeException | IOException ex) {
            return false;
        }
    }

    private Optional<ProcessHandle> trackedProcess(Path pidFile) {
        if (!Files.isRegularFile(pidFile)) {
            return Optional.empty();
        }
        try {
            String text = Files.readString(pidFile, StandardCharsets.UTF_8).trim();
            if (text.isBlank()) {
                return Optional.empty();
            }
            long pid = Long.parseLong(text);
            return ProcessHandle.of(pid).filter(ProcessHandle::isAlive);
        } catch (IOException | NumberFormatException ex) {
            return Optional.empty();
        }
    }

    private List<String> tail(Path file, int maxLines) throws IOException {
        Deque<String> buffer = new ArrayDeque<>(maxLines);
        try (var lines = Files.lines(file, StandardCharsets.UTF_8)) {
            lines.forEach(line -> {
                if (buffer.size() == maxLines) {
                    buffer.removeFirst();
                }
                buffer.addLast(sanitize(line));
            });
        }
        return new ArrayList<>(buffer);
    }

    private void appendLog(Path logFile, String text) throws IOException {
        Files.createDirectories(logFile.getParent());
        Files.writeString(logFile, text, StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
    }

    private void deleteQuietly(Path path) {
        try {
            Files.deleteIfExists(path);
        } catch (IOException ignored) {
            // Best effort cleanup.
        }
    }

    private RuntimePaths paths(Matrix26DemoPortalDefinition definition) {
        String profile = safe(definition.runtimeProfile());
        Path root = projectRoot();
        Path runtimeDir = root.resolve("runtime-clients").resolve(profile).normalize();
        Path dataDir = root.resolve("runtime-data").resolve("demo-center").normalize();
        Path logDir = root.resolve("logs").normalize();
        Integer port = definition.port() == null ? 0 : definition.port();
        return new RuntimePaths(
                runtimeDir,
                runtimeDir.resolve("application.properties").normalize(),
                dataDir.resolve(profile + ".pid").normalize(),
                logDir.resolve("demo-center-" + profile + "-" + port + ".log").normalize()
        );
    }

    private Path projectRoot() {
        return Path.of("").toAbsolutePath().normalize();
    }

    private String sanitize(String line) {
        if (line == null) {
            return "";
        }
        return line.replaceAll("(?i)(password|token|secret|key)=([^\\s]+)", "$1=***");
    }

    private String safe(String value) {
        return value == null ? "" : value.trim();
    }

    private boolean isWindows() {
        return System.getProperty("os.name", "").toLowerCase(Locale.ROOT).contains("win");
    }

    private record RuntimePaths(
            Path runtimeDir,
            Path configFile,
            Path pidFile,
            Path logFile
    ) {
    }
}
