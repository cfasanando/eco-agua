package com.ecoamazonas.eco_agua.platform.module;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class PlatformModuleManager {

    private static final String REGISTRY_TABLE = "platform_module_installation";
    private static final int LOCK_TIMEOUT_SECONDS = 15;

    private final JdbcTemplate jdbcTemplate;
    private final DataSource dataSource;
    private final Map<String, PlatformModuleInstaller> installers;
    private final boolean installationAllowed;

    public PlatformModuleManager(JdbcTemplate jdbcTemplate,
                                 DataSource dataSource,
                                 List<PlatformModuleInstaller> installerList,
                                 @Value("${ecoagua.modules.installation-allowed:false}") boolean installationAllowed) {
        this.jdbcTemplate = jdbcTemplate;
        this.dataSource = dataSource;
        this.installationAllowed = installationAllowed;
        this.installers = new LinkedHashMap<>();
        for (PlatformModuleInstaller installer : installerList) {
            String key = normalizeKey(installer.moduleKey());
            if (installers.putIfAbsent(key, installer) != null) {
                throw new IllegalStateException("Duplicate module installer: " + key);
            }
        }
    }

    public boolean supports(String moduleKey) {
        return installers.containsKey(normalizeKey(moduleKey));
    }

    public List<PlatformModuleInstallationStatus> listStatuses() {
        return installers.values().stream()
                .map(this::statusFor)
                .toList();
    }

    public PlatformModuleInstallationStatus getStatus(String moduleKey) {
        return statusFor(requiredInstaller(moduleKey));
    }

    public void installAndActivate(String moduleKey, boolean demoData) {
        assertInstallationAllowed();
        PlatformModuleInstaller installer = requiredInstaller(moduleKey);
        String lockName = lockNameFor(installer);
        Connection lockConnection = acquireLock(lockName);

        try {
            ensureRegistryTable();
            markInstalling(installer, "Preparing installation");

            for (PlatformModuleInstallStep step : installer.installationSteps(demoData)) {
                updateCurrentStep(installer.moduleKey(), step.code() + " - " + step.label());
                step.execute();
            }

            if (!installer.isInstalled()) {
                throw new IllegalStateException("The module schema validation did not pass after installation.");
            }

            installer.setEnabled(true);
            markCompleted(installer, "ACTIVE", true);
        } catch (RuntimeException ex) {
            safelyDisable(installer);
            markFailed(installer, cleanError(ex));
            throw new IllegalArgumentException(
                    "Could not install module " + installer.displayName() + ": " + cleanError(ex),
                    ex
            );
        } finally {
            releaseLock(lockConnection, lockName);
        }
    }

    public void synchronizeInstalledModule(String moduleKey) {
        assertInstallationAllowed();
        PlatformModuleInstaller installer = requiredInstaller(moduleKey);
        String lockName = lockNameFor(installer);
        Connection lockConnection = acquireLock(lockName);

        try {
            if (!installer.isInstalled()) {
                throw new IllegalArgumentException("The module schema is not complete and cannot be synchronized.");
            }
            ensureRegistryTable();
            boolean enabled = installer.isEnabled();
            markCompleted(installer, enabled ? "ACTIVE" : "DISABLED", enabled);
        } finally {
            releaseLock(lockConnection, lockName);
        }
    }

    public void disable(String moduleKey) {
        assertInstallationAllowed();
        PlatformModuleInstaller installer = requiredInstaller(moduleKey);
        String lockName = lockNameFor(installer);
        Connection lockConnection = acquireLock(lockName);

        try {
            installer.setEnabled(false);
            if (registryTableExists()) {
                jdbcTemplate.update("""
                        UPDATE platform_module_installation
                        SET status = 'DISABLED', enabled = 0, current_step = NULL, last_error = NULL, updated_at = NOW()
                        WHERE module_key = ?
                        """, installer.moduleKey());
            }
        } finally {
            releaseLock(lockConnection, lockName);
        }
    }

    public String currentDatabaseName() {
        String database = jdbcTemplate.queryForObject("SELECT DATABASE()", String.class);
        return database == null || database.isBlank() ? "unknown" : database;
    }

    public boolean isInstallationAllowed() {
        return installationAllowed;
    }

    private void assertInstallationAllowed() {
        if (!installationAllowed) {
            throw new IllegalStateException(
                    "Module installation is disabled for this runtime. "
                            + "Set ecoagua.modules.installation-allowed=true only in a disposable or managed instance."
            );
        }
    }

    private PlatformModuleInstallationStatus statusFor(PlatformModuleInstaller installer) {
        boolean schemaInstalled = safeInstalledCheck(installer);
        boolean enabled = safeEnabledCheck(installer);

        if (!registryTableExists()) {
            return unregisteredStatus(installer, schemaInstalled, enabled);
        }

        List<PlatformModuleInstallationStatus> rows = jdbcTemplate.query("""
                SELECT module_key, installed_version, target_version, status, enabled,
                       current_step, last_error, started_at, completed_at, updated_at
                FROM platform_module_installation
                WHERE module_key = ?
                """, (rs, rowNum) -> new PlatformModuleInstallationStatus(
                rs.getString("module_key"),
                installer.displayName(),
                rs.getString("installed_version"),
                installer.currentVersion(),
                rs.getString("status"),
                enabled,
                schemaInstalled,
                true,
                rs.getString("current_step"),
                rs.getString("last_error"),
                toLocalDateTime(rs.getTimestamp("started_at")),
                toLocalDateTime(rs.getTimestamp("completed_at")),
                toLocalDateTime(rs.getTimestamp("updated_at"))
        ), installer.moduleKey());

        if (rows.isEmpty()) {
            return unregisteredStatus(installer, schemaInstalled, enabled);
        }
        return rows.get(0);
    }

    private PlatformModuleInstallationStatus unregisteredStatus(PlatformModuleInstaller installer,
                                                                boolean schemaInstalled,
                                                                boolean enabled) {
        return new PlatformModuleInstallationStatus(
                installer.moduleKey(),
                installer.displayName(),
                null,
                installer.currentVersion(),
                schemaInstalled ? "INSTALLED" : "NOT_INSTALLED",
                enabled,
                schemaInstalled,
                false,
                null,
                null,
                null,
                null,
                null
        );
    }

    private void ensureRegistryTable() {
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS platform_module_installation (
                    id BIGINT NOT NULL AUTO_INCREMENT,
                    module_key VARCHAR(80) NOT NULL,
                    installed_version VARCHAR(50) NULL,
                    target_version VARCHAR(50) NULL,
                    status VARCHAR(30) NOT NULL DEFAULT 'NOT_INSTALLED',
                    enabled TINYINT(1) NOT NULL DEFAULT 0,
                    current_step VARCHAR(255) NULL,
                    last_error VARCHAR(2000) NULL,
                    started_at DATETIME NULL,
                    completed_at DATETIME NULL,
                    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                    PRIMARY KEY (id),
                    UNIQUE KEY uk_platform_module_installation_key (module_key),
                    KEY idx_platform_module_installation_status (status),
                    KEY idx_platform_module_installation_enabled (enabled)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
                """);
    }

    private void markInstalling(PlatformModuleInstaller installer, String currentStep) {
        jdbcTemplate.update("""
                INSERT INTO platform_module_installation
                    (module_key, installed_version, target_version, status, enabled, current_step,
                     last_error, started_at, completed_at, updated_at)
                VALUES (?, NULL, ?, 'INSTALLING', 0, ?, NULL, NOW(), NULL, NOW())
                ON DUPLICATE KEY UPDATE
                    target_version = VALUES(target_version),
                    status = 'INSTALLING',
                    enabled = 0,
                    current_step = VALUES(current_step),
                    last_error = NULL,
                    started_at = NOW(),
                    completed_at = NULL,
                    updated_at = NOW()
                """, installer.moduleKey(), installer.currentVersion(), currentStep);
    }

    private void updateCurrentStep(String moduleKey, String currentStep) {
        jdbcTemplate.update("""
                UPDATE platform_module_installation
                SET current_step = ?, updated_at = NOW()
                WHERE module_key = ?
                """, currentStep, moduleKey);
    }

    private void markCompleted(PlatformModuleInstaller installer, String status, boolean enabled) {
        jdbcTemplate.update("""
                INSERT INTO platform_module_installation
                    (module_key, installed_version, target_version, status, enabled, current_step,
                     last_error, started_at, completed_at, updated_at)
                VALUES (?, ?, ?, ?, ?, NULL, NULL, NOW(), NOW(), NOW())
                ON DUPLICATE KEY UPDATE
                    installed_version = VALUES(installed_version),
                    target_version = VALUES(target_version),
                    status = VALUES(status),
                    enabled = VALUES(enabled),
                    current_step = NULL,
                    last_error = NULL,
                    completed_at = NOW(),
                    updated_at = NOW()
                """,
                installer.moduleKey(),
                installer.currentVersion(),
                installer.currentVersion(),
                status,
                enabled
        );
    }

    private void markFailed(PlatformModuleInstaller installer, String error) {
        try {
            ensureRegistryTable();
            jdbcTemplate.update("""
                    INSERT INTO platform_module_installation
                        (module_key, installed_version, target_version, status, enabled, current_step,
                         last_error, started_at, completed_at, updated_at)
                    VALUES (?, NULL, ?, 'FAILED', 0, NULL, ?, NOW(), NULL, NOW())
                    ON DUPLICATE KEY UPDATE
                        target_version = VALUES(target_version),
                        status = 'FAILED',
                        enabled = 0,
                        current_step = NULL,
                        last_error = VALUES(last_error),
                        updated_at = NOW()
                    """, installer.moduleKey(), installer.currentVersion(), error);
        } catch (DataAccessException ignored) {
            // The original installation exception is more relevant than a registry write failure.
        }
    }

    private String lockNameFor(PlatformModuleInstaller installer) {
        String rawName = "ecoagua-module-" + currentDatabaseName() + "-" + installer.moduleKey();
        if (rawName.length() <= 64) {
            return rawName;
        }
        String suffix = Integer.toHexString(rawName.hashCode());
        return rawName.substring(0, Math.min(54, rawName.length())) + "-" + suffix;
    }

    private Connection acquireLock(String lockName) {
        try {
            Connection connection = dataSource.getConnection();
            try (PreparedStatement statement = connection.prepareStatement("SELECT GET_LOCK(?, ?)")) {
                statement.setString(1, lockName);
                statement.setInt(2, LOCK_TIMEOUT_SECONDS);
                try (ResultSet resultSet = statement.executeQuery()) {
                    Integer result = resultSet.next() ? resultSet.getInt(1) : null;
                    if (result == null || result != 1) {
                        connection.close();
                        throw new IllegalStateException("Another installation process is already running for this module.");
                    }
                }
            }
            return connection;
        } catch (SQLException ex) {
            throw new IllegalStateException("Could not acquire the module installation lock.", ex);
        }
    }

    private void releaseLock(Connection connection, String lockName) {
        if (connection == null) {
            return;
        }
        try {
            try (PreparedStatement statement = connection.prepareStatement("SELECT RELEASE_LOCK(?)")) {
                statement.setString(1, lockName);
                statement.executeQuery().close();
            }
        } catch (SQLException ignored) {
            // Closing the connection is still required even when lock release fails.
        } finally {
            try {
                connection.close();
            } catch (SQLException ignored) {
                // The pool will clean up the connection if needed.
            }
        }
    }

    private PlatformModuleInstaller requiredInstaller(String moduleKey) {
        PlatformModuleInstaller installer = installers.get(normalizeKey(moduleKey));
        if (installer == null) {
            throw new IllegalArgumentException("No installer is registered for module: " + moduleKey);
        }
        return installer;
    }

    private boolean registryTableExists() {
        Integer count = jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM information_schema.tables
                WHERE table_schema = DATABASE()
                  AND table_name = ?
                """, Integer.class, REGISTRY_TABLE);
        return count != null && count > 0;
    }

    private boolean safeInstalledCheck(PlatformModuleInstaller installer) {
        try {
            return installer.isInstalled();
        } catch (RuntimeException ex) {
            return false;
        }
    }

    private boolean safeEnabledCheck(PlatformModuleInstaller installer) {
        try {
            return installer.isEnabled();
        } catch (RuntimeException ex) {
            return false;
        }
    }

    private void safelyDisable(PlatformModuleInstaller installer) {
        try {
            installer.setEnabled(false);
        } catch (RuntimeException ignored) {
            // Preserve the original installation exception.
        }
    }

    private String cleanError(Throwable throwable) {
        String message = throwable.getMessage();
        if (message == null || message.isBlank()) {
            message = throwable.getClass().getSimpleName();
        }
        message = message.replaceAll("[\\r\\n\\t]+", " ").trim();
        return message.length() > 1900 ? message.substring(0, 1900) : message;
    }

    private String normalizeKey(String value) {
        if (value == null) {
            return "";
        }
        return value.trim().toLowerCase(Locale.ROOT);
    }

    private LocalDateTime toLocalDateTime(Timestamp timestamp) {
        return timestamp == null ? null : timestamp.toLocalDateTime();
    }
}
