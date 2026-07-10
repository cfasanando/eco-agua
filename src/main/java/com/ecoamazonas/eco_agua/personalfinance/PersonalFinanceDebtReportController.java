package com.ecoamazonas.eco_agua.personalfinance;

import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Set;

@Controller
@RequestMapping("/gasto-claro/debt-report")
public class PersonalFinanceDebtReportController {

    private final PersonalFinanceDebtReportService reportService;
    private final PersonalFinanceDebtReportPdfService pdfService;
    private final PersonalFinanceDebtReportFormatter formatter;

    public PersonalFinanceDebtReportController(
            PersonalFinanceDebtReportService reportService,
            PersonalFinanceDebtReportPdfService pdfService,
            PersonalFinanceDebtReportFormatter formatter
    ) {
        this.reportService = reportService;
        this.pdfService = pdfService;
        this.formatter = formatter;
    }

    @GetMapping
    public String preview(
            @RequestParam(name = "year", required = false) Integer year,
            @RequestParam(name = "month", required = false) Integer month,
            @RequestParam(name = "months", defaultValue = "12") Integer months,
            @RequestParam(name = "cutoffDate", required = false) LocalDate cutoffDate,
            @RequestParam(name = "shared", defaultValue = "false") boolean shared,
            @RequestParam(name = "includeSchedules", defaultValue = "true") boolean includeSchedules,
            @RequestParam(name = "includePrivateNotes", defaultValue = "true") boolean includePrivateNotes,
            @RequestParam(name = "anonymizeContacts", defaultValue = "false") boolean anonymizeContacts,
            @RequestParam(name = "sections", required = false) List<String> sections,
            Model model
    ) {
        PersonalFinanceDebtReportOptions options = options(
                year, month, months, cutoffDate, shared, includeSchedules, includePrivateNotes, anonymizeContacts, sections
        );
        PersonalFinanceDebtReport report = reportService.build(options);
        model.addAttribute("activePage", "gasto_claro_debt_report");
        model.addAttribute("report", report);
        model.addAttribute("options", options);
        model.addAttribute("reportSections", PersonalFinanceDebtReportSection.values());
        model.addAttribute("fmt", formatter);
        model.addAttribute("pdfMode", false);
        return "personal_finance/debt_report_preview";
    }

    @GetMapping(value = "/pdf", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> pdf(
            @RequestParam(name = "year", required = false) Integer year,
            @RequestParam(name = "month", required = false) Integer month,
            @RequestParam(name = "months", defaultValue = "12") Integer months,
            @RequestParam(name = "cutoffDate", required = false) LocalDate cutoffDate,
            @RequestParam(name = "shared", defaultValue = "false") boolean shared,
            @RequestParam(name = "includeSchedules", defaultValue = "true") boolean includeSchedules,
            @RequestParam(name = "includePrivateNotes", defaultValue = "true") boolean includePrivateNotes,
            @RequestParam(name = "anonymizeContacts", defaultValue = "false") boolean anonymizeContacts,
            @RequestParam(name = "sections", required = false) List<String> sections
    ) {
        PersonalFinanceDebtReportOptions options = options(
                year, month, months, cutoffDate, shared, includeSchedules, includePrivateNotes, anonymizeContacts, sections
        );
        PersonalFinanceDebtReport report = reportService.build(options);
        byte[] pdf = pdfService.render(report, options);
        String variant = options.shared() ? "compartible" : "privado";
        String filename = "gasto-claro-" + options.filenameSlug() + "-" + variant + "-" + options.cutoffDate() + ".pdf";
        ContentDisposition disposition = ContentDisposition.attachment()
                .filename(filename, StandardCharsets.UTF_8)
                .build();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .contentType(MediaType.APPLICATION_PDF)
                .contentLength(pdf.length)
                .body(pdf);
    }

    private PersonalFinanceDebtReportOptions options(
            Integer year,
            Integer month,
            Integer months,
            LocalDate cutoffDate,
            boolean shared,
            boolean includeSchedules,
            boolean includePrivateNotes,
            boolean anonymizeContacts,
            List<String> sections
    ) {
        LocalDate effectiveCutoff = cutoffDate == null ? LocalDate.now() : cutoffDate;
        int effectiveYear = year == null ? effectiveCutoff.getYear() : year;
        int effectiveMonth = month == null ? effectiveCutoff.getMonthValue() : Math.max(1, Math.min(12, month));
        int effectiveMonths = months == null ? 12 : months;
        Set<PersonalFinanceDebtReportSection> selectedSections = PersonalFinanceDebtReportSection.parse(sections);
        return new PersonalFinanceDebtReportOptions(
                effectiveCutoff,
                YearMonth.of(effectiveYear, effectiveMonth),
                effectiveMonths,
                shared ? PersonalFinanceDebtReportVersion.SHARED : PersonalFinanceDebtReportVersion.PRIVATE,
                includeSchedules,
                includePrivateNotes,
                anonymizeContacts,
                selectedSections
        );
    }
}
