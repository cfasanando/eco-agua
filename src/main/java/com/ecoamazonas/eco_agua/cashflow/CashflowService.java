package com.ecoamazonas.eco_agua.cashflow;

import com.ecoamazonas.eco_agua.expense.Expense;
import com.ecoamazonas.eco_agua.expense.ExpenseService;
import com.ecoamazonas.eco_agua.income.OtherIncome;
import com.ecoamazonas.eco_agua.income.OtherIncomeService;
import com.ecoamazonas.eco_agua.order.OrderService;
import com.ecoamazonas.eco_agua.order.OrderStatus;
import com.ecoamazonas.eco_agua.order.SaleOrder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

@Service
public class CashflowService {

    private final OrderService orderService;
    private final ExpenseService expenseService;
    private final OtherIncomeService otherIncomeService;

    public CashflowService(
            OrderService orderService,
            ExpenseService expenseService,
            OtherIncomeService otherIncomeService
    ) {
        this.orderService = orderService;
        this.expenseService = expenseService;
        this.otherIncomeService = otherIncomeService;
    }

    @Transactional(readOnly = true)
    public List<CashflowItem> buildCashflow(LocalDate start, LocalDate end) {
        if (start == null && end == null) {
            throw new IllegalArgumentException("At least one date must be provided.");
        }

        if (start == null) {
            start = end;
        }
        if (end == null) {
            end = start;
        }
        if (end.isBefore(start)) {
            LocalDate tmp = start;
            start = end;
            end = tmp;
        }

        Map<LocalDate, CashflowItem> map = new LinkedHashMap<>();
        LocalDate current = start;
        while (!current.isAfter(end)) {
            CashflowItem item = new CashflowItem();
            item.setDate(current);
            map.put(current, item);
            current = current.plusDays(1);
        }

        // Sales incomes (paid orders)
        List<SaleOrder> paidOrders =
                orderService.findOrdersBetweenDatesAndStatus(start, end, OrderStatus.PAID);

        for (SaleOrder order : paidOrders) {
            LocalDate d = order.getOrderDate();
            CashflowItem item = map.get(d);
            if (item == null) {
                continue;
            }

            BigDecimal amount = nvl(order.getTotalAmount());
            item.setSalesIncome(item.getSalesIncome().add(amount));
            item.setTotalIncome(item.getTotalIncome().add(amount));
        }

        // Other incomes
        List<OtherIncome> otherIncomes = otherIncomeService.findByDateRange(start, end);
        for (OtherIncome oi : otherIncomes) {
            LocalDate d = oi.getIncomeDate();
            CashflowItem item = map.get(d);
            if (item == null) {
                continue;
            }

            BigDecimal amount = nvl(oi.getAmount());
            item.setOtherIncome(item.getOtherIncome().add(amount));
            item.setTotalIncome(item.getTotalIncome().add(amount));
        }

        // Expenses (using expense_date as effective date)
        List<Expense> expenses = expenseService.findByDateRange(start, end);
        for (Expense exp : expenses) {
            LocalDate d = exp.getExpenseDate();
            CashflowItem item = map.get(d);
            if (item == null) {
                continue;
            }

            BigDecimal amount = nvl(exp.getAmount());
            item.setTotalExpense(item.getTotalExpense().add(amount));
        }

        // Net result per day
        for (CashflowItem item : map.values()) {
            item.setNetResult(
                    item.getTotalIncome().subtract(item.getTotalExpense())
            );
        }

        return new ArrayList<>(map.values());
    }

    private BigDecimal nvl(BigDecimal value) {
        return (value != null ? value : BigDecimal.ZERO);
    }

    public BigDecimal calculateTotalIncomes(List<CashflowItem> items) {
        if (items == null || items.isEmpty()) {
            return BigDecimal.ZERO;
        }
        return items.stream()
                .map(CashflowItem::getTotalIncome)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public BigDecimal calculateTotalExpenses(List<CashflowItem> items) {
        if (items == null || items.isEmpty()) {
            return BigDecimal.ZERO;
        }
        return items.stream()
                .map(CashflowItem::getTotalExpense)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public BigDecimal calculateNetResult(List<CashflowItem> items) {
        if (items == null || items.isEmpty()) {
            return BigDecimal.ZERO;
        }
        return items.stream()
                .map(CashflowItem::getNetResult)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
