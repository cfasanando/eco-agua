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
            redirectAttributes.addFlashAttribute("successMessage", "Comanda registrada correctamente. ID interno: " + orderId);
            return "redirect:/admin/restaurant/kitchen";
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
            return "redirect:/admin/restaurant/orders/new";
        }
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
        ensureRestaurantRuntimeReady();
        restaurantService.updateOrderStatus(id, status);
        redirectAttributes.addFlashAttribute("successMessage", "Estado de comanda actualizado.");
        if (returnTo.startsWith("/admin/restaurant")) {
            return "redirect:" + returnTo;
        }
        return "redirect:/admin/restaurant/kitchen";
    }

    private void ensureRestaurantRuntimeReady() {
        restaurantModuleInstaller.installAndActivate(true);
    }
}
