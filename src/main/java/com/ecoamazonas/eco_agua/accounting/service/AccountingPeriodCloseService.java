package com.ecoamazonas.eco_agua.accounting.service;

import com.ecoamazonas.eco_agua.accounting.*;
import com.ecoamazonas.eco_agua.accounting.repository.AccountingJournalEntryRepository;
import com.ecoamazonas.eco_agua.accounting.repository.AccountingPeriodCloseRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

@Service
public class AccountingPeriodCloseService {

    private final AccountingPeriodCloseRepository periodCloseRepository;
    private final AccountingJournalEntryRepository journalEntryRepository;

    public AccountingPeriodCloseService(
            AccountingPeriodCloseRepository periodCloseRepository,
            AccountingJournalEntryRepository journalEntryRepository
    ) {
        this.periodCloseRepository = periodCloseRepository;
        this.journalEntryRepository = journalEntryRepository;
    }

    @Transactional(readOnly = true)
    public AccountingPeriodCloseSnapshot build(Integer year, Integer month) {
        YearMonth period = resolvePeriod(year, month);
        LocalDate startDate = period.atDay(1);
        LocalDate endDate = period.atEndOfMonth();

        AccountingPeriodClose periodClose = periodCloseRepository
                .findByPeriodYearAndPeriodMonth(period.getYear(), period.getMonthValue())
                .orElse(null);

        List<AccountingJournalEntry> entries = journalEntryRepository
                .findByEntryDateBetweenOrderByEntryDateAscIdAsc(startDate, endDate);

        AccountingPeriodCloseSummary summary = buildSummary(entries);
        List<AccountingPeriodCloseCheckRow> checks = buildChecks(period, summary);

        return new AccountingPeriodCloseSnapshot(
                period.getYear(),
                period.getMonthValue(),
                startDate,
                endDate,
                periodClose == null ? AccountingPeriodCloseStatus.OPEN : periodClose.getStatus(),
                periodClose == null ? null : periodClose.getClosedAt(),
                periodClose == null ? null : periodClose.getReopenedAt(),
                periodClose == null ? null : periodClose.getNotes(),
                summary,
                checks
        );
    }

    @Transactional
    public void closePeriod(Integer year, Integer month, String notes) {
        AccountingPeriodCloseSnapshot snapshot = build(year, month);
        if (!snapshot.getSummary().isReadyToClose()) {
            throw new IllegalStateException("El período no puede cerrarse porque todavía tiene observaciones pendientes.");
        }

        AccountingPeriodClose periodClose = periodCloseRepository
                .findByPeriodYearAndPeriodMonth(snapshot.getYear(), snapshot.getMonth())
                .orElseGet(AccountingPeriodClose::new);

        periodClose.setPeriodYear(snapshot.getYear());
        periodClose.setPeriodMonth(snapshot.getMonth());
        periodClose.setStatus(AccountingPeriodCloseStatus.CLOSED);
        periodClose.setClosedAt(LocalDateTime.now());
        periodClose.setNotes(normalize(notes));

        periodCloseRepository.save(periodClose);
    }

    @Transactional
    public void reopenPeriod(Integer year, Integer month, String notes) {
        YearMonth period = resolvePeriod(year, month);
        AccountingPeriodClose periodClose = periodCloseRepository
                .findByPeriodYearAndPeriodMonth(period.getYear(), period.getMonthValue())
                .orElseThrow(() -> new IllegalStateException("El período seleccionado todavía no tiene cierre registrado."));

        periodClose.setStatus(AccountingPeriodCloseStatus.OPEN);
        periodClose.setReopenedAt(LocalDateTime.now());
        periodClose.setNotes(normalize(notes));

        periodCloseRepository.save(periodClose);
    }

    @Transactional(readOnly = true)
    public boolean isClosed(LocalDate date) {
        if (date == null) {
            return false;
        }
        return periodCloseRepository
                .findByPeriodYearAndPeriodMonth(date.getYear(), date.getMonthValue())
                .map(AccountingPeriodClose::isClosed)
                .orElse(false);
    }

    @Transactional(readOnly = true)
    public void assertPeriodOpen(LocalDate date) {
        if (isClosed(date)) {
            throw new IllegalStateException("El período contable " + formatPeriod(date) + " está cerrado. Reábrelo desde Cierre mensual antes de modificar asientos.");
        }
    }

