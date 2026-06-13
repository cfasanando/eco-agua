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
        model.addAttribute("locatedCount", rows.stream().filter(DeliveryDailyRow::hasLocation).count());
        model.addAttribute("openStreetMapRouteUrl", deliveryDailyService.buildOpenStreetMapRouteUrl(rows));

        return "delivery/daily_list";
    }

    @GetMapping("/map")
    public String map(
            @RequestParam(value = "date", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(value = "deliveryPerson", required = false) String deliveryPerson,
            @RequestParam(value = "deliveryStatus", required = false) DeliveryStatus deliveryStatus,
            Model model
    ) {
        LocalDate effectiveDate = date != null ? date : LocalDate.now();
        List<DeliveryDailyRow> rows = deliveryDailyService.findRows(effectiveDate, deliveryPerson, deliveryStatus);
        List<DeliveryDailyRow> locatedRows = rows.stream()
                .filter(DeliveryDailyRow::hasLocation)
                .toList();

        model.addAttribute("activePage", "delivery_map");
        model.addAttribute("today", effectiveDate);
        model.addAttribute("rows", rows);
        model.addAttribute("locatedRows", locatedRows);
        model.addAttribute("missingLocationCount", rows.size() - locatedRows.size());
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
        model.addAttribute("openStreetMapRouteUrl", deliveryDailyService.buildOpenStreetMapRouteUrl(rows));

        return "delivery/map";
    }

    @GetMapping("/routes")
    public String routes(
            @RequestParam(value = "date", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(value = "deliveryPerson", required = false) String deliveryPerson,
            @RequestParam(value = "deliveryStatus", required = false) DeliveryStatus deliveryStatus,
            Model model
    ) {
        LocalDate effectiveDate = date != null ? date : LocalDate.now();
        List<DeliveryDailyRow> rows = deliveryDailyService.findRows(effectiveDate, deliveryPerson, deliveryStatus);
        List<DeliveryDailyRow> routeRows = deliveryDailyService.buildSuggestedRoute(deliveryDailyService.filterRouteRows(rows, deliveryStatus));
        DeliveryRouteSummary routeSummary = deliveryDailyService.buildRouteSummary(rows, routeRows);

        model.addAttribute("activePage", "delivery_routes");
        model.addAttribute("today", effectiveDate);
        model.addAttribute("rows", rows);
        model.addAttribute("routeRows", routeRows);
        model.addAttribute("routeSummary", routeSummary);
        model.addAttribute("deliveryEmployees", employeeRepository.findByActiveTrueOrderByFirstNameAscLastNameAsc());
        model.addAttribute("deliveryStatuses", DeliveryStatus.values());
        model.addAttribute("selectedDeliveryPerson", deliveryPerson);
        model.addAttribute("selectedDeliveryStatus", deliveryStatus);
        model.addAttribute("totalAmount", routeRows.stream()
                .map(DeliveryDailyRow::getTotalAmount)
                .filter(amount -> amount != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add));
        model.addAttribute("pendingCount", countByStatus(routeRows, DeliveryStatus.PENDING));
        model.addAttribute("inRouteCount", countByStatus(routeRows, DeliveryStatus.IN_ROUTE));
        model.addAttribute("deliveredCount", countByStatus(routeRows, DeliveryStatus.DELIVERED));
        model.addAttribute("notDeliveredCount", countByStatus(routeRows, DeliveryStatus.NOT_DELIVERED));
        model.addAttribute("openStreetMapRouteUrl", deliveryDailyService.buildOpenStreetMapRouteUrl(routeRows));

        return "delivery/routes";
    }


    @GetMapping("/mobile")
    public String mobile(
            @RequestParam(value = "date", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(value = "deliveryPerson", required = false) String deliveryPerson,
            @RequestParam(value = "deliveryStatus", required = false) DeliveryStatus deliveryStatus,
            Model model
    ) {
        LocalDate effectiveDate = date != null ? date : LocalDate.now();
        List<DeliveryDailyRow> rows = deliveryDailyService.findRows(effectiveDate, deliveryPerson, deliveryStatus);
        List<DeliveryDailyRow> routeRows = deliveryDailyService.buildSuggestedRoute(deliveryDailyService.filterRouteRows(rows, deliveryStatus));
        DeliveryRouteSummary routeSummary = deliveryDailyService.buildRouteSummary(rows, routeRows);

        model.addAttribute("activePage", "delivery_mobile");
        model.addAttribute("today", effectiveDate);
        model.addAttribute("rows", rows);
        model.addAttribute("routeRows", routeRows);
        model.addAttribute("routeSummary", routeSummary);
        model.addAttribute("deliveryEmployees", employeeRepository.findByActiveTrueOrderByFirstNameAscLastNameAsc());
        model.addAttribute("deliveryStatuses", DeliveryStatus.values());
        model.addAttribute("selectedDeliveryPerson", deliveryPerson);
        model.addAttribute("selectedDeliveryStatus", deliveryStatus);
        model.addAttribute("totalAmount", routeRows.stream()
                .map(DeliveryDailyRow::getTotalAmount)
                .filter(amount -> amount != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add));
        model.addAttribute("pendingCount", countByStatus(routeRows, DeliveryStatus.PENDING));
        model.addAttribute("inRouteCount", countByStatus(routeRows, DeliveryStatus.IN_ROUTE));
        model.addAttribute("deliveredCount", countByStatus(rows, DeliveryStatus.DELIVERED));
        model.addAttribute("notDeliveredCount", countByStatus(rows, DeliveryStatus.NOT_DELIVERED));
        model.addAttribute("paymentMethods", deliveryDailyService.findPaymentMethods());
        model.addAttribute("incidentReasons", deliveryDailyService.findIncidentReasons());
        model.addAttribute("openStreetMapRouteUrl", deliveryDailyService.buildOpenStreetMapRouteUrl(routeRows));

        return "delivery/mobile";
    }

    @PostMapping("/mobile/orders/{id}/quick-status")
    public String updateMobileQuickStatus(@PathVariable Long id,
                                          @RequestParam("deliveryStatus") DeliveryStatus deliveryStatus,
                                          @RequestParam(value = "observation", required = false) String observation,
                                          @RequestParam(value = "date", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
                                          @RequestParam(value = "deliveryPerson", required = false) String deliveryPerson,
                                          @RequestParam(value = "selectedDeliveryStatus", required = false) DeliveryStatus selectedDeliveryStatus,
                                          RedirectAttributes redirectAttributes) {
        try {
            String effectiveObservation = observation != null && !observation.isBlank()
                    ? observation
                    : "Actualizado desde modo repartidor móvil.";
            switch (deliveryStatus) {
                case IN_ROUTE -> deliveryDailyService.markInRoute(id, effectiveObservation);
                case DELIVERED -> deliveryDailyService.markDelivered(id, effectiveObservation);
                case NOT_DELIVERED -> deliveryDailyService.markNotDelivered(id, effectiveObservation);
                case RESCHEDULED -> deliveryDailyService.markRescheduled(id, effectiveObservation);
                default -> throw new IllegalArgumentException("Unsupported mobile quick delivery status.");
            }
            redirectAttributes.addFlashAttribute("successMessage", "Estado de entrega actualizado.");
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }

        UriComponentsBuilder redirect = UriComponentsBuilder.fromPath("/delivery/mobile");
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

    @PostMapping("/mobile/orders/{id}/outcome")
    public String registerMobileOutcome(@PathVariable Long id,
                                        @RequestParam("deliveryStatus") DeliveryStatus deliveryStatus,
                                        @RequestParam(value = "observation", required = false) String observation,
                                        @RequestParam(value = "incidentReason", required = false) String incidentReason,
                                        @RequestParam(value = "proofReference", required = false) String proofReference,
                                        @RequestParam(value = "paymentAmount", required = false) BigDecimal paymentAmount,
                                        @RequestParam(value = "paymentMethod", required = false) String paymentMethod,
                                        @RequestParam(value = "paymentReference", required = false) String paymentReference,
                                        @RequestParam(value = "date", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
                                        @RequestParam(value = "deliveryPerson", required = false) String deliveryPerson,
                                        @RequestParam(value = "selectedDeliveryStatus", required = false) DeliveryStatus selectedDeliveryStatus,
                                        RedirectAttributes redirectAttributes) {
        try {
            deliveryDailyService.registerDeliveryOutcome(
                    id,
                    deliveryStatus,
                    observation,
                    incidentReason,
                    proofReference,
                    paymentAmount,
                    paymentMethod,
                    paymentReference
            );
            redirectAttributes.addFlashAttribute("successMessage", "Resultado de entrega registrado correctamente.");
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }

        UriComponentsBuilder redirect = UriComponentsBuilder.fromPath("/delivery/mobile");
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

    @PostMapping("/routes/save-order")
    public String saveRouteOrder(@RequestParam(value = "orderIds", required = false) List<Long> orderIds,
                                 @RequestParam(value = "date", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
                                 @RequestParam(value = "deliveryPerson", required = false) String deliveryPerson,
                                 @RequestParam(value = "selectedDeliveryStatus", required = false) DeliveryStatus selectedDeliveryStatus,
                                 RedirectAttributes redirectAttributes) {
        int updated = deliveryDailyService.saveRouteOrder(orderIds, deliveryPerson);
        if (updated > 0) {
            redirectAttributes.addFlashAttribute("successMessage", "Ruta diaria guardada con " + updated + " parada(s).");
        } else {
            redirectAttributes.addFlashAttribute("errorMessage", "No se encontraron paradas para guardar.");
        }

        UriComponentsBuilder redirect = UriComponentsBuilder.fromPath("/delivery/routes");
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

    @PostMapping("/routes/orders/{id}/quick-status")
    public String updateRouteQuickStatus(@PathVariable Long id,
                                         @RequestParam("deliveryStatus") DeliveryStatus deliveryStatus,
                                         @RequestParam(value = "date", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
                                         @RequestParam(value = "deliveryPerson", required = false) String deliveryPerson,
                                         @RequestParam(value = "selectedDeliveryStatus", required = false) DeliveryStatus selectedDeliveryStatus,
                                         RedirectAttributes redirectAttributes) {
        try {
            String observation = "Actualizado desde rutas del día.";
            switch (deliveryStatus) {
                case IN_ROUTE -> deliveryDailyService.markInRoute(id, observation);
                case DELIVERED -> deliveryDailyService.markDelivered(id, observation);
                case NOT_DELIVERED -> deliveryDailyService.markNotDelivered(id, observation);
                case RESCHEDULED -> deliveryDailyService.markRescheduled(id, observation);
                default -> throw new IllegalArgumentException("Unsupported route quick delivery status.");
            }
            redirectAttributes.addFlashAttribute("successMessage", "Estado de entrega actualizado.");
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }

        UriComponentsBuilder redirect = UriComponentsBuilder.fromPath("/delivery/routes");
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

    @GetMapping("/orders/{id}")
    public String detail(@PathVariable Long id, Model model) {
        SaleOrder order = deliveryDailyService.findDetailedOrder(id);
        model.addAttribute("activePage", "delivery_daily");
        model.addAttribute("order", order);
        model.addAttribute("zones", deliveryDailyService.findZones());
        model.addAttribute("events", deliveryDailyService.findEvents(id));
        model.addAttribute("paymentMethods", deliveryDailyService.findPaymentMethods());
        model.addAttribute("incidentReasons", deliveryDailyService.findIncidentReasons());
        model.addAttribute("whatsappUrl", deliveryDailyService.buildWhatsappUrl(order));
        model.addAttribute("mapsUrl", deliveryDailyService.buildGoogleMapsUrl(order));
        model.addAttribute("openStreetMapUrl", deliveryDailyService.buildOpenStreetMapUrl(order));
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

    @PostMapping("/orders/{id}/outcome")
    public String registerDetailOutcome(@PathVariable Long id,
                                        @RequestParam("deliveryStatus") DeliveryStatus deliveryStatus,
                                        @RequestParam(value = "observation", required = false) String observation,
                                        @RequestParam(value = "incidentReason", required = false) String incidentReason,
                                        @RequestParam(value = "proofReference", required = false) String proofReference,
                                        @RequestParam(value = "paymentAmount", required = false) BigDecimal paymentAmount,
                                        @RequestParam(value = "paymentMethod", required = false) String paymentMethod,
                                        @RequestParam(value = "paymentReference", required = false) String paymentReference,
                                        RedirectAttributes redirectAttributes) {
        try {
            deliveryDailyService.registerDeliveryOutcome(
                    id,
                    deliveryStatus,
                    observation,
                    incidentReason,
                    proofReference,
                    paymentAmount,
                    paymentMethod,
                    paymentReference
            );
            redirectAttributes.addFlashAttribute("successMessage", "Resultado de entrega y cobranza registrado correctamente.");
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
