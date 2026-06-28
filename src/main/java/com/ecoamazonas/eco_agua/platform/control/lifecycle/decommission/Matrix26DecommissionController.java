package com.ecoamazonas.eco_agua.platform.control.lifecycle.decommission;

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
@RequestMapping("/control-center/lifecycle/decommission")
@ConditionalOnProperty(name = "matrix26.control-center.enabled", havingValue = "true")
public class Matrix26DecommissionController {

    private final Matrix26DecommissionService service;
    private final Matrix26DecommissionProperties properties;

    public Matrix26DecommissionController(
            Matrix26DecommissionService service,
            Matrix26DecommissionProperties properties
    ) {
        this.service = service;
        this.properties = properties;
    }

    @GetMapping
    public String index(Model model) {
        model.addAttribute("activePage", "matrix26_decommission");
        model.addAttribute("summary", service.summary());
        model.addAttribute("candidates", service.candidates());
        model.addAttribute("jobs", service.recentJobs());
        model.addAttribute("defaultRetentionDays", properties.getDefaultRetentionDays());
        return "control_center/lifecycle/decommission/index";
    }

    @GetMapping("/new")
    public String create(Model model) {
        model.addAttribute("activePage", "matrix26_decommission");
        model.addAttribute("candidates", service.candidates());
        model.addAttribute("defaultRetentionDays", properties.getDefaultRetentionDays());
        return "control_center/lifecycle/decommission/new";
    }

    @GetMapping("/{jobId:\\d+}")
    public String detail(@PathVariable long jobId, Model model) {
        Matrix26DecommissionJob job = service.job(jobId);
        model.addAttribute("activePage", "matrix26_decommission");
        model.addAttribute("job", job);
        model.addAttribute("checks", service.checks(jobId));
        model.addAttribute("events", service.events(jobId));
        model.addAttribute("scheduleStates", service.scheduleStates(jobId));
        return "control_center/lifecycle/decommission/detail";
    }

    @GetMapping("/decommissioned")
    public String decommissioned(Model model) {
        model.addAttribute("activePage", "matrix26_decommissioned");
        model.addAttribute("jobs", service.decommissionedJobs());
        return "control_center/lifecycle/decommission/decommissioned";
    }

    @PostMapping("/prepare")
    public String prepare(
            @RequestParam("instanceId") long instanceId,
            @RequestParam("reason") String reason,
            @RequestParam(value = "administrativeNotes", defaultValue = "") String administrativeNotes,
            @RequestParam("retentionDays") int retentionDays,
            @RequestParam("confirmation") String confirmation,
            Principal principal,
            RedirectAttributes redirectAttributes
    ) {
        try {
            Matrix26DecommissionJob job = service.prepare(
                    instanceId,
                    reason,
                    administrativeNotes,
                    retentionDays,
                    confirmation,
                    actor(principal)
            );
            redirectAttributes.addFlashAttribute(
                    "decommissionSuccess",
                    "The final archive is verified and the plan is ready for a separate decommission confirmation."
            );
            return "redirect:/control-center/lifecycle/decommission/" + job.id();
        } catch (Matrix26DecommissionException ex) {
            redirectAttributes.addFlashAttribute("decommissionError", ex.getMessage());
            return "redirect:/control-center/lifecycle/decommission/new";
        }
    }

    @PostMapping("/{jobId:\\d+}/execute")
    public String execute(
            @PathVariable long jobId,
            @RequestParam("confirmation") String confirmation,
            Principal principal,
            RedirectAttributes redirectAttributes
    ) {
        try {
            Matrix26DecommissionJob job = service.execute(jobId, confirmation, actor(principal));
            redirectAttributes.addFlashAttribute(
                    "decommissionSuccess",
                    "The instance is decommissioned. Database, runtime, resources, and final archive remain preserved."
            );
            return "redirect:/control-center/lifecycle/decommission/" + job.id();
        } catch (Matrix26DecommissionException ex) {
            redirectAttributes.addFlashAttribute("decommissionError", ex.getMessage());
            return "redirect:/control-center/lifecycle/decommission/" + jobId;
        }
    }

    private String actor(Principal principal) {
        return principal == null || principal.getName() == null || principal.getName().isBlank()
                ? "matrix26-system"
                : principal.getName();
    }
}
