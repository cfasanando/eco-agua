package com.ecoamazonas.eco_agua.delivery;

import com.ecoamazonas.eco_agua.order.SaleOrder;
import com.ecoamazonas.eco_agua.user.EmployeeRepository;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.util.UriComponentsBuilder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Controller
@RequestMapping("/delivery")
public class DeliveryDailyController {

    private final DeliveryDailyService deliveryDailyService;
    private final EmployeeRepository employeeRepository;

    public DeliveryDailyController(DeliveryDailyService deliveryDailyService, EmployeeRepository employeeRepository) {
        this.deliveryDailyService = deliveryDailyService;
        this.employeeRepository = employeeRepository;
    }

    @GetMapping
    public String index(
            @RequestParam(value = "date", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(value = "deliveryPerson", required = false) String deliveryPerson,
            @RequestParam(value = "deliveryStatus", required = false) DeliveryStatus deliveryStatus,
            Model model
    ) {
        LocalDate effectiveDate = date != null ? date : LocalDate.now();
        List<DeliveryDailyRow> rows = deliveryDailyService.findRows(effectiveDate, deliveryPerson, deliveryStatus);

        model.addAttribute("activePage", "delivery_daily");
        model.addAttribute("today", effectiveDate);
        model.addAttribute("rows", rows);
        model.addAttribute("deliveryEmployees", employeeRepository.findByActiveTrueOrderByFirstNameAscLastNameAsc());
        model.addAttribute("deliveryStatuses", DeliveryStatus.values());
        model.addAttribute("selectedDeliveryPerson", deliveryPerson);
        model.addAttribute("selectedDeliveryStatus", deliveryStatus);
        model.addAttribute("totalAmount", rows.stream()
                .map(DeliveryDailyRow::getTotalAmount)
                .filter(amount -> amount != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add));
        model.addAttribute("pendingCount", countByStatus(rows, DeliveryStatus.PENDING));
        model.addAttribute("inRouteCount", countByStatus(rows, DeliveryStatus.IN_ROUTE));
        model.addAttribute("deliveredCount", countByStatus(rows, DeliveryStatus.DELIVERED));
        model.addAttribute("notDeliveredCount", countByStatus(rows, DeliveryStatus.NOT_DELIVERED));
        model.addAttribute("rescheduledCount", countByStatus(rows, DeliveryStatus.RESCHEDULED));
        model.addAttribute("canceledCount", countByStatus(rows, DeliveryStatus.CANCELED));

        return "delivery/daily_list";
    }

    @GetMapping("/orders/{id}")
    public String detail(@PathVariable Long id, Model model) {
        SaleOrder order = deliveryDailyService.findDetailedOrder(id);
        model.addAttribute("activePage", "delivery_daily");
        model.addAttribute("order", order);
        model.addAttribute("zones", deliveryDailyService.findZones());
        model.addAttribute("events", deliveryDailyService.findEvents(id));
        model.addAttribute("whatsappUrl", deliveryDailyService.buildWhatsappUrl(order));
        model.addAttribute("mapsUrl", deliveryDailyService.buildGoogleMapsUrl(order));
        return "delivery/daily_detail";
    }

    @PostMapping("/orders/{id}/route")
    public String updateRoute(@PathVariable Long id,
                              @RequestParam(value = "deliveryZoneId", required = false) Long deliveryZoneId,
                              @RequestParam(value = "deliveryOrderIndex", required = false) Integer deliveryOrderIndex,
                              @RequestParam(value = "deliveryPerson", required = false) String deliveryPerson,
                              RedirectAttributes redirectAttributes) {
        try {
            deliveryDailyService.updateRoute(id, deliveryZoneId, deliveryOrderIndex, deliveryPerson);
            redirectAttributes.addFlashAttribute("successMessage", "Datos de ruta actualizados correctamente.");
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/delivery/orders/" + id;
    }

    @PostMapping("/orders/{id}/status")
    public String updateStatus(@PathVariable Long id,
                               @RequestParam("deliveryStatus") DeliveryStatus deliveryStatus,
                               @RequestParam(value = "observation", required = false) String observation,
                               RedirectAttributes redirectAttributes) {
        try {
            switch (deliveryStatus) {
                case IN_ROUTE -> deliveryDailyService.markInRoute(id, observation);
                case DELIVERED -> deliveryDailyService.markDelivered(id, observation);
                case NOT_DELIVERED -> deliveryDailyService.markNotDelivered(id, observation);
                case RESCHEDULED -> deliveryDailyService.markRescheduled(id, observation);
                case CANCELED -> deliveryDailyService.markCanceled(id, observation);
                default -> throw new IllegalArgumentException("Unsupported delivery status for manual change.");
            }
            redirectAttributes.addFlashAttribute("successMessage", "Estado de entrega actualizado correctamente.");
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/delivery/orders/" + id;
    }

    @PostMapping("/orders/{id}/quick-status")
    public String updateQuickStatus(@PathVariable Long id,
                                    @RequestParam("deliveryStatus") DeliveryStatus deliveryStatus,
                                    @RequestParam(value = "date", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
                                    @RequestParam(value = "deliveryPerson", required = false) String deliveryPerson,
                                    @RequestParam(value = "selectedDeliveryStatus", required = false) DeliveryStatus selectedDeliveryStatus,
                                    RedirectAttributes redirectAttributes) {
        try {
            String observation = "Actualizado desde la lista diaria de entregas.";
            switch (deliveryStatus) {
                case IN_ROUTE -> deliveryDailyService.markInRoute(id, observation);
                case DELIVERED -> deliveryDailyService.markDelivered(id, observation);
                case NOT_DELIVERED -> deliveryDailyService.markNotDelivered(id, observation);
                default -> throw new IllegalArgumentException("Unsupported quick delivery status.");
            }
            redirectAttributes.addFlashAttribute("successMessage", "Estado de entrega actualizado.");
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }

        UriComponentsBuilder redirect = UriComponentsBuilder.fromPath("/delivery");
        if (date != null) {
            redirect.queryParam("date", date);
        }
        if (deliveryPerson != null && !deliveryPerson.isBlank()) {
            redirect.queryParam("deliveryPerson", deliveryPerson);
        }
        if (selectedDeliveryStatus != null) {
            redirect.queryParam("deliveryStatus", selectedDeliveryStatus);
        }
        return "redirect:" + redirect.toUriString();
    }

    private long countByStatus(List<DeliveryDailyRow> rows, DeliveryStatus status) {
        return rows.stream().filter(row -> row.getDeliveryStatus() == status).count();
    }
}
