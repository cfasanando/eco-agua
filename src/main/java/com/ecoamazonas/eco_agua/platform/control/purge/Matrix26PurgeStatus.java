package com.ecoamazonas.eco_agua.platform.control.purge;

public enum Matrix26PurgeStatus {
    DRAFT,
    DRY_RUN_RUNNING,
    DRY_RUN_READY,
    READY_TO_PURGE,
    PURGING,
    PARTIALLY_PURGED,
    PURGED,
    BLOCKED,
    FAILED,
    MANUAL_REVIEW_REQUIRED
}
