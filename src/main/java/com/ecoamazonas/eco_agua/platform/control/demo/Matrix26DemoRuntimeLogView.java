package com.ecoamazonas.eco_agua.platform.control.demo;

import java.util.List;

public record Matrix26DemoRuntimeLogView(
        Matrix26DemoPortalView portal,
        String logPath,
        boolean available,
        String message,
        List<String> lines
) {
}
