package com.ecoamazonas.eco_agua.platform.control.backups;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;

@Controller
@RequestMapping("/control-center/backups")
@ConditionalOnProperty(name = "matrix26.control-center.enabled", havingValue = "true")
public class Matrix26BackupScheduleController {

    private final Matrix26BackupScheduleService scheduleService;
    private final Matrix26BackupService backupService;
    private final Matrix26BackupSecurityService backupSecurityService;
    private final Matrix26BackupProperties properties;

    public Matrix26BackupScheduleController(
            Matrix26BackupScheduleService scheduleService,
            Matrix26BackupService backupService,
            Matrix26BackupSecurityService backupSecurityService,
            Matrix26BackupProperties properties
    ) {
        this.scheduleService = scheduleService;
        this.backupService = backupService;
        this.backupSecurityService = backupSecurityService;
        this.properties = properties;
    }

    @GetMapping("/schedules")
    public String schedules(Model model) {
        model.addAttribute("activePage", "matrix26_backup_schedules");
        model.addAttribute("schedules", scheduleService.schedules());
        model.addAttribute("summary", scheduleService.summary());
        model.addAttribute("keyStatus", backupSecurityService.keyStatus());
        model.addAttribute("tool", backupService.toolStatus());
        model.addAttribute("timezone", properties.getScheduleTimezone());
        return "control_center/backups/schedules";
    }

    @GetMapping("/schedules/new")
    public String newSchedule(
            @RequestParam(value = "instanceId", required = false) Long instanceId,
            Model model
    ) {
        populateForm(model, null, instanceId);
        return "control_center/backups/schedule_form";
    }

    @GetMapping("/schedules/{scheduleId}")
    public String scheduleDetail(@PathVariable long scheduleId, Model model) {
        Matrix26BackupSchedule schedule = scheduleService.schedule(scheduleId);
        model.addAttribute("activePage", "matrix26_backup_schedules");
        model.addAttribute("schedule", schedule);
        model.addAttribute("executions", scheduleService.executionsForSchedule(scheduleId));
        model.addAttribute("keyStatus", backupSecurityService.keyStatus());
        model.addAttribute("tool", backupService.toolStatus());
        return "control_center/backups/schedule_detail";
    }

    @GetMapping("/schedules/{scheduleId}/edit")
    public String editSchedule(@PathVariable long scheduleId, Model model) {
        Matrix26BackupSchedule schedule = scheduleService.schedule(scheduleId);
        populateForm(model, schedule, schedule.instanceId());
        return "control_center/backups/schedule_form";
    }

    @PostMapping("/schedules")
    public String saveSchedule(
            @RequestParam(value = "scheduleId", required = false) Long scheduleId,
            @RequestParam("instanceId") long instanceId,
            @RequestParam("name") String name,
            @RequestParam("frequency") String frequency,
            @RequestParam(value = "dayOfWeek", required = false) Integer dayOfWeek,
            @RequestParam(value = "dayOfMonth", required = false) Integer dayOfMonth,
            @RequestParam("hourOfDay") int hourOfDay,
            @RequestParam("minuteOfHour") int minuteOfHour,
            @RequestParam(value = "timezone", defaultValue = "America/Lima") String timezone,
            @RequestParam(value = "encryptionRequired", defaultValue = "false") boolean encryptionRequired,
            @RequestParam(value = "retentionClass", defaultValue = "DAILY") String retentionClass,
            @RequestParam(value = "maxAttempts", defaultValue = "3") int maxAttempts,
            @RequestParam(value = "retryDelayMinutes", defaultValue = "15") int retryDelayMinutes,
            @RequestParam(value = "missedPolicy", defaultValue = "RUN_ON_STARTUP") String missedPolicy,
            @RequestParam(value = "enabled", defaultValue = "false") boolean enabled,
            Principal principal,
            RedirectAttributes redirectAttributes
    ) {
        try {
            Matrix26BackupSchedule saved = scheduleService.saveSchedule(
                    scheduleId,
                    instanceId,
                    name,
                    frequency,
                    dayOfWeek,
                    dayOfMonth,
                    hourOfDay,
                    minuteOfHour,
                    timezone,
                    encryptionRequired,
                    retentionClass,
                    maxAttempts,
                    retryDelayMinutes,
                    missedPolicy,
                    enabled,
                    actor(principal)
            );
            redirectAttributes.addFlashAttribute("backupSuccess", "Backup schedule saved.");
            return "redirect:/control-center/backups/schedules/" + saved.id();
        } catch (Matrix26BackupException ex) {
            redirectAttributes.addFlashAttribute("backupError", ex.getMessage());
            String suffix = scheduleId == null ? "/new?instanceId=" + instanceId : "/" + scheduleId + "/edit";
            return "redirect:/control-center/backups/schedules" + suffix;
        }
    }

