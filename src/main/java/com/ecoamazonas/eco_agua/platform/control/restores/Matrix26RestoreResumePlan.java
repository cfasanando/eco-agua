package com.ecoamazonas.eco_agua.platform.control.restores;

import java.util.List;

public record Matrix26RestoreResumePlan(
        boolean resumable,
        String nextStepCode,
        String nextStepLabel,
        List<String> validatedCompletedSteps,
        List<String> blockers,
        String expectedConfirmation,
        String summary
) {
}
