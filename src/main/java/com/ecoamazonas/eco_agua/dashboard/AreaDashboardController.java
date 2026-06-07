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
public class AreaDashboardController {

    private static final Logger log = LoggerFactory.getLogger(AreaDashboardController.class);

    private final AreaDashboardService areaDashboardService;

    public AreaDashboardController(AreaDashboardService areaDashboardService) {
        this.areaDashboardService = areaDashboardService;
    }

    @GetMapping("/dashboard/areas")
    public String dashboard(
            @RequestParam(name = "start", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(name = "end", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            Model model
    ) {
        LocalDate effectiveEnd = endDate != null ? endDate : LocalDate.now();
        LocalDate effectiveStart = startDate != null ? startDate : effectiveEnd.withDayOfMonth(1);

        if (effectiveStart.isAfter(effectiveEnd)) {
            LocalDate temp = effectiveStart;
            effectiveStart = effectiveEnd;
            effectiveEnd = temp;
        }

        AreaDashboardSnapshot snapshot;
        try {
            snapshot = areaDashboardService.buildSnapshot(effectiveStart, effectiveEnd);
        } catch (Exception exception) {
            log.warn("Area dashboard could not be loaded", exception);
            snapshot = AreaDashboardSnapshot.empty(
                    effectiveStart,
                    effectiveEnd,
                    "El dashboard por áreas no pudo cargar todos los datos. Revisa los módulos por separado."
            );
        }

        long rangeDays = ChronoUnit.DAYS.between(snapshot.getStartDate(), snapshot.getEndDate()) + 1;

        model.addAttribute("activePage", "area_dashboard");
        model.addAttribute("snapshot", snapshot);
        model.addAttribute("startDate", snapshot.getStartDate());
        model.addAttribute("endDate", snapshot.getEndDate());
        model.addAttribute("rangeDays", rangeDays);

        return "dashboard/area_dashboard";
    }
}
