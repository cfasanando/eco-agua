package com.ecoamazonas.eco_agua.personalfinance;

import com.ecoamazonas.eco_agua.user.UserAccount;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Predicate;

@Service
public class PersonalFinanceDebtSimulationService {

    private static final BigDecimal ONE_HUNDRED = new BigDecimal("100");
    private static final BigDecimal ZERO = money(BigDecimal.ZERO);

    private final PersonalFinanceDebtRepository debtRepository;
    private final PersonalFinanceCurrentUserService currentUserService;

    public PersonalFinanceDebtSimulationService(
            PersonalFinanceDebtRepository debtRepository,
            PersonalFinanceCurrentUserService currentUserService
    ) {
        this.debtRepository = debtRepository;
        this.currentUserService = currentUserService;
    }

    @Transactional(readOnly = true)
    public PersonalFinanceDebtSimulation simulate(PersonalFinanceDebtSimulationOptions options) {
        UserAccount user = currentUserService.currentUser();
        List<PersonalFinanceDebt> allDebts = debtRepository.findByUserOrderByStatusAscDueDayAscNameAsc(user);
        long excludedCurrencyCount = allDebts.stream()
                .filter(this::isOpenDebt)
                .filter(debt -> currency(debt) != options.currency())
                .count();

        List<DebtState> initialStates = allDebts.stream()
                .filter(this::isOpenDebt)
                .filter(debt -> currency(debt) == options.currency())
                .filter(debt -> positive(debt.outstandingBalance()))
                .map(DebtState::from)
                .toList();

        List<String> warnings = buildWarnings(initialStates, options, excludedCurrencyCount);
        BigDecimal initialDebt = totalBalance(initialStates);
        BigDecimal currentMonthlyPressure = initialStates.stream()
                .map(state -> state.monthlyPayment)
                .reduce(ZERO, BigDecimal::add);

        SimulationRun baseline = run(
                copyStates(initialStates),
                options,
                ZERO,
                false,
                false
        );

        List<DebtState> strategyStates = copyStates(initialStates);
        BigDecimal lumpSumUnused = applyLumpSum(strategyStates, options);
        BigDecimal lumpSumApplied = money(options.lumpSum().subtract(lumpSumUnused));
        SimulationRun strategy = run(
                strategyStates,
                options,
                options.monthlyExtra(),
                true,
                true
        );

        Map<Long, DebtState> finalById = new HashMap<>();
        strategy.states.forEach(state -> finalById.put(state.id, state));
        List<DebtState> ranked = ordered(copyStates(initialStates), options, false);
        Map<Long, Integer> rankById = new LinkedHashMap<>();
        for (int index = 0; index < ranked.size(); index++) {
            rankById.put(ranked.get(index).id, index + 1);
        }

        List<PersonalFinanceDebtSimulationDebt> debtRows = initialStates.stream()
                .map(initial -> debtRow(
                        initial,
                        Objects.requireNonNull(finalById.get(initial.id)),
                        rankById.getOrDefault(initial.id, 0),
                        options
                ))
                .sorted(Comparator.comparingInt(PersonalFinanceDebtSimulationDebt::position))
                .toList();

        BigDecimal baselineEnding = totalBalance(baseline.states);
        BigDecimal strategyEnding = totalBalance(strategy.states);
        BigDecimal interestSaved = nonNegative(baseline.totalInterest.subtract(strategy.totalInterest));
        BigDecimal balanceAdvantage = nonNegative(baselineEnding.subtract(strategyEnding));
        BigDecimal pressureFreed = strategy.states.stream()
                .filter(state -> !positive(state.balance))
                .map(state -> state.monthlyPayment)
                .reduce(ZERO, BigDecimal::add);
        int remainingDebtCount = (int) strategy.states.stream().filter(state -> positive(state.balance)).count();
        int negativeAmortizationCount = (int) initialStates.stream().filter(this::negativeAmortizationRisk).count();

        return new PersonalFinanceDebtSimulation(
                options,
                LocalDateTime.now(),
                initialStates.size(),
                excludedCurrencyCount,
                initialDebt,
                currentMonthlyPressure,
                lumpSumApplied,
                lumpSumUnused,
                money(currentMonthlyPressure.add(options.monthlyExtra())),
                baselineEnding,
                strategyEnding,
                balanceAdvantage,
                baseline.totalInterest,
                strategy.totalInterest,
                interestSaved,
                strategy.totalPaid.add(lumpSumApplied),
                pressureFreed,
                payoffMonth(baseline.states, options.startMonth()),
                payoffMonth(strategy.states, options.startMonth()),
                remainingDebtCount,
                negativeAmortizationCount,
                debtRows,
                strategy.months,
                List.copyOf(warnings)
        );
    }

