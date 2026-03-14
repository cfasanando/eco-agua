package com.ecoamazonas.eco_agua.expense;

import com.ecoamazonas.eco_agua.order.OrderStatus;
import com.ecoamazonas.eco_agua.order.SaleOrder;
import com.ecoamazonas.eco_agua.order.SaleOrderRepository;
import com.ecoamazonas.eco_agua.user.Employee;
import com.ecoamazonas.eco_agua.user.JobPosition;
import com.ecoamazonas.eco_agua.user.PaymentMode;
import com.ecoamazonas.eco_agua.user.SalaryPeriod;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;

@Service
public class PersonnelExpenseCalculatorService {

    private final SaleOrderRepository saleOrderRepository;

    public PersonnelExpenseCalculatorService(SaleOrderRepository saleOrderRepository) {
        this.saleOrderRepository = saleOrderRepository;
    }

    @Transactional(readOnly = true)
    public PersonnelExpenseCalculation calculateSalaryExpense(Employee employee, LocalDate expenseDate) {
        String employeeName = buildEmployeeDisplayName(employee);

        if (employee == null) {
            return PersonnelExpenseCalculation.empty(employeeName, "Select an employee first.");
        }

        JobPosition position = employee.getJobPosition();
        if (position == null) {
            return PersonnelExpenseCalculation.empty(
                    employeeName,
                    "The selected employee has no job position configured."
            );
        }

        LocalDate effectiveDate = (expenseDate != null ? expenseDate : LocalDate.now());
        BigDecimal totalSales = getCommissionableSalesTotalForDate(effectiveDate);
        BigDecimal fixedComponent = resolveDailyFixedAmount(position);
        BigDecimal commissionRate = normalizeMoney(position.getCommissionRate());
        BigDecimal commissionComponent = totalSales
                .multiply(commissionRate)
                .divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);

        PaymentMode paymentMode = position.getPaymentMode() != null
                ? position.getPaymentMode()
                : PaymentMode.FIXED;

        BigDecimal suggestedAmount;
        String explanation;

        switch (paymentMode) {
            case COMMISSION:
                suggestedAmount = commissionComponent;
                explanation = "Commission-only mode. Amount calculated as "
                        + commissionRate + "% of paid and credit sales for the selected date.";
                break;
            case MIXED:
                suggestedAmount = fixedComponent.add(commissionComponent);
                explanation = "Mixed mode. Amount calculated as fixed daily amount plus "
                        + commissionRate + "% of paid and credit sales for the selected date.";
                break;
            case FIXED:
            default:
                suggestedAmount = fixedComponent;
                explanation = "Fixed mode. Amount calculated from the job position salary settings.";
                break;
        }

        return new PersonnelExpenseCalculation(
                true,
                employeeName,
                position.getName(),
                paymentMode.name(),
                position.getPaymentModeLabel(),
                position.getSalaryPeriod() != null ? position.getSalaryPeriod().name() : null,
                position.getSalaryPeriod() != null ? position.getSalaryPeriod().getLabel() : null,
                totalSales,
                fixedComponent,
                commissionComponent,
                commissionRate,
                suggestedAmount,
                explanation
        );
    }

    @Transactional(readOnly = true)
    public BigDecimal getCommissionableSalesTotalForDate(LocalDate expenseDate) {
        LocalDate effectiveDate = (expenseDate != null ? expenseDate : LocalDate.now());

        BigDecimal paidTotal = sumOrderTotals(
                saleOrderRepository.findByOrderDateAndStatus(effectiveDate, OrderStatus.PAID)
        );
        BigDecimal creditTotal = sumOrderTotals(
                saleOrderRepository.findByOrderDateAndStatus(effectiveDate, OrderStatus.CREDIT)
        );

        return normalizeMoney(paidTotal.add(creditTotal));
    }

    private BigDecimal sumOrderTotals(List<SaleOrder> orders) {
        if (orders == null || orders.isEmpty()) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }

        return normalizeMoney(
                orders.stream()
                        .map(SaleOrder::getTotalAmount)
                        .filter(amount -> amount != null)
                        .reduce(BigDecimal.ZERO, BigDecimal::add)
        );
    }

    private BigDecimal resolveDailyFixedAmount(JobPosition position) {
        if (position == null) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }

        BigDecimal reference = normalizeMoney(
                position.getSalaryAmount() != null && position.getSalaryAmount().signum() > 0
                        ? position.getSalaryAmount()
                        : position.getBaseSalary()
        );

        SalaryPeriod period = position.getSalaryPeriod() != null
                ? position.getSalaryPeriod()
                : SalaryPeriod.DAILY;

        BigDecimal dailyAmount = switch (period) {
            case DAILY -> reference;
            case WEEKLY -> reference.divide(new BigDecimal("7"), 2, RoundingMode.HALF_UP);
            case BIWEEKLY -> reference.divide(new BigDecimal("15"), 2, RoundingMode.HALF_UP);
            case MONTHLY -> reference.divide(new BigDecimal("30"), 2, RoundingMode.HALF_UP);
            case HOURLY -> reference;
        };

        return normalizeMoney(dailyAmount);
    }

    private BigDecimal normalizeMoney(BigDecimal value) {
        if (value == null) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }

        return value.setScale(2, RoundingMode.HALF_UP);
    }

    private String buildEmployeeDisplayName(Employee employee) {
        if (employee == null) {
            return "";
        }

        String firstName = employee.getFirstName() != null ? employee.getFirstName().trim() : "";
        String lastName = employee.getLastName() != null ? employee.getLastName().trim() : "";

        return (firstName + " " + lastName).trim();
    }
}
