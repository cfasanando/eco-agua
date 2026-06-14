package com.ecoamazonas.eco_agua.platform;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;

@Service
public class PlatformInstanceMonitorService {

    private static final int CONNECT_TIMEOUT_MS = 800;
    private static final int READ_TIMEOUT_MS = 800;

    private final PlatformBusinessClientRepository clientRepository;
    private final String runtimeClientsDirectory;

    public PlatformInstanceMonitorService(PlatformBusinessClientRepository clientRepository,
                                          @Value("${ecoagua.platform.runtime-clients-dir:runtime-clients}") String runtimeClientsDirectory) {
        this.clientRepository = clientRepository;
        this.runtimeClientsDirectory = runtimeClientsDirectory;
    }

    public List<PlatformInstanceMonitorItem> listInstances() {
        return clientRepository.findAllByOrderByCreatedAtDescIdDesc().stream()
                .map(this::buildItem)
                .toList();
    }

    public PlatformInstanceMonitorSummary buildSummary() {
        List<PlatformInstanceMonitorItem> items = listInstances();
        long readyClients = items.stream().filter(item -> item.databaseReady() && item.businessActive()).count();
        long runningInstances = items.stream().filter(PlatformInstanceMonitorItem::running).count();
        long stoppedReadyInstances = items.stream()
                .filter(item -> item.databaseReady() && item.businessActive() && item.runtimeFilesGenerated() && !item.running())
                .count();
        long pendingProvisioning = items.stream()
                .filter(item -> !item.databaseReady() || !item.businessActive())
                .count();
        long missingRuntimeFiles = items.stream()
                .filter(item -> item.databaseReady() && item.businessActive() && !item.runtimeFilesGenerated())
                .count();
        return new PlatformInstanceMonitorSummary(
                items.size(),
                readyClients,
                runningInstances,
                stoppedReadyInstances,
                pendingProvisioning,
                missingRuntimeFiles
        );
    }

    private PlatformInstanceMonitorItem buildItem(PlatformBusinessClient client) {
        String profile = normalizeProfile(defaultValue(client.getRuntimeProfile(), client.getCode()));
        int port = runtimePort(client);
        String localUrl = "http://localhost:" + port;
        String publicUrl = defaultValue(client.getPublicUrl(), localUrl);
        String databaseStatus = defaultValue(client.getDatabaseStatus(), "PENDING_STRUCTURE").toUpperCase(Locale.ROOT);
        String businessStatus = defaultValue(client.getStatus(), "DRAFT").toUpperCase(Locale.ROOT);
        String runtimeStatus = defaultValue(client.getRuntimeStatus(), "PENDING").toUpperCase(Locale.ROOT);
        boolean databaseReady = "READY".equals(databaseStatus);
        boolean businessActive = "ACTIVE".equals(businessStatus);
        boolean runtimeConfigured = client.getRuntimeProfile() != null && !client.getRuntimeProfile().isBlank()
                && client.getRuntimePort() != null;

        Path runtimeFolderPath = Path.of(runtimeClientsDirectory, profile).toAbsolutePath().normalize();
        Path configPath = runtimeFolderPath.resolve("application.properties");
        Path runScriptPath = runtimeFolderPath.resolve("run.sh");
        boolean configFileExists = Files.isRegularFile(configPath);
        boolean runScriptExists = Files.isRegularFile(runScriptPath);
        boolean runtimeFilesGenerated = configFileExists && runScriptExists;
        boolean running = databaseReady && businessActive && runtimeFilesGenerated && isReachable(localUrl);

        String healthStatus;
        String healthBadgeClass;
        String healthAlertClass;
        String healthDescription;

        if (!databaseReady) {
            healthStatus = "PENDIENTE";
            healthBadgeClass = "text-bg-warning";
            healthAlertClass = "alert-warning";
            healthDescription = "Falta completar el aprovisionamiento de base de datos.";
        } else if (!businessActive) {
            healthStatus = "FALTA ACTIVAR";
            healthBadgeClass = "text-bg-warning";
            healthAlertClass = "alert-warning";
            healthDescription = "La base está lista, pero el negocio todavía no está activo.";
        } else if (!runtimeFilesGenerated) {
            healthStatus = "SIN RUNTIME";
            healthBadgeClass = "text-bg-secondary";
            healthAlertClass = "alert-secondary";
            healthDescription = "Falta generar los archivos runtime para levantar esta instancia.";
        } else if (running) {
            healthStatus = "EN LÍNEA";
            healthBadgeClass = "text-bg-success";
            healthAlertClass = "alert-success";
            healthDescription = "La URL local respondió correctamente. Puedes abrir el negocio.";
        } else {
            healthStatus = "DETENIDO";
            healthBadgeClass = "text-bg-danger";
            healthAlertClass = "alert-danger";
            healthDescription = "La instalación está lista, pero el servidor del cliente no está levantado en su puerto.";
        }

        return new PlatformInstanceMonitorItem(
                client,
                profile,
                port,
                localUrl,
                publicUrl,
                databaseStatus,
                businessStatus,
                runtimeStatus,
                databaseReady,
                businessActive,
                runtimeConfigured,
                runtimeFilesGenerated,
                configFileExists,
                runScriptExists,
                running,
                healthStatus,
                healthBadgeClass,
                healthAlertClass,
                healthDescription,
                "bash scripts/run-client.sh " + profile + " " + port,
                runtimeFolderPath.toString(),
                client.getLastRuntimeGeneratedAt(),
                LocalDateTime.now()
        );
    }

    private boolean isReachable(String localUrl) {
        try {
            URL url = URI.create(localUrl).toURL();
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setConnectTimeout(CONNECT_TIMEOUT_MS);
            connection.setReadTimeout(READ_TIMEOUT_MS);
            connection.setInstanceFollowRedirects(false);
            connection.setRequestMethod("GET");
            int code = connection.getResponseCode();
            return code >= 200 && code < 500;
        } catch (IllegalArgumentException | IOException ex) {
            return false;
        }
    }

    private int runtimePort(PlatformBusinessClient client) {
        if (client.getRuntimePort() != null && client.getRuntimePort() > 0) {
            return client.getRuntimePort();
        }
        long id = client.getId() == null ? 1L : client.getId();
        int offset = (int) Math.min(Math.max(id, 1L), 500L);
        return 8081 + offset;
    }

    private String normalizeProfile(String value) {
        String normalized = defaultValue(value, "cliente_demo").toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9_]+", "_")
                .replaceAll("_+", "_")
                .replaceAll("^_|_$", "");
        return normalized.isBlank() ? "cliente_demo" : normalized;
    }

    private String defaultValue(String value, String fallback) {
        String clean = value == null ? "" : value.trim();
        return clean.isBlank() ? fallback : clean;
    }
}
