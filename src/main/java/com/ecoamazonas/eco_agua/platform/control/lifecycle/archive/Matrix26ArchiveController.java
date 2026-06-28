package com.ecoamazonas.eco_agua.platform.control.lifecycle.archive;

import com.ecoamazonas.eco_agua.platform.control.restores.Matrix26RestoreJob;
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
@RequestMapping("/control-center/lifecycle/archive")
@ConditionalOnProperty(name = "matrix26.control-center.enabled", havingValue = "true")
public class Matrix26ArchiveController {
    private final Matrix26ArchiveService service;
    private final Matrix26ArchiveProperties properties;

    public Matrix26ArchiveController(Matrix26ArchiveService service, Matrix26ArchiveProperties properties) {
        this.service = service;
        this.properties = properties;
    }

    @GetMapping
    public String index(Model model) {
        model.addAttribute("activePage", "matrix26_archive");
        model.addAttribute("summary", service.summary());
        model.addAttribute("records", service.records());
        model.addAttribute("cloneCode", properties.getCloneInstanceCode());
        model.addAttribute("clonePort", properties.getCloneRuntimePort());
        return "control_center/lifecycle/archive/index";
    }

    @GetMapping("/{id:\\d+}")
    public String detail(@PathVariable long id, Model model) {
        Matrix26ArchiveRecord record = service.record(id);
        model.addAttribute("activePage", "matrix26_archive");
        model.addAttribute("record", record);
        model.addAttribute("events", service.events(id));
        model.addAttribute("restoreLinks", service.restoreLinks(id));
        model.addAttribute("cloneCode", properties.getCloneInstanceCode());
        model.addAttribute("cloneName", properties.getCloneInstanceName());
        model.addAttribute("cloneDatabase", properties.getCloneDatabaseName());
        model.addAttribute("cloneRuntime", properties.getCloneRuntimeProfile());
        model.addAttribute("clonePort", properties.getCloneRuntimePort());
        model.addAttribute("cloneUrl", properties.getClonePublicUrl());
        model.addAttribute("restoreConfirmation", "RESTORE ARCHIVE " + properties.getCloneInstanceCode());
        return "control_center/lifecycle/archive/detail";
    }

    @GetMapping("/restores")
    public String restores(Model model) {
        model.addAttribute("activePage", "matrix26_archive_restores");
        model.addAttribute("restoreLinks", service.recentRestoreLinks());
        return "control_center/lifecycle/archive/restores";
    }

    @PostMapping("/refresh")
    public String refresh(Principal principal, RedirectAttributes redirectAttributes) {
        service.refresh(actor(principal));
        redirectAttributes.addFlashAttribute("archiveSuccess", "Historical archive inventory was refreshed from decommissioned instances.");
        return "redirect:/control-center/lifecycle/archive";
    }

    @PostMapping("/{id:\\d+}/verify")
    public String verify(@PathVariable long id, Principal principal, RedirectAttributes redirectAttributes) {
        try {
            service.verify(id, actor(principal));
            redirectAttributes.addFlashAttribute("archiveSuccess", "The final archive was reverified successfully.");
        } catch (RuntimeException ex) {
            redirectAttributes.addFlashAttribute("archiveError", ex.getMessage());
        }
        return "redirect:/control-center/lifecycle/archive/" + id;
    }

    @PostMapping("/{id:\\d+}/restore-as-clone")
    public String restoreAsClone(
            @PathVariable long id,
            @RequestParam(value = "startAfterRestore", defaultValue = "false") boolean startAfterRestore,
            @RequestParam("confirmation") String confirmation,
            Principal principal,
            RedirectAttributes redirectAttributes
    ) {
        try {
            Matrix26RestoreJob restore = service.restoreAsClone(id, startAfterRestore, confirmation, actor(principal));
            redirectAttributes.addFlashAttribute("archiveSuccess", "Archived instance restored as isolated clone " + restore.targetInstanceCode() + ".");
            return "redirect:/control-center/restores/" + restore.id();
        } catch (RuntimeException ex) {
            redirectAttributes.addFlashAttribute("archiveError", ex.getMessage());
            return "redirect:/control-center/lifecycle/archive/" + id;
        }
    }

    private String actor(Principal principal) {
        return principal == null || principal.getName() == null || principal.getName().isBlank()
                ? "matrix26-system"
                : principal.getName();
    }
}
