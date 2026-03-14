package com.ecoamazonas.eco_agua;

import com.ecoamazonas.eco_agua.cashflow.BreakEvenResult;
import com.ecoamazonas.eco_agua.cashflow.BreakEvenService;
import com.ecoamazonas.eco_agua.cashflow.BreakEvenStatus;
import com.ecoamazonas.eco_agua.expense.Expense;
import com.ecoamazonas.eco_agua.expense.ExpenseService;
import com.ecoamazonas.eco_agua.order.OrderService;
import com.ecoamazonas.eco_agua.order.OrderStatus;
import com.ecoamazonas.eco_agua.order.PossibleOrderSuggestion;
import com.ecoamazonas.eco_agua.order.SaleOrder;
import com.ecoamazonas.eco_agua.product.Product;
import com.ecoamazonas.eco_agua.product.ProductRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.math.BigDecimal;
import java.math.RoundingMode;
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
    private final BreakEvenService breakEvenService;
    private final ProductRepository productRepository;

    public HomeController(
            OrderService orderService,
            ExpenseService expenseService,
            BreakEvenService breakEvenService,
            ProductRepository productRepository
    ) {
        this.orderService = orderService;
        this.expenseService = expenseService;
        this.breakEvenService = breakEvenService;
        this.productRepository = productRepository;
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

        LocalDate monthStart = today.withDayOfMonth(1);
        LocalDate monthEnd = today.withDayOfMonth(today.lengthOfMonth());

        BreakEvenResult homeBreakEvenResult = null;
        Long homeBreakEvenProductId = null;
        Integer homeBreakEvenYear = today.getYear();
        Integer homeBreakEvenMonth = today.getMonthValue();
        String homeBreakEvenStatusLabel = "Sin datos";
        String homeBreakEvenStatusClass = "secondary";
        BigDecimal homeBreakEvenProgressPercent = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        BigDecimal homeBreakEvenGapUnits = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        BigDecimal homeBreakEvenUnitsPerRemainingDay = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        long homeBreakEvenRemainingDays = ChronoUnit.DAYS.between(today, monthEnd) + 1;

        List<Product> activeProducts = productRepository.findByActiveTrueOrderByNameAsc();
        if (!activeProducts.isEmpty()) {
            Product product = activeProducts.get(0);
            homeBreakEvenProductId = product.getId();

            homeBreakEvenResult = breakEvenService.calculateForProductAndPeriod(
                    homeBreakEvenProductId,
                    monthStart,
                    monthEnd
            );

            homeBreakEvenStatusLabel = buildBreakEvenStatusLabel(homeBreakEvenResult.getStatus());
            homeBreakEvenStatusClass = buildBreakEvenStatusClass(homeBreakEvenResult.getStatus());
            homeBreakEvenProgressPercent = calculateBreakEvenProgressPercent(
                    homeBreakEvenResult.getUnitsSold(),
                    homeBreakEvenResult.getBreakEvenUnitsRounded()
            );
            homeBreakEvenGapUnits = normalizeAmount(homeBreakEvenResult.getStructuralGapUnits(), 2);

            if (homeBreakEvenGapUnits.compareTo(BigDecimal.ZERO) > 0 && homeBreakEvenRemainingDays > 0) {
                homeBreakEvenUnitsPerRemainingDay = homeBreakEvenGapUnits
                        .divide(BigDecimal.valueOf(homeBreakEvenRemainingDays), 2, RoundingMode.HALF_UP);
            }
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

        model.addAttribute("homeBreakEvenResult", homeBreakEvenResult);
        model.addAttribute("homeBreakEvenProductId", homeBreakEvenProductId);
        model.addAttribute("homeBreakEvenYear", homeBreakEvenYear);
        model.addAttribute("homeBreakEvenMonth", homeBreakEvenMonth);
        model.addAttribute("homeBreakEvenStatusLabel", homeBreakEvenStatusLabel);
        model.addAttribute("homeBreakEvenStatusClass", homeBreakEvenStatusClass);
        model.addAttribute("homeBreakEvenProgressPercent", homeBreakEvenProgressPercent);
        model.addAttribute("homeBreakEvenGapUnits", homeBreakEvenGapUnits);
        model.addAttribute("homeBreakEvenUnitsPerRemainingDay", homeBreakEvenUnitsPerRemainingDay);
        model.addAttribute("homeBreakEvenRemainingDays", homeBreakEvenRemainingDays);

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

    private String buildBreakEvenStatusLabel(BreakEvenStatus status) {
        if (status == null) {
            return "Sin datos";
        }

        return switch (status) {
            case BEFORE_BREAK_EVEN -> "Debajo del equilibrio";
            case AT_BREAK_EVEN -> "En equilibrio";
            case AFTER_BREAK_EVEN -> "Encima del equilibrio";
        };
    }

    private String buildBreakEvenStatusClass(BreakEvenStatus status) {
        if (status == null) {
            return "secondary";
        }

        return switch (status) {
            case BEFORE_BREAK_EVEN -> "danger";
            case AT_BREAK_EVEN -> "warning";
            case AFTER_BREAK_EVEN -> "success";
        };
    }

    private BigDecimal calculateBreakEvenProgressPercent(BigDecimal soldUnits, BigDecimal targetUnits) {
        BigDecimal sold = normalizeAmount(soldUnits, 2);
        BigDecimal target = normalizeAmount(targetUnits, 2);

        if (target.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }

        BigDecimal percent = sold.multiply(BigDecimal.valueOf(100))
                .divide(target, 2, RoundingMode.HALF_UP);

        if (percent.compareTo(BigDecimal.valueOf(100)) > 0) {
            return BigDecimal.valueOf(100).setScale(2, RoundingMode.HALF_UP);
        }

        if (percent.compareTo(BigDecimal.ZERO) < 0) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }

        return percent;
    }

    private BigDecimal normalizeAmount(BigDecimal value, int scale) {
        if (value == null) {
            return BigDecimal.ZERO.setScale(scale, RoundingMode.HALF_UP);
        }

        return value.setScale(scale, RoundingMode.HALF_UP);
    }
}