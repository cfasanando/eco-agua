package com.ecoamazonas.eco_agua.platform.control.operations;

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
@RequestMapping("/control-center/operations/alerts")
@ConditionalOnProperty(name = "matrix26.control-center.enabled", havingValue = "true")
public class Matrix26OperationAlertController {
    private final Matrix26OperationAlertService alertService;

    public Matrix26OperationAlertController(Matrix26OperationAlertService alertService) {
        this.alertService = alertService;
    }

    @GetMapping
    public String alerts(
            @RequestParam(value = "all", defaultValue = "false") boolean all,
            @RequestParam(value = "status", required = false) Matrix26OperationAlertStatus status,
            @RequestParam(value = "severity", required = false) Matrix26OperationAlertSeverity severity,
            @RequestParam(value = "source", required = false) Matrix26OperationAlertSource source,
            @RequestParam(value = "refresh", defaultValue = "false") boolean refresh,
            Model model
    ) {
        boolean includeClosed = all || status == Matrix26OperationAlertStatus.RESOLVED || status == Matrix26OperationAlertStatus.IGNORED;
        Matrix26OperationAlertCenterView view = alertService.alertCenter(includeClosed, status, severity, source, refresh);
        model.addAttribute("activePage", "matrix26_operation_alerts");
        model.addAttribute("view", view);
        model.addAttribute("summary", view.summary());
        model.addAttribute("alerts", view.alerts());
        model.addAttribute("all", includeClosed);
        model.addAttribute("selectedStatus", status);
        model.addAttribute("selectedSeverity", severity);
        model.addAttribute("selectedSource", source);
        model.addAttribute("statuses", Matrix26OperationAlertStatus.values());
        model.addAttribute("severities", Matrix26OperationAlertSeverity.values());
        model.addAttribute("sources", Matrix26OperationAlertSource.values());
        return "control_center/operations/alerts/index";
    }

    @GetMapping("/{id}")
    public String detail(@PathVariable long id, Model model) {
        model.addAttribute("activePage", "matrix26_operation_alerts");
        model.addAttribute("alert", alertService.alert(id));
        model.addAttribute("events", alertService.events(id));
        return "control_center/operations/alerts/detail";
    }

    @PostMapping("/{id}/acknowledge")
    public String acknowledge(
            @PathVariable long id,
            @RequestParam(value = "note", required = false) String note,
            Principal principal,
            RedirectAttributes redirectAttributes
    ) {
        alertService.acknowledge(id, actor(principal), note);
        redirectAttributes.addFlashAttribute("operationAlertSuccess", "Alert acknowledged.");
        return "redirect:/control-center/operations/alerts/" + id;
    }

    @PostMapping("/{id}/resolve")
    public String resolve(
            @PathVariable long id,
            @RequestParam(value = "note", required = false) String note,
            Principal principal,
            RedirectAttributes redirectAttributes
    ) {
        alertService.resolve(id, actor(principal), note);
        redirectAttributes.addFlashAttribute("operationAlertSuccess", "Alert resolved.");
        return "redirect:/control-center/operations/alerts/" + id;
    }

    @PostMapping("/{id}/ignore")
    public String ignore(
            @PathVariable long id,
            @RequestParam(value = "note", required = false) String note,
            Principal principal,
            RedirectAttributes redirectAttributes
    ) {
        alertService.ignore(id, actor(principal), note);
        redirectAttributes.addFlashAttribute("operationAlertSuccess", "Alert ignored.");
        return "redirect:/control-center/operations/alerts/" + id;
    }

    @PostMapping("/{id}/reopen")
    public String reopen(
            @PathVariable long id,
            @RequestParam(value = "note", required = false) String note,
            Principal principal,
            RedirectAttributes redirectAttributes
    ) {
        alertService.reopen(id, actor(principal), note);
        redirectAttributes.addFlashAttribute("operationAlertSuccess", "Alert reopened.");
        return "redirect:/control-center/operations/alerts/" + id;
    }

    private String actor(Principal principal) {
        return principal == null || principal.getName() == null || principal.getName().isBlank()
                ? "matrix26-admin"
                : principal.getName();
    }
}
