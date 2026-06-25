package com.ecoamazonas.eco_agua.platform.control.operations;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public record Matrix26SystemSnapshot(
        LocalDateTime capturedAt,
        Map<Long, Matrix26ProcessInfo> processes,
        Map<Integer, Matrix26PortBinding> listeningPorts,
        List<String> warnings
) {
}
