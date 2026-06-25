package com.ecoamazonas.eco_agua.platform.control.operations;

import com.ecoamazonas.eco_agua.platform.PlatformBusinessClientRepository;
import com.ecoamazonas.eco_agua.platform.control.Matrix26ControlCenterProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@ConditionalOnProperty(name = "matrix26.control-center.enabled", havingValue = "true")
public class Matrix26OperationsInventoryService {

    private static final Pattern DATABASE_NAME = Pattern.compile(
            "(?i)^jdbc:mysql://[^/]+/([^?;]+)"
    );
    private static final Set<String> LOG_EXTENSIONS = Set.of(".log", ".out", ".err");
    private static final int MAX_LOG_FILES = 500;

    private final PlatformBusinessClientRepository clientRepository;
    private final Matrix26ControlCenterProperties properties;
    private final Matrix26OperationsProperties operationsProperties;
    private final Matrix26SystemProbe systemProbe;
    private final AtomicReference<CachedSnapshot> cache = new AtomicReference<>();

    public Matrix26OperationsInventoryService(
            PlatformBusinessClientRepository clientRepository,
            Matrix26ControlCenterProperties properties,
            Matrix26OperationsProperties operationsProperties,
            Matrix26SystemProbe systemProbe
    ) {
        this.clientRepository = clientRepository;
        this.properties = properties;
        this.operationsProperties = operationsProperties;
        this.systemProbe = systemProbe;
    }

    public void invalidateCache() {
        cache.set(null);
    }

    public Matrix26OperationsSnapshot snapshot(boolean forceRefresh) {
        CachedSnapshot cached = cache.get();
        long cacheSeconds = Math.max(2L, operationsProperties.getCacheSeconds());
        if (!forceRefresh && cached != null && cached.createdAt().isAfter(Instant.now().minusSeconds(cacheSeconds))) {
            return cached.snapshot();
        }

        Matrix26OperationsSnapshot refreshed = inspect();
        cache.set(new CachedSnapshot(refreshed, Instant.now()));
        return refreshed;
    }

