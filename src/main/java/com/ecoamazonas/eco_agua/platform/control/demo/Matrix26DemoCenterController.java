package com.ecoamazonas.eco_agua.platform.control.demo;

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

    @GetMapping("/{code}/logs")
    public String logs(
            @PathVariable String code,
            Model model
    ) {
        model.addAttribute("activePage", "matrix26_demo_center");
        model.addAttribute("logView", demoCenterService.logs(code));
        return "control_center/demo_center/logs";
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
                () -> demoCenterService.startPortal(code, actor(principal))
        );
    }

    @PostMapping("/{code}/stop")
    public String stop(
            @PathVariable String code,
            @RequestParam(value = "confirmation", required = false) String confirmation,
            Principal principal,
            RedirectAttributes redirectAttributes
    ) {
        return executeRuntimeAction(
                code,
                redirectAttributes,
                () -> demoCenterService.stopPortal(code, actor(principal))
        );
    }

    @PostMapping("/{code}/restart")
    public String restart(
            @PathVariable String code,
            @RequestParam(value = "confirmation", required = false) String confirmation,
            Principal principal,
            RedirectAttributes redirectAttributes
    ) {
        return executeRuntimeAction(
                code,
                redirectAttributes,
                () -> demoCenterService.restartPortal(code, actor(principal))
        );
    }

    private String executeRuntimeAction(
            String code,
            RedirectAttributes redirectAttributes,
            RuntimeAction action
    ) {
        try {
            Matrix26DemoRuntimeActionResult result = action.execute();
            if (result.success()) {
                redirectAttributes.addFlashAttribute("demoCenterSuccess", result.message());
            } else {
                redirectAttributes.addFlashAttribute("demoCenterError", result.message());
            }
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
        Matrix26DemoRuntimeActionResult execute();
    }
}
