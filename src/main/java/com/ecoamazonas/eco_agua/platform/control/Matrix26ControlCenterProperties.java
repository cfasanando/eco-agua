package com.ecoamazonas.eco_agua.platform.control;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "matrix26.control-center")
public class Matrix26ControlCenterProperties {

    private boolean enabled;
    private boolean readOnly = true;
    private String displayName = "Matrix26 Control Center";
    private String runtimeProfile = "matrix26_control";
    private String databaseName = "matrix26_platform_control";
    private String portalUrl = "http://localhost:8091";
    private String bootstrapAdminUsername = "matrix_admin";
    private String bootstrapAdminPassword = "Matrix26Demo123!";
    private long healthCacheSeconds = 30;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isReadOnly() {
        return readOnly;
    }

    public void setReadOnly(boolean readOnly) {
        this.readOnly = readOnly;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getRuntimeProfile() {
        return runtimeProfile;
    }

    public void setRuntimeProfile(String runtimeProfile) {
        this.runtimeProfile = runtimeProfile;
    }

    public String getDatabaseName() {
        return databaseName;
    }

    public void setDatabaseName(String databaseName) {
        this.databaseName = databaseName;
    }

    public String getPortalUrl() {
        return portalUrl;
    }

    public void setPortalUrl(String portalUrl) {
        this.portalUrl = portalUrl;
    }

    public String getBootstrapAdminUsername() {
        return bootstrapAdminUsername;
    }

    public void setBootstrapAdminUsername(String bootstrapAdminUsername) {
        this.bootstrapAdminUsername = bootstrapAdminUsername;
    }

    public String getBootstrapAdminPassword() {
        return bootstrapAdminPassword;
    }

    public void setBootstrapAdminPassword(String bootstrapAdminPassword) {
        this.bootstrapAdminPassword = bootstrapAdminPassword;
    }

    public long getHealthCacheSeconds() {
        return healthCacheSeconds;
    }

    public void setHealthCacheSeconds(long healthCacheSeconds) {
        this.healthCacheSeconds = healthCacheSeconds;
    }
}
