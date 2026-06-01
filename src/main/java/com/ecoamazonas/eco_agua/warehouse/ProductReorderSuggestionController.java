package com.ecoamazonas.eco_agua.warehouse;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequestMapping("/warehouse/reorder-suggestions")
public class ProductReorderSuggestionController {

    private final ProductReorderSuggestionService suggestionService;

    public ProductReorderSuggestionController(ProductReorderSuggestionService suggestionService) {
        this.suggestionService = suggestionService;
    }

    @GetMapping
    public String index(
            @RequestParam(value = "status", required = false, defaultValue = "NEEDS_REORDER") String status,
            Model model
    ) {
        ProductReorderSuggestionSnapshot snapshot = suggestionService.buildSuggestions(status);

        model.addAttribute("activePage", "warehouse_reorder_suggestions");
        model.addAttribute("selectedStatus", normalizeStatus(status));
        model.addAttribute("summary", snapshot.getSummary());
        model.addAttribute("rows", snapshot.getRows());

        return "warehouse/reorder_suggestions";
    }

    private String normalizeStatus(String status) {
        if (status == null || status.isBlank()) {
            return "NEEDS_REORDER";
        }

        return switch (status.trim().toUpperCase()) {
            case "OUT_OF_STOCK", "LOW_STOCK", "AT_LIMIT", "ALL_CONFIGURED" -> status.trim().toUpperCase();
            default -> "NEEDS_REORDER";
        };
    }
}
