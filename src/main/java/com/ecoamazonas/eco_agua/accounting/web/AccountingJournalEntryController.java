package com.ecoamazonas.eco_agua.accounting.web;

import com.ecoamazonas.eco_agua.accounting.AccountingJournalEntry;
import com.ecoamazonas.eco_agua.accounting.AccountingJournalEntryStatus;
import com.ecoamazonas.eco_agua.accounting.AccountingJournalSourceType;
import com.ecoamazonas.eco_agua.accounting.service.AccountingAccountService;
import com.ecoamazonas.eco_agua.accounting.service.AccountingJournalEntryService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.util.List;

@Controller
@RequestMapping("/accounting/journal-entries")
public class AccountingJournalEntryController {

    private final AccountingJournalEntryService journalEntryService;
    private final AccountingAccountService accountingAccountService;

    public AccountingJournalEntryController(
            AccountingJournalEntryService journalEntryService,
            AccountingAccountService accountingAccountService
    ) {
        this.journalEntryService = journalEntryService;
        this.accountingAccountService = accountingAccountService;
    }

    @GetMapping
    public String index(
            @RequestParam(name = "editId", required = false) Long editId,
            Model model
    ) {
        AccountingJournalEntry selectedEntry = journalEntryService.findForEdit(editId);

        model.addAttribute("entries", journalEntryService.findAll());
        model.addAttribute("selectedEntry", selectedEntry);
        model.addAttribute("accounts", accountingAccountService.findAll());
        model.addAttribute("sourceTypes", AccountingJournalSourceType.values());
        model.addAttribute("entryStatuses", List.of(AccountingJournalEntryStatus.DRAFT, AccountingJournalEntryStatus.POSTED));
        model.addAttribute("today", LocalDate.now());
        model.addAttribute("activePage", "accounting_journal_entries");

        return "accounting/journal_entries";
    }

    @PostMapping("/save")
    public String save(
            @RequestParam(name = "id", required = false) Long id,
            @RequestParam(name = "entryDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate entryDate,
            @RequestParam String description,
            @RequestParam(name = "sourceType", required = false) AccountingJournalSourceType sourceType,
            @RequestParam(name = "status", required = false) AccountingJournalEntryStatus status,
            @RequestParam(name = "accountId", required = false) List<Long> accountIds,
            @RequestParam(name = "lineDescription", required = false) List<String> lineDescriptions,
            @RequestParam(name = "debitAmount", required = false) List<String> debitAmounts,
            @RequestParam(name = "creditAmount", required = false) List<String> creditAmounts,
            RedirectAttributes redirectAttributes
    ) {
        try {
            journalEntryService.saveFromForm(
                    id,
                    entryDate,
                    description,
                    sourceType,
                    status,
                    accountIds,
                    lineDescriptions,
                    debitAmounts,
                    creditAmounts
            );
            redirectAttributes.addFlashAttribute("message", "Asiento contable guardado correctamente.");
            redirectAttributes.addFlashAttribute("messageType", "success");
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("message", ex.getMessage());
            redirectAttributes.addFlashAttribute("messageType", "error");
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("message", "No se pudo guardar el asiento contable.");
            redirectAttributes.addFlashAttribute("messageType", "error");
        }

        return "redirect:/accounting/journal-entries";
    }

    @PostMapping("/{id}/cancel")
    public String cancel(
            @PathVariable Long id,
            RedirectAttributes redirectAttributes
    ) {
        try {
            journalEntryService.cancel(id);
            redirectAttributes.addFlashAttribute("message", "Asiento contable anulado correctamente.");
            redirectAttributes.addFlashAttribute("messageType", "success");
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("message", "No se pudo anular el asiento contable.");
            redirectAttributes.addFlashAttribute("messageType", "error");
        }

        return "redirect:/accounting/journal-entries";
    }
}
