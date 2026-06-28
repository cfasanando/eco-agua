package com.ecoamazonas.eco_agua.platform.control.lifecycle.archive;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.LinkedHashSet;
import java.util.Set;

@Component
@ConfigurationProperties(prefix = "matrix26.control-center.lifecycle.archive")
public class Matrix26ArchiveProperties {
    private boolean enabled = true;
    private Set<String> allowedInstanceCodes = new LinkedHashSet<>(Set.of("matrix26-appearance-lab"));
    private String cloneInstanceCode = "matrix26-archived-restore-test";
    private String cloneInstanceName = "Matrix26 Archived Restore Test";
    private String cloneDatabaseName = "matrix26_archived_restore_test";
    private String cloneRuntimeProfile = "matrix26_archived_restore_test";
    private int cloneRuntimePort = 8096;
    private String clonePublicUrl = "http://localhost:8096";

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public Set<String> getAllowedInstanceCodes() { return allowedInstanceCodes; }
    public void setAllowedInstanceCodes(Set<String> values) { allowedInstanceCodes = values == null ? new LinkedHashSet<>() : new LinkedHashSet<>(values); }
    public String getCloneInstanceCode() { return cloneInstanceCode; }
    public void setCloneInstanceCode(String value) { cloneInstanceCode = value; }
    public String getCloneInstanceName() { return cloneInstanceName; }
    public void setCloneInstanceName(String value) { cloneInstanceName = value; }
    public String getCloneDatabaseName() { return cloneDatabaseName; }
    public void setCloneDatabaseName(String value) { cloneDatabaseName = value; }
    public String getCloneRuntimeProfile() { return cloneRuntimeProfile; }
    public void setCloneRuntimeProfile(String value) { cloneRuntimeProfile = value; }
    public int getCloneRuntimePort() { return cloneRuntimePort; }
    public void setCloneRuntimePort(int value) { cloneRuntimePort = value; }
    public String getClonePublicUrl() { return clonePublicUrl; }
    public void setClonePublicUrl(String value) { clonePublicUrl = value; }
}
