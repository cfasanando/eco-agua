package com.ecoamazonas.eco_agua.personalfinance;

import java.math.BigDecimal;
import java.time.LocalDate;

public class PersonalFinancePaymentForm {
    private LocalDate paymentDate = LocalDate.now();
    private BigDecimal totalAmount = BigDecimal.ZERO;
    private BigDecimal principalAmount = BigDecimal.ZERO;
    private BigDecimal interestAmount = BigDecimal.ZERO;
    private BigDecimal insuranceAmount = BigDecimal.ZERO;
    private BigDecimal feeAmount = BigDecimal.ZERO;
    private BigDecimal penaltyAmount = BigDecimal.ZERO;
    private BigDecimal otherAmount = BigDecimal.ZERO;
    private PersonalFinancePaymentMethod paymentMethod = PersonalFinancePaymentMethod.OTHER;
    private String operationNumber;
    private String recipient;
    private String notes;

    public BigDecimal componentTotal() {
        return safe(principalAmount)
                .add(safe(interestAmount))
                .add(safe(insuranceAmount))
                .add(safe(feeAmount))
                .add(safe(penaltyAmount))
                .add(safe(otherAmount));
    }

    private BigDecimal safe(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    public LocalDate getPaymentDate() { return paymentDate; }
    public void setPaymentDate(LocalDate paymentDate) { this.paymentDate = paymentDate; }
    public BigDecimal getTotalAmount() { return totalAmount; }
    public void setTotalAmount(BigDecimal totalAmount) { this.totalAmount = totalAmount; }
    public BigDecimal getPrincipalAmount() { return principalAmount; }
    public void setPrincipalAmount(BigDecimal principalAmount) { this.principalAmount = principalAmount; }
    public BigDecimal getInterestAmount() { return interestAmount; }
    public void setInterestAmount(BigDecimal interestAmount) { this.interestAmount = interestAmount; }
    public BigDecimal getInsuranceAmount() { return insuranceAmount; }
    public void setInsuranceAmount(BigDecimal insuranceAmount) { this.insuranceAmount = insuranceAmount; }
    public BigDecimal getFeeAmount() { return feeAmount; }
    public void setFeeAmount(BigDecimal feeAmount) { this.feeAmount = feeAmount; }
    public BigDecimal getPenaltyAmount() { return penaltyAmount; }
    public void setPenaltyAmount(BigDecimal penaltyAmount) { this.penaltyAmount = penaltyAmount; }
    public BigDecimal getOtherAmount() { return otherAmount; }
    public void setOtherAmount(BigDecimal otherAmount) { this.otherAmount = otherAmount; }
    public PersonalFinancePaymentMethod getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(PersonalFinancePaymentMethod paymentMethod) { this.paymentMethod = paymentMethod; }
    public String getOperationNumber() { return operationNumber; }
    public void setOperationNumber(String operationNumber) { this.operationNumber = operationNumber; }
    public String getRecipient() { return recipient; }
    public void setRecipient(String recipient) { this.recipient = recipient; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
}
