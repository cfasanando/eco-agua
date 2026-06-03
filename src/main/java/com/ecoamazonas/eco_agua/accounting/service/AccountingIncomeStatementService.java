package com.ecoamazonas.eco_agua.accounting.service;

import com.ecoamazonas.eco_agua.accounting.AccountingAccount;
import com.ecoamazonas.eco_agua.accounting.AccountingAccountType;
import com.ecoamazonas.eco_agua.accounting.AccountingIncomeStatementRow;
import com.ecoamazonas.eco_agua.accounting.AccountingIncomeStatementSnapshot;
import com.ecoamazonas.eco_agua.accounting.AccountingIncomeStatementSummary;
import com.ecoamazonas.eco_agua.accounting.AccountingJournalEntry;
import com.ecoamazonas.eco_agua.accounting.AccountingJournalEntryStatus;
import com.ecoamazonas.eco_agua.accounting.AccountingJournalLine;
import com.ecoamazonas.eco_agua.accounting.repository.AccountingJournalEntryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class AccountingIncomeStatementService {

    private static final String SECTION_SALES_INCOME = "SALES_INCOME";
    private static final String SECTION_SALES_DEDUCTIONS = "SALES_DEDUCTIONS";
    private static final String SECTION_OTHER_INCOME = "OTHER_INCOME";
    private static final String SECTION_COSTS = "COSTS";
    private static final String SECTION_EXPENSES = "EXPENSES";

    private final AccountingJournalEntryRepository journalEntryRepository;

    public AccountingIncomeStatementService(AccountingJournalEntryRepository journalEntryRepository) {
        this.journalEntryRepository = journalEntryRepository;
    }

    @Transactional(readOnly = true)
    public AccountingIncomeStatementSnapshot build(LocalDate startDate, LocalDate endDate, String statusFilter) {
        LocalDate today = LocalDate.now();
        LocalDate normalizedStart = startDate != null ? startDate : today.withDayOfMonth(1);
        LocalDate normalizedEnd = endDate != null ? endDate : normalizedStart.withDayOfMonth(normalizedStart.lengthOfMonth());

        if (normalizedEnd.isBefore(normalizedStart)) {
            LocalDate tmp = normalizedStart;
            normalizedStart = normalizedEnd;
            normalizedEnd = tmp;
        }

        String normalizedStatus = normalizeStatusFilter(statusFilter);
        List<AccountingJournalEntry> entries = journalEntryRepository
                .findByEntryDateBetweenOrderByEntryDateAscIdAsc(normalizedStart, normalizedEnd)
                .stream()
                .filter(entry -> matchesStatus(entry, normalizedStatus))
                .toList();

        Map<Long, AccountingIncomeStatementRow> rowsByAccount = new LinkedHashMap<>();
        for (AccountingJournalEntry entry : entries) {
            for (AccountingJournalLine line : entry.getLines()) {
                AccountingAccount account = line.getAccount();
                if (account == null || account.getId() == null || !isIncomeStatementAccount(account)) {
                    continue;
                }

                AccountingIncomeStatementRow row = rowsByAccount.computeIfAbsent(account.getId(), id -> buildRow(account));
                row.addMovement(line.getDebitAmount(), line.getCreditAmount());
            }
        }

        List<AccountingIncomeStatementRow> salesIncomeRows = new ArrayList<>();
        List<AccountingIncomeStatementRow> salesDeductionRows = new ArrayList<>();
        List<AccountingIncomeStatementRow> otherIncomeRows = new ArrayList<>();
        List<AccountingIncomeStatementRow> costRows = new ArrayList<>();
        List<AccountingIncomeStatementRow> expenseRows = new ArrayList<>();

        for (AccountingIncomeStatementRow row : rowsByAccount.values()) {
            calculateAmount(row);
            switch (row.getSectionCode()) {
                case SECTION_SALES_INCOME -> salesIncomeRows.add(row);
                case SECTION_SALES_DEDUCTIONS -> salesDeductionRows.add(row);
                case SECTION_OTHER_INCOME -> otherIncomeRows.add(row);
                case SECTION_COSTS -> costRows.add(row);
                default -> expenseRows.add(row);
            }
        }

        Comparator<AccountingIncomeStatementRow> accountComparator = Comparator
                .comparing(AccountingIncomeStatementRow::getAccountCode, Comparator.nullsLast(String::compareTo))
                .thenComparing(AccountingIncomeStatementRow::getAccountName, Comparator.nullsLast(String::compareTo));

        salesIncomeRows.sort(accountComparator);
        salesDeductionRows.sort(accountComparator);
        otherIncomeRows.sort(accountComparator);
        costRows.sort(accountComparator);
        expenseRows.sort(accountComparator);

        AccountingIncomeStatementSnapshot snapshot = new AccountingIncomeStatementSnapshot();
        snapshot.setStartDate(normalizedStart);
        snapshot.setEndDate(normalizedEnd);
        snapshot.setStatusFilter(normalizedStatus);
        snapshot.setSalesIncomeRows(salesIncomeRows);
        snapshot.setSalesDeductionRows(salesDeductionRows);
        snapshot.setOtherIncomeRows(otherIncomeRows);
        snapshot.setCostRows(costRows);
        snapshot.setExpenseRows(expenseRows);
        snapshot.setSummary(buildSummary(salesIncomeRows, salesDeductionRows, otherIncomeRows, costRows, expenseRows));
        return snapshot;
    }

    private AccountingIncomeStatementRow buildRow(AccountingAccount account) {
        AccountingIncomeStatementRow row = new AccountingIncomeStatementRow();
        row.setAccountId(account.getId());
        row.setAccountCode(account.getCode());
        row.setAccountName(account.getName());

        String section = resolveSection(account);
        row.setSectionCode(section);
        row.setSectionLabel(resolveSectionLabel(section));
        return row;
    }

    private AccountingIncomeStatementSummary buildSummary(
            List<AccountingIncomeStatementRow> salesIncomeRows,
            List<AccountingIncomeStatementRow> salesDeductionRows,
            List<AccountingIncomeStatementRow> otherIncomeRows,
            List<AccountingIncomeStatementRow> costRows,
            List<AccountingIncomeStatementRow> expenseRows
    ) {
        AccountingIncomeStatementSummary summary = new AccountingIncomeStatementSummary();
        summary.setSalesIncome(sum(salesIncomeRows));
        summary.setSalesDeductions(sum(salesDeductionRows));
        summary.setOtherIncome(sum(otherIncomeRows));
        summary.setCostOfSales(sum(costRows));
        summary.setOperatingExpenses(sum(expenseRows));
        summary.setTotalAccounts(
                salesIncomeRows.size()
                        + salesDeductionRows.size()
                        + otherIncomeRows.size()
                        + costRows.size()
                        + expenseRows.size()
        );
        summary.setTotalMovements(
                movementCount(salesIncomeRows)
                        + movementCount(salesDeductionRows)
                        + movementCount(otherIncomeRows)
                        + movementCount(costRows)
                        + movementCount(expenseRows)
        );
        return summary;
    }

    private BigDecimal sum(List<AccountingIncomeStatementRow> rows) {
        return rows.stream()
                .map(AccountingIncomeStatementRow::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private int movementCount(List<AccountingIncomeStatementRow> rows) {
        return rows.stream()
                .mapToInt(AccountingIncomeStatementRow::getMovementCount)
                .sum();
    }

    private void calculateAmount(AccountingIncomeStatementRow row) {
        if (SECTION_SALES_INCOME.equals(row.getSectionCode()) || SECTION_OTHER_INCOME.equals(row.getSectionCode())) {
            row.setAmount(row.getTotalCredit().subtract(row.getTotalDebit()));
            return;
        }
        row.setAmount(row.getTotalDebit().subtract(row.getTotalCredit()));
    }

    private boolean isIncomeStatementAccount(AccountingAccount account) {
        return account.getType() == AccountingAccountType.INCOME || account.getType() == AccountingAccountType.EXPENSE;
    }

    private String resolveSection(AccountingAccount account) {
        String code = normalize(account.getCode());
        String name = normalize(account.getName());

        if (account.getType() == AccountingAccountType.INCOME) {
            if (code.startsWith("70") || name.contains("venta")) {
                return SECTION_SALES_INCOME;
            }
            return SECTION_OTHER_INCOME;
        }

        if (isSalesDeduction(code, name)) {
            return SECTION_SALES_DEDUCTIONS;
        }
        if (isCostOrPurchase(code, name)) {
            return SECTION_COSTS;
        }
        return SECTION_EXPENSES;
    }

    private boolean isSalesDeduction(String code, String name) {
        return code.startsWith("70")
                || name.contains("descuento")
                || name.contains("rebaja")
                || name.contains("devolucion venta")
                || name.contains("devolución venta");
    }

    private boolean isCostOrPurchase(String code, String name) {
        return code.startsWith("60")
                || code.startsWith("61")
                || code.startsWith("69")
                || name.contains("compra")
                || name.contains("costo")
                || name.contains("mercader");
    }

    private String resolveSectionLabel(String section) {
        return switch (section) {
            case SECTION_SALES_INCOME -> "Ingresos por ventas";
            case SECTION_SALES_DEDUCTIONS -> "Deducciones de ventas";
            case SECTION_OTHER_INCOME -> "Otros ingresos";
            case SECTION_COSTS -> "Compras y costo de ventas";
            default -> "Gastos operativos";
        };
    }

    private boolean matchesStatus(AccountingJournalEntry entry, String statusFilter) {
        if ("ALL".equals(statusFilter)) {
            return true;
        }
        return entry.getStatus() != null && entry.getStatus().name().equals(statusFilter);
    }

    private String normalizeStatusFilter(String statusFilter) {
        if (statusFilter == null || statusFilter.isBlank()) {
            return AccountingJournalEntryStatus.POSTED.name();
        }
        if ("ALL".equalsIgnoreCase(statusFilter.trim())) {
            return "ALL";
        }

        try {
            return AccountingJournalEntryStatus.valueOf(statusFilter.trim().toUpperCase()).name();
        } catch (IllegalArgumentException ex) {
            return AccountingJournalEntryStatus.POSTED.name();
        }
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }
}
