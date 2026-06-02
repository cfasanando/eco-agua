package com.ecoamazonas.eco_agua.accounting.service;

import com.ecoamazonas.eco_agua.accounting.AccountingAccount;
import com.ecoamazonas.eco_agua.accounting.AccountingAutomationEvent;
import com.ecoamazonas.eco_agua.accounting.AccountingAutomationRule;
import com.ecoamazonas.eco_agua.accounting.repository.AccountingAccountRepository;
import com.ecoamazonas.eco_agua.accounting.repository.AccountingAutomationRuleRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;

@Service
public class AccountingAutomationRuleService {

    private final AccountingAutomationRuleRepository automationRuleRepository;
    private final AccountingAccountRepository accountRepository;

    public AccountingAutomationRuleService(
            AccountingAutomationRuleRepository automationRuleRepository,
            AccountingAccountRepository accountRepository
    ) {
        this.automationRuleRepository = automationRuleRepository;
        this.accountRepository = accountRepository;
    }

    public List<AccountingAutomationRule> findAll() {
        return automationRuleRepository.findAllByOrderByEventTypeAsc();
    }

    public AccountingAutomationRule findForEdit(Long id) {
        if (id == null) {
            return null;
        }
        return automationRuleRepository.findById(id).orElse(null);
    }

    public Set<AccountingAutomationEvent> configuredEvents() {
        Set<AccountingAutomationEvent> configured = EnumSet.noneOf(AccountingAutomationEvent.class);
        findAll().forEach(rule -> configured.add(rule.getEventType()));
        return configured;
    }

    @Transactional
    public void saveFromForm(
            Long id,
            AccountingAutomationEvent eventType,
            Long debitAccountId,
            Long creditAccountId,
            String description,
            boolean generateDraft,
            boolean active
    ) {
        if (eventType == null) {
            throw new IllegalArgumentException("Automation event is required.");
        }
        if (debitAccountId == null) {
            throw new IllegalArgumentException("Debit account is required.");
        }
        if (creditAccountId == null) {
            throw new IllegalArgumentException("Credit account is required.");
        }
        if (debitAccountId.equals(creditAccountId)) {
            throw new IllegalArgumentException("Debit and credit accounts must be different.");
        }

        AccountingAutomationRule rule = id == null
                ? new AccountingAutomationRule()
                : automationRuleRepository.findById(id)
                    .orElseThrow(() -> new IllegalArgumentException("Automation rule was not found."));

        automationRuleRepository.findByEventType(eventType)
                .filter(existing -> rule.getId() == null || !existing.getId().equals(rule.getId()))
                .ifPresent(existing -> {
                    throw new IllegalArgumentException("This automation event already has a rule.");
                });

        AccountingAccount debitAccount = accountRepository.findById(debitAccountId)
                .orElseThrow(() -> new IllegalArgumentException("Debit account was not found."));
        AccountingAccount creditAccount = accountRepository.findById(creditAccountId)
                .orElseThrow(() -> new IllegalArgumentException("Credit account was not found."));

        if (!debitAccount.isActive() || !creditAccount.isActive()) {
            throw new IllegalArgumentException("Selected accounts must be active.");
        }

        rule.setEventType(eventType);
        rule.setDebitAccount(debitAccount);
        rule.setCreditAccount(creditAccount);
        rule.setDescription(normalize(description));
        rule.setGenerateDraft(generateDraft);
        rule.setActive(active);

        automationRuleRepository.save(rule);
    }

    @Transactional
    public void toggleActive(Long id) {
        AccountingAutomationRule rule = automationRuleRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Automation rule was not found."));
        rule.setActive(!rule.isActive());
        automationRuleRepository.save(rule);
    }

    private String normalize(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
