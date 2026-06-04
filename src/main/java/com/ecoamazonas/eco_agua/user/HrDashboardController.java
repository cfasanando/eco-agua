package com.ecoamazonas.eco_agua.user;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequestMapping("/admin/personnel")
public class HrDashboardController {

    private final HrDashboardService hrDashboardService;

    public HrDashboardController(HrDashboardService hrDashboardService) {
        this.hrDashboardService = hrDashboardService;
    }

    @GetMapping
    public String index(
            @RequestParam(name = "year", required = false) Integer year,
            @RequestParam(name = "month", required = false) Integer month,
            Model model
    ) {
        HrDashboardSnapshot snapshot = hrDashboardService.buildSnapshot(year, month);

        model.addAttribute("activePage", "personnel_overview");
        model.addAttribute("snapshot", snapshot);

        return "admin/personnel_overview";
    }
}
