package com.ecoamazonas.eco_agua.platform.control.purge;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.LinkedHashSet;
import java.util.Set;

@Component
@ConfigurationProperties(prefix = "matrix26.control-center.purge")
public class Matrix26PurgeProperties {
    private boolean enabled = true;
    private boolean executionEnabled = true;
    private boolean archiveDestructionEnabled = true;
    private boolean archiveDestructionExecutionEnabled = false;
    private boolean archiveDestructionRequireRetentionExpired = true;
    private Set<String> allowedInstanceCodes = new LinkedHashSet<>(Set.of("matrix26-appearance-lab"));
    private Set<String> protectedInstanceCodes = new LinkedHashSet<>(Set.of(
            "eco-agua",
            "productos-selva-belen",
            "restaurante-buen-sabor",
            "matrix26-control-center",
            "matrix26-archived-restore-test"
    ));
    private int minimumReasonLength = 10;
    private boolean requireRetentionExpired = false;
    private String runtimeDirectory = "runtime-clients";
    private String dataDirectory = "runtime-data";
    private String backupRootDirectory = "C:/Users/PC/Matrix26/backups";

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isExecutionEnabled() {
        return executionEnabled;
    }

    public void setExecutionEnabled(boolean executionEnabled) {
        this.executionEnabled = executionEnabled;
    }

    public boolean isArchiveDestructionEnabled() {
        return archiveDestructionEnabled;
    }

    public void setArchiveDestructionEnabled(boolean archiveDestructionEnabled) {
        this.archiveDestructionEnabled = archiveDestructionEnabled;
    }

    public boolean isArchiveDestructionExecutionEnabled() {
        return archiveDestructionExecutionEnabled;
    }

    public void setArchiveDestructionExecutionEnabled(boolean archiveDestructionExecutionEnabled) {
        this.archiveDestructionExecutionEnabled = archiveDestructionExecutionEnabled;
    }

    public boolean isArchiveDestructionRequireRetentionExpired() {
        return archiveDestructionRequireRetentionExpired;
    }

    public void setArchiveDestructionRequireRetentionExpired(boolean archiveDestructionRequireRetentionExpired) {
        this.archiveDestructionRequireRetentionExpired = archiveDestructionRequireRetentionExpired;
    }

    public Set<String> getAllowedInstanceCodes() {
        return allowedInstanceCodes;
    }

    public void setAllowedInstanceCodes(Set<String> allowedInstanceCodes) {
        this.allowedInstanceCodes = allowedInstanceCodes == null
                ? new LinkedHashSet<>()
                : new LinkedHashSet<>(allowedInstanceCodes);
    }

    public Set<String> getProtectedInstanceCodes() {
        return protectedInstanceCodes;
    }

    public void setProtectedInstanceCodes(Set<String> protectedInstanceCodes) {
        this.protectedInstanceCodes = protectedInstanceCodes == null
                ? new LinkedHashSet<>()
                : new LinkedHashSet<>(protectedInstanceCodes);
    }

    public int getMinimumReasonLength() {
        return minimumReasonLength;
    }

    public void setMinimumReasonLength(int minimumReasonLength) {
        this.minimumReasonLength = minimumReasonLength;
    }

    public boolean isRequireRetentionExpired() {
        return requireRetentionExpired;
    }

    public void setRequireRetentionExpired(boolean requireRetentionExpired) {
        this.requireRetentionExpired = requireRetentionExpired;
    }

    public String getRuntimeDirectory() {
        return runtimeDirectory;
    }

    public void setRuntimeDirectory(String runtimeDirectory) {
        this.runtimeDirectory = runtimeDirectory;
    }

    public String getDataDirectory() {
        return dataDirectory;
    }

    public void setDataDirectory(String dataDirectory) {
        this.dataDirectory = dataDirectory;
    }

    public String getBackupRootDirectory() {
        return backupRootDirectory;
    }

    public void setBackupRootDirectory(String backupRootDirectory) {
        this.backupRootDirectory = backupRootDirectory;
    }
}
