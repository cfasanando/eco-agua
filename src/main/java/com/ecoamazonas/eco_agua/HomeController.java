package com.ecoamazonas.eco_agua;

import com.ecoamazonas.eco_agua.expense.Expense;
import com.ecoamazonas.eco_agua.expense.ExpenseService;
import com.ecoamazonas.eco_agua.order.OrderService;
import com.ecoamazonas.eco_agua.order.OrderStatus;
import com.ecoamazonas.eco_agua.order.PossibleOrderSuggestion;
import com.ecoamazonas.eco_agua.order.SaleOrder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Controller
public class HomeController {

    private final OrderService orderService;
    private final ExpenseService expenseService;

    public HomeController(
            OrderService orderService,
            ExpenseService expenseService
    ) {
        this.orderService = orderService;
        this.expenseService = expenseService;
    }

    @GetMapping("/home")
    public String home(Model model) {
        LocalDate today = LocalDate.now();

        List<SaleOrder> paidOrders = orderService.findOrdersForDateAndStatus(today, OrderStatus.PAID);
        List<SaleOrder> requestedOrders = orderService.findOrdersForDateAndStatus(today, OrderStatus.REQUESTED);
        List<SaleOrder> creditOrders = orderService.findOrdersByStatus(OrderStatus.CREDIT);
        List<PossibleOrderSuggestion> possibleOrders = orderService.getPossibleOrderSuggestions(today, 8);

        BigDecimal totalSalesToday = paidOrders.stream()
                .map(SaleOrder::getTotalAmount)
                .filter(amount -> amount != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        List<Expense> dailyExpenses = expenseService.findByDateRange(today, today);
        BigDecimal totalExpensesToday = dailyExpenses.stream()
                .map(Expense::getAmount)
                .filter(amount -> amount != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalCreditAmount = creditOrders.stream()
                .map(SaleOrder::getTotalAmount)
                .filter(amount -> amount != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Map<Long, Long> creditDaysByOrderId = new LinkedHashMap<>();
        for (SaleOrder order : creditOrders) {
            LocalDate orderDate = order.getOrderDate();
            long days = orderDate != null ? ChronoUnit.DAYS.between(orderDate, today) : 0;
            creditDaysByOrderId.put(order.getId(), Math.max(days, 0));
        }

        LocalDate weekStart = today.with(DayOfWeek.MONDAY);
        LocalDate weekEnd = weekStart.plusDays(6);

        List<SaleOrder> weeklyPaidOrders = orderService.findPaidOrdersBetween(weekStart, weekEnd);
        List<Expense> weeklyExpenses = expenseService.findByDateRange(weekStart, weekEnd);

        BigDecimal weeklySalesTotal = weeklyPaidOrders.stream()
                .map(SaleOrder::getTotalAmount)
                .filter(amount -> amount != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal weeklyExpensesTotal = weeklyExpenses.stream()
                .map(Expense::getAmount)
                .filter(amount -> amount != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal weeklyNetCashFlow = weeklySalesTotal.subtract(weeklyExpensesTotal);
        BigDecimal todayNetCashFlow = totalSalesToday.subtract(totalExpensesToday);

        Map<LocalDate, BigDecimal> weeklySalesByDate = new LinkedHashMap<>();
        Map<LocalDate, BigDecimal> weeklyExpensesByDate = new LinkedHashMap<>();

        for (int i = 0; i < 7; i++) {
            LocalDate currentDate = weekStart.plusDays(i);
            weeklySalesByDate.put(currentDate, BigDecimal.ZERO);
            weeklyExpensesByDate.put(currentDate, BigDecimal.ZERO);
        }

        for (SaleOrder order : weeklyPaidOrders) {
            LocalDate orderDate = order.getOrderDate();
            BigDecimal totalAmount = order.getTotalAmount();

            if (orderDate == null || totalAmount == null) {
                continue;
            }

            if (!orderDate.isBefore(weekStart) && !orderDate.isAfter(weekEnd)) {
                weeklySalesByDate.merge(orderDate, totalAmount, BigDecimal::add);
            }
        }

        for (Expense expense : weeklyExpenses) {
            LocalDate expenseDate = expense.getExpenseDate();
            BigDecimal amount = expense.getAmount();

            if (expenseDate == null || amount == null) {
                continue;
            }

            if (!expenseDate.isBefore(weekStart) && !expenseDate.isAfter(weekEnd)) {
                weeklyExpensesByDate.merge(expenseDate, amount, BigDecimal::add);
            }
        }

        List<Map<String, Object>> weeklyCashFlowRows = new ArrayList<>();
        for (int i = 0; i < 7; i++) {
            LocalDate currentDate = weekStart.plusDays(i);
            BigDecimal sales = weeklySalesByDate.getOrDefault(currentDate, BigDecimal.ZERO);
            BigDecimal expenses = weeklyExpensesByDate.getOrDefault(currentDate, BigDecimal.ZERO);
            BigDecimal net = sales.subtract(expenses);

            Map<String, Object> row = new LinkedHashMap<>();
            row.put("date", currentDate);
            row.put("dayLabel", buildWeekdayShortLabel(currentDate));
            row.put("sales", sales);
            row.put("expenses", expenses);
            row.put("net", net);
            row.put("today", currentDate.equals(today));

            weeklyCashFlowRows.add(row);
        }

        model.addAttribute("activePage", "home");
        model.addAttribute("today", today);
        model.addAttribute("paidOrders", paidOrders);
        model.addAttribute("requestedOrders", requestedOrders);
        model.addAttribute("creditOrders", creditOrders);
        model.addAttribute("possibleOrders", possibleOrders);
        model.addAttribute("creditDaysByOrderId", creditDaysByOrderId);
        model.addAttribute("totalSalesToday", totalSalesToday);
        model.addAttribute("totalCreditAmount", totalCreditAmount);
        model.addAttribute("dailyExpenses", dailyExpenses);
        model.addAttribute("expensesToday", dailyExpenses);
        model.addAttribute("totalExpensesToday", totalExpensesToday);

        model.addAttribute("weekStart", weekStart);
        model.addAttribute("weekEnd", weekEnd);
        model.addAttribute("weeklyCashFlowRows", weeklyCashFlowRows);
        model.addAttribute("weeklySalesTotal", weeklySalesTotal);
        model.addAttribute("weeklyExpensesTotal", weeklyExpensesTotal);
        model.addAttribute("weeklyNetCashFlow", weeklyNetCashFlow);
        model.addAttribute("todayNetCashFlow", todayNetCashFlow);

        model.addAttribute("expenseCategories", expenseService.findExpenseCategories());
        model.addAttribute("categories", expenseService.findExpenseCategories());
        model.addAttribute("suppliers", expenseService.findActiveSuppliers());
        model.addAttribute("supplies", expenseService.findActiveSupplies());
        model.addAttribute("employees", expenseService.findActiveEmployees());

        return "home";
    }

    private String buildWeekdayShortLabel(LocalDate date) {
        return switch (date.getDayOfWeek()) {
            case MONDAY -> "Lun";
            case TUESDAY -> "Mar";
            case WEDNESDAY -> "Mié";
            case THURSDAY -> "Jue";
            case FRIDAY -> "Vie";
            case SATURDAY -> "Sáb";
            case SUNDAY -> "Dom";
        };
    }
}