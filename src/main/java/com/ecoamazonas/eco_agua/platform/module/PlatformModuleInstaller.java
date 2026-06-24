package com.ecoamazonas.eco_agua.platform.module;

import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;

public interface PlatformModuleInstaller {

    String moduleKey();

    String displayName();

    String currentVersion();

    boolean isInstalled();

    boolean isEnabled();

    List<PlatformModuleInstallStep> installationSteps(boolean demoData);

    void setEnabled(boolean enabled);

    default boolean supportsTargetInstallation() {
        return false;
    }

    default void installOnTarget(JdbcTemplate targetJdbcTemplate, boolean demoData) {
        throw new UnsupportedOperationException(
                "Target installation is not implemented for module " + moduleKey() + "."
        );
    }
}
