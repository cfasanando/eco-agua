package com.ecoamazonas.eco_agua.accounting.service;

import com.ecoamazonas.eco_agua.accounting.AccountingAccount;
import com.ecoamazonas.eco_agua.accounting.AccountingAccountType;
import com.ecoamazonas.eco_agua.accounting.AccountingBalanceSheetRow;
import com.ecoamazonas.eco_agua.accounting.AccountingBalanceSheetSnapshot;
import com.ecoamazonas.eco_agua.accounting.AccountingBalanceSheetSummary;
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
import java.util.Map;

@Service
public class AccountingBalanceSheetService {

    private static final String SECTION_ASSETS = "ASSETS";
    private static final String SECTION_LIABILITIES = "LIABILITIES";
    private static final String SECTION_EQUITY = "EQUITY";

    private final AccountingJournalEntryRepository journalEntryRepository;

    public AccountingBalanceSheetService(AccountingJournalEntryRepository journalEntryRepository) {
        this.journalEntryRepository = journalEntryRepository;
    }

    @Transactional(readOnly = true)
    public AccountingBalanceSheetSnapshot build(LocalDate startDate, LocalDate endDate, String statusFilter) {
        LocalDate today = LocalDate.now();
        LocalDate normalizedStart = startDate != null ? startDate : LocalDate.of(today.getYear(), 1, 1);
        LocalDate normalizedEnd = endDate != null ? endDate : today;

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

        Map<Long, AccountingBalanceSheetRow> rowsByAccount = new LinkedHashMap<>();
        BigDecimal currentResult = BigDecimal.ZERO;
        int resultMovements = 0;

        for (AccountingJournalEntry entry : entries) {
            for (AccountingJournalLine line : entry.getLines()) {
                AccountingAccount account = line.getAccount();
                if (account == null || account.getId() == null || account.getType() == null) {
                    continue;
                }

                if (isBalanceSheetAccount(account)) {
                    AccountingBalanceSheetRow row = rowsByAccount.computeIfAbsent(account.getId(), id -> buildRow(account));
                    row.addMovement(line.getDebitAmount(), line.getCreditAmount());
                    continue;
                }

                if (account.getType() == AccountingAccountType.INCOME) {
                    currentResult = currentResult.add(line.getCreditAmount().subtract(line.getDebitAmount()));
                    resultMovements++;
                } else if (account.getType() == AccountingAccountType.EXPENSE) {
                    currentResult = currentResult.subtract(line.getDebitAmount().subtract(line.getCreditAmount()));
                    resultMovements++;
                }
            }
        }

        List<AccountingBalanceSheetRow> assetRows = new ArrayList<>();
        List<AccountingBalanceSheetRow> liabilityRows = new ArrayList<>();
        List<AccountingBalanceSheetRow> equityRows = new ArrayList<>();

        for (AccountingBalanceSheetRow row : rowsByAccount.values()) {
            calculateAmount(row);
            if (SECTION_ASSETS.equals(row.getSectionCode())) {
                assetRows.add(row);
            } else if (SECTION_LIABILITIES.equals(row.getSectionCode())) {
                liabilityRows.add(row);
            } else {
                equityRows.add(row);
            }
        }

        if (currentResult.compareTo(BigDecimal.ZERO) != 0 || resultMovements > 0) {
            equityRows.add(buildCurrentResultRow(currentResult, resultMovements));
        }

        Comparator<AccountingBalanceSheetRow> accountComparator = Comparator
                .comparing(AccountingBalanceSheetRow::getAccountCode, Comparator.nullsLast(String::compareTo))
                .thenComparing(AccountingBalanceSheetRow::getAccountName, Comparator.nullsLast(String::compareTo));

        assetRows.sort(accountComparator);
        liabilityRows.sort(accountComparator);
        equityRows.sort(accountComparator);

        AccountingBalanceSheetSnapshot snapshot = new AccountingBalanceSheetSnapshot();
        snapshot.setStartDate(normalizedStart);
        snapshot.setEndDate(normalizedEnd);
        snapshot.setStatusFilter(normalizedStatus);
        snapshot.setAssetRows(assetRows);
        snapshot.setLiabilityRows(liabilityRows);
        snapshot.setEquityRows(equityRows);
        snapshot.setSummary(buildSummary(assetRows, liabilityRows, equityRows, currentResult));
        return snapshot;
    }

