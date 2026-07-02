package com.ecoamazonas.eco_agua.personalfinance;

import com.ecoamazonas.eco_agua.user.UserAccount;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
public class PersonalFinanceService {

    private final PersonalFinanceDebtRepository debtRepository;
    private final PersonalFinanceFixedExpenseRepository fixedExpenseRepository;
    private final PersonalFinanceIncomeSourceRepository incomeSourceRepository;
    private final PersonalFinanceIncomeEventRepository incomeEventRepository;
    private final PersonalFinancePaymentObligationRepository paymentObligationRepository;
    private final PersonalFinanceCurrentUserService currentUserService;

    public PersonalFinanceService(
            PersonalFinanceDebtRepository debtRepository,
            PersonalFinanceFixedExpenseRepository fixedExpenseRepository,
            PersonalFinanceIncomeSourceRepository incomeSourceRepository,
            PersonalFinanceIncomeEventRepository incomeEventRepository,
            PersonalFinancePaymentObligationRepository paymentObligationRepository,
            PersonalFinanceCurrentUserService currentUserService
    ) {
        this.debtRepository = debtRepository;
        this.fixedExpenseRepository = fixedExpenseRepository;
        this.incomeSourceRepository = incomeSourceRepository;
        this.incomeEventRepository = incomeEventRepository;
        this.paymentObligationRepository = paymentObligationRepository;
        this.currentUserService = currentUserService;
    }

    @Transactional(readOnly = true)
    public PersonalFinanceDashboard dashboard(YearMonth yearMonth) {
        PersonalFinanceMonthlyPlan plan = monthlyPlan(yearMonth);
        return new PersonalFinanceDashboard(
                plan.expectedIncome(),
                plan.receivedIncome(),
                plan.basicLivingTotal(),
                plan.debtTotal().add(plan.otherTotal()),
                plan.projectedBalance(),
                debtsForMonthlyPlan(currentUserService.currentUser()).size(),
                fixedExpenseRepository.findByUserAndActiveTrueOrderByDueDayAscNameAsc(currentUserService.currentUser()).size(),
                plan.incomes().size(),
                yearMonth.getYear(),
                yearMonth.getMonthValue()
        );
    }

