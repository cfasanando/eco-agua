package com.ecoamazonas.eco_agua.platform.control.backups;

import com.ecoamazonas.eco_agua.platform.PlatformBusinessClient;
import com.ecoamazonas.eco_agua.platform.PlatformBusinessClientRepository;
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
@RequestMapping("/control-center")
@ConditionalOnProperty(name = "matrix26.control-center.enabled", havingValue = "true")
public class Matrix26BackupController {

    private final Matrix26BackupService backupService;
    private final Matrix26BackupSecurityService backupSecurityService;
    private final PlatformBusinessClientRepository clientRepository;

    public Matrix26BackupController(
            Matrix26BackupService backupService,
            Matrix26BackupSecurityService backupSecurityService,
            PlatformBusinessClientRepository clientRepository
    ) {
        this.backupService = backupService;
        this.backupSecurityService = backupSecurityService;
        this.clientRepository = clientRepository;
    }

    @GetMapping("/backups")
    public String index(Model model) {
        model.addAttribute("activePage", "matrix26_backups");
        model.addAttribute("summary", backupService.summary());
        model.addAttribute("jobs", backupService.recentJobs());
        model.addAttribute("tool", backupService.toolStatus());
        model.addAttribute("backupRoot", backupService.backupRoot().toString());
        model.addAttribute("keyStatus", backupSecurityService.keyStatus());
        return "control_center/backups/index";
    }

    @GetMapping("/backups/new")
    public String newBackup(
            @RequestParam(value = "instanceId", required = false) Long instanceId,
            Model model
    ) {
        model.addAttribute("activePage", "matrix26_backups");
        model.addAttribute("candidates", backupService.candidates());
        model.addAttribute("selectedInstanceId", instanceId);
        model.addAttribute("tool", backupService.toolStatus());
        model.addAttribute("backupRoot", backupService.backupRoot().toString());
        model.addAttribute("keyStatus", backupSecurityService.keyStatus());
        model.addAttribute("retentionClasses", Matrix26BackupRetentionClass.values());
        return "control_center/backups/new";
    }

    @PostMapping("/backups")
    public String createBackup(
            @RequestParam("instanceId") long instanceId,
            @RequestParam(value = "backupScope", defaultValue = "FULL") String backupScope,
            @RequestParam(value = "encryptPackage", defaultValue = "false") boolean encryptPackage,
            @RequestParam(value = "retentionClass", defaultValue = "DAILY") String retentionClass,
            @RequestParam(value = "confirmation", defaultValue = "false") boolean confirmation,
            Principal principal,
            RedirectAttributes redirectAttributes
    ) {
        try {
            boolean fullBackup = "FULL".equalsIgnoreCase(backupScope);
            if (encryptPackage && !fullBackup) {
                throw new Matrix26BackupException("Encryption is available only for full instance backups in Phase 3E.3.");
            }
            String actor = actor(principal);
            Matrix26BackupJob job = fullBackup
                    ? backupService.createManualFullBackup(instanceId, actor, confirmation)
                    : backupService.createManualDatabaseBackup(instanceId, actor, confirmation);
            if (encryptPackage) {
                job = backupSecurityService.encryptBackup(
                        job.id(),
                        Matrix26BackupRetentionClass.from(retentionClass),
                        actor
                );
            }
            redirectAttributes.addFlashAttribute(
                    "backupSuccess",
                    (encryptPackage ? "Encrypted full instance backup" : fullBackup ? "Full instance backup" : "Database backup")
                            + " completed and verified: " + job.publicId()
            );
            return "redirect:/control-center/backups/" + job.id();
        } catch (Matrix26BackupException ex) {
            redirectAttributes.addFlashAttribute("backupError", ex.getMessage());
            return "redirect:/control-center/backups/new?instanceId=" + instanceId;
        }
    }

    @GetMapping("/backups/{backupId}")
    public String detail(@PathVariable long backupId, Model model) {
        model.addAttribute("activePage", "matrix26_backups");
        model.addAttribute("backup", backupService.detail(backupId));
        model.addAttribute("encryption", backupSecurityService.metadata(backupId));
        model.addAttribute("keyStatus", backupSecurityService.keyStatus());
        return "control_center/backups/detail";
    }