    private AccountingBalanceSheetRow buildRow(AccountingAccount account) {
        AccountingBalanceSheetRow row = new AccountingBalanceSheetRow();
        row.setAccountId(account.getId());
        row.setAccountCode(account.getCode());
        row.setAccountName(account.getName());
        row.setAccountType(account.getType());
        row.setAccountTypeLabel(account.getType().getLabel());
        row.setSectionCode(resolveSection(account.getType()));
        row.setSectionLabel(resolveSectionLabel(row.getSectionCode()));
        return row;
    }

    private AccountingBalanceSheetRow buildCurrentResultRow(BigDecimal currentResult, int movementCount) {
        AccountingBalanceSheetRow row = new AccountingBalanceSheetRow();
        row.setAccountCode("RESULTADO");
        row.setAccountName("Resultado del período");
        row.setAccountType(AccountingAccountType.EQUITY);
        row.setAccountTypeLabel(AccountingAccountType.EQUITY.getLabel());
        row.setSectionCode(SECTION_EQUITY);
        row.setSectionLabel(resolveSectionLabel(SECTION_EQUITY));
        row.setMovementCount(movementCount);
        row.setAmount(currentResult);
        row.setGenerated(true);
        return row;
    }

    private AccountingBalanceSheetSummary buildSummary(
            List<AccountingBalanceSheetRow> assetRows,
            List<AccountingBalanceSheetRow> liabilityRows,
            List<AccountingBalanceSheetRow> equityRows,
            BigDecimal currentResult
    ) {
        AccountingBalanceSheetSummary summary = new AccountingBalanceSheetSummary();
        summary.setTotalAccounts(assetRows.size() + liabilityRows.size() + equityRows.size());
        summary.setTotalMovements(movementCount(assetRows) + movementCount(liabilityRows) + movementCount(equityRows));
        summary.setTotalAssets(sum(assetRows));
        summary.setTotalLiabilities(sum(liabilityRows));
        summary.setTotalEquity(sum(equityRows));
        summary.setCurrentPeriodResult(currentResult);
        return summary;
    }

    private BigDecimal sum(List<AccountingBalanceSheetRow> rows) {
        return rows.stream()
                .map(AccountingBalanceSheetRow::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private int movementCount(List<AccountingBalanceSheetRow> rows) {
        return rows.stream()
                .mapToInt(AccountingBalanceSheetRow::getMovementCount)
                .sum();
    }

    private void calculateAmount(AccountingBalanceSheetRow row) {
        if (row.getAccountType() == AccountingAccountType.ASSET) {
            row.setAmount(row.getTotalDebit().subtract(row.getTotalCredit()));
            return;
        }
        row.setAmount(row.getTotalCredit().subtract(row.getTotalDebit()));
    }

    private boolean isBalanceSheetAccount(AccountingAccount account) {
        return account.getType() == AccountingAccountType.ASSET
                || account.getType() == AccountingAccountType.LIABILITY
                || account.getType() == AccountingAccountType.EQUITY;
    }

    private String resolveSection(AccountingAccountType type) {
        if (type == AccountingAccountType.ASSET) {
            return SECTION_ASSETS;
        }
        if (type == AccountingAccountType.LIABILITY) {
            return SECTION_LIABILITIES;
        }
        return SECTION_EQUITY;
    }

    private String resolveSectionLabel(String section) {
        return switch (section) {
            case SECTION_ASSETS -> "Activo";
            case SECTION_LIABILITIES -> "Pasivo";
            default -> "Patrimonio";
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
        if ("ALL".equalsIgnoreCase(statusFilter)) {
            return "ALL";
        }

        try {
            return AccountingJournalEntryStatus.valueOf(statusFilter.trim().toUpperCase()).name();
        } catch (IllegalArgumentException ex) {
            return AccountingJournalEntryStatus.POSTED.name();
        }
    }
}