    public Matrix26RuntimeInventoryItem runtime(String runtimeKey, boolean forceRefresh) {
        return snapshot(forceRefresh).runtimes().stream()
                .filter(item -> item.target().key().equals(runtimeKey))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("The requested runtime does not exist."));
    }

    public Matrix26LogTailView logTail(String runtimeKey, boolean forceRefresh) {
        Matrix26RuntimeInventoryItem runtime = runtime(runtimeKey, forceRefresh);
        Matrix26LogInventoryItem log = runtime.primaryLog();
        if (log == null) {
            return new Matrix26LogTailView(null, List.of(), false, "No log file is associated with this runtime.");
        }

        Optional<Path> path = resolveSafeRelativePath(log.relativePath());
        if (path.isEmpty() || !Files.isRegularFile(path.get())) {
            return new Matrix26LogTailView(log, List.of(), false, "The log file is no longer available.");
        }

        int maximumLines = Math.max(20, Math.min(operationsProperties.getLogTailLines(), 500));
        try {
            List<String> lines = readLastLines(path.get(), maximumLines).stream()
                    .map(Matrix26OperationsSanitizer::sanitize)
                    .toList();
            return new Matrix26LogTailView(log, lines, true, "Last " + lines.size() + " sanitized lines.");
        } catch (IOException ex) {
            return new Matrix26LogTailView(log, List.of(), false, "The log could not be read: " + safeMessage(ex));
        }
    }

    private Matrix26OperationsSnapshot inspect() {
        Path projectRoot = Path.of("").toAbsolutePath().normalize();
        Path runtimeRoot = safeChild(projectRoot, operationsProperties.getRuntimeDirectory());
        Path dataRoot = safeChild(projectRoot, operationsProperties.getDataDirectory());
        Path logRoot = safeChild(projectRoot, operationsProperties.getLogDirectory());

        List<Matrix26RuntimeTarget> targets = new ArrayList<>();
        targets.add(Matrix26RuntimeTarget.controlCenter(properties));
        clientRepository.findAllByOrderByBusinessNameAsc().stream()
                .map(Matrix26RuntimeTarget::fromInstance)
                .forEach(targets::add);

        List<Integer> expectedPorts = targets.stream()
                .map(Matrix26RuntimeTarget::expectedPort)
                .filter(value -> value != null)
                .distinct()
                .toList();
        Matrix26SystemSnapshot system = systemProbe.capture(expectedPorts);

        Map<String, List<Path>> logsByRuntime = discoverLogs(projectRoot, logRoot, runtimeRoot, dataRoot, targets);
        List<Matrix26RuntimeInventoryItem> runtimes = targets.stream()
                .map(target -> inspectRuntime(
                        target,
                        projectRoot,
                        runtimeRoot,
                        dataRoot,
                        logsByRuntime.getOrDefault(target.key(), List.of()),
                        system
                ))
                .sorted(Comparator
                        .comparing((Matrix26RuntimeInventoryItem item) -> !item.target().controlCenter())
                        .thenComparing(item -> item.target().expectedPort(), Comparator.nullsLast(Integer::compareTo)))
                .toList();

        List<Matrix26LogInventoryItem> logs = runtimes.stream()
                .flatMap(runtime -> toLogItems(runtime, logsByRuntime.getOrDefault(runtime.target().key(), List.of()), projectRoot).stream())
                .sorted(Comparator.comparing(Matrix26LogInventoryItem::modifiedAt, Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();

        List<Matrix26PortBinding> ports = system.listeningPorts().values().stream()
                .filter(binding -> expectedPorts.contains(binding.port()))
                .sorted(Comparator.comparingInt(Matrix26PortBinding::port))
                .toList();

        long online = runtimes.stream().filter(Matrix26RuntimeInventoryItem::online).count();
        long degraded = runtimes.stream()
                .filter(item -> item.state() == Matrix26RuntimeState.DEGRADED
                        || item.state() == Matrix26RuntimeState.PORT_OCCUPIED
                        || item.state() == Matrix26RuntimeState.CONFIGURATION_MISSING
                        || item.state() == Matrix26RuntimeState.RUNTIME_MISSING)
                .count();
        long offline = runtimes.size() - online - degraded;
        long protectedRuntimes = runtimes.stream().filter(item -> item.target().protectedInstance()).count();
        long detectedProcesses = runtimes.stream().filter(item -> item.processId() != null).count();
        long totalStorage = runtimes.stream()
                .mapToLong(item -> item.runtimeStorageBytes() + item.assetStorageBytes())
                .sum();
        LocalDateTime inspectedAt = LocalDateTime.now();

        Matrix26OperationsSummary summary = new Matrix26OperationsSummary(
                runtimes.size(),
                online,
                Math.max(0, offline),
                degraded,
                protectedRuntimes,
                ports.size(),
                detectedProcesses,
                totalStorage,
                formatBytes(totalStorage),
                inspectedAt
        );

        return new Matrix26OperationsSnapshot(
                List.copyOf(runtimes),
                List.copyOf(ports),
                List.copyOf(logs),
                summary,
                system.warnings(),
                inspectedAt
        );
    }

    private Matrix26RuntimeInventoryItem inspectRuntime(
            Matrix26RuntimeTarget target,
            Path projectRoot,
            Path runtimeRoot,
            Path dataRoot,
            List<Path> logCandidates,
            Matrix26SystemSnapshot system
    ) {
        List<String> warnings = new ArrayList<>();
        Path runtimeDirectory = resolveRuntimeDirectory(runtimeRoot, target, warnings);
        boolean runtimeDirectoryPresent = runtimeDirectory != null && Files.isDirectory(runtimeDirectory);
        Path configuration = runtimeDirectoryPresent ? runtimeDirectory.resolve("application.properties").normalize() : null;
        boolean configurationPresent = configuration != null && Files.isRegularFile(configuration);
        RuntimeConfiguration runtimeConfiguration = readConfiguration(configuration, warnings);
        Path launcher = findLauncher(runtimeDirectory);
        boolean launcherPresent = launcher != null && Files.isRegularFile(launcher);

        Integer configuredPort = runtimeConfiguration.port();
        String configuredProfile = runtimeConfiguration.runtimeProfile();
        String configuredDatabase = runtimeConfiguration.databaseName();
        boolean configurationConsistent = configurationPresent && configurationMatches(
                target,
                configuredPort,
                configuredProfile,
                configuredDatabase,
                warnings
        );

        Matrix26PortBinding portBinding = target.expectedPort() == null
                ? null
                : system.listeningPorts().get(target.expectedPort());
        boolean portListening = portBinding != null;
        Matrix26ProcessInfo process = resolveProcess(target, configuration, portBinding, system.processes());
        boolean expectedProcess = process != null && matchesTarget(process, target, configuration);
        if (portBinding != null && portBinding.pid() != null && process != null && process.pid() == portBinding.pid()) {
            expectedProcess = matchesTarget(process, target, configuration) || target.controlCenter();
        }

        Matrix26HttpProbe http = probeHttp(target.publicUrl());
        Matrix26LogInventoryItem primaryLog = logCandidates.isEmpty()
                ? null
                : toLogItem(target, logCandidates.get(0), projectRoot, true);

        long runtimeStorage = directorySize(runtimeDirectory);
        Path assetsDirectory = resolveAssetsDirectory(dataRoot, target);
        long assetStorage = directorySize(assetsDirectory);

        Matrix26RuntimeState state = determineState(
                target,
                runtimeDirectoryPresent,
                configurationPresent,
                portListening,
                portBinding,
                expectedProcess,
                process,
                http,
                warnings
        );
        String stateDetail = stateDetail(state, portBinding, http, process);

        if (primaryLog == null) {
            warnings.add("No log file is associated with the runtime.");
        }
        if (!launcherPresent) {
            warnings.add("No compatible launcher was found in the runtime directory.");
        }

        LocalDateTime processStartedAt = process == null
                ? null
                : Matrix26SystemProbe.toLocalDateTime(process.startedAt());
        String uptimeLabel = process == null || process.startedAt() == null
                ? "—"
                : formatDuration(Duration.between(process.startedAt(), Instant.now()));

        return new Matrix26RuntimeInventoryItem(
                target,
                state,
                stateDetail,
                runtimeDirectoryPresent,
                relative(projectRoot, runtimeDirectory),
                configurationPresent,
                relative(projectRoot, configuration),
                launcherPresent,
                relative(projectRoot, launcher),
                configurationConsistent,
                configuredPort,
                configuredProfile,
                configuredDatabase,
                portListening,
                portBinding == null ? null : portBinding.pid(),
                portBinding == null ? "" : portBinding.processName(),
                expectedProcess,
                process == null ? null : process.pid(),
                process == null ? "" : process.executable(),
                process == null ? "" : process.commandLine(),
                processStartedAt,
                uptimeLabel,
                http,
                primaryLog,
                runtimeStorage,
                formatBytes(runtimeStorage),
                assetStorage,
                formatBytes(assetStorage),
                List.copyOf(new LinkedHashSet<>(warnings)),
                LocalDateTime.now()
        );
    }

    private Matrix26RuntimeState determineState(
            Matrix26RuntimeTarget target,
            boolean runtimeDirectoryPresent,
            boolean configurationPresent,
            boolean portListening,
            Matrix26PortBinding portBinding,
            boolean expectedProcess,
            Matrix26ProcessInfo process,
            Matrix26HttpProbe http,
            List<String> warnings
    ) {
        if (http.online()) {
            if (portBinding != null && portBinding.pid() != null && !expectedProcess && !target.controlCenter()) {
                warnings.add("The portal responded, but the PID could not be correlated with the runtime profile with certainty.");
            }
            return Matrix26RuntimeState.ONLINE;
        }
        if (portListening && portBinding != null && portBinding.pid() != null && !expectedProcess && !target.controlCenter()) {
            warnings.add("The port is owned by a process that does not match the expected runtime.");
            return Matrix26RuntimeState.PORT_OCCUPIED;
        }
        if (portListening || process != null) {
            return Matrix26RuntimeState.DEGRADED;
        }
        if (!runtimeDirectoryPresent && !isExternallyManaged(target)) {
            return Matrix26RuntimeState.RUNTIME_MISSING;
        }
        if (runtimeDirectoryPresent && !configurationPresent) {
            return Matrix26RuntimeState.CONFIGURATION_MISSING;
        }
        return Matrix26RuntimeState.OFFLINE;
    }

    private String stateDetail(
            Matrix26RuntimeState state,
            Matrix26PortBinding portBinding,
            Matrix26HttpProbe http,
            Matrix26ProcessInfo process
    ) {
        return switch (state) {
            case ONLINE -> http.statusCode() == null
                    ? "The portal responded successfully."
                    : "The portal responded with HTTP " + http.statusCode() + ".";
            case PORT_OCCUPIED -> portBinding != null && portBinding.pid() != null
                    ? "The port belongs to PID " + portBinding.pid() + " and does not match the expected runtime."
                    : "The port is occupied by an unidentified process.";
            case DEGRADED -> process != null
                    ? "The process exists, but the HTTP probe was not successful."
                    : "The port is listening, but the portal did not respond successfully.";
            case RUNTIME_MISSING -> "No managed runtime directory exists for this instance.";
            case CONFIGURATION_MISSING -> "The runtime directory exists, but application.properties is missing.";
            case OFFLINE -> "No process, listening port, or HTTP response was detected.";
            default -> state.getLabel();
        };
    }

    private Matrix26ProcessInfo resolveProcess(
            Matrix26RuntimeTarget target,
            Path configuration,
            Matrix26PortBinding binding,
            Map<Long, Matrix26ProcessInfo> processes
    ) {
        if (binding != null && binding.pid() != null) {
            Matrix26ProcessInfo owner = processes.get(binding.pid());
            if (owner != null) {
                return owner;
            }
        }

        return processes.values().stream()
                .filter(Matrix26ProcessInfo::alive)
                .map(process -> Map.entry(process, processScore(process, target, configuration)))
                .filter(entry -> entry.getValue() > 0)
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(null);
    }

    private int processScore(Matrix26ProcessInfo process, Matrix26RuntimeTarget target, Path configuration) {
        String commandLine = (process.commandLine() + " " + process.executable()).toLowerCase(Locale.ROOT);
        int score = 0;
        String profile = lower(target.runtimeProfile());
        if (!profile.isBlank() && commandLine.contains(profile)) {
            score += 12;
        }
        if (configuration != null) {
            String fileName = configuration.toString().replace('\\', '/').toLowerCase(Locale.ROOT);
            String shortPath = "runtime-clients/" + profile + "/application.properties";
            if (commandLine.replace('\\', '/').contains(fileName) || commandLine.replace('\\', '/').contains(shortPath)) {
                score += 15;
            }
        }
        if (target.expectedPort() != null
                && (commandLine.contains("server.port=" + target.expectedPort())
                || commandLine.contains("--server.port=" + target.expectedPort()))) {
            score += 8;
        }
        if (target.controlCenter() && commandLine.contains("matrix26_control")) {
            score += 20;
        }
        return score;
    }

    private boolean matchesTarget(Matrix26ProcessInfo process, Matrix26RuntimeTarget target, Path configuration) {
        return processScore(process, target, configuration) >= 8;
    }

    private Matrix26HttpProbe probeHttp(String value) {
        if (value == null || value.isBlank()) {
            return new Matrix26HttpProbe(false, false, null, null, "No URL is registered.");
        }
        URI uri;
        try {
            uri = URI.create(value.trim());
        } catch (IllegalArgumentException ex) {
            return new Matrix26HttpProbe(false, false, null, null, "The registered URL is invalid.");
        }
        if (!isLocalHost(uri.getHost())) {
            return new Matrix26HttpProbe(false, false, null, null, "The URL is not local and was not probed by Runtime Inventory.");
        }

        long startedAt = System.nanoTime();
        Integer status = null;
        try {
            URL url = uri.toURL();
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setConnectTimeout(Math.max(250, operationsProperties.getConnectTimeoutMs()));
            connection.setReadTimeout(Math.max(250, operationsProperties.getReadTimeoutMs()));
            connection.setInstanceFollowRedirects(false);
            connection.setRequestMethod("GET");
            connection.setRequestProperty("User-Agent", "Matrix26-Operations-Inventory/3D.1");
            status = connection.getResponseCode();
            connection.disconnect();
            long responseTime = Math.max(0, (System.nanoTime() - startedAt) / 1_000_000L);
            boolean online = status >= 200 && status < 500;
            return new Matrix26HttpProbe(
                    true,
                    online,
                    status,
                    responseTime,
                    online ? "The local portal responded successfully." : "The local portal returned HTTP " + status + "."
            );
        } catch (IOException ex) {
            long responseTime = Math.max(0, (System.nanoTime() - startedAt) / 1_000_000L);
            return new Matrix26HttpProbe(true, false, status, responseTime, safeMessage(ex));
        }
    }

    private boolean configurationMatches(
            Matrix26RuntimeTarget target,
            Integer configuredPort,
            String configuredProfile,
            String configuredDatabase,
            List<String> warnings
    ) {
        boolean consistent = true;
        if (configuredPort != null && target.expectedPort() != null && !configuredPort.equals(target.expectedPort())) {
            warnings.add("The configured port (" + configuredPort + ") does not match the registered port (" + target.expectedPort() + ").");
            consistent = false;
        }
        if (!isBlank(configuredProfile) && !isBlank(target.runtimeProfile())
                && !configuredProfile.equalsIgnoreCase(target.runtimeProfile())) {
            warnings.add("The configured runtime profile does not match the registered profile.");
            consistent = false;
        }
        if (!isBlank(configuredDatabase) && !isBlank(target.databaseName())
                && !configuredDatabase.equalsIgnoreCase(target.databaseName())) {
            warnings.add("The configured database does not match the declared database.");
            consistent = false;
        }
        return consistent;
    }

    private RuntimeConfiguration readConfiguration(Path configuration, List<String> warnings) {
        if (configuration == null || !Files.isRegularFile(configuration)) {
            return RuntimeConfiguration.empty();
        }
        Properties loaded = new Properties();
        try (BufferedReader reader = Files.newBufferedReader(configuration, StandardCharsets.UTF_8)) {
            loaded.load(reader);
        } catch (IOException ex) {
            warnings.add("application.properties could not be read: " + safeMessage(ex));
            return RuntimeConfiguration.empty();
        }

        Integer port = parseInteger(loaded.getProperty("server.port"));
        String profile = firstNonBlank(
                loaded.getProperty("ecoagua.platform.runtime-profile"),
                loaded.getProperty("matrix26.control-center.runtime-profile"),
                loaded.getProperty("spring.profiles.active")
        );
        String database = databaseName(loaded.getProperty("spring.datasource.url"));
        return new RuntimeConfiguration(port, profile, database);
    }

    private Map<String, List<Path>> discoverLogs(
            Path projectRoot,
            Path logRoot,
            Path runtimeRoot,
            Path dataRoot,
            List<Matrix26RuntimeTarget> targets
    ) {
        Set<Path> files = new LinkedHashSet<>();
        collectLogFiles(logRoot, 3, files);
        collectLogFiles(runtimeRoot, 4, files);
        collectLogFiles(dataRoot, 5, files);

        Map<String, List<Path>> result = new LinkedHashMap<>();
        for (Matrix26RuntimeTarget target : targets) {
            List<Path> matches = files.stream()
                    .map(path -> Map.entry(path, logScore(path, target)))
                    .filter(entry -> entry.getValue() > 0)
                    .sorted(Comparator
                            .<Map.Entry<Path, Integer>>comparingInt(Map.Entry::getValue)
                            .reversed()
                            .thenComparing(entry -> lastModified(entry.getKey()), Comparator.nullsLast(Comparator.reverseOrder())))
                    .map(Map.Entry::getKey)
                    .toList();
            result.put(target.key(), matches);
        }
        return result;
    }

    private void collectLogFiles(Path root, int depth, Set<Path> result) {
        if (root == null || !Files.isDirectory(root) || result.size() >= MAX_LOG_FILES) {
            return;
        }
        try (var stream = Files.walk(root, depth)) {
            stream.filter(Files::isRegularFile)
                    .filter(this::isLogFile)
                    .limit(MAX_LOG_FILES - result.size())
                    .map(Path::toAbsolutePath)
                    .map(Path::normalize)
                    .forEach(result::add);
        } catch (IOException ignored) {
            // Runtime inventory remains available even when a log folder cannot be read.
        }
    }

    private int logScore(Path path, Matrix26RuntimeTarget target) {
        String normalized = path.toString().replace('\\', '/').toLowerCase(Locale.ROOT);
        int score = 0;
        String profile = lower(target.runtimeProfile());
        String code = lower(target.code());
        if (!profile.isBlank() && normalized.contains(profile)) {
            score += 12;
        }
        if (!code.isBlank() && normalized.contains(code)) {
            score += 12;
        }
        if (target.expectedPort() != null && normalized.contains(String.valueOf(target.expectedPort()))) {
            score += 8;
        }
        if (target.controlCenter() && normalized.contains("matrix26") && normalized.contains("control")) {
            score += 10;
        }
        return score;
    }

    private List<Matrix26LogInventoryItem> toLogItems(
            Matrix26RuntimeInventoryItem runtime,
            List<Path> paths,
            Path projectRoot
    ) {
        List<Matrix26LogInventoryItem> result = new ArrayList<>();
        for (int index = 0; index < paths.size(); index++) {
            result.add(toLogItem(runtime.target(), paths.get(index), projectRoot, index == 0));
        }
        return result;
    }

    private Matrix26LogInventoryItem toLogItem(
            Matrix26RuntimeTarget target,
            Path path,
            Path projectRoot,
            boolean currentCandidate
    ) {
        long size = fileSize(path);
        return new Matrix26LogInventoryItem(
                target.key(),
                target.businessName(),
                relative(projectRoot, path),
                size,
                formatBytes(size),
                lastModified(path),
                currentCandidate
        );
    }

    private Path resolveRuntimeDirectory(Path runtimeRoot, Matrix26RuntimeTarget target, List<String> warnings) {
        String profile = target.runtimeProfile();
        if (!Matrix26OperationsSanitizer.isSafeRuntimeName(profile)) {
            warnings.add("The runtime profile contains unsupported characters and was not used to build a path.");
            return null;
        }
        Path resolved = runtimeRoot.resolve(profile).normalize();
        if (!resolved.startsWith(runtimeRoot)) {
            warnings.add("The runtime path was blocked by the security policy.");
            return null;
        }
        return resolved;
    }

    private Path resolveAssetsDirectory(Path dataRoot, Matrix26RuntimeTarget target) {
        String code = target.code();
        if (!Matrix26OperationsSanitizer.isSafeRuntimeName(code)) {
            return null;
        }
        Path direct = dataRoot.resolve(code).normalize();
        if (Files.exists(direct) && direct.startsWith(dataRoot)) {
            return direct;
        }
        String profileSlug = Matrix26OperationsSanitizer.slug(target.runtimeProfile());
        Path profile = dataRoot.resolve(profileSlug).normalize();
        return profile.startsWith(dataRoot) ? profile : null;
    }

    private Path findLauncher(Path runtimeDirectory) {
        if (runtimeDirectory == null || !Files.isDirectory(runtimeDirectory)) {
            return null;
        }
        for (String name : List.of("run.sh", "run.ps1", "run.cmd", "run.bat")) {
            Path candidate = runtimeDirectory.resolve(name).normalize();
            if (Files.isRegularFile(candidate)) {
                return candidate;
            }
        }
        return null;
    }

    private List<String> readLastLines(Path path, int maximumLines) throws IOException {
        Deque<String> lines = new ArrayDeque<>(maximumLines);
        try (BufferedReader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (lines.size() == maximumLines) {
                    lines.removeFirst();
                }
                lines.addLast(Matrix26OperationsSanitizer.limit(line, 1200));
            }
        }
        return List.copyOf(lines);
    }

    private Optional<Path> resolveSafeRelativePath(String relativePath) {
        if (relativePath == null || relativePath.isBlank()) {
            return Optional.empty();
        }
        Path projectRoot = Path.of("").toAbsolutePath().normalize();
        Path resolved = projectRoot.resolve(relativePath).normalize();
        if (!resolved.startsWith(projectRoot)) {
            return Optional.empty();
        }
        return Optional.of(resolved);
    }

    private Path safeChild(Path projectRoot, String configuredPath) {
        String value = configuredPath == null || configuredPath.isBlank() ? "." : configuredPath.trim();
        Path candidate = Path.of(value);
        Path resolved = candidate.isAbsolute() ? candidate.normalize() : projectRoot.resolve(candidate).normalize();
        if (!resolved.startsWith(projectRoot)) {
            return projectRoot.resolve("__matrix26_blocked_path__");
        }
        return resolved;
    }

    private long directorySize(Path root) {
        if (root == null || !Files.isDirectory(root)) {
            return 0L;
        }
        try (var stream = Files.walk(root, 6)) {
            return stream.filter(Files::isRegularFile)
                    .mapToLong(this::fileSize)
                    .sum();
        } catch (IOException ex) {
            return 0L;
        }
    }

    private long fileSize(Path path) {
        try {
            return Files.size(path);
        } catch (IOException ex) {
            return 0L;
        }
    }

    private LocalDateTime lastModified(Path path) {
        try {
            return LocalDateTime.ofInstant(Files.getLastModifiedTime(path).toInstant(), ZoneId.systemDefault());
        } catch (IOException ex) {
            return null;
        }
    }

    private String relative(Path projectRoot, Path path) {
        if (path == null) {
            return "";
        }
        Path normalized = path.toAbsolutePath().normalize();
        if (!normalized.startsWith(projectRoot)) {
            return "";
        }
        return projectRoot.relativize(normalized).toString().replace('\\', '/');
    }

    private boolean isLogFile(Path path) {
        String name = path.getFileName().toString().toLowerCase(Locale.ROOT);
        return LOG_EXTENSIONS.stream().anyMatch(name::endsWith);
    }

    private boolean isExternallyManaged(Matrix26RuntimeTarget target) {
        return "PROTECTED".equalsIgnoreCase(target.managementMode())
                || "EXTERNAL".equalsIgnoreCase(target.managementMode());
    }

    private boolean isLocalHost(String host) {
        if (host == null) {
            return false;
        }
        String normalized = host.toLowerCase(Locale.ROOT);
        return normalized.equals("localhost")
                || normalized.equals("127.0.0.1")
                || normalized.equals("::1")
                || normalized.equals("0:0:0:0:0:0:0:1");
    }

    private String databaseName(String jdbcUrl) {
        if (jdbcUrl == null || jdbcUrl.isBlank()) {
            return "";
        }
        Matcher matcher = DATABASE_NAME.matcher(jdbcUrl.trim());
        return matcher.find() ? matcher.group(1) : "";
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (!isBlank(value)) {
                return value.trim();
            }
        }
        return "";
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private Integer parseInteger(String value) {
        try {
            return value == null ? null : Integer.valueOf(value.trim());
        } catch (RuntimeException ex) {
            return null;
        }
    }

    private String lower(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT);
    }

    private String formatBytes(long bytes) {
        if (bytes < 1024) {
            return bytes + " B";
        }
        double value = bytes;
        String[] units = {"KB", "MB", "GB", "TB"};
        int unit = -1;
        do {
            value /= 1024.0;
            unit++;
        } while (value >= 1024.0 && unit < units.length - 1);
        return String.format(Locale.US, value >= 10 ? "%.1f %s" : "%.2f %s", value, units[unit]);
    }

    private String formatDuration(Duration duration) {
        if (duration.isNegative()) {
            return "—";
        }
        long seconds = duration.getSeconds();
        long days = seconds / 86_400;
        long hours = (seconds % 86_400) / 3_600;
        long minutes = (seconds % 3_600) / 60;
        if (days > 0) {
            return days + " d " + hours + " h";
        }
        if (hours > 0) {
            return hours + " h " + minutes + " min";
        }
        return Math.max(0, minutes) + " min";
    }

    private String safeMessage(Exception ex) {
        String message = ex.getMessage();
        return message == null || message.isBlank()
                ? ex.getClass().getSimpleName()
                : Matrix26OperationsSanitizer.limit(message, 300);
    }

    private record RuntimeConfiguration(Integer port, String runtimeProfile, String databaseName) {
        private static RuntimeConfiguration empty() {
            return new RuntimeConfiguration(null, "", "");
        }
    }

    private record CachedSnapshot(Matrix26OperationsSnapshot snapshot, Instant createdAt) {
    }
}
