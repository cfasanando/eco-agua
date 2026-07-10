package com.ecoamazonas.eco_agua.personalfinance;

import com.ecoamazonas.eco_agua.user.UserAccount;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
public class PersonalFinanceDebtReportService {

    private static final BigDecimal WEEKS_PER_MONTH = new BigDecimal("4.33");

    private final PersonalFinanceDebtRepository debtRepository;
    private final PersonalFinanceDebtScheduleLineRepository scheduleLineRepository;
    private final PersonalFinanceIncomeSourceRepository incomeSourceRepository;
    private final PersonalFinanceFixedExpenseRepository fixedExpenseRepository;
    private final PersonalFinanceCurrentUserService currentUserService;
    private final PersonalFinanceService personalFinanceService;

    public PersonalFinanceDebtReportService(
            PersonalFinanceDebtRepository debtRepository,
            PersonalFinanceDebtScheduleLineRepository scheduleLineRepository,
            PersonalFinanceIncomeSourceRepository incomeSourceRepository,
            PersonalFinanceFixedExpenseRepository fixedExpenseRepository,
            PersonalFinanceCurrentUserService currentUserService,
            PersonalFinanceService personalFinanceService
    ) {
        this.debtRepository = debtRepository;
        this.scheduleLineRepository = scheduleLineRepository;
        this.incomeSourceRepository = incomeSourceRepository;
        this.fixedExpenseRepository = fixedExpenseRepository;
        this.currentUserService = currentUserService;
        this.personalFinanceService = personalFinanceService;
    }

