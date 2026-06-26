package com.ecoamazonas.eco_agua.platform.control.backups;

import java.util.List;

public record Matrix26RetentionPreview(
        Long instanceId,
        String instanceCode,
        String instanceName,
        Matrix26BackupPolicy policy,
        List<Matrix26RetentionItem> items,
        long reclaimableBytes
) {
    public long deletableCount() {
        return items.stream().filter(Matrix26RetentionItem::deletable).count();
    }

    public String reclaimableLabel() {
        return Matrix26BackupService.formatBytes(reclaimableBytes);
    }
}
