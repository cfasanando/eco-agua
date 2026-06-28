package com.ecoamazonas.eco_agua.platform.control.lifecycle.decommission;

import java.time.LocalDateTime;

public record Matrix26DecommissionCheck(
        Long id,
        Long decommissionJobId,
        String checkCode,
        String label,
        String status,
        String detail,
        LocalDateTime checkedAt
) {
}
