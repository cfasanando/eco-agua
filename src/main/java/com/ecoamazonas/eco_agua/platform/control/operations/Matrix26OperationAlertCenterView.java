package com.ecoamazonas.eco_agua.platform.control.operations;

import java.time.LocalDateTime;
import java.util.List;

public record Matrix26OperationAlertCenterView(
        Matrix26OperationAlertSummary summary,
        List<Matrix26OperationAlert> alerts,
        LocalDateTime synchronizedAt,
        boolean partialSync,
        String syncMessage
) {
}
