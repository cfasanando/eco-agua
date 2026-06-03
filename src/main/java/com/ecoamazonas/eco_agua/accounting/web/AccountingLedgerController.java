package com.ecoamazonas.eco_agua.accounting.web;

import com.ecoamazonas.eco_agua.accounting.AccountingJournalEntryStatus;
import com.ecoamazonas.eco_agua.accounting.AccountingLedgerAccountGroup;
import com.ecoamazonas.eco_agua.accounting.AccountingLedgerRow;
import com.ecoamazonas.eco_agua.accounting.AccountingLedgerSnapshot;
import com.ecoamazonas.eco_agua.accounting.repository.AccountingAccountRepository;
import com.ecoamazonas.eco_agua.accounting.service.AccountingLedgerService;
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
public class AccountingLedgerController {

    private static final DateTimeFormatter LABEL_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final AccountingLedgerService ledgerService;
    private final AccountingAccountRepository accountRepository;

    public AccountingLedgerController(
            AccountingLedgerService ledgerService,
            AccountingAccountRepository accountRepository
    ) {
        this.ledgerService = ledgerService;
        this.accountRepository = accountRepository;
    }

    @GetMapping("/accounting/ledger")
    public String index(
            @RequestParam(name = "fromDate", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(name = "toDate", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            @RequestParam(name = "status", required = false, defaultValue = "ALL") String status,
            @RequestParam(name = "accountId", required = false) Long accountId,
            Model model
    ) {
        AccountingLedgerSnapshot snapshot = ledgerService.build(fromDate, toDate, status, accountId);

        model.addAttribute("activePage", "accounting_ledger");
        model.addAttribute("snapshot", snapshot);
        model.addAttribute("fromDate", snapshot.getStartDate());
        model.addAttribute("toDate", snapshot.getEndDate());
        model.addAttribute("selectedStatus", snapshot.getStatusFilter());
        model.addAttribute("selectedAccountId", snapshot.getAccountId());
        model.addAttribute("accounts", accountRepository.findAllByOrderByCodeAsc());
        model.addAttribute("entryStatuses", List.of(AccountingJournalEntryStatus.DRAFT, AccountingJournalEntryStatus.POSTED, AccountingJournalEntryStatus.CANCELLED));
        model.addAttribute("periodLabel", buildPeriodLabel(snapshot.getStartDate(), snapshot.getEndDate()));

        return "accounting/ledger";
    }

    @GetMapping(value = "/accounting/ledger/export.csv", produces = "text/csv")
    public ResponseEntity<byte[]> exportCsv(
            @RequestParam(name = "fromDate", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(name = "toDate", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            @RequestParam(name = "status", required = false, defaultValue = "ALL") String status,
            @RequestParam(name = "accountId", required = false) Long accountId
    ) {
        AccountingLedgerSnapshot snapshot = ledgerService.build(fromDate, toDate, status, accountId);
        StringBuilder csv = new StringBuilder();

        AccountingCsvExportHelper.row(csv, "Reporte", "Libro mayor interno");
        AccountingCsvExportHelper.row(csv, "Período", buildPeriodLabel(snapshot.getStartDate(), snapshot.getEndDate()));
        AccountingCsvExportHelper.row(csv, "Estado", snapshot.getStatusFilter());
        AccountingCsvExportHelper.row(csv);
        AccountingCsvExportHelper.row(csv, "Cuenta", "Tipo", "Fecha", "Asiento", "Estado", "Origen", "Glosa", "Descripción línea", "Debe", "Haber", "Saldo acumulado", "Lado saldo");

        for (AccountingLedgerAccountGroup group : snapshot.getAccountGroups()) {
            for (AccountingLedgerRow row : group.getRows()) {
                AccountingCsvExportHelper.row(csv,
                        group.getAccountCode() + " - " + group.getAccountName(),
                        group.getAccountTypeLabel(),
                        AccountingCsvExportHelper.date(row.getEntryDate()),
                        row.getEntryId(),
                        row.getStatusLabel(),
                        row.getSourceLabel(),
                        row.getEntryDescription(),
                        row.getLineDescription(),
                        AccountingCsvExportHelper.money(row.getDebitAmount()),
                        AccountingCsvExportHelper.money(row.getCreditAmount()),
                        AccountingCsvExportHelper.money(row.getRunningBalance()),
                        row.getBalanceSideLabel()
                );
            }
        }

        AccountingCsvExportHelper.row(csv);
        AccountingCsvExportHelper.row(csv, "Total debe", AccountingCsvExportHelper.money(snapshot.getSummary().getTotalDebit()));
        AccountingCsvExportHelper.row(csv, "Total haber", AccountingCsvExportHelper.money(snapshot.getSummary().getTotalCredit()));
        AccountingCsvExportHelper.row(csv, "Diferencia", AccountingCsvExportHelper.money(snapshot.getSummary().getDifference()));

        return AccountingCsvExportHelper.csv("libro-mayor-interno.csv", csv);
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
