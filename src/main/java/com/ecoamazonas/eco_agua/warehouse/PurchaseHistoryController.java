package com.ecoamazonas.eco_agua.warehouse;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDate;

@Controller
@RequestMapping("/warehouse/purchase-history")
public class PurchaseHistoryController {

    private final PurchaseHistoryService purchaseHistoryService;

    public PurchaseHistoryController(PurchaseHistoryService purchaseHistoryService) {
        this.purchaseHistoryService = purchaseHistoryService;
    }

    @GetMapping
    public String index(
            @RequestParam(value = "startDate", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(value = "endDate", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(value = "supplierId", required = false) Long supplierId,
            @RequestParam(value = "productId", required = false) Long productId,
            Model model
    ) {
        DateRange range = normalizeRange(startDate, endDate);
        PurchaseHistorySnapshot snapshot = purchaseHistoryService.buildHistory(
                range.start(),
                range.end(),
                supplierId,
                productId
        );

        model.addAttribute("activePage", "warehouse_purchase_history");
        model.addAttribute("startDate", range.start());
        model.addAttribute("endDate", range.end());
        model.addAttribute("selectedSupplierId", supplierId);
        model.addAttribute("selectedProductId", productId);
        model.addAttribute("summary", snapshot.getSummary());
        model.addAttribute("rows", snapshot.getRows());
        model.addAttribute("suppliers", purchaseHistoryService.findActiveSuppliers());
        model.addAttribute("products", purchaseHistoryService.findActiveProducts());

        return "warehouse/purchase_history";
    }

    private DateRange normalizeRange(LocalDate startDate, LocalDate endDate) {
        LocalDate today = LocalDate.now();
        LocalDate start = startDate != null ? startDate : today.withDayOfMonth(1);
        LocalDate end = endDate != null ? endDate : today;

        if (end.isBefore(start)) {
            return new DateRange(end, start);
        }

        return new DateRange(start, end);
    }

    private record DateRange(LocalDate start, LocalDate end) {
    }
}
