package com.ecoamazonas.eco_agua.platform.control.operations;

public record Matrix26PortBinding(
        int port,
        String localAddress,
        Long pid,
        String processName,
        String commandLine
) {
}
