package com.ecoamazonas.eco_agua.academy;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/admin/academy/dashboard")
public class AcademyDashboardController {

    private final AcademyDashboardService dashboardService;

    public AcademyDashboardController(AcademyDashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping
    public String dashboard(Model model) {
        model.addAttribute("activePage", "academy_dashboard");
        model.addAttribute("summary", dashboardService.buildSummary());
        return "admin/academy/dashboard";
    }
}
