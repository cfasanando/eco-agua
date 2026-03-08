package com.ecoamazonas.eco_agua.order;

import com.ecoamazonas.eco_agua.category.CategoryRepository;
import com.ecoamazonas.eco_agua.category.CategoryType;
import com.ecoamazonas.eco_agua.client.ClientRepository;
import com.ecoamazonas.eco_agua.product.ProductService;
import com.ecoamazonas.eco_agua.promotion.PromotionService;
import com.ecoamazonas.eco_agua.promotion.PromotionService.ClientPromotionDTO;
import com.ecoamazonas.eco_agua.user.EmployeeRepository;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Controller
@RequestMapping("/orders")
public class OrderController {

    private final OrderService orderService;
    private final ProductService productService;
    private final ClientRepository clientRepository;
    private final CategoryRepository categoryRepository;
    private final PromotionService promotionService;
    private final EmployeeRepository employeeRepository;

    public OrderController(
            OrderService orderService,
            ProductService productService,
            ClientRepository clientRepository,
            CategoryRepository categoryRepository,
            PromotionService promotionService,
            EmployeeRepository employeeRepository
    ) {
        this.orderService = orderService;
        this.productService = productService;
        this.clientRepository = clientRepository;
        this.categoryRepository = categoryRepository;
        this.promotionService = promotionService;
        this.employeeRepository = employeeRepository;
    }

    @GetMapping("/new")
    public String newOrderForm(Model model) {
        LocalDate today = LocalDate.now();

        model.addAttribute("activePage", "home");
        model.addAttribute("today", today);
        model.addAttribute("clients", clientRepository.findByActiveTrueOrderByNameAsc());
        model.addAttribute("products", productService.findAllActiveForOrder());
        model.addAttribute(
                "productCategories",
                categoryRepository.findByTypeAndActiveTrueOrderByNameAsc(CategoryType.PRODUCT)
        );
        model.addAttribute(
                "deliveryEmployees",
                employeeRepository.findActiveByJobPositionName("Repartidor")
        );

        return "orders/order_form";
    }

    @PostMapping
    public String createOrder(
            @RequestParam("orderDate")
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate orderDate,
            @RequestParam("clientId") Long clientId,
            @RequestParam(value = "deliveryPerson", required = false) String deliveryPerson,
            @RequestParam(value = "borrowedBottles", required = false) Integer borrowedBottles,
            @RequestParam(value = "comment", required = false) String comment,
            @RequestParam(value = "productId", required = false) List<Long> productIds,
            @RequestParam(value = "quantity", required = false) List<BigDecimal> quantities,
            @RequestParam(value = "unitPrice", required = false) List<BigDecimal> unitPrices,
            @RequestParam(value = "lineTotal", required = false) List<BigDecimal> lineTotals,
            @RequestParam("action") String action,
            RedirectAttributes redirectAttributes
    ) {
        try {
            OrderStatus initialStatus;

            if ("PAID".equalsIgnoreCase(action)) {
                initialStatus = OrderStatus.PAID;
            } else if ("CREDIT".equalsIgnoreCase(action)) {
                initialStatus = OrderStatus.CREDIT;
            } else {
                initialStatus = OrderStatus.REQUESTED;
            }

            SaleOrder order = orderService.createOrderFromForm(
                    orderDate,
                    clientId,
                    deliveryPerson,
                    borrowedBottles,
                    comment,
                    productIds,
                    quantities,
                    unitPrices,
                    lineTotals,
                    initialStatus
            );

            redirectAttributes.addFlashAttribute(
                    "message",
                    "Pedido registrado correctamente. Nº " + order.getOrderNumber()
            );
            redirectAttributes.addFlashAttribute("messageType", "success");

            return "redirect:/home";

        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("message", ex.getMessage());
            redirectAttributes.addFlashAttribute("messageType", "error");
            return "redirect:/orders/new";
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("message", "Error al guardar el pedido: " + ex.getMessage());
            redirectAttributes.addFlashAttribute("messageType", "error");
            return "redirect:/orders/new";
        }
    }

    @PostMapping("/{id}/pay")
    public String markAsPaid(
            @PathVariable Long id,
            @RequestParam(value = "redirect", required = false) String redirect,
            RedirectAttributes redirectAttributes
    ) {
        return changeStatusAndRedirect(id, OrderStatus.PAID, redirect, redirectAttributes);
    }

    @PostMapping("/{id}/credit")
    public String markAsCredit(
            @PathVariable Long id,
            @RequestParam(value = "redirect", required = false) String redirect,
            RedirectAttributes redirectAttributes
    ) {
        return changeStatusAndRedirect(id, OrderStatus.CREDIT, redirect, redirectAttributes);
    }

    @PostMapping("/{id}/cancel")
    public String cancelOrder(
            @PathVariable Long id,
            @RequestParam(value = "reason", required = false) String reason,
            @RequestParam(value = "returnToStock", defaultValue = "false") boolean returnToStock,
            @RequestParam(value = "redirect", required = false) String redirect,
            RedirectAttributes redirectAttributes
    ) {
        try {
            orderService.changeStatus(id, OrderStatus.CANCELED, returnToStock, reason, LocalDate.now());
            redirectAttributes.addFlashAttribute("message", "Pedido anulado correctamente.");
            redirectAttributes.addFlashAttribute("messageType", "success");
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("message", "Error al anular el pedido: " + ex.getMessage());
            redirectAttributes.addFlashAttribute("messageType", "error");
        }

        return "redirect:" + resolveRedirectTarget(redirect, "/home");
    }

    private String changeStatusAndRedirect(
            Long id,
            OrderStatus newStatus,
            String redirect,
            RedirectAttributes redirectAttributes
    ) {
        try {
            orderService.changeStatus(id, newStatus);

            String message = switch (newStatus) {
                case PAID -> "Pedido marcado como pagado.";
                case CREDIT -> "Pedido marcado como fiado.";
                default -> "Estado del pedido actualizado.";
            };

            redirectAttributes.addFlashAttribute("message", message);
            redirectAttributes.addFlashAttribute("messageType", "success");
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("message", "Error al actualizar el estado del pedido: " + ex.getMessage());
            redirectAttributes.addFlashAttribute("messageType", "error");
        }

        return "redirect:" + resolveRedirectTarget(redirect, "/home");
    }

    @GetMapping("/client/{clientId}/promotions")
    @ResponseBody
    public List<ClientPromotionDTO> getClientPromotions(
            @PathVariable Long clientId,
            @RequestParam(value = "date", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    ) {
        return promotionService.findApplicablePromotionDtos(clientId, date);
    }

    @GetMapping("/{id}")
    public String viewOrder(
            @PathVariable Long id,
            @RequestParam(value = "from", required = false) String from,
            Model model
    ) {
        SaleOrder order = orderService.findById(id);
        String backUrl = resolveRedirectTarget(from, "/home");

        Long daysSinceOrder = null;
        if (order.getOrderDate() != null) {
            daysSinceOrder = Math.max(ChronoUnit.DAYS.between(order.getOrderDate(), LocalDate.now()), 0);
        }

        model.addAttribute("activePage", "home");
        model.addAttribute("order", order);
        model.addAttribute("backUrl", backUrl);
        model.addAttribute("daysSinceOrder", daysSinceOrder);

        return "orders/order_detail";
    }

    private String resolveRedirectTarget(String redirect, String defaultPath) {
        if (redirect == null || redirect.isBlank()) {
            return defaultPath;
        }

        String trimmed = redirect.trim();
        if (!trimmed.startsWith("/") || trimmed.startsWith("//")) {
            return defaultPath;
        }

        return trimmed;
    }
}