    @Transactional(readOnly = true)
    public PersonalFinanceMonthlyPlan monthlyPlan(YearMonth yearMonth) {
        UserAccount user = currentUserService.currentUser();
        LocalDate start = yearMonth.atDay(1);
        LocalDate end = yearMonth.atEndOfMonth();

        List<PersonalFinanceIncomeEvent> incomes = incomeEventRepository.findByUserAndExpectedDateBetweenOrderByExpectedDateAscIdAsc(user, start, end);
        List<PersonalFinanceFixedExpense> fixedExpenses = fixedExpenseRepository.findByUserAndActiveTrueOrderByDueDayAscNameAsc(user);
        List<PersonalFinanceDebt> debts = debtsForMonthlyPlan(user);
        List<PersonalFinancePaymentObligation> obligations = paymentObligationRepository.findByUserAndDueDateBetweenOrderByDueDateAscPriorityAscIdAsc(user, start, end);

        List<PersonalFinanceMonthlyPlanItem> basicItems = new ArrayList<>();
        List<PersonalFinanceMonthlyPlanItem> debtItems = new ArrayList<>();
        List<PersonalFinanceMonthlyPlanItem> otherItems = new ArrayList<>();

        for (PersonalFinanceFixedExpense expense : fixedExpenses) {
            PersonalFinanceObligationGroup group = expense.getCategory() == PersonalFinanceExpenseCategory.STUDY
                    ? PersonalFinanceObligationGroup.STUDY
                    : PersonalFinanceObligationGroup.BASIC_LIVING;
            PersonalFinancePriority priority = expense.isMandatory() ? PersonalFinancePriority.CRITICAL : PersonalFinancePriority.MEDIUM;
            basicItems.add(new PersonalFinanceMonthlyPlanItem(
                    null,
                    expense.getName(),
                    expense.getCategory().getLabel(),
                    PersonalFinanceObligationSourceType.FIXED_EXPENSE,
                    group,
                    expense.getCurrency(),
                    safe(expense.getAmount()),
                    BigDecimal.ZERO,
                    safe(expense.getAmount()),
                    dateFromDueDay(yearMonth, expense.getDueDay()),
                    PersonalFinanceObligationStatus.PENDING,
                    priority,
                    expense.getNotes(),
                    true
            ));
        }

        for (PersonalFinanceDebt debt : debts) {
            debtItems.add(new PersonalFinanceMonthlyPlanItem(
                    null,
                    debt.getName(),
                    debt.getDebtType().getLabel(),
                    PersonalFinanceObligationSourceType.DEBT,
                    PersonalFinanceObligationGroup.DEBT_PAYMENT,
                    debt.getCurrency(),
                    debt.monthlyPressure(),
                    BigDecimal.ZERO,
                    debt.monthlyPressure(),
                    dateFromDueDay(yearMonth, debt.getDueDay()),
                    obligationStatusFromDebt(debt),
                    defaultEnum(debt.getPriority(), PersonalFinancePriority.MEDIUM),
                    debtPlanNotes(debt),
                    true
            ));
        }

        for (PersonalFinancePaymentObligation obligation : obligations) {
            PersonalFinanceMonthlyPlanItem item = itemFromObligation(obligation);
            if (obligation.getGroup() == PersonalFinanceObligationGroup.BASIC_LIVING || obligation.getGroup() == PersonalFinanceObligationGroup.STUDY) {
                basicItems.add(item);
            } else if (obligation.getGroup() == PersonalFinanceObligationGroup.DEBT_PAYMENT) {
                debtItems.add(item);
            } else {
                otherItems.add(item);
            }
        }

        sortItems(basicItems);
        sortItems(debtItems);
        sortItems(otherItems);

        BigDecimal expectedIncome = incomes.stream()
                .filter(event -> event.getStatus() != PersonalFinanceIncomeStatus.CANCELLED && event.getStatus() != PersonalFinanceIncomeStatus.MISSED)
                .map(PersonalFinanceIncomeEvent::getAmount)
                .map(this::safe)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal receivedIncome = incomes.stream()
                .filter(event -> event.getStatus() == PersonalFinanceIncomeStatus.RECEIVED)
                .map(PersonalFinanceIncomeEvent::getAmount)
                .map(this::safe)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal basicTotal = totalDue(basicItems);
        BigDecimal debtTotal = totalDue(debtItems);
        BigDecimal otherTotal = totalDue(otherItems);
        BigDecimal obligationsTotal = basicTotal.add(debtTotal).add(otherTotal);
        BigDecimal paidTotal = totalPaid(basicItems).add(totalPaid(debtItems)).add(totalPaid(otherItems));
        BigDecimal pendingTotal = totalPending(basicItems).add(totalPending(debtItems)).add(totalPending(otherItems));
        BigDecimal projectedBalance = expectedIncome.subtract(obligationsTotal);
        BigDecimal cashAfterBasic = expectedIncome.subtract(basicTotal);
        BigDecimal cashAfterAll = expectedIncome.subtract(obligationsTotal);
        long overdueCount = debtItems.stream().filter(item -> item.status() == PersonalFinanceObligationStatus.OVERDUE).count()
                + basicItems.stream().filter(item -> item.status() == PersonalFinanceObligationStatus.OVERDUE).count()
                + otherItems.stream().filter(item -> item.status() == PersonalFinanceObligationStatus.OVERDUE).count();
        long highInterestDebtCount = debts.stream().filter(PersonalFinanceDebt::isHighInterest).count();

        return new PersonalFinanceMonthlyPlan(
                yearMonth,
                incomes,
                basicItems,
                debtItems,
                otherItems,
                expectedIncome,
                receivedIncome,
                basicTotal,
                debtTotal,
                otherTotal,
                obligationsTotal,
                paidTotal,
                pendingTotal,
                projectedBalance,
                cashAfterBasic,
                cashAfterAll,
                overdueCount,
                highInterestDebtCount
        );
    }

