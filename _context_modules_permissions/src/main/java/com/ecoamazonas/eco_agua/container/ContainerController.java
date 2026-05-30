package com.ecoamazonas.eco_agua.container;

import com.ecoamazonas.eco_agua.client.ClientRepository;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.util.List;

@Controller
@RequestMapping("/containers")
public class ContainerController {

    private final ClientContainerService clientContainerService;
    private final ClientRepository clientRepository;

    public ContainerController(
            ClientContainerService clientContainerService,
            ClientRepository clientRepository
    ) {
        this.clientContainerService = clientContainerService;
        this.clientRepository = clientRepository;
    }

    @GetMapping
    public String index(
            @RequestParam(value = "clientId", required = false) Long clientId,
            @RequestParam(value = "includeZero", required = false, defaultValue = "false") boolean includeZero,
            Model model
    ) {
        List<ClientContainerBalanceRow> rows = clientContainerService.buildBalanceRows(clientId, includeZero);

        model.addAttribute("rows", rows);
        model.addAttribute("clients", clientRepository.findByActiveTrueOrderByNameAsc());
        model.addAttribute("selectedClientId", clientId);
        model.addAttribute("includeZero", includeZero);
        model.addAttribute("totalOutstanding", rows.stream().mapToInt(ClientContainerBalanceRow::getBalance).sum());
        model.addAttribute("clientsWithBalanceCount", rows.stream().filter(row -> row.getBalance() > 0).count());

        return "warehouse/container_balances";
    }

    @PostMapping("/sync-orders")
    public String syncOrders(
            @RequestParam(value = "startDate", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(value = "endDate", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            RedirectAttributes redirectAttributes
    ) {
        int synced = clientContainerService.syncLoansFromOrders(startDate, endDate);
        redirectAttributes.addFlashAttribute("successMessage", "Order loan synchronization completed. Changes: " + synced);

        return "redirect:/containers";
    }

    @GetMapping("/client/{clientId}")
    public String detail(@PathVariable Long clientId, Model model) {
        ClientContainerClientSnapshot snapshot = clientContainerService.getClientSnapshot(clientId);

        model.addAttribute("snapshot", snapshot);

        return "warehouse/container_detail";
    }

    @PostMapping("/client/{clientId}/return")
    public String registerReturn(
            @PathVariable Long clientId,
            @RequestParam(value = "movementDate", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate movementDate,
            @RequestParam("quantity") Integer quantity,
            @RequestParam(value = "observation", required = false) String observation,
            @RequestParam(value = "saleOrderId", required = false) Long saleOrderId,
            @RequestParam(value = "allowExcessReturn", required = false, defaultValue = "false") boolean allowExcessReturn,
            RedirectAttributes redirectAttributes
    ) {
        try {
            clientContainerService.registerReturn(
                    clientId,
                    movementDate,
                    quantity,
                    observation,
                    saleOrderId,
                    allowExcessReturn
            );
            redirectAttributes.addFlashAttribute("successMessage", "Return registered successfully.");
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }

        return "redirect:/containers/client/" + clientId;
    }

    @PostMapping("/client/{clientId}/adjustment")
    public String registerAdjustment(
            @PathVariable Long clientId,
            @RequestParam(value = "movementDate", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate movementDate,
            @RequestParam("movementType") ContainerMovementType movementType,
            @RequestParam("quantity") Integer quantity,
            @RequestParam(value = "observation", required = false) String observation,
            @RequestParam(value = "saleOrderId", required = false) Long saleOrderId,
            RedirectAttributes redirectAttributes
    ) {
        try {
            clientContainerService.registerAdjustment(
                    clientId,
                    movementDate,
                    movementType,
                    quantity,
                    observation,
                    saleOrderId
            );
            redirectAttributes.addFlashAttribute("successMessage", "Adjustment registered successfully.");
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }

        return "redirect:/containers/client/" + clientId;
    }
}
