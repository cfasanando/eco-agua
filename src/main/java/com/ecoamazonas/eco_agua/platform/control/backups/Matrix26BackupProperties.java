package com.ecoamazonas.eco_agua.platform.control.backups;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.LinkedHashSet;
import java.util.Set;

@Component
@ConfigurationProperties(prefix = "matrix26.control-center.backups")
public class Matrix26BackupProperties {

    private boolean enabled = true;
    private String rootDirectory = "";
    private String runtimeDirectory = "runtime-clients";
    private String runtimeDataDirectory = "runtime-data";
    private String dumpExecutable = "";
    private long minimumFreeBytes = 256L * 1024L * 1024L;
    private long processTimeoutSeconds = 900L;
    private long maximumSingleFileBytes = 512L * 1024L * 1024L;
    private long maximumArchiveSourceBytes = 2L * 1024L * 1024L * 1024L;
    private int diagnosticLogTailLines = 400;
    private boolean encryptionEnabled = true;
    private String masterKeyEnvironment = "MATRIX26_BACKUP_MASTER_KEY";
    private boolean retentionEnabled = true;
    private boolean schedulingEnabled = true;
    private long schedulerPollMilliseconds = 60000L;
    private long schedulerInitialDelayMilliseconds = 15000L;
    private int schedulerGraceMinutes = 2;
    private String scheduleTimezone = "America/Lima";
    private Set<String> allowedInstanceCodes = new LinkedHashSet<>(Set.of("matrix26-appearance-lab"));

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getRootDirectory() {
        return rootDirectory;
    }

    public void setRootDirectory(String rootDirectory) {
        this.rootDirectory = rootDirectory;
    }

    public String getRuntimeDirectory() {
        return runtimeDirectory;
    }

    public void setRuntimeDirectory(String runtimeDirectory) {
        this.runtimeDirectory = runtimeDirectory;
    }


    public String getRuntimeDataDirectory() {
        return runtimeDataDirectory;
    }

    public void setRuntimeDataDirectory(String runtimeDataDirectory) {
        this.runtimeDataDirectory = runtimeDataDirectory;
    }

    public String getDumpExecutable() {
        return dumpExecutable;
    }

    public void setDumpExecutable(String dumpExecutable) {
        this.dumpExecutable = dumpExecutable;
    }

    public long getMinimumFreeBytes() {
        return minimumFreeBytes;
    }

    public void setMinimumFreeBytes(long minimumFreeBytes) {
        this.minimumFreeBytes = minimumFreeBytes;
    }

    public long getProcessTimeoutSeconds() {
        return processTimeoutSeconds;
    }

    public void setProcessTimeoutSeconds(long processTimeoutSeconds) {
        this.processTimeoutSeconds = processTimeoutSeconds;
    }


    public long getMaximumSingleFileBytes() {
        return maximumSingleFileBytes;
    }

    public void setMaximumSingleFileBytes(long maximumSingleFileBytes) {
        this.maximumSingleFileBytes = maximumSingleFileBytes;
    }

    public long getMaximumArchiveSourceBytes() {
        return maximumArchiveSourceBytes;
    }

    public void setMaximumArchiveSourceBytes(long maximumArchiveSourceBytes) {
        this.maximumArchiveSourceBytes = maximumArchiveSourceBytes;
    }

    public int getDiagnosticLogTailLines() {
        return diagnosticLogTailLines;
    }

    public void setDiagnosticLogTailLines(int diagnosticLogTailLines) {
        this.diagnosticLogTailLines = diagnosticLogTailLines;
    }


    public boolean isEncryptionEnabled() {
        return encryptionEnabled;
    }

    public void setEncryptionEnabled(boolean encryptionEnabled) {
        this.encryptionEnabled = encryptionEnabled;
    }

    public String getMasterKeyEnvironment() {
        return masterKeyEnvironment;
    }

    public void setMasterKeyEnvironment(String masterKeyEnvironment) {
        this.masterKeyEnvironment = masterKeyEnvironment;
    }

    public boolean isRetentionEnabled() {
        return retentionEnabled;
    }

    public void setRetentionEnabled(boolean retentionEnabled) {
        this.retentionEnabled = retentionEnabled;
    }



    public boolean isSchedulingEnabled() {
        return schedulingEnabled;
    }

    public void setSchedulingEnabled(boolean schedulingEnabled) {
        this.schedulingEnabled = schedulingEnabled;
    }

    public long getSchedulerPollMilliseconds() {
        return schedulerPollMilliseconds;
    }

    public void setSchedulerPollMilliseconds(long schedulerPollMilliseconds) {
        this.schedulerPollMilliseconds = schedulerPollMilliseconds;
    }

    public long getSchedulerInitialDelayMilliseconds() {
        return schedulerInitialDelayMilliseconds;
    }

    public void setSchedulerInitialDelayMilliseconds(long schedulerInitialDelayMilliseconds) {
        this.schedulerInitialDelayMilliseconds = schedulerInitialDelayMilliseconds;
    }

    public int getSchedulerGraceMinutes() {
        return schedulerGraceMinutes;
    }

    public void setSchedulerGraceMinutes(int schedulerGraceMinutes) {
        this.schedulerGraceMinutes = schedulerGraceMinutes;
    }

    public String getScheduleTimezone() {
        return scheduleTimezone;
    }

    public void setScheduleTimezone(String scheduleTimezone) {
        this.scheduleTimezone = scheduleTimezone;
    }

    public Set<String> getAllowedInstanceCodes() {
        return allowedInstanceCodes;
    }

    public void setAllowedInstanceCodes(Set<String> allowedInstanceCodes) {
        this.allowedInstanceCodes = allowedInstanceCodes == null
                ? new LinkedHashSet<>()
                : new LinkedHashSet<>(allowedInstanceCodes);
    }
}
