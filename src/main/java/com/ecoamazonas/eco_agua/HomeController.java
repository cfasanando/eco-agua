package com.ecoamazonas.eco_agua;

import com.ecoamazonas.eco_agua.category.Category;
import com.ecoamazonas.eco_agua.category.CategoryRepository;
import com.ecoamazonas.eco_agua.category.CategoryType;
import com.ecoamazonas.eco_agua.expense.Expense;
import com.ecoamazonas.eco_agua.expense.ExpenseService;
import com.ecoamazonas.eco_agua.order.OrderService;
import com.ecoamazonas.eco_agua.order.OrderStatus;
import com.ecoamazonas.eco_agua.order.SaleOrder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Controller
public class HomeController {

    private final OrderService orderService;
    private final ExpenseService expenseService;
    private final CategoryRepository categoryRepository;

    public HomeController(
            OrderService orderService,
            ExpenseService expenseService,
            CategoryRepository categoryRepository
    ) {
        this.orderService = orderService;
        this.expenseService = expenseService;
        this.categoryRepository = categoryRepository;
    }

    @GetMapping("/home")
    public String home(Model model) {
        LocalDate today = LocalDate.now();

        // Orders paid today (for "Ventas del día")
        List<SaleOrder> paidOrders =
                orderService.findOrdersForDateAndStatus(today, OrderStatus.PAID);

        // Orders requested today and still pending delivery
        List<SaleOrder> requestedOrders =
                orderService.findOrdersForDateAndStatus(today, OrderStatus.REQUESTED);

        // Total amount of paid orders
        BigDecimal totalSalesToday = paidOrders.stream()
                .map(SaleOrder::getTotalAmount)
                .filter(amount -> amount != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Expenses of today (for "Gastos del día" widget)
        List<Expense> dailyExpenses = expenseService.findByDateRange(today, today);

        BigDecimal totalExpensesToday = dailyExpenses.stream()
                .map(Expense::getAmount)
                .filter(amount -> amount != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Expense categories (supporting legacy EXPENSE and current EXPENSES)
        List<Category> expenseCategories =
                categoryRepository.findByTypeInAndActiveTrueOrderByNameAsc(CategoryType.expenseTypes());

        model.addAttribute("activePage", "home");
        model.addAttribute("today", today);

        // Sales
        model.addAttribute("paidOrders", paidOrders);
        model.addAttribute("requestedOrders", requestedOrders);
        model.addAttribute("totalSalesToday", totalSalesToday);

        // Expenses (home widget)
        model.addAttribute("dailyExpenses", dailyExpenses);
        model.addAttribute("expensesToday", dailyExpenses);
        model.addAttribute("totalExpensesToday", totalExpensesToday);
        model.addAttribute("expenseCategories", expenseCategories);
        model.addAttribute("categories", expenseCategories);

        return "home";
    }
}