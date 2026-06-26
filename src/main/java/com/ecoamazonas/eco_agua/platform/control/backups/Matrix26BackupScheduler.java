package com.ecoamazonas.eco_agua.platform.control.backups;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.event.EventListener;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(
        name = "matrix26.control-center.backups.scheduling-enabled",
        havingValue = "true",
        matchIfMissing = true
)
public class Matrix26BackupScheduler {

    private final Matrix26BackupScheduleService scheduleService;

    public Matrix26BackupScheduler(Matrix26BackupScheduleService scheduleService) {
        this.scheduleService = scheduleService;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void recoverMissedExecutions() {
        scheduleService.recoverOnStartup();
    }

    @Scheduled(
            fixedDelayString = "${matrix26.control-center.backups.scheduler-poll-milliseconds:60000}",
            initialDelayString = "${matrix26.control-center.backups.scheduler-initial-delay-milliseconds:15000}"
    )
    public void evaluateSchedules() {
        scheduleService.processSchedulerTick();
    }
}
