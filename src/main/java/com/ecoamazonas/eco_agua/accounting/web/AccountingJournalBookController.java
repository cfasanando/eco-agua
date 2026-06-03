package com.ecoamazonas.eco_agua.accounting.web;

import com.ecoamazonas.eco_agua.accounting.AccountingJournalBookSnapshot;
import com.ecoamazonas.eco_agua.accounting.AccountingJournalEntry;
import com.ecoamazonas.eco_agua.accounting.AccountingJournalEntryStatus;
import com.ecoamazonas.eco_agua.accounting.AccountingJournalLine;
import com.ecoamazonas.eco_agua.accounting.service.AccountingJournalBookService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Controller
public class AccountingJournalBookController {

    private static final DateTimeFormatter LABEL_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final AccountingJournalBookService journalBookService;

    public AccountingJournalBookController(AccountingJournalBookService journalBookService) {
        this.journalBookService = journalBookService;
    }

    @GetMapping("/accounting/journal-book")
    public String index(
            @RequestParam(name = "fromDate", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(name = "toDate", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            @RequestParam(name = "status", required = false, defaultValue = "ALL") String status,
            Model model
    ) {
        AccountingJournalBookSnapshot snapshot = journalBookService.build(fromDate, toDate, status);

        model.addAttribute("activePage", "accounting_journal_book");
        model.addAttribute("snapshot", snapshot);
        model.addAttribute("fromDate", snapshot.getStartDate());
        model.addAttribute("toDate", snapshot.getEndDate());
        model.addAttribute("selectedStatus", snapshot.getStatusFilter());
        model.addAttribute("entryStatuses", List.of(AccountingJournalEntryStatus.DRAFT, AccountingJournalEntryStatus.POSTED, AccountingJournalEntryStatus.CANCELLED));
        model.addAttribute("periodLabel", buildPeriodLabel(snapshot.getStartDate(), snapshot.getEndDate()));

        return "accounting/journal_book";
    }

    @GetMapping(value = "/accounting/journal-book/export.csv", produces = "text/csv")
    public ResponseEntity<byte[]> exportCsv(
            @RequestParam(name = "fromDate", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(name = "toDate", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            @RequestParam(name = "status", required = false, defaultValue = "ALL") String status
    ) {
        AccountingJournalBookSnapshot snapshot = journalBookService.build(fromDate, toDate, status);
        StringBuilder csv = new StringBuilder();

        AccountingCsvExportHelper.row(csv, "Reporte", "Libro diario interno");
        AccountingCsvExportHelper.row(csv, "Período", buildPeriodLabel(snapshot.getStartDate(), snapshot.getEndDate()));
        AccountingCsvExportHelper.row(csv, "Estado", snapshot.getStatusFilter());
        AccountingCsvExportHelper.row(csv);
        AccountingCsvExportHelper.row(csv, "Fecha", "Asiento", "Estado", "Origen", "Referencia", "Glosa", "Cuenta", "Descripción línea", "Debe", "Haber");

        for (AccountingJournalEntry entry : snapshot.getEntries()) {
            if (entry.getLines().isEmpty()) {
                AccountingCsvExportHelper.row(csv,
                        AccountingCsvExportHelper.date(entry.getEntryDate()),
                        entry.getId(),
                        statusLabel(entry),
                        sourceLabel(entry),
                        entry.getSourceReferenceCode(),
                        entry.getDescription(),
                        "", "", "0.00", "0.00"
                );
                continue;
            }

            for (AccountingJournalLine line : entry.getLines()) {
                AccountingCsvExportHelper.row(csv,
                        AccountingCsvExportHelper.date(entry.getEntryDate()),
                        entry.getId(),
                        statusLabel(entry),
                        sourceLabel(entry),
                        entry.getSourceReferenceCode(),
                        entry.getDescription(),
                        line.getAccount() == null ? "" : line.getAccount().getCode() + " - " + line.getAccount().getName(),
                        line.getDescription(),
                        AccountingCsvExportHelper.money(line.getDebitAmount()),
                        AccountingCsvExportHelper.money(line.getCreditAmount())
                );
            }
        }

        AccountingCsvExportHelper.row(csv);
        AccountingCsvExportHelper.row(csv, "Total debe", AccountingCsvExportHelper.money(snapshot.getSummary().getTotalDebit()));
        AccountingCsvExportHelper.row(csv, "Total haber", AccountingCsvExportHelper.money(snapshot.getSummary().getTotalCredit()));
        AccountingCsvExportHelper.row(csv, "Diferencia", AccountingCsvExportHelper.money(snapshot.getSummary().getDifference()));

        return AccountingCsvExportHelper.csv("libro-diario-interno.csv", csv);
    }

    private String buildPeriodLabel(LocalDate startDate, LocalDate endDate) {
        if (startDate == null || endDate == null) {
            return "sin período definido";
        }
        if (startDate.equals(endDate)) {
            return startDate.format(LABEL_FORMATTER);
        }
        return startDate.format(LABEL_FORMATTER) + " al " + endDate.format(LABEL_FORMATTER);
    }

    private String statusLabel(AccountingJournalEntry entry) {
        return entry.getStatus() == null ? "Sin estado" : entry.getStatus().getLabel();
    }

    private String sourceLabel(AccountingJournalEntry entry) {
        if (entry.getSourceEvent() != null) {
            return entry.getSourceEvent().getLabel();
        }
        return entry.getSourceType() == null ? "Sin origen" : entry.getSourceType().getLabel();
    }
}
