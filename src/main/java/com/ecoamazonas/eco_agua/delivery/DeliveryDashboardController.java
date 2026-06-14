package com.ecoamazonas.eco_agua.delivery;

import com.ecoamazonas.eco_agua.user.EmployeeRepository;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDate;

@Controller
@RequestMapping("/delivery/dashboard")
public class DeliveryDashboardController {

    private final DeliveryDashboardService deliveryDashboardService;
    private final DeliveryDailyService deliveryDailyService;
    private final EmployeeRepository employeeRepository;

    public DeliveryDashboardController(
            DeliveryDashboardService deliveryDashboardService,
            DeliveryDailyService deliveryDailyService,
            EmployeeRepository employeeRepository
    ) {
        this.deliveryDashboardService = deliveryDashboardService;
        this.deliveryDailyService = deliveryDailyService;
        this.employeeRepository = employeeRepository;
    }

    @GetMapping
    public String index(
            @RequestParam(value = "date", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(value = "deliveryPerson", required = false) String deliveryPerson,
            Model model
    ) {
        LocalDate effectiveDate = date != null ? date : LocalDate.now();
        DeliveryDashboardSnapshot snapshot = deliveryDashboardService.buildSnapshot(effectiveDate, deliveryPerson);

        model.addAttribute("activePage", "delivery_dashboard");
        model.addAttribute("today", effectiveDate);
        model.addAttribute("snapshot", snapshot);
        model.addAttribute("deliveryEmployees", employeeRepository.findByActiveTrueOrderByFirstNameAscLastNameAsc());
        model.addAttribute("selectedDeliveryPerson", deliveryPerson);
        model.addAttribute("openStreetMapRouteUrl", deliveryDailyService.buildOpenStreetMapRouteUrl(snapshot.getRouteRows()));

        return "delivery/dashboard";
    }
}
