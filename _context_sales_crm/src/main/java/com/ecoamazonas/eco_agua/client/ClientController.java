package com.ecoamazonas.eco_agua.client;

import com.ecoamazonas.eco_agua.promotion.PromotionService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Controller
@RequestMapping("/admin/clients")
public class ClientController {

    private final ClientService clientService;
    private final ClientProfileService clientProfileService;
    private final PromotionService promotionService;
    private final GeocodingService geocodingService;
    private final ClientAnalyticsService clientAnalyticsService;
    private final ClientPortfolioService clientPortfolioService;

    public ClientController(
            ClientService clientService,
            ClientProfileService clientProfileService,
            PromotionService promotionService,
            GeocodingService geocodingService,
            ClientAnalyticsService clientAnalyticsService,
            ClientPortfolioService clientPortfolioService
    ) {
        this.clientService = clientService;
        this.clientProfileService = clientProfileService;
        this.promotionService = promotionService;
        this.geocodingService = geocodingService;
        this.clientAnalyticsService = clientAnalyticsService;
        this.clientPortfolioService = clientPortfolioService;
    }

    @GetMapping
    public String index(Model model) {
        model.addAttribute("clients", clientService.findAll());
        model.addAttribute("profiles", clientProfileService.findAll());
        model.addAttribute("promotions", promotionService.findAllActive());

        return "admin/clients";
    }

    @GetMapping("/portfolio")
    public String portfolio(
            @RequestParam(value = "days", required = false) Integer days,
            @RequestParam(value = "from", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(value = "to", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            Model model
    ) {
        LocalDate today = LocalDate.now();
        int resolvedDays = (days != null && days > 0) ? days : 30;

        LocalDate fromDate;
        LocalDate toDate;

        if (from != null || to != null) {
            fromDate = (from != null ? from : to);
            toDate = (to != null ? to : from);
        } else {
            toDate = today;
            fromDate = today.minusDays(resolvedDays - 1L);
        }

        if (fromDate == null) {
            fromDate = today.minusDays(resolvedDays - 1L);
        }
        if (toDate == null) {
            toDate = today;
        }

        if (fromDate.isAfter(toDate)) {
            LocalDate tmp = fromDate;
            fromDate = toDate;
            toDate = tmp;
        }

        ClientPortfolioSnapshot snapshot = clientPortfolioService.buildSnapshot(fromDate, toDate);

        Integer selectedPresetDays = null;
        if (from == null && to == null && (resolvedDays == 30 || resolvedDays == 60 || resolvedDays == 90)) {
            selectedPresetDays = resolvedDays;
        }

        model.addAttribute("snapshot", snapshot);
        model.addAttribute("selectedPresetDays", selectedPresetDays);
        model.addAttribute("currentRangeDays", ChronoUnit.DAYS.between(snapshot.getFromDate(), snapshot.getToDate()) + 1);

        return "admin/client_portfolio";
    }

    @GetMapping("/{id}/stats")
    public String stats(
            @PathVariable Long id,
            @RequestParam(value = "days", required = false) Integer days,
            @RequestParam(value = "from", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(value = "to", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            Model model
    ) {
        LocalDate today = LocalDate.now();
        int resolvedDays = (days != null && days > 0) ? days : 30;

        LocalDate fromDate;
        LocalDate toDate;

        if (from != null || to != null) {
            fromDate = (from != null ? from : to);
            toDate = (to != null ? to : from);
        } else {
            toDate = today;
            fromDate = today.minusDays(resolvedDays - 1L);
        }

        if (fromDate == null) {
            fromDate = today.minusDays(resolvedDays - 1L);
        }
        if (toDate == null) {
            toDate = today;
        }

        if (fromDate.isAfter(toDate)) {
            LocalDate tmp = fromDate;
            fromDate = toDate;
            toDate = tmp;
        }

        ClientAnalyticsSnapshot snapshot;
        try {
            snapshot = clientAnalyticsService.buildSnapshot(id, fromDate, toDate);
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, ex.getMessage(), ex);
        }

        Integer selectedPresetDays = null;
        if (from == null && to == null && (resolvedDays == 30 || resolvedDays == 60 || resolvedDays == 90)) {
            selectedPresetDays = resolvedDays;
        }

        model.addAttribute("snapshot", snapshot);
        model.addAttribute("selectedPresetDays", selectedPresetDays);
        model.addAttribute("currentRangeDays", ChronoUnit.DAYS.between(snapshot.getFromDate(), snapshot.getToDate()) + 1);

        return "admin/client_stats";
    }

    @PostMapping("/save")
    public String save(
            @RequestParam(required = false) Long id,
            @RequestParam String name,
            @RequestParam(required = false) DocumentType docType,
            @RequestParam(required = false) String docNumber,
            @RequestParam(required = false) String phone,
            @RequestParam(required = false) String address,
            @RequestParam(required = false) String reference,
            @RequestParam("profileId") Long profileId,
            @RequestParam(value = "promotionIds", required = false) List<Long> promotionIds,
            @RequestParam(required = false) BigDecimal latitude,
            @RequestParam(required = false) BigDecimal longitude,
            RedirectAttributes redirectAttributes
    ) {
        try {
            clientService.saveFromForm(
                    id,
                    name,
                    docType,
                    docNumber,
                    phone,
                    address,
                    reference,
                    profileId,
                    promotionIds,
                    latitude,
                    longitude
            );

            redirectAttributes.addFlashAttribute("message", "Cliente guardado correctamente.");
            redirectAttributes.addFlashAttribute("messageType", "success");
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("message", ex.getMessage());
            redirectAttributes.addFlashAttribute("messageType", "error");
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("message", "Error al guardar el cliente.");
            redirectAttributes.addFlashAttribute("messageType", "error");
        }

        return "redirect:/admin/clients";
    }

    @PostMapping("/{id}/delete")
    public String deleteSingle(
            @PathVariable Long id,
            RedirectAttributes redirectAttributes
    ) {
        try {
            clientService.delete(id);
            redirectAttributes.addFlashAttribute("message", "Cliente eliminado correctamente.");
            redirectAttributes.addFlashAttribute("messageType", "success");
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("message", "Error al eliminar el cliente.");
            redirectAttributes.addFlashAttribute("messageType", "error");
        }

        return "redirect:/admin/clients";
    }

    @PostMapping("/bulk-delete")
    public String bulkDelete(
            @RequestParam("ids") List<Long> ids,
            RedirectAttributes redirectAttributes
    ) {
        try {
            clientService.deleteBulk(ids);
            redirectAttributes.addFlashAttribute("message", "Clientes eliminados correctamente.");
            redirectAttributes.addFlashAttribute("messageType", "success");
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("message", "Error al eliminar los clientes seleccionados.");
            redirectAttributes.addFlashAttribute("messageType", "error");
        }

        return "redirect:/admin/clients";
    }

    @PostMapping("/geocode")
    @ResponseBody
    public Map<String, Object> geocode(
            @RequestParam(required = false) String address,
            @RequestParam(required = false) String reference
    ) {
        Map<String, Object> response = new HashMap<>();

        try {
            Optional<GeocodingService.LatLng> result = geocodingService.geocode(address, reference);

            if (result.isEmpty()) {
                response.put("status", "NOT_FOUND");
            } else {
                GeocodingService.LatLng latLng = result.get();
                response.put("status", "OK");
                response.put("lat", latLng.getLat());
                response.put("lng", latLng.getLng());
            }
        } catch (Exception e) {
            response.put("status", "ERROR");
        }

        return response;
    }
}