    private SimulationRun run(
            List<DebtState> states,
            PersonalFinanceDebtSimulationOptions options,
            BigDecimal monthlyExtra,
            boolean rollFreedPayments,
            boolean applyStrategy
    ) {
        BigDecimal fixedMonthlyBudget = states.stream()
                .map(state -> state.monthlyPayment)
                .reduce(ZERO, BigDecimal::add)
                .add(monthlyExtra);
        BigDecimal totalInterest = ZERO;
        BigDecimal totalPaid = ZERO;
        List<PersonalFinanceDebtSimulationMonth> months = new ArrayList<>();

        for (int monthNumber = 1; monthNumber <= options.horizonMonths(); monthNumber++) {
            if (states.stream().noneMatch(state -> positive(state.balance))) {
                break;
            }

            BigDecimal openingBalance = totalBalance(states);
            BigDecimal interestAdded = ZERO;
            for (DebtState state : states) {
                if (!positive(state.balance)) {
                    continue;
                }
                BigDecimal interest = monthlyInterest(state.balance, state.monthlyRate);
                state.balance = money(state.balance.add(interest));
                state.interestAccrued = money(state.interestAccrued.add(interest));
                interestAdded = money(interestAdded.add(interest));
            }
            totalInterest = money(totalInterest.add(interestAdded));

            Map<Long, Boolean> activeBeforePayment = new HashMap<>();
            states.forEach(state -> activeBeforePayment.put(state.id, positive(state.balance)));

            BigDecimal scheduledPaid = ZERO;
            for (DebtState state : states) {
                if (!positive(state.balance) || !positive(state.monthlyPayment)) {
                    continue;
                }
                BigDecimal payment = state.monthlyPayment.min(state.balance);
                state.balance = money(state.balance.subtract(payment));
                state.scheduledPaid = money(state.scheduledPaid.add(payment));
                scheduledPaid = money(scheduledPaid.add(payment));
            }

            BigDecimal extraBudget = ZERO;
            if (applyStrategy) {
                extraBudget = rollFreedPayments
                        ? nonNegative(fixedMonthlyBudget.subtract(scheduledPaid))
                        : monthlyExtra;
            }
            BigDecimal extraPaid = applyExtraBudget(states, options, extraBudget);
            BigDecimal unusedBudget = money(extraBudget.subtract(extraPaid));
            totalPaid = money(totalPaid.add(scheduledPaid).add(extraPaid));

            int paidOffCount = 0;
            for (DebtState state : states) {
                if (Boolean.TRUE.equals(activeBeforePayment.get(state.id)) && !positive(state.balance)) {
                    if (state.payoffMonthNumber == null) {
                        state.payoffMonthNumber = monthNumber;
                    }
                    paidOffCount++;
                }
            }

            BigDecimal endingBalance = totalBalance(states);
            int activeDebtCount = (int) states.stream().filter(state -> positive(state.balance)).count();
            BigDecimal monthlyPressureFreed = states.stream()
                    .filter(state -> !positive(state.balance))
                    .map(state -> state.monthlyPayment)
                    .reduce(ZERO, BigDecimal::add);

            months.add(new PersonalFinanceDebtSimulationMonth(
                    monthNumber,
                    options.startMonth().plusMonths(monthNumber - 1L),
                    openingBalance,
                    interestAdded,
                    scheduledPaid,
                    extraPaid,
                    unusedBudget,
                    endingBalance,
                    activeDebtCount,
                    paidOffCount,
                    monthlyPressureFreed
            ));
        }

        return new SimulationRun(states, List.copyOf(months), totalInterest, totalPaid);
    }

