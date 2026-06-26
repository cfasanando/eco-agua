package com.ecoamazonas.eco_agua.platform.control.restores;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Component
@ConfigurationProperties(prefix = "matrix26.control-center.restores")
public class Matrix26RestoreProperties {
    private boolean enabled = true;
    private Set<String> allowedSourceInstanceCodes = new LinkedHashSet<>(Set.of("matrix26-appearance-lab"));
    private String targetInstanceCode = "matrix26-restore-test";
    private String targetInstanceName = "Matrix26 Restore Test";
    private String targetDatabaseName = "matrix26_restore_test";
    private String targetRuntimeProfile = "matrix26_restore_test";
    private int targetRuntimePort = 8095;
    private String targetPublicUrl = "http://localhost:8095";
    private String runtimeDirectory = "runtime-clients";
    private String runtimeDataDirectory = "runtime-data";
    private String importExecutable = "";
    private long processTimeoutSeconds = 1200L;
    private boolean startAfterRestoreDefault = true;
    private boolean verificationEnabled = true;
    private boolean resumeEnabled = true;
    private int verificationHttpTimeoutSeconds = 10;
    private List<String> verificationHttpPaths = new ArrayList<>(List.of("/", "/login", "/restaurant/menu", "/admin/restaurant/dashboard"));
    private boolean cleanupEnabled = true;
    private boolean inPlaceEnabled = true;
    private Set<String> inPlaceAllowedInstanceCodes = new LinkedHashSet<>(Set.of("matrix26-appearance-lab"));
    private String inPlaceStageDirectory = ".restore-stage";
    private String inPlaceRollbackDirectory = ".restore-rollback";
    private int inPlaceRollbackRetentionHours = 168;

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public Set<String> getAllowedSourceInstanceCodes() { return allowedSourceInstanceCodes; }
    public void setAllowedSourceInstanceCodes(Set<String> values) { allowedSourceInstanceCodes = values == null ? new LinkedHashSet<>() : new LinkedHashSet<>(values); }
    public String getTargetInstanceCode() { return targetInstanceCode; }
    public void setTargetInstanceCode(String value) { targetInstanceCode = value; }
    public String getTargetInstanceName() { return targetInstanceName; }
    public void setTargetInstanceName(String value) { targetInstanceName = value; }
    public String getTargetDatabaseName() { return targetDatabaseName; }
    public void setTargetDatabaseName(String value) { targetDatabaseName = value; }
    public String getTargetRuntimeProfile() { return targetRuntimeProfile; }
    public void setTargetRuntimeProfile(String value) { targetRuntimeProfile = value; }
    public int getTargetRuntimePort() { return targetRuntimePort; }
    public void setTargetRuntimePort(int value) { targetRuntimePort = value; }
    public String getTargetPublicUrl() { return targetPublicUrl; }
    public void setTargetPublicUrl(String value) { targetPublicUrl = value; }
    public String getRuntimeDirectory() { return runtimeDirectory; }
    public void setRuntimeDirectory(String value) { runtimeDirectory = value; }
    public String getRuntimeDataDirectory() { return runtimeDataDirectory; }
    public void setRuntimeDataDirectory(String value) { runtimeDataDirectory = value; }
    public String getImportExecutable() { return importExecutable; }
    public void setImportExecutable(String value) { importExecutable = value; }
    public long getProcessTimeoutSeconds() { return processTimeoutSeconds; }
    public void setProcessTimeoutSeconds(long value) { processTimeoutSeconds = value; }
    public boolean isStartAfterRestoreDefault() { return startAfterRestoreDefault; }
    public void setStartAfterRestoreDefault(boolean value) { startAfterRestoreDefault = value; }
    public boolean isVerificationEnabled() { return verificationEnabled; }
    public void setVerificationEnabled(boolean value) { verificationEnabled = value; }
    public boolean isResumeEnabled() { return resumeEnabled; }
    public void setResumeEnabled(boolean value) { resumeEnabled = value; }
    public int getVerificationHttpTimeoutSeconds() { return verificationHttpTimeoutSeconds; }
    public void setVerificationHttpTimeoutSeconds(int value) { verificationHttpTimeoutSeconds = value; }
    public List<String> getVerificationHttpPaths() { return verificationHttpPaths; }
    public void setVerificationHttpPaths(List<String> values) { verificationHttpPaths = values == null ? new ArrayList<>() : new ArrayList<>(values); }
    public boolean isCleanupEnabled() { return cleanupEnabled; }
    public void setCleanupEnabled(boolean value) { cleanupEnabled = value; }
    public boolean isInPlaceEnabled() { return inPlaceEnabled; }
    public void setInPlaceEnabled(boolean value) { inPlaceEnabled = value; }
    public Set<String> getInPlaceAllowedInstanceCodes() { return inPlaceAllowedInstanceCodes; }
    public void setInPlaceAllowedInstanceCodes(Set<String> values) { inPlaceAllowedInstanceCodes = values == null ? new LinkedHashSet<>() : new LinkedHashSet<>(values); }
    public String getInPlaceStageDirectory() { return inPlaceStageDirectory; }
    public void setInPlaceStageDirectory(String value) { inPlaceStageDirectory = value; }
    public String getInPlaceRollbackDirectory() { return inPlaceRollbackDirectory; }
    public void setInPlaceRollbackDirectory(String value) { inPlaceRollbackDirectory = value; }
    public int getInPlaceRollbackRetentionHours() { return inPlaceRollbackRetentionHours; }
    public void setInPlaceRollbackRetentionHours(int value) { inPlaceRollbackRetentionHours = value; }
}
