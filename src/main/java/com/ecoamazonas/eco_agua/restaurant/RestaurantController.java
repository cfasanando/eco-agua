package com.ecoamazonas.eco_agua.restaurant;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.Map;

@Controller
public class RestaurantController {

    private final RestaurantService restaurantService;
    private final RestaurantModuleInstaller restaurantModuleInstaller;

    public RestaurantController(RestaurantService restaurantService,
                                RestaurantModuleInstaller restaurantModuleInstaller) {
        this.restaurantService = restaurantService;
        this.restaurantModuleInstaller = restaurantModuleInstaller;
    }

    @GetMapping({"/restaurant", "/restaurant/menu"})
    public String publicMenu(Model model) {
        ensureRestaurantRuntimeReady();
        List<RestaurantMenuItemRow> items = restaurantService.menuItems();
        model.addAttribute("menuItems", items);
        model.addAttribute("featuredMenuItems", restaurantService.featuredMenuItems());
        model.addAttribute("menuItemCount", items.size());
        return "public/restaurant_menu";
    }

    @GetMapping("/admin/restaurant")
    public String restaurantHome() {
        return "redirect:/admin/restaurant/dashboard";
    }

    @GetMapping("/admin/restaurant/dashboard")
    public String dashboard(Model model) {
        ensureRestaurantRuntimeReady();
        model.addAttribute("activePage", "restaurant_dashboard");
        model.addAttribute("summary", restaurantService.dashboardSummary());
        List<RestaurantOrderRow> activeOrders = restaurantService.activeOrders();
        List<RestaurantOrderRow> kitchenOrders = restaurantService.kitchenOrders();
        model.addAttribute("tableBoard", restaurantService.tableBoard());
        model.addAttribute("activeOrders", activeOrders);
        model.addAttribute("kitchenOrders", kitchenOrders);
        model.addAttribute("itemsByOrder", restaurantService.itemsByOrder(activeOrders));
        model.addAttribute("kitchenItemsByOrder", restaurantService.itemsByOrder(kitchenOrders));
        return "admin/restaurant/dashboard";
    }

    @GetMapping("/admin/restaurant/tables")
    public String tables(Model model) {
        ensureRestaurantRuntimeReady();
        model.addAttribute("activePage", "restaurant_tables");
        model.addAttribute("tables", restaurantService.tables());
        return "admin/restaurant/tables";
    }

    @PostMapping("/admin/restaurant/tables/{id}/status")
    public String updateTableStatus(@PathVariable Long id,
                                    @RequestParam String status,
                                    RedirectAttributes redirectAttributes) {
        ensureRestaurantRuntimeReady();
        restaurantService.updateTableStatus(id, status);
        redirectAttributes.addFlashAttribute("successMessage", "Estado de mesa actualizado.");
        return "redirect:/admin/restaurant/tables";
    }

    @GetMapping("/admin/restaurant/orders/new")
    public String newOrder(@RequestParam(required = false) Long tableId, Model model) {
        ensureRestaurantRuntimeReady();
        model.addAttribute("activePage", "restaurant_orders_new");
        model.addAttribute("tables", restaurantService.availableTables());
        model.addAttribute("menuItems", restaurantService.menuItems());
        model.addAttribute("selectedTableId", tableId);
        return "admin/restaurant/order_form";
    }

    @PostMapping("/admin/restaurant/orders")
    public String createOrder(@RequestParam(defaultValue = "DINE_IN") String serviceType,
                              @RequestParam(required = false) Long tableId,
                              @RequestParam(required = false) String customerName,
                              @RequestParam(required = false) String customerPhone,
                              @RequestParam(required = false) String notes,
                              @RequestParam Map<String, String> params,
                              RedirectAttributes redirectAttributes) {
        try {
            ensureRestaurantRuntimeReady();
            Long orderId = restaurantService.createOrder(serviceType, tableId, customerName, customerPhone, notes, params);
            redirectAttributes.addFlashAttribute("successMessage", "Comanda registrada y enviada a cocina.");
            return "redirect:/admin/restaurant/orders/" + orderId;
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
            return "redirect:/admin/restaurant/orders/new";
        }
    }

