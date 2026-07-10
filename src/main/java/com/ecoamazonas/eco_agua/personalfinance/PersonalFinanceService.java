package com.ecoamazonas.eco_agua.personalfinance;

import com.ecoamazonas.eco_agua.user.UserAccount;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Service
public class PersonalFinanceService {

    private final PersonalFinanceDebtRepository debtRepository;
    private final PersonalFinanceFixedExpenseRepository fixedExpenseRepository;
    private final PersonalFinanceIncomeSourceRepository incomeSourceRepository;
    private final PersonalFinanceIncomeEventRepository incomeEventRepository;
    private final PersonalFinancePaymentObligationRepository paymentObligationRepository;
    private final PersonalFinanceDebtScheduleLineRepository debtScheduleLineRepository;
    private final PersonalFinanceCurrentUserService currentUserService;
    private final PersonalFinancePaymentService paymentService;
    private final PersonalFinanceDebtNegotiationRepository debtNegotiationRepository;

    public PersonalFinanceService(
            PersonalFinanceDebtRepository debtRepository,
            PersonalFinanceFixedExpenseRepository fixedExpenseRepository,
            PersonalFinanceIncomeSourceRepository incomeSourceRepository,
            PersonalFinanceIncomeEventRepository incomeEventRepository,
            PersonalFinancePaymentObligationRepository paymentObligationRepository,
            PersonalFinanceDebtScheduleLineRepository debtScheduleLineRepository,
            PersonalFinanceCurrentUserService currentUserService,
            PersonalFinancePaymentService paymentService,
            PersonalFinanceDebtNegotiationRepository debtNegotiationRepository
    ) {
        this.debtRepository = debtRepository;
        this.fixedExpenseRepository = fixedExpenseRepository;
        this.incomeSourceRepository = incomeSourceRepository;
        this.incomeEventRepository = incomeEventRepository;
        this.paymentObligationRepository = paymentObligationRepository;
        this.debtScheduleLineRepository = debtScheduleLineRepository;
        this.currentUserService = currentUserService;
        this.paymentService = paymentService;
        this.debtNegotiationRepository = debtNegotiationRepository;
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
        List<PersonalFinanceDebt> debts = debtsForMonthlyPlan(user);
        Map<Long, PersonalFinanceDebt> debtsById = new HashMap<>();
        for (PersonalFinanceDebt debt : debts) {
            if (debt.getId() != null) {
                debtsById.put(debt.getId(), debt);
            }
        }
        List<PersonalFinancePaymentObligation> obligations = paymentObligationRepository.findByUserAndDueDateBetweenOrderByDueDateAscPriorityAscIdAsc(user, start, end);
        List<PersonalFinanceDelinquentDebtItem> delinquentDebts = delinquentDebtsForPlan(user);

        List<PersonalFinanceMonthlyPlanItem> basicItems = new ArrayList<>();
        List<PersonalFinanceMonthlyPlanItem> debtItems = new ArrayList<>();
        List<PersonalFinanceMonthlyPlanItem> otherItems = new ArrayList<>();

        for (PersonalFinancePaymentObligation obligation : obligations) {
            if (obligation.getStatus() == PersonalFinanceObligationStatus.CANCELLED) {
                continue;
            }
            PersonalFinanceMonthlyPlanItem item = itemFromObligation(obligation, linkedDebtForObligation(obligation, debtsById));
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
        BigDecimal delinquentDebtPenTotal = delinquentDebts.stream()
                .filter(item -> item.currency() == PersonalFinanceCurrency.PEN)
                .map(PersonalFinanceDelinquentDebtItem::currentBalance)
                .map(this::safe)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal delinquentDebtUsdTotal = delinquentDebts.stream()
                .filter(item -> item.currency() == PersonalFinanceCurrency.USD)
                .map(PersonalFinanceDelinquentDebtItem::currentBalance)
                .map(this::safe)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        long reviewDueCount = delinquentDebts.stream().filter(PersonalFinanceDelinquentDebtItem::reviewDue).count();
        PersonalFinanceDebtPortfolioSummary debtPortfolio = debtPortfolioSummary(debts, debtItems);

        return new PersonalFinanceMonthlyPlan(
                yearMonth,
                incomes,
                basicItems,
                debtItems,
                otherItems,
                delinquentDebts,
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
                delinquentDebtPenTotal,
                delinquentDebtUsdTotal,
                debtPortfolio,
                delinquentDebts.size(),
                overdueCount,
                highInterestDebtCount,
                reviewDueCount
        );
    }

    @Transactional(readOnly = true)
    public PersonalFinanceMonthlyLiveSummary monthlyLiveSummary(YearMonth yearMonth, PersonalFinanceCurrency requestedCurrency) {
        PersonalFinanceCurrency currency = defaultEnum(requestedCurrency, PersonalFinanceCurrency.PEN);
        PersonalFinanceMonthlyPlan plan = monthlyPlan(yearMonth);

        BigDecimal expectedIncome = plan.incomes().stream()
                .filter(income -> defaultEnum(income.getCurrency(), PersonalFinanceCurrency.PEN) == currency)
                .filter(income -> income.getStatus() != PersonalFinanceIncomeStatus.CANCELLED
                        && income.getStatus() != PersonalFinanceIncomeStatus.MISSED)
                .map(PersonalFinanceIncomeEvent::getAmount)
                .map(this::safe)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal receivedIncome = plan.incomes().stream()
                .filter(income -> defaultEnum(income.getCurrency(), PersonalFinanceCurrency.PEN) == currency)
                .filter(income -> income.getStatus() == PersonalFinanceIncomeStatus.RECEIVED)
                .map(PersonalFinanceIncomeEvent::getAmount)
                .map(this::safe)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        List<PersonalFinanceMonthlyPlanItem> items = new ArrayList<>();
        items.addAll(plan.basicLivingItems());
        items.addAll(plan.debtItems());
        items.addAll(plan.otherItems());
        List<PersonalFinanceMonthlyPlanItem> currencyItems = items.stream()
                .filter(item -> defaultEnum(item.currency(), PersonalFinanceCurrency.PEN) == currency)
                .toList();

        BigDecimal paidTotal = totalPaid(currencyItems);
        BigDecimal pendingTotal = totalPending(currencyItems);
        long paidPayments = currencyItems.stream().filter(PersonalFinanceMonthlyPlanItem::isPaid).count();
        long totalIncomes = plan.incomes().stream()
                .filter(income -> defaultEnum(income.getCurrency(), PersonalFinanceCurrency.PEN) == currency)
                .filter(income -> income.getStatus() != PersonalFinanceIncomeStatus.CANCELLED
                        && income.getStatus() != PersonalFinanceIncomeStatus.MISSED)
                .count();
        long receivedIncomes = plan.incomes().stream()
                .filter(income -> defaultEnum(income.getCurrency(), PersonalFinanceCurrency.PEN) == currency)
                .filter(income -> income.getStatus() == PersonalFinanceIncomeStatus.RECEIVED)
                .count();

        return new PersonalFinanceMonthlyLiveSummary(
                yearMonth,
                currency,
                expectedIncome,
                receivedIncome,
                paidTotal,
                pendingTotal,
                receivedIncome.subtract(paidTotal),
                expectedIncome.subtract(paidTotal.add(pendingTotal)),
                currencyItems.size(),
                paidPayments,
                currencyItems.size() - paidPayments,
                totalIncomes,
                receivedIncomes
        );
    }

    @Transactional
    public PersonalFinancePaymentObligation setPaymentObligationPaid(Long id, boolean paid) {
        return paymentService.setObligationPaid(id, paid);
    }

    @Transactional
    public PersonalFinanceIncomeEvent setIncomeEventReceived(Long id, boolean received) {
        UserAccount user = currentUserService.currentUser();
        PersonalFinanceIncomeEvent event = incomeEventRepository.findByIdAndUser(id, user).orElseThrow();
        if (event.getStatus() == PersonalFinanceIncomeStatus.CANCELLED || event.getStatus() == PersonalFinanceIncomeStatus.MISSED) {
            throw new IllegalArgumentException("No se puede cambiar este ingreso desde el control rápido.");
        }
        event.setStatus(received ? PersonalFinanceIncomeStatus.RECEIVED : PersonalFinanceIncomeStatus.PLANNED);
        event.setReceivedDate(received ? LocalDate.now() : null);
        return incomeEventRepository.save(event);
    }

    @Transactional(readOnly = true)
    public PersonalFinancePriorityPlan priorityPlan(
            YearMonth yearMonth,
            PersonalFinanceCurrency requestedCurrency,
            PersonalFinanceCashBasis requestedCashBasis,
            BigDecimal requestedManualCash
    ) {
        PersonalFinanceMonthlyPlan monthlyPlan = monthlyPlan(yearMonth);
        PersonalFinanceCurrency currency = defaultEnum(requestedCurrency, PersonalFinanceCurrency.PEN);
        PersonalFinanceCashBasis cashBasis = defaultEnum(requestedCashBasis, PersonalFinanceCashBasis.EXPECTED);
        BigDecimal manualCash = nonNegative(requestedManualCash);

        List<PersonalFinanceMonthlyPlanItem> allItems = new ArrayList<>();
        allItems.addAll(monthlyPlan.basicLivingItems());
        allItems.addAll(monthlyPlan.debtItems());
        allItems.addAll(monthlyPlan.otherItems());

        BigDecimal expectedIncome = monthlyPlan.incomes().stream()
                .filter(income -> defaultEnum(income.getCurrency(), PersonalFinanceCurrency.PEN) == currency)
                .filter(income -> income.getStatus() != PersonalFinanceIncomeStatus.CANCELLED
                        && income.getStatus() != PersonalFinanceIncomeStatus.MISSED)
                .map(PersonalFinanceIncomeEvent::getAmount)
                .map(this::safe)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal receivedIncome = monthlyPlan.incomes().stream()
                .filter(income -> defaultEnum(income.getCurrency(), PersonalFinanceCurrency.PEN) == currency)
                .filter(income -> income.getStatus() == PersonalFinanceIncomeStatus.RECEIVED)
                .map(PersonalFinanceIncomeEvent::getAmount)
                .map(this::safe)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal alreadyPaid = allItems.stream()
                .filter(item -> defaultEnum(item.currency(), PersonalFinanceCurrency.PEN) == currency)
                .map(PersonalFinanceMonthlyPlanItem::amountPaid)
                .map(this::safe)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal cashBasisAmount = switch (cashBasis) {
            case RECEIVED -> receivedIncome;
            case MANUAL -> manualCash;
            default -> expectedIncome;
        };
        BigDecimal availableCash = cashBasis == PersonalFinanceCashBasis.MANUAL
                ? manualCash
                : nonNegative(cashBasisAmount.subtract(alreadyPaid));

        List<PersonalFinanceMonthlyPlanItem> candidates = allItems.stream()
                .filter(item -> defaultEnum(item.currency(), PersonalFinanceCurrency.PEN) == currency)
                .filter(item -> !item.isPaid())
                .filter(item -> safe(item.pendingAmount()).compareTo(BigDecimal.ZERO) > 0)
                .sorted(priorityComparator())
                .toList();

        BigDecimal essentialPending = candidates.stream()
                .filter(this::isEssential)
                .map(PersonalFinanceMonthlyPlanItem::pendingAmount)
                .map(this::safe)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal debtPending = candidates.stream()
                .filter(item -> item.group() == PersonalFinanceObligationGroup.DEBT_PAYMENT)
                .map(PersonalFinanceMonthlyPlanItem::pendingAmount)
                .map(this::safe)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal otherPending = candidates.stream()
                .filter(item -> !isEssential(item) && item.group() != PersonalFinanceObligationGroup.DEBT_PAYMENT)
                .map(PersonalFinanceMonthlyPlanItem::pendingAmount)
                .map(this::safe)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalPending = essentialPending.add(debtPending).add(otherPending);

        List<PersonalFinancePriorityPlanItem> priorityItems = new ArrayList<>();
        BigDecimal cashRemaining = availableCash;
        int position = 1;
        for (PersonalFinanceMonthlyPlanItem item : candidates) {
            BigDecimal pendingAmount = nonNegative(item.pendingAmount());
            BigDecimal recommendedAmount = pendingAmount.min(cashRemaining);
            BigDecimal unfundedAmount = pendingAmount.subtract(recommendedAmount);
            cashRemaining = nonNegative(cashRemaining.subtract(recommendedAmount));
            PersonalFinanceAllocationStatus allocationStatus = allocationStatus(recommendedAmount, pendingAmount);
            priorityItems.add(new PersonalFinancePriorityPlanItem(
                    position++,
                    item.id(),
                    item.title(),
                    item.sourceLabel(),
                    item.sourceType(),
                    item.group(),
                    item.currency(),
                    pendingAmount,
                    item.dueDate(),
                    item.status(),
                    item.priority(),
                    recommendedAmount,
                    unfundedAmount,
                    cashRemaining,
                    allocationStatus,
                    allocationReason(item),
                    isEssential(item),
                    item.generated()
            ));
        }

        BigDecimal allocatedTotal = priorityItems.stream()
                .map(PersonalFinancePriorityPlanItem::recommendedAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal unfundedTotal = priorityItems.stream()
                .map(PersonalFinancePriorityPlanItem::unfundedAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal unfundedDebtTotal = priorityItems.stream()
                .filter(item -> item.group() == PersonalFinanceObligationGroup.DEBT_PAYMENT)
                .map(PersonalFinancePriorityPlanItem::unfundedAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal essentialGap = nonNegative(essentialPending.subtract(availableCash));
        BigDecimal incomeForPressure = expectedIncome.compareTo(BigDecimal.ZERO) > 0 ? expectedIncome : cashBasisAmount;
        BigDecimal debtPressurePercentage = percentage(debtPending, incomeForPressure);

        PersonalFinanceMonthlyPlanItem largestDebt = candidates.stream()
                .filter(item -> item.group() == PersonalFinanceObligationGroup.DEBT_PAYMENT)
                .max(Comparator.comparing(PersonalFinanceMonthlyPlanItem::pendingAmount))
                .orElse(null);
        String largestDebtTitle = largestDebt == null ? null : largestDebt.title();
        BigDecimal largestDebtAmount = largestDebt == null ? BigDecimal.ZERO : safe(largestDebt.pendingAmount());
        BigDecimal largestDebtSharePercentage = percentage(largestDebtAmount, incomeForPressure);

        PersonalFinanceHealthLevel healthLevel = healthLevel(
                totalPending,
                availableCash,
                essentialGap,
                unfundedTotal,
                unfundedDebtTotal,
                debtPressurePercentage
        );

        long excludedIncomeCount = monthlyPlan.incomes().stream()
                .filter(income -> defaultEnum(income.getCurrency(), PersonalFinanceCurrency.PEN) != currency)
                .count();
        long excludedObligationCount = allItems.stream()
                .filter(item -> defaultEnum(item.currency(), PersonalFinanceCurrency.PEN) != currency)
                .filter(item -> !item.isPaid())
                .filter(item -> safe(item.pendingAmount()).compareTo(BigDecimal.ZERO) > 0)
                .count();

        return new PersonalFinancePriorityPlan(
                yearMonth,
                currency,
                cashBasis,
                manualCash,
                expectedIncome,
                receivedIncome,
                alreadyPaid,
                cashBasisAmount,
                availableCash,
                essentialPending,
                debtPending,
                otherPending,
                totalPending,
                allocatedTotal,
                unfundedTotal,
                unfundedDebtTotal,
                cashRemaining,
                essentialGap,
                debtPressurePercentage,
                largestDebtTitle,
                largestDebtAmount,
                largestDebtSharePercentage,
                healthLevel,
                healthHeadline(healthLevel),
                healthRecommendation(healthLevel),
                priorityItems,
                priorityItems.stream().filter(item -> item.allocationStatus() == PersonalFinanceAllocationStatus.COVERED).count(),
                priorityItems.stream().filter(item -> item.allocationStatus() == PersonalFinanceAllocationStatus.PARTIAL).count(),
                priorityItems.stream().filter(item -> item.allocationStatus() == PersonalFinanceAllocationStatus.UNFUNDED).count(),
                excludedIncomeCount,
                excludedObligationCount
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
        boolean newEntity = entity.getId() == null;
        BigDecimal recordedPaidAmount = newEntity ? BigDecimal.ZERO : defaultAmount(entity.getAmountPaid());
        PersonalFinanceObligationStatus requestedStatus = defaultEnum(obligation.getStatus(), PersonalFinanceObligationStatus.PENDING);
        entity.setAmountDue(defaultAmount(obligation.getAmountDue()));
        entity.setAmountPaid(recordedPaidAmount);
        entity.setCurrency(defaultEnum(obligation.getCurrency(), PersonalFinanceCurrency.PEN));
        entity.setDueDate(obligation.getDueDate());
        entity.setStatus(requestedStatus == PersonalFinanceObligationStatus.CANCELLED
                ? PersonalFinanceObligationStatus.CANCELLED
                : resolveObligationStatus(PersonalFinanceObligationStatus.PENDING, entity.getAmountDue(), recordedPaidAmount));
        entity.setPriority(defaultEnum(obligation.getPriority(), PersonalFinancePriority.MEDIUM));
        entity.setNotes(clean(obligation.getNotes()));
        paymentObligationRepository.save(entity);
    }

    @Transactional
    public void deletePaymentObligation(Long id) {
        paymentObligationRepository.findByIdAndUser(id, currentUserService.currentUser()).ifPresent(paymentObligationRepository::delete);
    }

    @Transactional
    public PersonalFinancePaymentObligation createVoluntaryPayment(Long debtId, PersonalFinanceVoluntaryPaymentForm form) {
        UserAccount user = currentUserService.currentUser();
        PersonalFinanceDebt debt = debtRepository.findByIdAndUser(debtId, user).orElseThrow();
        BigDecimal amount = defaultAmount(form.getAmount());
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("El monto del abono debe ser mayor que cero.");
        }
        LocalDate dueDate = form.getDueDate() == null ? LocalDate.now() : form.getDueDate();
        PersonalFinancePaymentObligation obligation = new PersonalFinancePaymentObligation();
        obligation.setUser(user);
        obligation.setSourceType(PersonalFinanceObligationSourceType.DEBT_VOLUNTARY_PAYMENT);
        obligation.setSourceId(debt.getId());
        obligation.setGroup(PersonalFinanceObligationGroup.DEBT_PAYMENT);
        obligation.setTitle("Abono voluntario - " + debt.getName());
        obligation.setAmountDue(amount);
        obligation.setAmountPaid(BigDecimal.ZERO);
        obligation.setCurrency(defaultEnum(form.getCurrency(), debt.getCurrency()));
        obligation.setDueDate(dueDate);
        obligation.setStatus(dueDate.isBefore(LocalDate.now()) ? PersonalFinanceObligationStatus.OVERDUE : PersonalFinanceObligationStatus.PENDING);
        obligation.setPriority(defaultEnum(form.getPriority(), debt.getPriority()));
        List<String> notes = new ArrayList<>();
        notes.add("Abono voluntario creado desde deuda en seguimiento");
        if (form.getNotes() != null && !form.getNotes().isBlank()) {
            notes.add(form.getNotes().trim());
        }
        obligation.setNotes(String.join(" · ", notes));
        return paymentObligationRepository.save(obligation);
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
        entity.setScheduleMode(defaultEnum(debt.getScheduleMode(), inferScheduleMode(debt)));
        entity.setScheduleStartDate(debt.getScheduleStartDate());
        entity.setScheduleEndDate(debt.getScheduleEndDate());
        entity.setInstallmentCount(normalizePositiveInteger(debt.getInstallmentCount()));
        entity.setAutoGenerateMonthly(debt.isAutoGenerateMonthly());
        entity.setStatus(defaultEnum(debt.getStatus(), PersonalFinanceDebtStatus.ACTIVE));
        entity.setPriority(defaultEnum(debt.getPriority(), PersonalFinancePriority.MEDIUM));
        entity.setFixedPayment(debt.isFixedPayment());
        entity.setPreviousMonthlyPayment(defaultAmount(debt.getPreviousMonthlyPayment()));
        entity.setLastPaymentDate(debt.getLastPaymentDate());
        entity.setDelinquencyStartDate(debt.getDelinquencyStartDate());
        entity.setCollectionStatus(defaultEnum(debt.getCollectionStatus(), PersonalFinanceCollectionStatus.NONE));
        entity.setNegotiationStatus(defaultEnum(debt.getNegotiationStatus(), PersonalFinanceNegotiationStatus.NOT_STARTED));
        entity.setNextReviewDate(debt.getNextReviewDate());
        entity.setNotes(clean(debt.getNotes()));
        normalizeTrackingDebt(entity);
        PersonalFinanceDebt saved = debtRepository.save(entity);
        if (saved.isDelinquentTracking()) {
            cancelGeneratedObligationsForTrackingDebt(user, saved);
        }
    }

    @Transactional(readOnly = true)
    public PersonalFinanceDebt debt(Long id) {
        return debtRepository.findByIdAndUser(id, currentUserService.currentUser()).orElseThrow();
    }

    @Transactional
    public void deleteDebt(Long id) {
        UserAccount user = currentUserService.currentUser();
        debtRepository.findByIdAndUser(id, user).ifPresent(debt -> {
            if (debtNegotiationRepository.existsByUserAndDebt(user, debt)) {
                throw new IllegalArgumentException("Esta deuda tiene historial de negociación y no puede eliminarse. Márcala como cancelada o pagada.");
            }
            debtRepository.delete(debt);
        });
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
        entity.setStartDate(expense.getStartDate());
        entity.setEndDate(expense.getEndDate());
        entity.setAutoGenerateMonthly(expense.isAutoGenerateMonthly());
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
        entity.setFrequency(defaultEnum(source.getFrequency(), PersonalFinanceFrequency.MONTHLY));
        entity.setExpectedDay(normalizeDueDay(source.getExpectedDay()));
        entity.setStartDate(source.getStartDate());
        entity.setEndDate(source.getEndDate());
        entity.setAutoGenerateMonthly(source.isAutoGenerateMonthly());
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

    @Transactional(readOnly = true)
    public List<PersonalFinanceDebtScheduleLine> debtSchedule(Long debtId) {
        UserAccount user = currentUserService.currentUser();
        PersonalFinanceDebt debt = debtRepository.findByIdAndUser(debtId, user).orElseThrow();
        return debtScheduleLineRepository.findByUserAndDebtOrderByDueDateAscLineNumberAscIdAsc(user, debt);
    }

    @Transactional(readOnly = true)
    public PersonalFinanceDebtScheduleLine debtScheduleLine(Long id) {
        return debtScheduleLineRepository.findByIdAndUser(id, currentUserService.currentUser()).orElseThrow();
    }

    @Transactional(readOnly = true)
    public PersonalFinanceBankScheduleSummary bankScheduleSummary(Long debtId) {
        UserAccount user = currentUserService.currentUser();
        PersonalFinanceDebt debt = debtRepository.findByIdAndUser(debtId, user).orElseThrow();
        return summarizeBankSchedule(debtScheduleLineRepository.findByUserAndDebtOrderByDueDateAscLineNumberAscIdAsc(user, debt));
    }

    @Transactional
    public PersonalFinanceBankScheduleImportResult importBankSchedule(Long debtId, String rawContent, boolean replaceExisting) {
        UserAccount user = currentUserService.currentUser();
        PersonalFinanceDebt debt = debtRepository.findByIdAndUser(debtId, user).orElseThrow();
        List<String> errors = new ArrayList<>();
        List<ImportedBankScheduleRow> rows = parseBankScheduleRows(rawContent, errors);
        if (!errors.isEmpty()) {
            return new PersonalFinanceBankScheduleImportResult(rows.size(), 0, 0, 0, List.copyOf(errors));
        }
        if (rows.isEmpty()) {
            return new PersonalFinanceBankScheduleImportResult(0, 0, 0, 0, List.of("No se encontraron cuotas válidas para importar."));
        }

        List<PersonalFinanceDebtScheduleLine> existingLines = debtScheduleLineRepository.findByUserAndDebtOrderByDueDateAscLineNumberAscIdAsc(user, debt);
        if (replaceExisting) {
            for (PersonalFinanceDebtScheduleLine existing : existingLines) {
                if (hasRecordedSchedulePayment(existing, user)) {
                    errors.add("No se puede reemplazar el cronograma porque la cuota " + displayLineNumber(existing) + " ya tiene pago registrado.");
                }
            }
            if (!errors.isEmpty()) {
                return new PersonalFinanceBankScheduleImportResult(rows.size(), 0, 0, 0, List.copyOf(errors));
            }
            for (PersonalFinanceDebtScheduleLine existing : existingLines) {
                removeGeneratedObligation(existing, user);
            }
            debtScheduleLineRepository.deleteAll(existingLines);
            debtScheduleLineRepository.flush();
        }

        int created = 0;
        int updated = 0;
        int preservedPaid = 0;
        for (ImportedBankScheduleRow row : rows) {
            PersonalFinanceDebtScheduleLine entity = debtScheduleLineRepository
                    .findByUserAndDebtAndLineNumber(user, debt, row.lineNumber())
                    .orElseGet(PersonalFinanceDebtScheduleLine::new);
            boolean isNew = entity.getId() == null;
            boolean preservePayment = !isNew && hasRecordedSchedulePayment(entity, user);

            entity.setUser(user);
            entity.setDebt(debt);
            entity.setLineNumber(row.lineNumber());
            entity.setLineType(PersonalFinanceScheduleLineType.INSTALLMENT);
            entity.setTitle("Cuota " + row.lineNumber());
            entity.setPrincipalAmount(row.principal());
            entity.setInterestAmount(row.interest());
            entity.setInsuranceAmount(row.insurance());
            entity.setFeeAmount(BigDecimal.ZERO);
            entity.setTotalAmount(row.total());
            entity.setCurrency(defaultEnum(debt.getCurrency(), PersonalFinanceCurrency.PEN));
            entity.setDueDate(row.dueDate());
            entity.setNotes("Cronograma bancario real importado.");
            if (!preservePayment) {
                entity.setPaidAmount(BigDecimal.ZERO);
                entity.setPaidAt(null);
                entity.setStatus(row.dueDate().isBefore(LocalDate.now())
                        ? PersonalFinanceObligationStatus.OVERDUE
                        : PersonalFinanceObligationStatus.PENDING);
            } else {
                preservedPaid++;
            }
            PersonalFinanceDebtScheduleLine saved = debtScheduleLineRepository.save(entity);
            synchronizeGeneratedObligation(saved, user);
            if (isNew) created++; else updated++;
        }

        List<PersonalFinanceDebtScheduleLine> currentLines = debtScheduleLineRepository.findByUserAndDebtOrderByDueDateAscLineNumberAscIdAsc(user, debt);
        debt.setScheduleMode(PersonalFinanceDebtScheduleMode.BANK_SCHEDULE);
        debt.setAutoGenerateMonthly(true);
        debt.setFixedPayment(true);
        updateDebtFromBankSchedule(debt, currentLines);
        return new PersonalFinanceBankScheduleImportResult(rows.size(), created, updated, preservedPaid, List.of());
    }

    @Transactional
    public int markBankSchedulePaidThrough(Long debtId, Integer throughLineNumber) {
        if (throughLineNumber == null || throughLineNumber <= 0) {
            throw new IllegalArgumentException("Indica un número de cuota válido.");
        }
        UserAccount user = currentUserService.currentUser();
        PersonalFinanceDebt debt = debtRepository.findByIdAndUser(debtId, user).orElseThrow();
        int updated = 0;
        List<PersonalFinanceDebtScheduleLine> lines = debtScheduleLineRepository.findByUserAndDebtOrderByDueDateAscLineNumberAscIdAsc(user, debt);
        for (PersonalFinanceDebtScheduleLine line : lines) {
            if (line.getLineNumber() == null
                    || line.getLineNumber() > throughLineNumber
                    || line.getStatus() == PersonalFinanceObligationStatus.CANCELLED
                    || line.isPaidLike()) {
                continue;
            }
            if (paymentService.setScheduleLinePaid(line.getId(), true)) {
                updated++;
            }
        }
        return updated;
    }

    @Transactional
    public void setBankScheduleLinePaid(Long debtId, Long lineId, boolean paid) {
        UserAccount user = currentUserService.currentUser();
        PersonalFinanceDebt debt = debtRepository.findByIdAndUser(debtId, user).orElseThrow();
        PersonalFinanceDebtScheduleLine line = debtScheduleLineRepository.findByIdAndUser(lineId, user).orElseThrow();
        if (line.getDebt() == null || !debt.getId().equals(line.getDebt().getId())) {
            throw new IllegalArgumentException("La cuota no pertenece a esta deuda.");
        }
        paymentService.setScheduleLinePaid(lineId, paid);
    }

    @Transactional
    public void saveDebtScheduleLine(Long debtId, PersonalFinanceDebtScheduleLine line) {
        UserAccount user = currentUserService.currentUser();
        PersonalFinanceDebt debt = debtRepository.findByIdAndUser(debtId, user).orElseThrow();
        PersonalFinanceDebtScheduleLine entity = line.getId() == null
                ? new PersonalFinanceDebtScheduleLine()
                : debtScheduleLineRepository.findByIdAndUser(line.getId(), user).orElseThrow();
        entity.setUser(user);
        entity.setDebt(debt);
        entity.setLineNumber(line.getLineNumber());
        entity.setLineType(defaultEnum(line.getLineType(), PersonalFinanceScheduleLineType.INSTALLMENT));
        entity.setTitle(clean(line.getTitle()));
        entity.setPrincipalAmount(defaultAmount(line.getPrincipalAmount()));
        entity.setInterestAmount(defaultAmount(line.getInterestAmount()));
        entity.setInsuranceAmount(defaultAmount(line.getInsuranceAmount()));
        entity.setFeeAmount(defaultAmount(line.getFeeAmount()));
        entity.setTotalAmount(defaultAmount(line.getTotalAmount()));
        entity.setCurrency(defaultEnum(line.getCurrency(), debt.getCurrency()));
        entity.setDueDate(line.getDueDate());
        if (entity.getId() == null) {
            entity.setPaidAmount(BigDecimal.ZERO);
            entity.setPaidAt(null);
            entity.setStatus(entity.getDueDate() != null && entity.getDueDate().isBefore(LocalDate.now())
                    ? PersonalFinanceObligationStatus.OVERDUE
                    : PersonalFinanceObligationStatus.PENDING);
        }
        if (entity.getId() == null || line.getGeneratedObligationId() != null) {
            entity.setGeneratedObligationId(line.getGeneratedObligationId());
        }
        entity.setNotes(clean(line.getNotes()));
        PersonalFinanceDebtScheduleLine saved = debtScheduleLineRepository.save(entity);
        synchronizeGeneratedObligation(saved, user);
        refreshDebtBalanceFromSchedule(debt, user);
    }

    @Transactional
    public void deleteDebtScheduleLine(Long id) {
        UserAccount user = currentUserService.currentUser();
        debtScheduleLineRepository.findByIdAndUser(id, user).ifPresent(line -> {
            if (hasRecordedSchedulePayment(line, user)) {
                throw new IllegalArgumentException("No se puede eliminar una cuota que ya tiene pago registrado.");
            }
            PersonalFinanceDebt debt = line.getDebt();
            removeGeneratedObligation(line, user);
            debtScheduleLineRepository.delete(line);
            if (debt != null && debt.getScheduleMode() == PersonalFinanceDebtScheduleMode.BANK_SCHEDULE) {
                debtScheduleLineRepository.flush();
                updateDebtFromBankSchedule(
                        debt,
                        debtScheduleLineRepository.findByUserAndDebtOrderByDueDateAscLineNumberAscIdAsc(user, debt)
                );
            }
        });
    }

    @Transactional
    public PersonalFinanceDebtScheduleGenerationResult generateDebtSchedule(Long debtId, YearMonth fromMonth, Integer requestedMonths) {
        UserAccount user = currentUserService.currentUser();
        PersonalFinanceDebt debt = debtRepository.findByIdAndUser(debtId, user).orElseThrow();
        PersonalFinanceDebtScheduleMode mode = defaultEnum(debt.getScheduleMode(), inferScheduleMode(debt));
        if (mode == PersonalFinanceDebtScheduleMode.TRACKING_ONLY) {
            return PersonalFinanceDebtScheduleGenerationResult.empty(false);
        }

        if (mode == PersonalFinanceDebtScheduleMode.PRIVATE_LENDER_INTEREST
                && (requestedMonths == null || requestedMonths <= 0)
                && (debt.getInstallmentCount() == null || debt.getInstallmentCount() <= 0)) {
            throw new IllegalArgumentException("Indica el número de cuotas para calcular capital e interés automáticamente.");
        }

        int count = requestedMonths != null && requestedMonths > 0 ? requestedMonths : defaultScheduleCount(debt, mode);
        count = Math.max(1, Math.min(60, count));
        if (mode == PersonalFinanceDebtScheduleMode.ONE_TIME) {
            count = 1;
        }

        LocalDate firstDue = firstDueDate(debt, fromMonth);
        if (mode == PersonalFinanceDebtScheduleMode.PRIVATE_LENDER_INTEREST) {
            return generatePrivateLenderAmortizationSchedule(user, debt, firstDue, count);
        }

        int baseLineNumber = (int) debtScheduleLineRepository.countByUserAndDebt(user, debt);
        int created = 0;
        int unchanged = 0;
        for (int i = 0; i < count; i++) {
            YearMonth dueMonth = YearMonth.from(firstDue).plusMonths(i);
            LocalDate dueDate = mode == PersonalFinanceDebtScheduleMode.ONE_TIME
                    ? firstDue
                    : dueMonth.atDay(Math.min(firstDue.getDayOfMonth(), dueMonth.lengthOfMonth()));
            PersonalFinanceScheduleLineType lineType = lineTypeForMode(mode);
            if (debtScheduleLineRepository.existsByUserAndDebtAndDueDateAndLineType(user, debt, dueDate, lineType)) {
                unchanged++;
                continue;
            }
            PersonalFinanceDebtScheduleLine line = new PersonalFinanceDebtScheduleLine();
            line.setUser(user);
            line.setDebt(debt);
            line.setLineNumber(baseLineNumber + created + 1);
            line.setLineType(lineType);
            line.setTitle(scheduleTitle(debt, mode, i + 1));
            line.setCurrency(debt.getCurrency());
            line.setDueDate(dueDate);
            line.setStatus(defaultPendingStatus(dueDate));
            applyScheduleAmounts(line, debt, mode);
            line.setNotes(scheduleNotes(debt, mode));
            debtScheduleLineRepository.save(line);
            created++;
        }
        return new PersonalFinanceDebtScheduleGenerationResult(created, 0, 0, 0, unchanged, false);
    }

    @Transactional
    public PersonalFinanceMonthGenerationResult generateMonthlyPlan(YearMonth yearMonth) {
        UserAccount user = currentUserService.currentUser();
        int incomeEventsCreated = generateIncomeEventsForMonth(user, yearMonth);
        int fixedExpenseObligationsCreated = generateFixedExpenseObligationsForMonth(user, yearMonth);
        int debtScheduleLinesCreated = ensureDebtScheduleLinesForMonth(user, yearMonth);
        int debtScheduleObligationsCreated = generateDebtScheduleObligationsForMonth(user, yearMonth);
        int simpleDebtObligationsCreated = generateSimpleDebtObligationsForMonth(user, yearMonth);
        return new PersonalFinanceMonthGenerationResult(
                incomeEventsCreated,
                fixedExpenseObligationsCreated,
                debtScheduleLinesCreated,
                debtScheduleObligationsCreated,
                simpleDebtObligationsCreated
        );
    }

    @Transactional
    public int generateMonthlyObligations(YearMonth yearMonth) {
        return generateMonthlyPlan(yearMonth).totalCreated();
    }

    private int generateIncomeEventsForMonth(UserAccount user, YearMonth yearMonth) {
        LocalDate start = yearMonth.atDay(1);
        LocalDate end = yearMonth.atEndOfMonth();
        int created = 0;
        for (PersonalFinanceIncomeSource source : incomeSourceRepository.findByUserAndActiveTrueOrderByNameAsc(user)) {
            if (!source.isAutoGenerateMonthly() || !appliesToMonth(yearMonth, source.getStartDate(), source.getEndDate())) {
                continue;
            }
            if (source.getFrequency() != PersonalFinanceFrequency.MONTHLY) {
                continue;
            }
            if (incomeEventRepository.existsByUserAndIncomeSourceAndExpectedDateBetween(user, source, start, end)) {
                continue;
            }
            PersonalFinanceIncomeEvent event = new PersonalFinanceIncomeEvent();
            event.setUser(user);
            event.setIncomeSource(source);
            event.setTitle(source.getName());
            event.setAmount(defaultAmount(source.getDefaultAmount()));
            event.setCurrency(defaultEnum(source.getCurrency(), PersonalFinanceCurrency.PEN));
            event.setExpectedDate(dateFromDueDay(yearMonth, source.getExpectedDay() == null ? yearMonth.lengthOfMonth() : source.getExpectedDay()));
            event.setStatus(PersonalFinanceIncomeStatus.PLANNED);
            event.setNotes(generatedIncomeNotes(source));
            incomeEventRepository.save(event);
            created++;
        }
        return created;
    }

    private int generateFixedExpenseObligationsForMonth(UserAccount user, YearMonth yearMonth) {
        LocalDate start = yearMonth.atDay(1);
        LocalDate end = yearMonth.atEndOfMonth();
        int created = 0;
        for (PersonalFinanceFixedExpense expense : fixedExpenseRepository.findByUserAndActiveTrueOrderByDueDayAscNameAsc(user)) {
            if (!expense.isAutoGenerateMonthly() || expense.getFrequency() != PersonalFinanceFrequency.MONTHLY || !appliesToMonth(yearMonth, expense.getStartDate(), expense.getEndDate())) {
                continue;
            }
            if (paymentObligationRepository.existsByUserAndSourceTypeAndSourceIdAndDueDateBetween(user, PersonalFinanceObligationSourceType.FIXED_EXPENSE, expense.getId(), start, end)) {
                continue;
            }
            PersonalFinancePaymentObligation obligation = new PersonalFinancePaymentObligation();
            obligation.setUser(user);
            obligation.setSourceType(PersonalFinanceObligationSourceType.FIXED_EXPENSE);
            obligation.setSourceId(expense.getId());
            obligation.setGroup(expense.getCategory() == PersonalFinanceExpenseCategory.STUDY ? PersonalFinanceObligationGroup.STUDY : PersonalFinanceObligationGroup.BASIC_LIVING);
            obligation.setTitle(expense.getName());
            obligation.setAmountDue(defaultAmount(expense.getAmount()));
            obligation.setAmountPaid(BigDecimal.ZERO);
            obligation.setCurrency(defaultEnum(expense.getCurrency(), PersonalFinanceCurrency.PEN));
            obligation.setDueDate(dateFromDueDay(yearMonth, expense.getDueDay() == null ? 1 : expense.getDueDay()));
            obligation.setStatus(obligation.getDueDate() != null && obligation.getDueDate().isBefore(LocalDate.now()) ? PersonalFinanceObligationStatus.OVERDUE : PersonalFinanceObligationStatus.PENDING);
            obligation.setPriority(expense.isMandatory() ? PersonalFinancePriority.CRITICAL : PersonalFinancePriority.MEDIUM);
            obligation.setNotes(generatedFixedExpenseNotes(expense));
            paymentObligationRepository.save(obligation);
            created++;
        }
        return created;
    }

    private int ensureDebtScheduleLinesForMonth(UserAccount user, YearMonth yearMonth) {
        int created = 0;
        for (PersonalFinanceDebt debt : debtsForMonthlyPlan(user)) {
            PersonalFinanceDebtScheduleMode mode = defaultEnum(debt.getScheduleMode(), inferScheduleMode(debt));
            if (!debt.isAutoGenerateMonthly() || !debt.usesGeneratedSchedule() || mode == PersonalFinanceDebtScheduleMode.TRACKING_ONLY) {
                continue;
            }
            if (!debtScheduleAppliesToMonth(debt, mode, yearMonth)) {
                continue;
            }
            if (mode == PersonalFinanceDebtScheduleMode.PRIVATE_LENDER_INTEREST
                    && debt.getInstallmentCount() != null
                    && debt.getInstallmentCount() > 0) {
                PersonalFinanceDebtScheduleGenerationResult result = generatePrivateLenderAmortizationSchedule(
                        user,
                        debt,
                        firstDueDate(debt, yearMonth),
                        debt.getInstallmentCount()
                );
                created += result.created();
                continue;
            }
            LocalDate dueDate = debtDueDateForMonth(debt, yearMonth, mode);
            PersonalFinanceScheduleLineType lineType = lineTypeForMode(mode);
            if (debtScheduleLineRepository.existsByUserAndDebtAndDueDateAndLineType(user, debt, dueDate, lineType)) {
                continue;
            }
            PersonalFinanceDebtScheduleLine line = new PersonalFinanceDebtScheduleLine();
            line.setUser(user);
            line.setDebt(debt);
            line.setLineNumber(nextScheduleLineNumber(user, debt));
            line.setLineType(lineType);
            line.setTitle(scheduleTitle(debt, mode, scheduleLineNumberForMonth(debt, yearMonth)));
            line.setCurrency(defaultEnum(debt.getCurrency(), PersonalFinanceCurrency.PEN));
            line.setDueDate(dueDate);
            line.setStatus(dueDate.isBefore(LocalDate.now()) ? PersonalFinanceObligationStatus.OVERDUE : PersonalFinanceObligationStatus.PENDING);
            applyScheduleAmounts(line, debt, mode);
            line.setNotes(scheduleNotes(debt, mode));
            debtScheduleLineRepository.save(line);
            created++;
        }
        return created;
    }

    private int generateDebtScheduleObligationsForMonth(UserAccount user, YearMonth yearMonth) {
        LocalDate start = yearMonth.atDay(1);
        LocalDate end = yearMonth.atEndOfMonth();
        List<PersonalFinanceDebtScheduleLine> lines = debtScheduleLineRepository.findByUserAndDueDateBetweenOrderByDueDateAscLineNumberAscIdAsc(user, start, end);
        int created = 0;
        for (PersonalFinanceDebtScheduleLine line : lines) {
            if (line.getDebt() == null || line.getDebt().isDelinquentTracking() || !line.getDebt().isAutoGenerateMonthly()) {
                continue;
            }
            if (line.getStatus() == PersonalFinanceObligationStatus.CANCELLED || line.isPaidLike()) {
                continue;
            }
            if (line.getGeneratedObligationId() != null && paymentObligationRepository.findByIdAndUser(line.getGeneratedObligationId(), user).isPresent()) {
                continue;
            }
            if (paymentObligationRepository.findByScheduleLineIdAndUser(line.getId(), user).isPresent()) {
                continue;
            }
            PersonalFinancePaymentObligation obligation = new PersonalFinancePaymentObligation();
            obligation.setUser(user);
            obligation.setSourceType(sourceTypeForLine(line));
            obligation.setSourceId(line.getDebt().getId());
            obligation.setScheduleLineId(line.getId());
            obligation.setGroup(PersonalFinanceObligationGroup.DEBT_PAYMENT);
            obligation.setTitle(line.getTitle());
            obligation.setAmountDue(line.calculatedTotal());
            obligation.setAmountPaid(defaultAmount(line.getPaidAmount()));
            obligation.setCurrency(line.getCurrency());
            obligation.setDueDate(line.getDueDate());
            obligation.setStatus(line.getDueDate() != null && line.getDueDate().isBefore(LocalDate.now()) ? PersonalFinanceObligationStatus.OVERDUE : line.getStatus());
            obligation.setPriority(defaultEnum(line.getDebt().getPriority(), PersonalFinancePriority.MEDIUM));
            obligation.setNotes(scheduleObligationNotes(line));
            PersonalFinancePaymentObligation saved = paymentObligationRepository.save(obligation);
            line.setGeneratedObligationId(saved.getId());
            debtScheduleLineRepository.save(line);
            created++;
        }
        return created;
    }

    private int generateSimpleDebtObligationsForMonth(UserAccount user, YearMonth yearMonth) {
        LocalDate start = yearMonth.atDay(1);
        LocalDate end = yearMonth.atEndOfMonth();
        int created = 0;
        for (PersonalFinanceDebt debt : debtsForMonthlyPlan(user)) {
            PersonalFinanceDebtScheduleMode mode = defaultEnum(debt.getScheduleMode(), inferScheduleMode(debt));
            if (!debt.isAutoGenerateMonthly() || debt.usesGeneratedSchedule() || mode == PersonalFinanceDebtScheduleMode.TRACKING_ONLY) {
                continue;
            }
            BigDecimal monthlyPressure = debt.monthlyPressure();
            if (monthlyPressure.compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }
            if (!debtScheduleAppliesToMonth(debt, mode, yearMonth)) {
                continue;
            }
            if (paymentObligationRepository.existsByUserAndSourceTypeAndSourceIdAndDueDateBetween(user, PersonalFinanceObligationSourceType.DEBT, debt.getId(), start, end)) {
                continue;
            }
            PersonalFinancePaymentObligation obligation = new PersonalFinancePaymentObligation();
            obligation.setUser(user);
            obligation.setSourceType(PersonalFinanceObligationSourceType.DEBT);
            obligation.setSourceId(debt.getId());
            obligation.setGroup(PersonalFinanceObligationGroup.DEBT_PAYMENT);
            obligation.setTitle(debt.getName());
            obligation.setAmountDue(monthlyPressure);
            obligation.setAmountPaid(BigDecimal.ZERO);
            obligation.setCurrency(defaultEnum(debt.getCurrency(), PersonalFinanceCurrency.PEN));
            obligation.setDueDate(dateFromDueDay(yearMonth, debt.getDueDay() == null ? yearMonth.lengthOfMonth() : debt.getDueDay()));
            obligation.setStatus(obligation.getDueDate() != null && obligation.getDueDate().isBefore(LocalDate.now()) ? PersonalFinanceObligationStatus.OVERDUE : obligationStatusFromDebt(debt));
            obligation.setPriority(defaultEnum(debt.getPriority(), PersonalFinancePriority.MEDIUM));
            obligation.setNotes(generatedSimpleDebtNotes(debt));
            paymentObligationRepository.save(obligation);
            created++;
        }
        return created;
    }

    private List<PersonalFinanceDelinquentDebtItem> delinquentDebtsForPlan(UserAccount user) {
        return debtRepository.findByUserOrderByStatusAscDueDayAscNameAsc(user).stream()
                .filter(PersonalFinanceDebt::isDelinquentTracking)
                .map(debt -> new PersonalFinanceDelinquentDebtItem(
                        debt.getId(),
                        debt.getName(),
                        debt.getCreditorName(),
                        defaultEnum(debt.getCurrency(), PersonalFinanceCurrency.PEN),
                        safe(debt.getCurrentBalance()),
                        safe(debt.getPreviousMonthlyPayment()),
                        defaultEnum(debt.getStatus(), PersonalFinanceDebtStatus.STOPPED_PAYMENT),
                        defaultEnum(debt.getPriority(), PersonalFinancePriority.MEDIUM),
                        defaultEnum(debt.getCollectionStatus(), PersonalFinanceCollectionStatus.NONE),
                        defaultEnum(debt.getNegotiationStatus(), PersonalFinanceNegotiationStatus.NOT_STARTED),
                        debt.getLastPaymentDate(),
                        debt.getDelinquencyStartDate(),
                        debt.overdueDays(),
                        debt.getNextReviewDate(),
                        debt.getContactName(),
                        debt.getNotes()
                ))
                .sorted(Comparator
                        .comparing(PersonalFinanceDelinquentDebtItem::reviewDue).reversed()
                        .thenComparing(PersonalFinanceDelinquentDebtItem::priority)
                        .thenComparing(PersonalFinanceDelinquentDebtItem::overdueDays, Comparator.reverseOrder())
                        .thenComparing(PersonalFinanceDelinquentDebtItem::name, Comparator.nullsLast(String::compareToIgnoreCase)))
                .toList();
    }

    private List<PersonalFinanceDebt> debtsForMonthlyPlan(UserAccount user) {
        return debtRepository.findByUserOrderByStatusAscDueDayAscNameAsc(user).stream()
                .filter(debt -> debt.getStatus() == PersonalFinanceDebtStatus.ACTIVE
                        || debt.getStatus() == PersonalFinanceDebtStatus.OVERDUE
                        || debt.getStatus() == PersonalFinanceDebtStatus.STOPPED_PAYMENT
                        || debt.getStatus() == PersonalFinanceDebtStatus.COLLECTION
                        || debt.getStatus() == PersonalFinanceDebtStatus.PENDING_NEGOTIATION
                        || debt.getStatus() == PersonalFinanceDebtStatus.NEGOTIATION
                        || debt.getStatus() == PersonalFinanceDebtStatus.REPROGRAMMED
                        || debt.getStatus() == PersonalFinanceDebtStatus.SUSPENDED)
                .toList();
    }

    private PersonalFinanceMonthlyPlanItem itemFromObligation(
            PersonalFinancePaymentObligation obligation,
            PersonalFinanceDebt debt
    ) {
        boolean debtBalanceKnown = debt != null && debt.hasKnownBalance();
        BigDecimal debtBalance = debt == null ? BigDecimal.ZERO : debt.outstandingBalance();
        PersonalFinanceDebtClassification classification = debt == null
                ? PersonalFinanceDebtClassification.MANUAL_COMMITMENT
                : debt.classification();
        boolean settlementOpportunity = debtBalanceKnown
                && debtBalance.compareTo(BigDecimal.ZERO) > 0
                && safe(obligation.getAmountDue()).compareTo(debtBalance) >= 0;

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
                obligation.getSourceType() != PersonalFinanceObligationSourceType.MANUAL
                        && obligation.getSourceType() != PersonalFinanceObligationSourceType.STUDY_CYCLE
                        && obligation.getSourceType() != PersonalFinanceObligationSourceType.LIFE_COST
                        && obligation.getSourceType() != PersonalFinanceObligationSourceType.DEBT_VOLUNTARY_PAYMENT,
                debt == null ? null : debt.getId(),
                debt == null ? null : debt.getName(),
                classification,
                debtBalance,
                debtBalanceKnown,
                debt != null && debt.isBankBalanceReference(),
                settlementOpportunity
        );
    }

    private PersonalFinanceDebt linkedDebtForObligation(
            PersonalFinancePaymentObligation obligation,
            Map<Long, PersonalFinanceDebt> debtsById
    ) {
        if (obligation == null
                || obligation.getGroup() != PersonalFinanceObligationGroup.DEBT_PAYMENT
                || obligation.getSourceId() == null) {
            return null;
        }
        PersonalFinanceObligationSourceType sourceType = defaultEnum(
                obligation.getSourceType(),
                PersonalFinanceObligationSourceType.MANUAL
        );
        if (sourceType == PersonalFinanceObligationSourceType.DEBT
                || sourceType == PersonalFinanceObligationSourceType.DEBT_SCHEDULE
                || sourceType == PersonalFinanceObligationSourceType.PRIVATE_LENDER_INTEREST
                || sourceType == PersonalFinanceObligationSourceType.AUTO_DEDUCTION
                || sourceType == PersonalFinanceObligationSourceType.DEBT_VOLUNTARY_PAYMENT) {
            return debtsById.get(obligation.getSourceId());
        }
        return null;
    }

    private PersonalFinanceDebtPortfolioSummary debtPortfolioSummary(
            List<PersonalFinanceDebt> debts,
            List<PersonalFinanceMonthlyPlanItem> debtItems
    ) {
        BigDecimal bankPen = BigDecimal.ZERO;
        BigDecimal lenderPen = BigDecimal.ZERO;
        BigDecimal directPen = BigDecimal.ZERO;
        BigDecimal commitmentPen = BigDecimal.ZERO;
        BigDecimal otherPen = BigDecimal.ZERO;
        BigDecimal knownPen = BigDecimal.ZERO;
        BigDecimal knownUsd = BigDecimal.ZERO;
        long knownCount = 0;
        long undefinedCount = 0;

        for (PersonalFinanceDebt debt : debts) {
            if (!debt.hasKnownBalance()) {
                undefinedCount++;
                continue;
            }
            knownCount++;
            BigDecimal balance = debt.outstandingBalance();
            PersonalFinanceCurrency currency = defaultEnum(debt.getCurrency(), PersonalFinanceCurrency.PEN);
            if (currency == PersonalFinanceCurrency.USD) {
                knownUsd = knownUsd.add(balance);
                continue;
            }
            knownPen = knownPen.add(balance);
            PersonalFinanceDebtClassification classification = debt.classification();
            if (classification.isBankRelated()) {
                bankPen = bankPen.add(balance);
            } else if (classification.isLender()) {
                lenderPen = lenderPen.add(balance);
            } else if (classification.isDirect()) {
                directPen = directPen.add(balance);
            } else if (classification.isCommitment()) {
                commitmentPen = commitmentPen.add(balance);
            } else {
                otherPen = otherPen.add(balance);
            }
        }

        Set<Long> opportunityDebtIds = new HashSet<>();
        for (PersonalFinanceMonthlyPlanItem item : debtItems) {
            if (item.debtId() != null && item.settlementOpportunity()) {
                opportunityDebtIds.add(item.debtId());
            }
        }

        return new PersonalFinanceDebtPortfolioSummary(
                bankPen,
                lenderPen,
                directPen,
                commitmentPen,
                otherPen,
                knownPen,
                knownUsd,
                knownCount,
                undefinedCount,
                opportunityDebtIds.size()
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

    private Comparator<PersonalFinanceMonthlyPlanItem> priorityComparator() {
        return Comparator
                .comparingInt(this::priorityBucket)
                .thenComparing(item -> item.status() != PersonalFinanceObligationStatus.OVERDUE)
                .thenComparing(PersonalFinanceMonthlyPlanItem::dueDate, Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(PersonalFinanceMonthlyPlanItem::priority)
                .thenComparing(PersonalFinanceMonthlyPlanItem::title, Comparator.nullsLast(String::compareToIgnoreCase));
    }

    private int priorityBucket(PersonalFinanceMonthlyPlanItem item) {
        if (item.priority() == PersonalFinancePriority.OPTIONAL) {
            return 90;
        }
        if (item.sourceType() == PersonalFinanceObligationSourceType.AUTO_DEDUCTION) {
            return 0;
        }
        if (item.group() == PersonalFinanceObligationGroup.BASIC_LIVING) {
            return item.priority() == PersonalFinancePriority.CRITICAL || item.priority() == PersonalFinancePriority.HIGH ? 1 : 3;
        }
        if (item.group() == PersonalFinanceObligationGroup.STUDY) {
            return item.priority() == PersonalFinancePriority.CRITICAL || item.priority() == PersonalFinancePriority.HIGH ? 2 : 4;
        }
        if (item.group() == PersonalFinanceObligationGroup.DEBT_PAYMENT) {
            if (item.status() == PersonalFinanceObligationStatus.OVERDUE) {
                return 5;
            }
            return switch (item.priority()) {
                case CRITICAL -> 6;
                case HIGH -> 7;
                case MEDIUM -> 9;
                case LOW -> 10;
                case OPTIONAL -> 90;
            };
        }
        return item.priority() == PersonalFinancePriority.CRITICAL || item.priority() == PersonalFinancePriority.HIGH ? 8 : 11;
    }

    private boolean isEssential(PersonalFinanceMonthlyPlanItem item) {
        return item.priority() != PersonalFinancePriority.OPTIONAL
                && (item.group() == PersonalFinanceObligationGroup.BASIC_LIVING
                || item.group() == PersonalFinanceObligationGroup.STUDY);
    }

    private PersonalFinanceAllocationStatus allocationStatus(BigDecimal recommendedAmount, BigDecimal pendingAmount) {
        if (recommendedAmount.compareTo(pendingAmount) >= 0) {
            return PersonalFinanceAllocationStatus.COVERED;
        }
        if (recommendedAmount.compareTo(BigDecimal.ZERO) > 0) {
            return PersonalFinanceAllocationStatus.PARTIAL;
        }
        return PersonalFinanceAllocationStatus.UNFUNDED;
    }

    private String allocationReason(PersonalFinanceMonthlyPlanItem item) {
        if (item.sourceType() == PersonalFinanceObligationSourceType.AUTO_DEDUCTION) {
            return "Descuento automático: reserva este importe antes de distribuir el resto.";
        }
        if (item.group() == PersonalFinanceObligationGroup.BASIC_LIVING) {
            return "Costo de vida básico: protege vivienda, servicios, salud o continuidad diaria.";
        }
        if (item.group() == PersonalFinanceObligationGroup.STUDY) {
            return "Estudio programado: cubre solo lo que vence este mes y evita financiarlo con deuda cara.";
        }
        if (item.status() == PersonalFinanceObligationStatus.OVERDUE) {
            return "Pago vencido: revisa mora, penalidad y posibilidad de negociación antes de pagar.";
        }
        if (item.group() == PersonalFinanceObligationGroup.DEBT_PAYMENT
                && (item.priority() == PersonalFinancePriority.CRITICAL || item.priority() == PersonalFinancePriority.HIGH)) {
            return "Deuda prioritaria: atiéndela después del costo básico y confirma que el pago reduzca el riesgo o el saldo.";
        }
        if (item.priority() == PersonalFinancePriority.OPTIONAL) {
            return "Compromiso opcional: posterga mientras el mes tenga déficit.";
        }
        if (item.group() == PersonalFinanceObligationGroup.DEBT_PAYMENT) {
            return "Deuda de prioridad media o baja: negocia fecha o monto si no queda efectivo.";
        }
        return "Compromiso postergable: revisa si puede moverse al siguiente mes sin generar un costo mayor.";
    }

    private PersonalFinanceHealthLevel healthLevel(
            BigDecimal totalPending,
            BigDecimal availableCash,
            BigDecimal essentialGap,
            BigDecimal unfundedTotal,
            BigDecimal unfundedDebtTotal,
            BigDecimal debtPressurePercentage
    ) {
        if (totalPending.compareTo(BigDecimal.ZERO) <= 0 || unfundedTotal.compareTo(BigDecimal.ZERO) <= 0) {
            return PersonalFinanceHealthLevel.STABLE;
        }
        if (availableCash.compareTo(BigDecimal.ZERO) <= 0 || essentialGap.compareTo(BigDecimal.ZERO) > 0) {
            return PersonalFinanceHealthLevel.CRITICAL;
        }
        if (unfundedDebtTotal.compareTo(BigDecimal.ZERO) > 0 || debtPressurePercentage.compareTo(new BigDecimal("50.0")) >= 0) {
            return PersonalFinanceHealthLevel.RED;
        }
        return PersonalFinanceHealthLevel.TIGHT;
    }

    private String healthHeadline(PersonalFinanceHealthLevel healthLevel) {
        return switch (healthLevel) {
            case STABLE -> "El dinero disponible cubre los compromisos pendientes del mes.";
            case TIGHT -> "El mes está ajustado y requiere postergar compromisos no esenciales.";
            case RED -> "El costo básico puede cubrirse, pero no alcanza para todas las deudas.";
            case CRITICAL -> "El dinero disponible no alcanza para cubrir el costo básico del mes.";
        };
    }

    private String healthRecommendation(PersonalFinanceHealthLevel healthLevel) {
        return switch (healthLevel) {
            case STABLE -> "Mantén una reserva antes de adelantar deuda y registra cada pago para conservar una proyección real.";
            case TIGHT -> "Cubre el orden sugerido y posterga lo opcional. No agregues nuevas cuotas sin liberar flujo mensual.";
            case RED -> "Paga primero el costo básico y las deudas críticas o vencidas. Negocia el resto antes de usar otro préstamo.";
            case CRITICAL -> "Protege vivienda, servicios, salud y transporte. Reduce o negocia pagos de deuda antes de asumir nueva deuda.";
        };
    }

    private BigDecimal percentage(BigDecimal amount, BigDecimal base) {
        BigDecimal safeBase = safe(base);
        if (safeBase.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }
        return safe(amount)
                .multiply(new BigDecimal("100"))
                .divide(safeBase, 1, RoundingMode.HALF_UP);
    }

    private BigDecimal nonNegative(BigDecimal value) {
        return safe(value).max(BigDecimal.ZERO);
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

    private void cancelGeneratedObligationsForTrackingDebt(UserAccount user, PersonalFinanceDebt debt) {
        List<PersonalFinanceObligationSourceType> generatedSources = List.of(
                PersonalFinanceObligationSourceType.DEBT,
                PersonalFinanceObligationSourceType.DEBT_SCHEDULE,
                PersonalFinanceObligationSourceType.PRIVATE_LENDER_INTEREST,
                PersonalFinanceObligationSourceType.AUTO_DEDUCTION
        );
        for (PersonalFinancePaymentObligation obligation : paymentObligationRepository.findByUserAndSourceIdAndSourceTypeIn(user, debt.getId(), generatedSources)) {
            if (obligation.getStatus() == PersonalFinanceObligationStatus.PAID
                    || safe(obligation.getAmountPaid()).compareTo(BigDecimal.ZERO) > 0
                    || obligation.getStatus() == PersonalFinanceObligationStatus.CANCELLED) {
                continue;
            }
            obligation.setStatus(PersonalFinanceObligationStatus.CANCELLED);
            String existingNotes = clean(obligation.getNotes());
            obligation.setNotes(existingNotes == null || existingNotes.isBlank()
                    ? "Cancelada automáticamente al pasar la deuda a seguimiento de mora"
                    : existingNotes + " · Cancelada automáticamente al pasar la deuda a seguimiento de mora");
            paymentObligationRepository.save(obligation);
        }
    }

    private void normalizeTrackingDebt(PersonalFinanceDebt debt) {
        boolean trackingStatus = debt.getStatus() == PersonalFinanceDebtStatus.STOPPED_PAYMENT
                || debt.getStatus() == PersonalFinanceDebtStatus.COLLECTION
                || debt.getStatus() == PersonalFinanceDebtStatus.PENDING_NEGOTIATION
                || debt.getStatus() == PersonalFinanceDebtStatus.NEGOTIATION;
        if (trackingStatus) {
            debt.setScheduleMode(PersonalFinanceDebtScheduleMode.TRACKING_ONLY);
            debt.setAutoGenerateMonthly(false);
            debt.setFixedPayment(false);
        }
        if (debt.getScheduleMode() == PersonalFinanceDebtScheduleMode.TRACKING_ONLY) {
            debt.setAutoGenerateMonthly(false);
            debt.setFixedPayment(false);
        }
    }

    private PersonalFinanceDebtScheduleMode inferScheduleMode(PersonalFinanceDebt debt) {
        if (debt.getDebtType() == PersonalFinanceDebtType.PRIVATE_LENDER && safe(debt.getInterestRateMonthly()).compareTo(BigDecimal.ZERO) > 0) {
            return PersonalFinanceDebtScheduleMode.PRIVATE_LENDER_INTEREST;
        }
        if (debt.getDebtType() == PersonalFinanceDebtType.BANK_THIRD_PARTY) {
            return PersonalFinanceDebtScheduleMode.BANK_SCHEDULE;
        }
        if (debt.getDebtType() == PersonalFinanceDebtType.RECURRING_COMMITMENT && debt.getHolderType() == PersonalFinanceDebtHolderType.OWN_NAME) {
            return PersonalFinanceDebtScheduleMode.AUTO_DEDUCTION;
        }
        return PersonalFinanceDebtScheduleMode.SIMPLE_MONTHLY;
    }

    private int defaultScheduleCount(PersonalFinanceDebt debt, PersonalFinanceDebtScheduleMode mode) {
        if (debt.getInstallmentCount() != null && debt.getInstallmentCount() > 0) {
            return debt.getInstallmentCount();
        }
        if (mode == PersonalFinanceDebtScheduleMode.BANK_SCHEDULE || mode == PersonalFinanceDebtScheduleMode.AUTO_DEDUCTION) {
            return 12;
        }
        if (mode == PersonalFinanceDebtScheduleMode.PRIVATE_LENDER_INTEREST) {
            return 6;
        }
        return 3;
    }

    private LocalDate firstDueDate(PersonalFinanceDebt debt, YearMonth fromMonth) {
        if (debt.getScheduleStartDate() != null) {
            return debt.getScheduleStartDate();
        }
        return dateFromDueDay(fromMonth, debt.getDueDay() == null ? LocalDate.now().getDayOfMonth() : debt.getDueDay());
    }

    private PersonalFinanceScheduleLineType lineTypeForMode(PersonalFinanceDebtScheduleMode mode) {
        return switch (mode) {
            case PRIVATE_LENDER_INTEREST -> PersonalFinanceScheduleLineType.LENDER_INSTALLMENT;
            case ONE_TIME -> PersonalFinanceScheduleLineType.ONE_TIME;
            case AUTO_DEDUCTION -> PersonalFinanceScheduleLineType.AUTO_DEDUCTION;
            default -> PersonalFinanceScheduleLineType.INSTALLMENT;
        };
    }

    private String scheduleTitle(PersonalFinanceDebt debt, PersonalFinanceDebtScheduleMode mode, int number) {
        String prefix = switch (mode) {
            case PRIVATE_LENDER_INTEREST -> "Cuota prestamista";
            case ONE_TIME -> "Pago único";
            case AUTO_DEDUCTION -> "Descuento automático";
            case BANK_SCHEDULE -> "Cuota bancaria";
            default -> "Cuota";
        };
        return prefix + " - " + debt.getName() + (mode == PersonalFinanceDebtScheduleMode.ONE_TIME ? "" : " #" + number);
    }

    private void applyScheduleAmounts(PersonalFinanceDebtScheduleLine line, PersonalFinanceDebt debt, PersonalFinanceDebtScheduleMode mode) {
        if (mode == PersonalFinanceDebtScheduleMode.PRIVATE_LENDER_INTEREST) {
            line.setInterestAmount(debt.monthlyInterestAmount().setScale(2, RoundingMode.HALF_UP));
            line.setPrincipalAmount(BigDecimal.ZERO);
            line.setTotalAmount(debt.monthlyInterestAmount().setScale(2, RoundingMode.HALF_UP));
            return;
        }
        if (mode == PersonalFinanceDebtScheduleMode.ONE_TIME) {
            BigDecimal amount = safe(debt.getMonthlyDueAmount()).compareTo(BigDecimal.ZERO) > 0 ? debt.getMonthlyDueAmount() : debt.getCurrentBalance();
            line.setPrincipalAmount(safe(amount));
            line.setInterestAmount(BigDecimal.ZERO);
            line.setTotalAmount(safe(amount));
            return;
        }
        BigDecimal amount = safe(debt.getMonthlyDueAmount()).compareTo(BigDecimal.ZERO) > 0 ? debt.getMonthlyDueAmount() : debt.monthlyPressure();
        line.setPrincipalAmount(safe(amount));
        line.setInterestAmount(BigDecimal.ZERO);
        line.setTotalAmount(safe(amount));
    }

    private PersonalFinanceObligationSourceType sourceTypeForLine(PersonalFinanceDebtScheduleLine line) {
        if (line.getLineType() == PersonalFinanceScheduleLineType.INTEREST
                || line.getLineType() == PersonalFinanceScheduleLineType.LENDER_INSTALLMENT) {
            return PersonalFinanceObligationSourceType.PRIVATE_LENDER_INTEREST;
        }
        if (line.getLineType() == PersonalFinanceScheduleLineType.AUTO_DEDUCTION) {
            return PersonalFinanceObligationSourceType.AUTO_DEDUCTION;
        }
        return PersonalFinanceObligationSourceType.DEBT_SCHEDULE;
    }

    private String scheduleNotes(PersonalFinanceDebt debt, PersonalFinanceDebtScheduleMode mode) {
        List<String> notes = new ArrayList<>();
        notes.add(mode.getLabel());
        if (debt.getInterestRateMonthly() != null && debt.getInterestRateMonthly().compareTo(BigDecimal.ZERO) > 0) {
            notes.add("Interés mensual " + debt.getInterestRateMonthly() + "%");
        }
        if (mode == PersonalFinanceDebtScheduleMode.PRIVATE_LENDER_INTEREST) {
            notes.add("Solo interés mensual; genera un cronograma con número de cuotas para amortizar capital");
        }
        String debtNotes = debtPlanNotes(debt);
        if (debtNotes != null && !debtNotes.isBlank()) {
            notes.add(debtNotes);
        }
        return String.join(" · ", notes);
    }

    private String scheduleObligationNotes(PersonalFinanceDebtScheduleLine line) {
        List<String> notes = new ArrayList<>();
        notes.add("Generado desde cronograma");
        if (line.getDebt() != null) {
            notes.add(line.getDebt().getDebtType().getLabel());
            String debtNotes = debtPlanNotes(line.getDebt());
            if (debtNotes != null && !debtNotes.isBlank()) {
                notes.add(debtNotes);
            }
        }
        if (line.getNotes() != null && !line.getNotes().isBlank()) {
            notes.add(line.getNotes());
        }
        return String.join(" · ", notes);
    }

    private boolean appliesToMonth(YearMonth yearMonth, LocalDate startDate, LocalDate endDate) {
        if (startDate != null && YearMonth.from(startDate).isAfter(yearMonth)) {
            return false;
        }
        return endDate == null || !YearMonth.from(endDate).isBefore(yearMonth);
    }

    private boolean debtScheduleAppliesToMonth(PersonalFinanceDebt debt, PersonalFinanceDebtScheduleMode mode, YearMonth yearMonth) {
        if (!appliesToMonth(yearMonth, debt.getScheduleStartDate(), debt.getScheduleEndDate())) {
            return false;
        }
        if (mode == PersonalFinanceDebtScheduleMode.ONE_TIME && debt.getScheduleStartDate() != null) {
            return YearMonth.from(debt.getScheduleStartDate()).equals(yearMonth);
        }
        if (debt.getScheduleStartDate() != null && debt.getInstallmentCount() != null && debt.getInstallmentCount() > 0) {
            long monthIndex = ChronoUnit.MONTHS.between(YearMonth.from(debt.getScheduleStartDate()), yearMonth);
            return monthIndex >= 0 && monthIndex < debt.getInstallmentCount();
        }
        return true;
    }

    private LocalDate debtDueDateForMonth(PersonalFinanceDebt debt, YearMonth yearMonth, PersonalFinanceDebtScheduleMode mode) {
        if (mode == PersonalFinanceDebtScheduleMode.ONE_TIME && debt.getScheduleStartDate() != null) {
            return debt.getScheduleStartDate();
        }
        Integer dueDay = debt.getDueDay();
        if (dueDay == null && debt.getScheduleStartDate() != null) {
            dueDay = debt.getScheduleStartDate().getDayOfMonth();
        }
        return dateFromDueDay(yearMonth, dueDay == null ? yearMonth.lengthOfMonth() : dueDay);
    }

    private int scheduleLineNumberForMonth(PersonalFinanceDebt debt, YearMonth yearMonth) {
        if (debt.getScheduleStartDate() == null) {
            return 1;
        }
        long index = ChronoUnit.MONTHS.between(YearMonth.from(debt.getScheduleStartDate()), yearMonth);
        return (int) Math.max(1, index + 1);
    }

    private PersonalFinanceBankScheduleSummary summarizeBankSchedule(List<PersonalFinanceDebtScheduleLine> lines) {
        int paidLines = 0;
        int pendingLines = 0;
        int overdueLines = 0;
        BigDecimal principalTotal = BigDecimal.ZERO;
        BigDecimal interestTotal = BigDecimal.ZERO;
        BigDecimal insuranceTotal = BigDecimal.ZERO;
        BigDecimal feeTotal = BigDecimal.ZERO;
        BigDecimal scheduledTotal = BigDecimal.ZERO;
        BigDecimal principalPaid = BigDecimal.ZERO;
        BigDecimal principalPending = BigDecimal.ZERO;
        BigDecimal futureTotal = BigDecimal.ZERO;
        BigDecimal futureInterest = BigDecimal.ZERO;
        PersonalFinanceDebtScheduleLine nextInstallment = null;

        for (PersonalFinanceDebtScheduleLine line : lines) {
            principalTotal = principalTotal.add(safe(line.getPrincipalAmount()));
            interestTotal = interestTotal.add(safe(line.getInterestAmount()));
            insuranceTotal = insuranceTotal.add(safe(line.getInsuranceAmount()));
            feeTotal = feeTotal.add(safe(line.getFeeAmount()));
            scheduledTotal = scheduledTotal.add(line.calculatedTotal());
            boolean paid = line.isPaidLike();
            boolean cancelled = line.getStatus() == PersonalFinanceObligationStatus.CANCELLED;
            if (!cancelled) {
                principalPaid = principalPaid.add(safe(line.getPaidPrincipalAmount()));
                principalPending = principalPending.add(line.principalPendingAmount());
            }
            if (paid) {
                paidLines++;
            } else if (!cancelled) {
                pendingLines++;
                futureTotal = futureTotal.add(line.pendingAmount());
                futureInterest = futureInterest.add(line.interestPendingAmount());
                if (line.getDueDate() != null && line.getDueDate().isBefore(LocalDate.now())) {
                    overdueLines++;
                }
                if (nextInstallment == null || compareSchedulePosition(line, nextInstallment) < 0) {
                    nextInstallment = line;
                }
            }
        }
        return new PersonalFinanceBankScheduleSummary(
                lines.size(), paidLines, pendingLines, overdueLines,
                principalTotal, interestTotal, insuranceTotal, feeTotal, scheduledTotal,
                principalPaid, principalPending, futureTotal, futureInterest, nextInstallment
        );
    }

    private int compareSchedulePosition(PersonalFinanceDebtScheduleLine left, PersonalFinanceDebtScheduleLine right) {
        if (left.getDueDate() == null && right.getDueDate() != null) return 1;
        if (left.getDueDate() != null && right.getDueDate() == null) return -1;
        if (left.getDueDate() != null) {
            int dateCompare = left.getDueDate().compareTo(right.getDueDate());
            if (dateCompare != 0) return dateCompare;
        }
        return Integer.compare(left.getLineNumber() == null ? Integer.MAX_VALUE : left.getLineNumber(),
                right.getLineNumber() == null ? Integer.MAX_VALUE : right.getLineNumber());
    }

    private List<ImportedBankScheduleRow> parseBankScheduleRows(String rawContent, List<String> errors) {
        List<ImportedBankScheduleRow> rows = new ArrayList<>();
        if (rawContent == null || rawContent.isBlank()) {
            errors.add("Pega el cronograma o selecciona un archivo CSV/TXT.");
            return rows;
        }
        if (rawContent.length() > 1_000_000) {
            errors.add("El archivo o texto supera el límite de 1 MB.");
            return rows;
        }
        Set<Integer> lineNumbers = new HashSet<>();
        String[] rawLines = rawContent.replace("\r", "").split("\n");
        for (int index = 0; index < rawLines.length; index++) {
            String rawLine = rawLines[index].trim();
            if (rawLine.isBlank() || rawLine.startsWith("#")) continue;
            String normalized = rawLine.toLowerCase(Locale.ROOT);
            if (normalized.contains("cuota") && normalized.contains("fecha")) continue;
            if (normalized.startsWith("totales") || normalized.startsWith("total")) continue;

            String[] columns = splitScheduleColumns(rawLine);
            if (columns.length < 6) {
                errors.add("Línea " + (index + 1) + ": se esperaban 6 columnas (cuota, fecha, capital, interés, seguro, total).");
                continue;
            }
            try {
                int lineNumber = Integer.parseInt(cleanCell(columns[0]));
                if (lineNumber <= 0) throw new IllegalArgumentException("número de cuota inválido");
                if (!lineNumbers.add(lineNumber)) throw new IllegalArgumentException("número de cuota duplicado");
                LocalDate dueDate = parseScheduleDate(cleanCell(columns[1]));
                BigDecimal principal = parseScheduleAmount(columns[2]);
                BigDecimal interest = parseScheduleAmount(columns[3]);
                BigDecimal insurance = parseScheduleAmount(columns[4]);
                BigDecimal total = parseScheduleAmount(columns[5]);
                BigDecimal calculated = principal.add(interest).add(insurance);
                if (total.compareTo(BigDecimal.ZERO) <= 0) {
                    total = calculated;
                }
                if (calculated.subtract(total).abs().compareTo(new BigDecimal("0.20")) > 0) {
                    throw new IllegalArgumentException("capital + interés + seguro no coincide con el total");
                }
                rows.add(new ImportedBankScheduleRow(lineNumber, dueDate, principal, interest, insurance, total));
            } catch (RuntimeException exception) {
                errors.add("Línea " + (index + 1) + ": " + exception.getMessage() + ".");
            }
        }
        rows.sort(Comparator.comparing(ImportedBankScheduleRow::lineNumber));
        return rows;
    }

    private String[] splitScheduleColumns(String rawLine) {
        if (rawLine.contains("\t")) return rawLine.split("\t+");
        if (rawLine.contains(";")) return rawLine.split(";");
        if (rawLine.contains("|") ) return rawLine.split("\\|");
        String[] commaColumns = rawLine.split(",");
        if (commaColumns.length >= 6 && rawLine.indexOf('/') >= 0) return commaColumns;
        return rawLine.trim().split("\\s+");
    }

    private String cleanCell(String value) {
        if (value == null) return "";
        String cleaned = value.trim();
        if (cleaned.startsWith("\"") && cleaned.endsWith("\"") && cleaned.length() >= 2) {
            cleaned = cleaned.substring(1, cleaned.length() - 1);
        }
        return cleaned.trim();
    }

    private LocalDate parseScheduleDate(String value) {
        List<DateTimeFormatter> formatters = List.of(
                DateTimeFormatter.ofPattern("d/M/uuuu"),
                DateTimeFormatter.ofPattern("d/M/uu"),
                DateTimeFormatter.ISO_LOCAL_DATE
        );
        for (DateTimeFormatter formatter : formatters) {
            try {
                return LocalDate.parse(value, formatter);
            } catch (DateTimeParseException ignored) {
            }
        }
        throw new IllegalArgumentException("fecha inválida: " + value);
    }

    private BigDecimal parseScheduleAmount(String value) {
        String cleaned = cleanCell(value)
                .replace("S/", "")
                .replace("PEN", "")
                .replace(" ", "")
                .replaceAll("[^0-9,.-]", "");
        if (cleaned.isBlank() || cleaned.equals("-")) return BigDecimal.ZERO;
        int lastComma = cleaned.lastIndexOf(',');
        int lastDot = cleaned.lastIndexOf('.');
        if (lastComma >= 0 && lastDot >= 0) {
            if (lastComma > lastDot) {
                cleaned = cleaned.replace(".", "").replace(',', '.');
            } else {
                cleaned = cleaned.replace(",", "");
            }
        } else if (lastComma >= 0) {
            cleaned = cleaned.replace(',', '.');
        }
        try {
            BigDecimal amount = new BigDecimal(cleaned).setScale(2, RoundingMode.HALF_UP);
            if (amount.compareTo(BigDecimal.ZERO) < 0) throw new IllegalArgumentException("monto negativo");
            return amount;
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("monto inválido: " + value);
        }
    }

    private PersonalFinanceDebtScheduleGenerationResult generatePrivateLenderAmortizationSchedule(
            UserAccount user,
            PersonalFinanceDebt debt,
            LocalDate firstDue,
            int count
    ) {
        List<PersonalFinanceLenderInstallment> installments = PersonalFinanceLenderAmortizationCalculator.calculate(
                safe(debt.getCurrentBalance()),
                safe(debt.getInterestRateMonthly()),
                count
        );
        List<PersonalFinanceDebtScheduleLine> existingLines =
                debtScheduleLineRepository.findByUserAndDebtOrderByDueDateAscLineNumberAscIdAsc(user, debt);
        Set<LocalDate> targetDates = new HashSet<>();
        int baseLineNumber = existingLines.stream()
                .filter(line -> line.getDueDate() != null && line.getDueDate().isBefore(firstDue))
                .map(PersonalFinanceDebtScheduleLine::getLineNumber)
                .filter(java.util.Objects::nonNull)
                .max(Integer::compareTo)
                .orElse(0);

        int created = 0;
        int updated = 0;
        int removed = 0;
        int protectedLines = 0;
        int unchanged = 0;

        for (PersonalFinanceLenderInstallment installment : installments) {
            YearMonth dueMonth = YearMonth.from(firstDue).plusMonths(installment.number() - 1L);
            LocalDate dueDate = dueMonth.atDay(Math.min(firstDue.getDayOfMonth(), dueMonth.lengthOfMonth()));
            targetDates.add(dueDate);

            List<PersonalFinanceDebtScheduleLine> matchingLines = existingLines.stream()
                    .filter(existing -> dueDate.equals(existing.getDueDate()))
                    .toList();
            PersonalFinanceDebtScheduleLine line = matchingLines.isEmpty() ? null : matchingLines.get(0);
            for (int duplicateIndex = 1; duplicateIndex < matchingLines.size(); duplicateIndex++) {
                PersonalFinanceDebtScheduleLine duplicate = matchingLines.get(duplicateIndex);
                if (hasRecordedSchedulePayment(duplicate, user)) {
                    protectedLines++;
                    continue;
                }
                removeGeneratedObligation(duplicate, user);
                debtScheduleLineRepository.delete(duplicate);
                removed++;
            }
            if (line != null && hasRecordedSchedulePayment(line, user)) {
                protectedLines++;
                continue;
            }

            boolean isNew = line == null;
            if (isNew) {
                line = new PersonalFinanceDebtScheduleLine();
                line.setUser(user);
                line.setDebt(debt);
                line.setLineNumber(baseLineNumber + installment.number());
                line.setPaidAmount(BigDecimal.ZERO);
            } else if (line.getLineNumber() == null) {
                line.setLineNumber(baseLineNumber + installment.number());
            }

            line.setLineType(PersonalFinanceScheduleLineType.LENDER_INSTALLMENT);
            line.setTitle("Cuota prestamista - " + debt.getName() + " #" + installment.number());
            line.setCurrency(defaultEnum(debt.getCurrency(), PersonalFinanceCurrency.PEN));
            line.setDueDate(dueDate);
            line.setPrincipalAmount(installment.principalAmount());
            line.setInterestAmount(installment.interestAmount());
            line.setInsuranceAmount(BigDecimal.ZERO);
            line.setFeeAmount(BigDecimal.ZERO);
            line.setTotalAmount(installment.totalAmount());
            line.setPaidAmount(BigDecimal.ZERO);
            line.setPaidAt(null);
            line.setStatus(defaultPendingStatus(dueDate));
            line.setNotes(lenderInstallmentNotes(debt, installment));

            PersonalFinanceDebtScheduleLine saved = debtScheduleLineRepository.save(line);
            synchronizeGeneratedObligation(saved, user);
            if (isNew) {
                created++;
            } else {
                updated++;
            }
        }

        for (PersonalFinanceDebtScheduleLine line : existingLines) {
            if (!isGeneratedLenderLine(line)
                    || line.getDueDate() == null
                    || line.getDueDate().isBefore(firstDue)
                    || targetDates.contains(line.getDueDate())) {
                continue;
            }
            if (hasRecordedSchedulePayment(line, user)) {
                protectedLines++;
                continue;
            }
            removeGeneratedObligation(line, user);
            debtScheduleLineRepository.delete(line);
            removed++;
        }

        debt.setScheduleMode(PersonalFinanceDebtScheduleMode.PRIVATE_LENDER_INTEREST);
        debt.setInstallmentCount(count);
        debt.setScheduleStartDate(firstDue);
        YearMonth lastMonth = YearMonth.from(firstDue).plusMonths(count - 1L);
        debt.setScheduleEndDate(lastMonth.atDay(Math.min(firstDue.getDayOfMonth(), lastMonth.lengthOfMonth())));
        debt.setMonthlyDueAmount(installments.get(0).totalAmount());
        debt.setFixedPayment(false);
        debt.setAutoGenerateMonthly(true);
        debtRepository.save(debt);

        return new PersonalFinanceDebtScheduleGenerationResult(
                created,
                updated,
                removed,
                protectedLines,
                unchanged,
                true
        );
    }

    private boolean isGeneratedLenderLine(PersonalFinanceDebtScheduleLine line) {
        if (line.getLineType() == PersonalFinanceScheduleLineType.LENDER_INSTALLMENT) {
            return true;
        }
        return line.getLineType() == PersonalFinanceScheduleLineType.INTEREST
                && line.getTitle() != null
                && line.getTitle().startsWith("Interés mensual");
    }

    private PersonalFinanceObligationStatus defaultPendingStatus(LocalDate dueDate) {
        return dueDate != null && dueDate.isBefore(LocalDate.now())
                ? PersonalFinanceObligationStatus.OVERDUE
                : PersonalFinanceObligationStatus.PENDING;
    }

    private String lenderInstallmentNotes(
            PersonalFinanceDebt debt,
            PersonalFinanceLenderInstallment installment
    ) {
        return "Capital fijo + interés sobre saldo pendiente"
                + " · Saldo inicial " + installment.openingBalance()
                + " · Capital " + installment.principalAmount()
                + " · Interés " + safe(debt.getInterestRateMonthly()) + "% = " + installment.interestAmount()
                + " · Saldo final " + installment.closingBalance();
    }

    private boolean hasRecordedSchedulePayment(PersonalFinanceDebtScheduleLine line, UserAccount user) {
        if (line.isPaidLike() || safe(line.getPaidAmount()).compareTo(BigDecimal.ZERO) > 0
                || line.getStatus() == PersonalFinanceObligationStatus.PARTIAL) {
            return true;
        }
        return paymentObligationRepository.findByScheduleLineIdAndUser(line.getId(), user)
                .map(obligation -> obligation.isPaidLike() || safe(obligation.getAmountPaid()).compareTo(BigDecimal.ZERO) > 0
                        || obligation.getStatus() == PersonalFinanceObligationStatus.PARTIAL)
                .orElse(false);
    }

    private void removeGeneratedObligation(PersonalFinanceDebtScheduleLine line, UserAccount user) {
        paymentObligationRepository.findByScheduleLineIdAndUser(line.getId(), user).ifPresent(paymentObligationRepository::delete);
        line.setGeneratedObligationId(null);
    }

    private void synchronizeGeneratedObligation(PersonalFinanceDebtScheduleLine line, UserAccount user) {
        paymentObligationRepository.findByScheduleLineIdAndUser(line.getId(), user).ifPresent(obligation -> {
            obligation.setSourceType(sourceTypeForLine(line));
            obligation.setSourceId(line.getDebt() == null ? obligation.getSourceId() : line.getDebt().getId());
            obligation.setTitle(line.getTitle());
            obligation.setAmountDue(line.calculatedTotal());
            obligation.setAmountPaid(defaultAmount(line.getPaidAmount()));
            obligation.setCurrency(line.getCurrency());
            obligation.setDueDate(line.getDueDate());
            obligation.setStatus(resolveObligationStatus(line.getStatus(), obligation.getAmountDue(), obligation.getAmountPaid()));
            obligation.setNotes(scheduleObligationNotes(line));
            if (line.getDebt() != null) {
                obligation.setPriority(defaultEnum(line.getDebt().getPriority(), PersonalFinancePriority.MEDIUM));
            }
            if (line.getStatus() == PersonalFinanceObligationStatus.CANCELLED) {
                obligation.setStatus(PersonalFinanceObligationStatus.CANCELLED);
            }
            paymentObligationRepository.save(obligation);
            line.setGeneratedObligationId(obligation.getId());
        });
    }

    private void refreshDebtBalanceFromSchedule(PersonalFinanceDebt debt, UserAccount user) {
        if (debt == null || debt.getId() == null) {
            return;
        }
        List<PersonalFinanceDebtScheduleLine> lines =
                debtScheduleLineRepository.findByUserAndDebtOrderByDueDateAscLineNumberAscIdAsc(user, debt);
        if (lines.isEmpty()) {
            return;
        }
        PersonalFinanceDebtScheduleMode mode = defaultEnum(debt.getScheduleMode(), PersonalFinanceDebtScheduleMode.SIMPLE_MONTHLY);
        if (mode == PersonalFinanceDebtScheduleMode.BANK_SCHEDULE) {
            updateDebtFromBankSchedule(debt, lines);
            return;
        }
        if (mode != PersonalFinanceDebtScheduleMode.PRIVATE_LENDER_INTEREST
                && mode != PersonalFinanceDebtScheduleMode.ONE_TIME) {
            return;
        }

        BigDecimal principalTotal = lines.stream()
                .filter(line -> line.getStatus() != PersonalFinanceObligationStatus.CANCELLED)
                .map(PersonalFinanceDebtScheduleLine::getPrincipalAmount)
                .map(this::safe)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        if (principalTotal.compareTo(BigDecimal.ZERO) <= 0) {
            return;
        }
        BigDecimal principalPending = lines.stream()
                .filter(line -> line.getStatus() != PersonalFinanceObligationStatus.CANCELLED)
                .map(PersonalFinanceDebtScheduleLine::principalPendingAmount)
                .map(this::safe)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        debt.setCurrentBalance(principalPending);
        if (safe(debt.getOriginalAmount()).compareTo(BigDecimal.ZERO) <= 0 && principalTotal.compareTo(BigDecimal.ZERO) > 0) {
            debt.setOriginalAmount(principalTotal);
        }
        debtRepository.save(debt);
    }

    private void updateDebtFromBankSchedule(PersonalFinanceDebt debt, List<PersonalFinanceDebtScheduleLine> lines) {
        if (lines == null || lines.isEmpty()) return;
        List<PersonalFinanceDebtScheduleLine> sorted = lines.stream().sorted(this::compareSchedulePosition).toList();
        PersonalFinanceBankScheduleSummary summary = summarizeBankSchedule(sorted);
        debt.setScheduleMode(PersonalFinanceDebtScheduleMode.BANK_SCHEDULE);
        debt.setAutoGenerateMonthly(true);
        debt.setFixedPayment(true);
        debt.setInstallmentCount(sorted.stream().map(PersonalFinanceDebtScheduleLine::getLineNumber).filter(java.util.Objects::nonNull).max(Integer::compareTo).orElse(sorted.size()));
        debt.setScheduleStartDate(sorted.stream().map(PersonalFinanceDebtScheduleLine::getDueDate).filter(java.util.Objects::nonNull).min(LocalDate::compareTo).orElse(debt.getScheduleStartDate()));
        debt.setScheduleEndDate(sorted.stream().map(PersonalFinanceDebtScheduleLine::getDueDate).filter(java.util.Objects::nonNull).max(LocalDate::compareTo).orElse(debt.getScheduleEndDate()));
        debt.setCurrentBalance(summary.principalPending());
        if (debt.getOriginalAmount() == null || debt.getOriginalAmount().compareTo(BigDecimal.ZERO) <= 0) {
            debt.setOriginalAmount(summary.principalTotal());
        }
        if (summary.nextInstallment() != null) {
            debt.setMonthlyDueAmount(summary.nextInstallment().calculatedTotal());
            if (summary.nextInstallment().getDueDate() != null) debt.setDueDay(summary.nextInstallment().getDueDate().getDayOfMonth());
        }
        debtRepository.save(debt);
    }

    private String displayLineNumber(PersonalFinanceDebtScheduleLine line) {
        return line.getLineNumber() == null ? String.valueOf(line.getId()) : String.valueOf(line.getLineNumber());
    }

    private record ImportedBankScheduleRow(
            int lineNumber,
            LocalDate dueDate,
            BigDecimal principal,
            BigDecimal interest,
            BigDecimal insurance,
            BigDecimal total
    ) {
    }

    private int nextScheduleLineNumber(UserAccount user, PersonalFinanceDebt debt) {
        long count = debtScheduleLineRepository.countByUserAndDebt(user, debt);
        return (int) Math.min(Integer.MAX_VALUE, count + 1);
    }

    private String generatedIncomeNotes(PersonalFinanceIncomeSource source) {
        List<String> notes = new ArrayList<>();
        notes.add("Generado desde fuente recurrente");
        notes.add(source.getFrequency().getLabel());
        if (source.getNotes() != null && !source.getNotes().isBlank()) {
            notes.add(source.getNotes());
        }
        return String.join(" · ", notes);
    }

    private String generatedFixedExpenseNotes(PersonalFinanceFixedExpense expense) {
        List<String> notes = new ArrayList<>();
        notes.add("Generado desde gasto fijo recurrente");
        notes.add(expense.getCategory().getLabel());
        notes.add(expense.getFrequency().getLabel());
        if (!expense.isMandatory()) {
            notes.add("No obligatorio");
        }
        if (expense.getNotes() != null && !expense.getNotes().isBlank()) {
            notes.add(expense.getNotes());
        }
        return String.join(" · ", notes);
    }

    private String generatedSimpleDebtNotes(PersonalFinanceDebt debt) {
        List<String> notes = new ArrayList<>();
        notes.add("Generado desde deuda mensual simple");
        String debtNotes = debtPlanNotes(debt);
        if (debtNotes != null && !debtNotes.isBlank()) {
            notes.add(debtNotes);
        }
        return String.join(" · ", notes);
    }

    private Integer normalizePositiveInteger(Integer value) {
        if (value == null) {
            return null;
        }
        return Math.max(0, value);
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
