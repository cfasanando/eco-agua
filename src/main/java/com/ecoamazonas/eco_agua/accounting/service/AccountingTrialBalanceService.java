package com.ecoamazonas.eco_agua.accounting.service;

import com.ecoamazonas.eco_agua.accounting.AccountingAccount;
import com.ecoamazonas.eco_agua.accounting.AccountingJournalEntry;
import com.ecoamazonas.eco_agua.accounting.AccountingJournalEntryStatus;
import com.ecoamazonas.eco_agua.accounting.AccountingJournalLine;
import com.ecoamazonas.eco_agua.accounting.AccountingTrialBalanceRow;
import com.ecoamazonas.eco_agua.accounting.AccountingTrialBalanceSnapshot;
import com.ecoamazonas.eco_agua.accounting.AccountingTrialBalanceSummary;
import com.ecoamazonas.eco_agua.accounting.repository.AccountingJournalEntryRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class AccountingTrialBalanceService {

    private final AccountingJournalEntryRepository journalEntryRepository;

    public AccountingTrialBalanceService(AccountingJournalEntryRepository journalEntryRepository) {
        this.journalEntryRepository = journalEntryRepository;
    }

    public AccountingTrialBalanceSnapshot build(LocalDate startDate, LocalDate endDate, String statusFilter) {
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

        List<AccountingTrialBalanceRow> rows = buildRows(entries);

        AccountingTrialBalanceSnapshot snapshot = new AccountingTrialBalanceSnapshot();
        snapshot.setStartDate(normalizedStart);
        snapshot.setEndDate(normalizedEnd);
        snapshot.setStatusFilter(normalizedStatus);
        snapshot.setRows(rows);
        snapshot.setSummary(buildSummary(rows));
        return snapshot;
    }

    private List<AccountingTrialBalanceRow> buildRows(List<AccountingJournalEntry> entries) {
        Map<Long, AccountingTrialBalanceRow> rowsByAccount = new LinkedHashMap<>();

        for (AccountingJournalEntry entry : entries) {
            for (AccountingJournalLine line : entry.getLines()) {
                AccountingAccount account = line.getAccount();
                if (account == null || account.getId() == null) {
                    continue;
                }

                AccountingTrialBalanceRow row = rowsByAccount.computeIfAbsent(account.getId(), id -> buildRow(account));
                row.addMovement(line.getDebitAmount(), line.getCreditAmount());
            }
        }

        return rowsByAccount.values()
                .stream()
                .sorted(Comparator.comparing(AccountingTrialBalanceRow::getAccountCode, Comparator.nullsLast(String::compareTo)))
                .toList();
    }

    private AccountingTrialBalanceRow buildRow(AccountingAccount account) {
        AccountingTrialBalanceRow row = new AccountingTrialBalanceRow();
        row.setAccountId(account.getId());
        row.setAccountCode(account.getCode());
        row.setAccountName(account.getName());
        row.setAccountTypeLabel(account.getType() != null ? account.getType().getLabel() : "Sin tipo");
        return row;
    }

    private AccountingTrialBalanceSummary buildSummary(List<AccountingTrialBalanceRow> rows) {
        AccountingTrialBalanceSummary summary = new AccountingTrialBalanceSummary();
        BigDecimal totalDebit = BigDecimal.ZERO;
        BigDecimal totalCredit = BigDecimal.ZERO;
        BigDecimal totalDebitBalance = BigDecimal.ZERO;
        BigDecimal totalCreditBalance = BigDecimal.ZERO;
        int totalMovements = 0;

        for (AccountingTrialBalanceRow row : rows) {
            totalDebit = totalDebit.add(row.getTotalDebit());
            totalCredit = totalCredit.add(row.getTotalCredit());
            totalDebitBalance = totalDebitBalance.add(row.getDebitBalance());
            totalCreditBalance = totalCreditBalance.add(row.getCreditBalance());
            totalMovements += row.getMovementCount();
        }

        summary.setTotalAccounts(rows.size());
        summary.setTotalMovements(totalMovements);
        summary.setTotalDebit(totalDebit);
        summary.setTotalCredit(totalCredit);
        summary.setTotalDebitBalance(totalDebitBalance);
        summary.setTotalCreditBalance(totalCreditBalance);
        return summary;
    }

    private boolean matchesStatus(AccountingJournalEntry entry, String statusFilter) {
        if ("ALL".equals(statusFilter)) {
            return true;
        }
        return entry.getStatus() != null && entry.getStatus().name().equals(statusFilter);
    }

    private String normalizeStatusFilter(String statusFilter) {
        if (statusFilter == null || statusFilter.isBlank() || "ALL".equalsIgnoreCase(statusFilter)) {
            return "ALL";
        }

        try {
            return AccountingJournalEntryStatus.valueOf(statusFilter.trim().toUpperCase()).name();
        } catch (IllegalArgumentException ex) {
            return "ALL";
        }
    }
}
