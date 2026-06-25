package com.ecoamazonas.eco_agua.platform.control.operations;

public record Matrix26HttpProbe(
        boolean attempted,
        boolean online,
        Integer statusCode,
        Long responseTimeMs,
        String message
) {
}
