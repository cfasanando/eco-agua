package com.ecoamazonas.eco_agua.delivery;

import com.ecoamazonas.eco_agua.order.OrderStatus;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
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
        model.addAttribute("orderStatuses", Arrays.asList(OrderStatus.REQUESTED, OrderStatus.CREDIT, OrderStatus.PAID));
        model.addAttribute("paymentMethods", deliveryImportService.findPaymentMethods());
        model.addAttribute("openStreetMapRouteUrl", deliveryImportService.buildOpenStreetMapRouteUrl(stops));
        model.addAttribute("routeDistance", deliveryImportService.calculateRouteDistance(stops));
        model.addAttribute("totalAmount", deliveryImportService.calculateTotalAmount(stops));
        model.addAttribute("clientLinkedStops", deliveryImportService.countClientLinkedStops(stops));
        model.addAttribute("orderLinkedStops", deliveryImportService.countOrderLinkedStops(stops));
        model.addAttribute("linkedOrderAmount", deliveryImportService.calculateLinkedOrderAmount(stops));
        return "delivery/import_detail";
    }

    @PostMapping("/{batchId}/integrate-clients")
    public String integrateClients(@PathVariable Long batchId,
                                   @RequestParam(value = "createIfMissing", defaultValue = "true") boolean createIfMissing,
                                   @RequestParam(value = "updateExisting", defaultValue = "true") boolean updateExisting,
                                   RedirectAttributes redirectAttributes) {
        try {
            int updated = deliveryImportService.linkOrCreateClientsForBatch(batchId, createIfMissing, updateExisting);
            redirectAttributes.addFlashAttribute("successMessage", updated + " cliente(s) vinculados o creados.");
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/delivery/import/" + batchId;
    }

    @PostMapping("/{batchId}/create-orders")
    public String createOrders(@PathVariable Long batchId,
                               @RequestParam(value = "orderStatus", required = false) OrderStatus orderStatus,
                               RedirectAttributes redirectAttributes) {
        try {
            int created = deliveryImportService.createOrdersForBatch(batchId, orderStatus);
            redirectAttributes.addFlashAttribute("successMessage", created + " pedido(s) creados desde la ruta importada.");
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/delivery/import/" + batchId;
    }

    @PostMapping("/{batchId}/stops/{stopId}/status")
    public String updateStopStatus(@PathVariable Long batchId,
                                   @PathVariable Long stopId,
                                   @RequestParam("status") DeliveryImportStopStatus status,
                                   @RequestParam(value = "observation", required = false) String observation,
                                   RedirectAttributes redirectAttributes) {
        try {
            deliveryImportService.updateStopStatus(batchId, stopId, status, observation);
            redirectAttributes.addFlashAttribute("successMessage", "Estado de parada actualizado.");
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/delivery/import/" + batchId;
    }

    @PostMapping("/{batchId}/stops/{stopId}/client")
    public String linkClient(@PathVariable Long batchId,
                             @PathVariable Long stopId,
                             @RequestParam(value = "createIfMissing", defaultValue = "true") boolean createIfMissing,
                             @RequestParam(value = "updateExisting", defaultValue = "true") boolean updateExisting,
                             RedirectAttributes redirectAttributes) {
        try {
            deliveryImportService.linkOrCreateClient(batchId, stopId, createIfMissing, updateExisting);
            redirectAttributes.addFlashAttribute("successMessage", "Cliente vinculado correctamente.");
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/delivery/import/" + batchId;
    }

    @PostMapping("/{batchId}/stops/{stopId}/order")
    public String createOrder(@PathVariable Long batchId,
                              @PathVariable Long stopId,
                              @RequestParam(value = "orderStatus", required = false) OrderStatus orderStatus,
                              RedirectAttributes redirectAttributes) {
        try {
            deliveryImportService.createOrderFromStop(batchId, stopId, orderStatus);
            redirectAttributes.addFlashAttribute("successMessage", "Pedido creado desde la parada importada.");
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/delivery/import/" + batchId;
    }

    @PostMapping("/{batchId}/stops/{stopId}/payment")
    public String registerPayment(@PathVariable Long batchId,
                                  @PathVariable Long stopId,
                                  @RequestParam(value = "paymentAmount", required = false) BigDecimal paymentAmount,
                                  @RequestParam(value = "paymentMethod", required = false) String paymentMethod,
                                  @RequestParam(value = "paymentReference", required = false) String paymentReference,
                                  RedirectAttributes redirectAttributes) {
        try {
            deliveryImportService.registerPaymentForStop(batchId, stopId, paymentAmount, paymentMethod, paymentReference);
            redirectAttributes.addFlashAttribute("successMessage", "Cobro registrado y vinculado al pedido.");
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/delivery/import/" + batchId;
    }
}
