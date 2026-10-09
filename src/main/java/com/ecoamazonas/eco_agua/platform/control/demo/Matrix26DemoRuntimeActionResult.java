package com.ecoamazonas.eco_agua.platform.control.demo;

public record Matrix26DemoRuntimeActionResult(
        boolean success,
        String message
) {
    public static Matrix26DemoRuntimeActionResult success(String message) {
        return new Matrix26DemoRuntimeActionResult(true, message);
    }

    public static Matrix26DemoRuntimeActionResult failure(String message) {
        return new Matrix26DemoRuntimeActionResult(false, message);
    }
}
