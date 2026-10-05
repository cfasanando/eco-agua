package com.ecoamazonas.eco_agua.personalfinance;

import com.ecoamazonas.eco_agua.user.UserAccount;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.Comparator;
import java.util.List;

@Service
public class PersonalFinanceReservedFundService {

    private static final BigDecimal ZERO = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);

    private final PersonalFinanceReservedFundRepository fundRepository;
    private final PersonalFinanceReservedFundMovementRepository movementRepository;
    private final PersonalFinancePaymentObligationRepository obligationRepository;
    private final PersonalFinanceDebtRepository debtRepository;
    private final PersonalFinanceDebtNegotiationRepository negotiationRepository;
    private final PersonalFinanceIncomeEventRepository incomeEventRepository;
    private final PersonalFinancePaymentRepository paymentRepository;
    private final PersonalFinanceCurrentUserService currentUserService;

    public PersonalFinanceReservedFundService(
            PersonalFinanceReservedFundRepository fundRepository,
            PersonalFinanceReservedFundMovementRepository movementRepository,
            PersonalFinancePaymentObligationRepository obligationRepository,
            PersonalFinanceDebtRepository debtRepository,
            PersonalFinanceDebtNegotiationRepository negotiationRepository,
            PersonalFinanceIncomeEventRepository incomeEventRepository,
            PersonalFinancePaymentRepository paymentRepository,
            PersonalFinanceCurrentUserService currentUserService
    ) {
        this.fundRepository = fundRepository;
        this.movementRepository = movementRepository;
        this.obligationRepository = obligationRepository;
        this.debtRepository = debtRepository;
        this.negotiationRepository = negotiationRepository;
        this.incomeEventRepository = incomeEventRepository;
        this.paymentRepository = paymentRepository;
        this.currentUserService = currentUserService;
    }

    @Transactional(readOnly = true)
    public PersonalFinanceReservedFundDashboard dashboard(YearMonth month, PersonalFinanceCurrency requestedCurrency) {
        YearMonth selectedMonth = month == null ? YearMonth.now() : month;
        PersonalFinanceCurrency currency = requestedCurrency == null ? PersonalFinanceCurrency.PEN : requestedCurrency;
        UserAccount user = currentUserService.currentUser();

        List<PersonalFinanceReservedFund> funds = fundRepository
                .findByUserOrderByStatusAscTargetDateAscCreatedAtDesc(user);
        List<PersonalFinanceReservedFundView> views = funds.stream()
                .map(this::toView)
                .sorted(Comparator
                        .comparing((PersonalFinanceReservedFundView view) -> view.status() != PersonalFinanceReservedFundStatus.ACTIVE)
                        .thenComparing(PersonalFinanceReservedFundView::targetDate, Comparator.nullsLast(Comparator.naturalOrder()))
                        .thenComparing(PersonalFinanceReservedFundView::title, String.CASE_INSENSITIVE_ORDER))
                .toList();

        List<PersonalFinanceReservedFundMovementView> movements = movementRepository
                .findTop100ByUserOrderByCreatedAtDescIdDesc(user)
                .stream()
                .map(this::toMovementView)
                .toList();

        List<PersonalFinancePaymentObligation> obligations = PersonalFinanceObligationVisibility.withoutDuplicateDebtParents(
                obligationRepository.findByUserOrderByDueDateAscPriorityAscIdAsc(user)
        ).stream()
                .filter(obligation -> !obligation.isPaidLike())
                .filter(obligation -> obligation.getStatus() != PersonalFinanceObligationStatus.CANCELLED)
                .toList();
        List<PersonalFinanceDebt> debts = debtRepository.findByUserOrderByStatusAscDueDayAscNameAsc(user);
        List<PersonalFinanceDebtNegotiation> negotiations = negotiationRepository
                .findByUserOrderByConversationDateDescIdDesc(user)
                .stream()
                .filter(entry -> entry.getStatus() == PersonalFinanceNegotiationEntryStatus.ACCEPTED
                        || !entry.getStatus().isTerminal())
                .toList();

        return new PersonalFinanceReservedFundDashboard(
                summary(user, selectedMonth, currency, views, obligations),
                views,
                movements,
                obligations,
                debts,
                negotiations,
                new PersonalFinanceReservedFundForm()
        );
    }

    @Transactional
    public PersonalFinanceReservedFund create(PersonalFinanceReservedFundForm form) {
        UserAccount user = currentUserService.currentUser();
        PersonalFinanceReservedFundTargetType targetType = form.getTargetType() == null
                ? PersonalFinanceReservedFundTargetType.FREE
                : form.getTargetType();
        BigDecimal amount = positive(form.getAmount(), "El importe reservado debe ser mayor que cero.");

        PersonalFinanceReservedFund fund = new PersonalFinanceReservedFund();
        fund.setUser(user);
        fund.setTargetType(targetType);
        fund.setAmount(amount);
        fund.setStatus(PersonalFinanceReservedFundStatus.ACTIVE);
        fund.setNotes(clean(form.getNotes()));

        bindTarget(user, fund, form);
        if (fund.getTargetAmount() == null || fund.getTargetAmount().signum() <= 0) {
            fund.setTargetAmount(amount);
        }
        fund = fundRepository.save(fund);
        recordMovement(user, fund, PersonalFinanceReservedFundMovementType.CREATED, amount, null, null, "Reserva creada.");
        return fund;
    }

    @Transactional
    public PersonalFinanceReservedFund adjust(Long id, BigDecimal adjustment, String notes) {
        UserAccount user = currentUserService.currentUser();
        PersonalFinanceReservedFund fund = requireActiveFund(id, user);
        BigDecimal delta = money(adjustment);
        if (delta.signum() == 0) {
            throw new IllegalArgumentException("Indica un importe distinto de cero.");
        }
        BigDecimal updated = money(fund.getAmount()).add(delta);
        if (updated.signum() < 0) {
            throw new IllegalArgumentException("La reducción supera el dinero reservado disponible.");
        }
        fund.setAmount(updated);
        if (updated.signum() == 0) {
            fund.setStatus(PersonalFinanceReservedFundStatus.RELEASED);
            fund.setClosedAt(LocalDateTime.now());
        }
        fund = fundRepository.save(fund);
        recordMovement(
                user,
                fund,
                delta.signum() > 0 ? PersonalFinanceReservedFundMovementType.INCREASED : PersonalFinanceReservedFundMovementType.REDUCED,
                delta.abs(),
                null,
                null,
                clean(notes)
        );
        return fund;
    }

    @Transactional
    public void transfer(Long fromId, Long toId, BigDecimal requestedAmount, String notes) {
        UserAccount user = currentUserService.currentUser();
        if (fromId == null || toId == null || fromId.equals(toId)) {
            throw new IllegalArgumentException("Selecciona dos reservas distintas.");
        }
        PersonalFinanceReservedFund source = requireActiveFund(fromId, user);
        PersonalFinanceReservedFund target = requireActiveFund(toId, user);
        if (source.getCurrency() != target.getCurrency()) {
            throw new IllegalArgumentException("Solo puedes transferir entre reservas de la misma moneda.");
        }
        BigDecimal amount = positive(requestedAmount, "El importe a transferir debe ser mayor que cero.");
        if (money(source.getAmount()).compareTo(amount) < 0) {
            throw new IllegalArgumentException("La reserva de origen no tiene saldo suficiente.");
        }

        source.setAmount(money(source.getAmount()).subtract(amount));
        if (source.getAmount().signum() == 0) {
            source.setStatus(PersonalFinanceReservedFundStatus.RELEASED);
            source.setClosedAt(LocalDateTime.now());
        }
        target.setAmount(money(target.getAmount()).add(amount));
        fundRepository.save(source);
        fundRepository.save(target);
        String detail = clean(notes);
        recordMovement(user, source, PersonalFinanceReservedFundMovementType.TRANSFER_OUT, amount, target.getId(), null, detail);
        recordMovement(user, target, PersonalFinanceReservedFundMovementType.TRANSFER_IN, amount, source.getId(), null, detail);
    }

    @Transactional
    public void release(Long id, String notes) {
        UserAccount user = currentUserService.currentUser();
        PersonalFinanceReservedFund fund = requireActiveFund(id, user);
        BigDecimal released = money(fund.getAmount());
        if (released.signum() <= 0) {
            throw new IllegalArgumentException("La reserva ya no tiene dinero disponible.");
        }
        fund.setAmount(ZERO);
        fund.setStatus(PersonalFinanceReservedFundStatus.RELEASED);
        fund.setClosedAt(LocalDateTime.now());
        fundRepository.save(fund);
        recordMovement(user, fund, PersonalFinanceReservedFundMovementType.RELEASED, released, null, null, clean(notes));
    }

    @Transactional(readOnly = true)
    public List<PersonalFinanceReservedFund> activeFundsForObligation(Long obligationId) {
        UserAccount user = currentUserService.currentUser();
        PersonalFinancePaymentObligation obligation = obligationRepository.findByIdAndUser(obligationId, user).orElseThrow();
        return fundRepository.findByUserAndStatusOrderByTargetDateAscCreatedAtDesc(user, PersonalFinanceReservedFundStatus.ACTIVE)
                .stream()
                .filter(fund -> canApply(fund, obligation, user))
                .filter(fund -> money(fund.getAmount()).signum() > 0)
                .toList();
    }

    @Transactional(readOnly = true)
    public PersonalFinanceReservedFund paymentFund(Long fundId, Long obligationId) {
        if (fundId == null) {
            return null;
        }
        UserAccount user = currentUserService.currentUser();
        PersonalFinancePaymentObligation obligation = obligationRepository.findByIdAndUser(obligationId, user).orElseThrow();
        PersonalFinanceReservedFund fund = requireActiveFund(fundId, user);
        if (!canApply(fund, obligation, user)) {
            throw new IllegalArgumentException("La reserva seleccionada no corresponde a este compromiso.");
        }
        return fund;
    }

    @Transactional
    public void consumeForPayment(
            UserAccount user,
            PersonalFinanceReservedFund fund,
            PersonalFinancePayment payment,
            BigDecimal requestedAmount
    ) {
        if (fund == null || requestedAmount == null || requestedAmount.signum() <= 0) {
            return;
        }
        PersonalFinanceReservedFund managed = fundRepository.findByIdAndUser(fund.getId(), user).orElseThrow();
        if (!managed.isActive()) {
            throw new IllegalArgumentException("La reserva seleccionada ya no está activa.");
        }
        BigDecimal amount = money(requestedAmount);
        if (money(managed.getAmount()).compareTo(amount) < 0) {
            throw new IllegalArgumentException("La reserva seleccionada no tiene saldo suficiente.");
        }
        if (payment.getCurrency() != managed.getCurrency()) {
            throw new IllegalArgumentException("La moneda de la reserva no coincide con la del pago.");
        }
        if (payment.getObligation() != null && !canApply(managed, payment.getObligation(), user)) {
            throw new IllegalArgumentException("La reserva seleccionada no corresponde a este pago.");
        }

        managed.setAmount(money(managed.getAmount()).subtract(amount));
        if (managed.getAmount().signum() == 0) {
            managed.setStatus(PersonalFinanceReservedFundStatus.APPLIED);
            managed.setClosedAt(LocalDateTime.now());
        }
        fundRepository.save(managed);
        recordMovement(user, managed, PersonalFinanceReservedFundMovementType.APPLIED, amount, null, payment.getId(), "Aplicada al pago " + payment.getPublicId() + ".");
    }

    @Transactional
    public void restoreFromReversedPayment(UserAccount user, PersonalFinancePayment payment) {
        PersonalFinanceReservedFund fund = payment.getReservedFund();
        BigDecimal reservedAmount = money(payment.getReservedAmount());
        if (fund == null || reservedAmount.signum() <= 0) {
            return;
        }
        PersonalFinanceReservedFund managed = fundRepository.findByIdAndUser(fund.getId(), user).orElseThrow();
        managed.setAmount(money(managed.getAmount()).add(reservedAmount));
        managed.setStatus(PersonalFinanceReservedFundStatus.ACTIVE);
        managed.setClosedAt(null);
        fundRepository.save(managed);
        recordMovement(user, managed, PersonalFinanceReservedFundMovementType.RESTORED, reservedAmount, null, payment.getId(), "Restaurada por reversión del pago " + payment.getPublicId() + ".");
    }

    private PersonalFinanceReservedFundSummary summary(
            UserAccount user,
            YearMonth month,
            PersonalFinanceCurrency currency,
            List<PersonalFinanceReservedFundView> views,
            List<PersonalFinancePaymentObligation> obligations
    ) {
        BigDecimal receivedIncome = incomeEventRepository
                .findByUserAndExpectedDateBetweenOrderByExpectedDateAscIdAsc(user, month.atDay(1), month.atEndOfMonth())
                .stream()
                .filter(event -> event.getCurrency() == currency)
                .filter(event -> event.getStatus() == PersonalFinanceIncomeStatus.RECEIVED)
                .map(PersonalFinanceIncomeEvent::getAmount)
                .map(this::money)
                .reduce(ZERO, BigDecimal::add);
        BigDecimal paidTotal = paymentRepository
                .findByUserAndPaymentDateBetweenOrderByPaymentDateDescIdDesc(user, month.atDay(1), month.atEndOfMonth())
                .stream()
                .filter(PersonalFinancePayment::isActive)
                .filter(payment -> payment.getCurrency() == currency)
                .map(PersonalFinancePayment::getTotalAmount)
                .map(this::money)
                .reduce(ZERO, BigDecimal::add);
        BigDecimal available = receivedIncome.subtract(paidTotal);
        BigDecimal reserved = views.stream()
                .filter(view -> view.status() == PersonalFinanceReservedFundStatus.ACTIVE)
                .filter(view -> view.currency() == currency)
                .map(PersonalFinanceReservedFundView::amount)
                .map(this::money)
                .reduce(ZERO, BigDecimal::add);
        BigDecimal free = available.subtract(reserved);
        BigDecimal overReserved = free.signum() < 0 ? free.abs() : ZERO;
        long uncoveredUpcoming = obligations.stream()
                .filter(obligation -> obligation.getCurrency() == currency)
                .filter(obligation -> obligation.getDueDate() != null)
                .filter(obligation -> !obligation.getDueDate().isAfter(month.atEndOfMonth()))
                .filter(obligation -> activeReservedForObligation(views, obligation.getId()).compareTo(money(obligation.pendingAmount())) < 0)
                .count();

        return new PersonalFinanceReservedFundSummary(
                month,
                currency,
                money(receivedIncome),
                money(paidTotal),
                money(available),
                money(reserved),
                money(free),
                money(overReserved),
                views.stream()
                        .filter(view -> view.status() == PersonalFinanceReservedFundStatus.ACTIVE)
                        .filter(view -> view.currency() == currency)
                        .count(),
                views.stream()
                        .filter(view -> view.status() == PersonalFinanceReservedFundStatus.ACTIVE)
                        .filter(view -> view.currency() == currency)
                        .filter(view -> view.coverageStatus() == PersonalFinanceAllocationStatus.COVERED)
                        .count(),
                views.stream()
                        .filter(view -> view.status() == PersonalFinanceReservedFundStatus.ACTIVE)
                        .filter(view -> view.currency() == currency)
                        .filter(view -> view.coverageStatus() == PersonalFinanceAllocationStatus.PARTIAL)
                        .count(),
                uncoveredUpcoming
        );
    }

    private BigDecimal activeReservedForObligation(List<PersonalFinanceReservedFundView> views, Long obligationId) {
        return views.stream()
                .filter(view -> view.status() == PersonalFinanceReservedFundStatus.ACTIVE)
                .filter(view -> obligationId != null && obligationId.equals(view.obligationId()))
                .map(PersonalFinanceReservedFundView::amount)
                .map(this::money)
                .reduce(ZERO, BigDecimal::add);
    }

    private void bindTarget(UserAccount user, PersonalFinanceReservedFund fund, PersonalFinanceReservedFundForm form) {
        PersonalFinanceReservedFundTargetType targetType = fund.getTargetType();
        switch (targetType) {
            case OBLIGATION -> {
                PersonalFinancePaymentObligation obligation = obligationRepository.findByIdAndUser(form.getObligationId(), user)
                        .orElseThrow(() -> new IllegalArgumentException("Selecciona un compromiso válido."));
                if (obligation.isPaidLike() || obligation.getStatus() == PersonalFinanceObligationStatus.CANCELLED) {
                    throw new IllegalArgumentException("El compromiso seleccionado ya no está pendiente.");
                }
                fund.setObligation(obligation);
                fund.setDebt(resolveDebt(user, obligation));
                fund.setTitle(firstNonBlank(form.getTitle(), obligation.getTitle()));
                fund.setTargetAmount(money(obligation.pendingAmount()));
                fund.setCurrency(obligation.getCurrency());
                fund.setTargetDate(obligation.getDueDate());
            }
            case DEBT -> {
                PersonalFinanceDebt debt = debtRepository.findByIdAndUser(form.getDebtId(), user)
                        .orElseThrow(() -> new IllegalArgumentException("Selecciona una deuda válida."));
                fund.setDebt(debt);
                fund.setTitle(firstNonBlank(form.getTitle(), debt.getName()));
                fund.setTargetAmount(money(debt.outstandingBalance()));
                fund.setCurrency(debt.getCurrency());
                fund.setTargetDate(form.getTargetDate());
            }
            case NEGOTIATION -> {
                PersonalFinanceDebtNegotiation negotiation = negotiationRepository.findByIdAndUser(form.getNegotiationId(), user)
                        .orElseThrow(() -> new IllegalArgumentException("Selecciona una negociación válida."));
                fund.setNegotiation(negotiation);
                fund.setDebt(negotiation.getDebt());
                fund.setTitle(firstNonBlank(form.getTitle(), "Acuerdo: " + negotiation.getDebt().getName()));
                BigDecimal proposed = money(negotiation.getInitialPaymentAmount()).signum() > 0
                        ? money(negotiation.getInitialPaymentAmount())
                        : money(negotiation.getProposedInstallmentAmount());
                fund.setTargetAmount(proposed.signum() > 0 ? proposed : money(negotiation.getCreditorRequestedAmount()));
                fund.setCurrency(negotiation.getCurrency());
                fund.setTargetDate(negotiation.getFirstPaymentDate() != null
                        ? negotiation.getFirstPaymentDate()
                        : negotiation.getNextActionDate());
            }
            case FREE -> {
                fund.setTitle(requiredText(form.getTitle(), "Indica un nombre para la reserva libre."));
                fund.setTargetAmount(money(form.getTargetAmount()));
                fund.setCurrency(form.getCurrency() == null ? PersonalFinanceCurrency.PEN : form.getCurrency());
                fund.setTargetDate(form.getTargetDate());
            }
        }
    }

    private PersonalFinanceReservedFundView toView(PersonalFinanceReservedFund fund) {
        BigDecimal liveTarget = targetAmount(fund);
        BigDecimal amount = money(fund.getAmount());
        PersonalFinanceAllocationStatus coverage = amount.signum() <= 0
                ? PersonalFinanceAllocationStatus.UNFUNDED
                : (liveTarget.signum() > 0 && amount.compareTo(liveTarget) < 0
                    ? PersonalFinanceAllocationStatus.PARTIAL
                    : PersonalFinanceAllocationStatus.COVERED);
        String targetLabel = switch (fund.getTargetType()) {
            case OBLIGATION -> fund.getObligation() == null ? "Compromiso" : fund.getObligation().getTitle();
            case DEBT -> fund.getDebt() == null ? "Deuda" : fund.getDebt().getName();
            case NEGOTIATION -> fund.getNegotiation() == null || fund.getNegotiation().getDebt() == null
                    ? "Negociación"
                    : fund.getNegotiation().getDebt().getName();
            case FREE -> "Uso libre";
        };
        boolean applicable = fund.isActive()
                && fund.getObligation() != null
                && !fund.getObligation().isPaidLike()
                && amount.signum() > 0;
        return new PersonalFinanceReservedFundView(
                fund.getId(),
                fund.getPublicId(),
                fund.getTitle(),
                fund.getTargetType(),
                targetLabel,
                amount,
                liveTarget,
                fund.getCurrency(),
                fund.getTargetDate(),
                fund.getStatus(),
                coverage,
                fund.getNotes(),
                fund.getObligation() == null ? null : fund.getObligation().getId(),
                applicable
        );
    }

    private PersonalFinanceReservedFundMovementView toMovementView(PersonalFinanceReservedFundMovement movement) {
        return new PersonalFinanceReservedFundMovementView(
                movement.getId(),
                movement.getReservedFund().getTitle(),
                movement.getMovementType(),
                money(movement.getAmount()),
                money(movement.getBalanceAfter()),
                movement.getReservedFund().getCurrency(),
                movement.getNotes(),
                movement.getCreatedAt()
        );
    }

    private BigDecimal targetAmount(PersonalFinanceReservedFund fund) {
        if (fund.getObligation() != null) {
            return money(fund.getObligation().pendingAmount());
        }
        if (fund.getDebt() != null && fund.getTargetType() == PersonalFinanceReservedFundTargetType.DEBT) {
            return money(fund.getDebt().outstandingBalance());
        }
        return money(fund.getTargetAmount());
    }

    private boolean canApply(PersonalFinanceReservedFund fund, PersonalFinancePaymentObligation obligation, UserAccount user) {
        if (!fund.isActive() || fund.getCurrency() != obligation.getCurrency()) {
            return false;
        }
        if (fund.getTargetType() == PersonalFinanceReservedFundTargetType.FREE) {
            return true;
        }
        if (fund.getObligation() != null) {
            return fund.getObligation().getId().equals(obligation.getId());
        }
        PersonalFinanceDebt obligationDebt = resolveDebt(user, obligation);
        if (fund.getDebt() != null && obligationDebt != null) {
            return fund.getDebt().getId().equals(obligationDebt.getId());
        }
        return false;
    }

    private PersonalFinanceDebt resolveDebt(UserAccount user, PersonalFinancePaymentObligation obligation) {
        if (obligation.getSourceId() == null) {
            return null;
        }
        return switch (obligation.getSourceType()) {
            case DEBT, DEBT_SCHEDULE, PRIVATE_LENDER_INTEREST, AUTO_DEDUCTION, DEBT_VOLUNTARY_PAYMENT ->
                    debtRepository.findByIdAndUser(obligation.getSourceId(), user).orElse(null);
            default -> null;
        };
    }

    private PersonalFinanceReservedFund requireActiveFund(Long id, UserAccount user) {
        PersonalFinanceReservedFund fund = fundRepository.findByIdAndUser(id, user).orElseThrow();
        if (!fund.isActive()) {
            throw new IllegalArgumentException("La reserva ya no está activa.");
        }
        return fund;
    }

    private void recordMovement(
            UserAccount user,
            PersonalFinanceReservedFund fund,
            PersonalFinanceReservedFundMovementType type,
            BigDecimal amount,
            Long relatedFundId,
            Long paymentId,
            String notes
    ) {
        PersonalFinanceReservedFundMovement movement = new PersonalFinanceReservedFundMovement();
        movement.setUser(user);
        movement.setReservedFund(fund);
        movement.setMovementType(type);
        movement.setAmount(money(amount));
        movement.setBalanceAfter(money(fund.getAmount()));
        movement.setRelatedFundId(relatedFundId);
        movement.setPaymentId(paymentId);
        movement.setNotes(clean(notes));
        movementRepository.save(movement);
    }

    private BigDecimal positive(BigDecimal value, String message) {
        BigDecimal result = money(value);
        if (result.signum() <= 0) {
            throw new IllegalArgumentException(message);
        }
        return result;
    }

    private BigDecimal money(BigDecimal value) {
        return (value == null ? BigDecimal.ZERO : value).setScale(2, RoundingMode.HALF_UP);
    }

    private String requiredText(String value, String message) {
        String result = clean(value);
        if (result == null) {
            throw new IllegalArgumentException(message);
        }
        return result;
    }

    private String firstNonBlank(String first, String second) {
        String firstValue = clean(first);
        return firstValue == null ? second : firstValue;
    }

    private String clean(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
