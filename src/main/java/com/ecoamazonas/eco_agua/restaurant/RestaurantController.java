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
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
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
        model.addAttribute("tableRequestTypes", publicTableRequestTypes());
        addPublicRestaurantAttributes(model, tableContext, publicMenuUrl);
        return "public/restaurant_menu";
    }

    @PostMapping("/restaurant/table-request")
    public String createPublicTableRequest(@RequestParam(required = false) Long tableId,
                                           @RequestParam(defaultValue = "ATTENTION") String requestType,
                                           @RequestParam(required = false) String customerNote,
                                           RedirectAttributes redirectAttributes) {
        try {
            ensureRestaurantRuntimeReady();
            restaurantService.createTableRequest(tableId, requestType, customerNote);
            redirectAttributes.addFlashAttribute("successMessage", publicTableRequestMessage(requestType));
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return tableId == null ? "redirect:/restaurant/menu" : "redirect:/restaurant/menu?tableId=" + tableId;
    }

    @PostMapping("/restaurant/qr-order")
    public String createPublicQrOrder(@RequestParam Long tableId,
                                      @RequestParam(required = false) String customerNote,
                                      @RequestParam Map<String, String> params,
                                      RedirectAttributes redirectAttributes) {
        try {
            ensureRestaurantRuntimeReady();
            Long qrOrderId = restaurantService.createQrOrder(tableId, customerNote, params);
            redirectAttributes.addFlashAttribute("successMessage", "Pedido QR enviado. Un mozo lo revisará antes de enviarlo a cocina. Código interno: QR-" + qrOrderId + ".");
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/restaurant/menu?tableId=" + tableId;
    }

    @GetMapping("/admin/restaurant")
    public String restaurantHome() {
        return "redirect:/admin/restaurant/dashboard";
    }

    @GetMapping("/admin/restaurant/dashboard")
    public String dashboard(Model model) {
        ensureRestaurantRuntimeReady();
        model.addAttribute("activePage", "restaurant_dashboard");
        List<RestaurantTableBoardRow> tableBoard = restaurantService.tableBoard();
        model.addAttribute("summary", restaurantService.dashboardSummary());
        List<RestaurantOrderRow> activeOrders = restaurantService.activeOrders();
        List<RestaurantOrderRow> kitchenOrders = restaurantService.kitchenOrders();
        model.addAttribute("tableBoard", tableBoard);
        model.addAttribute("activeOrders", activeOrders);
        model.addAttribute("kitchenOrders", kitchenOrders);
        model.addAttribute("itemsByOrder", restaurantService.itemsByOrder(activeOrders));
        model.addAttribute("kitchenItemsByOrder", restaurantService.itemsByOrder(kitchenOrders));
        List<RestaurantQrOrderRow> pendingQrOrders = restaurantService.pendingQrOrders();
        model.addAttribute("pendingTableRequests", restaurantService.pendingTableRequests());
        model.addAttribute("pendingTableRequestCount", restaurantService.pendingTableRequestCount());
        model.addAttribute("pendingQrOrders", pendingQrOrders);
        model.addAttribute("pendingQrOrderCount", restaurantService.pendingQrOrderCount());
        model.addAttribute("qrItemsByOrder", restaurantService.itemsByQrOrder(pendingQrOrders));
        model.addAttribute("upcomingReservations", restaurantService.upcomingReservations());
        model.addAttribute("nextReservationsByTable", restaurantService.nextReservationsByTable());
        model.addAttribute("externalOrders", restaurantService.externalOrdersForDashboard());
        model.addAttribute("activeExternalOrderCount", restaurantService.activeExternalOrderCount());
        return "admin/restaurant/dashboard";
    }

    @GetMapping("/admin/restaurant/qr-orders")
    public String qrOrders(@RequestParam(defaultValue = "PENDING") String statusFilter, Model model) {
        ensureRestaurantRuntimeReady();
        List<RestaurantQrOrderRow> qrOrders = restaurantService.qrOrders(statusFilter);
        model.addAttribute("activePage", "restaurant_qr_orders");
        model.addAttribute("currentStatusFilter", normalizeQrOrderFilter(statusFilter));
        model.addAttribute("pendingQrOrderCount", restaurantService.pendingQrOrderCount());
        model.addAttribute("qrOrders", qrOrders);
        model.addAttribute("itemsByOrder", restaurantService.itemsByQrOrder(qrOrders));
        return "admin/restaurant/qr_orders";
    }

    @PostMapping("/admin/restaurant/qr-orders/{id}/approve")
    public String approveQrOrder(@PathVariable Long id,
                                 @RequestParam(defaultValue = "/admin/restaurant/qr-orders") String returnTo,
                                 RedirectAttributes redirectAttributes) {
        try {
            ensureRestaurantRuntimeReady();
            Long orderId = restaurantService.approveQrOrder(id);
            redirectAttributes.addFlashAttribute("successMessage", "Pedido QR aprobado y enviado a cocina.");
            return "redirect:/admin/restaurant/orders/" + orderId;
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        if (returnTo.startsWith("/admin/restaurant")) {
            return "redirect:" + returnTo;
        }
        return "redirect:/admin/restaurant/qr-orders";
    }

    @PostMapping("/admin/restaurant/qr-orders/{id}/reject")
    public String rejectQrOrder(@PathVariable Long id,
                                @RequestParam(defaultValue = "/admin/restaurant/qr-orders") String returnTo,
                                RedirectAttributes redirectAttributes) {
        try {
            ensureRestaurantRuntimeReady();
            restaurantService.rejectQrOrder(id);
            redirectAttributes.addFlashAttribute("successMessage", "Pedido QR rechazado.");
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        if (returnTo.startsWith("/admin/restaurant")) {
            return "redirect:" + returnTo;
        }
        return "redirect:/admin/restaurant/qr-orders";
    }

    @GetMapping("/admin/restaurant/table-requests")
    public String tableRequests(@RequestParam(defaultValue = "PENDING") String statusFilter, Model model) {
        ensureRestaurantRuntimeReady();
        model.addAttribute("activePage", "restaurant_table_requests");
        model.addAttribute("currentStatusFilter", normalizeTableRequestFilter(statusFilter));
        model.addAttribute("pendingTableRequestCount", restaurantService.pendingTableRequestCount());
        model.addAttribute("requests", restaurantService.tableRequests(statusFilter));
        return "admin/restaurant/table_requests";
    }

    @PostMapping("/admin/restaurant/table-requests/{id}/resolve")
    public String resolveTableRequest(@PathVariable Long id,
                                      @RequestParam(defaultValue = "/admin/restaurant/table-requests") String returnTo,
                                      RedirectAttributes redirectAttributes) {
        try {
            ensureRestaurantRuntimeReady();
            restaurantService.resolveTableRequest(id);
            redirectAttributes.addFlashAttribute("successMessage", "Solicitud marcada como atendida.");
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        if (returnTo.startsWith("/admin/restaurant")) {
            return "redirect:" + returnTo;
        }
        return "redirect:/admin/restaurant/table-requests";
    }

    @GetMapping("/admin/restaurant/reservations")
    public String reservations(@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
                               @RequestParam(defaultValue = "ALL") String statusFilter,
                               Model model) {
        ensureRestaurantRuntimeReady();
        LocalDate selectedDate = date == null ? LocalDate.now() : date;
        model.addAttribute("activePage", "restaurant_reservations");
        model.addAttribute("selectedDate", selectedDate);
        model.addAttribute("currentStatusFilter", normalizeReservationFilter(statusFilter));
        model.addAttribute("reservationCounts", restaurantService.reservationStatusCounts(selectedDate));
        model.addAttribute("reservations", restaurantService.reservations(selectedDate, statusFilter));
        return "admin/restaurant/reservations";
    }

    @GetMapping("/admin/restaurant/reservations/new")
    public String newReservation(@RequestParam(required = false) Long tableId, Model model) {
        ensureRestaurantRuntimeReady();
        model.addAttribute("activePage", "restaurant_reservations");
        model.addAttribute("formTitle", "Nueva reserva");
        model.addAttribute("reservation", null);
        model.addAttribute("formAction", "/admin/restaurant/reservations");
        model.addAttribute("tables", restaurantService.reservationTables());
        model.addAttribute("selectedTableId", tableId);
        model.addAttribute("defaultReservationAt", LocalDateTime.now().plusHours(1).withMinute(0).withSecond(0).withNano(0));
        return "admin/restaurant/reservation_form";
    }

    @GetMapping("/admin/restaurant/reservations/{id}/edit")
    public String editReservation(@PathVariable Long id, Model model, RedirectAttributes redirectAttributes) {
        ensureRestaurantRuntimeReady();
        RestaurantReservationRow reservation = restaurantService.reservation(id);
        if (reservation == null) {
            redirectAttributes.addFlashAttribute("errorMessage", "La reserva seleccionada no existe.");
            return "redirect:/admin/restaurant/reservations";
        }
        if (!reservation.canEdit()) {
            redirectAttributes.addFlashAttribute("errorMessage", "La reserva ya fue cerrada o convertida en comanda.");
            return "redirect:/admin/restaurant/reservations?date=" + reservation.reservationAt().toLocalDate();
        }
        model.addAttribute("activePage", "restaurant_reservations");
        model.addAttribute("formTitle", "Editar reserva");
        model.addAttribute("reservation", reservation);
        model.addAttribute("formAction", "/admin/restaurant/reservations/" + reservation.id());
        model.addAttribute("tables", restaurantService.reservationTables());
        model.addAttribute("selectedTableId", reservation.tableId());
        model.addAttribute("defaultReservationAt", reservation.reservationAt());
        return "admin/restaurant/reservation_form";
    }

    @PostMapping("/admin/restaurant/reservations")
    public String createReservation(@RequestParam Long tableId,
                                    @RequestParam String customerName,
                                    @RequestParam(required = false) String customerPhone,
                                    @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm") LocalDateTime reservationAt,
                                    @RequestParam(defaultValue = "90") int durationMinutes,
                                    @RequestParam(defaultValue = "1") int partySize,
                                    @RequestParam(defaultValue = "PENDING") String status,
                                    @RequestParam(required = false) String notes,
                                    RedirectAttributes redirectAttributes) {
        try {
            ensureRestaurantRuntimeReady();
            restaurantService.createReservation(tableId, customerName, customerPhone, reservationAt,
                    durationMinutes, partySize, status, notes);
            redirectAttributes.addFlashAttribute("successMessage", "Reserva registrada correctamente.");
            return "redirect:/admin/restaurant/reservations?date=" + reservationAt.toLocalDate();
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
            return "redirect:/admin/restaurant/reservations/new?tableId=" + tableId;
        }
    }

    @PostMapping("/admin/restaurant/reservations/{id}")
    public String updateReservation(@PathVariable Long id,
                                    @RequestParam Long tableId,
                                    @RequestParam String customerName,
                                    @RequestParam(required = false) String customerPhone,
                                    @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm") LocalDateTime reservationAt,
                                    @RequestParam(defaultValue = "90") int durationMinutes,
                                    @RequestParam(defaultValue = "1") int partySize,
                                    @RequestParam(defaultValue = "PENDING") String status,
                                    @RequestParam(required = false) String notes,
                                    RedirectAttributes redirectAttributes) {
        try {
            ensureRestaurantRuntimeReady();
            restaurantService.updateReservation(id, tableId, customerName, customerPhone, reservationAt,
                    durationMinutes, partySize, status, notes);
            redirectAttributes.addFlashAttribute("successMessage", "Reserva actualizada correctamente.");
            return "redirect:/admin/restaurant/reservations?date=" + reservationAt.toLocalDate();
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
            return "redirect:/admin/restaurant/reservations/" + id + "/edit";
        }
    }

    @PostMapping("/admin/restaurant/reservations/{id}/status")
    public String updateReservationStatus(@PathVariable Long id,
                                          @RequestParam String status,
                                          @RequestParam(defaultValue = "/admin/restaurant/reservations") String returnTo,
                                          RedirectAttributes redirectAttributes) {
        try {
            ensureRestaurantRuntimeReady();
            restaurantService.updateReservationStatus(id, status);
            redirectAttributes.addFlashAttribute("successMessage", "Estado de la reserva actualizado.");
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return returnTo.startsWith("/admin/restaurant") ? "redirect:" + returnTo : "redirect:/admin/restaurant/reservations";
    }

    @GetMapping("/admin/restaurant/reservations/{id}/open-order")
    public String openReservationOrder(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            ensureRestaurantRuntimeReady();
            restaurantService.reservationForOrder(id);
            return "redirect:/admin/restaurant/orders/new?reservationId=" + id;
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
            return "redirect:/admin/restaurant/reservations";
        }
    }

    @GetMapping("/admin/restaurant/external-orders")
    public String externalOrders(@RequestParam(defaultValue = "ACTIVE") String statusFilter, Model model) {
        ensureRestaurantRuntimeReady();
        model.addAttribute("activePage", "restaurant_external_orders");
        model.addAttribute("currentStatusFilter", normalizeExternalOrderFilter(statusFilter));
        model.addAttribute("statusCounts", restaurantService.externalOrderStatusCounts());
        model.addAttribute("orders", restaurantService.externalOrders(statusFilter));
        return "admin/restaurant/external_orders";
    }

    @GetMapping("/admin/restaurant/external-orders/new")
    public String newExternalOrder(@RequestParam(defaultValue = "TAKEAWAY") String serviceType, Model model) {
        ensureRestaurantRuntimeReady();
        String cleanServiceType = "DELIVERY".equalsIgnoreCase(serviceType) ? "DELIVERY" : "TAKEAWAY";
        model.addAttribute("activePage", "restaurant_external_orders");
        model.addAttribute("serviceType", cleanServiceType);
        model.addAttribute("defaultScheduledAt", LocalDateTime.now().plusMinutes(30).format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm")));
        model.addAttribute("menuItems", restaurantService.menuItems());
        return "admin/restaurant/external_order_form";
    }

    @PostMapping("/admin/restaurant/external-orders")
    public String createExternalOrder(@RequestParam(defaultValue = "TAKEAWAY") String serviceType,
                                      @RequestParam String customerName,
                                      @RequestParam String customerPhone,
                                      @RequestParam(required = false) String deliveryAddress,
                                      @RequestParam(required = false) String deliveryReference,
                                      @RequestParam(required = false)
                                      @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime scheduledAt,
                                      @RequestParam(required = false) BigDecimal deliveryFee,
                                      @RequestParam(required = false) String notes,
                                      @RequestParam Map<String, String> params,
                                      RedirectAttributes redirectAttributes) {
        try {
            ensureRestaurantRuntimeReady();
            Long orderId = restaurantService.createExternalOrder(
                    serviceType,
                    customerName,
                    customerPhone,
                    deliveryAddress,
                    deliveryReference,
                    scheduledAt,
                    deliveryFee,
                    notes,
                    params
            );
            redirectAttributes.addFlashAttribute("successMessage", "Pedido externo registrado correctamente.");
            return "redirect:/admin/restaurant/external-orders/" + orderId;
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
            String cleanServiceType = "DELIVERY".equalsIgnoreCase(serviceType) ? "DELIVERY" : "TAKEAWAY";
            return "redirect:/admin/restaurant/external-orders/new?serviceType=" + cleanServiceType;
        }
    }

    @GetMapping("/admin/restaurant/external-orders/{id}")
    public String externalOrderDetail(@PathVariable Long id, Model model, RedirectAttributes redirectAttributes) {
        ensureRestaurantRuntimeReady();
        RestaurantExternalOrderRow order = restaurantService.externalOrder(id);
        if (order == null) {
            redirectAttributes.addFlashAttribute("errorMessage", "El pedido externo seleccionado no existe.");
            return "redirect:/admin/restaurant/external-orders";
        }

        model.addAttribute("activePage", "restaurant_external_orders");
        model.addAttribute("order", order);
        model.addAttribute("items", restaurantService.orderItems(id));
        return "admin/restaurant/external_order_detail";
    }

    @PostMapping("/admin/restaurant/external-orders/{id}/status")
    public String updateExternalOrderStatus(@PathVariable Long id,
                                            @RequestParam String status,
                                            RedirectAttributes redirectAttributes) {
        try {
            ensureRestaurantRuntimeReady();
            restaurantService.updateExternalOrderStatus(id, status);
            redirectAttributes.addFlashAttribute("successMessage", "Estado del pedido actualizado.");
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/admin/restaurant/external-orders/" + id;
    }

    @PostMapping("/admin/restaurant/external-orders/{id}/cancel")
    public String cancelExternalOrder(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            ensureRestaurantRuntimeReady();
            restaurantService.cancelOrder(id);
            redirectAttributes.addFlashAttribute("successMessage", "Pedido externo cancelado y stock devuelto.");
            return "redirect:/admin/restaurant/external-orders";
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
            return "redirect:/admin/restaurant/external-orders/" + id;
        }
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


    @GetMapping("/admin/restaurant/ingredients")
    public String ingredients(@RequestParam(defaultValue = "ALL") String stockFilter, Model model) {
        ensureRestaurantRuntimeReady();
        model.addAttribute("activePage", "restaurant_ingredients");
        model.addAttribute("currentStockFilter", normalizeIngredientFilter(stockFilter));
        model.addAttribute("summary", restaurantService.ingredientSummary());
        model.addAttribute("ingredients", restaurantService.ingredients(stockFilter));
        return "admin/restaurant/ingredients";
    }

    @GetMapping("/admin/restaurant/ingredients/new")
    public String newIngredient(Model model) {
        ensureRestaurantRuntimeReady();
        model.addAttribute("activePage", "restaurant_ingredients");
        model.addAttribute("ingredient", null);
        model.addAttribute("unitOptions", ingredientUnitOptions());
        model.addAttribute("formAction", "/admin/restaurant/ingredients");
        model.addAttribute("formTitle", "Nuevo ingrediente");
        return "admin/restaurant/ingredient_form";
    }

    @GetMapping("/admin/restaurant/ingredients/{id}/edit")
    public String editIngredient(@PathVariable Long id, Model model, RedirectAttributes redirectAttributes) {
        ensureRestaurantRuntimeReady();
        RestaurantIngredientRow ingredient = restaurantService.ingredient(id);
        if (ingredient == null) {
            redirectAttributes.addFlashAttribute("errorMessage", "El ingrediente seleccionado no existe.");
            return "redirect:/admin/restaurant/ingredients";
        }
        model.addAttribute("activePage", "restaurant_ingredients");
        model.addAttribute("ingredient", ingredient);
        model.addAttribute("unitOptions", ingredientUnitOptions());
        model.addAttribute("formAction", "/admin/restaurant/ingredients/" + id);
        model.addAttribute("formTitle", "Editar ingrediente");
        return "admin/restaurant/ingredient_form";
    }

    @PostMapping("/admin/restaurant/ingredients")
    public String createIngredient(@RequestParam String name,
                                   @RequestParam(defaultValue = "UNIT") String unitCode,
                                   @RequestParam(required = false) BigDecimal unitCost,
                                   @RequestParam(required = false) BigDecimal stock,
                                   @RequestParam(required = false) BigDecimal minimumStock,
                                   @RequestParam(required = false) String active,
                                   @RequestParam(required = false) String notes,
                                   RedirectAttributes redirectAttributes) {
        try {
            ensureRestaurantRuntimeReady();
            Long id = restaurantService.createIngredient(name, unitCode, unitCost, stock, minimumStock, isChecked(active), notes);
            redirectAttributes.addFlashAttribute("successMessage", "Ingrediente creado correctamente.");
            return "redirect:/admin/restaurant/ingredients/" + id + "/edit";
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
            return "redirect:/admin/restaurant/ingredients/new";
        }
    }

    @PostMapping("/admin/restaurant/ingredients/{id}")
    public String updateIngredient(@PathVariable Long id,
                                   @RequestParam String name,
                                   @RequestParam(defaultValue = "UNIT") String unitCode,
                                   @RequestParam(required = false) BigDecimal unitCost,
                                   @RequestParam(required = false) BigDecimal stock,
                                   @RequestParam(required = false) BigDecimal minimumStock,
                                   @RequestParam(required = false) String active,
                                   @RequestParam(required = false) String notes,
                                   RedirectAttributes redirectAttributes) {
        try {
            ensureRestaurantRuntimeReady();
            restaurantService.updateIngredient(id, name, unitCode, unitCost, stock, minimumStock, isChecked(active), notes);
            redirectAttributes.addFlashAttribute("successMessage", "Ingrediente actualizado correctamente.");
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/admin/restaurant/ingredients/" + id + "/edit";
    }

    @PostMapping("/admin/restaurant/ingredients/{id}/active")
    public String toggleIngredientActive(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            ensureRestaurantRuntimeReady();
            restaurantService.toggleIngredientActive(id);
            redirectAttributes.addFlashAttribute("successMessage", "Estado del ingrediente actualizado.");
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/admin/restaurant/ingredients";
    }

    @PostMapping("/admin/restaurant/ingredients/{id}/stock")
    public String replenishIngredientStock(@PathVariable Long id,
                                           @RequestParam(required = false) BigDecimal quantity,
                                           @RequestParam(required = false) String notes,
                                           RedirectAttributes redirectAttributes) {
        try {
            ensureRestaurantRuntimeReady();
            restaurantService.replenishIngredientStock(id, quantity, notes);
            redirectAttributes.addFlashAttribute("successMessage", "Stock del ingrediente repuesto correctamente.");
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/admin/restaurant/ingredients";
    }

    @GetMapping("/admin/restaurant/ingredients/{id}/movements")
    public String ingredientMovements(@PathVariable Long id,
                                      Model model,
                                      RedirectAttributes redirectAttributes) {
        ensureRestaurantRuntimeReady();
        RestaurantIngredientRow ingredient = restaurantService.ingredient(id);
        if (ingredient == null) {
            redirectAttributes.addFlashAttribute("errorMessage", "El ingrediente seleccionado no existe.");
            return "redirect:/admin/restaurant/ingredients";
        }
        model.addAttribute("activePage", "restaurant_ingredient_movements");
        model.addAttribute("ingredient", ingredient);
        model.addAttribute("movements", restaurantService.ingredientMovements(id));
        return "admin/restaurant/ingredient_movements";
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
        model.addAttribute("stockControlOptions", stockControlOptions());
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
        model.addAttribute("stockControlOptions", stockControlOptions());
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
                                 @RequestParam(defaultValue = "PRODUCT") String stockControlMode,
                                 @RequestParam(defaultValue = "0") int sortOrder,
                                 RedirectAttributes redirectAttributes) {
        try {
            ensureRestaurantRuntimeReady();
            restaurantService.createMenuItem(
                    name, description, imagePath, price, stock, minimumStock, categoryId, newCategoryName,
                    isChecked(active), isChecked(featured), isChecked(restaurantVisible), isChecked(restaurantAvailable),
                    stockControlMode, sortOrder
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
                                 @RequestParam(defaultValue = "PRODUCT") String stockControlMode,
                                 @RequestParam(defaultValue = "0") int sortOrder,
                                 RedirectAttributes redirectAttributes) {
        try {
            ensureRestaurantRuntimeReady();
            restaurantService.updateMenuItem(
                    id, name, description, imagePath, price, stock, minimumStock, categoryId, newCategoryName,
                    isChecked(active), isChecked(featured), isChecked(restaurantVisible), isChecked(restaurantAvailable),
                    stockControlMode, sortOrder
            );
            redirectAttributes.addFlashAttribute("successMessage", "Plato actualizado correctamente.");
            return "redirect:/admin/restaurant/menu-items";
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
            return "redirect:/admin/restaurant/menu-items/" + id + "/edit";
        }
    }

    @GetMapping("/admin/restaurant/menu-items/{id}/recipe")
    public String recipe(@PathVariable Long id, Model model, RedirectAttributes redirectAttributes) {
        ensureRestaurantRuntimeReady();
        RestaurantMenuAdminRow item = restaurantService.menuItemAdmin(id);
        if (item == null) {
            redirectAttributes.addFlashAttribute("errorMessage", "El plato seleccionado no existe.");
            return "redirect:/admin/restaurant/menu-items";
        }
        model.addAttribute("activePage", "restaurant_recipe");
        model.addAttribute("item", item);
        model.addAttribute("recipeItems", restaurantService.recipeItems(id));
        model.addAttribute("ingredients", restaurantService.ingredients("ACTIVE"));
        return "admin/restaurant/recipe";
    }

    @PostMapping("/admin/restaurant/menu-items/{id}/recipe")
    public String saveRecipeItem(@PathVariable Long id,
                                 @RequestParam Long ingredientId,
                                 @RequestParam BigDecimal quantity,
                                 RedirectAttributes redirectAttributes) {
        try {
            ensureRestaurantRuntimeReady();
            restaurantService.saveRecipeItem(id, ingredientId, quantity);
            redirectAttributes.addFlashAttribute("successMessage", "Ingrediente agregado a la receta.");
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/admin/restaurant/menu-items/" + id + "/recipe";
    }

    @PostMapping("/admin/restaurant/menu-items/{productId}/recipe/{recipeItemId}/quantity")
    public String updateRecipeItemQuantity(@PathVariable Long productId,
                                           @PathVariable Long recipeItemId,
                                           @RequestParam BigDecimal quantity,
                                           RedirectAttributes redirectAttributes) {
        try {
            ensureRestaurantRuntimeReady();
            restaurantService.updateRecipeItemQuantity(productId, recipeItemId, quantity);
            redirectAttributes.addFlashAttribute("successMessage", "Cantidad de la receta actualizada.");
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/admin/restaurant/menu-items/" + productId + "/recipe";
    }

    @PostMapping("/admin/restaurant/menu-items/{productId}/recipe/{recipeItemId}/remove")
    public String removeRecipeItem(@PathVariable Long productId,
                                   @PathVariable Long recipeItemId,
                                   RedirectAttributes redirectAttributes) {
        try {
            ensureRestaurantRuntimeReady();
            restaurantService.removeRecipeItem(productId, recipeItemId);
            redirectAttributes.addFlashAttribute("successMessage", "Ingrediente retirado de la receta.");
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/admin/restaurant/menu-items/" + productId + "/recipe";
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
        model.addAttribute("pendingTableRequests", restaurantService.pendingTableRequests());
        model.addAttribute("pendingBillRequestCount", restaurantService.pendingBillRequestCount());
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
    public String newOrder(@RequestParam(required = false) Long tableId,
                           @RequestParam(required = false) Long reservationId,
                           Model model,
                           RedirectAttributes redirectAttributes) {
        ensureRestaurantRuntimeReady();
        RestaurantReservationRow reservation = null;
        if (reservationId != null) {
            try {
                reservation = restaurantService.reservationForOrder(reservationId);
                tableId = reservation.tableId();
            } catch (IllegalArgumentException ex) {
                redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
                return "redirect:/admin/restaurant/reservations";
            }
        }
        model.addAttribute("activePage", "restaurant_orders_new");
        model.addAttribute("tables", restaurantService.availableTables());
        model.addAttribute("menuItems", restaurantService.menuItems());
        model.addAttribute("selectedTableId", tableId);
        model.addAttribute("reservation", reservation);
        model.addAttribute("prefillCustomerName", reservation == null ? "" : reservation.customerName());
        model.addAttribute("prefillCustomerPhone", reservation == null ? "" : reservation.customerPhone());
        model.addAttribute("prefillNotes", reservation == null ? "" : reservation.notes());
        return "admin/restaurant/order_form";
    }

    @PostMapping("/admin/restaurant/orders")
    public String createOrder(@RequestParam(required = false) Long reservationId,
                              @RequestParam(defaultValue = "DINE_IN") String serviceType,
                              @RequestParam(required = false) Long tableId,
                              @RequestParam(required = false) String customerName,
                              @RequestParam(required = false) String customerPhone,
                              @RequestParam(required = false) String notes,
                              @RequestParam Map<String, String> params,
                              RedirectAttributes redirectAttributes) {
        try {
            ensureRestaurantRuntimeReady();
            Long orderId = reservationId == null
                    ? restaurantService.createOrder(serviceType, tableId, customerName, customerPhone, notes, params)
                    : restaurantService.createOrderFromReservation(reservationId, notes, params);
            redirectAttributes.addFlashAttribute("successMessage", reservationId == null
                    ? "Comanda registrada y enviada a cocina."
                    : "Reserva convertida en comanda y enviada a cocina.");
            return "redirect:/admin/restaurant/orders/" + orderId;
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
            return reservationId == null
                    ? "redirect:/admin/restaurant/orders/new"
                    : "redirect:/admin/restaurant/orders/new?reservationId=" + reservationId;
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

        RestaurantExternalOrderRow externalOrder = restaurantService.externalOrder(id);
        if (externalOrder != null) {
            return "redirect:/admin/restaurant/external-orders/" + id;
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
        model.addAttribute("externalOrder", restaurantService.externalOrder(id));
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
        model.addAttribute("externalOrder", restaurantService.externalOrder(id));
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
        model.addAttribute("externalOrder", restaurantService.externalOrder(id));
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

    private List<Map<String, String>> publicTableRequestTypes() {
        return List.of(
                Map.of("value", "ATTENTION", "label", "Solicitar atención", "icon", "bi-bell"),
                Map.of("value", "WAITER", "label", "Llamar al mozo", "icon", "bi-person-raised-hand"),
                Map.of("value", "BILL", "label", "Pedir la cuenta", "icon", "bi-receipt"),
                Map.of("value", "PAID_NOTICE", "label", "Ya pagué", "icon", "bi-check2-circle")
        );
    }

    private String publicTableRequestMessage(String requestType) {
        String cleanType = requestType == null ? "ATTENTION" : requestType.trim().toUpperCase();
        return switch (cleanType) {
            case "BILL" -> "Solicitud enviada. En breve te llevarán la cuenta.";
            case "PAID_NOTICE" -> "Aviso enviado. El equipo verificará tu pago.";
            case "WAITER" -> "Solicitud enviada. Un mozo se acercará a tu mesa.";
            case "NOTE" -> "Nota enviada al equipo del restaurante.";
            default -> "Solicitud enviada. Te atenderemos en breve.";
        };
    }

    private String normalizeQrOrderFilter(String value) {
        String clean = value == null ? "PENDING" : value.trim().toUpperCase();
        return switch (clean) {
            case "PENDING", "APPROVED", "REJECTED", "ALL" -> clean;
            default -> "PENDING";
        };
    }

    private String normalizeExternalOrderFilter(String value) {
        String clean = value == null ? "ACTIVE" : value.trim().toUpperCase();
        return switch (clean) {
            case "ALL", "ACTIVE", "NEW", "CONFIRMED", "IN_KITCHEN", "READY",
                    "OUT_FOR_DELIVERY", "DELIVERED", "PAID", "CANCELLED" -> clean;
            default -> "ACTIVE";
        };
    }

    private String normalizeReservationFilter(String value) {
        String clean = value == null ? "ALL" : value.trim().toUpperCase();
        return switch (clean) {
            case "ACTIVE", "PENDING", "CONFIRMED", "ATTENDED", "CANCELLED", "NO_SHOW", "ALL" -> clean;
            default -> "ALL";
        };
    }

    private String normalizeTableRequestFilter(String value) {
        String clean = value == null ? "PENDING" : value.trim().toUpperCase();
        return switch (clean) {
            case "PENDING", "RESOLVED", "ALL" -> clean;
            default -> "PENDING";
        };
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

    private List<Map<String, String>> stockControlOptions() {
        return List.of(
                Map.of("value", "PRODUCT", "label", "Por plato terminado"),
                Map.of("value", "RECIPE", "label", "Por ingredientes de la receta"),
                Map.of("value", "NONE", "label", "Sin control de stock")
        );
    }

    private List<Map<String, String>> ingredientUnitOptions() {
        return List.of(
                Map.of("value", "UNIT", "label", "Unidad"),
                Map.of("value", "KG", "label", "Kilogramo"),
                Map.of("value", "G", "label", "Gramo"),
                Map.of("value", "L", "label", "Litro"),
                Map.of("value", "ML", "label", "Mililitro"),
                Map.of("value", "PORTION", "label", "Porción")
        );
    }

    private String normalizeIngredientFilter(String value) {
        String clean = value == null ? "ALL" : value.trim().toUpperCase();
        return switch (clean) {
            case "ACTIVE", "LOW", "OUT", "INACTIVE" -> clean;
            default -> "ALL";
        };
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