    @Transactional(readOnly = true)
    public PersonalFinancePaymentObligation paymentObligation(Long id) {
        return paymentObligationRepository.findByIdAndUser(id, currentUserService.currentUser()).orElseThrow();
    }

    @Transactional
    public void savePaymentObligation(PersonalFinancePaymentObligation obligation) {
        UserAccount user = currentUserService.currentUser();
        PersonalFinancePaymentObligation entity = obligation.getId() == null
                ? new PersonalFinancePaymentObligation()
                : paymentObligationRepository.findByIdAndUser(obligation.getId(), user).orElseThrow();
        entity.setUser(user);
        entity.setSourceType(defaultEnum(obligation.getSourceType(), PersonalFinanceObligationSourceType.MANUAL));
        entity.setSourceId(obligation.getSourceId());
        entity.setGroup(defaultEnum(obligation.getGroup(), PersonalFinanceObligationGroup.OTHER));
        entity.setTitle(clean(obligation.getTitle()));
        entity.setAmountDue(defaultAmount(obligation.getAmountDue()));
        entity.setAmountPaid(defaultAmount(obligation.getAmountPaid()));
        entity.setCurrency(defaultEnum(obligation.getCurrency(), PersonalFinanceCurrency.PEN));
        entity.setDueDate(obligation.getDueDate());
        entity.setStatus(resolveObligationStatus(defaultEnum(obligation.getStatus(), PersonalFinanceObligationStatus.PENDING), entity.getAmountDue(), entity.getAmountPaid()));
        entity.setPriority(defaultEnum(obligation.getPriority(), PersonalFinancePriority.MEDIUM));
        entity.setNotes(clean(obligation.getNotes()));
        paymentObligationRepository.save(entity);
    }

    @Transactional
    public void deletePaymentObligation(Long id) {
        paymentObligationRepository.findByIdAndUser(id, currentUserService.currentUser()).ifPresent(paymentObligationRepository::delete);
    }

    @Transactional(readOnly = true)
    public List<PersonalFinanceDebt> debts() {
        return debtRepository.findByUserOrderByStatusAscDueDayAscNameAsc(currentUserService.currentUser());
    }

    @Transactional
    public void saveDebt(PersonalFinanceDebt debt) {
        UserAccount user = currentUserService.currentUser();
        PersonalFinanceDebt entity = debt.getId() == null
                ? new PersonalFinanceDebt()
                : debtRepository.findByIdAndUser(debt.getId(), user).orElseThrow();
        entity.setUser(user);
        entity.setName(clean(debt.getName()));
        entity.setCreditorName(clean(debt.getCreditorName()));
        entity.setHolderType(defaultEnum(debt.getHolderType(), PersonalFinanceDebtHolderType.OWN_NAME));
        entity.setContactName(clean(debt.getContactName()));
        entity.setDebtType(defaultEnum(debt.getDebtType(), PersonalFinanceDebtType.CREDIT_CARD));
        entity.setCurrency(defaultEnum(debt.getCurrency(), PersonalFinanceCurrency.PEN));
        entity.setOriginalAmount(defaultAmount(debt.getOriginalAmount()));
        entity.setCurrentBalance(defaultAmount(debt.getCurrentBalance()));
        entity.setMonthlyDueAmount(defaultAmount(debt.getMonthlyDueAmount()));
        entity.setMinimumPayment(defaultAmount(debt.getMinimumPayment()));
        entity.setInterestRateMonthly(defaultAmount(debt.getInterestRateMonthly()));
        entity.setDueDay(normalizeDueDay(debt.getDueDay()));
        entity.setStatus(defaultEnum(debt.getStatus(), PersonalFinanceDebtStatus.ACTIVE));
        entity.setPriority(defaultEnum(debt.getPriority(), PersonalFinancePriority.MEDIUM));
        entity.setFixedPayment(debt.isFixedPayment());
        entity.setNotes(clean(debt.getNotes()));
        debtRepository.save(entity);
    }

