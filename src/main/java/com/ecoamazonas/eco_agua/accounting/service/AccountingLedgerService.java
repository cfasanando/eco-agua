package com.ecoamazonas.eco_agua.accounting.service;

import com.ecoamazonas.eco_agua.accounting.*;
import com.ecoamazonas.eco_agua.accounting.repository.AccountingJournalEntryRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class AccountingLedgerService {

    private final AccountingJournalEntryRepository journalEntryRepository;

    public AccountingLedgerService(AccountingJournalEntryRepository journalEntryRepository) {
        this.journalEntryRepository = journalEntryRepository;
    }

    public AccountingLedgerSnapshot build(LocalDate startDate, LocalDate endDate, String statusFilter, Long accountId) {
        LocalDate today = LocalDate.now();
        LocalDate normalizedStart = startDate != null ? startDate : today.withDayOfMonth(1);
        LocalDate normalizedEnd = endDate != null ? endDate : normalizedStart.withDayOfMonth(normalizedStart.lengthOfMonth());

        if (normalizedEnd.isBefore(normalizedStart)) {
            LocalDate tmp = normalizedStart;
            normalizedStart = normalizedEnd;
            normalizedEnd = tmp;
        }

        String normalizedStatus = normalizeStatusFilter(statusFilter);
        Long normalizedAccountId = accountId != null && accountId > 0 ? accountId : null;

        List<AccountingJournalEntry> entries = journalEntryRepository
                .findByEntryDateBetweenOrderByEntryDateAscIdAsc(normalizedStart, normalizedEnd)
                .stream()
                .filter(entry -> matchesStatus(entry, normalizedStatus))
                .toList();

        List<AccountingLedgerAccountGroup> groups = buildGroups(entries, normalizedAccountId);
        AccountingLedgerSnapshot snapshot = new AccountingLedgerSnapshot();
        snapshot.setStartDate(normalizedStart);
        snapshot.setEndDate(normalizedEnd);
        snapshot.setStatusFilter(normalizedStatus);
        snapshot.setAccountId(normalizedAccountId);
        snapshot.setAccountGroups(groups);
        snapshot.setSummary(buildSummary(groups));
        return snapshot;
    }

    private List<AccountingLedgerAccountGroup> buildGroups(List<AccountingJournalEntry> entries, Long accountId) {
        Map<Long, AccountingLedgerAccountGroup> groupsByAccount = new LinkedHashMap<>();

        for (AccountingJournalEntry entry : entries) {
            for (AccountingJournalLine line : entry.getLines()) {
                AccountingAccount account = line.getAccount();
                if (account == null || account.getId() == null) {
                    continue;
                }
                if (accountId != null && !accountId.equals(account.getId())) {
                    continue;
                }

                AccountingLedgerAccountGroup group = groupsByAccount.computeIfAbsent(account.getId(), id -> buildGroup(account));
                group.addRow(buildRow(entry, line));
            }
        }

        return groupsByAccount.values()
                .stream()
                .sorted(Comparator.comparing(AccountingLedgerAccountGroup::getAccountCode, Comparator.nullsLast(String::compareTo)))
                .toList();
    }

    private AccountingLedgerAccountGroup buildGroup(AccountingAccount account) {
        AccountingLedgerAccountGroup group = new AccountingLedgerAccountGroup();
        group.setAccountId(account.getId());
        group.setAccountCode(account.getCode());
        group.setAccountName(account.getName());
        group.setAccountTypeLabel(account.getType() != null ? account.getType().getLabel() : "Sin tipo");
        return group;
    }

    private AccountingLedgerRow buildRow(AccountingJournalEntry entry, AccountingJournalLine line) {
        AccountingLedgerRow row = new AccountingLedgerRow();
        row.setEntryId(entry.getId());
        row.setEntryDate(entry.getEntryDate());
        row.setEntryDescription(entry.getDescription());
        row.setLineDescription(line.getDescription() != null && !line.getDescription().isBlank()
                ? line.getDescription()
                : entry.getDescription());
        row.setSourceLabel(entry.getSourceDetailLabel());
        row.setStatus(entry.getStatus());
        row.setDebitAmount(line.getDebitAmount());
        row.setCreditAmount(line.getCreditAmount());
        return row;
    }

    private AccountingLedgerSummary buildSummary(List<AccountingLedgerAccountGroup> groups) {
        AccountingLedgerSummary summary = new AccountingLedgerSummary();
        BigDecimal totalDebit = BigDecimal.ZERO;
        BigDecimal totalCredit = BigDecimal.ZERO;
        int totalMovements = 0;

        for (AccountingLedgerAccountGroup group : groups) {
            totalDebit = totalDebit.add(group.getTotalDebit());
            totalCredit = totalCredit.add(group.getTotalCredit());
            totalMovements += group.getRows().size();
        }

        summary.setTotalAccounts(groups.size());
        summary.setTotalMovements(totalMovements);
        summary.setTotalDebit(totalDebit);
        summary.setTotalCredit(totalCredit);
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