    @Transactional(readOnly = true)
    public PersonalFinanceDebtReport build(PersonalFinanceDebtReportOptions options) {
        UserAccount user = currentUserService.currentUser();
        List<PersonalFinanceDebt> rawDebts = debtRepository.findByUserOrderByStatusAscDueDayAscNameAsc(user).stream()
                .filter(this::includeDebt)
                .filter(debt -> options.includesDebtClassification(debt.classification()))
                .toList();

        Map<Long, List<PersonalFinanceDebtScheduleLine>> linesByDebt = new HashMap<>();
        for (PersonalFinanceDebt debt : rawDebts) {
            linesByDebt.put(debt.getId(), scheduleLineRepository.findByUserAndDebtOrderByDueDateAscLineNumberAscIdAsc(user, debt));
        }

        Anonymizer anonymizer = new Anonymizer(options.anonymizeContacts());
        List<PersonalFinanceDebtReportDebt> debts = new ArrayList<>();
        for (PersonalFinanceDebt debt : rawDebts) {
            debts.add(toReportDebt(debt, linesByDebt.getOrDefault(debt.getId(), List.of()), options, anonymizer));
        }
        debts.sort(Comparator
                .comparing(PersonalFinanceDebtReportDebt::delinquent)
                .reversed()
                .thenComparing(PersonalFinanceDebtReportDebt::priorityLabel)
                .thenComparing(PersonalFinanceDebtReportDebt::outstandingBalance, Comparator.nullsLast(Comparator.reverseOrder()))
                .thenComparing(PersonalFinanceDebtReportDebt::name, String.CASE_INSENSITIVE_ORDER));

        List<PersonalFinanceIncomeSource> incomeSources = incomeSourceRepository.findByUserAndActiveTrueOrderByNameAsc(user);
        List<PersonalFinanceFixedExpense> fixedExpenses = fixedExpenseRepository.findByUserAndActiveTrueOrderByDueDayAscNameAsc(user);
        List<PersonalFinanceDebtReportLivingCostItem> livingCostItems = options.includesLivingCost()
                ? livingCostItems(fixedExpenses, options)
                : List.of();
        List<PersonalFinanceDebtReportIncomeItem> incomeItems = options.includesIncomeCapacity()
                ? incomeItems(incomeSources, options)
                : List.of();

        List<PersonalFinanceDebtReportMonth> monthlyProjection = new ArrayList<>();
        for (int index = 0; index < options.months(); index++) {
            YearMonth month = options.startMonth().plusMonths(index);
            PersonalFinanceMonthlyPlan existingPlan = personalFinanceService.monthlyPlan(month);
            BigDecimal expectedIncome = options.includesIncomeCapacity()
                    ? positiveOrFallback(existingPlan.expectedIncome(), recurringIncome(incomeSources, month))
                    : BigDecimal.ZERO;
            BigDecimal basicExpenses = options.includesLivingCost()
                    ? positiveOrFallback(existingPlan.basicLivingTotal(), recurringExpenses(fixedExpenses, month))
                    : BigDecimal.ZERO;
            List<PersonalFinanceDebtReportScheduleItem> payments = options.includesDebtContent()
                    ? projectedPaymentsForMonth(
                            month,
                            rawDebts,
                            linesByDebt,
                            existingPlan,
                            options,
                            anonymizer
                    )
                    : List.of();
            BigDecimal debtPayments = payments.stream()
                    .map(PersonalFinanceDebtReportScheduleItem::totalAmount)
                    .map(this::safe)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal projectedBalance = options.includesIncomeCapacity()
                    ? expectedIncome.subtract(basicExpenses).subtract(debtPayments)
                    : null;
            monthlyProjection.add(new PersonalFinanceDebtReportMonth(
                    month,
                    expectedIncome,
                    basicExpenses,
                    debtPayments,
                    projectedBalance,
                    payments
            ));
        }

        List<PersonalFinanceDebtReportCategoryTotal> categoryTotals = categoryTotals(rawDebts);
        List<PersonalFinanceDebtReportDebt> cancellationCandidates = debts.stream()
                .filter(PersonalFinanceDebtReportDebt::balanceKnown)
                .filter(debt -> debt.outstandingBalance().compareTo(BigDecimal.ZERO) > 0)
                .sorted(Comparator.comparing(PersonalFinanceDebtReportDebt::outstandingBalance))
                .limit(12)
                .toList();

        BigDecimal knownCapitalPen = rawDebts.stream()
                .filter(PersonalFinanceDebt::hasKnownBalance)
                .filter(debt -> currency(debt) == PersonalFinanceCurrency.PEN)
                .map(PersonalFinanceDebt::outstandingBalance)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal knownCapitalUsd = rawDebts.stream()
                .filter(PersonalFinanceDebt::hasKnownBalance)
                .filter(debt -> currency(debt) == PersonalFinanceCurrency.USD)
                .map(PersonalFinanceDebt::outstandingBalance)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal futureScheduledPaymentsPen = debts.stream()
                .filter(debt -> debt.currency() == PersonalFinanceCurrency.PEN)
                .map(PersonalFinanceDebtReportDebt::futurePayments)
                .map(this::safe)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal futureScheduledInterestPen = debts.stream()
                .filter(debt -> debt.currency() == PersonalFinanceCurrency.PEN)
                .map(PersonalFinanceDebtReportDebt::futureInterest)
                .map(this::safe)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        PersonalFinanceDebtReportMonth firstMonth = monthlyProjection.get(0);
        BigDecimal projectedLivingCostPeriodPen = monthlyProjection.stream()
                .map(PersonalFinanceDebtReportMonth::basicExpenses)
                .map(this::safe)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal projectedIncomePeriodPen = monthlyProjection.stream()
                .map(PersonalFinanceDebtReportMonth::expectedIncome)
                .map(this::safe)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return new PersonalFinanceDebtReport(
                java.time.LocalDateTime.now(),
                options.cutoffDate(),
                options.startMonth(),
                options.months(),
                options.version(),
                options.shared() ? "Titular del informe" : user.getUsername(),
                options.reportTitle(),
                options.selectionSummary(),
                options.includesDebtContent(),
                options.includesLivingCost(),
                options.includesIncomeCapacity(),
                List.copyOf(debts),
                List.copyOf(monthlyProjection),
                categoryTotals,
                cancellationCandidates,
                livingCostItems,
                incomeItems,
                knownCapitalPen,
                knownCapitalUsd,
                firstMonth.debtPayments(),
                firstMonth.expectedIncome(),
                firstMonth.basicExpenses(),
                firstMonth.projectedBalance(),
                futureScheduledPaymentsPen,
                futureScheduledInterestPen,
                projectedLivingCostPeriodPen,
                projectedIncomePeriodPen,
                rawDebts.size(),
                rawDebts.stream().filter(debt -> !debt.hasKnownBalance()).count(),
                rawDebts.stream().filter(PersonalFinanceDebt::isHighInterest).count(),
                rawDebts.stream().filter(PersonalFinanceDebt::isDelinquentTracking).count(),
                debts.stream().filter(PersonalFinanceDebtReportDebt::settlementOpportunity).count()
        );
    }