    private AccountingPeriodCloseSummary buildSummary(List<AccountingJournalEntry> entries) {
        int draftEntries = 0;
        int postedEntries = 0;
        int cancelledEntries = 0;
        int unbalancedEntries = 0;
        BigDecimal totalDebit = BigDecimal.ZERO;
        BigDecimal totalCredit = BigDecimal.ZERO;

        for (AccountingJournalEntry entry : entries) {
            AccountingJournalEntryStatus status = entry.getStatus();
            if (AccountingJournalEntryStatus.DRAFT.equals(status)) {
                draftEntries++;
            } else if (AccountingJournalEntryStatus.POSTED.equals(status)) {
                postedEntries++;
            } else if (AccountingJournalEntryStatus.CANCELLED.equals(status)) {
                cancelledEntries++;
            }

            if (!AccountingJournalEntryStatus.CANCELLED.equals(status)) {
                totalDebit = totalDebit.add(entry.getTotalDebit());
                totalCredit = totalCredit.add(entry.getTotalCredit());
                if (!entry.isBalanced()) {
                    unbalancedEntries++;
                }
            }
        }

        return new AccountingPeriodCloseSummary(
                entries.size(),
                draftEntries,
                postedEntries,
                cancelledEntries,
                unbalancedEntries,
                totalDebit,
                totalCredit
        );
    }

    private List<AccountingPeriodCloseCheckRow> buildChecks(YearMonth period, AccountingPeriodCloseSummary summary) {
        List<AccountingPeriodCloseCheckRow> checks = new ArrayList<>();
        String dateParams = "?fromDate=" + period.atDay(1) + "&toDate=" + period.atEndOfMonth();
        String periodParams = "?year=" + period.getYear() + "&month=" + period.getMonthValue();

        checks.add(new AccountingPeriodCloseCheckRow(
                "Asientos en borrador",
                summary.getDraftEntries() == 0
                        ? "No hay asientos pendientes de revisar."
                        : summary.getDraftEntries() + " asiento(s) siguen en borrador.",
                summary.getDraftEntries() == 0,
                "/accounting/draft-review",
                "Revisar borradores"
        ));

        checks.add(new AccountingPeriodCloseCheckRow(
                "Asientos descuadrados",
                summary.getUnbalancedEntries() == 0
                        ? "No hay asientos activos descuadrados."
                        : summary.getUnbalancedEntries() + " asiento(s) activos tienen diferencia entre debe y haber.",
                summary.getUnbalancedEntries() == 0,
                "/accounting/journal-book" + dateParams + "&status=ALL",
                "Ver libro diario"
        ));

        checks.add(new AccountingPeriodCloseCheckRow(
                "Cuadre general del período",
                summary.getDifference().compareTo(BigDecimal.ZERO) == 0
                        ? "El total debe coincide con el total haber."
                        : "Existe una diferencia de S/. " + summary.getAbsoluteDifference() + ".",
                summary.getDifference().compareTo(BigDecimal.ZERO) == 0,
                "/accounting/trial-balance" + dateParams + "&status=ALL",
                "Ver balance de comprobación"
        ));

        checks.add(new AccountingPeriodCloseCheckRow(
                "Estado de resultados",
                "Revisa utilidad o pérdida antes de cerrar el período.",
                true,
                "/accounting/income-statement" + dateParams + "&status=POSTED",
                "Ver estado de resultados"
        ));

        checks.add(new AccountingPeriodCloseCheckRow(
                "Balance general",
                "Revisa activo, pasivo y patrimonio antes de cerrar el período.",
                true,
                "/accounting/balance-sheet" + dateParams + "&status=POSTED",
                "Ver balance general"
        ));

        return checks;
    }

    private String formatPeriod(LocalDate date) {
        if (date == null) {
            return "seleccionado";
        }
        return String.format("%02d/%d", date.getMonthValue(), date.getYear());
    }

    private YearMonth resolvePeriod(Integer year, Integer month) {
        YearMonth current = YearMonth.now();
        int resolvedYear = year == null ? current.getYear() : year;
        int resolvedMonth = month == null ? current.getMonthValue() : month;
        if (resolvedMonth < 1 || resolvedMonth > 12) {
            resolvedMonth = current.getMonthValue();
        }
        return YearMonth.of(resolvedYear, resolvedMonth);
    }

    private String normalize(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
