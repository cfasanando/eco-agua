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
@RequestMapping("/control-center/purge/archive-destruction")
@ConditionalOnProperty(name = "matrix26.control-center.enabled", havingValue = "true")
public class Matrix26ArchiveDestructionController {
    private final Matrix26ArchiveDestructionService service;
    private final Matrix26PurgeProperties properties;

    public Matrix26ArchiveDestructionController(
            Matrix26ArchiveDestructionService service,
            Matrix26PurgeProperties properties
    ) {
        this.service = service;
        this.properties = properties;
    }

    @GetMapping
    public String index(Model model) {
        model.addAttribute("activePage", "matrix26_archive_destruction");
        model.addAttribute("summary", service.summary());
        model.addAttribute("candidates", service.candidates());
        model.addAttribute("plans", service.recentPlans());
        model.addAttribute("archiveDestructionExecutionEnabled", properties.isArchiveDestructionExecutionEnabled());
        return "control_center/purge/archive-destruction/index";
    }

    @GetMapping("/new")
    public String create(Model model) {
        model.addAttribute("activePage", "matrix26_archive_destruction");
        model.addAttribute("candidates", service.candidates());
        model.addAttribute("minimumReasonLength", properties.getMinimumReasonLength());
        return "control_center/purge/archive-destruction/new";
    }

    @GetMapping("/{planId:\\d+}")
    public String detail(@PathVariable long planId, Model model) {
        Matrix26ArchiveDestructionPlan plan = service.plan(planId);
        model.addAttribute("activePage", "matrix26_archive_destruction");
        model.addAttribute("plan", plan);
        model.addAttribute("items", service.items(planId));
        model.addAttribute("checks", service.checks(planId));
        model.addAttribute("events", service.events(planId));
        model.addAttribute("archiveDestructionExecutionEnabled", properties.isArchiveDestructionExecutionEnabled());
        return "control_center/purge/archive-destruction/detail";
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
            Matrix26ArchiveDestructionPlan plan = service.prepare(archiveRecordId, reason, confirmation, actor(principal));
            redirectAttributes.addFlashAttribute("purgeSuccess", "The archive destruction planner was prepared. Deleted resources: 0.");
            return "redirect:/control-center/purge/archive-destruction/" + plan.id();
        } catch (RuntimeException ex) {
            redirectAttributes.addFlashAttribute("purgeError", ex.getMessage());
            return "redirect:/control-center/purge/archive-destruction/new";
        }
    }

    @PostMapping("/{planId:\\d+}/refresh")
    public String refresh(@PathVariable long planId, Principal principal, RedirectAttributes redirectAttributes) {
        try {
            service.refresh(planId, actor(principal));
            redirectAttributes.addFlashAttribute("purgeSuccess", "The archive destruction planner was refreshed. Deleted resources: 0.");
        } catch (RuntimeException ex) {
            redirectAttributes.addFlashAttribute("purgeError", ex.getMessage());
        }
        return "redirect:/control-center/purge/archive-destruction/" + planId;
    }


    @PostMapping("/{planId:\\d+}/approve")
    public String approve(
            @PathVariable long planId,
            @RequestParam("approvalConfirmation") String approvalConfirmation,
            Principal principal,
            RedirectAttributes redirectAttributes
    ) {
        try {
            service.approveDestruction(planId, approvalConfirmation, actor(principal));
            redirectAttributes.addFlashAttribute("purgeSuccess", "Archive destruction was approved. No archive package was destroyed yet.");
        } catch (RuntimeException ex) {
            redirectAttributes.addFlashAttribute("purgeError", ex.getMessage());
        }
        return "redirect:/control-center/purge/archive-destruction/" + planId;
    }

    @PostMapping("/{planId:\\d+}/execute")
    public String execute(
            @PathVariable long planId,
            @RequestParam("destroyConfirmation") String destroyConfirmation,
            @RequestParam("irreversibleConfirmation") String irreversibleConfirmation,
            Principal principal,
            RedirectAttributes redirectAttributes
    ) {
        try {
            Matrix26ArchiveDestructionPlan plan = service.executeDestruction(
                    planId,
                    destroyConfirmation,
                    irreversibleConfirmation,
                    actor(principal)
            );
            if (plan.status() == Matrix26ArchiveDestructionStatus.DESTROYED) {
                redirectAttributes.addFlashAttribute("purgeSuccess", "Archive package destruction completed. Central audit metadata was preserved.");
            } else {
                redirectAttributes.addFlashAttribute("purgeError", "Archive package destruction needs manual review: " + plan.lastError());
            }
        } catch (RuntimeException ex) {
            redirectAttributes.addFlashAttribute("purgeError", ex.getMessage());
        }
        return "redirect:/control-center/purge/archive-destruction/" + planId;
    }

    @GetMapping("/{planId:\\d+}/report")
    public ResponseEntity<byte[]> report(@PathVariable long planId) {
        Matrix26ArchiveDestructionPlan plan = service.plan(planId);
        byte[] body = service.report(planId).getBytes(StandardCharsets.UTF_8);
        ContentDisposition disposition = ContentDisposition.attachment()
                .filename(plan.publicId() + "-archive-destruction-report.txt")
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
