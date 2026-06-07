package com.ecoamazonas.eco_agua.product.cost;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDate;

@Controller
@RequestMapping("/admin/products/channel-profitability")
public class SalesChannelProfitabilityController {

    private final SalesChannelProfitabilityService salesChannelProfitabilityService;

    public SalesChannelProfitabilityController(SalesChannelProfitabilityService salesChannelProfitabilityService) {
        this.salesChannelProfitabilityService = salesChannelProfitabilityService;
    }

    @GetMapping
    public String showChannelProfitability(
            @RequestParam(value = "start", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
            @RequestParam(value = "end", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end,
            Model model
    ) {
        SalesChannelProfitabilitySnapshot snapshot = salesChannelProfitabilityService.buildSnapshot(start, end);
        model.addAttribute("snapshot", snapshot);
        model.addAttribute("startDate", snapshot.getStartDate());
        model.addAttribute("endDate", snapshot.getEndDate());
        return "admin/product_channel_profitability";
    }
}
