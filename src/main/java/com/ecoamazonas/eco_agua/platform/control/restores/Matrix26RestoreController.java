package com.ecoamazonas.eco_agua.platform.control.restores;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
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
import java.util.LinkedHashMap;
import java.util.Map;

@Controller
@RequestMapping("/control-center/restores")
@ConditionalOnProperty(name = "matrix26.control-center.enabled", havingValue = "true")
public class Matrix26RestoreController {

    private final Matrix26RestoreService restoreService;
    private final Matrix26RestoreVerificationService verificationService;
    private final Matrix26RestoreCleanupService cleanupService;

    public Matrix26RestoreController(
            Matrix26RestoreService restoreService,
            Matrix26RestoreVerificationService verificationService,
            Matrix26RestoreCleanupService cleanupService
    ) {
        this.restoreService = restoreService;
        this.verificationService = verificationService;
        this.cleanupService = cleanupService;
    }

    @GetMapping
    public String index(Model model) {
        model.addAttribute("activePage", "matrix26_restores");
        model.addAttribute("summary", restoreService.summary());
        var jobs = restoreService.recentJobs();
        Map<Long, Matrix26RestoreValidationRun> latestValidations = new LinkedHashMap<>();
        for (Matrix26RestoreJob job : jobs) {
            Matrix26RestoreValidationRun latest = verificationService.latest(job.id());
            if (latest != null) latestValidations.put(job.id(), latest);
        }
        model.addAttribute("jobs", jobs);
        model.addAttribute("latestValidations", latestValidations);
        model.addAttribute("candidates", restoreService.candidates());
        model.addAttribute("restoreProperties", restoreService.properties());
        return "control_center/restores/index";
    }

    @GetMapping("/new")
    public String newRestore(@RequestParam(value = "backupId", required = false) Long backupId, Model model) {
        model.addAttribute("activePage", "matrix26_restores");
        model.addAttribute("candidates", restoreService.candidates());
        model.addAttribute("selectedBackupId", backupId);
        model.addAttribute("restoreProperties", restoreService.properties());
        return "control_center/restores/new";
    }

    @PostMapping
    public String execute(
            @RequestParam("backupId") long backupId,
            @RequestParam(value = "startAfterRestore", defaultValue = "false") boolean startAfterRestore,
            @RequestParam("confirmation") String confirmation,
            Principal principal,
            RedirectAttributes redirectAttributes
    ) {
        try {
            Matrix26RestoreJob job = restoreService.restoreClone(
                    backupId,
                    startAfterRestore,
                    confirmation,
                    actor(principal)
            );
            redirectAttributes.addFlashAttribute("restoreSuccess", "Restore job completed: " + job.publicId());
            return "redirect:/control-center/restores/" + job.id();
        } catch (Matrix26RestoreException ex) {
            redirectAttributes.addFlashAttribute("restoreError", ex.getMessage());
            return "redirect:/control-center/restores/new?backupId=" + backupId;
        }
    }

    @GetMapping("/{id:\\d+}")
    public String detail(@PathVariable long id, Model model) {
        Matrix26RestoreJob job = restoreService.job(id);
        Matrix26RestoreValidationRun latestValidation = verificationService.latest(id);
        model.addAttribute("activePage", "matrix26_restores");
        model.addAttribute("job", job);
        model.addAttribute("cleanupEligible", job.status() == Matrix26RestoreStatus.FAILED
                || job.status() == Matrix26RestoreStatus.CLEANUP_REQUIRED
                || job.status() == Matrix26RestoreStatus.CLEANING
                || job.status() == Matrix26RestoreStatus.PARTIALLY_CLEANED);
        model.addAttribute("steps", restoreService.steps(id));
        model.addAttribute("artifacts", restoreService.artifacts(id));
        model.addAttribute("verifications", restoreService.verifications(id));
        model.addAttribute("latestValidation", latestValidation);
        model.addAttribute("validationItems", latestValidation == null ? java.util.List.of() : verificationService.items(latestValidation.id()));
        model.addAttribute("resumePlan", restoreService.resumePlan(id));
        model.addAttribute("cleanupPreview", restoreService.cleanupPreview(id));
        Matrix26RestoreCleanupPlan cleanupPlan = cleanupService.latest(id);
        model.addAttribute("cleanupPlan", cleanupPlan);
        model.addAttribute("cleanupItems", cleanupService.items(cleanupPlan));
        model.addAttribute("restoreProperties", restoreService.properties());
        return "control_center/restores/detail";
    }

