package com.ecoamazonas.eco_agua.platform.control.lifecycle;

import java.time.LocalDateTime;

public record Matrix26LifecycleBackupView(
        Long jobId,
        String publicId,
        LocalDateTime completedAt,
        LocalDateTime verifiedAt
) {
    public boolean available() {
        return jobId != null;
    }
}
