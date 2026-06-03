package com.ecoamazonas.eco_agua.accounting.service;

import com.ecoamazonas.eco_agua.accounting.*;
import com.ecoamazonas.eco_agua.accounting.repository.AccountingAccountRepository;
import com.ecoamazonas.eco_agua.accounting.repository.AccountingJournalEntryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class AccountingJournalEntryService {

    private final AccountingJournalEntryRepository journalEntryRepository;
    private final AccountingAccountRepository accountRepository;
    private final AccountingPeriodCloseService periodCloseService;

    public AccountingJournalEntryService(
            AccountingJournalEntryRepository journalEntryRepository,
            AccountingAccountRepository accountRepository,
            AccountingPeriodCloseService periodCloseService
    ) {
        this.journalEntryRepository = journalEntryRepository;
        this.accountRepository = accountRepository;
        this.periodCloseService = periodCloseService;
    }

    public List<AccountingJournalEntry> findAll() {
        return journalEntryRepository.findAllByOrderByEntryDateDescIdDesc();
    }

    public AccountingJournalEntry findForEdit(Long id) {
        if (id == null) {
            return null;
        }
        return journalEntryRepository.findById(id).orElse(null);
    }

    public boolean isEntryPeriodClosed(AccountingJournalEntry entry) {
        return entry != null && periodCloseService.isClosed(entry.getEntryDate());
    }

    public Set<Long> findClosedEntryIds(List<AccountingJournalEntry> entries) {
        Set<Long> closedIds = new HashSet<>();
        if (entries == null || entries.isEmpty()) {
            return closedIds;
        }
        for (AccountingJournalEntry entry : entries) {
            if (entry != null && entry.getId() != null && isEntryPeriodClosed(entry)) {
                closedIds.add(entry.getId());
            }
        }
        return closedIds;
    }

    @Transactional
    public void saveFromForm(
            Long id,
            LocalDate entryDate,
            String description,
            AccountingJournalSourceType sourceType,
            AccountingJournalEntryStatus status,
            List<Long> accountIds,
            List<String> lineDescriptions,
            List<String> debitAmounts,
            List<String> creditAmounts
    ) {
        if (entryDate == null) {
            throw new IllegalArgumentException("Entry date is required.");
        }

        String normalizedDescription = normalize(description);
        if (normalizedDescription == null) {
            throw new IllegalArgumentException("Entry description is required.");
        }

        AccountingJournalEntry entry = id == null
                ? new AccountingJournalEntry()
                : journalEntryRepository.findById(id)
                    .orElseThrow(() -> new IllegalArgumentException("Journal entry was not found."));

        if (entry.getId() != null) {
            periodCloseService.assertPeriodOpen(entry.getEntryDate());
        }
        periodCloseService.assertPeriodOpen(entryDate);

        entry.setEntryDate(entryDate);
        entry.setDescription(normalizedDescription);
        entry.setSourceType(sourceType == null ? AccountingJournalSourceType.MANUAL : sourceType);
        entry.setStatus(normalizeStatus(status));
        entry.setLines(buildLines(accountIds, lineDescriptions, debitAmounts, creditAmounts));

        validateBalanced(entry);

        journalEntryRepository.save(entry);
    }

    @Transactional
    public void cancel(Long id) {
        AccountingJournalEntry entry = journalEntryRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Journal entry was not found."));
        periodCloseService.assertPeriodOpen(entry.getEntryDate());
        entry.setStatus(AccountingJournalEntryStatus.CANCELLED);
        journalEntryRepository.save(entry);
    }

    private List<AccountingJournalLine> buildLines(
            List<Long> accountIds,
            List<String> lineDescriptions,
            List<String> debitAmounts,
            List<String> creditAmounts
    ) {
        List<AccountingJournalLine> lines = new ArrayList<>();
        int rows = maxSize(accountIds, lineDescriptions, debitAmounts, creditAmounts);

        for (int i = 0; i < rows; i++) {
            Long accountId = getValue(accountIds, i);
            String lineDescription = normalize(getValue(lineDescriptions, i));
            BigDecimal debitAmount = parseAmount(getValue(debitAmounts, i));
            BigDecimal creditAmount = parseAmount(getValue(creditAmounts, i));

            boolean hasAmount = debitAmount.compareTo(BigDecimal.ZERO) > 0 || creditAmount.compareTo(BigDecimal.ZERO) > 0;
            boolean hasContent = accountId != null || lineDescription != null || hasAmount;

            if (!hasContent) {
                continue;
            }

            if (accountId == null) {
                throw new IllegalArgumentException("Each journal line with amount must have an account.");
            }
            if (debitAmount.compareTo(BigDecimal.ZERO) > 0 && creditAmount.compareTo(BigDecimal.ZERO) > 0) {
                throw new IllegalArgumentException("A journal line cannot have debit and credit at the same time.");
            }
            if (!hasAmount) {
                throw new IllegalArgumentException("Each journal line must have a debit or credit amount.");
            }

            AccountingAccount account = accountRepository.findById(accountId)
                    .orElseThrow(() -> new IllegalArgumentException("One selected account was not found."));

            AccountingJournalLine line = new AccountingJournalLine();
            line.setLineOrder(lines.size() + 1);
            line.setAccount(account);
            line.setDescription(lineDescription);
            line.setDebitAmount(debitAmount);
            line.setCreditAmount(creditAmount);
            lines.add(line);
        }

        if (lines.size() < 2) {
            throw new IllegalArgumentException("A journal entry needs at least two valid lines.");
        }

        return lines;
    }

    private void validateBalanced(AccountingJournalEntry entry) {
        BigDecimal totalDebit = entry.getTotalDebit();
        BigDecimal totalCredit = entry.getTotalCredit();

        if (totalDebit.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Total debit must be greater than zero.");
        }
        if (totalDebit.compareTo(totalCredit) != 0) {
            throw new IllegalArgumentException("Total debit and total credit must be equal.");
        }
    }

    private AccountingJournalEntryStatus normalizeStatus(AccountingJournalEntryStatus status) {
        if (status == null || status == AccountingJournalEntryStatus.CANCELLED) {
            return AccountingJournalEntryStatus.DRAFT;
        }
        return status;
    }

    @SafeVarargs
    private final int maxSize(List<?>... lists) {
        int max = 0;
        for (List<?> list : lists) {
            if (list != null && list.size() > max) {
                max = list.size();
            }
        }
        return max;
    }

    private <T> T getValue(List<T> values, int index) {
        if (values == null || index < 0 || index >= values.size()) {
            return null;
        }
        return values.get(index);
    }

    private BigDecimal parseAmount(String value) {
        String normalized = normalize(value);
        if (normalized == null) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }

        if (normalized.contains(",") && !normalized.contains(".")) {
            normalized = normalized.replace(',', '.');
        } else {
            normalized = normalized.replace(",", "");
        }

        BigDecimal amount;
        try {
            amount = new BigDecimal(normalized).setScale(2, RoundingMode.HALF_UP);
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("Amounts must be valid numbers.");
        }

        if (amount.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Amounts cannot be negative.");
        }
        return amount;
    }

    private String normalize(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