    private PersonalFinanceDebtReportDebt toReportDebt(
            PersonalFinanceDebt debt,
            List<PersonalFinanceDebtScheduleLine> lines,
            PersonalFinanceDebtReportOptions options,
            Anonymizer anonymizer
    ) {
        List<PersonalFinanceDebtScheduleLine> pendingLines = lines.stream()
                .filter(line -> line.getStatus() != PersonalFinanceObligationStatus.CANCELLED)
                .filter(line -> !line.isPaidLike())
                .toList();
        PersonalFinanceDebtScheduleLine nextLine = pendingLines.stream()
                .min(this::compareLinePosition)
                .orElse(null);
        LocalDate lastPendingDate = pendingLines.stream()
                .map(PersonalFinanceDebtScheduleLine::getDueDate)
                .filter(Objects::nonNull)
                .max(LocalDate::compareTo)
                .orElse(null);

        LocalDate estimatedEndDate = lastPendingDate;
        boolean approximateEndDate = false;
        if (estimatedEndDate == null && debt.getScheduleEndDate() != null) {
            estimatedEndDate = debt.getScheduleEndDate();
        }
        if (estimatedEndDate == null) {
            estimatedEndDate = estimateEndDate(debt, options.cutoffDate());
            approximateEndDate = estimatedEndDate != null;
        }

        BigDecimal futurePrincipal = pendingLines.stream()
                .map(PersonalFinanceDebtScheduleLine::getPrincipalAmount)
                .map(this::safe)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal futureInterest = pendingLines.stream()
                .map(PersonalFinanceDebtScheduleLine::getInterestAmount)
                .map(this::safe)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal futurePayments = pendingLines.stream()
                .map(PersonalFinanceDebtScheduleLine::pendingAmount)
                .map(this::safe)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        if (pendingLines.isEmpty() && debt.hasKnownBalance()) {
            futurePrincipal = debt.outstandingBalance();
        }

        List<PersonalFinanceDebtReportScheduleItem> scheduleItems = options.includeSchedules()
                ? lines.stream().map(line -> toScheduleItem(debt, line, options, anonymizer)).toList()
                : List.of();
        String notes = options.includePrivateNotes() ? clean(debt.getNotes()) : "";
        String displayName = anonymizer.debtName(debt);
        String creditor = anonymizer.creditorName(debt);
        String contact = anonymizer.contactName(debt);
        BigDecimal monthlyPayment = safe(debt.monthlyPressure());
        BigDecimal outstanding = debt.outstandingBalance();
        boolean settlementOpportunity = debt.hasKnownBalance()
                && outstanding.compareTo(BigDecimal.ZERO) > 0
                && monthlyPayment.compareTo(outstanding) >= 0;

        return new PersonalFinanceDebtReportDebt(
                debt.getId(),
                displayName,
                creditor,
                label(debt.getHolderType()),
                contact,
                label(debt.getDebtType()),
                debt.classification().getLabel(),
                label(debt.getScheduleMode()),
                label(debt.getStatus()),
                label(debt.getPriority()),
                label(debt.getCollectionStatus()),
                label(debt.getNegotiationStatus()),
                currency(debt),
                safe(debt.getOriginalAmount()),
                outstanding,
                monthlyPayment,
                safe(debt.getInterestRateMonthly()),
                nextLine == null ? nextDueDateWithoutSchedule(debt, options.cutoffDate()) : nextLine.getDueDate(),
                estimatedEndDate,
                approximateEndDate,
                pendingLines.isEmpty() ? estimatedRemainingInstallments(debt, options.cutoffDate()) : pendingLines.size(),
                futurePrincipal,
                futureInterest,
                futurePayments,
                debt.hasKnownBalance(),
                debt.isBankBalanceReference(),
                debt.isHighInterest(),
                debt.isDelinquentTracking(),
                settlementOpportunity,
                notes,
                scheduleItems
        );
    }