    @GetMapping("/instances/{instanceId}/backups")
    public String instanceBackups(@PathVariable long instanceId, Model model) {
        PlatformBusinessClient instance = clientRepository.findById(instanceId)
                .orElseThrow(() -> new IllegalArgumentException("The requested instance does not exist."));
        model.addAttribute("activePage", "matrix26_backups");
        model.addAttribute("instance", instance);
        model.addAttribute("jobs", backupService.jobsForInstance(instanceId));
        model.addAttribute("candidate", backupService.candidates().stream()
                .filter(value -> value.instanceId().equals(instanceId))
                .findFirst()
                .orElse(null));
        return "control_center/backups/instance";
    }

    @PostMapping("/backups/{backupId}/verify")
    public String verifyEncryptedBackup(
            @PathVariable long backupId,
            Principal principal,
            RedirectAttributes redirectAttributes
    ) {
        try {
            backupSecurityService.verifyEncryptedBackup(backupId, actor(principal));
            redirectAttributes.addFlashAttribute("backupSuccess", "Encrypted backup verification completed successfully.");
        } catch (Matrix26BackupException ex) {
            redirectAttributes.addFlashAttribute("backupError", ex.getMessage());
        }
        return "redirect:/control-center/backups/" + backupId;
    }

    @GetMapping("/backups/policies")
    public String policies(Model model) {
        model.addAttribute("activePage", "matrix26_backup_policies");
        model.addAttribute("candidates", backupService.candidates());
        return "control_center/backups/policies";
    }

    @GetMapping("/backups/policies/{instanceId}")
    public String policy(@PathVariable long instanceId, Model model) {
        PlatformBusinessClient instance = clientRepository.findById(instanceId)
                .orElseThrow(() -> new IllegalArgumentException("The requested instance does not exist."));
        model.addAttribute("activePage", "matrix26_backup_policies");
        model.addAttribute("instance", instance);
        model.addAttribute("policy", backupSecurityService.policy(instanceId));
        return "control_center/backups/policy";
    }

    @PostMapping("/backups/policies/{instanceId}")
    public String savePolicy(
            @PathVariable long instanceId,
            @RequestParam("dailyKeep") int dailyKeep,
            @RequestParam("weeklyKeep") int weeklyKeep,
            @RequestParam("monthlyKeep") int monthlyKeep,
            @RequestParam(value = "finalKeepIndefinitely", defaultValue = "false") boolean finalKeepIndefinitely,
            @RequestParam(value = "enabled", defaultValue = "false") boolean enabled,
            Principal principal,
            RedirectAttributes redirectAttributes
    ) {
        try {
            backupSecurityService.savePolicy(instanceId, dailyKeep, weeklyKeep, monthlyKeep,
                    finalKeepIndefinitely, enabled, actor(principal));
            redirectAttributes.addFlashAttribute("backupSuccess", "Retention policy saved.");
        } catch (Matrix26BackupException ex) {
            redirectAttributes.addFlashAttribute("backupError", ex.getMessage());
        }
        return "redirect:/control-center/backups/policies/" + instanceId;
    }

    @GetMapping("/backups/retention")
    public String retention(
            @RequestParam(value = "instanceId", required = false) Long instanceId,
            Model model
    ) {
        model.addAttribute("activePage", "matrix26_backup_retention");
        model.addAttribute("candidates", backupService.candidates());
        if (instanceId != null) {
            model.addAttribute("preview", backupSecurityService.retentionPreview(instanceId));
        }
        return "control_center/backups/retention";
    }

    @PostMapping("/backups/retention/execute")
    public String executeRetention(
            @RequestParam("instanceId") long instanceId,
            @RequestParam("confirmation") String confirmation,
            Principal principal,
            RedirectAttributes redirectAttributes
    ) {
        try {
            backupSecurityService.executeRetention(instanceId, confirmation, actor(principal));
            redirectAttributes.addFlashAttribute("backupSuccess", "Manual retention cleanup completed.");
        } catch (Matrix26BackupException ex) {
            redirectAttributes.addFlashAttribute("backupError", ex.getMessage());
        }
        return "redirect:/control-center/backups/retention?instanceId=" + instanceId;
    }

    private String actor(Principal principal) {
        return principal == null || principal.getName() == null || principal.getName().isBlank()
                ? "matrix26-system"
                : principal.getName();
    }
}
