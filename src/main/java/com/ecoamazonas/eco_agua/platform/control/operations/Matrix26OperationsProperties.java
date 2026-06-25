package com.ecoamazonas.eco_agua.platform.control.operations;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.LinkedHashSet;
import java.util.Set;

@Component
@ConfigurationProperties(prefix = "matrix26.control-center.operations")
public class Matrix26OperationsProperties {

    private String runtimeDirectory = "runtime-clients";
    private String dataDirectory = "runtime-data";
    private String logDirectory = "logs";
    private long cacheSeconds = 5;
    private int connectTimeoutMs = 700;
    private int readTimeoutMs = 700;
    private int logTailLines = 120;
    private boolean runtimeControlEnabled = true;
    private Set<String> allowedInstanceCodes = new LinkedHashSet<>(Set.of(
            "matrix26-restaurant-lab",
            "matrix26-appearance-lab"
    ));
    private int startTimeoutSeconds = 45;
    private int stopTimeoutSeconds = 25;
    private int pollIntervalMs = 1000;
    private String operationDirectory = "operations";
    private long logRotationMaxBytes = 10L * 1024L * 1024L;
    private int logRotationCopies = 5;
    private int forceStopTimeoutSeconds = 10;

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

    public String getLogDirectory() {
        return logDirectory;
    }

    public void setLogDirectory(String logDirectory) {
        this.logDirectory = logDirectory;
    }

    public long getCacheSeconds() {
        return cacheSeconds;
    }

    public void setCacheSeconds(long cacheSeconds) {
        this.cacheSeconds = cacheSeconds;
    }

    public int getConnectTimeoutMs() {
        return connectTimeoutMs;
    }

    public void setConnectTimeoutMs(int connectTimeoutMs) {
        this.connectTimeoutMs = connectTimeoutMs;
    }

    public int getReadTimeoutMs() {
        return readTimeoutMs;
    }

    public void setReadTimeoutMs(int readTimeoutMs) {
        this.readTimeoutMs = readTimeoutMs;
    }

    public int getLogTailLines() {
        return logTailLines;
    }

    public void setLogTailLines(int logTailLines) {
        this.logTailLines = logTailLines;
    }

    public boolean isRuntimeControlEnabled() {
        return runtimeControlEnabled;
    }

    public void setRuntimeControlEnabled(boolean runtimeControlEnabled) {
        this.runtimeControlEnabled = runtimeControlEnabled;
    }

    public Set<String> getAllowedInstanceCodes() {
        return allowedInstanceCodes;
    }

    public void setAllowedInstanceCodes(Set<String> allowedInstanceCodes) {
        this.allowedInstanceCodes = allowedInstanceCodes == null
                ? new LinkedHashSet<>()
                : new LinkedHashSet<>(allowedInstanceCodes);
    }

    public int getStartTimeoutSeconds() {
        return startTimeoutSeconds;
    }

    public void setStartTimeoutSeconds(int startTimeoutSeconds) {
        this.startTimeoutSeconds = startTimeoutSeconds;
    }

    public int getStopTimeoutSeconds() {
        return stopTimeoutSeconds;
    }

    public void setStopTimeoutSeconds(int stopTimeoutSeconds) {
        this.stopTimeoutSeconds = stopTimeoutSeconds;
    }

    public int getPollIntervalMs() {
        return pollIntervalMs;
    }

    public void setPollIntervalMs(int pollIntervalMs) {
        this.pollIntervalMs = pollIntervalMs;
    }

    public String getOperationDirectory() {
        return operationDirectory;
    }

    public void setOperationDirectory(String operationDirectory) {
        this.operationDirectory = operationDirectory;
    }

    public long getLogRotationMaxBytes() {
        return logRotationMaxBytes;
    }

    public void setLogRotationMaxBytes(long logRotationMaxBytes) {
        this.logRotationMaxBytes = logRotationMaxBytes;
    }

    public int getLogRotationCopies() {
        return logRotationCopies;
    }

    public void setLogRotationCopies(int logRotationCopies) {
        this.logRotationCopies = logRotationCopies;
    }

    public int getForceStopTimeoutSeconds() {
        return forceStopTimeoutSeconds;
    }

    public void setForceStopTimeoutSeconds(int forceStopTimeoutSeconds) {
        this.forceStopTimeoutSeconds = forceStopTimeoutSeconds;
    }

}
