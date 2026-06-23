package com.ecoamazonas.eco_agua.platform.module;

import java.util.Objects;

public record PlatformModuleInstallStep(
        String code,
        String label,
        Runnable action
) {
    public PlatformModuleInstallStep {
        code = requireText(code, "Step code is required.");
        label = requireText(label, "Step label is required.");
        action = Objects.requireNonNull(action, "Step action is required.");
    }

    public void execute() {
        action.run();
    }

    private static String requireText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }
        return value.trim();
    }
}
