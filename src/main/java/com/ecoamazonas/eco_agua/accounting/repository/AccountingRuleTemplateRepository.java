package com.ecoamazonas.eco_agua.accounting.repository;

import com.ecoamazonas.eco_agua.accounting.AccountingAutomationEvent;
import com.ecoamazonas.eco_agua.accounting.AccountingRuleTemplate;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AccountingRuleTemplateRepository extends JpaRepository<AccountingRuleTemplate, Long> {

    @EntityGraph(attributePaths = {"lines", "lines.account"})
    List<AccountingRuleTemplate> findAllByOrderByEventTypeAsc();

    @EntityGraph(attributePaths = {"lines", "lines.account"})
    Optional<AccountingRuleTemplate> findByEventType(AccountingAutomationEvent eventType);

    @Override
    @EntityGraph(attributePaths = {"lines", "lines.account"})
    Optional<AccountingRuleTemplate> findById(Long id);
}
