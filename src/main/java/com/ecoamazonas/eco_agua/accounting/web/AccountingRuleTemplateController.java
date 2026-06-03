package com.ecoamazonas.eco_agua.accounting.web;

import com.ecoamazonas.eco_agua.accounting.*;
import com.ecoamazonas.eco_agua.accounting.service.AccountingAccountService;
import com.ecoamazonas.eco_agua.accounting.service.AccountingRuleTemplateService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/accounting/rule-templates")
public class AccountingRuleTemplateController {

    private final AccountingRuleTemplateService ruleTemplateService;
    private final AccountingAccountService accountingAccountService;

    public AccountingRuleTemplateController(
            AccountingRuleTemplateService ruleTemplateService,
            AccountingAccountService accountingAccountService
    ) {
        this.ruleTemplateService = ruleTemplateService;
        this.accountingAccountService = accountingAccountService;
    }

    @GetMapping
    public String index(
            @RequestParam(name = "editId", required = false) Long editId,
            Model model
    ) {
        AccountingRuleTemplate selectedTemplate = ruleTemplateService.findForEdit(editId);

        model.addAttribute("templates", ruleTemplateService.findAll());
        model.addAttribute("selectedTemplate", selectedTemplate);
        model.addAttribute("eventTypes", AccountingAutomationEvent.values());
        model.addAttribute("configuredEvents", ruleTemplateService.configuredEvents());
        model.addAttribute("accounts", accountingAccountService.findActive());
        model.addAttribute("lineSides", AccountingRuleLineSide.values());
        model.addAttribute("amountBases", AccountingRuleAmountBase.values());
        model.addAttribute("activePage", "accounting_rule_templates");

        return "accounting/accounting_rule_templates";
    }

    @PostMapping("/save")
    public String save(
            @RequestParam(name = "id", required = false) Long id,
            @RequestParam AccountingAutomationEvent eventType,
            @RequestParam String name,
            @RequestParam(name = "description", required = false) String description,
            @RequestParam(name = "generateDraft", defaultValue = "false") boolean generateDraft,
            @RequestParam(name = "active", defaultValue = "false") boolean active,
            @RequestParam(name = "accountId", required = false) List<Long> accountIds,
            @RequestParam(name = "lineSide", required = false) List<AccountingRuleLineSide> lineSides,
            @RequestParam(name = "amountBase", required = false) List<AccountingRuleAmountBase> amountBases,
            @RequestParam(name = "fixedAmount", required = false) List<String> fixedAmounts,
            @RequestParam(name = "lineDescription", required = false) List<String> lineDescriptions,
            RedirectAttributes redirectAttributes
    ) {
        try {
            ruleTemplateService.saveFromForm(
                    id,
                    eventType,
                    name,
                    description,
                    generateDraft,
                    active,
                    accountIds,
                    lineSides,
                    amountBases,
                    fixedAmounts,
                    lineDescriptions
            );
            redirectAttributes.addFlashAttribute("message", "Plantilla contable guardada correctamente.");
            redirectAttributes.addFlashAttribute("messageType", "success");
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("message", ex.getMessage());
            redirectAttributes.addFlashAttribute("messageType", "error");
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("message", "No se pudo guardar la plantilla contable.");
            redirectAttributes.addFlashAttribute("messageType", "error");
        }

        return "redirect:/accounting/rule-templates";
    }

    @PostMapping("/{id}/toggle")
    public String toggle(
            @PathVariable Long id,
            RedirectAttributes redirectAttributes
    ) {
        try {
            ruleTemplateService.toggleActive(id);
            redirectAttributes.addFlashAttribute("message", "Estado de la plantilla actualizado correctamente.");
            redirectAttributes.addFlashAttribute("messageType", "success");
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("message", "No se pudo actualizar el estado de la plantilla.");
            redirectAttributes.addFlashAttribute("messageType", "error");
        }

        return "redirect:/accounting/rule-templates";
    }
}
