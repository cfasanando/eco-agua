package com.ecoamazonas.eco_agua.accounting.web;

import com.ecoamazonas.eco_agua.accounting.AccountingJournalEntryStatus;
import com.ecoamazonas.eco_agua.accounting.AccountingTrialBalanceRow;
import com.ecoamazonas.eco_agua.accounting.AccountingTrialBalanceSnapshot;
import com.ecoamazonas.eco_agua.accounting.service.AccountingTrialBalanceService;
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
public class AccountingTrialBalanceController {

    private static final DateTimeFormatter LABEL_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final AccountingTrialBalanceService trialBalanceService;

    public AccountingTrialBalanceController(AccountingTrialBalanceService trialBalanceService) {
        this.trialBalanceService = trialBalanceService;
    }

    @GetMapping("/accounting/trial-balance")
    public String index(
            @RequestParam(name = "fromDate", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(name = "toDate", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            @RequestParam(name = "status", required = false, defaultValue = "ALL") String status,
            Model model
    ) {
        AccountingTrialBalanceSnapshot snapshot = trialBalanceService.build(fromDate, toDate, status);

        model.addAttribute("activePage", "accounting_trial_balance");
        model.addAttribute("snapshot", snapshot);
        model.addAttribute("fromDate", snapshot.getStartDate());
        model.addAttribute("toDate", snapshot.getEndDate());
        model.addAttribute("selectedStatus", snapshot.getStatusFilter());
        model.addAttribute("entryStatuses", List.of(AccountingJournalEntryStatus.DRAFT, AccountingJournalEntryStatus.POSTED, AccountingJournalEntryStatus.CANCELLED));
        model.addAttribute("periodLabel", buildPeriodLabel(snapshot.getStartDate(), snapshot.getEndDate()));

        return "accounting/trial_balance";
    }

    @GetMapping(value = "/accounting/trial-balance/export.csv", produces = "text/csv")
    public ResponseEntity<byte[]> exportCsv(
            @RequestParam(name = "fromDate", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(name = "toDate", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            @RequestParam(name = "status", required = false, defaultValue = "ALL") String status
    ) {
        AccountingTrialBalanceSnapshot snapshot = trialBalanceService.build(fromDate, toDate, status);
        StringBuilder csv = new StringBuilder();

        AccountingCsvExportHelper.row(csv, "Reporte", "Balance de comprobación interno");
        AccountingCsvExportHelper.row(csv, "Período", buildPeriodLabel(snapshot.getStartDate(), snapshot.getEndDate()));
        AccountingCsvExportHelper.row(csv, "Estado", snapshot.getStatusFilter());
        AccountingCsvExportHelper.row(csv);
        AccountingCsvExportHelper.row(csv, "Cuenta", "Nombre", "Tipo", "Movimientos", "Debe", "Haber", "Saldo deudor", "Saldo acreedor", "Lado saldo");

        for (AccountingTrialBalanceRow row : snapshot.getRows()) {
            AccountingCsvExportHelper.row(csv,
                    row.getAccountCode(),
                    row.getAccountName(),
                    row.getAccountTypeLabel(),
                    row.getMovementCount(),
                    AccountingCsvExportHelper.money(row.getTotalDebit()),
                    AccountingCsvExportHelper.money(row.getTotalCredit()),
                    AccountingCsvExportHelper.money(row.getDebitBalance()),
                    AccountingCsvExportHelper.money(row.getCreditBalance()),
                    row.getBalanceSideLabel()
            );
        }

        AccountingCsvExportHelper.row(csv);
        AccountingCsvExportHelper.row(csv, "Total debe", AccountingCsvExportHelper.money(snapshot.getSummary().getTotalDebit()));
        AccountingCsvExportHelper.row(csv, "Total haber", AccountingCsvExportHelper.money(snapshot.getSummary().getTotalCredit()));
        AccountingCsvExportHelper.row(csv, "Saldo deudor", AccountingCsvExportHelper.money(snapshot.getSummary().getTotalDebitBalance()));
        AccountingCsvExportHelper.row(csv, "Saldo acreedor", AccountingCsvExportHelper.money(snapshot.getSummary().getTotalCreditBalance()));
        AccountingCsvExportHelper.row(csv, "Diferencia movimientos", AccountingCsvExportHelper.money(snapshot.getSummary().getMovementDifference()));
        AccountingCsvExportHelper.row(csv, "Diferencia saldos", AccountingCsvExportHelper.money(snapshot.getSummary().getBalanceDifference()));

        return AccountingCsvExportHelper.csv("balance-comprobacion-interno.csv", csv);
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
