package com.ecoamazonas.eco_agua.cashflow;

import com.ecoamazonas.eco_agua.expense.Expense;
import com.ecoamazonas.eco_agua.expense.ExpensePayment;
import com.ecoamazonas.eco_agua.expense.ExpensePaymentRepository;
import com.ecoamazonas.eco_agua.expense.ExpenseRepository;
import com.ecoamazonas.eco_agua.expense.ExpenseStatus;
import com.ecoamazonas.eco_agua.income.OtherIncome;
import com.ecoamazonas.eco_agua.income.OtherIncomeService;
import com.ecoamazonas.eco_agua.order.OrderStatus;
import com.ecoamazonas.eco_agua.order.SaleOrder;
import com.ecoamazonas.eco_agua.order.SaleOrderPayment;
import com.ecoamazonas.eco_agua.order.SaleOrderPaymentRepository;
import com.ecoamazonas.eco_agua.order.SaleOrderRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
public class CashflowService {

    private static final BigDecimal ZERO = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);

    private final SaleOrderRepository saleOrderRepository;
    private final SaleOrderPaymentRepository saleOrderPaymentRepository;
    private final ExpenseRepository expenseRepository;
    private final ExpensePaymentRepository expensePaymentRepository;
    private final OtherIncomeService otherIncomeService;

    public CashflowService(
            SaleOrderRepository saleOrderRepository,
            SaleOrderPaymentRepository saleOrderPaymentRepository,
            ExpenseRepository expenseRepository,
            ExpensePaymentRepository expensePaymentRepository,
            OtherIncomeService otherIncomeService
    ) {
        this.saleOrderRepository = saleOrderRepository;
        this.saleOrderPaymentRepository = saleOrderPaymentRepository;
        this.expenseRepository = expenseRepository;
        this.expensePaymentRepository = expensePaymentRepository;
        this.otherIncomeService = otherIncomeService;
    }

    @Transactional(readOnly = true)
    public List<CashflowItem> buildCashflow(LocalDate start, LocalDate end) {
        DateRange range = normalizeRange(start, end);
        Map<LocalDate, CashflowItem> map = initializeItems(range.start(), range.end());

        List<SaleOrder> directPaidSales = findDirectPaidSales(range.start(), range.end());
        for (SaleOrder order : directPaidSales) {
            CashflowItem item = map.get(order.getOrderDate());
            if (item == null) {
                continue;
            }

            BigDecimal amount = nvl(order.getTotalAmount());
            item.setDirectSalesIncome(item.getDirectSalesIncome().add(amount));
            item.setSalesIncome(item.getSalesIncome().add(amount));
            item.setTotalIncome(item.getTotalIncome().add(amount));
        }

        List<SaleOrderPayment> creditCollections = saleOrderPaymentRepository.findCashflowPaymentsBetween(range.start(), range.end());
        for (SaleOrderPayment payment : creditCollections) {
            CashflowItem item = map.get(payment.getPaymentDate());
            if (item == null) {
                continue;
            }

            BigDecimal amount = nvl(payment.getAmount());
            item.setCreditCollectionIncome(item.getCreditCollectionIncome().add(amount));
            item.setSalesIncome(item.getSalesIncome().add(amount));
            item.setTotalIncome(item.getTotalIncome().add(amount));
        }

        List<OtherIncome> otherIncomes = otherIncomeService.findByDateRange(range.start(), range.end());
        for (OtherIncome income : otherIncomes) {
            CashflowItem item = map.get(income.getIncomeDate());
            if (item == null) {
                continue;
            }

            BigDecimal amount = nvl(income.getAmount());
            item.setOtherIncome(item.getOtherIncome().add(amount));
            item.setTotalIncome(item.getTotalIncome().add(amount));
        }

        List<Expense> paidExpenses = expenseRepository.findCashflowPaidExpensesBetween(range.start(), range.end());
        for (Expense expense : paidExpenses) {
            CashflowItem item = map.get(expense.getExpenseDate());
            if (item == null) {
                continue;
            }

            BigDecimal amount = nvl(expense.getPaidAmount()).compareTo(BigDecimal.ZERO) > 0
                    ? nvl(expense.getPaidAmount())
                    : nvl(expense.getAmount());
            item.setCashExpense(item.getCashExpense().add(amount));
            item.setTotalExpense(item.getTotalExpense().add(amount));
        }

        List<ExpensePayment> debtPayments = expensePaymentRepository.findCashflowPaymentsBetween(range.start(), range.end());
        for (ExpensePayment payment : debtPayments) {
            CashflowItem item = map.get(payment.getPaymentDate());
            if (item == null) {
                continue;
            }

            BigDecimal amount = nvl(payment.getAmount());
            item.setDebtPaymentExpense(item.getDebtPaymentExpense().add(amount));
            item.setTotalExpense(item.getTotalExpense().add(amount));
        }

        List<SaleOrder> registeredSales = saleOrderRepository.findRegisteredSalesBetween(
                range.start(),
                range.end(),
                List.of(OrderStatus.PAID, OrderStatus.CREDIT)
        );
        for (SaleOrder order : registeredSales) {
            CashflowItem item = map.get(order.getOrderDate());
            if (item != null) {
                item.setRegisteredSales(item.getRegisteredSales().add(nvl(order.getTotalAmount())));
            }
        }

        List<Expense> registeredExpenses = expenseRepository.findByExpenseDateBetweenOrderByExpenseDateAsc(range.start(), range.end());
        for (Expense expense : registeredExpenses) {
            CashflowItem item = map.get(expense.getExpenseDate());
            if (item != null) {
                item.setRegisteredExpenses(item.getRegisteredExpenses().add(nvl(expense.getAmount())));
            }
        }

        for (CashflowItem item : map.values()) {
            item.setNetResult(item.getTotalIncome().subtract(item.getTotalExpense()));
            item.setRegisteredResult(item.getRegisteredSales().subtract(item.getRegisteredExpenses()));
        }

        return new ArrayList<>(map.values());
    }

    @Transactional(readOnly = true)
    public CashflowSummary buildSummary(LocalDate start, LocalDate end) {
        return buildSummary(buildCashflow(start, end));
    }

    @Transactional(readOnly = true)
    public CashflowSummary buildSummary(List<CashflowItem> items) {
        CashflowSummary summary = new CashflowSummary();

        summary.setDirectSalesIncome(sum(items, CashflowItem::getDirectSalesIncome));
        summary.setCreditCollections(sum(items, CashflowItem::getCreditCollectionIncome));
        summary.setOtherIncome(sum(items, CashflowItem::getOtherIncome));
        summary.setCollectedIncome(sum(items, CashflowItem::getTotalIncome));
        summary.setCashExpenses(sum(items, CashflowItem::getCashExpense));
        summary.setDebtPayments(sum(items, CashflowItem::getDebtPaymentExpense));
        summary.setPaidExpenses(sum(items, CashflowItem::getTotalExpense));
        summary.setNetCashResult(sum(items, CashflowItem::getNetResult));
        summary.setRegisteredSales(sum(items, CashflowItem::getRegisteredSales));
        summary.setRegisteredExpenses(sum(items, CashflowItem::getRegisteredExpenses));
        summary.setRegisteredResult(sum(items, CashflowItem::getRegisteredResult));
        summary.setPendingReceivables(calculatePendingReceivables());
        summary.setPendingPayables(calculatePendingPayables());

        return summary;
    }

    @Transactional(readOnly = true)
    public CashflowDayDetail getDayDetail(LocalDate date) {
        if (date == null) {
            throw new IllegalArgumentException("Date is required.");
        }

        List<SaleOrder> directSales = findDirectPaidSales(date, date);
        List<SaleOrderPayment> creditCollections = saleOrderPaymentRepository.findCashflowPaymentsBetween(date, date);
        List<OtherIncome> otherIncomes = otherIncomeService.findByDateRange(date, date);
        List<Expense> cashExpenses = expenseRepository.findCashflowPaidExpensesBetween(date, date);
        List<ExpensePayment> debtPayments = expensePaymentRepository.findCashflowPaymentsBetween(date, date);

        BigDecimal directSalesTotal = directSales.stream()
                .map(SaleOrder::getTotalAmount)
                .filter(Objects::nonNull)
                .reduce(ZERO, BigDecimal::add);

        BigDecimal creditCollectionTotal = creditCollections.stream()
                .map(SaleOrderPayment::getAmount)
                .filter(Objects::nonNull)
                .reduce(ZERO, BigDecimal::add);

        BigDecimal otherIncomeTotal = otherIncomes.stream()
                .map(OtherIncome::getAmount)
                .filter(Objects::nonNull)
                .reduce(ZERO, BigDecimal::add);

        BigDecimal cashExpenseTotal = cashExpenses.stream()
                .map(expense -> nvl(expense.getPaidAmount()).compareTo(BigDecimal.ZERO) > 0
                        ? expense.getPaidAmount()
                        : expense.getAmount())
                .filter(Objects::nonNull)
                .reduce(ZERO, BigDecimal::add);

        BigDecimal debtPaymentTotal = debtPayments.stream()
                .map(ExpensePayment::getAmount)
                .filter(Objects::nonNull)
                .reduce(ZERO, BigDecimal::add);

        CashflowDayDetail detail = new CashflowDayDetail();
        detail.setDate(date);
        detail.setDirectSales(directSales);
        detail.setCreditCollections(creditCollections);
        detail.setOtherIncomes(otherIncomes);
        detail.setCashExpenses(cashExpenses);
        detail.setDebtPayments(debtPayments);
        detail.setDirectSalesTotal(directSalesTotal);
        detail.setCreditCollectionTotal(creditCollectionTotal);
        detail.setSalesTotal(directSalesTotal.add(creditCollectionTotal));
        detail.setOtherIncomeTotal(otherIncomeTotal);
        detail.setTotalIncome(detail.getSalesTotal().add(otherIncomeTotal));
        detail.setCashExpenseTotal(cashExpenseTotal);
        detail.setDebtPaymentTotal(debtPaymentTotal);
        detail.setExpenseTotal(cashExpenseTotal.add(debtPaymentTotal));
        detail.setNetResult(detail.getTotalIncome().subtract(detail.getExpenseTotal()));

        return detail;
    }

    private List<SaleOrder> findDirectPaidSales(LocalDate start, LocalDate end) {
        return saleOrderRepository.findCashflowOrdersBetweenAndStatus(start, end, OrderStatus.PAID).stream()
                .filter(order -> order.getPayments() == null || order.getPayments().isEmpty())
                .toList();
    }

    private BigDecimal calculatePendingReceivables() {
        return saleOrderRepository.findCashflowOrdersByStatus(OrderStatus.CREDIT).stream()
                .map(SaleOrder::getPendingAmount)
                .filter(Objects::nonNull)
                .reduce(ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal calculatePendingPayables() {
        return expenseRepository.findByDebtTrueAndStatusInOrderByDueDateAsc(List.of(ExpenseStatus.OPEN, ExpenseStatus.PARTIAL)).stream()
                .map(Expense::getBalance)
                .filter(Objects::nonNull)
                .reduce(ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);
    }

    private Map<LocalDate, CashflowItem> initializeItems(LocalDate start, LocalDate end) {
        Map<LocalDate, CashflowItem> map = new LinkedHashMap<>();
        LocalDate current = start;

        while (!current.isAfter(end)) {
            CashflowItem item = new CashflowItem();
            item.setDate(current);
            map.put(current, item);
            current = current.plusDays(1);
        }

        return map;
    }

    private DateRange normalizeRange(LocalDate start, LocalDate end) {
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

        return new DateRange(start, end);
    }

    private BigDecimal nvl(BigDecimal value) {
        return value != null ? value : ZERO;
    }

    private BigDecimal sum(List<CashflowItem> items, AmountExtractor extractor) {
        if (items == null || items.isEmpty()) {
            return ZERO;
        }

        return items.stream()
                .map(extractor::getAmount)
                .filter(Objects::nonNull)
                .reduce(ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);
    }

    public BigDecimal calculateTotalIncomes(List<CashflowItem> items) {
        return sum(items, CashflowItem::getTotalIncome);
    }

    public BigDecimal calculateTotalExpenses(List<CashflowItem> items) {
        return sum(items, CashflowItem::getTotalExpense);
    }

    public BigDecimal calculateNetResult(List<CashflowItem> items) {
        return sum(items, CashflowItem::getNetResult);
    }

    private interface AmountExtractor {
        BigDecimal getAmount(CashflowItem item);
    }

    private record DateRange(LocalDate start, LocalDate end) {
    }
}
