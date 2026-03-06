package com.ecoamazonas.eco_agua.client;

import com.ecoamazonas.eco_agua.promotion.PromotionService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

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

    public ClientController(
            ClientService clientService,
            ClientProfileService clientProfileService,
            PromotionService promotionService,
            GeocodingService geocodingService
    ) {
        this.clientService = clientService;
        this.clientProfileService = clientProfileService;
        this.promotionService = promotionService;
        this.geocodingService = geocodingService;
    }

    @GetMapping
    public String index(Model model) {
        model.addAttribute("clients", clientService.findAll());
        model.addAttribute("profiles", clientProfileService.findAll());
        model.addAttribute("promotions", promotionService.findAllActive());

        return "admin/clients";
    }

    @PostMapping("/save")
    public String save(
            @RequestParam(required = false) Long id,
            @RequestParam String name,
            @RequestParam("docType") DocumentType docType,
            @RequestParam("docNumber") String docNumber,
            @RequestParam(required = false) String phone,
            @RequestParam(required = false) String address,
            @RequestParam(required = false) String reference,
            @RequestParam("profileId") Long profileId,
            @RequestParam(value = "promotionIds", required = false) List<Long> promotionIds,
            RedirectAttributes redirectAttributes
    ) {
        try {
            clientService.saveFromForm(
                    id, name, docType, docNumber, phone, address, reference, profileId, promotionIds
            );
            redirectAttributes.addFlashAttribute("message", "Cliente guardado correctamente.");
            redirectAttributes.addFlashAttribute("messageType", "success");
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
    
    /**
     * Geocode address + reference and return JSON with lat/lng.
     */
    @PostMapping("/geocode")
    @ResponseBody
    public Map<String, Object> geocode(
            @RequestParam(required = false) String address,
            @RequestParam(required = false) String reference
    ) {
        Map<String, Object> response = new HashMap<>();

        try {
            Optional<GeocodingService.LatLng> result =
                    geocodingService.geocode(address, reference);

            if (result.isEmpty()) {
                response.put("status", "NOT_FOUND");
            } else {
                GeocodingService.LatLng latLng = result.get();
                response.put("status", "OK");
                response.put("lat", latLng.getLat());
                response.put("lng", latLng.getLng());
            }
        } catch (Exception e) {
            // You could log the error here
            response.put("status", "ERROR");
        }

        return response;
    }

}