    private BigDecimal applyLumpSum(List<DebtState> states, PersonalFinanceDebtSimulationOptions options) {
        BigDecimal remaining = options.lumpSum();
        if (!positive(remaining)) {
            return ZERO;
        }
        for (DebtState state : ordered(states, options, true)) {
            if (!positive(remaining)) {
                break;
            }
            BigDecimal applied = remaining.min(state.balance);
            state.balance = money(state.balance.subtract(applied));
            state.lumpSumApplied = money(state.lumpSumApplied.add(applied));
            remaining = money(remaining.subtract(applied));
            if (!positive(state.balance)) {
                state.payoffMonthNumber = 0;
            }
        }
        return remaining;
    }

    private BigDecimal applyExtraBudget(
            List<DebtState> states,
            PersonalFinanceDebtSimulationOptions options,
            BigDecimal available
    ) {
        BigDecimal remaining = available;
        if (!positive(remaining)) {
            return ZERO;
        }
        BigDecimal appliedTotal = ZERO;
        for (DebtState state : ordered(states, options, true)) {
            if (!positive(remaining)) {
                break;
            }
            if (!positive(state.balance)) {
                continue;
            }
            BigDecimal applied = remaining.min(state.balance);
            state.balance = money(state.balance.subtract(applied));
            state.extraPaid = money(state.extraPaid.add(applied));
            appliedTotal = money(appliedTotal.add(applied));
            remaining = money(remaining.subtract(applied));
        }
        return appliedTotal;
    }

    private List<DebtState> ordered(
            List<DebtState> states,
            PersonalFinanceDebtSimulationOptions options,
            boolean onlyEligible
    ) {
        Set<Long> selected = Set.copyOf(options.targetDebtIds());
        Predicate<DebtState> eligibility = state -> !options.customSelection() || selected.contains(state.id);
        Comparator<DebtState> comparator = strategyComparator(options, selected);
        return states.stream()
                .filter(state -> positive(state.balance))
                .filter(state -> !onlyEligible || eligibility.test(state))
                .sorted(comparator)
                .toList();
    }

    private Comparator<DebtState> strategyComparator(
            PersonalFinanceDebtSimulationOptions options,
            Set<Long> selected
    ) {
        Comparator<DebtState> tieBreaker = Comparator
                .comparing((DebtState state) -> state.name, String.CASE_INSENSITIVE_ORDER)
                .thenComparing(state -> state.id);
        return switch (options.strategy()) {
            case LOWEST_BALANCE -> Comparator
                    .comparing((DebtState state) -> state.balance)
                    .thenComparing(Comparator.comparing((DebtState state) -> state.monthlyRate).reversed())
                    .thenComparing(tieBreaker);
            case HIGHEST_MONTHLY_PAYMENT -> Comparator
                    .comparing((DebtState state) -> state.monthlyPayment, Comparator.reverseOrder())
                    .thenComparing((DebtState state) -> state.monthlyRate, Comparator.reverseOrder())
                    .thenComparing(tieBreaker);
            case FREE_CASH_FLOW -> Comparator
                    .comparing(this::monthsToRelease)
                    .thenComparing((DebtState state) -> state.monthlyPayment, Comparator.reverseOrder())
                    .thenComparing(tieBreaker);
            case CUSTOM_SELECTION -> Comparator
                    .comparingInt((DebtState state) -> customRank(state.id, options.targetDebtIds()))
                    .thenComparing(tieBreaker);
            default -> Comparator
                    .comparing((DebtState state) -> state.monthlyRate, Comparator.reverseOrder())
                    .thenComparing(state -> state.balance)
                    .thenComparing(tieBreaker);
        };
    }

    private int customRank(Long debtId, List<Long> selectedDebtIds) {
        int index = selectedDebtIds.indexOf(debtId);
        return index < 0 ? Integer.MAX_VALUE : index;
    }

    private BigDecimal monthsToRelease(DebtState state) {
        if (!positive(state.monthlyPayment)) {
            return new BigDecimal("999999");
        }
        return state.balance.divide(state.monthlyPayment, 6, RoundingMode.HALF_UP);
    }

