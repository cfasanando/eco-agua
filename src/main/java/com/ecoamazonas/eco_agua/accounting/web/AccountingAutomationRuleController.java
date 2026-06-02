package com.ecoamazonas.eco_agua.accounting.web;

import com.ecoamazonas.eco_agua.accounting.AccountingAutomationEvent;
import com.ecoamazonas.eco_agua.accounting.AccountingAutomationRule;
import com.ecoamazonas.eco_agua.accounting.service.AccountingAccountService;
import com.ecoamazonas.eco_agua.accounting.service.AccountingAutomationRuleService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/accounting/rules")
public class AccountingAutomationRuleController {

    private final AccountingAutomationRuleService automationRuleService;
    private final AccountingAccountService accountingAccountService;

    public AccountingAutomationRuleController(
            AccountingAutomationRuleService automationRuleService,
            AccountingAccountService accountingAccountService
    ) {
        this.automationRuleService = automationRuleService;
        this.accountingAccountService = accountingAccountService;
    }

    @GetMapping
    public String index(
            @RequestParam(name = "editId", required = false) Long editId,
            Model model
    ) {
        AccountingAutomationRule selectedRule = automationRuleService.findForEdit(editId);

        model.addAttribute("rules", automationRuleService.findAll());
        model.addAttribute("selectedRule", selectedRule);
        model.addAttribute("eventTypes", AccountingAutomationEvent.values());
        model.addAttribute("configuredEvents", automationRuleService.configuredEvents());
        model.addAttribute("accounts", accountingAccountService.findActive());
        model.addAttribute("activePage", "accounting_rules");

        return "accounting/accounting_rules";
    }

    @PostMapping("/save")
    public String save(
            @RequestParam(name = "id", required = false) Long id,
            @RequestParam AccountingAutomationEvent eventType,
            @RequestParam Long debitAccountId,
            @RequestParam Long creditAccountId,
            @RequestParam(name = "description", required = false) String description,
            @RequestParam(name = "generateDraft", defaultValue = "false") boolean generateDraft,
            @RequestParam(name = "active", defaultValue = "false") boolean active,
            RedirectAttributes redirectAttributes
    ) {
        try {
            automationRuleService.saveFromForm(
                    id,
                    eventType,
                    debitAccountId,
                    creditAccountId,
                    description,
                    generateDraft,
                    active
            );
            redirectAttributes.addFlashAttribute("message", "Regla contable guardada correctamente.");
            redirectAttributes.addFlashAttribute("messageType", "success");
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("message", ex.getMessage());
            redirectAttributes.addFlashAttribute("messageType", "error");
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("message", "No se pudo guardar la regla contable.");
            redirectAttributes.addFlashAttribute("messageType", "error");
        }

        return "redirect:/accounting/rules";
    }

    @PostMapping("/{id}/toggle")
    public String toggle(
            @PathVariable Long id,
            RedirectAttributes redirectAttributes
    ) {
        try {
            automationRuleService.toggleActive(id);
            redirectAttributes.addFlashAttribute("message", "Estado de la regla actualizado correctamente.");
            redirectAttributes.addFlashAttribute("messageType", "success");
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("message", "No se pudo actualizar el estado de la regla.");
            redirectAttributes.addFlashAttribute("messageType", "error");
        }

        return "redirect:/accounting/rules";
    }
}
