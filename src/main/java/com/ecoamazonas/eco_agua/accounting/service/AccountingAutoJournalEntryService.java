package com.ecoamazonas.eco_agua.accounting.service;

import com.ecoamazonas.eco_agua.accounting.*;
import com.ecoamazonas.eco_agua.accounting.repository.AccountingJournalEntryRepository;
import com.ecoamazonas.eco_agua.accounting.repository.AccountingRuleTemplateRepository;
import com.ecoamazonas.eco_agua.expense.Expense;
import com.ecoamazonas.eco_agua.expense.ExpensePayment;
import com.ecoamazonas.eco_agua.income.OtherIncome;
import com.ecoamazonas.eco_agua.order.OrderStatus;
import com.ecoamazonas.eco_agua.order.SaleOrder;
import com.ecoamazonas.eco_agua.order.SaleOrderPayment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class AccountingAutoJournalEntryService {

    private static final Logger log = LoggerFactory.getLogger(AccountingAutoJournalEntryService.class);
    private static final BigDecimal ZERO = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);

    private final AccountingRuleTemplateRepository templateRepository;
    private final AccountingJournalEntryRepository journalEntryRepository;

    public AccountingAutoJournalEntryService(
            AccountingRuleTemplateRepository templateRepository,
            AccountingJournalEntryRepository journalEntryRepository
    ) {
        this.templateRepository = templateRepository;
        this.journalEntryRepository = journalEntryRepository;
    }

    @Transactional
    public void generateForSaleOrder(SaleOrder order) {
        if (order == null || order.getId() == null || order.getStatus() == null) {
            return;
        }

        AccountingAutomationEvent event = resolveSaleEvent(order.getStatus());
        if (event == null) {
            return;
        }

        AmountContext context = AmountContext.fromSaleOrder(order);
        String referenceCode = "SALE_ORDER-" + order.getId();
        String description = "Automatic draft for order #" + resolveOrderNumber(order);

        generateDraftEntry(
                event,
                AccountingJournalSourceType.SALE,
                order.getId(),
                referenceCode,
                order.getOrderDate(),
                description,
                context
        );
    }

    @Transactional
    public void cancelForSaleOrder(SaleOrder order) {
        if (order == null || order.getId() == null) {
            return;
        }
        cancelDraftEntries(AccountingAutomationEvent.SALE_PAID, order.getId());
        cancelDraftEntries(AccountingAutomationEvent.SALE_CREDIT, order.getId());
    }

    @Transactional
    public void generateForCreditCollection(SaleOrderPayment payment) {
        if (payment == null || payment.getId() == null) {
            return;
        }

        AmountContext context = AmountContext.fromPayment(payment.getAmount());
        String referenceCode = "SALE_PAYMENT-" + payment.getId();
        String description = "Automatic draft for customer payment #" + payment.getId();

        generateDraftEntry(
                AccountingAutomationEvent.CREDIT_COLLECTION,
                AccountingJournalSourceType.PAYMENT,
                payment.getId(),
                referenceCode,
                payment.getPaymentDate(),
                description,
                context
        );
    }

    @Transactional
    public void generateForExpense(Expense expense, boolean stockPurchase) {
        if (expense == null || expense.getId() == null) {
            return;
        }

        AccountingAutomationEvent event = stockPurchase
                ? AccountingAutomationEvent.STOCK_PURCHASE
                : (expense.isDebt() ? AccountingAutomationEvent.EXPENSE_CREDIT : AccountingAutomationEvent.EXPENSE_PAID);

        AmountContext context = AmountContext.fromExpense(expense);
        String referenceCode = "EXPENSE-" + expense.getId();
        String description = stockPurchase
                ? "Automatic draft for stock purchase expense #" + expense.getId()
                : "Automatic draft for expense #" + expense.getId();

        generateDraftEntry(
                event,
                stockPurchase ? AccountingJournalSourceType.PURCHASE : AccountingJournalSourceType.PURCHASE,
                expense.getId(),
                referenceCode,
                expense.getExpenseDate(),
                description,
                context
        );
    }

    @Transactional
    public void regenerateForExpense(Expense expense, boolean stockPurchase) {
        if (expense == null || expense.getId() == null) {
            return;
        }
        AccountingAutomationEvent event = stockPurchase
                ? AccountingAutomationEvent.STOCK_PURCHASE
                : (expense.isDebt() ? AccountingAutomationEvent.EXPENSE_CREDIT : AccountingAutomationEvent.EXPENSE_PAID);
        cancelDraftEntries(event, expense.getId());
        generateForExpense(expense, stockPurchase);
    }

    @Transactional
    public void generateForSupplierPayment(ExpensePayment payment) {
        if (payment == null || payment.getId() == null) {
            return;
        }

        AmountContext context = AmountContext.fromPayment(payment.getAmount());
        String referenceCode = "EXPENSE_PAYMENT-" + payment.getId();
        String description = "Automatic draft for supplier payment #" + payment.getId();

        generateDraftEntry(
                AccountingAutomationEvent.SUPPLIER_PAYMENT,
                AccountingJournalSourceType.PAYMENT,
                payment.getId(),
                referenceCode,
                payment.getPaymentDate(),
                description,
                context
        );
    }

    @Transactional
    public void generateForOtherIncome(OtherIncome income) {
        if (income == null || income.getId() == null) {
            return;
        }

        AmountContext context = AmountContext.fromOtherIncome(income);
        String referenceCode = "OTHER_INCOME-" + income.getId();
        String description = "Automatic draft for other income #" + income.getId();

        generateDraftEntry(
                AccountingAutomationEvent.OTHER_INCOME,
                AccountingJournalSourceType.INCOME,
                income.getId(),
                referenceCode,
                income.getIncomeDate(),
                description,
                context
        );
    }

    @Transactional
    public void cancelDraftEntries(AccountingAutomationEvent event, Long referenceId) {
        if (event == null || referenceId == null) {
            return;
        }

        List<AccountingJournalEntry> draftEntries = journalEntryRepository
                .findBySourceEventAndSourceReferenceIdAndStatus(event, referenceId, AccountingJournalEntryStatus.DRAFT);

        for (AccountingJournalEntry entry : draftEntries) {
            entry.setStatus(AccountingJournalEntryStatus.CANCELLED);
        }

        if (!draftEntries.isEmpty()) {
            journalEntryRepository.saveAll(draftEntries);
        }
    }

    private void generateDraftEntry(
            AccountingAutomationEvent event,
            AccountingJournalSourceType sourceType,
            Long referenceId,
            String referenceCode,
            LocalDate entryDate,
            String description,
            AmountContext context
    ) {
        try {
            if (event == null || referenceId == null || context == null) {
                return;
            }

            boolean alreadyGenerated = journalEntryRepository
                    .existsBySourceEventAndSourceReferenceIdAndStatusNot(
                            event,
                            referenceId,
                            AccountingJournalEntryStatus.CANCELLED
                    );

            if (alreadyGenerated) {
                return;
            }

            Optional<AccountingRuleTemplate> templateOptional = templateRepository.findByEventType(event)
                    .filter(AccountingRuleTemplate::isActive)
                    .filter(AccountingRuleTemplate::isGenerateDraft);

            if (templateOptional.isEmpty()) {
                return;
            }

            AccountingRuleTemplate template = templateOptional.get();
            AccountingJournalEntry entry = new AccountingJournalEntry();
            entry.setEntryDate(entryDate != null ? entryDate : LocalDate.now());
            entry.setDescription(description != null ? description : template.getName());
            entry.setSourceType(sourceType != null ? sourceType : AccountingJournalSourceType.ADJUSTMENT);
            entry.setSourceEvent(event);
            entry.setSourceReferenceId(referenceId);
            entry.setSourceReferenceCode(normalizeReferenceCode(referenceCode));
            entry.setStatus(AccountingJournalEntryStatus.DRAFT);
            entry.setLines(buildJournalLines(template, context));

            validateGeneratedEntry(entry, event, referenceId);
            journalEntryRepository.save(entry);
        } catch (Exception ex) {
            log.warn(
                    "Automatic journal entry was not generated. event={}, referenceId={}, reason={}",
                    event,
                    referenceId,
                    ex.getMessage()
            );
        }
    }

    private List<AccountingJournalLine> buildJournalLines(AccountingRuleTemplate template, AmountContext context) {
        List<AccountingJournalLine> lines = new ArrayList<>();

        for (AccountingRuleTemplateLine templateLine : template.getLines()) {
            BigDecimal amount = resolveAmount(templateLine, context);
            if (amount.compareTo(ZERO) <= 0) {
                continue;
            }

            AccountingJournalLine line = new AccountingJournalLine();
            line.setLineOrder(lines.size() + 1);
            line.setAccount(templateLine.getAccount());
            line.setDescription(templateLine.getDescription());

            if (templateLine.getLineSide() == AccountingRuleLineSide.DEBIT) {
                line.setDebitAmount(amount);
                line.setCreditAmount(ZERO);
            } else {
                line.setDebitAmount(ZERO);
                line.setCreditAmount(amount);
            }

            lines.add(line);
        }

        return lines;
    }

    private BigDecimal resolveAmount(AccountingRuleTemplateLine templateLine, AmountContext context) {
        if (templateLine == null || templateLine.getAmountBase() == null) {
            return ZERO;
        }

        BigDecimal amount = switch (templateLine.getAmountBase()) {
            case TOTAL -> context.total();
            case NET_BASE -> context.netBase();
            case TAX_IGV -> context.taxIgv();
            case PAID_AMOUNT -> context.paidAmount();
            case PENDING_AMOUNT -> context.pendingAmount();
            case STOCK_VALUE -> context.stockValue();
            case FIXED_AMOUNT -> templateLine.getFixedAmount();
        };

        return normalizeAmount(amount);
    }

    private void validateGeneratedEntry(
            AccountingJournalEntry entry,
            AccountingAutomationEvent event,
            Long referenceId
    ) {
        if (entry.getLines().size() < 2) {
            throw new IllegalArgumentException("Generated entry has less than two lines.");
        }

        if (entry.getTotalDebit().compareTo(entry.getTotalCredit()) != 0) {
            throw new IllegalArgumentException(
                    "Generated entry is not balanced. debit="
                            + entry.getTotalDebit()
                            + ", credit="
                            + entry.getTotalCredit()
                            + ", event="
                            + event
                            + ", referenceId="
                            + referenceId
            );
        }
    }

    private AccountingAutomationEvent resolveSaleEvent(OrderStatus status) {
        if (status == OrderStatus.PAID) {
            return AccountingAutomationEvent.SALE_PAID;
        }
        if (status == OrderStatus.CREDIT) {
            return AccountingAutomationEvent.SALE_CREDIT;
        }
        return null;
    }

    private String resolveOrderNumber(SaleOrder order) {
        if (order == null) {
            return "-";
        }
        if (order.getOrderNumber() != null) {
            return order.getOrderNumber().toString();
        }
        return order.getId() != null ? order.getId().toString() : "-";
    }

    private String normalizeReferenceCode(String referenceCode) {
        if (referenceCode == null) {
            return null;
        }
        String trimmed = referenceCode.trim();
        if (trimmed.isEmpty()) {
            return null;
        }
        return trimmed.length() > 80 ? trimmed.substring(0, 80) : trimmed;
    }

    private static BigDecimal normalizeAmount(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) < 0) {
            return ZERO;
        }
        return amount.setScale(2, RoundingMode.HALF_UP);
    }

    private record AmountContext(
            BigDecimal total,
            BigDecimal netBase,
            BigDecimal taxIgv,
            BigDecimal paidAmount,
            BigDecimal pendingAmount,
            BigDecimal stockValue
    ) {
        private static AmountContext fromSaleOrder(SaleOrder order) {
            BigDecimal total = normalizeAmount(order.getTotalAmount());
            BigDecimal netBase = normalizeAmount(order.getTaxBase());
            BigDecimal taxIgv = normalizeAmount(order.getTaxIgv());
            if (netBase.compareTo(ZERO) <= 0 && taxIgv.compareTo(ZERO) <= 0) {
                netBase = total;
            }
            return new AmountContext(
                    total,
                    netBase,
                    taxIgv,
                    normalizeAmount(order.getPaidAmount()),
                    normalizeAmount(order.getPendingAmount()),
                    netBase.compareTo(ZERO) > 0 ? netBase : total
            );
        }

        private static AmountContext fromExpense(Expense expense) {
            BigDecimal total = normalizeAmount(expense.getAmount());
            BigDecimal netBase = normalizeAmount(expense.getTaxBase());
            BigDecimal taxIgv = normalizeAmount(expense.getTaxIgv());
            if (netBase.compareTo(ZERO) <= 0 && taxIgv.compareTo(ZERO) <= 0) {
                netBase = total;
            }
            return new AmountContext(
                    total,
                    netBase,
                    taxIgv,
                    normalizeAmount(expense.getPaidAmount()),
                    normalizeAmount(expense.getBalance()),
                    netBase.compareTo(ZERO) > 0 ? netBase : total
            );
        }

        private static AmountContext fromOtherIncome(OtherIncome income) {
            BigDecimal total = normalizeAmount(income.getAmount());
            BigDecimal netBase = normalizeAmount(income.getTaxBase());
            BigDecimal taxIgv = normalizeAmount(income.getTaxIgv());
            if (netBase.compareTo(ZERO) <= 0 && taxIgv.compareTo(ZERO) <= 0) {
                netBase = total;
            }
            return new AmountContext(
                    total,
                    netBase,
                    taxIgv,
                    total,
                    ZERO,
                    netBase.compareTo(ZERO) > 0 ? netBase : total
            );
        }

        private static AmountContext fromPayment(BigDecimal amount) {
            BigDecimal normalizedAmount = normalizeAmount(amount);
            return new AmountContext(
                    normalizedAmount,
                    normalizedAmount,
                    ZERO,
                    normalizedAmount,
                    ZERO,
                    normalizedAmount
            );
        }
    }
}