    @Transactional(readOnly = true)
    public PersonalFinanceDebt debt(Long id) {
        return debtRepository.findByIdAndUser(id, currentUserService.currentUser()).orElseThrow();
    }

    @Transactional
    public void deleteDebt(Long id) {
        debtRepository.findByIdAndUser(id, currentUserService.currentUser()).ifPresent(debtRepository::delete);
    }

    @Transactional(readOnly = true)
    public List<PersonalFinanceFixedExpense> fixedExpenses() {
        return fixedExpenseRepository.findByUserOrderByActiveDescDueDayAscNameAsc(currentUserService.currentUser());
    }

    @Transactional
    public void saveFixedExpense(PersonalFinanceFixedExpense expense) {
        UserAccount user = currentUserService.currentUser();
        PersonalFinanceFixedExpense entity = expense.getId() == null
                ? new PersonalFinanceFixedExpense()
                : fixedExpenseRepository.findByIdAndUser(expense.getId(), user).orElseThrow();
        entity.setUser(user);
        entity.setName(clean(expense.getName()));
        entity.setCategory(defaultEnum(expense.getCategory(), PersonalFinanceExpenseCategory.OTHER));
        entity.setAmount(defaultAmount(expense.getAmount()));
        entity.setCurrency(defaultEnum(expense.getCurrency(), PersonalFinanceCurrency.PEN));
        entity.setDueDay(normalizeDueDay(expense.getDueDay()));
        entity.setFrequency(defaultEnum(expense.getFrequency(), PersonalFinanceFrequency.MONTHLY));
        entity.setMandatory(expense.isMandatory());
        entity.setActive(expense.isActive());
        entity.setNotes(clean(expense.getNotes()));
        fixedExpenseRepository.save(entity);
    }

    @Transactional(readOnly = true)
    public PersonalFinanceFixedExpense fixedExpense(Long id) {
        return fixedExpenseRepository.findByIdAndUser(id, currentUserService.currentUser()).orElseThrow();
    }

    @Transactional
    public void deleteFixedExpense(Long id) {
        fixedExpenseRepository.findByIdAndUser(id, currentUserService.currentUser()).ifPresent(fixedExpenseRepository::delete);
    }

    @Transactional(readOnly = true)
    public List<PersonalFinanceIncomeSource> incomeSources() {
        return incomeSourceRepository.findByUserOrderByActiveDescNameAsc(currentUserService.currentUser());
    }

    @Transactional(readOnly = true)
    public List<PersonalFinanceIncomeSource> activeIncomeSources() {
        return incomeSourceRepository.findByUserAndActiveTrueOrderByNameAsc(currentUserService.currentUser());
    }

    @Transactional
    public void saveIncomeSource(PersonalFinanceIncomeSource source) {
        UserAccount user = currentUserService.currentUser();
        PersonalFinanceIncomeSource entity = source.getId() == null
                ? new PersonalFinanceIncomeSource()
                : incomeSourceRepository.findByIdAndUser(source.getId(), user).orElseThrow();
        entity.setUser(user);
        entity.setName(clean(source.getName()));
        entity.setType(defaultEnum(source.getType(), PersonalFinanceIncomeType.OTHER));
        entity.setDefaultAmount(defaultAmount(source.getDefaultAmount()));
        entity.setCurrency(defaultEnum(source.getCurrency(), PersonalFinanceCurrency.PEN));
        entity.setActive(source.isActive());
        entity.setNotes(clean(source.getNotes()));
        incomeSourceRepository.save(entity);
    }

    @Transactional(readOnly = true)
    public PersonalFinanceIncomeSource incomeSource(Long id) {
        return incomeSourceRepository.findByIdAndUser(id, currentUserService.currentUser()).orElseThrow();
    }