    @GetMapping("/admin/restaurant/orders/{id}")
    public String orderDetail(@PathVariable Long id, Model model, RedirectAttributes redirectAttributes) {
        ensureRestaurantRuntimeReady();
        RestaurantOrderRow order = restaurantService.order(id);
        if (order == null) {
            redirectAttributes.addFlashAttribute("errorMessage", "La comanda solicitada no existe.");
            return "redirect:/admin/restaurant/dashboard";
        }

        model.addAttribute("activePage", "restaurant_kitchen");
        model.addAttribute("order", order);
        model.addAttribute("items", restaurantService.orderItems(id));
        model.addAttribute("menuItems", restaurantService.menuItems());
        model.addAttribute("canEdit", order.canEdit());
        model.addAttribute("canSendToKitchen", order.canSendToKitchen());
        model.addAttribute("canMarkReady", order.canMarkReady());
        model.addAttribute("canMarkServed", order.canMarkServed());
        model.addAttribute("canPay", order.canPay());
        model.addAttribute("canCancel", order.canCancel());
        return "admin/restaurant/order_detail";
    }

    @PostMapping("/admin/restaurant/orders/{id}/items")
    public String addItemsToOrder(@PathVariable Long id,
                                  @RequestParam Map<String, String> params,
                                  RedirectAttributes redirectAttributes) {
        try {
            ensureRestaurantRuntimeReady();
            restaurantService.addItemsToOrder(id, params);
            redirectAttributes.addFlashAttribute("successMessage", "Productos agregados a la comanda.");
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/admin/restaurant/orders/" + id;
    }

    @PostMapping("/admin/restaurant/orders/{orderId}/items/{itemId}/quantity")
    public String updateItemQuantity(@PathVariable Long orderId,
                                     @PathVariable Long itemId,
                                     @RequestParam(defaultValue = "1") int quantity,
                                     RedirectAttributes redirectAttributes) {
        try {
            ensureRestaurantRuntimeReady();
            restaurantService.updateItemQuantity(orderId, itemId, quantity);
            redirectAttributes.addFlashAttribute("successMessage", "Cantidad actualizada.");
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/admin/restaurant/orders/" + orderId;
    }

    @PostMapping("/admin/restaurant/orders/{orderId}/items/{itemId}/remove")
    public String removeItem(@PathVariable Long orderId,
                             @PathVariable Long itemId,
                             RedirectAttributes redirectAttributes) {
        try {
            ensureRestaurantRuntimeReady();
            restaurantService.removeItem(orderId, itemId);
            redirectAttributes.addFlashAttribute("successMessage", "Producto retirado de la comanda.");
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/admin/restaurant/orders/" + orderId;
    }

    @GetMapping("/admin/restaurant/kitchen")
    public String kitchen(Model model) {
        ensureRestaurantRuntimeReady();
        model.addAttribute("activePage", "restaurant_kitchen");
        List<RestaurantOrderRow> orders = restaurantService.kitchenOrders();
        model.addAttribute("orders", orders);
        model.addAttribute("itemsByOrder", restaurantService.itemsByOrder(orders));
        return "admin/restaurant/kitchen";
    }

    @PostMapping("/admin/restaurant/orders/{id}/status")
    public String updateOrderStatus(@PathVariable Long id,
                                    @RequestParam String status,
                                    @RequestParam(defaultValue = "/admin/restaurant/kitchen") String returnTo,
                                    RedirectAttributes redirectAttributes) {
        try {
            ensureRestaurantRuntimeReady();
            restaurantService.updateOrderStatus(id, status);
            redirectAttributes.addFlashAttribute("successMessage", "Estado de comanda actualizado.");
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        if (returnTo.startsWith("/admin/restaurant")) {
            return "redirect:" + returnTo;
        }
        return "redirect:/admin/restaurant/kitchen";
    }

    @PostMapping("/admin/restaurant/orders/{id}/pay")
    public String payOrder(@PathVariable Long id,
                           @RequestParam(defaultValue = "CASH") String paymentMethod,
                           RedirectAttributes redirectAttributes) {
        try {
            ensureRestaurantRuntimeReady();
            restaurantService.payOrder(id, paymentMethod);
            redirectAttributes.addFlashAttribute("successMessage", "Comanda cobrada y mesa liberada.");
            return "redirect:/admin/restaurant/dashboard";
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
            return "redirect:/admin/restaurant/orders/" + id;
        }
    }

    @PostMapping("/admin/restaurant/orders/{id}/cancel")
    public String cancelOrder(@PathVariable Long id,
                              RedirectAttributes redirectAttributes) {
        try {
            ensureRestaurantRuntimeReady();
            restaurantService.cancelOrder(id);
            redirectAttributes.addFlashAttribute("successMessage", "Comanda anulada y mesa liberada.");
            return "redirect:/admin/restaurant/dashboard";
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
            return "redirect:/admin/restaurant/orders/" + id;
        }
    }

    private void ensureRestaurantRuntimeReady() {
        restaurantModuleInstaller.installAndActivate(true);
    }
}
