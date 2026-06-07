package com.ecoamazonas.eco_agua.dashboard;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

@Controller
public class ManagementDashboardController {

    private static final Logger log = LoggerFactory.getLogger(ManagementDashboardController.class);

    private final ManagementDashboardService managementDashboardService;

    public ManagementDashboardController(ManagementDashboardService managementDashboardService) {
        this.managementDashboardService = managementDashboardService;
    }

    @GetMapping("/dashboard/business")
    public String dashboard(
            @RequestParam(name = "start", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(name = "end", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            Model model
    ) {
        LocalDate effectiveEnd = endDate != null ? endDate : LocalDate.now();
        LocalDate effectiveStart = startDate != null ? startDate : effectiveEnd.withDayOfMonth(1);

        if (effectiveStart.isAfter(effectiveEnd)) {
            LocalDate tmp = effectiveStart;
            effectiveStart = effectiveEnd;
            effectiveEnd = tmp;
        }

        ManagementDashboardSnapshot snapshot;
        try {
            snapshot = managementDashboardService.buildSnapshot(effectiveStart, effectiveEnd);
        } catch (Exception exception) {
            log.warn("Management dashboard could not be fully loaded", exception);
            snapshot = ManagementDashboardSnapshot.empty(
                    effectiveStart,
                    effectiveEnd,
                    "El dashboard no pudo cargar todos los datos. Revisa los módulos por separado y valida la consola."
            );
        }

        long rangeDays = ChronoUnit.DAYS.between(snapshot.getStartDate(), snapshot.getEndDate()) + 1;

        model.addAttribute("activePage", "management_dashboard");
        model.addAttribute("snapshot", snapshot);
        model.addAttribute("startDate", snapshot.getStartDate());
        model.addAttribute("endDate", snapshot.getEndDate());
        model.addAttribute("rangeDays", rangeDays);

        return "dashboard/management_dashboard";
    }
}