    @Transactional
    public void deleteIncomeSource(Long id) {
        incomeSourceRepository.findByIdAndUser(id, currentUserService.currentUser()).ifPresent(incomeSourceRepository::delete);
    }

    @Transactional(readOnly = true)
    public List<PersonalFinanceIncomeEvent> incomeEvents(YearMonth yearMonth) {
        LocalDate start = yearMonth.atDay(1);
        LocalDate end = yearMonth.atEndOfMonth();
        return incomeEventRepository.findByUserAndExpectedDateBetweenOrderByExpectedDateAscIdAsc(currentUserService.currentUser(), start, end);
    }

    @Transactional
    public void saveIncomeEvent(PersonalFinanceIncomeEvent event, Long sourceId) {
        UserAccount user = currentUserService.currentUser();
        PersonalFinanceIncomeEvent entity = event.getId() == null
                ? new PersonalFinanceIncomeEvent()
                : incomeEventRepository.findByIdAndUser(event.getId(), user).orElseThrow();
        entity.setUser(user);
        entity.setTitle(clean(event.getTitle()));
        entity.setAmount(defaultAmount(event.getAmount()));
        entity.setCurrency(defaultEnum(event.getCurrency(), PersonalFinanceCurrency.PEN));
        entity.setExpectedDate(event.getExpectedDate());
        entity.setReceivedDate(event.getReceivedDate());
        entity.setStatus(defaultEnum(event.getStatus(), PersonalFinanceIncomeStatus.PLANNED));
        entity.setNotes(clean(event.getNotes()));
        if (sourceId != null) {
            entity.setIncomeSource(incomeSourceRepository.findByIdAndUser(sourceId, user).orElse(null));
        } else {
            entity.setIncomeSource(null);
        }
        incomeEventRepository.save(entity);
    }

    @Transactional(readOnly = true)
    public PersonalFinanceIncomeEvent incomeEvent(Long id) {
        return incomeEventRepository.findByIdAndUser(id, currentUserService.currentUser()).orElseThrow();
    }

    @Transactional
    public void deleteIncomeEvent(Long id) {
        incomeEventRepository.findByIdAndUser(id, currentUserService.currentUser()).ifPresent(incomeEventRepository::delete);
    }

    private List<PersonalFinanceDebt> debtsForMonthlyPlan(UserAccount user) {
        return debtRepository.findByUserOrderByStatusAscDueDayAscNameAsc(user).stream()
                .filter(debt -> debt.getStatus() == PersonalFinanceDebtStatus.ACTIVE
                        || debt.getStatus() == PersonalFinanceDebtStatus.OVERDUE
                        || debt.getStatus() == PersonalFinanceDebtStatus.STOPPED_PAYMENT
                        || debt.getStatus() == PersonalFinanceDebtStatus.NEGOTIATION
                        || debt.getStatus() == PersonalFinanceDebtStatus.SUSPENDED)
                .toList();
    }

    private PersonalFinanceMonthlyPlanItem itemFromObligation(PersonalFinancePaymentObligation obligation) {
        return new PersonalFinanceMonthlyPlanItem(
                obligation.getId(),
                obligation.getTitle(),
                obligation.getSourceType().getLabel(),
                obligation.getSourceType(),
                obligation.getGroup(),
                obligation.getCurrency(),
                safe(obligation.getAmountDue()),
                safe(obligation.getAmountPaid()),
                obligation.pendingAmount(),
                obligation.getDueDate(),
                obligation.getStatus(),
                defaultEnum(obligation.getPriority(), PersonalFinancePriority.MEDIUM),
                obligation.getNotes(),
                false
        );
    }

