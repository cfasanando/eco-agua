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
    private final PlatformBusinessClientRepository clientRepository;

    public Matrix26BackupController(
            Matrix26BackupService backupService,
            PlatformBusinessClientRepository clientRepository
    ) {
        this.backupService = backupService;
        this.clientRepository = clientRepository;
    }

    @GetMapping("/backups")
    public String index(Model model) {
        model.addAttribute("activePage", "matrix26_backups");
        model.addAttribute("summary", backupService.summary());
        model.addAttribute("jobs", backupService.recentJobs());
        model.addAttribute("tool", backupService.toolStatus());
        model.addAttribute("backupRoot", backupService.backupRoot().toString());
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
        return "control_center/backups/new";
    }

    @PostMapping("/backups")
    public String createBackup(
            @RequestParam("instanceId") long instanceId,
            @RequestParam(value = "backupScope", defaultValue = "FULL") String backupScope,
            @RequestParam(value = "confirmation", defaultValue = "false") boolean confirmation,
            Principal principal,
            RedirectAttributes redirectAttributes
    ) {
        try {
            boolean fullBackup = "FULL".equalsIgnoreCase(backupScope);
            Matrix26BackupJob job = fullBackup
                    ? backupService.createManualFullBackup(instanceId, actor(principal), confirmation)
                    : backupService.createManualDatabaseBackup(instanceId, actor(principal), confirmation);
            redirectAttributes.addFlashAttribute(
                    "backupSuccess",
                    (fullBackup ? "Full instance backup" : "Database backup")
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

    private String actor(Principal principal) {
        return principal == null || principal.getName() == null || principal.getName().isBlank()
                ? "matrix26-system"
                : principal.getName();
    }
}
