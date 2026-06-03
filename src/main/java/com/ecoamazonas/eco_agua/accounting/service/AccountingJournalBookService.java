package com.ecoamazonas.eco_agua.accounting.service;

import com.ecoamazonas.eco_agua.accounting.AccountingJournalBookSnapshot;
import com.ecoamazonas.eco_agua.accounting.AccountingJournalBookSummary;
import com.ecoamazonas.eco_agua.accounting.AccountingJournalEntry;
import com.ecoamazonas.eco_agua.accounting.AccountingJournalEntryStatus;
import com.ecoamazonas.eco_agua.accounting.repository.AccountingJournalEntryRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Service
public class AccountingJournalBookService {

    private final AccountingJournalEntryRepository journalEntryRepository;

    public AccountingJournalBookService(AccountingJournalEntryRepository journalEntryRepository) {
        this.journalEntryRepository = journalEntryRepository;
    }

    public AccountingJournalBookSnapshot build(LocalDate startDate, LocalDate endDate, String statusFilter) {
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

        AccountingJournalBookSnapshot snapshot = new AccountingJournalBookSnapshot();
        snapshot.setStartDate(normalizedStart);
        snapshot.setEndDate(normalizedEnd);
        snapshot.setStatusFilter(normalizedStatus);
        snapshot.setEntries(entries);
        snapshot.setSummary(buildSummary(entries));

        return snapshot;
    }

    private AccountingJournalBookSummary buildSummary(List<AccountingJournalEntry> entries) {
        AccountingJournalBookSummary summary = new AccountingJournalBookSummary();
        summary.setTotalEntries(entries.size());

        BigDecimal totalDebit = BigDecimal.ZERO;
        BigDecimal totalCredit = BigDecimal.ZERO;
        int totalLines = 0;
        int draftEntries = 0;
        int postedEntries = 0;
        int cancelledEntries = 0;

        for (AccountingJournalEntry entry : entries) {
            totalLines += entry.getLines().size();
            totalDebit = totalDebit.add(entry.getTotalDebit());
            totalCredit = totalCredit.add(entry.getTotalCredit());

            if (entry.getStatus() == AccountingJournalEntryStatus.DRAFT) {
                draftEntries++;
            } else if (entry.getStatus() == AccountingJournalEntryStatus.POSTED) {
                postedEntries++;
            } else if (entry.getStatus() == AccountingJournalEntryStatus.CANCELLED) {
                cancelledEntries++;
            }
        }

        summary.setTotalLines(totalLines);
        summary.setDraftEntries(draftEntries);
        summary.setPostedEntries(postedEntries);
        summary.setCancelledEntries(cancelledEntries);
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