    private List<PersonalFinanceDebtReportScheduleItem> projectedPaymentsForMonth(
            YearMonth month,
            List<PersonalFinanceDebt> debts,
            Map<Long, List<PersonalFinanceDebtScheduleLine>> linesByDebt,
            PersonalFinanceMonthlyPlan plan,
            PersonalFinanceDebtReportOptions options,
            Anonymizer anonymizer
    ) {
        List<PersonalFinanceDebtReportScheduleItem> result = new ArrayList<>();
        Map<Long, Boolean> representedDebts = new HashMap<>();

        for (PersonalFinanceDebt debt : debts) {
            List<PersonalFinanceDebtScheduleLine> monthLines = linesByDebt.getOrDefault(debt.getId(), List.of()).stream()
                    .filter(line -> line.getDueDate() != null && YearMonth.from(line.getDueDate()).equals(month))
                    .filter(line -> line.getStatus() != PersonalFinanceObligationStatus.CANCELLED)
                    .filter(line -> !line.isPaidLike())
                    .toList();
            if (!monthLines.isEmpty()) {
                for (PersonalFinanceDebtScheduleLine line : monthLines) {
                    result.add(toScheduleItem(debt, line, options, anonymizer));
                }
                representedDebts.put(debt.getId(), true);
            }
        }

        for (PersonalFinanceMonthlyPlanItem item : plan.debtItems()) {
            if (item.status() == PersonalFinanceObligationStatus.CANCELLED) {
                continue;
            }
            if (item.debtId() != null && representedDebts.containsKey(item.debtId())) {
                continue;
            }
            PersonalFinanceDebt debt = item.debtId() == null ? null : debts.stream()
                    .filter(candidate -> Objects.equals(candidate.getId(), item.debtId()))
                    .findFirst()
                    .orElse(null);
            if (item.debtId() != null && debt == null) {
                continue;
            }
            PersonalFinanceDebtClassification itemClassification = debt == null
                    ? item.debtClassification()
                    : debt.classification();
            if (itemClassification == null || !options.includesDebtClassification(itemClassification)) {
                continue;
            }
            result.add(new PersonalFinanceDebtReportScheduleItem(
                    item.debtId(),
                    debt == null ? item.title() : anonymizer.debtName(debt),
                    itemClassification.getLabel(),
                    null,
                    item.dueDate(),
                    BigDecimal.ZERO,
                    BigDecimal.ZERO,
                    BigDecimal.ZERO,
                    BigDecimal.ZERO,
                    safe(item.amountDue()),
                    safe(item.amountPaid()),
                    safe(item.pendingAmount()),
                    item.currency(),
                    item.status().getLabel(),
                    false,
                    options.includePrivateNotes() ? clean(item.notes()) : ""
            ));
            if (item.debtId() != null) {
                representedDebts.put(item.debtId(), true);
            }
        }

        for (PersonalFinanceDebt debt : debts) {
            if (representedDebts.containsKey(debt.getId()) || !shouldProjectDebt(debt, month, linesByDebt.getOrDefault(debt.getId(), List.of()))) {
                continue;
            }
            BigDecimal amount = projectedMonthlyAmount(debt);
            if (amount.compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }
            LocalDate dueDate = dueDateForMonth(debt, month);
            result.add(new PersonalFinanceDebtReportScheduleItem(
                    debt.getId(),
                    anonymizer.debtName(debt),
                    debt.classification().getLabel(),
                    estimatedLineNumber(debt, month),
                    dueDate,
                    debt.getScheduleMode() == PersonalFinanceDebtScheduleMode.PRIVATE_LENDER_INTEREST ? BigDecimal.ZERO : amount,
                    debt.getScheduleMode() == PersonalFinanceDebtScheduleMode.PRIVATE_LENDER_INTEREST ? amount : BigDecimal.ZERO,
                    BigDecimal.ZERO,
                    BigDecimal.ZERO,
                    amount,
                    BigDecimal.ZERO,
                    amount,
                    currency(debt),
                    dueDate.isBefore(options.cutoffDate()) ? PersonalFinanceObligationStatus.OVERDUE.getLabel() : PersonalFinanceObligationStatus.PENDING.getLabel(),
                    true,
                    "Proyección calculada desde la deuda; confirmar el importe real con el acreedor."
            ));
        }

        result.sort(Comparator
                .comparing(PersonalFinanceDebtReportScheduleItem::dueDate, Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(PersonalFinanceDebtReportScheduleItem::debtName, String.CASE_INSENSITIVE_ORDER));
        return List.copyOf(result);
    }

    private PersonalFinanceDebtReportScheduleItem toScheduleItem(
            PersonalFinanceDebt debt,
            PersonalFinanceDebtScheduleLine line,
            PersonalFinanceDebtReportOptions options,
            Anonymizer anonymizer
    ) {
        return new PersonalFinanceDebtReportScheduleItem(
                debt.getId(),
                anonymizer.debtName(debt),
                debt.classification().getLabel(),
                line.getLineNumber(),
                line.getDueDate(),
                safe(line.getPrincipalAmount()),
                safe(line.getInterestAmount()),
                safe(line.getInsuranceAmount()),
                safe(line.getFeeAmount()),
                line.calculatedTotal(),
                safe(line.getPaidAmount()),
                line.pendingAmount(),
                line.getCurrency() == null ? currency(debt) : line.getCurrency(),
                line.getStatus().getLabel(),
                false,
                options.includePrivateNotes() ? clean(line.getNotes()) : ""
        );
    }

    private List<PersonalFinanceDebtReportCategoryTotal> categoryTotals(List<PersonalFinanceDebt> debts) {
        Map<PersonalFinanceDebtClassification, BigDecimal> pen = new EnumMap<>(PersonalFinanceDebtClassification.class);
        Map<PersonalFinanceDebtClassification, BigDecimal> usd = new EnumMap<>(PersonalFinanceDebtClassification.class);
        Map<PersonalFinanceDebtClassification, Long> counts = new EnumMap<>(PersonalFinanceDebtClassification.class);
        for (PersonalFinanceDebt debt : debts) {
            PersonalFinanceDebtClassification classification = debt.classification();
            counts.merge(classification, 1L, Long::sum);
            if (!debt.hasKnownBalance()) {
                continue;
            }
            Map<PersonalFinanceDebtClassification, BigDecimal> target = currency(debt) == PersonalFinanceCurrency.USD ? usd : pen;
            target.merge(classification, debt.outstandingBalance(), BigDecimal::add);
        }
        List<PersonalFinanceDebtReportCategoryTotal> result = new ArrayList<>();
        for (PersonalFinanceDebtClassification classification : PersonalFinanceDebtClassification.values()) {
            long count = counts.getOrDefault(classification, 0L);
            if (count == 0) {
                continue;
            }
            result.add(new PersonalFinanceDebtReportCategoryTotal(
                    classification.getLabel(),
                    pen.getOrDefault(classification, BigDecimal.ZERO),
                    usd.getOrDefault(classification, BigDecimal.ZERO),
                    count
            ));
        }
        return List.copyOf(result);
    }

    private boolean includeDebt(PersonalFinanceDebt debt) {
        return debt.getStatus() != PersonalFinanceDebtStatus.PAID
                && debt.getStatus() != PersonalFinanceDebtStatus.CANCELLED;
    }

    private boolean shouldProjectDebt(PersonalFinanceDebt debt, YearMonth month, List<PersonalFinanceDebtScheduleLine> lines) {
        if (!includeDebt(debt) || debt.isDelinquentTracking() || !debt.isAutoGenerateMonthly()) {
            return false;
        }
        if (!lines.isEmpty() && (debt.getScheduleMode() == PersonalFinanceDebtScheduleMode.BANK_SCHEDULE
                || debt.getScheduleMode() == PersonalFinanceDebtScheduleMode.PRIVATE_LENDER_INTEREST
                || debt.getScheduleMode() == PersonalFinanceDebtScheduleMode.ONE_TIME)) {
            return false;
        }
        LocalDate start = debt.getScheduleStartDate();
        LocalDate end = effectiveScheduleEnd(debt);
        if (start != null && month.isBefore(YearMonth.from(start))) {
            return false;
        }
        if (end != null && month.isAfter(YearMonth.from(end))) {
            return false;
        }
        if (debt.getScheduleMode() == PersonalFinanceDebtScheduleMode.ONE_TIME && start != null) {
            return month.equals(YearMonth.from(start));
        }
        return true;
    }

    private BigDecimal projectedMonthlyAmount(PersonalFinanceDebt debt) {
        if (debt.getScheduleMode() == PersonalFinanceDebtScheduleMode.PRIVATE_LENDER_INTEREST) {
            return safe(debt.monthlyInterestAmount()).setScale(2, RoundingMode.HALF_UP);
        }
        BigDecimal amount = safe(debt.monthlyPressure());
        if (amount.compareTo(BigDecimal.ZERO) <= 0 && debt.getScheduleMode() == PersonalFinanceDebtScheduleMode.ONE_TIME) {
            amount = debt.outstandingBalance();
        }
        return amount;
    }

    private LocalDate effectiveScheduleEnd(PersonalFinanceDebt debt) {
        if (debt.getScheduleEndDate() != null) {
            return debt.getScheduleEndDate();
        }
        if (debt.getScheduleStartDate() != null && debt.getInstallmentCount() != null && debt.getInstallmentCount() > 0) {
            return debt.getScheduleStartDate().plusMonths(debt.getInstallmentCount() - 1L);
        }
        return null;
    }

    private LocalDate estimateEndDate(PersonalFinanceDebt debt, LocalDate cutoffDate) {
        LocalDate end = effectiveScheduleEnd(debt);
        if (end != null) {
            return end;
        }
        if (!debt.hasKnownBalance() || debt.getDebtType() == PersonalFinanceDebtType.CREDIT_CARD
                || debt.getDebtType() == PersonalFinanceDebtType.STORE_CREDIT
                || debt.isDelinquentTracking()) {
            return null;
        }
        BigDecimal payment = safe(debt.monthlyPressure());
        if (payment.compareTo(BigDecimal.ZERO) <= 0) {
            return null;
        }
        int months = debt.outstandingBalance().divide(payment, 0, RoundingMode.CEILING).intValue();
        if (months <= 0 || months > 600) {
            return null;
        }
        LocalDate base = debt.getScheduleStartDate() != null && debt.getScheduleStartDate().isAfter(cutoffDate)
                ? debt.getScheduleStartDate()
                : cutoffDate.withDayOfMonth(Math.min(normalizeDueDay(debt.getDueDay()), cutoffDate.lengthOfMonth()));
        return base.plusMonths(months - 1L);
    }

    private int estimatedRemainingInstallments(PersonalFinanceDebt debt, LocalDate cutoffDate) {
        if (debt.getScheduleStartDate() != null && debt.getInstallmentCount() != null && debt.getInstallmentCount() > 0) {
            long elapsed = Math.max(0, ChronoUnit.MONTHS.between(YearMonth.from(debt.getScheduleStartDate()), YearMonth.from(cutoffDate)));
            return (int) Math.max(0, debt.getInstallmentCount() - elapsed);
        }
        if (debt.hasKnownBalance() && safe(debt.monthlyPressure()).compareTo(BigDecimal.ZERO) > 0
                && debt.getDebtType() != PersonalFinanceDebtType.CREDIT_CARD
                && debt.getDebtType() != PersonalFinanceDebtType.STORE_CREDIT) {
            return debt.outstandingBalance().divide(safe(debt.monthlyPressure()), 0, RoundingMode.CEILING).intValue();
        }
        return 0;
    }

    private LocalDate nextDueDateWithoutSchedule(PersonalFinanceDebt debt, LocalDate cutoffDate) {
        if (debt.isDelinquentTracking()) {
            return debt.getNextReviewDate();
        }
        if (debt.getScheduleStartDate() != null && !debt.getScheduleStartDate().isBefore(cutoffDate)) {
            return debt.getScheduleStartDate();
        }
        int day = normalizeDueDay(debt.getDueDay());
        YearMonth month = YearMonth.from(cutoffDate);
        LocalDate candidate = month.atDay(Math.min(day, month.lengthOfMonth()));
        if (candidate.isBefore(cutoffDate)) {
            YearMonth next = month.plusMonths(1);
            candidate = next.atDay(Math.min(day, next.lengthOfMonth()));
        }
        LocalDate end = effectiveScheduleEnd(debt);
        return end != null && candidate.isAfter(end) ? null : candidate;
    }

    private LocalDate dueDateForMonth(PersonalFinanceDebt debt, YearMonth month) {
        int day = debt.getScheduleStartDate() != null
                ? debt.getScheduleStartDate().getDayOfMonth()
                : normalizeDueDay(debt.getDueDay());
        return month.atDay(Math.min(day, month.lengthOfMonth()));
    }

    private Integer estimatedLineNumber(PersonalFinanceDebt debt, YearMonth month) {
        if (debt.getScheduleStartDate() == null) {
            return null;
        }
        long index = ChronoUnit.MONTHS.between(YearMonth.from(debt.getScheduleStartDate()), month);
        return index < 0 ? null : (int) index + 1;
    }

    private List<PersonalFinanceDebtReportLivingCostItem> livingCostItems(
            List<PersonalFinanceFixedExpense> expenses,
            PersonalFinanceDebtReportOptions options
    ) {
        return expenses.stream()
                .filter(expense -> appliesToRange(expense.getStartDate(), expense.getEndDate(), options))
                .map(expense -> new PersonalFinanceDebtReportLivingCostItem(
                        clean(expense.getName()),
                        expense.getCategory() == null ? "Otro" : expense.getCategory().getLabel(),
                        safe(expense.getAmount()),
                        applies(expense.getStartDate(), expense.getEndDate(), options.startMonth())
                                ? frequencyAmount(expense.getAmount(), expense.getFrequency(), expense.getStartDate(), options.startMonth())
                                : BigDecimal.ZERO,
                        projectedExpenseAmount(expense, options),
                        expense.getCurrency() == null ? PersonalFinanceCurrency.PEN : expense.getCurrency(),
                        expense.getFrequency() == null ? PersonalFinanceFrequency.MONTHLY.getLabel() : expense.getFrequency().getLabel(),
                        expense.getDueDay(),
                        expense.getStartDate(),
                        expense.getEndDate(),
                        expense.isMandatory()
                ))
                .toList();
    }

    private List<PersonalFinanceDebtReportIncomeItem> incomeItems(
            List<PersonalFinanceIncomeSource> sources,
            PersonalFinanceDebtReportOptions options
    ) {
        return sources.stream()
                .filter(source -> appliesToRange(source.getStartDate(), source.getEndDate(), options))
                .map(source -> new PersonalFinanceDebtReportIncomeItem(
                        clean(source.getName()),
                        source.getType() == null ? "Otro ingreso" : source.getType().getLabel(),
                        safe(source.getDefaultAmount()),
                        applies(source.getStartDate(), source.getEndDate(), options.startMonth())
                                ? frequencyAmount(source.getDefaultAmount(), source.getFrequency(), source.getStartDate(), options.startMonth())
                                : BigDecimal.ZERO,
                        projectedIncomeAmount(source, options),
                        source.getCurrency() == null ? PersonalFinanceCurrency.PEN : source.getCurrency(),
                        source.getFrequency() == null ? PersonalFinanceFrequency.MONTHLY.getLabel() : source.getFrequency().getLabel(),
                        source.getExpectedDay(),
                        source.getStartDate(),
                        source.getEndDate()
                ))
                .toList();
    }

    private BigDecimal projectedExpenseAmount(
            PersonalFinanceFixedExpense expense,
            PersonalFinanceDebtReportOptions options
    ) {
        BigDecimal total = BigDecimal.ZERO;
        for (int index = 0; index < options.months(); index++) {
            YearMonth month = options.startMonth().plusMonths(index);
            if (applies(expense.getStartDate(), expense.getEndDate(), month)) {
                total = total.add(frequencyAmount(expense.getAmount(), expense.getFrequency(), expense.getStartDate(), month));
            }
        }
        return total;
    }

    private BigDecimal projectedIncomeAmount(
            PersonalFinanceIncomeSource source,
            PersonalFinanceDebtReportOptions options
    ) {
        BigDecimal total = BigDecimal.ZERO;
        for (int index = 0; index < options.months(); index++) {
            YearMonth month = options.startMonth().plusMonths(index);
            if (applies(source.getStartDate(), source.getEndDate(), month)) {
                total = total.add(frequencyAmount(source.getDefaultAmount(), source.getFrequency(), source.getStartDate(), month));
            }
        }
        return total;
    }

    private boolean appliesToRange(
            LocalDate startDate,
            LocalDate endDate,
            PersonalFinanceDebtReportOptions options
    ) {
        YearMonth rangeStart = options.startMonth();
        YearMonth rangeEnd = options.startMonth().plusMonths(options.months() - 1L);
        return (startDate == null || !YearMonth.from(startDate).isAfter(rangeEnd))
                && (endDate == null || !YearMonth.from(endDate).isBefore(rangeStart));
    }

    private BigDecimal recurringIncome(List<PersonalFinanceIncomeSource> sources, YearMonth month) {
        return sources.stream()
                .filter(source -> applies(source.getStartDate(), source.getEndDate(), month))
                .filter(source -> source.getCurrency() == PersonalFinanceCurrency.PEN)
                .map(source -> frequencyAmount(source.getDefaultAmount(), source.getFrequency(), source.getStartDate(), month))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal recurringExpenses(List<PersonalFinanceFixedExpense> expenses, YearMonth month) {
        return expenses.stream()
                .filter(expense -> applies(expense.getStartDate(), expense.getEndDate(), month))
                .filter(expense -> expense.getCurrency() == PersonalFinanceCurrency.PEN)
                .map(expense -> frequencyAmount(expense.getAmount(), expense.getFrequency(), expense.getStartDate(), month))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal frequencyAmount(BigDecimal amount, PersonalFinanceFrequency frequency, LocalDate startDate, YearMonth month) {
        BigDecimal safeAmount = safe(amount);
        PersonalFinanceFrequency safeFrequency = frequency == null ? PersonalFinanceFrequency.MONTHLY : frequency;
        return switch (safeFrequency) {
            case MONTHLY -> safeAmount;
            case WEEKLY -> safeAmount.multiply(WEEKS_PER_MONTH).setScale(2, RoundingMode.HALF_UP);
            case YEARLY -> startDate != null && month.getMonth() == startDate.getMonth() ? safeAmount : BigDecimal.ZERO;
        };
    }

    private boolean applies(LocalDate startDate, LocalDate endDate, YearMonth month) {
        return (startDate == null || !month.isBefore(YearMonth.from(startDate)))
                && (endDate == null || !month.isAfter(YearMonth.from(endDate)));
    }

    private BigDecimal positiveOrFallback(BigDecimal value, BigDecimal fallback) {
        BigDecimal safeValue = safe(value);
        return safeValue.compareTo(BigDecimal.ZERO) > 0 ? safeValue : safe(fallback);
    }

    private int compareLinePosition(PersonalFinanceDebtScheduleLine left, PersonalFinanceDebtScheduleLine right) {
        if (left.getDueDate() == null && right.getDueDate() != null) return 1;
        if (left.getDueDate() != null && right.getDueDate() == null) return -1;
        if (left.getDueDate() != null) {
            int dateCompare = left.getDueDate().compareTo(right.getDueDate());
            if (dateCompare != 0) return dateCompare;
        }
        return Integer.compare(left.getLineNumber() == null ? Integer.MAX_VALUE : left.getLineNumber(),
                right.getLineNumber() == null ? Integer.MAX_VALUE : right.getLineNumber());
    }

    private PersonalFinanceCurrency currency(PersonalFinanceDebt debt) {
        return debt.getCurrency() == null ? PersonalFinanceCurrency.PEN : debt.getCurrency();
    }

    private int normalizeDueDay(Integer dueDay) {
        return dueDay == null ? 30 : Math.max(1, Math.min(31, dueDay));
    }

    private String label(PersonalFinanceDebtHolderType value) { return value == null ? "No definido" : value.getLabel(); }
    private String label(PersonalFinanceDebtType value) { return value == null ? "Otra deuda" : value.getLabel(); }
    private String label(PersonalFinanceDebtScheduleMode value) { return value == null ? "Pago mensual simple" : value.getLabel(); }
    private String label(PersonalFinanceDebtStatus value) { return value == null ? "Activa" : value.getLabel(); }
    private String label(PersonalFinancePriority value) { return value == null ? "Medio" : value.getLabel(); }
    private String label(PersonalFinanceCollectionStatus value) { return value == null ? "Sin gestión de cobranza" : value.getLabel(); }
    private String label(PersonalFinanceNegotiationStatus value) { return value == null ? "Sin iniciar" : value.getLabel(); }
    private BigDecimal safe(BigDecimal value) { return value == null ? BigDecimal.ZERO : value; }
    private String clean(String value) { return value == null ? "" : value.trim(); }

    private static final class Anonymizer {
        private final boolean enabled;
        private final Map<PersonalFinanceDebtClassification, Integer> counters = new EnumMap<>(PersonalFinanceDebtClassification.class);
        private final Map<Long, String> names = new HashMap<>();

        private Anonymizer(boolean enabled) {
            this.enabled = enabled;
        }

        private String debtName(PersonalFinanceDebt debt) {
            if (!enabled) {
                return cleanStatic(debt.getName(), debt.classification().getLabel());
            }
            return names.computeIfAbsent(debt.getId(), ignored -> {
                int number = counters.merge(debt.classification(), 1, Integer::sum);
                return debt.classification().getLabel() + " " + number;
            });
        }

        private String creditorName(PersonalFinanceDebt debt) {
            String value = cleanStatic(debt.getCreditorName(), "No definido");
            if (!enabled) {
                return value;
            }
            return debt.classification().isBankRelated() ? value : "Acreedor privado";
        }

        private String contactName(PersonalFinanceDebt debt) {
            if (!enabled) {
                return cleanStatic(debt.getContactName(), "No definido");
            }
            return debt.getContactName() == null || debt.getContactName().isBlank() ? "No definido" : "Contacto privado";
        }

        private static String cleanStatic(String value, String fallback) {
            return value == null || value.isBlank() ? fallback : value.trim();
        }
    }
}