    @PostMapping("/schedules/{scheduleId}/toggle")
    public String toggleSchedule(
            @PathVariable long scheduleId,
            @RequestParam("enabled") boolean enabled,
            Principal principal,
            RedirectAttributes redirectAttributes
    ) {
        try {
            scheduleService.setEnabled(scheduleId, enabled, actor(principal));
            redirectAttributes.addFlashAttribute(
                    "backupSuccess",
                    enabled ? "Backup schedule enabled." : "Backup schedule disabled."
            );
        } catch (Matrix26BackupException ex) {
            redirectAttributes.addFlashAttribute("backupError", ex.getMessage());
        }
        return "redirect:/control-center/backups/schedules/" + scheduleId;
    }

    @PostMapping("/schedules/{scheduleId}/run")
    public String runScheduleNow(
            @PathVariable long scheduleId,
            @RequestParam("confirmation") String confirmation,
            Principal principal,
            RedirectAttributes redirectAttributes
    ) {
        Matrix26BackupSchedule schedule = scheduleService.schedule(scheduleId);
        String expected = "RUN BACKUP " + schedule.instanceCode();
        if (!expected.equals(confirmation)) {
            redirectAttributes.addFlashAttribute("backupError", "Type exactly: " + expected);
            return "redirect:/control-center/backups/schedules/" + scheduleId;
        }
        try {
            Matrix26BackupScheduleExecution execution = scheduleService.runNow(scheduleId, actor(principal));
            redirectAttributes.addFlashAttribute(
                    execution.status() == Matrix26BackupScheduleExecutionStatus.COMPLETED ? "backupSuccess" : "backupError",
                    "Execution finished with status " + execution.status().getLabel() + "."
            );
        } catch (Matrix26BackupException ex) {
            redirectAttributes.addFlashAttribute("backupError", ex.getMessage());
        }
        return "redirect:/control-center/backups/schedules/" + scheduleId;
    }

    @GetMapping("/calendar")
    public String calendar(
            @RequestParam(value = "days", defaultValue = "31") int days,
            Model model
    ) {
        model.addAttribute("activePage", "matrix26_backup_calendar");
        model.addAttribute("days", Math.max(7, Math.min(days, 90)));
        model.addAttribute("events", scheduleService.calendar(days));
        model.addAttribute("timezone", properties.getScheduleTimezone());
        return "control_center/backups/calendar";
    }

    @GetMapping("/executions")
    public String executions(Model model) {
        model.addAttribute("activePage", "matrix26_backup_executions");
        model.addAttribute("executions", scheduleService.recentExecutions());
        return "control_center/backups/executions";
    }

    @GetMapping("/alerts")
    public String alerts(
            @RequestParam(value = "all", defaultValue = "false") boolean all,
            Model model
    ) {
        model.addAttribute("activePage", "matrix26_backup_alerts");
        model.addAttribute("all", all);
        model.addAttribute("alerts", scheduleService.alerts(!all));
        return "control_center/backups/alerts";
    }

    @PostMapping("/alerts/{alertId}/resolve")
    public String resolveAlert(
            @PathVariable long alertId,
            Principal principal,
            RedirectAttributes redirectAttributes
    ) {
        scheduleService.resolveAlert(alertId, actor(principal));
        redirectAttributes.addFlashAttribute("backupSuccess", "Backup alert resolved.");
        return "redirect:/control-center/backups/alerts";
    }

    private void populateForm(Model model, Matrix26BackupSchedule schedule, Long instanceId) {
        model.addAttribute("activePage", "matrix26_backup_schedules");
        model.addAttribute("schedule", schedule);
        model.addAttribute("selectedInstanceId", instanceId);
        model.addAttribute("candidates", backupService.candidates());
        model.addAttribute("frequencies", Matrix26BackupScheduleFrequency.values());
        model.addAttribute("retentionClasses", Matrix26BackupRetentionClass.values());
        model.addAttribute("missedPolicies", Matrix26BackupMissedPolicy.values());
        model.addAttribute("defaultTimezone", properties.getScheduleTimezone());
    }

    private String actor(Principal principal) {
        return principal == null || principal.getName() == null || principal.getName().isBlank()
                ? "matrix26-admin"
                : principal.getName();
    }
}
