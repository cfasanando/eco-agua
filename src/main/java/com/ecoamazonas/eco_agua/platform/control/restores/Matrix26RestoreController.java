package com.ecoamazonas.eco_agua.platform.control.restores;

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
@RequestMapping("/control-center/restores")
@ConditionalOnProperty(name = "matrix26.control-center.enabled", havingValue = "true")
public class Matrix26RestoreController {

    private final Matrix26RestoreService restoreService;

    public Matrix26RestoreController(Matrix26RestoreService restoreService) {
        this.restoreService = restoreService;
    }

    @GetMapping
    public String index(Model model) {
        model.addAttribute("activePage", "matrix26_restores");
        model.addAttribute("summary", restoreService.summary());
        model.addAttribute("jobs", restoreService.recentJobs());
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
                    principal == null ? "matrix26-admin" : principal.getName()
            );
            redirectAttributes.addFlashAttribute("restoreSuccess", "Restore job completed: " + job.publicId());
            return "redirect:/control-center/restores/" + job.id();
        } catch (Matrix26RestoreException ex) {
            redirectAttributes.addFlashAttribute("restoreError", ex.getMessage());
            return "redirect:/control-center/restores/new?backupId=" + backupId;
        }
    }

    @GetMapping("/{id}")
    public String detail(@PathVariable long id, Model model) {
        model.addAttribute("activePage", "matrix26_restores");
        model.addAttribute("job", restoreService.job(id));
        model.addAttribute("steps", restoreService.steps(id));
        model.addAttribute("artifacts", restoreService.artifacts(id));
        model.addAttribute("verifications", restoreService.verifications(id));
        return "control_center/restores/detail";
    }

    @GetMapping("/{id}/steps")
    public String steps(@PathVariable long id) {
        return "redirect:/control-center/restores/" + id + "#restore-steps";
    }
}
