package com.ecoamazonas.eco_agua.platform.control.modules.acceptance;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequestMapping("/control-center/modules/acceptance")
@ConditionalOnProperty(name = "matrix26.control-center.enabled", havingValue = "true")
public class Matrix26FeatureFlagAcceptanceController {

    private final Matrix26FeatureFlagAcceptanceService acceptanceService;

    public Matrix26FeatureFlagAcceptanceController(Matrix26FeatureFlagAcceptanceService acceptanceService) {
        this.acceptanceService = acceptanceService;
    }

    @GetMapping
    public String index(@RequestParam(value = "refresh", defaultValue = "false") boolean refresh, Model model) {
        model.addAttribute("activePage", "matrix26_module_acceptance");
        model.addAttribute("acceptance", acceptanceService.acceptance(refresh));
        return "control_center/modules/acceptance/index";
    }
}
