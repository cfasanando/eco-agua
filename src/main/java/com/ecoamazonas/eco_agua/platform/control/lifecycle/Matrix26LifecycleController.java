package com.ecoamazonas.eco_agua.platform.control.lifecycle;

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
@RequestMapping("/control-center/lifecycle")
@ConditionalOnProperty(name = "matrix26.control-center.enabled", havingValue = "true")
public class Matrix26LifecycleController {

    private final Matrix26LifecycleService lifecycleService;
    private final Matrix26LifecycleProperties properties;

    public Matrix26LifecycleController(
            Matrix26LifecycleService lifecycleService,
            Matrix26LifecycleProperties properties
    ) {
        this.lifecycleService = lifecycleService;
        this.properties = properties;
    }

    @GetMapping
    public String index(Model model) {
        model.addAttribute("activePage", "matrix26_lifecycle");
        model.addAttribute("summary", lifecycleService.summary());
        model.addAttribute("instances", lifecycleService.instances());
        model.addAttribute("jobs", lifecycleService.recentJobs());
        model.addAttribute("maximumBackupAgeHours", properties.getMaximumVerifiedBackupAgeHours());
        return "control_center/lifecycle/index";
    }

    @GetMapping("/jobs/{jobId:\\d+}")
    public String detail(@PathVariable long jobId, Model model) {
        Matrix26LifecycleJob job = lifecycleService.job(jobId);
        model.addAttribute("activePage", "matrix26_lifecycle");
        model.addAttribute("job", job);
        model.addAttribute("events", lifecycleService.events(jobId));
        model.addAttribute("scheduleStates", lifecycleService.scheduleStates(jobId));
        model.addAttribute("instanceView", lifecycleService.instanceView(job.instanceId()));
        return "control_center/lifecycle/detail";
    }

    @PostMapping("/instances/{instanceId:\\d+}/suspend")
    public String suspend(
            @PathVariable long instanceId,
            @RequestParam("reason") String reason,
            @RequestParam("confirmation") String confirmation,
            Principal principal,
            RedirectAttributes redirectAttributes
    ) {
        try {
            Matrix26LifecycleJob job = lifecycleService.suspend(
                    instanceId, reason, confirmation, actor(principal)
            );
            redirectAttributes.addFlashAttribute(
                    "lifecycleSuccess",
                    "Instance suspended successfully. Database, resources, and backups were preserved."
            );
            return "redirect:/control-center/lifecycle/jobs/" + job.id();
        } catch (Matrix26LifecycleException ex) {
            redirectAttributes.addFlashAttribute("lifecycleError", ex.getMessage());
            return "redirect:/control-center/lifecycle";
        }
    }

    @PostMapping("/instances/{instanceId:\\d+}/reactivate")
    public String reactivate(
            @PathVariable long instanceId,
            @RequestParam("reason") String reason,
            @RequestParam("confirmation") String confirmation,
            Principal principal,
            RedirectAttributes redirectAttributes
    ) {
        try {
            Matrix26LifecycleJob job = lifecycleService.reactivate(
                    instanceId, reason, confirmation, actor(principal)
            );
            redirectAttributes.addFlashAttribute(
                    "lifecycleSuccess",
                    "Instance reactivated successfully and its previous backup schedules were restored."
            );
            return "redirect:/control-center/lifecycle/jobs/" + job.id();
        } catch (Matrix26LifecycleException ex) {
            redirectAttributes.addFlashAttribute("lifecycleError", ex.getMessage());
            return "redirect:/control-center/lifecycle";
        }
    }

    private String actor(Principal principal) {
        return principal == null || principal.getName() == null || principal.getName().isBlank()
                ? "matrix26-system"
                : principal.getName();
    }
}
