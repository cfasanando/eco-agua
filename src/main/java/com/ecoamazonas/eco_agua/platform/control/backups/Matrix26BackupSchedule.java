package com.ecoamazonas.eco_agua.platform.control.backups;

import java.time.LocalDateTime;

public record Matrix26BackupSchedule(
        Long id,
        Long instanceId,
        String instanceCode,
        String instanceName,
        String name,
        Matrix26BackupScheduleFrequency frequency,
        Integer dayOfWeek,
        Integer dayOfMonth,
        Integer hourOfDay,
        Integer minuteOfHour,
        String timezone,
        boolean encryptionRequired,
        Matrix26BackupRetentionClass retentionClass,
        Integer maxAttempts,
        Integer retryDelayMinutes,
        Matrix26BackupMissedPolicy missedPolicy,
        boolean enabled,
        LocalDateTime nextRunAt,
        LocalDateTime lastRunAt,
        String lastStatus,
        String createdBy,
        LocalDateTime createdAt,
        String updatedBy,
        LocalDateTime updatedAt
) {
    public String scheduleLabel() {
        String time = String.format("%02d:%02d", hourOfDay, minuteOfHour);
        return switch (frequency) {
            case DAILY -> "Every day at " + time;
            case WEEKLY -> "Every " + weekdayName(dayOfWeek) + " at " + time;
            case MONTHLY -> "Day " + dayOfMonth + " of each month at " + time;
        };
    }

    private String weekdayName(Integer value) {
        if (value == null || value < 1 || value > 7) {
            return "day";
        }
        return java.time.DayOfWeek.of(value).getDisplayName(
                java.time.format.TextStyle.FULL,
                java.util.Locale.ENGLISH
        );
    }
}
