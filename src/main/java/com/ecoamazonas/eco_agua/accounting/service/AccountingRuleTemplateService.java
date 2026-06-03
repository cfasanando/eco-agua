package com.ecoamazonas.eco_agua.accounting.service;

import com.ecoamazonas.eco_agua.accounting.*;
import com.ecoamazonas.eco_agua.accounting.repository.AccountingAccountRepository;
import com.ecoamazonas.eco_agua.accounting.repository.AccountingRuleTemplateRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

@Service
public class AccountingRuleTemplateService {

    private final AccountingRuleTemplateRepository templateRepository;
    private final AccountingAccountRepository accountRepository;

    public AccountingRuleTemplateService(
            AccountingRuleTemplateRepository templateRepository,
            AccountingAccountRepository accountRepository
    ) {
        this.templateRepository = templateRepository;
        this.accountRepository = accountRepository;
    }

    public List<AccountingRuleTemplate> findAll() {
        return templateRepository.findAllByOrderByEventTypeAsc();
    }

    public AccountingRuleTemplate findForEdit(Long id) {
        if (id == null) {
            return null;
        }
        return templateRepository.findById(id).orElse(null);
    }

    public Set<AccountingAutomationEvent> configuredEvents() {
        Set<AccountingAutomationEvent> configured = EnumSet.noneOf(AccountingAutomationEvent.class);
        findAll().forEach(template -> configured.add(template.getEventType()));
        return configured;
    }

    @Transactional
    public void saveFromForm(
            Long id,
            AccountingAutomationEvent eventType,
            String name,
            String description,
            boolean generateDraft,
            boolean active,
            List<Long> accountIds,
            List<AccountingRuleLineSide> lineSides,
            List<AccountingRuleAmountBase> amountBases,
            List<String> fixedAmounts,
            List<String> lineDescriptions
    ) {
        if (eventType == null) {
            throw new IllegalArgumentException("Automation event is required.");
        }

        String normalizedName = normalize(name);
        if (normalizedName == null) {
            throw new IllegalArgumentException("Template name is required.");
        }

        AccountingRuleTemplate template = id == null
                ? new AccountingRuleTemplate()
                : templateRepository.findById(id)
                    .orElseThrow(() -> new IllegalArgumentException("Accounting rule template was not found."));

        templateRepository.findByEventType(eventType)
                .filter(existing -> template.getId() == null || !existing.getId().equals(template.getId()))
                .ifPresent(existing -> {
                    throw new IllegalArgumentException("This automation event already has a multi-line template.");
                });

        template.setEventType(eventType);
        template.setName(normalizedName);
        template.setDescription(normalize(description));
        template.setGenerateDraft(generateDraft);
        template.setActive(active);
        template.setLines(buildLines(accountIds, lineSides, amountBases, fixedAmounts, lineDescriptions));

        validateTemplate(template);

        templateRepository.save(template);
    }

    @Transactional
    public void toggleActive(Long id) {
        AccountingRuleTemplate template = templateRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Accounting rule template was not found."));
        template.setActive(!template.isActive());
        templateRepository.save(template);
    }

    private List<AccountingRuleTemplateLine> buildLines(
            List<Long> accountIds,
            List<AccountingRuleLineSide> lineSides,
            List<AccountingRuleAmountBase> amountBases,
            List<String> fixedAmounts,
            List<String> lineDescriptions
    ) {
        List<AccountingRuleTemplateLine> lines = new ArrayList<>();
        int rows = maxSize(accountIds, lineSides, amountBases, fixedAmounts, lineDescriptions);

        for (int i = 0; i < rows; i++) {
            Long accountId = getValue(accountIds, i);
            AccountingRuleLineSide lineSide = getValue(lineSides, i);
            AccountingRuleAmountBase amountBase = getValue(amountBases, i);
            BigDecimal fixedAmount = parseAmount(getValue(fixedAmounts, i));
            String lineDescription = normalize(getValue(lineDescriptions, i));

            boolean hasContent = accountId != null
                    || lineSide != null
                    || amountBase != null
                    || fixedAmount.compareTo(BigDecimal.ZERO) > 0
                    || lineDescription != null;

            if (!hasContent) {
                continue;
            }

            if (accountId == null) {
                throw new IllegalArgumentException("Each template line must have an account.");
            }
            if (lineSide == null) {
                throw new IllegalArgumentException("Each template line must have a debit or credit side.");
            }
            if (amountBase == null) {
                throw new IllegalArgumentException("Each template line must have an amount base.");
            }
            if (amountBase == AccountingRuleAmountBase.FIXED_AMOUNT && fixedAmount.compareTo(BigDecimal.ZERO) <= 0) {
                throw new IllegalArgumentException("Lines using fixed amount must have an amount greater than zero.");
            }

            AccountingAccount account = accountRepository.findById(accountId)
                    .orElseThrow(() -> new IllegalArgumentException("One selected account was not found."));

            if (!account.isActive()) {
                throw new IllegalArgumentException("Selected accounts must be active.");
            }

            AccountingRuleTemplateLine line = new AccountingRuleTemplateLine();
            line.setLineOrder(lines.size() + 1);
            line.setAccount(account);
            line.setLineSide(lineSide);
            line.setAmountBase(amountBase);
            line.setFixedAmount(fixedAmount);
            line.setDescription(lineDescription);
            lines.add(line);
        }

        return lines;
    }

    private void validateTemplate(AccountingRuleTemplate template) {
        if (template.getLines().size() < 2) {
            throw new IllegalArgumentException("A multi-line accounting template needs at least two lines.");
        }
        if (template.getDebitLineCount() == 0) {
            throw new IllegalArgumentException("A template needs at least one debit line.");
        }
        if (template.getCreditLineCount() == 0) {
            throw new IllegalArgumentException("A template needs at least one credit line.");
        }
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

        try {
            BigDecimal amount = new BigDecimal(normalized).setScale(2, RoundingMode.HALF_UP);
            if (amount.compareTo(BigDecimal.ZERO) < 0) {
                throw new IllegalArgumentException("Fixed amounts cannot be negative.");
            }
            return amount;
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("Fixed amounts must be valid numbers.");
        }
    }

    private String normalize(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
