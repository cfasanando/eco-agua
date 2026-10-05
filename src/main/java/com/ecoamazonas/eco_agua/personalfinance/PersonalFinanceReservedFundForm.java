package com.ecoamazonas.eco_agua.personalfinance;

import java.math.BigDecimal;
import java.time.LocalDate;

public class PersonalFinanceReservedFundForm {
    private PersonalFinanceReservedFundTargetType targetType = PersonalFinanceReservedFundTargetType.OBLIGATION;
    private Long obligationId;
    private Long debtId;
    private Long negotiationId;
    private String title;
    private BigDecimal amount = BigDecimal.ZERO;
    private BigDecimal targetAmount = BigDecimal.ZERO;
    private PersonalFinanceCurrency currency = PersonalFinanceCurrency.PEN;
    private LocalDate targetDate;
    private String notes;

    public PersonalFinanceReservedFundTargetType getTargetType() { return targetType; }
    public void setTargetType(PersonalFinanceReservedFundTargetType targetType) { this.targetType = targetType; }
    public Long getObligationId() { return obligationId; }
    public void setObligationId(Long obligationId) { this.obligationId = obligationId; }
    public Long getDebtId() { return debtId; }
    public void setDebtId(Long debtId) { this.debtId = debtId; }
    public Long getNegotiationId() { return negotiationId; }
    public void setNegotiationId(Long negotiationId) { this.negotiationId = negotiationId; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public BigDecimal getTargetAmount() { return targetAmount; }
    public void setTargetAmount(BigDecimal targetAmount) { this.targetAmount = targetAmount; }
    public PersonalFinanceCurrency getCurrency() { return currency; }
    public void setCurrency(PersonalFinanceCurrency currency) { this.currency = currency; }
    public LocalDate getTargetDate() { return targetDate; }
    public void setTargetDate(LocalDate targetDate) { this.targetDate = targetDate; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
}