    @PostMapping("/{id:\\d+}/verify")
    public String verify(
            @PathVariable long id,
            @RequestParam("confirmation") String confirmation,
            Principal principal,
            RedirectAttributes redirectAttributes
    ) {
        try {
            Matrix26RestoreValidationRun run = verificationService.verify(id, confirmation, actor(principal));
            redirectAttributes.addFlashAttribute("restoreSuccess",
                    "Automated restore verification completed: " + run.status().getLabel());
        } catch (Matrix26RestoreException ex) {
            redirectAttributes.addFlashAttribute("restoreError", ex.getMessage());
        }
        return "redirect:/control-center/restores/" + id + "#restore-validation";
    }

    @PostMapping("/{id:\\d+}/resume")
    public String resume(
            @PathVariable long id,
            @RequestParam("confirmation") String confirmation,
            Principal principal,
            RedirectAttributes redirectAttributes
    ) {
        try {
            Matrix26RestoreJob job = restoreService.resume(id, confirmation, actor(principal));
            redirectAttributes.addFlashAttribute("restoreSuccess", "Restore job resumed and completed: " + job.publicId());
        } catch (Matrix26RestoreException ex) {
            redirectAttributes.addFlashAttribute("restoreError", ex.getMessage());
        }
        return "redirect:/control-center/restores/" + id;
    }


    @PostMapping("/{id:\\d+}/cleanup/prepare")
    public String prepareCleanup(
            @PathVariable long id,
            @RequestParam("confirmation") String confirmation,
            Principal principal,
            RedirectAttributes redirectAttributes
    ) {
        try {
            Matrix26RestoreCleanupPlan plan = cleanupService.prepare(id, confirmation, actor(principal));
            redirectAttributes.addFlashAttribute("restoreSuccess",
                    "Cleanup preview created: " + plan.publicId() + " — " + plan.status().getLabel());
        } catch (Matrix26RestoreException ex) {
            redirectAttributes.addFlashAttribute("restoreError", ex.getMessage());
        }
        return "redirect:/control-center/restores/" + id + "#restore-cleanup";
    }

    @PostMapping("/{id:\\d+}/cleanup/{planId:\\d+}/approve")
    public String approveCleanup(
            @PathVariable long id,
            @PathVariable long planId,
            @RequestParam("stopConfirmation") String stopConfirmation,
            @RequestParam("filesConfirmation") String filesConfirmation,
            @RequestParam("databaseConfirmation") String databaseConfirmation,
            @RequestParam("registrationConfirmation") String registrationConfirmation,
            Principal principal,
            RedirectAttributes redirectAttributes
    ) {
        try {
            Matrix26RestoreCleanupPlan plan = cleanupService.approve(
                    id, planId, stopConfirmation, filesConfirmation, databaseConfirmation,
                    registrationConfirmation, actor(principal));
            redirectAttributes.addFlashAttribute("restoreSuccess",
                    "Cleanup plan approved: " + plan.publicId());
        } catch (Matrix26RestoreException ex) {
            redirectAttributes.addFlashAttribute("restoreError", ex.getMessage());
        }
        return "redirect:/control-center/restores/" + id + "#restore-cleanup";
    }

    @PostMapping("/{id:\\d+}/cleanup/{planId:\\d+}/execute")
    public String executeCleanup(
            @PathVariable long id,
            @PathVariable long planId,
            @RequestParam("confirmation") String confirmation,
            Principal principal,
            RedirectAttributes redirectAttributes
    ) {
        try {
            Matrix26RestoreCleanupPlan plan = cleanupService.execute(id, planId, confirmation, actor(principal));
            redirectAttributes.addFlashAttribute("restoreSuccess",
                    "Cleanup completed: " + plan.publicId());
        } catch (Matrix26RestoreException ex) {
            redirectAttributes.addFlashAttribute("restoreError", ex.getMessage());
        }
        return "redirect:/control-center/restores/" + id + "#restore-cleanup";
    }

    @GetMapping("/validations/{runId:\\d+}/report")
    public ResponseEntity<byte[]> report(@PathVariable long runId) {
        Matrix26RestoreValidationRun run = verificationService.run(runId);
        byte[] content = verificationService.report(runId).getBytes(StandardCharsets.UTF_8);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"matrix26-restore-verification-" + run.publicId() + ".txt\"")
                .contentType(new MediaType("text", "plain", StandardCharsets.UTF_8))
                .contentLength(content.length)
                .body(content);
    }

    @GetMapping("/{id:\\d+}/steps")
    public String steps(@PathVariable long id) {
        return "redirect:/control-center/restores/" + id + "#restore-steps";
    }

    private String actor(Principal principal) {
        return principal == null ? "matrix26-admin" : principal.getName();
    }
}
