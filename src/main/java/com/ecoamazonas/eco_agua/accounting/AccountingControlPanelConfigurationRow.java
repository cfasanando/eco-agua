package com.ecoamazonas.eco_agua.accounting;

public class AccountingControlPanelConfigurationRow {

    private final AccountingAutomationEvent event;
    private final boolean hasSimpleRule;
    private final boolean simpleRuleActive;
    private final boolean hasTemplate;
    private final boolean templateActive;
    private final boolean templateValid;
    private final int templateLineCount;

    public AccountingControlPanelConfigurationRow(
            AccountingAutomationEvent event,
            boolean hasSimpleRule,
            boolean simpleRuleActive,
            boolean hasTemplate,
            boolean templateActive,
            boolean templateValid,
            int templateLineCount
    ) {
        this.event = event;
        this.hasSimpleRule = hasSimpleRule;
        this.simpleRuleActive = simpleRuleActive;
        this.hasTemplate = hasTemplate;
        this.templateActive = templateActive;
        this.templateValid = templateValid;
        this.templateLineCount = templateLineCount;
    }

    public AccountingAutomationEvent getEvent() {
        return event;
    }

    public String getEventLabel() {
        return event.getLabel();
    }

    public boolean isHasSimpleRule() {
        return hasSimpleRule;
    }

    public boolean isSimpleRuleActive() {
        return simpleRuleActive;
    }

    public boolean isHasTemplate() {
        return hasTemplate;
    }

    public boolean isTemplateActive() {
        return templateActive;
    }

    public boolean isTemplateValid() {
        return templateValid;
    }

    public int getTemplateLineCount() {
        return templateLineCount;
    }

    public boolean isReady() {
        return hasTemplate && templateActive && templateValid;
    }

    public String getTemplateStatusLabel() {
        if (!hasTemplate) {
            return "Falta plantilla";
        }
        if (!templateActive) {
            return "Plantilla inactiva";
        }
        if (!templateValid) {
            return "Plantilla incompleta";
        }
        return "Lista";
    }

    public String getTemplateBadgeClass() {
        return isReady() ? "text-bg-success" : "text-bg-warning";
    }

    public String getSimpleRuleStatusLabel() {
        if (!hasSimpleRule) {
            return "Sin regla simple";
        }
        return simpleRuleActive ? "Activa" : "Inactiva";
    }

    public String getSimpleRuleBadgeClass() {
        if (!hasSimpleRule) {
            return "text-bg-secondary";
        }
        return simpleRuleActive ? "text-bg-success" : "text-bg-warning";
    }
}
