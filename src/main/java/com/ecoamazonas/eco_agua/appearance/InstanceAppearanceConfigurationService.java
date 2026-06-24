package com.ecoamazonas.eco_agua.appearance;

import com.ecoamazonas.eco_agua.platform.control.appearance.Matrix26JsonCodec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.Map;

@Service
@ConditionalOnProperty(
        name = "matrix26.control-center.enabled",
        havingValue = "false",
        matchIfMissing = true
)
public class InstanceAppearanceConfigurationService {

    private static final Logger LOG = LoggerFactory.getLogger(InstanceAppearanceConfigurationService.class);
    private static final long CACHE_MILLIS = 2_000L;

    private final JdbcTemplate jdbcTemplate;
    private final String runtimeClientCode;
    private volatile CacheEntry cache = new CacheEntry(0L, InstanceAppearanceConfiguration.defaults());

    public InstanceAppearanceConfigurationService(
            JdbcTemplate jdbcTemplate,
            @Value("${ecoagua.platform.client-code:}") String runtimeClientCode
    ) {
        this.jdbcTemplate = jdbcTemplate;
        this.runtimeClientCode = runtimeClientCode == null ? "" : runtimeClientCode.trim();
    }

    public InstanceAppearanceConfiguration current() {
        long now = System.currentTimeMillis();
        CacheEntry current = cache;
        if (now - current.loadedAt() < CACHE_MILLIS) {
            return current.configuration();
        }

        synchronized (this) {
            current = cache;
            if (now - current.loadedAt() < CACHE_MILLIS) {
                return current.configuration();
            }
            InstanceAppearanceConfiguration loaded = load();
            cache = new CacheEntry(now, loaded);
            return loaded;
        }
    }

    public void invalidate() {
        cache = new CacheEntry(0L, InstanceAppearanceConfiguration.defaults());
    }

    private InstanceAppearanceConfiguration load() {
        try {
            Integer tableCount = jdbcTemplate.queryForObject(
                    """
                    SELECT COUNT(*)
                    FROM information_schema.TABLES
                    WHERE TABLE_SCHEMA = DATABASE()
                      AND TABLE_NAME = 'matrix26_instance_appearance_config'
                    """,
                    Integer.class
            );
            if (tableCount == null || tableCount == 0) {
                return InstanceAppearanceConfiguration.defaults();
            }

            return jdbcTemplate.query(
                    """
                    SELECT instance_code,
                           public_theme_code,
                           public_layout_code,
                           admin_theme_code,
                           admin_layout_code,
                           login_layout_code,
                           overrides_json,
                           published_version,
                           published_at,
                           published_by
                    FROM matrix26_instance_appearance_config
                    WHERE id = 1
                    """,
                    resultSet -> {
                        if (!resultSet.next()) {
                            return InstanceAppearanceConfiguration.defaults();
                        }
                        String storedInstanceCode = resultSet.getString("instance_code");
                        if (!runtimeClientCode.isBlank()
                                && (storedInstanceCode == null
                                || !runtimeClientCode.equalsIgnoreCase(storedInstanceCode.trim()))) {
                            LOG.warn(
                                    "Ignoring Matrix26 appearance configuration for instance {} in runtime {}.",
                                    storedInstanceCode,
                                    runtimeClientCode
                            );
                            return InstanceAppearanceConfiguration.defaults();
                        }

                        Timestamp publishedAt = resultSet.getTimestamp("published_at");
                        Map<String, String> overrides = Matrix26JsonCodec.readFlatObject(
                                resultSet.getString("overrides_json")
                        );
                        return new InstanceAppearanceConfiguration(
                                true,
                                resultSet.getString("public_theme_code"),
                                resultSet.getString("public_layout_code"),
                                resultSet.getString("admin_theme_code"),
                                resultSet.getString("admin_layout_code"),
                                resultSet.getString("login_layout_code"),
                                overrides,
                                resultSet.getInt("published_version"),
                                publishedAt == null ? null : publishedAt.toLocalDateTime(),
                                resultSet.getString("published_by")
                        );
                    }
            );
        } catch (DataAccessException ex) {
            LOG.warn("Could not load the local Matrix26 appearance configuration. Using the existing portal styles: {}",
                    safeMessage(ex));
            return InstanceAppearanceConfiguration.defaults();
        }
    }

    private String safeMessage(Exception ex) {
        String message = ex.getMessage();
        if (message == null || message.isBlank()) {
            return ex.getClass().getSimpleName();
        }
        return message.length() > 300 ? message.substring(0, 300) : message;
    }

    private record CacheEntry(long loadedAt, InstanceAppearanceConfiguration configuration) {
    }
}
