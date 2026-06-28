package com.ecoamazonas.eco_agua.platform.control.purge;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.nio.charset.StandardCharsets;
import java.security.Principal;

@Controller
@RequestMapping("/control-center/purge")
@ConditionalOnProperty(name = "matrix26.control-center.enabled", havingValue = "true")
public class Matrix26PurgeController {
    private final Matrix26PurgeService service;
    private final Matrix26PurgeProperties properties;

    public Matrix26PurgeController(Matrix26PurgeService service, Matrix26PurgeProperties properties) {
        this.service = service;
        this.properties = properties;
    }

    @GetMapping
    public String index(Model model) {
        model.addAttribute("activePage", "matrix26_purge");
        model.addAttribute("summary", service.summary());
        model.addAttribute("candidates", service.candidates());
        model.addAttribute("plans", service.recentPlans());
        return "control_center/purge/index";
    }

    @GetMapping("/new")
    public String create(Model model) {
        model.addAttribute("activePage", "matrix26_purge");
        model.addAttribute("candidates", service.candidates());
        model.addAttribute("minimumReasonLength", properties.getMinimumReasonLength());
        return "control_center/purge/new";
    }

    @GetMapping("/{planId:\\d+}")
    public String detail(@PathVariable long planId, Model model) {
        Matrix26PurgePlan plan = service.plan(planId);
        model.addAttribute("activePage", "matrix26_purge");
        model.addAttribute("plan", plan);
        model.addAttribute("items", service.items(planId));
        model.addAttribute("checks", service.checks(planId));
        model.addAttribute("events", service.events(planId));
        return "control_center/purge/detail";
    }

    @PostMapping("/prepare")
    public String prepare(
            @RequestParam("archiveRecordId") long archiveRecordId,
            @RequestParam("reason") String reason,
            @RequestParam("confirmation") String confirmation,
            Principal principal,
            RedirectAttributes redirectAttributes
    ) {
        try {
            Matrix26PurgePlan plan = service.prepare(archiveRecordId, reason, confirmation, actor(principal));
            redirectAttributes.addFlashAttribute("purgeSuccess", "The purge dry run was prepared. Deleted resources: 0.");
            return "redirect:/control-center/purge/" + plan.id();
        } catch (RuntimeException ex) {
            redirectAttributes.addFlashAttribute("purgeError", ex.getMessage());
            return "redirect:/control-center/purge/new";
        }
    }

    @PostMapping("/{planId:\\d+}/refresh")
    public String refresh(@PathVariable long planId, Principal principal, RedirectAttributes redirectAttributes) {
        try {
            service.refresh(planId, actor(principal));
            redirectAttributes.addFlashAttribute("purgeSuccess", "The dry run was refreshed. Deleted resources: 0.");
        } catch (RuntimeException ex) {
            redirectAttributes.addFlashAttribute("purgeError", ex.getMessage());
        }
        return "redirect:/control-center/purge/" + planId;
    }

    @PostMapping("/{planId:\\d+}/prepare-execution")
    public String prepareExecution(
            @PathVariable long planId,
            @RequestParam("confirmation") String confirmation,
            Principal principal,
            RedirectAttributes redirectAttributes
    ) {
        try {
            service.prepareExecution(planId, confirmation, actor(principal));
            redirectAttributes.addFlashAttribute("purgeSuccess", "The dry run was frozen and is READY_TO_PURGE. No resource was deleted yet.");
        } catch (RuntimeException ex) {
            redirectAttributes.addFlashAttribute("purgeError", ex.getMessage());
        }
        return "redirect:/control-center/purge/" + planId;
    }

    @PostMapping("/{planId:\\d+}/execute")
    public String execute(
            @PathVariable long planId,
            @RequestParam("purgeConfirmation") String purgeConfirmation,
            @RequestParam("databaseConfirmation") String databaseConfirmation,
            Principal principal,
            RedirectAttributes redirectAttributes
    ) {
        try {
            Matrix26PurgePlan plan = service.execute(planId, purgeConfirmation, databaseConfirmation, actor(principal));
            if (plan.status() == Matrix26PurgeStatus.PURGED) {
                redirectAttributes.addFlashAttribute("purgeSuccess", "Operational purge completed. Final archive and audit evidence were preserved.");
            } else {
                redirectAttributes.addFlashAttribute("purgeError", "Operational purge needs manual review: " + plan.lastError());
            }
        } catch (RuntimeException ex) {
            redirectAttributes.addFlashAttribute("purgeError", ex.getMessage());
        }
        return "redirect:/control-center/purge/" + planId;
    }

    @GetMapping("/{planId:\\d+}/report")
    public ResponseEntity<byte[]> report(@PathVariable long planId) {
        Matrix26PurgePlan plan = service.plan(planId);
        byte[] body = service.report(planId).getBytes(StandardCharsets.UTF_8);
        ContentDisposition disposition = ContentDisposition.attachment()
                .filename(plan.publicId() + "-purge-report.txt")
                .build();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .contentType(MediaType.TEXT_PLAIN)
                .body(body);
    }

    private String actor(Principal principal) {
        return principal == null || principal.getName() == null || principal.getName().isBlank()
                ? "matrix26-system"
                : principal.getName();
    }
}
