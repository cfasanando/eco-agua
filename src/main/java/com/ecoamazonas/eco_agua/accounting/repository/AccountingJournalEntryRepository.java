package com.ecoamazonas.eco_agua.accounting.repository;

import com.ecoamazonas.eco_agua.accounting.AccountingJournalEntry;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AccountingJournalEntryRepository extends JpaRepository<AccountingJournalEntry, Long> {

    @EntityGraph(attributePaths = {"lines", "lines.account"})
    List<AccountingJournalEntry> findAllByOrderByEntryDateDescIdDesc();

    @Override
    @EntityGraph(attributePaths = {"lines", "lines.account"})
    Optional<AccountingJournalEntry> findById(Long id);
}
