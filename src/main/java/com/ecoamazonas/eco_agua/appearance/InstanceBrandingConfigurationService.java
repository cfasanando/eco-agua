package com.ecoamazonas.eco_agua.appearance;

import com.ecoamazonas.eco_agua.platform.control.appearance.Matrix26JsonCodec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@ConditionalOnProperty(
        name = "matrix26.control-center.enabled",
        havingValue = "false",
        matchIfMissing = true
)
public class InstanceBrandingConfigurationService {

    private static final Logger LOG = LoggerFactory.getLogger(InstanceBrandingConfigurationService.class);
    private static final long CACHE_MILLIS = 2_000L;

    private final JdbcTemplate jdbcTemplate;
    private final String runtimeClientCode;
    private volatile CacheEntry cache = new CacheEntry(0L, InstanceBrandingConfiguration.defaults());

    public InstanceBrandingConfigurationService(
            JdbcTemplate jdbcTemplate,
            @Value("${ecoagua.platform.client-code:}") String runtimeClientCode
    ) {
        this.jdbcTemplate = jdbcTemplate;
        this.runtimeClientCode = runtimeClientCode == null ? "" : runtimeClientCode.trim();
    }

    public InstanceBrandingConfiguration current() {
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
            InstanceBrandingConfiguration loaded = load();
            cache = new CacheEntry(now, loaded);
            return loaded;
        }
    }

    public void invalidate() {
        cache = new CacheEntry(0L, InstanceBrandingConfiguration.defaults());
    }

    private InstanceBrandingConfiguration load() {
        try {
            Integer columns = jdbcTemplate.queryForObject(
                    """
                    SELECT COUNT(*)
                    FROM information_schema.COLUMNS
                    WHERE TABLE_SCHEMA = DATABASE()
                      AND TABLE_NAME = 'matrix26_instance_appearance_config'
                      AND COLUMN_NAME IN ('branding_json', 'asset_manifest_json')
                    """,
                    Integer.class
            );
            if (columns == null || columns < 2) {
                return InstanceBrandingConfiguration.defaults();
            }

            return jdbcTemplate.query(
                    """
                    SELECT instance_code, branding_json, asset_manifest_json
                    FROM matrix26_instance_appearance_config
                    WHERE id = 1
                    """,
                    resultSet -> {
                        if (!resultSet.next()) {
                            return InstanceBrandingConfiguration.defaults();
                        }
                        String storedInstanceCode = resultSet.getString("instance_code");
                        if (!runtimeClientCode.isBlank()
                                && (storedInstanceCode == null
                                || !runtimeClientCode.equalsIgnoreCase(storedInstanceCode.trim()))) {
                            return InstanceBrandingConfiguration.defaults();
                        }
                        Map<String, String> branding = Matrix26JsonCodec.readFlatObject(
                                resultSet.getString("branding_json")
                        );
                        Map<String, String> assets = Matrix26JsonCodec.readFlatObject(
                                resultSet.getString("asset_manifest_json")
                        );
                        return new InstanceBrandingConfiguration(
                                !branding.isEmpty() || !assets.isEmpty(),
                                branding,
                                assets
                        );
                    }
            );
        } catch (DataAccessException ex) {
            LOG.warn("Could not load Matrix26 branding configuration: {}", safeMessage(ex));
            return InstanceBrandingConfiguration.defaults();
        }
    }

    private String safeMessage(Exception ex) {
        String message = ex.getMessage();
        if (message == null || message.isBlank()) {
            return ex.getClass().getSimpleName();
        }
        return message.length() > 300 ? message.substring(0, 300) : message;
    }

    private record CacheEntry(long loadedAt, InstanceBrandingConfiguration configuration) {
    }
}
