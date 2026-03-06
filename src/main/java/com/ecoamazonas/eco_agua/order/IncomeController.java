package com.ecoamazonas.eco_agua.order;

import com.ecoamazonas.eco_agua.category.CategoryRepository;
import com.ecoamazonas.eco_agua.category.CategoryType;
import com.ecoamazonas.eco_agua.income.OtherIncome;
import com.ecoamazonas.eco_agua.income.OtherIncomeService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Controller
@RequestMapping("/income")
public class IncomeController {

    private final OrderService orderService;
    private final OtherIncomeService otherIncomeService;
    private final CategoryRepository categoryRepository;

    public IncomeController(
            OrderService orderService,
            OtherIncomeService otherIncomeService,
            CategoryRepository categoryRepository
    ) {
        this.orderService = orderService;
        this.otherIncomeService = otherIncomeService;
        this.categoryRepository = categoryRepository;
    }

    @GetMapping("/sales")
    public String salesByDate(
            @RequestParam(name = "mode", required = false, defaultValue = "DAY") String mode,
            @RequestParam(name = "date", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(name = "startDate", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(name = "endDate", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            Model model
    ) {
        LocalDate today = LocalDate.now();
        mode = mode != null ? mode.toUpperCase() : "DAY";

        List<SaleOrder> orders;

        if ("PERIOD".equals(mode)) {
            if (startDate == null && endDate == null) {
                startDate = today;
                endDate = today;
            }
            if (startDate == null) {
                startDate = endDate;
            }
            if (endDate == null) {
                endDate = startDate;
            }
            if (endDate.isBefore(startDate)) {
                LocalDate tmp = startDate;
                startDate = endDate;
                endDate = tmp;
            }

            orders = orderService.findOrdersBetweenDatesAndStatus(
                    startDate,
                    endDate,
                    OrderStatus.PAID
            );
            date = null;
        } else {
            if (date == null) {
                date = today;
            }
            orders = orderService.findOrdersForDateAndStatus(date, OrderStatus.PAID);
            startDate = date;
            endDate = date;
            mode = "DAY";
        }

        BigDecimal total = orders.stream()
                .map(SaleOrder::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        String summaryRange = ("DAY".equals(mode))
                ? "No hay ventas en la fecha " + startDate + " - (pagados)."
                : "No hay ventas entre " + startDate + " y " + endDate + " - (pagados).";

        model.addAttribute("activePage", "income_sales");
        model.addAttribute("mode", mode);
        model.addAttribute("date", date);
        model.addAttribute("startDate", startDate);
        model.addAttribute("endDate", endDate);
        model.addAttribute("orders", orders);
        model.addAttribute("totalAmount", total);
        model.addAttribute("summaryRange", summaryRange);

        return "income/sales_by_date";
    }

    @GetMapping("/credit")
    public String creditAccounts(
            @RequestParam(name = "startDate", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(name = "endDate", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            Model model
    ) {
        LocalDate today = LocalDate.now();
        if (startDate == null) {
            startDate = today;
        }
        if (endDate == null) {
            endDate = today;
        }
        if (endDate.isBefore(startDate)) {
            LocalDate tmp = startDate;
            startDate = endDate;
            endDate = tmp;
        }

        List<SaleOrder> orders = orderService.findOrdersBetweenDatesAndStatus(
                startDate,
                endDate,
                OrderStatus.CREDIT
        );

        BigDecimal total = orders.stream()
                .map(SaleOrder::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        model.addAttribute("activePage", "income_credit");
        model.addAttribute("startDate", startDate);
        model.addAttribute("endDate", endDate);
        model.addAttribute("orders", orders);
        model.addAttribute("totalAmount", total);

        return "income/credit_accounts";
    }

    @GetMapping("/others")
    public String otherIncomeList(
            @RequestParam(name = "startDate", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(name = "endDate", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            Model model
    ) {
        LocalDate today = LocalDate.now();
        if (startDate == null) {
            startDate = today;
        }
        if (endDate == null) {
            endDate = today;
        }
        if (endDate.isBefore(startDate)) {
            LocalDate tmp = startDate;
            startDate = endDate;
            endDate = tmp;
        }

        List<OtherIncome> incomes = otherIncomeService.findByDateRange(startDate, endDate);

        BigDecimal total = incomes.stream()
                .map(OtherIncome::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        model.addAttribute("activePage", "income_others");
        model.addAttribute("startDate", startDate);
        model.addAttribute("endDate", endDate);
        model.addAttribute("incomes", incomes);
        model.addAttribute("totalAmount", total);
        model.addAttribute("incomeCategories",
                categoryRepository.findByTypeAndActiveTrueOrderByNameAsc(CategoryType.INCOME));

        return "income/other_income_placeholder";
    }

    @PostMapping("/others")
    public String addOtherIncome(
            @RequestParam("categoryId") Long categoryId,
            @RequestParam("amount") BigDecimal amount,
            @RequestParam("incomeDate")
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate incomeDate,
            @RequestParam(name = "observation", required = false) String observation,
            RedirectAttributes redirectAttributes
    ) {
        try {
            otherIncomeService.saveFromForm(categoryId, amount, incomeDate, observation);
            redirectAttributes.addFlashAttribute("message", "Ingreso registrado correctamente.");
            redirectAttributes.addFlashAttribute("messageType", "success");
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("message", ex.getMessage());
            redirectAttributes.addFlashAttribute("messageType", "error");
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("message", "Error al registrar el ingreso: " + ex.getMessage());
            redirectAttributes.addFlashAttribute("messageType", "error");
        }

        LocalDate redirectDate = (incomeDate != null ? incomeDate : LocalDate.now());
        return "redirect:/income/others?startDate=" + redirectDate + "&endDate=" + redirectDate;
    }

    @PostMapping("/others/delete")
    public String deleteOtherIncome(
            @RequestParam(name = "ids", required = false) List<Long> ids,
            @RequestParam(name = "startDate", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(name = "endDate", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            RedirectAttributes redirectAttributes
    ) {
        if (ids == null || ids.isEmpty()) {
            redirectAttributes.addFlashAttribute("message", "Seleccione al menos un registro para borrar.");
            redirectAttributes.addFlashAttribute("messageType", "warning");
        } else {
            otherIncomeService.deleteByIds(ids);
            redirectAttributes.addFlashAttribute("message", "Registros eliminados correctamente.");
            redirectAttributes.addFlashAttribute("messageType", "success");
        }

        LocalDate today = LocalDate.now();
        if (startDate == null) {
            startDate = today;
        }
        if (endDate == null) {
            endDate = startDate;
        }

        return "redirect:/income/others?startDate=" + startDate + "&endDate=" + endDate;
    }
}
