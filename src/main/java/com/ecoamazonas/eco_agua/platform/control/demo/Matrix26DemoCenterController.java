package com.ecoamazonas.eco_agua.platform.control.demo;

import com.ecoamazonas.eco_agua.platform.control.operations.Matrix26RuntimeControlException;
import com.ecoamazonas.eco_agua.platform.control.operations.Matrix26RuntimeControlResult;
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
@RequestMapping("/control-center/demo-center")
public class Matrix26DemoCenterController {

    private final Matrix26DemoCenterService demoCenterService;

    public Matrix26DemoCenterController(Matrix26DemoCenterService demoCenterService) {
        this.demoCenterService = demoCenterService;
    }

    @GetMapping
    public String index(
            @RequestParam(value = "refresh", defaultValue = "false") boolean refresh,
            Model model
    ) {
        model.addAttribute("activePage", "matrix26_demo_center");
        model.addAttribute("demoCenter", demoCenterService.dashboard(refresh));
        return "control_center/demo_center/index";
    }

    @GetMapping("/{code}")
    public String detail(
            @PathVariable String code,
            @RequestParam(value = "refresh", defaultValue = "false") boolean refresh,
            Model model
    ) {
        model.addAttribute("activePage", "matrix26_demo_center");
        model.addAttribute("portal", demoCenterService.portal(code, refresh));
        return "control_center/demo_center/detail";
    }

    @PostMapping("/{code}/start")
    public String start(
            @PathVariable String code,
            Principal principal,
            RedirectAttributes redirectAttributes
    ) {
        return executeRuntimeAction(
                code,
                redirectAttributes,
                () -> demoCenterService.runtimeControlService().start(runtimeKey(code), actor(principal))
        );
    }

    @PostMapping("/{code}/stop")
    public String stop(
            @PathVariable String code,
            @RequestParam("confirmation") String confirmation,
            Principal principal,
            RedirectAttributes redirectAttributes
    ) {
        return executeRuntimeAction(
                code,
                redirectAttributes,
                () -> demoCenterService.runtimeControlService().stop(runtimeKey(code), actor(principal), confirmation)
        );
    }

    @PostMapping("/{code}/restart")
    public String restart(
            @PathVariable String code,
            @RequestParam("confirmation") String confirmation,
            Principal principal,
            RedirectAttributes redirectAttributes
    ) {
        return executeRuntimeAction(
                code,
                redirectAttributes,
                () -> demoCenterService.runtimeControlService().restart(runtimeKey(code), actor(principal), confirmation)
        );
    }

    private String runtimeKey(String code) {
        Matrix26DemoPortalView portal = demoCenterService.portal(code, true);
        if (portal.runtimeKey() == null || portal.runtimeKey().isBlank()) {
            throw new Matrix26RuntimeControlException("Este portal demo no tiene runtime administrable registrado en Matrix26.");
        }
        return portal.runtimeKey();
    }

    private String executeRuntimeAction(
            String code,
            RedirectAttributes redirectAttributes,
            RuntimeAction action
    ) {
        try {
            Matrix26RuntimeControlResult result = action.execute();
            redirectAttributes.addFlashAttribute("demoCenterSuccess", result.message());
        } catch (RuntimeException ex) {
            redirectAttributes.addFlashAttribute("demoCenterError", ex.getMessage());
        }
        return "redirect:/control-center/demo-center/" + code + "?refresh=true";
    }

    private String actor(Principal principal) {
        return principal == null || principal.getName() == null || principal.getName().isBlank()
                ? "matrix26-demo-center"
                : principal.getName();
    }

    @FunctionalInterface
    private interface RuntimeAction {
        Matrix26RuntimeControlResult execute();
    }
}