    private PersonalFinanceDebtSimulationDebt debtRow(
            DebtState initial,
            DebtState result,
            int position,
            PersonalFinanceDebtSimulationOptions options
    ) {
        boolean selectedTarget = !options.customSelection() || options.targetDebtIds().contains(initial.id);
        return new PersonalFinanceDebtSimulationDebt(
                position,
                initial.id,
                initial.name,
                initial.typeLabel,
                initial.classificationLabel,
                initial.statusLabel,
                initial.initialBalance,
                initial.monthlyRate,
                initial.monthlyPayment,
                result.lumpSumApplied,
                result.interestAccrued,
                money(result.lumpSumApplied.add(result.scheduledPaid).add(result.extraPaid)),
                result.balance,
                result.payoffMonthNumber == null
                        ? null
                        : options.startMonth().plusMonths(Math.max(0, result.payoffMonthNumber - 1L)),
                !positive(result.balance) ? result.monthlyPayment : ZERO,
                selectedTarget,
                result.bankEstimate,
                negativeAmortizationRisk(initial),
                strategyReason(initial, options, selectedTarget)
        );
    }

    private List<String> buildWarnings(
            List<DebtState> states,
            PersonalFinanceDebtSimulationOptions options,
            long excludedCurrencyCount
    ) {
        List<String> warnings = new ArrayList<>();
        if (excludedCurrencyCount > 0) {
            warnings.add("Se excluyeron " + excludedCurrencyCount + " deuda(s) de otra moneda para evitar mezclar importes.");
        }
        long zeroPaymentCount = states.stream().filter(state -> !positive(state.monthlyPayment)).count();
        if (zeroPaymentCount > 0) {
            warnings.add(zeroPaymentCount + " deuda(s) no tienen cuota mensual; solo avanzarán si reciben dinero extra.");
        }
        long bankEstimateCount = states.stream().filter(state -> state.bankEstimate).count();
        if (bankEstimateCount > 0) {
            warnings.add("Las deudas bancarias son estimaciones. El monto real de cancelación debe confirmarse con una liquidación oficial.");
        }
        long negativeCount = states.stream().filter(this::negativeAmortizationRisk).count();
        if (negativeCount > 0) {
            warnings.add(negativeCount + " deuda(s) podrían crecer porque la cuota mensual no supera el interés estimado.");
        }
        if (options.customSelection() && options.targetDebtIds().isEmpty()) {
            warnings.add("La estrategia manual no tiene deudas seleccionadas; no se aplicará dinero extra.");
        }
        if (states.stream().anyMatch(state -> state.paymentStopped)) {
            warnings.add("Las deudas detenidas o en cobranza se simulan matemáticamente; cualquier acuerdo real puede cambiar tasa, mora y cuota.");
        }
        warnings.add("La simulación es solo lectura y no registra pagos, no cambia saldos y no reemplaza acuerdos con bancos o acreedores.");
        return warnings;
    }

    private boolean negativeAmortizationRisk(DebtState state) {
        if (!positive(state.balance) || !positive(state.monthlyRate)) {
            return false;
        }
        return state.monthlyPayment.compareTo(monthlyInterest(state.balance, state.monthlyRate)) <= 0;
    }

    private String strategyReason(
            DebtState state,
            PersonalFinanceDebtSimulationOptions options,
            boolean selectedTarget
    ) {
        if (options.customSelection() && !selectedTarget) {
            return "No seleccionada: conserva únicamente su pago mensual programado.";
        }
        return switch (options.strategy()) {
            case LOWEST_BALANCE -> "Saldo bajo para cerrar una obligación más rápido.";
            case HIGHEST_MONTHLY_PAYMENT -> "Cuota alta que podría liberar más flujo mensual.";
            case FREE_CASH_FLOW -> "Buena relación entre saldo y cuota para liberar flujo antes.";
            case CUSTOM_SELECTION -> "Seleccionada manualmente para recibir pagos extraordinarios.";
            default -> state.monthlyRate.compareTo(BigDecimal.ZERO) > 0
                    ? "Tasa mensual alta para reducir intereses futuros."
                    : "Sin tasa registrada; se ordena después de las deudas con interés conocido.";
        };
    }

    private boolean isOpenDebt(PersonalFinanceDebt debt) {
        return debt != null
                && debt.getStatus() != PersonalFinanceDebtStatus.PAID
                && debt.getStatus() != PersonalFinanceDebtStatus.CANCELLED;
    }

    private PersonalFinanceCurrency currency(PersonalFinanceDebt debt) {
        return debt.getCurrency() == null ? PersonalFinanceCurrency.PEN : debt.getCurrency();
    }

