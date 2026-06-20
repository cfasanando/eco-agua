package com.ecoamazonas.eco_agua.restaurant;

import com.ecoamazonas.eco_agua.config.BusinessProperties;
import com.ecoamazonas.eco_agua.config.PlatformSettingService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.net.URLEncoder;
import java.time.LocalDate;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

@Controller
public class RestaurantController {

    private final RestaurantService restaurantService;
    private final RestaurantModuleInstaller restaurantModuleInstaller;
    private final PlatformSettingService platformSettingService;
    private final BusinessProperties businessProperties;

    public RestaurantController(RestaurantService restaurantService,
                                RestaurantModuleInstaller restaurantModuleInstaller,
                                PlatformSettingService platformSettingService,
                                BusinessProperties businessProperties) {
        this.restaurantService = restaurantService;
        this.restaurantModuleInstaller = restaurantModuleInstaller;
        this.platformSettingService = platformSettingService;
        this.businessProperties = businessProperties;
    }

    @GetMapping({"/restaurant", "/restaurant/menu"})
    public String publicMenu(@RequestParam(required = false) Long tableId,
                             HttpServletRequest request,
                             Model model) {
        ensureRestaurantRuntimeReady();
        List<RestaurantMenuItemRow> items = restaurantService.menuItems();
        RestaurantPublicTableContext tableContext = restaurantService.publicTableContext(tableId);
        String publicMenuUrl = absoluteUrl(request, tableContext == null ? "/restaurant/menu" : "/restaurant/menu?tableId=" + tableContext.id());

        model.addAttribute("menuItems", items);
        model.addAttribute("menuGroups", restaurantService.menuGroups());
        model.addAttribute("featuredMenuItems", restaurantService.featuredMenuItems());
        model.addAttribute("menuItemCount", items.size());
        model.addAttribute("tableContext", tableContext);
        model.addAttribute("publicMenuUrl", publicMenuUrl);
        model.addAttribute("attentionWhatsappLink", attentionWhatsappLink(tableContext, publicMenuUrl));
        addPublicRestaurantAttributes(model, tableContext, publicMenuUrl);
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
    public String tables(HttpServletRequest request, Model model) {
        ensureRestaurantRuntimeReady();
        model.addAttribute("activePage", "restaurant_tables");
        model.addAttribute("tables", restaurantService.tables());
        model.addAttribute("publicMenuBaseUrl", absoluteUrl(request, "/restaurant/menu"));
        return "admin/restaurant/tables";
    }

    @GetMapping("/admin/restaurant/tables/qr-cards")
    public String tableQrCards(HttpServletRequest request, Model model) {
        ensureRestaurantRuntimeReady();
        model.addAttribute("activePage", "restaurant_tables");
        model.addAttribute("tables", restaurantService.tables());
        model.addAttribute("publicMenuBaseUrl", absoluteUrl(request, "/restaurant/menu"));
        return "admin/restaurant/table_qr_cards";
    }


    @GetMapping("/admin/restaurant/menu-items")
    public String menuItems(@RequestParam(defaultValue = "ALL") String stockFilter, Model model) {
        ensureRestaurantRuntimeReady();
        model.addAttribute("activePage", "restaurant_menu_items");
        model.addAttribute("currentStockFilter", normalizeStockFilter(stockFilter));
        model.addAttribute("stockSummary", restaurantService.stockSummary());
        model.addAttribute("items", restaurantService.menuItemsAdmin(stockFilter));
        return "admin/restaurant/menu_items";
    }

    @GetMapping("/admin/restaurant/menu-items/new")
    public String newMenuItem(Model model) {
        ensureRestaurantRuntimeReady();
        model.addAttribute("activePage", "restaurant_menu_items");
        model.addAttribute("item", null);
        model.addAttribute("categories", restaurantService.productCategories());
        model.addAttribute("formAction", "/admin/restaurant/menu-items");
        model.addAttribute("formTitle", "Nuevo plato");
        return "admin/restaurant/menu_item_form";
    }

    @GetMapping("/admin/restaurant/menu-items/{id}/edit")
    public String editMenuItem(@PathVariable Long id, Model model, RedirectAttributes redirectAttributes) {
        ensureRestaurantRuntimeReady();
        RestaurantMenuAdminRow item = restaurantService.menuItemAdmin(id);
        if (item == null) {
            redirectAttributes.addFlashAttribute("errorMessage", "El plato seleccionado no existe.");
            return "redirect:/admin/restaurant/menu-items";
        }
        model.addAttribute("activePage", "restaurant_menu_items");
        model.addAttribute("item", item);
        model.addAttribute("categories", restaurantService.productCategories());
        model.addAttribute("formAction", "/admin/restaurant/menu-items/" + id);
        model.addAttribute("formTitle", "Editar plato");
        return "admin/restaurant/menu_item_form";
    }

    @PostMapping("/admin/restaurant/menu-items")
    public String createMenuItem(@RequestParam String name,
                                 @RequestParam(required = false) String description,
                                 @RequestParam(required = false) String imagePath,
                                 @RequestParam(required = false) BigDecimal price,
                                 @RequestParam(required = false) BigDecimal stock,
                                 @RequestParam(required = false) BigDecimal minimumStock,
                                 @RequestParam(required = false) Long categoryId,
                                 @RequestParam(required = false) String newCategoryName,
                                 @RequestParam(required = false) String active,
                                 @RequestParam(required = false) String featured,
                                 @RequestParam(required = false) String restaurantVisible,
                                 @RequestParam(required = false) String restaurantAvailable,
                                 @RequestParam(defaultValue = "0") int sortOrder,
                                 RedirectAttributes redirectAttributes) {
        try {
            ensureRestaurantRuntimeReady();
            restaurantService.createMenuItem(
                    name, description, imagePath, price, stock, minimumStock, categoryId, newCategoryName,
                    isChecked(active), isChecked(featured), isChecked(restaurantVisible), isChecked(restaurantAvailable), sortOrder
            );
            redirectAttributes.addFlashAttribute("successMessage", "Plato creado correctamente.");
            return "redirect:/admin/restaurant/menu-items";
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
            return "redirect:/admin/restaurant/menu-items/new";
        }
    }

    @PostMapping("/admin/restaurant/menu-items/{id}")
    public String updateMenuItem(@PathVariable Long id,
                                 @RequestParam String name,
                                 @RequestParam(required = false) String description,
                                 @RequestParam(required = false) String imagePath,
                                 @RequestParam(required = false) BigDecimal price,
                                 @RequestParam(required = false) BigDecimal stock,
                                 @RequestParam(required = false) BigDecimal minimumStock,
                                 @RequestParam(required = false) Long categoryId,
                                 @RequestParam(required = false) String newCategoryName,
                                 @RequestParam(required = false) String active,
                                 @RequestParam(required = false) String featured,
                                 @RequestParam(required = false) String restaurantVisible,
                                 @RequestParam(required = false) String restaurantAvailable,
                                 @RequestParam(defaultValue = "0") int sortOrder,
                                 RedirectAttributes redirectAttributes) {
        try {
            ensureRestaurantRuntimeReady();
            restaurantService.updateMenuItem(
                    id, name, description, imagePath, price, stock, minimumStock, categoryId, newCategoryName,
                    isChecked(active), isChecked(featured), isChecked(restaurantVisible), isChecked(restaurantAvailable), sortOrder
            );
            redirectAttributes.addFlashAttribute("successMessage", "Plato actualizado correctamente.");
            return "redirect:/admin/restaurant/menu-items";
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
            return "redirect:/admin/restaurant/menu-items/" + id + "/edit";
        }
    }

    @PostMapping("/admin/restaurant/menu-items/{id}/availability")
    public String toggleMenuItemAvailability(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            ensureRestaurantRuntimeReady();
            restaurantService.toggleMenuItemAvailability(id);
            redirectAttributes.addFlashAttribute("successMessage", "Disponibilidad actualizada.");
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/admin/restaurant/menu-items";
    }

    @PostMapping("/admin/restaurant/menu-items/{id}/stock")
    public String replenishMenuItemStock(@PathVariable Long id,
                                         @RequestParam(required = false) BigDecimal quantity,
                                         RedirectAttributes redirectAttributes) {
        try {
            ensureRestaurantRuntimeReady();
            restaurantService.replenishMenuItemStock(id, quantity);
            redirectAttributes.addFlashAttribute("successMessage", "Stock repuesto y plato marcado como disponible.");
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/admin/restaurant/menu-items";
    }

    @PostMapping("/admin/restaurant/menu-items/{id}/visibility")
    public String toggleMenuItemVisibility(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            ensureRestaurantRuntimeReady();
            restaurantService.toggleMenuItemVisibility(id);
            redirectAttributes.addFlashAttribute("successMessage", "Visibilidad actualizada.");
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/admin/restaurant/menu-items";
    }

    @PostMapping("/admin/restaurant/menu-items/{id}/featured")
    public String toggleMenuItemFeatured(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            ensureRestaurantRuntimeReady();
            restaurantService.toggleMenuItemFeatured(id);
            redirectAttributes.addFlashAttribute("successMessage", "Destacado actualizado.");
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/admin/restaurant/menu-items";
    }


    @GetMapping("/admin/restaurant/cash")
    public String cash(@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
                       Model model) {
        ensureRestaurantRuntimeReady();
        LocalDate businessDate = date == null ? LocalDate.now() : date;
        model.addAttribute("activePage", "restaurant_cash");
        model.addAttribute("businessDate", businessDate);
        model.addAttribute("summary", restaurantService.cashSummary(businessDate));
        model.addAttribute("paymentBreakdown", restaurantService.paymentBreakdown(businessDate));
        model.addAttribute("paidOrders", restaurantService.paidOrdersForDate(businessDate));
        model.addAttribute("openOrders", restaurantService.openOrdersForCash());
        return "admin/restaurant/cash";
    }

    @GetMapping("/admin/restaurant/reports/daily")
    public String dailyReport(@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
                              Model model) {
        ensureRestaurantRuntimeReady();
        LocalDate businessDate = date == null ? LocalDate.now() : date;
        model.addAttribute("activePage", "restaurant_cash");
        model.addAttribute("businessDate", businessDate);
        model.addAttribute("summary", restaurantService.cashSummary(businessDate));
        model.addAttribute("paymentBreakdown", restaurantService.paymentBreakdown(businessDate));
        model.addAttribute("paidOrders", restaurantService.paidOrdersForDate(businessDate));
        model.addAttribute("openOrders", restaurantService.openOrdersForCash());
        model.addAttribute("generatedAt", java.time.LocalDateTime.now());
        return "admin/restaurant/daily_report";
    }

    @PostMapping("/admin/restaurant/tables/{id}/status")
    public String updateTableStatus(@PathVariable Long id,
                                    @RequestParam String status,
                                    RedirectAttributes redirectAttributes) {
        ensureRestaurantRuntimeReady();
        restaurantService.updateTableStatus(id, status);
        redirectAttributes.addFlashAttribute("successMessage", "Table status updated.");
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
            redirectAttributes.addFlashAttribute("successMessage", "Comanda registered and sent to kitchen.");
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
            redirectAttributes.addFlashAttribute("errorMessage", "The requested comanda does not exist.");
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
            redirectAttributes.addFlashAttribute("successMessage", "Products added to the comanda.");
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
            redirectAttributes.addFlashAttribute("successMessage", "Quantity updated.");
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
            redirectAttributes.addFlashAttribute("successMessage", "Product removed from the comanda.");
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/admin/restaurant/orders/" + orderId;
    }


    @GetMapping("/admin/restaurant/orders/{id}/kitchen-ticket")
    public String kitchenTicket(@PathVariable Long id, Model model, RedirectAttributes redirectAttributes) {
        ensureRestaurantRuntimeReady();
        RestaurantOrderRow order = restaurantService.order(id);
        if (order == null) {
            redirectAttributes.addFlashAttribute("errorMessage", "La comanda seleccionada no existe.");
            return "redirect:/admin/restaurant/kitchen";
        }
        model.addAttribute("activePage", "restaurant_kitchen");
        model.addAttribute("order", order);
        model.addAttribute("items", restaurantService.orderItems(id));
        model.addAttribute("generatedAt", java.time.LocalDateTime.now());
        addPrintableRestaurantAttributes(model);
        return "admin/restaurant/kitchen_ticket";
    }

    @GetMapping("/admin/restaurant/orders/{id}/bill")
    public String bill(@PathVariable Long id, Model model, RedirectAttributes redirectAttributes) {
        ensureRestaurantRuntimeReady();
        RestaurantOrderRow order = restaurantService.order(id);
        if (order == null) {
            redirectAttributes.addFlashAttribute("errorMessage", "La comanda seleccionada no existe.");
            return "redirect:/admin/restaurant/dashboard";
        }
        model.addAttribute("activePage", "restaurant_cash");
        model.addAttribute("order", order);
        model.addAttribute("items", restaurantService.orderItems(id));
        model.addAttribute("generatedAt", java.time.LocalDateTime.now());
        addPrintableRestaurantAttributes(model);
        return "admin/restaurant/bill";
    }

    @GetMapping("/admin/restaurant/orders/{id}/receipt")
    public String receipt(@PathVariable Long id, Model model, RedirectAttributes redirectAttributes) {
        ensureRestaurantRuntimeReady();
        RestaurantOrderRow order = restaurantService.order(id);
        if (order == null) {
            redirectAttributes.addFlashAttribute("errorMessage", "La comanda seleccionada no existe.");
            return "redirect:/admin/restaurant/cash";
        }
        RestaurantCashOrderRow paidOrder = restaurantService.cashOrder(id);
        model.addAttribute("activePage", "restaurant_cash");
        model.addAttribute("order", order);
        model.addAttribute("paidOrder", paidOrder);
        model.addAttribute("items", restaurantService.orderItems(id));
        model.addAttribute("generatedAt", java.time.LocalDateTime.now());
        addPrintableRestaurantAttributes(model);
        return "admin/restaurant/receipt";
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
            redirectAttributes.addFlashAttribute("successMessage", "Comanda status updated.");
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
            redirectAttributes.addFlashAttribute("successMessage", "Comanda paid and table released.");
            return "redirect:/admin/restaurant/orders/" + id + "/receipt";
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
            redirectAttributes.addFlashAttribute("successMessage", "Comanda cancelled and table released.");
            return "redirect:/admin/restaurant/dashboard";
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
            return "redirect:/admin/restaurant/orders/" + id;
        }
    }

    private void ensureRestaurantRuntimeReady() {
        restaurantModuleInstaller.installAndActivate(true);
    }


    private void addPrintableRestaurantAttributes(Model model) {
        model.addAttribute("businessName", setting("platform.name", businessProperties.getName()));
        model.addAttribute("businessTagline", setting("platform.tagline", businessProperties.getTagline()));
        model.addAttribute("platformLogo", setting("platform.logo", businessProperties.getLogo()));
        model.addAttribute("whatsappNumber", setting("public.whatsapp.number", businessProperties.getWhatsappNumber()));
    }

    private void addPublicRestaurantAttributes(Model model, RestaurantPublicTableContext tableContext, String publicMenuUrl) {
        String platformName = setting("platform.name", businessProperties.getName());
        String platformTagline = setting("platform.tagline", businessProperties.getTagline());
        String platformLogo = setting("platform.logo", businessProperties.getLogo());
        String whatsappNumber = setting("public.whatsapp.number", businessProperties.getWhatsappNumber());

        model.addAttribute("businessName", platformName);
        model.addAttribute("businessTagline", platformTagline);
        model.addAttribute("platformLogo", platformLogo);
        model.addAttribute("whatsappNumber", whatsappNumber);
        model.addAttribute("topbarLocation", setting("public.topbar.location", "Iquitos"));
        model.addAttribute("publicPrimaryColor", setting("public.theme.primary_color", "#dc3545"));
        model.addAttribute("publicSecondaryColor", setting("public.theme.secondary_color", "#f97316"));
        model.addAttribute("seoTitle", tableContext == null ? "Carta digital - " + platformName : "Carta digital " + tableContext.displayName() + " - " + platformName);
        model.addAttribute("seoDescription", "Carta digital QR de " + platformName + ". Revisa platos, bebidas y solicita atención desde tu mesa.");
        model.addAttribute("seoCanonicalUrl", publicMenuUrl);
        model.addAttribute("seoOgType", "website");
        model.addAttribute("seoOgImage", platformLogo);
    }

    private String attentionWhatsappLink(RestaurantPublicTableContext tableContext, String publicMenuUrl) {
        String whatsappNumber = setting("public.whatsapp.number", businessProperties.getWhatsappNumber());
        String tableLabel = tableContext == null ? "la carta digital" : tableContext.displayName();
        String message = "Hola, estoy revisando " + tableLabel + " y deseo solicitar atención. Link: " + publicMenuUrl;
        return "https://wa.me/" + whatsappNumber + "?text=" + URLEncoder.encode(message, StandardCharsets.UTF_8);
    }

    private String absoluteUrl(HttpServletRequest request, String path) {
        String cleanPath = path == null || path.isBlank() ? "/" : path;
        if (!cleanPath.startsWith("/")) {
            cleanPath = "/" + cleanPath;
        }
        return request.getScheme() + "://" + request.getServerName() + portPart(request) + cleanPath;
    }

    private String portPart(HttpServletRequest request) {
        int port = request.getServerPort();
        boolean defaultHttp = "http".equalsIgnoreCase(request.getScheme()) && port == 80;
        boolean defaultHttps = "https".equalsIgnoreCase(request.getScheme()) && port == 443;
        return defaultHttp || defaultHttps ? "" : ":" + port;
    }

    private String normalizeStockFilter(String value) {
        String clean = value == null ? "ALL" : value.trim().toUpperCase();
        return switch (clean) {
            case "AVAILABLE", "LOW", "OUT", "HIDDEN" -> clean;
            default -> "ALL";
        };
    }

    private boolean isChecked(String value) {
        return value != null && ("on".equalsIgnoreCase(value) || "true".equalsIgnoreCase(value) || "1".equals(value));
    }

    private String setting(String variable, String defaultValue) {
        return platformSettingService.get(variable, defaultValue);
    }
}
