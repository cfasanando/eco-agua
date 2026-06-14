package com.ecoamazonas.eco_agua.delivery;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.util.List;

@Controller
@RequestMapping("/delivery/import")
public class DeliveryImportController {

    private final DeliveryImportService deliveryImportService;

    public DeliveryImportController(DeliveryImportService deliveryImportService) {
        this.deliveryImportService = deliveryImportService;
    }

    @GetMapping
    public String index(Model model) {
        model.addAttribute("activePage", "delivery_import");
        model.addAttribute("today", LocalDate.now());
        model.addAttribute("batches", deliveryImportService.findRecentBatches());
        return "delivery/import";
    }

    @PostMapping
    public String importCsv(@RequestParam("file") MultipartFile file,
                            @RequestParam(value = "routeDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate routeDate,
                            @RequestParam(value = "title", required = false) String title,
                            @RequestParam(value = "deliveryPerson", required = false) String deliveryPerson,
                            RedirectAttributes redirectAttributes) {
        try {
            DeliveryImportBatch batch = deliveryImportService.importCsv(file, routeDate, title, deliveryPerson);
            redirectAttributes.addFlashAttribute("successMessage", "Ruta importada con " + batch.getTotalStops() + " parada(s).");
            return "redirect:/delivery/import/" + batch.getId();
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
            return "redirect:/delivery/import";
        }
    }

    @GetMapping("/{id}")
    public String detail(@PathVariable Long id, Model model) {
        DeliveryImportBatch batch = deliveryImportService.findBatch(id);
        List<DeliveryImportStop> stops = deliveryImportService.findStops(id);
        model.addAttribute("activePage", "delivery_import");
        model.addAttribute("batch", batch);
        model.addAttribute("stops", stops);
        model.addAttribute("statuses", DeliveryImportStopStatus.values());
        model.addAttribute("openStreetMapRouteUrl", deliveryImportService.buildOpenStreetMapRouteUrl(stops));
        model.addAttribute("routeDistance", deliveryImportService.calculateRouteDistance(stops));
        model.addAttribute("totalAmount", deliveryImportService.calculateTotalAmount(stops));
        return "delivery/import_detail";
    }

    @PostMapping("/{batchId}/stops/{stopId}/status")
    public String updateStopStatus(@PathVariable Long batchId,
                                   @PathVariable Long stopId,
                                   @RequestParam("status") DeliveryImportStopStatus status,
                                   @RequestParam(value = "observation", required = false) String observation,
                                   RedirectAttributes redirectAttributes) {
        try {
            deliveryImportService.updateStopStatus(stopId, status, observation);
            redirectAttributes.addFlashAttribute("successMessage", "Estado de parada actualizado.");
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/delivery/import/" + batchId;
    }
}
