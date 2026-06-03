package com.ecoamazonas.eco_agua.accounting;

import jakarta.persistence.*;

import java.math.BigDecimal;

@Entity
@Table(name = "accounting_rule_template_line")
public class AccountingRuleTemplateLine {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "template_id", nullable = false)
    private AccountingRuleTemplate template;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id", nullable = false)
    private AccountingAccount account;

    @Column(name = "line_order", nullable = false)
    private int lineOrder;

    @Enumerated(EnumType.STRING)
    @Column(name = "line_side", nullable = false, length = 10)
    private AccountingRuleLineSide lineSide;

    @Enumerated(EnumType.STRING)
    @Column(name = "amount_base", nullable = false, length = 30)
    private AccountingRuleAmountBase amountBase;

    @Column(name = "fixed_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal fixedAmount = BigDecimal.ZERO;

    @Column(length = 255)
    private String description;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public AccountingRuleTemplate getTemplate() {
        return template;
    }

    public void setTemplate(AccountingRuleTemplate template) {
        this.template = template;
    }

    public AccountingAccount getAccount() {
        return account;
    }

    public void setAccount(AccountingAccount account) {
        this.account = account;
    }

    public int getLineOrder() {
        return lineOrder;
    }

    public void setLineOrder(int lineOrder) {
        this.lineOrder = lineOrder;
    }

    public AccountingRuleLineSide getLineSide() {
        return lineSide;
    }

    public void setLineSide(AccountingRuleLineSide lineSide) {
        this.lineSide = lineSide;
    }

    public AccountingRuleAmountBase getAmountBase() {
        return amountBase;
    }

    public void setAmountBase(AccountingRuleAmountBase amountBase) {
        this.amountBase = amountBase;
    }

    public BigDecimal getFixedAmount() {
        return fixedAmount == null ? BigDecimal.ZERO : fixedAmount;
    }

    public void setFixedAmount(BigDecimal fixedAmount) {
        this.fixedAmount = fixedAmount == null ? BigDecimal.ZERO : fixedAmount;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
