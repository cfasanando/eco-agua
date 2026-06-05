package com.ecoamazonas.eco_agua.production;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Controller
@RequestMapping("/production")
public class ProductionController {

    private final ProductionService productionService;

    public ProductionController(ProductionService productionService) {
        this.productionService = productionService;
    }

    @GetMapping("/overview")
    public String overview(
            @RequestParam(value = "startDate", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(value = "endDate", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            Model model
    ) {
        LocalDate effectiveEnd = endDate != null ? endDate : LocalDate.now();
        LocalDate effectiveStart = startDate != null ? startDate : effectiveEnd.minusDays(30);

        ProductionOverviewSnapshot snapshot = productionService.buildOverview(effectiveStart, effectiveEnd);

        model.addAttribute("activePage", "production");
        model.addAttribute("snapshot", snapshot);
        model.addAttribute("startDate", snapshot.getSummary().getStartDate());
        model.addAttribute("endDate", snapshot.getSummary().getEndDate());

        return "production/overview";
    }

    @GetMapping
    public String index(
            @RequestParam(value = "startDate", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(value = "endDate", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(value = "status", required = false) ProductionStatus status,
            Model model
    ) {
        LocalDate effectiveEnd = endDate != null ? endDate : LocalDate.now();
        LocalDate effectiveStart = startDate != null ? startDate : effectiveEnd.minusDays(30);
        List<ProductionOrder> rows = productionService.findByDateRange(effectiveStart, effectiveEnd, status);

        model.addAttribute("activePage", "production");
        model.addAttribute("rows", rows);
        model.addAttribute("startDate", effectiveStart);
        model.addAttribute("endDate", effectiveEnd);
        model.addAttribute("selectedStatus", status);
        model.addAttribute("statuses", ProductionStatus.values());
        model.addAttribute("confirmedCount", rows.stream().filter(r -> r.getStatus() == ProductionStatus.CONFIRMED).count());
        model.addAttribute("draftCount", rows.stream().filter(r -> r.getStatus() == ProductionStatus.DRAFT).count());

        return "production/list";
    }

    @GetMapping("/new")
    public String newForm(
            @RequestParam(value = "productId", required = false) Long selectedProductId,
            @RequestParam(value = "quantityExpected", required = false) BigDecimal quantityExpected,
            @RequestParam(value = "quantityProduced", required = false) BigDecimal quantityProduced,
            Model model
    ) {
        BigDecimal defaultQuantityExpected = quantityExpected != null && quantityExpected.compareTo(BigDecimal.ZERO) > 0
                ? quantityExpected
                : BigDecimal.ONE;
        BigDecimal defaultQuantityProduced = quantityProduced != null && quantityProduced.compareTo(BigDecimal.ZERO) > 0
                ? quantityProduced
                : defaultQuantityExpected;

        model.addAttribute("activePage", "production");
        model.addAttribute("today", LocalDate.now());
        model.addAttribute("products", productionService.findActiveProducts());
        model.addAttribute("selectedProductId", selectedProductId);
        model.addAttribute("quantityExpected", defaultQuantityExpected);
        model.addAttribute("quantityProduced", defaultQuantityProduced);

        return "production/form";
    }

    @GetMapping("/planning")
    public String planning(
            @RequestParam(value = "productId", required = false) Long productId,
            @RequestParam(value = "quantity", required = false) BigDecimal quantity,
            Model model
    ) {
        ProductionPlanningSnapshot snapshot = productionService.buildPlanning(productId, quantity);

        model.addAttribute("activePage", "production_planning");
        model.addAttribute("snapshot", snapshot);
        model.addAttribute("products", productionService.findActiveProducts());
        model.addAttribute("selectedProductId", productId);
        model.addAttribute("quantity", snapshot.getSummary().getPlannedQuantity());

        return "production/planning";
    }

    @GetMapping("/product/{productId}/recipe")
    @ResponseBody
    public List<ProductionRecipeLine> getRecipe(
            @PathVariable Long productId,
            @RequestParam(value = "quantityProduced", required = false) BigDecimal quantityProduced
    ) {
        return productionService.buildRecipeLines(productId, quantityProduced);
    }

    @PostMapping
    public String createDraft(
            @RequestParam("productionDate")
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate productionDate,
            @RequestParam("productId") Long productId,
            @RequestParam(value = "quantityExpected", required = false) BigDecimal quantityExpected,
            @RequestParam("quantityProduced") BigDecimal quantityProduced,
            @RequestParam(value = "batchCode", required = false) String batchCode,
            @RequestParam(value = "expiryDate", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate expiryDate,
            @RequestParam(value = "expiryObservation", required = false) String expiryObservation,
            @RequestParam(value = "observation", required = false) String observation,
            @RequestParam(value = "lossReason", required = false) String lossReason,
            @RequestParam(value = "supplyId", required = false) List<Long> supplyIds,
            @RequestParam(value = "quantityUsed", required = false) List<BigDecimal> quantitiesUsed,
            RedirectAttributes redirectAttributes
    ) {
        try {
            ProductionOrder order = productionService.createDraft(
                    productionDate,
                    productId,
                    quantityExpected,
                    quantityProduced,
                    batchCode,
                    expiryDate,
                    expiryObservation,
                    observation,
                    lossReason,
                    supplyIds,
                    quantitiesUsed
            );

            redirectAttributes.addFlashAttribute("successMessage", "Production draft created successfully.");
            return "redirect:/production/" + order.getId();
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
            return "redirect:/production/new";
        }
    }



    @GetMapping("/traceability")
    public String traceability(
            @RequestParam(value = "startDate", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(value = "endDate", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(value = "productId", required = false) Long productId,
            @RequestParam(value = "batchCode", required = false) String batchCode,
            @RequestParam(value = "status", required = false) ProductionStatus status,
            @RequestParam(value = "qualityStatus", required = false) ProductionQualityStatus qualityStatus,
            Model model
    ) {
        LocalDate effectiveEnd = endDate != null ? endDate : LocalDate.now();
        LocalDate effectiveStart = startDate != null ? startDate : effectiveEnd.minusDays(30);

        ProductionTraceabilitySnapshot snapshot = productionService.buildTraceability(
                effectiveStart,
                effectiveEnd,
                productId,
                batchCode,
                status,
                qualityStatus
        );

        model.addAttribute("activePage", "production_traceability");
        model.addAttribute("snapshot", snapshot);
        model.addAttribute("startDate", snapshot.getSummary().getStartDate());
        model.addAttribute("endDate", snapshot.getSummary().getEndDate());
        model.addAttribute("selectedProductId", productId);
        model.addAttribute("selectedBatchCode", batchCode);
        model.addAttribute("selectedStatus", status);
        model.addAttribute("selectedQualityStatus", qualityStatus);
        model.addAttribute("statuses", ProductionStatus.values());
        model.addAttribute("qualityStatuses", ProductionQualityStatus.values());
        model.addAttribute("products", productionService.findActiveProducts());

        return "production/traceability";
    }

    @GetMapping("/reports")
    public String reports(
            @RequestParam(value = "startDate", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(value = "endDate", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            Model model
    ) {
        LocalDate effectiveEnd = endDate != null ? endDate : LocalDate.now();
        LocalDate effectiveStart = startDate != null ? startDate : effectiveEnd.minusDays(30);

        ProductionReportSnapshot snapshot = productionService.buildReport(effectiveStart, effectiveEnd);

        model.addAttribute("activePage", "production_reports");
        model.addAttribute("snapshot", snapshot);
        model.addAttribute("startDate", snapshot.getSummary().getStartDate());
        model.addAttribute("endDate", snapshot.getSummary().getEndDate());

        return "production/reports";
    }

    @GetMapping("/expiry")
    public String expiryDashboard(
            @RequestParam(value = "startDate", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(value = "endDate", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(value = "productId", required = false) Long productId,
            @RequestParam(value = "batchCode", required = false) String batchCode,
            @RequestParam(value = "status", required = false) ProductionStatus status,
            @RequestParam(value = "expiryStatus", required = false) ProductionExpiryStatus expiryStatus,
            Model model
    ) {
        LocalDate effectiveEnd = endDate != null ? endDate : LocalDate.now();
        LocalDate effectiveStart = startDate != null ? startDate : effectiveEnd.minusDays(30);

        ProductionExpirySnapshot snapshot = productionService.buildExpiryDashboard(
                effectiveStart,
                effectiveEnd,
                productId,
                batchCode,
                status,
                expiryStatus
        );

        model.addAttribute("activePage", "production_expiry");
        model.addAttribute("snapshot", snapshot);
        model.addAttribute("startDate", snapshot.getSummary().getStartDate());
        model.addAttribute("endDate", snapshot.getSummary().getEndDate());
        model.addAttribute("selectedProductId", productId);
        model.addAttribute("selectedBatchCode", batchCode);
        model.addAttribute("selectedStatus", status);
        model.addAttribute("selectedExpiryStatus", expiryStatus);
        model.addAttribute("statuses", ProductionStatus.values());
        model.addAttribute("expiryStatuses", ProductionExpiryStatus.values());
        model.addAttribute("products", productionService.findActiveProducts());

        return "production/expiry";
    }

    @GetMapping("/quality")
    public String qualityDashboard(
            @RequestParam(value = "startDate", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(value = "endDate", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(value = "qualityStatus", required = false) ProductionQualityStatus qualityStatus,
            Model model
    ) {
        LocalDate effectiveEnd = endDate != null ? endDate : LocalDate.now();
        LocalDate effectiveStart = startDate != null ? startDate : effectiveEnd.minusDays(30);

        ProductionQualitySnapshot snapshot = productionService.buildQualityDashboard(effectiveStart, effectiveEnd, qualityStatus);

        model.addAttribute("activePage", "production_quality");
        model.addAttribute("snapshot", snapshot);
        model.addAttribute("startDate", snapshot.getSummary().getStartDate());
        model.addAttribute("endDate", snapshot.getSummary().getEndDate());
        model.addAttribute("selectedQualityStatus", qualityStatus);
        model.addAttribute("qualityStatuses", ProductionQualityStatus.values());

        return "production/quality";
    }

    @GetMapping("/{id}/quality")
    public String qualityForm(@PathVariable Long id, Model model) {
        model.addAttribute("activePage", "production_quality");
        model.addAttribute("order", productionService.findDetailedById(id));
        model.addAttribute("qualityStatuses", ProductionQualityStatus.values());

        return "production/quality_form";
    }

    @PostMapping("/{id}/quality")
    public String updateQuality(
            @PathVariable Long id,
            @RequestParam(value = "qualityStatus", required = false) ProductionQualityStatus qualityStatus,
            @RequestParam(value = "qualityCleaningOk", defaultValue = "false") boolean qualityCleaningOk,
            @RequestParam(value = "qualityPackagingOk", defaultValue = "false") boolean qualityPackagingOk,
            @RequestParam(value = "qualityLabelingOk", defaultValue = "false") boolean qualityLabelingOk,
            @RequestParam(value = "qualityProductOk", defaultValue = "false") boolean qualityProductOk,
            @RequestParam(value = "qualityCheckedBy", required = false) String qualityCheckedBy,
            @RequestParam(value = "qualityObservation", required = false) String qualityObservation,
            RedirectAttributes redirectAttributes
    ) {
        try {
            productionService.updateQualityControl(
                    id,
                    qualityStatus,
                    qualityCleaningOk,
                    qualityPackagingOk,
                    qualityLabelingOk,
                    qualityProductOk,
                    qualityCheckedBy,
                    qualityObservation
            );
            redirectAttributes.addFlashAttribute("successMessage", "Control de calidad guardado correctamente.");
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }

        return "redirect:/production/" + id + "/quality";
    }

    @GetMapping("/{id}/sheet")
    public String sheet(@PathVariable Long id, Model model) {
        model.addAttribute("activePage", "production");
        model.addAttribute("order", productionService.findDetailedById(id));

        return "production/sheet";
    }

    @GetMapping("/{id}")
    public String detail(@PathVariable Long id, Model model) {
        model.addAttribute("activePage", "production");
        model.addAttribute("order", productionService.findDetailedById(id));

        return "production/detail";
    }

    @PostMapping("/{id}/confirm")
    public String confirm(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            productionService.confirm(id);
            redirectAttributes.addFlashAttribute("successMessage", "Production confirmed successfully.");
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }

        return "redirect:/production/" + id;
    }

    @PostMapping("/{id}/cancel")
    public String cancel(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            productionService.cancel(id);
            redirectAttributes.addFlashAttribute("successMessage", "Production canceled successfully.");
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }

        return "redirect:/production/" + id;
    }

    @PostMapping("/{id}/delete")
    public String deleteDraft(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            productionService.deleteDraft(id);
            redirectAttributes.addFlashAttribute("successMessage", "Production draft deleted successfully.");
            return "redirect:/production";
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
            return "redirect:/production/" + id;
        }
    }
}
