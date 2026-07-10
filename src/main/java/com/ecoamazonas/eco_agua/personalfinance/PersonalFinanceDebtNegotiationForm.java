package com.ecoamazonas.eco_agua.personalfinance;

import java.math.BigDecimal;
import java.time.LocalDate;

public class PersonalFinanceDebtNegotiationForm {
    private Long id;
    private Long debtId;
    private LocalDate conversationDate = LocalDate.now();
    private PersonalFinanceNegotiationChannel channel = PersonalFinanceNegotiationChannel.WHATSAPP;
    private PersonalFinanceNegotiationEntryStatus status = PersonalFinanceNegotiationEntryStatus.DRAFT;
    private String contactPerson;
    private BigDecimal creditorRequestedAmount = BigDecimal.ZERO;
    private BigDecimal affordableAmount = BigDecimal.ZERO;
    private BigDecimal initialPaymentAmount = BigDecimal.ZERO;
    private Integer installmentCount;
    private BigDecimal proposedInstallmentAmount = BigDecimal.ZERO;
    private BigDecimal proposedMonthlyRate = BigDecimal.ZERO;
    private LocalDate firstPaymentDate;
    private LocalDate responseDeadline;
    private LocalDate nextActionDate;
    private String nextAction;
    private String privateNotes;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getDebtId() { return debtId; }
    public void setDebtId(Long debtId) { this.debtId = debtId; }
    public LocalDate getConversationDate() { return conversationDate; }
    public void setConversationDate(LocalDate conversationDate) { this.conversationDate = conversationDate; }
    public PersonalFinanceNegotiationChannel getChannel() { return channel; }
    public void setChannel(PersonalFinanceNegotiationChannel channel) { this.channel = channel; }
    public PersonalFinanceNegotiationEntryStatus getStatus() { return status; }
    public void setStatus(PersonalFinanceNegotiationEntryStatus status) { this.status = status; }
    public String getContactPerson() { return contactPerson; }
    public void setContactPerson(String contactPerson) { this.contactPerson = contactPerson; }
    public BigDecimal getCreditorRequestedAmount() { return creditorRequestedAmount; }
    public void setCreditorRequestedAmount(BigDecimal creditorRequestedAmount) { this.creditorRequestedAmount = creditorRequestedAmount; }
    public BigDecimal getAffordableAmount() { return affordableAmount; }
    public void setAffordableAmount(BigDecimal affordableAmount) { this.affordableAmount = affordableAmount; }
    public BigDecimal getInitialPaymentAmount() { return initialPaymentAmount; }
    public void setInitialPaymentAmount(BigDecimal initialPaymentAmount) { this.initialPaymentAmount = initialPaymentAmount; }
    public Integer getInstallmentCount() { return installmentCount; }
    public void setInstallmentCount(Integer installmentCount) { this.installmentCount = installmentCount; }
    public BigDecimal getProposedInstallmentAmount() { return proposedInstallmentAmount; }
    public void setProposedInstallmentAmount(BigDecimal proposedInstallmentAmount) { this.proposedInstallmentAmount = proposedInstallmentAmount; }
    public BigDecimal getProposedMonthlyRate() { return proposedMonthlyRate; }
    public void setProposedMonthlyRate(BigDecimal proposedMonthlyRate) { this.proposedMonthlyRate = proposedMonthlyRate; }
    public LocalDate getFirstPaymentDate() { return firstPaymentDate; }
    public void setFirstPaymentDate(LocalDate firstPaymentDate) { this.firstPaymentDate = firstPaymentDate; }
    public LocalDate getResponseDeadline() { return responseDeadline; }
    public void setResponseDeadline(LocalDate responseDeadline) { this.responseDeadline = responseDeadline; }
    public LocalDate getNextActionDate() { return nextActionDate; }
    public void setNextActionDate(LocalDate nextActionDate) { this.nextActionDate = nextActionDate; }
    public String getNextAction() { return nextAction; }
    public void setNextAction(String nextAction) { this.nextAction = nextAction; }
    public String getPrivateNotes() { return privateNotes; }
    public void setPrivateNotes(String privateNotes) { this.privateNotes = privateNotes; }
}
