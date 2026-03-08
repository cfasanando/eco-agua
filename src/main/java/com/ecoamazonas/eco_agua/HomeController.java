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
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
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
        model.addAttribute("expenseCategories", expenseService.findExpenseCategories());
        model.addAttribute("categories", expenseService.findExpenseCategories());
        model.addAttribute("suppliers", expenseService.findActiveSuppliers());
        model.addAttribute("supplies", expenseService.findActiveSupplies());
        model.addAttribute("employees", expenseService.findActiveEmployees());

        return "home";
    }
}