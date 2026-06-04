package com.ecoamazonas.eco_agua.user;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

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

    @GetMapping("/monthly-payroll")
    public String showMonthlyPayroll(
            @RequestParam(name = "year", required = false) Integer year,
            @RequestParam(name = "month", required = false) Integer month,
            Model model
    ) {
        HrMonthlyPayrollSnapshot snapshot = hrDashboardService.buildMonthlyPayroll(year, month);

        model.addAttribute("activePage", "personnel_monthly_payroll");
        model.addAttribute("snapshot", snapshot);

        return "admin/personnel_monthly_payroll";
    }

    @GetMapping("/obligations")
    public String showEmployeeObligations(
            @RequestParam(name = "status", required = false) String status,
            Model model
    ) {
        HrEmployeeObligationOverviewSnapshot snapshot = hrDashboardService.buildEmployeeObligationsOverview(status);

        model.addAttribute("activePage", "personnel_obligations");
        model.addAttribute("snapshot", snapshot);

        return "admin/personnel_obligations";
    }

    @GetMapping("/employees/{employeeId}")
    public String showEmployeeProfile(
            @PathVariable("employeeId") Long employeeId,
            @RequestParam(name = "year", required = false) Integer year,
            @RequestParam(name = "month", required = false) Integer month,
            Model model,
            RedirectAttributes redirectAttributes
    ) {
        try {
            HrEmployeeProfile profile = hrDashboardService.buildEmployeeProfile(employeeId, year, month);

            model.addAttribute("activePage", "personnel_employee_profile");
            model.addAttribute("profile", profile);

            return "admin/personnel_employee_profile";
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("message", "No se encontró el trabajador seleccionado.");
            redirectAttributes.addFlashAttribute("messageType", "error");

            return "redirect:/admin/personnel";
        }
    }
}