    private YearMonth payoffMonth(List<DebtState> states, YearMonth startMonth) {
        if (states.isEmpty() || states.stream().anyMatch(state -> positive(state.balance))) {
            return null;
        }
        int finalMonth = states.stream()
                .map(state -> state.payoffMonthNumber == null ? 0 : state.payoffMonthNumber)
                .max(Integer::compareTo)
                .orElse(0);
        return finalMonth <= 0 ? startMonth : startMonth.plusMonths(finalMonth - 1L);
    }

    private BigDecimal totalBalance(List<DebtState> states) {
        return states.stream().map(state -> state.balance).reduce(ZERO, BigDecimal::add);
    }

    private BigDecimal monthlyInterest(BigDecimal balance, BigDecimal rate) {
        if (!positive(balance) || !positive(rate)) {
            return ZERO;
        }
        return money(balance.multiply(rate).divide(ONE_HUNDRED, 8, RoundingMode.HALF_UP));
    }

    private List<DebtState> copyStates(List<DebtState> source) {
        return source.stream().map(DebtState::copy).toList();
    }

    private static BigDecimal nonNegative(BigDecimal value) {
        if (value == null || value.compareTo(BigDecimal.ZERO) < 0) {
            return ZERO;
        }
        return money(value);
    }

    private static boolean positive(BigDecimal value) {
        return value != null && value.compareTo(BigDecimal.ZERO) > 0;
    }

    private static BigDecimal money(BigDecimal value) {
        return (value == null ? BigDecimal.ZERO : value).setScale(2, RoundingMode.HALF_UP);
    }

    private static BigDecimal rate(BigDecimal value) {
        return (value == null ? BigDecimal.ZERO : value).setScale(4, RoundingMode.HALF_UP);
    }

    private record SimulationRun(
            List<DebtState> states,
            List<PersonalFinanceDebtSimulationMonth> months,
            BigDecimal totalInterest,
            BigDecimal totalPaid
    ) {
    }

    private static final class DebtState {
        private final Long id;
        private final String name;
        private final String typeLabel;
        private final String classificationLabel;
        private final String statusLabel;
        private final BigDecimal initialBalance;
        private final BigDecimal monthlyRate;
        private final BigDecimal monthlyPayment;
        private final boolean bankEstimate;
        private final boolean paymentStopped;
        private BigDecimal balance;
        private BigDecimal lumpSumApplied = ZERO;
        private BigDecimal interestAccrued = ZERO;
        private BigDecimal scheduledPaid = ZERO;
        private BigDecimal extraPaid = ZERO;
        private Integer payoffMonthNumber;

        private DebtState(
                Long id,
                String name,
                String typeLabel,
                String classificationLabel,
                String statusLabel,
                BigDecimal initialBalance,
                BigDecimal monthlyRate,
                BigDecimal monthlyPayment,
                boolean bankEstimate,
                boolean paymentStopped
        ) {
            this.id = id;
            this.name = name;
            this.typeLabel = typeLabel;
            this.classificationLabel = classificationLabel;
            this.statusLabel = statusLabel;
            this.initialBalance = money(initialBalance);
            this.balance = money(initialBalance);
            this.monthlyRate = rate(monthlyRate);
            this.monthlyPayment = money(monthlyPayment);
            this.bankEstimate = bankEstimate;
            this.paymentStopped = paymentStopped;
        }

        private static DebtState from(PersonalFinanceDebt debt) {
            return new DebtState(
                    debt.getId(),
                    debt.getName(),
                    debt.getDebtType() == null ? "Otra deuda" : debt.getDebtType().getLabel(),
                    debt.classification().getLabel(),
                    debt.getStatus() == null ? "Activa" : debt.getStatus().getLabel(),
                    debt.outstandingBalance(),
                    debt.getInterestRateMonthly(),
                    debt.monthlyPressure(),
                    debt.isBankBalanceReference(),
                    debt.isPaymentStopped()
            );
        }

        private DebtState copy() {
            DebtState copy = new DebtState(
                    id,
                    name,
                    typeLabel,
                    classificationLabel,
                    statusLabel,
                    initialBalance,
                    monthlyRate,
                    monthlyPayment,
                    bankEstimate,
                    paymentStopped
            );
            copy.balance = balance;
            copy.lumpSumApplied = lumpSumApplied;
            copy.interestAccrued = interestAccrued;
            copy.scheduledPaid = scheduledPaid;
            copy.extraPaid = extraPaid;
            copy.payoffMonthNumber = payoffMonthNumber;
            return copy;
        }
    }
}
