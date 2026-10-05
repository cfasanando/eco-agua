package com.ecoamazonas.eco_agua.platform.control.demo;

import java.util.List;

public record Matrix26DemoCenterDashboard(
        Matrix26DemoCenterSummary summary,
        List<Matrix26DemoPortalView> portals,
        List<String> warnings
) {
}
