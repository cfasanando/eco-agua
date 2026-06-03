package com.ecoamazonas.eco_agua.accounting.service;

import com.ecoamazonas.eco_agua.accounting.*;
import com.ecoamazonas.eco_agua.accounting.repository.AccountingAutomationRuleRepository;
import com.ecoamazonas.eco_agua.accounting.repository.AccountingJournalEntryRepository;
import com.ecoamazonas.eco_agua.accounting.repository.AccountingPeriodCloseRepository;
import com.ecoamazonas.eco_agua.accounting.repository.AccountingRuleTemplateRepository;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.TextStyle;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class AccountingControlPanelService {

    private final AccountingJournalEntryRepository journalEntryRepository;
    private final AccountingPeriodCloseRepository periodCloseRepository;
    private final AccountingAutomationRuleRepository automationRuleRepository;
    private final AccountingRuleTemplateRepository ruleTemplateRepository;

    public AccountingControlPanelService(
            AccountingJournalEntryRepository journalEntryRepository,
            AccountingPeriodCloseRepository periodCloseRepository,
            AccountingAutomationRuleRepository automationRuleRepository,
            AccountingRuleTemplateRepository ruleTemplateRepository
    ) {
        this.journalEntryRepository = journalEntryRepository;
        this.periodCloseRepository = periodCloseRepository;
        this.automationRuleRepository = automationRuleRepository;
        this.ruleTemplateRepository = ruleTemplateRepository;
    }

    @Transactional(readOnly = true)
    public AccountingControlPanelSnapshot buildSnapshot() {
        List<AccountingJournalEntry> entries = journalEntryRepository.findAllByOrderByEntryDateDescIdDesc();
        List<AccountingPeriodClose> periodCloses = periodCloseRepository.findAll(
                Sort.by(Sort.Direction.DESC, "periodYear", "periodMonth")
        );
        List<AccountingAutomationRule> simpleRules = automationRuleRepository.findAllByOrderByEventTypeAsc();
        List<AccountingRuleTemplate> templates = ruleTemplateRepository.findAllByOrderByEventTypeAsc();

        Map<YearMonth, AccountingPeriodClose> closeByPeriod = periodCloses.stream()
                .collect(Collectors.toMap(
                        close -> YearMonth.of(close.getPeriodYear(), close.getPeriodMonth()),
                        Function.identity(),
                        (left, right) -> left
                ));

        List<AccountingControlPanelConfigurationRow> configurationRows = buildConfigurationRows(simpleRules, templates);
        AccountingControlPanelSummary summary = buildSummary(entries, periodCloses, configurationRows, closeByPeriod);
        List<AccountingControlPanelAlert> alerts = buildAlerts(summary);
        List<AccountingControlPanelPeriodRow> periodRows = buildPeriodRows(entries, closeByPeriod);
        List<AccountingControlPanelRecentEntry> recentAutomaticEntries = entries.stream()
                .filter(entry -> entry.getSourceEvent() != null)
                .limit(8)
                .map(AccountingControlPanelRecentEntry::new)
                .toList();

        return new AccountingControlPanelSnapshot(
                summary,
                alerts,
                periodRows,
                configurationRows,
                recentAutomaticEntries,
                LocalDateTime.now()
        );
    }

    private AccountingControlPanelSummary buildSummary(
            List<AccountingJournalEntry> entries,
            List<AccountingPeriodClose> periodCloses,
            List<AccountingControlPanelConfigurationRow> configurationRows,
            Map<YearMonth, AccountingPeriodClose> closeByPeriod
    ) {
        int draftEntries = 0;
        int automaticDraftEntries = 0;
        int postedEntries = 0;
        int cancelledEntries = 0;
        int unbalancedEntries = 0;
        BigDecimal totalDebit = BigDecimal.ZERO;
        BigDecimal totalCredit = BigDecimal.ZERO;
        Set<YearMonth> periodsWithMovements = new HashSet<>();

        for (AccountingJournalEntry entry : entries) {
            if (entry.getEntryDate() != null) {
                periodsWithMovements.add(YearMonth.from(entry.getEntryDate()));
            }

            AccountingJournalEntryStatus status = entry.getStatus();
            if (AccountingJournalEntryStatus.DRAFT.equals(status)) {
                draftEntries++;
                if (entry.getSourceEvent() != null) {
                    automaticDraftEntries++;
                }
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

        int closedPeriods = (int) periodCloses.stream().filter(AccountingPeriodClose::isClosed).count();
        int openPeriodsWithMovements = (int) periodsWithMovements.stream()
                .filter(period -> !isClosed(period, closeByPeriod))
                .count();
        int missingTemplates = (int) configurationRows.stream().filter(row -> !row.isHasTemplate()).count();
        int inactiveTemplates = (int) configurationRows.stream()
                .filter(row -> row.isHasTemplate() && !row.isTemplateActive())
                .count();
        int invalidTemplates = (int) configurationRows.stream()
                .filter(row -> row.isHasTemplate() && row.isTemplateActive() && !row.isTemplateValid())
                .count();
        int missingSimpleRules = (int) configurationRows.stream().filter(row -> !row.isHasSimpleRule()).count();
        int inactiveSimpleRules = (int) configurationRows.stream()
                .filter(row -> row.isHasSimpleRule() && !row.isSimpleRuleActive())
                .count();

        return new AccountingControlPanelSummary(
                entries.size(),
                draftEntries,
                automaticDraftEntries,
                postedEntries,
                cancelledEntries,
                unbalancedEntries,
                openPeriodsWithMovements,
                closedPeriods,
                missingTemplates,
                inactiveTemplates,
                invalidTemplates,
                missingSimpleRules,
                inactiveSimpleRules,
                totalDebit,
                totalCredit
        );
    }

    private List<AccountingControlPanelConfigurationRow> buildConfigurationRows(
            List<AccountingAutomationRule> simpleRules,
            List<AccountingRuleTemplate> templates
    ) {
        Map<AccountingAutomationEvent, AccountingAutomationRule> simpleRuleByEvent = simpleRules.stream()
                .collect(Collectors.toMap(AccountingAutomationRule::getEventType, Function.identity(), (left, right) -> left));
        Map<AccountingAutomationEvent, AccountingRuleTemplate> templateByEvent = templates.stream()
                .collect(Collectors.toMap(AccountingRuleTemplate::getEventType, Function.identity(), (left, right) -> left));

        List<AccountingControlPanelConfigurationRow> rows = new ArrayList<>();
        for (AccountingAutomationEvent event : AccountingAutomationEvent.values()) {
            AccountingAutomationRule simpleRule = simpleRuleByEvent.get(event);
            AccountingRuleTemplate template = templateByEvent.get(event);
            boolean hasTemplate = template != null;
            boolean templateValid = hasTemplate
                    && template.getLines().size() >= 2
                    && template.getDebitLineCount() > 0
                    && template.getCreditLineCount() > 0;

            rows.add(new AccountingControlPanelConfigurationRow(
                    event,
                    simpleRule != null,
                    simpleRule != null && simpleRule.isActive(),
                    hasTemplate,
                    hasTemplate && template.isActive() && template.isGenerateDraft(),
                    templateValid,
                    hasTemplate ? template.getLines().size() : 0
            ));
        }
        return rows;
    }

    private List<AccountingControlPanelAlert> buildAlerts(AccountingControlPanelSummary summary) {
        List<AccountingControlPanelAlert> alerts = new ArrayList<>();

        if (summary.getDraftEntries() > 0) {
            alerts.add(new AccountingControlPanelAlert(
                    "warning",
                    "Asientos en borrador",
                    summary.getDraftEntries() + " asiento(s) siguen pendientes de revisión. Regístralos o anúlalos antes de cerrar períodos.",
                    "/accounting/draft-review",
                    "Revisar borradores"
            ));
        }

        if (summary.getUnbalancedEntries() > 0) {
            alerts.add(new AccountingControlPanelAlert(
                    "danger",
                    "Asientos descuadrados",
                    summary.getUnbalancedEntries() + " asiento(s) activos tienen diferencia entre debe y haber.",
                    "/accounting/journal-book?status=ALL",
                    "Ver libro diario"
            ));
        }

        if (!summary.isBalanced()) {
            alerts.add(new AccountingControlPanelAlert(
                    "danger",
                    "Diferencia contable general",
                    "El total debe y haber de los asientos activos tiene una diferencia de S/. " + summary.getAbsoluteDifference() + ".",
                    "/accounting/trial-balance?status=ALL",
                    "Ver balance de comprobación"
            ));
        }

        if (summary.getMissingTemplateEvents() > 0 || summary.getInvalidTemplates() > 0) {
            alerts.add(new AccountingControlPanelAlert(
                    "warning",
                    "Plantillas contables incompletas",
                    "Hay " + summary.getMissingTemplateEvents() + " evento(s) sin plantilla y " + summary.getInvalidTemplates() + " plantilla(s) incompletas.",
                    "/accounting/rule-templates",
                    "Revisar plantillas"
            ));
        }

        if (summary.getInactiveTemplates() > 0) {
            alerts.add(new AccountingControlPanelAlert(
                    "warning",
                    "Plantillas contables inactivas",
                    summary.getInactiveTemplates() + " plantilla(s) no están activas para generar borradores automáticos.",
                    "/accounting/rule-templates",
                    "Activar plantillas"
            ));
        }

        if (summary.getOpenPeriodsWithMovements() > 0) {
            alerts.add(new AccountingControlPanelAlert(
                    "info",
                    "Períodos con movimientos abiertos",
                    summary.getOpenPeriodsWithMovements() + " período(s) con asientos todavía no están cerrados.",
                    "/accounting/period-close",
                    "Ir a cierre mensual"
            ));
        }

        if (alerts.isEmpty()) {
            alerts.add(new AccountingControlPanelAlert(
                    "success",
                    "Contabilidad sin observaciones críticas",
                    "No se detectaron borradores pendientes, descuadres ni plantillas contables faltantes.",
                    "/accounting/trial-balance",
                    "Ver balance de comprobación"
            ));
        }

        return alerts;
    }

    private List<AccountingControlPanelPeriodRow> buildPeriodRows(
            List<AccountingJournalEntry> entries,
            Map<YearMonth, AccountingPeriodClose> closeByPeriod
    ) {
        Map<YearMonth, List<AccountingJournalEntry>> entriesByPeriod = entries.stream()
                .filter(entry -> entry.getEntryDate() != null)
                .collect(Collectors.groupingBy(entry -> YearMonth.from(entry.getEntryDate())));

        return entriesByPeriod.entrySet().stream()
                .sorted(Map.Entry.<YearMonth, List<AccountingJournalEntry>>comparingByKey().reversed())
                .limit(8)
                .map(entry -> buildPeriodRow(entry.getKey(), entry.getValue(), closeByPeriod))
                .toList();
    }

    private AccountingControlPanelPeriodRow buildPeriodRow(
            YearMonth period,
            List<AccountingJournalEntry> entries,
            Map<YearMonth, AccountingPeriodClose> closeByPeriod
    ) {
        int draftEntries = 0;
        int unbalancedEntries = 0;
        for (AccountingJournalEntry entry : entries) {
            if (AccountingJournalEntryStatus.DRAFT.equals(entry.getStatus())) {
                draftEntries++;
            }
            if (!AccountingJournalEntryStatus.CANCELLED.equals(entry.getStatus()) && !entry.isBalanced()) {
                unbalancedEntries++;
            }
        }

        return new AccountingControlPanelPeriodRow(
                period.getYear(),
                period.getMonthValue(),
                period.getMonth().getDisplayName(TextStyle.FULL, Locale.forLanguageTag("es-PE")) + " " + period.getYear(),
                isClosed(period, closeByPeriod),
                entries.size(),
                draftEntries,
                unbalancedEntries
        );
    }

    private boolean isClosed(YearMonth period, Map<YearMonth, AccountingPeriodClose> closeByPeriod) {
        AccountingPeriodClose close = closeByPeriod.get(period);
        return close != null && close.isClosed();
    }
}
