package com.ecoamazonas.eco_agua.accounting;

import java.math.BigDecimal;

public class AccountingControlPanelSummary {

    private final int totalEntries;
    private final int draftEntries;
    private final int automaticDraftEntries;
    private final int postedEntries;
    private final int cancelledEntries;
    private final int unbalancedEntries;
    private final int openPeriodsWithMovements;
    private final int closedPeriods;
    private final int missingTemplateEvents;
    private final int inactiveTemplates;
    private final int invalidTemplates;
    private final int missingSimpleRules;
    private final int inactiveSimpleRules;
    private final BigDecimal totalDebit;
    private final BigDecimal totalCredit;

    public AccountingControlPanelSummary(
            int totalEntries,
            int draftEntries,
            int automaticDraftEntries,
            int postedEntries,
            int cancelledEntries,
            int unbalancedEntries,
            int openPeriodsWithMovements,
            int closedPeriods,
            int missingTemplateEvents,
            int inactiveTemplates,
            int invalidTemplates,
            int missingSimpleRules,
            int inactiveSimpleRules,
            BigDecimal totalDebit,
            BigDecimal totalCredit
    ) {
        this.totalEntries = totalEntries;
        this.draftEntries = draftEntries;
        this.automaticDraftEntries = automaticDraftEntries;
        this.postedEntries = postedEntries;
        this.cancelledEntries = cancelledEntries;
        this.unbalancedEntries = unbalancedEntries;
        this.openPeriodsWithMovements = openPeriodsWithMovements;
        this.closedPeriods = closedPeriods;
        this.missingTemplateEvents = missingTemplateEvents;
        this.inactiveTemplates = inactiveTemplates;
        this.invalidTemplates = invalidTemplates;
        this.missingSimpleRules = missingSimpleRules;
        this.inactiveSimpleRules = inactiveSimpleRules;
        this.totalDebit = safe(totalDebit);
        this.totalCredit = safe(totalCredit);
    }

    public int getTotalEntries() {
        return totalEntries;
    }

    public int getDraftEntries() {
        return draftEntries;
    }

    public int getAutomaticDraftEntries() {
        return automaticDraftEntries;
    }

    public int getPostedEntries() {
        return postedEntries;
    }

    public int getCancelledEntries() {
        return cancelledEntries;
    }

    public int getUnbalancedEntries() {
        return unbalancedEntries;
    }

    public int getOpenPeriodsWithMovements() {
        return openPeriodsWithMovements;
    }

    public int getClosedPeriods() {
        return closedPeriods;
    }

    public int getMissingTemplateEvents() {
        return missingTemplateEvents;
    }

    public int getInactiveTemplates() {
        return inactiveTemplates;
    }

    public int getInvalidTemplates() {
        return invalidTemplates;
    }

    public int getMissingSimpleRules() {
        return missingSimpleRules;
    }

    public int getInactiveSimpleRules() {
        return inactiveSimpleRules;
    }

    public BigDecimal getTotalDebit() {
        return totalDebit;
    }

    public BigDecimal getTotalCredit() {
        return totalCredit;
    }

    public BigDecimal getDifference() {
        return totalDebit.subtract(totalCredit);
    }

    public BigDecimal getAbsoluteDifference() {
        return getDifference().abs();
    }

    public boolean isBalanced() {
        return getDifference().compareTo(BigDecimal.ZERO) == 0;
    }

    public boolean isHealthy() {
        return draftEntries == 0
                && unbalancedEntries == 0
                && missingTemplateEvents == 0
                && inactiveTemplates == 0
                && invalidTemplates == 0
                && isBalanced();
    }

    public int getCriticalIssueCount() {
        int count = 0;
        if (draftEntries > 0) {
            count++;
        }
        if (unbalancedEntries > 0) {
            count++;
        }
        if (missingTemplateEvents > 0) {
            count++;
        }
        if (invalidTemplates > 0) {
            count++;
        }
        if (!isBalanced()) {
            count++;
        }
        return count;
    }

    private BigDecimal safe(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }
}
