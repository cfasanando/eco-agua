package com.ecoamazonas.eco_agua.marketing;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/marketing/admin")
public class MarketingCampaignsController {

    private final MarketingCampaignsService marketingCampaignsService;

    public MarketingCampaignsController(MarketingCampaignsService marketingCampaignsService) {
        this.marketingCampaignsService = marketingCampaignsService;
    }

    @GetMapping("/campaigns")
    public String dashboard(Model model) {
        model.addAttribute("activePage", "marketing_campaigns");
        model.addAttribute("snapshot", marketingCampaignsService.buildSnapshot());
        return "marketing/admin_campaigns";
    }
}
