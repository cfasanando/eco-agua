package com.ecoamazonas.eco_agua.platform.control;

import java.util.List;

public record Matrix26ProvisioningPlanView(
        Matrix26ProvisioningJob job,
        List<Matrix26ProvisioningStep> steps,
        List<Matrix26ProvisioningModule> modules
) {
}
