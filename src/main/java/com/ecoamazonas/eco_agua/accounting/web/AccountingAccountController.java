package com.ecoamazonas.eco_agua.accounting.web;

import com.ecoamazonas.eco_agua.accounting.AccountingAccount;
import com.ecoamazonas.eco_agua.accounting.AccountingAccountType;
import com.ecoamazonas.eco_agua.accounting.service.AccountingAccountService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/accounting/accounts")
public class AccountingAccountController {

    private final AccountingAccountService accountingAccountService;

    public AccountingAccountController(AccountingAccountService accountingAccountService) {
        this.accountingAccountService = accountingAccountService;
    }

    @GetMapping
    public String index(
            @RequestParam(name = "editId", required = false) Long editId,
            Model model
    ) {
        AccountingAccount selectedAccount = accountingAccountService.findForEdit(editId);

        model.addAttribute("accounts", accountingAccountService.findAll());
        model.addAttribute("selectedAccount", selectedAccount);
        model.addAttribute("accountTypes", AccountingAccountType.values());
        model.addAttribute("activePage", "accounting_accounts");

        return "accounting/account_plan";
    }

    @PostMapping("/save")
    public String save(
            @RequestParam(name = "id", required = false) Long id,
            @RequestParam String code,
            @RequestParam String name,
            @RequestParam AccountingAccountType type,
            @RequestParam(name = "description", required = false) String description,
            @RequestParam(name = "active", defaultValue = "false") boolean active,
            RedirectAttributes redirectAttributes
    ) {
        try {
            accountingAccountService.saveFromForm(id, code, name, type, description, active);
            redirectAttributes.addFlashAttribute("message", "Cuenta contable guardada correctamente.");
            redirectAttributes.addFlashAttribute("messageType", "success");
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("message", ex.getMessage());
            redirectAttributes.addFlashAttribute("messageType", "error");
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("message", "No se pudo guardar la cuenta contable.");
            redirectAttributes.addFlashAttribute("messageType", "error");
        }

        return "redirect:/accounting/accounts";
    }

    @PostMapping("/{id}/toggle")
    public String toggle(
            @PathVariable Long id,
            RedirectAttributes redirectAttributes
    ) {
        try {
            accountingAccountService.toggleActive(id);
            redirectAttributes.addFlashAttribute("message", "Estado de la cuenta actualizado correctamente.");
            redirectAttributes.addFlashAttribute("messageType", "success");
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("message", "No se pudo actualizar el estado de la cuenta.");
            redirectAttributes.addFlashAttribute("messageType", "error");
        }

        return "redirect:/accounting/accounts";
    }
}
