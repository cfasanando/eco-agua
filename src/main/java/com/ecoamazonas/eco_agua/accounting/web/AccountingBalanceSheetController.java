package com.ecoamazonas.eco_agua.accounting.web;

import com.ecoamazonas.eco_agua.accounting.AccountingBalanceSheetRow;
import com.ecoamazonas.eco_agua.accounting.AccountingBalanceSheetSnapshot;
import com.ecoamazonas.eco_agua.accounting.AccountingJournalEntryStatus;
import com.ecoamazonas.eco_agua.accounting.service.AccountingBalanceSheetService;
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
public class AccountingBalanceSheetController {

    private static final DateTimeFormatter LABEL_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final AccountingBalanceSheetService balanceSheetService;

    public AccountingBalanceSheetController(AccountingBalanceSheetService balanceSheetService) {
        this.balanceSheetService = balanceSheetService;
    }

    @GetMapping("/accounting/balance-sheet")
    public String index(
            @RequestParam(name = "fromDate", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(name = "toDate", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            @RequestParam(name = "status", required = false, defaultValue = "POSTED") String status,
            Model model
    ) {
        AccountingBalanceSheetSnapshot snapshot = balanceSheetService.build(fromDate, toDate, status);

        model.addAttribute("activePage", "accounting_balance_sheet");
        model.addAttribute("snapshot", snapshot);
        model.addAttribute("fromDate", snapshot.getStartDate());
        model.addAttribute("toDate", snapshot.getEndDate());
        model.addAttribute("selectedStatus", snapshot.getStatusFilter());
        model.addAttribute("entryStatuses", List.of(AccountingJournalEntryStatus.DRAFT, AccountingJournalEntryStatus.POSTED, AccountingJournalEntryStatus.CANCELLED));
        model.addAttribute("periodLabel", buildPeriodLabel(snapshot.getStartDate(), snapshot.getEndDate()));

        return "accounting/balance_sheet";
    }

    @GetMapping(value = "/accounting/balance-sheet/export.csv", produces = "text/csv")
    public ResponseEntity<byte[]> exportCsv(
            @RequestParam(name = "fromDate", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(name = "toDate", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            @RequestParam(name = "status", required = false, defaultValue = "POSTED") String status
    ) {
        AccountingBalanceSheetSnapshot snapshot = balanceSheetService.build(fromDate, toDate, status);
        StringBuilder csv = new StringBuilder();

        AccountingCsvExportHelper.row(csv, "Reporte", "Balance general interno");
        AccountingCsvExportHelper.row(csv, "Período", buildPeriodLabel(snapshot.getStartDate(), snapshot.getEndDate()));
        AccountingCsvExportHelper.row(csv, "Estado", snapshot.getStatusFilter());
        AccountingCsvExportHelper.row(csv);
        AccountingCsvExportHelper.row(csv, "Sección", "Cuenta", "Nombre", "Tipo", "Movimientos", "Debe", "Haber", "Importe");

        appendBalanceRows(csv, snapshot.getAssetRows());
        appendBalanceRows(csv, snapshot.getLiabilityRows());
        appendBalanceRows(csv, snapshot.getEquityRows());

        AccountingCsvExportHelper.row(csv);
        AccountingCsvExportHelper.row(csv, "Total activo", AccountingCsvExportHelper.money(snapshot.getSummary().getTotalAssets()));
        AccountingCsvExportHelper.row(csv, "Total pasivo", AccountingCsvExportHelper.money(snapshot.getSummary().getTotalLiabilities()));
        AccountingCsvExportHelper.row(csv, "Total patrimonio", AccountingCsvExportHelper.money(snapshot.getSummary().getTotalEquity()));
        AccountingCsvExportHelper.row(csv, "Pasivo + patrimonio", AccountingCsvExportHelper.money(snapshot.getSummary().getTotalLiabilitiesAndEquity()));
        AccountingCsvExportHelper.row(csv, "Diferencia", AccountingCsvExportHelper.money(snapshot.getSummary().getAbsoluteDifference()));
        AccountingCsvExportHelper.row(csv, snapshot.getSummary().getCurrentPeriodResultLabel(), AccountingCsvExportHelper.money(snapshot.getSummary().getAbsoluteCurrentPeriodResult()));

        return AccountingCsvExportHelper.csv("balance-general-interno.csv", csv);
    }

    private void appendBalanceRows(StringBuilder csv, List<AccountingBalanceSheetRow> rows) {
        for (AccountingBalanceSheetRow row : rows) {
            AccountingCsvExportHelper.row(csv,
                    row.getSectionLabel(),
                    row.getAccountCode(),
                    row.getAccountName(),
                    row.getAccountTypeLabel(),
                    row.getMovementCount(),
                    AccountingCsvExportHelper.money(row.getTotalDebit()),
                    AccountingCsvExportHelper.money(row.getTotalCredit()),
                    AccountingCsvExportHelper.money(row.getAmount())
            );
        }
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
}
