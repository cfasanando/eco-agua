package com.ecoamazonas.eco_agua.platform.control.purge;

import java.time.LocalDateTime;

public record Matrix26ArchiveDestructionCheck(
        Long id,
        Long destructionPlanId,
        Integer runNumber,
        String checkCode,
        String label,
        String status,
        String detail,
        LocalDateTime checkedAt
) {}
