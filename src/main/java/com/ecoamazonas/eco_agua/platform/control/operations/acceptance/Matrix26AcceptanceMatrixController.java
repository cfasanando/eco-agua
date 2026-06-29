package com.ecoamazonas.eco_agua.platform.control.operations.acceptance;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequestMapping("/control-center/operations/acceptance")
@ConditionalOnProperty(name = "matrix26.control-center.enabled", havingValue = "true")
public class Matrix26AcceptanceMatrixController {
    private final Matrix26AcceptanceMatrixService acceptanceMatrixService;

    public Matrix26AcceptanceMatrixController(Matrix26AcceptanceMatrixService acceptanceMatrixService) {
        this.acceptanceMatrixService = acceptanceMatrixService;
    }

    @GetMapping
    public String acceptance(
            @RequestParam(value = "refresh", defaultValue = "false") boolean refresh,
            Model model
    ) {
        Matrix26AcceptanceMatrix acceptanceMatrix = acceptanceMatrixService.matrix(refresh);
        model.addAttribute("activePage", "matrix26_operations_acceptance");
        model.addAttribute("acceptanceMatrix", acceptanceMatrix);
        return "control_center/operations/acceptance/index";
    }
}
