package com.ecoamazonas.eco_agua.delivery;

import com.ecoamazonas.eco_agua.user.EmployeeRepository;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

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
        model.addAttribute("deliveryEmployees", employeeRepository.findActiveByJobPositionName("Repartidor"));
        model.addAttribute("deliveryStatuses", DeliveryStatus.values());
        model.addAttribute("selectedDeliveryPerson", deliveryPerson);
        model.addAttribute("selectedDeliveryStatus", deliveryStatus);
        model.addAttribute("deliveredCount", rows.stream().filter(r -> r.getDeliveryStatus() == DeliveryStatus.DELIVERED).count());
        model.addAttribute("pendingCount", rows.stream().filter(r -> r.getDeliveryStatus() == DeliveryStatus.PENDING).count());

        return "delivery/daily_list";
    }

    @GetMapping("/orders/{id}")
    public String detail(@PathVariable Long id, Model model) {
        model.addAttribute("activePage", "delivery_daily");
        model.addAttribute("order", deliveryDailyService.findDetailedOrder(id));
        model.addAttribute("zones", deliveryDailyService.findZones());
        model.addAttribute("events", deliveryDailyService.findEvents(id));
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
            redirectAttributes.addFlashAttribute("successMessage", "Route data updated successfully.");
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
            redirectAttributes.addFlashAttribute("successMessage", "Delivery status updated successfully.");
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/delivery/orders/" + id;
    }
}
