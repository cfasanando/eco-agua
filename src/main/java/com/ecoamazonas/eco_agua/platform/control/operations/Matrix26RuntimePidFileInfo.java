package com.ecoamazonas.eco_agua.platform.control.operations;

public record Matrix26RuntimePidFileInfo(
        boolean present,
        boolean readable,
        Long pid,
        String instanceCode,
        Integer port,
        boolean processAlive,
        boolean ownedByRuntime,
        String message
) {
    public static Matrix26RuntimePidFileInfo missing() {
        return new Matrix26RuntimePidFileInfo(
                false,
                true,
                null,
                "",
                null,
                false,
                false,
                "No PID file exists."
        );
    }
}