    private void sortItems(List<PersonalFinanceMonthlyPlanItem> items) {
        items.sort(Comparator
                .comparing(PersonalFinanceMonthlyPlanItem::isPaid)
                .thenComparing(PersonalFinanceMonthlyPlanItem::dueDate, Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(PersonalFinanceMonthlyPlanItem::priority)
                .thenComparing(PersonalFinanceMonthlyPlanItem::title, Comparator.nullsLast(String::compareToIgnoreCase)));
    }

    private BigDecimal totalDue(List<PersonalFinanceMonthlyPlanItem> items) {
        return items.stream().map(PersonalFinanceMonthlyPlanItem::amountDue).map(this::safe).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal totalPaid(List<PersonalFinanceMonthlyPlanItem> items) {
        return items.stream().map(PersonalFinanceMonthlyPlanItem::amountPaid).map(this::safe).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal totalPending(List<PersonalFinanceMonthlyPlanItem> items) {
        return items.stream().map(PersonalFinanceMonthlyPlanItem::pendingAmount).map(this::safe).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private LocalDate dateFromDueDay(YearMonth yearMonth, Integer dueDay) {
        if (dueDay == null) {
            return null;
        }
        int safeDay = Math.max(1, Math.min(yearMonth.lengthOfMonth(), dueDay));
        return yearMonth.atDay(safeDay);
    }

    private PersonalFinanceObligationStatus obligationStatusFromDebt(PersonalFinanceDebt debt) {
        if (debt.getStatus() == PersonalFinanceDebtStatus.OVERDUE || debt.getStatus() == PersonalFinanceDebtStatus.STOPPED_PAYMENT) {
            return PersonalFinanceObligationStatus.OVERDUE;
        }
        if (debt.getStatus() == PersonalFinanceDebtStatus.PAID) {
            return PersonalFinanceObligationStatus.PAID;
        }
        if (debt.getStatus() == PersonalFinanceDebtStatus.CANCELLED) {
            return PersonalFinanceObligationStatus.CANCELLED;
        }
        return PersonalFinanceObligationStatus.PENDING;
    }

    private String debtPlanNotes(PersonalFinanceDebt debt) {
        List<String> notes = new ArrayList<>();
        if (debt.getHolderType() != null && debt.getHolderType() != PersonalFinanceDebtHolderType.OWN_NAME) {
            notes.add(debt.getHolderType().getLabel());
        }
        if (debt.getContactName() != null && !debt.getContactName().isBlank()) {
            notes.add("Contacto: " + debt.getContactName());
        }
        if (debt.getInterestRateMonthly() != null && debt.getInterestRateMonthly().compareTo(BigDecimal.ZERO) > 0) {
            notes.add("Interés mensual: " + debt.getInterestRateMonthly() + "%");
        }
        if (debt.getNotes() != null && !debt.getNotes().isBlank()) {
            notes.add(debt.getNotes());
        }
        return String.join(" · ", notes);
    }

    private PersonalFinanceObligationStatus resolveObligationStatus(PersonalFinanceObligationStatus status, BigDecimal amountDue, BigDecimal amountPaid) {
        if (status == PersonalFinanceObligationStatus.CANCELLED || status == PersonalFinanceObligationStatus.OVERDUE) {
            return status;
        }
        BigDecimal due = safe(amountDue);
        BigDecimal paid = safe(amountPaid);
        if (due.compareTo(BigDecimal.ZERO) > 0 && paid.compareTo(due) >= 0) {
            return PersonalFinanceObligationStatus.PAID;
        }
        if (paid.compareTo(BigDecimal.ZERO) > 0) {
            return PersonalFinanceObligationStatus.PARTIAL;
        }
        return status == PersonalFinanceObligationStatus.PAID ? PersonalFinanceObligationStatus.PENDING : status;
    }

    private String clean(String value) {
        return value == null ? null : value.trim();
    }

    private BigDecimal defaultAmount(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private BigDecimal safe(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private Integer normalizeDueDay(Integer dueDay) {
        if (dueDay == null) {
            return null;
        }
        if (dueDay < 1) {
            return 1;
        }
        if (dueDay > 31) {
            return 31;
        }
        return dueDay;
    }

    private <T> T defaultEnum(T value, T fallback) {
        return value == null ? fallback : value;
    }
}
