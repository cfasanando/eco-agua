package com.ecoamazonas.eco_agua.personalfinance;

import java.math.BigDecimal;
import java.time.LocalDate;

public class PersonalFinanceVoluntaryPaymentForm {

    private BigDecimal amount = BigDecimal.ZERO;
    private LocalDate dueDate = LocalDate.now();
    private PersonalFinanceCurrency currency = PersonalFinanceCurrency.PEN;
    private PersonalFinancePriority priority = PersonalFinancePriority.MEDIUM;
    private String notes;

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public LocalDate getDueDate() {
        return dueDate;
    }

    public void setDueDate(LocalDate dueDate) {
        this.dueDate = dueDate;
    }

    public PersonalFinanceCurrency getCurrency() {
        return currency;
    }

    public void setCurrency(PersonalFinanceCurrency currency) {
        this.currency = currency;
    }

    public PersonalFinancePriority getPriority() {
        return priority;
    }

    public void setPriority(PersonalFinancePriority priority) {
        this.priority = priority;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }
}
