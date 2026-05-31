package com.ecoamazonas.eco_agua.dashboard;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

@Controller
public class BusinessOverviewController {

    private final BusinessOverviewService businessOverviewService;

    public BusinessOverviewController(BusinessOverviewService businessOverviewService) {
        this.businessOverviewService = businessOverviewService;
    }

    @GetMapping("/dashboard/business-overview")
    public String businessOverview(
            @RequestParam(name = "days", required = false) Integer days,
            @RequestParam(name = "from", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(name = "to", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            Model model
    ) {
        Integer selectedPresetDays = normalizePresetDays(days);
        LocalDate effectiveTo = to;
        LocalDate effectiveFrom = from;

        if (selectedPresetDays != null) {
            effectiveTo = LocalDate.now();
            effectiveFrom = effectiveTo.minusDays(selectedPresetDays - 1L);
        }

        BusinessOverviewSnapshot snapshot = businessOverviewService.buildSnapshot(effectiveFrom, effectiveTo);

        long currentRangeDays = ChronoUnit.DAYS.between(snapshot.getFromDate(), snapshot.getToDate()) + 1;

        model.addAttribute("activePage", "business_overview");
        model.addAttribute("snapshot", snapshot);
        model.addAttribute("selectedPresetDays", selectedPresetDays);
        model.addAttribute("currentRangeDays", currentRangeDays);

        return "dashboard/business_overview";
    }

    private Integer normalizePresetDays(Integer days) {
        if (days == null) {
            return null;
        }

        if (days == 7 || days == 30 || days == 90) {
            return days;
        }

        return null;
    }
}
