package com.ecoamazonas.eco_agua.platform.control.purge;

import java.time.LocalDateTime;

public record Matrix26ArchiveDestructionItem(
        Long id,
        Long destructionPlanId,
        Integer runNumber,
        String resourceType,
        String resourceName,
        String resourcePath,
        Matrix26PurgeDisposition disposition,
        Long sizeBytes,
        Integer fileCount,
        String detail,
        LocalDateTime createdAt,
        String executionStatus,
        LocalDateTime executedAt,
        String executionDetail
) {
    public String sizeLabel() {
        if (sizeBytes == null || sizeBytes < 0) {
            return "—";
        }
        double value = sizeBytes;
        String[] units = {"B", "KB", "MB", "GB", "TB"};
        int unit = 0;
        while (value >= 1024 && unit < units.length - 1) {
            value = value / 1024;
            unit++;
        }
        if (unit == 0) {
            return String.format("%d %s", sizeBytes, units[unit]);
        }
        return String.format("%.2f %s", value, units[unit]);
    }

    public String executionStatusLabel() {
        return executionStatus == null || executionStatus.isBlank() ? "—" : executionStatus;
    }

    public String executionDetailLabel() {
        return executionDetail == null || executionDetail.isBlank() ? "—" : executionDetail;
    }
}
