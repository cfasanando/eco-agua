package com.ecoamazonas.eco_agua.platform.control.demo;

public record Matrix26DemoRuntimeStatus(
        boolean applicable,
        boolean configured,
        boolean portListening,
        boolean httpReachable,
        boolean trackedProcess,
        boolean trackedProcessAlive,
        boolean canStart,
        boolean canStop,
        boolean canRestart,
        String statusLabel,
        String badgeClass,
        String detail,
        String logRelativePath,
        String pidText
) {
    public static Matrix26DemoRuntimeStatus notApplicable() {
        return new Matrix26DemoRuntimeStatus(
                false,
                false,
                false,
                false,
                false,
                false,
                false,
                false,
                false,
                "No aplica",
                "text-bg-secondary",
                "Este portal no usa runtime separado.",
                "",
                ""
        );
    }
}
