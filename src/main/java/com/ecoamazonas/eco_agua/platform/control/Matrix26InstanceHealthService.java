package com.ecoamazonas.eco_agua.platform.control;

import com.ecoamazonas.eco_agua.platform.PlatformBusinessClient;
import com.ecoamazonas.eco_agua.platform.PlatformBusinessClientRepository;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Service
@ConditionalOnProperty(name = "matrix26.control-center.enabled", havingValue = "true")
public class Matrix26InstanceHealthService {

    private static final int CONNECT_TIMEOUT_MS = 1_200;
    private static final int READ_TIMEOUT_MS = 1_200;

    private final PlatformBusinessClientRepository clientRepository;
    private final Matrix26InstanceHealthCheckRepository healthCheckRepository;
    private final Matrix26ControlCenterProperties properties;

    public Matrix26InstanceHealthService(
            PlatformBusinessClientRepository clientRepository,
            Matrix26InstanceHealthCheckRepository healthCheckRepository,
            Matrix26ControlCenterProperties properties
    ) {
        this.clientRepository = clientRepository;
        this.healthCheckRepository = healthCheckRepository;
        this.properties = properties;
    }

    public List<Matrix26InstanceStatus> currentStatuses(boolean forceRefresh) {
        return clientRepository.findByMonitorVisibleTrueOrderByCreatedAtDescIdDesc().stream()
                .map(client -> currentStatus(client, forceRefresh))
                .sorted(Comparator.comparing(status -> status.instance().getRuntimePort(), Comparator.nullsLast(Integer::compareTo)))
                .toList();
    }

    public List<Matrix26HealthCheckView> recentChecks() {
        return healthCheckRepository.findTop20ByOrderByCheckedAtDesc().stream()
                .map(check -> new Matrix26HealthCheckView(
                        check.getInstance().getBusinessName(),
                        check.getInstance().getCode(),
                        check.isOnline(),
                        check.getHttpStatus(),
                        check.getResponseTimeMs(),
                        check.getMessage(),
                        check.getCheckedAt(),
                        check.isOnline() ? "text-bg-success" : "text-bg-danger",
                        check.isOnline() ? "En línea" : "Fuera de línea"
                ))
                .toList();
    }

    public Matrix26ControlSummary buildSummary(List<Matrix26InstanceStatus> statuses, long totalModules) {
        long online = statuses.stream().filter(Matrix26InstanceStatus::online).count();
        long protectedInstances = statuses.stream()
                .filter(status -> status.instance().isProtectedInstance())
                .count();
        LocalDateTime lastCheckedAt = statuses.stream()
                .map(Matrix26InstanceStatus::checkedAt)
                .filter(value -> value != null)
                .max(LocalDateTime::compareTo)
                .orElse(null);

        return new Matrix26ControlSummary(
                statuses.size(),
                online,
                statuses.size() - online,
                protectedInstances,
                totalModules,
                lastCheckedAt
        );
    }

    private Matrix26InstanceStatus currentStatus(PlatformBusinessClient client, boolean forceRefresh) {
        Optional<Matrix26InstanceHealthCheck> latest = healthCheckRepository
                .findTopByInstance_IdOrderByCheckedAtDesc(client.getId());

        Matrix26InstanceHealthCheck check = latest
                .filter(value -> !forceRefresh && isFresh(value.getCheckedAt()))
                .orElseGet(() -> performCheck(client));

        return new Matrix26InstanceStatus(
                client,
                check.isOnline(),
                check.getHttpStatus(),
                check.getResponseTimeMs(),
                check.getMessage(),
                check.getCheckedAt(),
                check.isOnline() ? "En línea" : "Fuera de línea",
                check.isOnline() ? "text-bg-success" : "text-bg-danger"
        );
    }

    private boolean isFresh(LocalDateTime checkedAt) {
        if (checkedAt == null) {
            return false;
        }
        long cacheSeconds = Math.max(properties.getHealthCacheSeconds(), 5);
        return checkedAt.isAfter(LocalDateTime.now().minusSeconds(cacheSeconds));
    }

    private Matrix26InstanceHealthCheck performCheck(PlatformBusinessClient client) {
        String urlValue = defaultUrl(client);
        long startedAt = System.nanoTime();
        boolean online = false;
        Integer httpStatus = null;
        String message;

        try {
            URL url = URI.create(urlValue).toURL();
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setConnectTimeout(CONNECT_TIMEOUT_MS);
            connection.setReadTimeout(READ_TIMEOUT_MS);
            connection.setInstanceFollowRedirects(false);
            connection.setRequestMethod("GET");
            connection.setRequestProperty("User-Agent", "Matrix26-Control-Center/1.0");
            httpStatus = connection.getResponseCode();
            online = httpStatus >= 200 && httpStatus < 500;
            message = online
                    ? "The instance responded successfully."
                    : "The instance returned HTTP status " + httpStatus + ".";
            connection.disconnect();
        } catch (IllegalArgumentException | IOException ex) {
            message = safeMessage(ex);
        }

        long responseTimeMs = Math.max(0, (System.nanoTime() - startedAt) / 1_000_000L);

        Matrix26InstanceHealthCheck check = new Matrix26InstanceHealthCheck();
        check.setInstance(client);
        check.setOnline(online);
        check.setHttpStatus(httpStatus);
        check.setResponseTimeMs(responseTimeMs);
        check.setMessage(message);
        check.setCheckedAt(LocalDateTime.now());
        return healthCheckRepository.save(check);
    }

    private String defaultUrl(PlatformBusinessClient client) {
        if (client.getPublicUrl() != null && !client.getPublicUrl().isBlank()) {
            return client.getPublicUrl().trim();
        }
        Integer port = client.getRuntimePort();
        return "http://localhost:" + (port != null ? port : 8080);
    }

    private String safeMessage(Exception ex) {
        String message = ex.getMessage();
        if (message == null || message.isBlank()) {
            return "The instance did not respond.";
        }
        return message.length() > 450 ? message.substring(0, 450) : message;
    }
}
