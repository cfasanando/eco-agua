package com.ecoamazonas.eco_agua.accounting.repository;

import com.ecoamazonas.eco_agua.accounting.AccountingAutomationEvent;
import com.ecoamazonas.eco_agua.accounting.AccountingAutomationRule;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AccountingAutomationRuleRepository extends JpaRepository<AccountingAutomationRule, Long> {

    List<AccountingAutomationRule> findAllByOrderByEventTypeAsc();

    Optional<AccountingAutomationRule> findByEventType(AccountingAutomationEvent eventType);
}
