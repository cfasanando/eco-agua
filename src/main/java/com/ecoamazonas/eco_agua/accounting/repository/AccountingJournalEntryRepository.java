package com.ecoamazonas.eco_agua.accounting.repository;

import com.ecoamazonas.eco_agua.accounting.AccountingAutomationEvent;
import com.ecoamazonas.eco_agua.accounting.AccountingJournalEntry;
import com.ecoamazonas.eco_agua.accounting.AccountingJournalEntryStatus;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface AccountingJournalEntryRepository extends JpaRepository<AccountingJournalEntry, Long> {

    @EntityGraph(attributePaths = {"lines", "lines.account"})
    List<AccountingJournalEntry> findAllByOrderByEntryDateDescIdDesc();

    @EntityGraph(attributePaths = {"lines", "lines.account"})
    List<AccountingJournalEntry> findByEntryDateBetweenOrderByEntryDateAscIdAsc(LocalDate startDate, LocalDate endDate);

    boolean existsBySourceEventAndSourceReferenceIdAndStatusNot(
            AccountingAutomationEvent sourceEvent,
            Long sourceReferenceId,
            AccountingJournalEntryStatus status
    );

    @EntityGraph(attributePaths = {"lines", "lines.account"})
    List<AccountingJournalEntry> findBySourceEventAndSourceReferenceIdAndStatus(
            AccountingAutomationEvent sourceEvent,
            Long sourceReferenceId,
            AccountingJournalEntryStatus status
    );

    @Override
    @EntityGraph(attributePaths = {"lines", "lines.account"})
    Optional<AccountingJournalEntry> findById(Long id);
}
