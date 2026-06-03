package com.ecoamazonas.eco_agua.accounting.web;

import com.ecoamazonas.eco_agua.accounting.AccountingIncomeStatementRow;
import com.ecoamazonas.eco_agua.accounting.AccountingIncomeStatementSnapshot;
import com.ecoamazonas.eco_agua.accounting.AccountingJournalEntryStatus;
import com.ecoamazonas.eco_agua.accounting.service.AccountingIncomeStatementService;
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
public class AccountingIncomeStatementController {

    private static final DateTimeFormatter LABEL_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final AccountingIncomeStatementService incomeStatementService;

    public AccountingIncomeStatementController(AccountingIncomeStatementService incomeStatementService) {
        this.incomeStatementService = incomeStatementService;
    }

    @GetMapping("/accounting/income-statement")
    public String index(
            @RequestParam(name = "fromDate", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(name = "toDate", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            @RequestParam(name = "status", required = false, defaultValue = "POSTED") String status,
            Model model
    ) {
        AccountingIncomeStatementSnapshot snapshot = incomeStatementService.build(fromDate, toDate, status);

        model.addAttribute("activePage", "accounting_income_statement");
        model.addAttribute("snapshot", snapshot);
        model.addAttribute("fromDate", snapshot.getStartDate());
        model.addAttribute("toDate", snapshot.getEndDate());
        model.addAttribute("selectedStatus", snapshot.getStatusFilter());
        model.addAttribute("entryStatuses", List.of(AccountingJournalEntryStatus.DRAFT, AccountingJournalEntryStatus.POSTED, AccountingJournalEntryStatus.CANCELLED));
        model.addAttribute("periodLabel", buildPeriodLabel(snapshot.getStartDate(), snapshot.getEndDate()));

        return "accounting/income_statement";
    }

    @GetMapping(value = "/accounting/income-statement/export.csv", produces = "text/csv")
    public ResponseEntity<byte[]> exportCsv(
            @RequestParam(name = "fromDate", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(name = "toDate", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            @RequestParam(name = "status", required = false, defaultValue = "POSTED") String status
    ) {
        AccountingIncomeStatementSnapshot snapshot = incomeStatementService.build(fromDate, toDate, status);
        StringBuilder csv = new StringBuilder();

        AccountingCsvExportHelper.row(csv, "Reporte", "Estado de resultados interno");
        AccountingCsvExportHelper.row(csv, "Período", buildPeriodLabel(snapshot.getStartDate(), snapshot.getEndDate()));
        AccountingCsvExportHelper.row(csv, "Estado", snapshot.getStatusFilter());
        AccountingCsvExportHelper.row(csv);
        AccountingCsvExportHelper.row(csv, "Sección", "Cuenta", "Nombre", "Movimientos", "Debe", "Haber", "Importe");

        appendIncomeStatementRows(csv, snapshot.getSalesIncomeRows());
        appendIncomeStatementRows(csv, snapshot.getSalesDeductionRows());
        appendIncomeStatementRows(csv, snapshot.getOtherIncomeRows());
        appendIncomeStatementRows(csv, snapshot.getCostRows());
        appendIncomeStatementRows(csv, snapshot.getExpenseRows());

        AccountingCsvExportHelper.row(csv);
        AccountingCsvExportHelper.row(csv, "Ventas brutas", AccountingCsvExportHelper.money(snapshot.getSummary().getSalesIncome()));
        AccountingCsvExportHelper.row(csv, "Deducciones de ventas", AccountingCsvExportHelper.money(snapshot.getSummary().getSalesDeductions()));
        AccountingCsvExportHelper.row(csv, "Ventas netas", AccountingCsvExportHelper.money(snapshot.getSummary().getNetSales()));
        AccountingCsvExportHelper.row(csv, "Otros ingresos", AccountingCsvExportHelper.money(snapshot.getSummary().getOtherIncome()));
        AccountingCsvExportHelper.row(csv, "Total ingresos", AccountingCsvExportHelper.money(snapshot.getSummary().getTotalIncome()));
        AccountingCsvExportHelper.row(csv, "Compras / costo", AccountingCsvExportHelper.money(snapshot.getSummary().getCostOfSales()));
        AccountingCsvExportHelper.row(csv, "Gastos operativos", AccountingCsvExportHelper.money(snapshot.getSummary().getOperatingExpenses()));
        AccountingCsvExportHelper.row(csv, "Utilidad bruta", AccountingCsvExportHelper.money(snapshot.getSummary().getGrossProfit()));
        AccountingCsvExportHelper.row(csv, snapshot.getSummary().getResultLabel(), AccountingCsvExportHelper.money(snapshot.getSummary().getAbsoluteNetResult()));

        return AccountingCsvExportHelper.csv("estado-resultados-interno.csv", csv);
    }

    private void appendIncomeStatementRows(StringBuilder csv, List<AccountingIncomeStatementRow> rows) {
        for (AccountingIncomeStatementRow row : rows) {
            AccountingCsvExportHelper.row(csv,
                    row.getSectionLabel(),
                    row.getAccountCode(),
                    row.getAccountName(),
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
