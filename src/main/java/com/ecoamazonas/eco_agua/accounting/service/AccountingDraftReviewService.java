package com.ecoamazonas.eco_agua.accounting.service;

import com.ecoamazonas.eco_agua.accounting.*;
import com.ecoamazonas.eco_agua.accounting.repository.AccountingJournalEntryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class AccountingDraftReviewService {

    private final AccountingJournalEntryRepository journalEntryRepository;

    public AccountingDraftReviewService(AccountingJournalEntryRepository journalEntryRepository) {
        this.journalEntryRepository = journalEntryRepository;
    }

    public AccountingDraftReviewSnapshot buildSnapshot() {
        List<AccountingDraftReviewRow> rows = journalEntryRepository
                .findByStatusAndSourceEventIsNotNullOrderByEntryDateDescIdDesc(AccountingJournalEntryStatus.DRAFT)
                .stream()
                .map(AccountingDraftReviewRow::new)
                .toList();
        return new AccountingDraftReviewSnapshot(rows);
    }

    @Transactional
    public void postDraft(Long id) {
        AccountingJournalEntry entry = findAutomaticDraft(id);
        if (!entry.isBalanced()) {
            throw new IllegalArgumentException("El asiento no cuadra. Revisa debe y haber antes de registrarlo.");
        }
        entry.setStatus(AccountingJournalEntryStatus.POSTED);
        journalEntryRepository.save(entry);
    }

    @Transactional
    public void cancelDraft(Long id) {
        AccountingJournalEntry entry = findAutomaticDraft(id);
        entry.setStatus(AccountingJournalEntryStatus.CANCELLED);
        journalEntryRepository.save(entry);
    }

    private AccountingJournalEntry findAutomaticDraft(Long id) {
        AccountingJournalEntry entry = journalEntryRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("El asiento contable no existe."));

        if (entry.getSourceEvent() == null) {
            throw new IllegalArgumentException("Solo los asientos automáticos se gestionan desde esta revisión.");
        }
        if (entry.getStatus() != AccountingJournalEntryStatus.DRAFT) {
            throw new IllegalArgumentException("Solo se pueden revisar asientos en borrador.");
        }
        return entry;
    }
}
