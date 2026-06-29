package com.ecoamazonas.eco_agua.platform.control.security;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/control-center/security")
@ConditionalOnProperty(name = "matrix26.control-center.enabled", havingValue = "true")
public class Matrix26ControlSecurityController {

    private final Matrix26ControlSecurityService securityService;

    public Matrix26ControlSecurityController(Matrix26ControlSecurityService securityService) {
        this.securityService = securityService;
    }

    @GetMapping
    public String index(Model model) {
        model.addAttribute("activePage", "matrix26_security");
        model.addAttribute("overview", securityService.overview());
        return "control_center/security/index";
    }
}
