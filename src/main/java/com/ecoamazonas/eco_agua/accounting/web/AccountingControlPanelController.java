package com.ecoamazonas.eco_agua.accounting.web;

import com.ecoamazonas.eco_agua.accounting.AccountingControlPanelSnapshot;
import com.ecoamazonas.eco_agua.accounting.service.AccountingControlPanelService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class AccountingControlPanelController {

    private final AccountingControlPanelService controlPanelService;

    public AccountingControlPanelController(AccountingControlPanelService controlPanelService) {
        this.controlPanelService = controlPanelService;
    }

    @GetMapping("/accounting/control-panel")
    public String index(Model model) {
        AccountingControlPanelSnapshot snapshot = controlPanelService.buildSnapshot();
        model.addAttribute("activePage", "accounting_control_panel");
        model.addAttribute("snapshot", snapshot);
        return "accounting/control_panel";
    }
}
