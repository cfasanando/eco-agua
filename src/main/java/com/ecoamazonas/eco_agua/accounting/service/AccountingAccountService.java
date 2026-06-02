package com.ecoamazonas.eco_agua.accounting.service;

import com.ecoamazonas.eco_agua.accounting.AccountingAccount;
import com.ecoamazonas.eco_agua.accounting.AccountingAccountType;
import com.ecoamazonas.eco_agua.accounting.repository.AccountingAccountRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class AccountingAccountService {

    private final AccountingAccountRepository accountingAccountRepository;

    public AccountingAccountService(AccountingAccountRepository accountingAccountRepository) {
        this.accountingAccountRepository = accountingAccountRepository;
    }

    public List<AccountingAccount> findAll() {
        return accountingAccountRepository.findAllByOrderByCodeAsc();
    }

    public List<AccountingAccount> findActive() {
        return accountingAccountRepository.findByActiveTrueOrderByCodeAsc();
    }

    public AccountingAccount findForEdit(Long id) {
        if (id == null) {
            return null;
        }

        return accountingAccountRepository.findById(id).orElse(null);
    }

    @Transactional
    public void saveFromForm(
            Long id,
            String code,
            String name,
            AccountingAccountType type,
            String description,
            boolean active
    ) {
        String normalizedCode = normalize(code);
        String normalizedName = normalize(name);

        if (normalizedCode == null || normalizedCode.isBlank()) {
            throw new IllegalArgumentException("Account code is required.");
        }
        if (normalizedName == null || normalizedName.isBlank()) {
            throw new IllegalArgumentException("Account name is required.");
        }
        if (type == null) {
            throw new IllegalArgumentException("Account type is required.");
        }

        AccountingAccount account = id == null
                ? new AccountingAccount()
                : accountingAccountRepository.findById(id)
                    .orElseThrow(() -> new IllegalArgumentException("Account was not found."));

        accountingAccountRepository.findByCode(normalizedCode)
                .filter(existing -> account.getId() == null || !existing.getId().equals(account.getId()))
                .ifPresent(existing -> {
                    throw new IllegalArgumentException("Account code already exists.");
                });

        account.setCode(normalizedCode);
        account.setName(normalizedName);
        account.setType(type);
        account.setDescription(normalize(description));
        account.setActive(active);

        accountingAccountRepository.save(account);
    }

    @Transactional
    public void toggleActive(Long id) {
        AccountingAccount account = accountingAccountRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Account was not found."));
        account.setActive(!account.isActive());
        accountingAccountRepository.save(account);
    }

    private String normalize(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
