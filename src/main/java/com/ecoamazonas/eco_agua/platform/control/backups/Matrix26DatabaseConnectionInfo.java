package com.ecoamazonas.eco_agua.platform.control.backups;

public record Matrix26DatabaseConnectionInfo(
        String host,
        int port,
        String databaseName,
        String username,
        String password,
        String jdbcUrl
) {
}
