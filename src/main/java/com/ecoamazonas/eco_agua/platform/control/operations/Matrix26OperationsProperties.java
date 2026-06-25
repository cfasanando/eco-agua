package com.ecoamazonas.eco_agua.platform.control.operations;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

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
}
