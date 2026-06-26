package com.ecoamazonas.eco_agua.platform.control.restores;

import java.time.LocalDateTime;

public record Matrix26InPlaceRestoreCheck(
        Long id, Long jobId, String checkCode, String label, String status,
        String expectedValue, String actualValue, String detail, LocalDateTime checkedAt
) { }
