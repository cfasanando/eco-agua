package com.ecoamazonas.eco_agua.expense;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class PersonnelExpenseCalculation {

    private final boolean autoFilled;
    private final String employeeName;
    private final String jobPositionName;
    private final String paymentMode;
    private final String paymentModeLabel;
    private final String salaryPeriod;
    private final String salaryPeriodLabel;
    private final BigDecimal totalSales;
    private final BigDecimal fixedComponent;
    private final BigDecimal commissionComponent;
    private final BigDecimal commissionRate;
    private final BigDecimal suggestedAmount;
    private final String message;

    public PersonnelExpenseCalculation(
            boolean autoFilled,
            String employeeName,
            String jobPositionName,
            String paymentMode,
            String paymentModeLabel,
            String salaryPeriod,
            String salaryPeriodLabel,
            BigDecimal totalSales,
            BigDecimal fixedComponent,
            BigDecimal commissionComponent,
            BigDecimal commissionRate,
            BigDecimal suggestedAmount,
            String message
    ) {
        this.autoFilled = autoFilled;
        this.employeeName = employeeName;
        this.jobPositionName = jobPositionName;
        this.paymentMode = paymentMode;
        this.paymentModeLabel = paymentModeLabel;
        this.salaryPeriod = salaryPeriod;
        this.salaryPeriodLabel = salaryPeriodLabel;
        this.totalSales = normalizeMoney(totalSales);
        this.fixedComponent = normalizeMoney(fixedComponent);
        this.commissionComponent = normalizeMoney(commissionComponent);
        this.commissionRate = normalizeMoney(commissionRate);
        this.suggestedAmount = normalizeMoney(suggestedAmount);
        this.message = message;
    }

    public static PersonnelExpenseCalculation empty(String employeeName, String message) {
        return new PersonnelExpenseCalculation(
                false,
                employeeName,
                null,
                null,
                null,
                null,
                null,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                message
        );
    }

    private static BigDecimal normalizeMoney(BigDecimal value) {
        if (value == null) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }

        return value.setScale(2, RoundingMode.HALF_UP);
    }

    public boolean isAutoFilled() {
        return autoFilled;
    }

    public String getEmployeeName() {
        return employeeName;
    }

    public String getJobPositionName() {
        return jobPositionName;
    }

    public String getPaymentMode() {
        return paymentMode;
    }

    public String getPaymentModeLabel() {
        return paymentModeLabel;
    }

    public String getSalaryPeriod() {
        return salaryPeriod;
    }

    public String getSalaryPeriodLabel() {
        return salaryPeriodLabel;
    }

    public BigDecimal getTotalSales() {
        return totalSales;
    }

    public BigDecimal getFixedComponent() {
        return fixedComponent;
    }

    public BigDecimal getCommissionComponent() {
        return commissionComponent;
    }

    public BigDecimal getCommissionRate() {
        return commissionRate;
    }

    public BigDecimal getSuggestedAmount() {
        return suggestedAmount;
    }

    public String getMessage() {
        return message;
    }
}
