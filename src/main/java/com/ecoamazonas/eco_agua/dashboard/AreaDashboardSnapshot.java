package com.ecoamazonas.eco_agua.dashboard;

import java.time.LocalDate;
import java.util.List;

public class AreaDashboardSnapshot {

    private final LocalDate startDate;
    private final LocalDate endDate;
    private final List<AreaDashboardSection> sections;
    private final List<String> sectionErrors;

    public AreaDashboardSnapshot(
            LocalDate startDate,
            LocalDate endDate,
            List<AreaDashboardSection> sections,
            List<String> sectionErrors
    ) {
        this.startDate = startDate;
        this.endDate = endDate;
        this.sections = sections != null ? sections : List.of();
        this.sectionErrors = sectionErrors != null ? sectionErrors : List.of();
    }

    public static AreaDashboardSnapshot empty(LocalDate startDate, LocalDate endDate, String errorMessage) {
        return new AreaDashboardSnapshot(
                startDate,
                endDate,
                List.of(),
                errorMessage == null || errorMessage.isBlank() ? List.of() : List.of(errorMessage)
        );
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public List<AreaDashboardSection> getSections() {
        return sections;
    }

    public List<String> getSectionErrors() {
        return sectionErrors;
    }

    public boolean hasSectionErrors() {
        return !sectionErrors.isEmpty();
    }

    public int getSectionCount() {
        return sections.size();
    }
}
