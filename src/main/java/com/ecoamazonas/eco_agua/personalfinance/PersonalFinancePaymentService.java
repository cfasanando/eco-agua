package com.ecoamazonas.eco_agua.personalfinance;

import com.ecoamazonas.eco_agua.user.UserAccount;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
public class PersonalFinancePaymentService {

    private static final BigDecimal ZERO = new BigDecimal("0.00");
    private static final long MAX_RECEIPT_BYTES = 5L * 1024L * 1024L;
    private static final Set<String> ALLOWED_RECEIPT_TYPES = Set.of(
            "application/pdf",
            "image/jpeg",
            "image/png",
            "image/webp"
    );

    private final PersonalFinancePaymentRepository paymentRepository;
    private final PersonalFinancePaymentObligationRepository obligationRepository;
    private final PersonalFinanceDebtRepository debtRepository;
    private final PersonalFinanceDebtScheduleLineRepository scheduleLineRepository;
    private final PersonalFinanceCurrentUserService currentUserService;
    private final Path receiptRoot;

    public PersonalFinancePaymentService(
            PersonalFinancePaymentRepository paymentRepository,
            PersonalFinancePaymentObligationRepository obligationRepository,
            PersonalFinanceDebtRepository debtRepository,
            PersonalFinanceDebtScheduleLineRepository scheduleLineRepository,
            PersonalFinanceCurrentUserService currentUserService,
            @Value("${personal.finance.receipt-storage-dir:runtime-data/personal-finance/receipts}") String receiptStorageDir
    ) {
        this.paymentRepository = paymentRepository;
        this.obligationRepository = obligationRepository;
        this.debtRepository = debtRepository;
        this.scheduleLineRepository = scheduleLineRepository;
        this.currentUserService = currentUserService;
        this.receiptRoot = Path.of(receiptStorageDir).toAbsolutePath().normalize();
    }

    @Transactional(readOnly = true)
    public PersonalFinancePaymentContext paymentContext(Long obligationId) {
        UserAccount user = currentUserService.currentUser();
        PersonalFinancePaymentObligation obligation = obligationRepository.findByIdAndUser(obligationId, user).orElseThrow();
        PersonalFinanceDebtScheduleLine line = linkedScheduleLine(obligation, user);
        PersonalFinanceDebt debt = linkedDebt(obligation, line, user);
        PersonalFinancePaymentForm form = suggestedForm(obligation, line, debt);
        return new PersonalFinancePaymentContext(
                obligation.getId(),
                obligation.getTitle(),
                obligation.getCurrency(),
                obligation.getDueDate(),
                money(obligation.getAmountDue()),
                money(obligation.getAmountPaid()),
                money(obligation.pendingAmount()),
                debt == null ? null : debt.getId(),
                debt == null ? null : debt.getName(),
                debt == null ? ZERO : money(debt.outstandingBalance()),
                debt != null && debt.hasKnownBalance(),
                line == null ? null : line.getId(),
                line == null ? null : line.getLineNumber(),
                form
        );
    }

    @Transactional(rollbackFor = IOException.class)
    public PersonalFinancePayment registerPayment(
            Long obligationId,
            PersonalFinancePaymentForm form,
            MultipartFile receipt
    ) throws IOException {
        UserAccount user = currentUserService.currentUser();
        PersonalFinancePaymentObligation obligation = obligationRepository.findByIdAndUser(obligationId, user).orElseThrow();
        if (obligation.getStatus() == PersonalFinanceObligationStatus.CANCELLED) {
            throw new IllegalArgumentException("No se puede registrar un pago sobre un compromiso cancelado.");
        }
        PersonalFinanceDebtScheduleLine line = linkedScheduleLine(obligation, user);
        PersonalFinanceDebt debt = linkedDebt(obligation, line, user);
        return registerPaymentInternal(user, obligation, line, debt, form, receipt, PersonalFinancePaymentOrigin.MANUAL);
    }

    @Transactional
    public PersonalFinancePaymentObligation setObligationPaid(Long obligationId, boolean paid) {
        UserAccount user = currentUserService.currentUser();
        PersonalFinancePaymentObligation obligation = obligationRepository.findByIdAndUser(obligationId, user).orElseThrow();
        if (obligation.getStatus() == PersonalFinanceObligationStatus.CANCELLED) {
            throw new IllegalArgumentException("No se puede cambiar un compromiso cancelado.");
        }
        if (paid) {
            if (obligation.pendingAmount().compareTo(BigDecimal.ZERO) > 0) {
                PersonalFinanceDebtScheduleLine line = linkedScheduleLine(obligation, user);
                PersonalFinanceDebt debt = linkedDebt(obligation, line, user);
                PersonalFinancePaymentForm form = suggestedForm(obligation, line, debt);
                form.setNotes("Registro rápido desde el Plan mensual.");
                try {
                    registerPaymentInternal(user, obligation, line, debt, form, null, PersonalFinancePaymentOrigin.QUICK_MONTHLY);
                } catch (IOException exception) {
                    throw new IllegalStateException("No se pudo registrar el pago rápido.", exception);
                }
            }
        } else {
            reverseActivePaymentsForObligation(user, obligation, "Compromiso reabierto desde el Plan mensual.");
        }
        return obligationRepository.findByIdAndUser(obligationId, user).orElseThrow();
    }

    @Transactional
    public boolean setScheduleLinePaid(Long scheduleLineId, boolean paid) {
        UserAccount user = currentUserService.currentUser();
        PersonalFinanceDebtScheduleLine line = scheduleLineRepository.findByIdAndUser(scheduleLineId, user).orElseThrow();
        if (line.getStatus() == PersonalFinanceObligationStatus.CANCELLED) {
            throw new IllegalArgumentException("No se puede cambiar una cuota cancelada.");
        }
        PersonalFinancePaymentObligation obligation = obligationRepository.findByScheduleLineIdAndUser(line.getId(), user).orElse(null);
        if (paid) {
            if (line.pendingAmount().compareTo(BigDecimal.ZERO) <= 0) {
                return false;
            }
            PersonalFinanceDebt debt = line.getDebt();
            PersonalFinancePaymentForm form = suggestedScheduleForm(line);
            form.setNotes("Registro rápido desde el cronograma de deuda.");
            try {
                registerPaymentInternal(user, obligation, line, debt, form, null, PersonalFinancePaymentOrigin.QUICK_SCHEDULE);
            } catch (IOException exception) {
                throw new IllegalStateException("No se pudo registrar el pago rápido.", exception);
            }
            return true;
        }
        return reverseActivePaymentsForScheduleLine(user, line, "Cuota reabierta desde el cronograma.") > 0;
    }

    @Transactional
    public void reversePayment(Long paymentId, String reason) {
        UserAccount user = currentUserService.currentUser();
        PersonalFinancePayment payment = paymentRepository.findByIdAndUser(paymentId, user).orElseThrow();
        reversePaymentInternal(user, payment, requiredText(reason, "Indica el motivo de la reversión."));
    }

    @Transactional(readOnly = true)
    public List<PersonalFinancePaymentView> payments(YearMonth month, Long debtId, boolean includeAllMonths) {
        UserAccount user = currentUserService.currentUser();
        List<PersonalFinancePayment> payments;
        if (debtId != null) {
            PersonalFinanceDebt debt = debtRepository.findByIdAndUser(debtId, user).orElseThrow();
            payments = paymentRepository.findByUserAndDebtOrderByPaymentDateDescIdDesc(user, debt);
        } else if (!includeAllMonths && month != null) {
            payments = paymentRepository.findByUserAndPaymentDateBetweenOrderByPaymentDateDescIdDesc(
                    user,
                    month.atDay(1),
                    month.atEndOfMonth()
            );
        } else {
            payments = paymentRepository.findByUserOrderByPaymentDateDescIdDesc(user);
        }
        if (debtId != null && !includeAllMonths && month != null) {
            LocalDate start = month.atDay(1);
            LocalDate end = month.atEndOfMonth();
            payments = payments.stream()
                    .filter(payment -> !payment.getPaymentDate().isBefore(start) && !payment.getPaymentDate().isAfter(end))
                    .toList();
        }
        return payments.stream().map(this::toView).toList();
    }

    @Transactional(readOnly = true)
    public List<PersonalFinancePaymentView> obligationPayments(Long obligationId) {
        UserAccount user = currentUserService.currentUser();
        PersonalFinancePaymentObligation obligation = obligationRepository.findByIdAndUser(obligationId, user).orElseThrow();
        return paymentRepository.findByUserAndObligationOrderByPaymentDateDescIdDesc(user, obligation)
                .stream()
                .map(this::toView)
                .toList();
    }

    @Transactional(readOnly = true)
    public PersonalFinancePaymentSummary summary(List<PersonalFinancePaymentView> payments) {
        BigDecimal total = ZERO;
        BigDecimal principal = ZERO;
        BigDecimal interest = ZERO;
        BigDecimal charges = ZERO;
        BigDecimal reversed = ZERO;
        long activeCount = 0;
        long reversedCount = 0;
        for (PersonalFinancePaymentView payment : payments) {
            if (payment.status() == PersonalFinancePaymentStatus.ACTIVE) {
                if (payment.currency() == PersonalFinanceCurrency.PEN) {
                    total = total.add(money(payment.totalAmount()));
                    principal = principal.add(money(payment.principalAmount()));
                    interest = interest.add(money(payment.interestAmount()));
                    charges = charges.add(money(payment.chargesAmount()));
                }
                activeCount++;
            } else {
                if (payment.currency() == PersonalFinanceCurrency.PEN) {
                    reversed = reversed.add(money(payment.totalAmount()));
                }
                reversedCount++;
            }
        }
        return new PersonalFinancePaymentSummary(total, principal, interest, charges, reversed, activeCount, reversedCount);
    }

    @Transactional(readOnly = true)
    public PersonalFinancePaymentReceipt receipt(String publicId) {
        UserAccount user = currentUserService.currentUser();
        PersonalFinancePayment payment = paymentRepository.findByPublicIdAndUser(publicId, user).orElseThrow();
        if (!payment.hasReceipt()) {
            throw new IllegalArgumentException("Este pago no tiene comprobante.");
        }
        Path path = receiptRoot.resolve(payment.getReceiptStoredPath()).normalize();
        if (!path.startsWith(receiptRoot) || !Files.isRegularFile(path)) {
            throw new IllegalArgumentException("El comprobante no está disponible.");
        }
        return new PersonalFinancePaymentReceipt(
                path,
                payment.getReceiptOriginalName(),
                payment.getReceiptContentType(),
                payment.getReceiptSizeBytes() == null ? 0L : payment.getReceiptSizeBytes()
        );
    }

    private PersonalFinancePayment registerPaymentInternal(
            UserAccount user,
            PersonalFinancePaymentObligation obligation,
            PersonalFinanceDebtScheduleLine line,
            PersonalFinanceDebt debt,
            PersonalFinancePaymentForm form,
            MultipartFile receipt,
            PersonalFinancePaymentOrigin origin
    ) throws IOException {
        NormalizedPayment normalized = normalizeAndValidate(form);
        BigDecimal pending = money(obligation == null ? line.pendingAmount() : obligation.pendingAmount());
        BigDecimal extraordinaryPrincipalAllowance = line == null && debt != null
                ? normalized.principal()
                : BigDecimal.ZERO;
        BigDecimal maximumAllowed = pending
                .add(extraordinaryPrincipalAllowance)
                .add(normalized.fee())
                .add(normalized.penalty());
        if (normalized.total().compareTo(maximumAllowed) > 0) {
            throw new IllegalArgumentException("El pago supera el importe pendiente. El excedente solo puede corresponder a capital extraordinario, comisión o mora.");
        }
        validatePrincipal(normalized.principal(), debt, line);
        validateReceipt(receipt);

        PersonalFinancePayment payment = new PersonalFinancePayment();
        payment.setUser(user);
        payment.setObligation(obligation);
        payment.setDebt(debt);
        payment.setScheduleLineId(line == null ? null : line.getId());
        payment.setObligationTitle(obligation != null ? obligation.getTitle() : line.getTitle());
        payment.setDebtName(debt == null ? null : debt.getName());
        payment.setPaymentDate(form.getPaymentDate() == null ? LocalDate.now() : form.getPaymentDate());
        payment.setTotalAmount(normalized.total());
        payment.setPrincipalAmount(normalized.principal());
        payment.setInterestAmount(normalized.interest());
        payment.setInsuranceAmount(normalized.insurance());
        payment.setFeeAmount(normalized.fee());
        payment.setPenaltyAmount(normalized.penalty());
        payment.setOtherAmount(normalized.other());
        payment.setCurrency(obligation != null ? obligation.getCurrency() : line.getCurrency());
        payment.setPaymentMethod(form.getPaymentMethod() == null ? PersonalFinancePaymentMethod.OTHER : form.getPaymentMethod());
        payment.setOrigin(origin == null ? PersonalFinancePaymentOrigin.MANUAL : origin);
        payment.setOperationNumber(clean(form.getOperationNumber()));
        payment.setRecipient(clean(form.getRecipient()));
        payment.setNotes(clean(form.getNotes()));
        payment.setStatus(PersonalFinancePaymentStatus.ACTIVE);
        payment = paymentRepository.save(payment);

        Path storedFile = null;
        try {
            if (receipt != null && !receipt.isEmpty()) {
                storedFile = storeReceipt(user, payment, receipt);
                payment = paymentRepository.save(payment);
            }
            if (line != null) {
                reconcileScheduleLine(user, line);
            }
            if (obligation != null) {
                reconcileObligation(user, obligation);
            }
            if (debt != null) {
                if (line == null && normalized.principal().compareTo(BigDecimal.ZERO) > 0) {
                    debt.setCurrentBalance(money(debt.outstandingBalance()).subtract(normalized.principal()).max(BigDecimal.ZERO));
                } else if (line != null) {
                    recalculateScheduledDebt(user, debt);
                }
                refreshDebtLastPayment(user, debt);
                debtRepository.save(debt);
            }
            return payment;
        } catch (RuntimeException | IOException exception) {
            if (storedFile != null) {
                try {
                    Files.deleteIfExists(storedFile);
                } catch (IOException ignored) {
                    // The original exception is more important than cleanup failure.
                }
            }
            throw exception;
        }
    }

    private void reversePaymentInternal(UserAccount user, PersonalFinancePayment payment, String reason) {
        if (payment.getStatus() == PersonalFinancePaymentStatus.REVERSED) {
            throw new IllegalArgumentException("El pago ya fue revertido.");
        }
        PersonalFinanceDebt debt = payment.getDebt();
        boolean scheduled = payment.getScheduleLineId() != null;
        payment.setStatus(PersonalFinancePaymentStatus.REVERSED);
        payment.setReversedAt(LocalDateTime.now());
        payment.setReversedBy(currentUserService.currentUsername());
        payment.setReversalReason(reason);
        paymentRepository.save(payment);

        if (payment.getObligation() != null) {
            reconcileObligation(user, payment.getObligation());
        }
        if (scheduled) {
            scheduleLineRepository.findByIdAndUser(payment.getScheduleLineId(), user)
                    .ifPresent(line -> reconcileScheduleLine(user, line));
        }
        if (debt != null) {
            if (!scheduled && money(payment.getPrincipalAmount()).compareTo(BigDecimal.ZERO) > 0) {
                debt.setCurrentBalance(money(debt.outstandingBalance()).add(money(payment.getPrincipalAmount())));
            } else if (scheduled) {
                recalculateScheduledDebt(user, debt);
            }
            refreshDebtLastPayment(user, debt);
            debtRepository.save(debt);
        }
    }

    private int reverseActivePaymentsForObligation(UserAccount user, PersonalFinancePaymentObligation obligation, String reason) {
        List<PersonalFinancePayment> active = paymentRepository.findByUserAndObligationAndStatus(
                user,
                obligation,
                PersonalFinancePaymentStatus.ACTIVE
        );
        List<PersonalFinancePayment> reversible = active.stream()
                .filter(payment -> payment.getOrigin() == PersonalFinancePaymentOrigin.QUICK_MONTHLY)
                .toList();
        if (reversible.isEmpty() && active.size() == 1
                && active.get(0).getOrigin() == PersonalFinancePaymentOrigin.LEGACY_MIGRATION) {
            reversible = active;
        }
        if (reversible.isEmpty() && !active.isEmpty()) {
            throw new IllegalArgumentException("Este compromiso tiene pagos manuales. Revierte la operación específica desde Pagos y abonos.");
        }
        for (PersonalFinancePayment payment : reversible) {
            reversePaymentInternal(user, payment, reason);
        }
        if (active.isEmpty() && money(obligation.getAmountPaid()).compareTo(BigDecimal.ZERO) > 0) {
            obligation.setAmountPaid(BigDecimal.ZERO);
            obligation.setStatus(statusFor(obligation.getDueDate(), BigDecimal.ZERO, obligation.getAmountDue()));
            obligationRepository.save(obligation);
            if (obligation.getScheduleLineId() != null) {
                scheduleLineRepository.findByIdAndUser(obligation.getScheduleLineId(), user).ifPresent(line -> {
                    clearSchedulePayment(line);
                    scheduleLineRepository.save(line);
                    recalculateScheduledDebt(user, line.getDebt());
                });
            }
        }
        return reversible.size();
    }

    private int reverseActivePaymentsForScheduleLine(UserAccount user, PersonalFinanceDebtScheduleLine line, String reason) {
        List<PersonalFinancePayment> active = paymentRepository.findByUserAndScheduleLineIdAndStatus(
                user,
                line.getId(),
                PersonalFinancePaymentStatus.ACTIVE
        );
        List<PersonalFinancePayment> reversible = active.stream()
                .filter(payment -> payment.getOrigin() == PersonalFinancePaymentOrigin.QUICK_SCHEDULE
                        || payment.getOrigin() == PersonalFinancePaymentOrigin.QUICK_MONTHLY)
                .toList();
        if (reversible.isEmpty() && active.size() == 1
                && active.get(0).getOrigin() == PersonalFinancePaymentOrigin.LEGACY_MIGRATION) {
            reversible = active;
        }
        if (reversible.isEmpty() && !active.isEmpty()) {
            throw new IllegalArgumentException("Esta cuota tiene pagos manuales. Revierte la operación específica desde Pagos y abonos.");
        }
        for (PersonalFinancePayment payment : reversible) {
            reversePaymentInternal(user, payment, reason);
        }
        if (active.isEmpty() && money(line.getPaidAmount()).compareTo(BigDecimal.ZERO) > 0) {
            clearSchedulePayment(line);
            scheduleLineRepository.save(line);
            obligationRepository.findByScheduleLineIdAndUser(line.getId(), user).ifPresent(obligation -> {
                obligation.setAmountPaid(BigDecimal.ZERO);
                obligation.setStatus(statusFor(obligation.getDueDate(), BigDecimal.ZERO, obligation.getAmountDue()));
                obligationRepository.save(obligation);
            });
            recalculateScheduledDebt(user, line.getDebt());
        }
        return reversible.size();
    }

    private void reconcileObligation(UserAccount user, PersonalFinancePaymentObligation obligation) {
        List<PersonalFinancePayment> active = paymentRepository.findByUserAndObligationAndStatus(
                user,
                obligation,
                PersonalFinancePaymentStatus.ACTIVE
        );
        BigDecimal paid = active.stream()
                .map(PersonalFinancePayment::getTotalAmount)
                .map(this::money)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        obligation.setAmountPaid(paid);
        obligation.setStatus(statusFor(obligation.getDueDate(), paid, obligation.getAmountDue()));
        obligationRepository.save(obligation);
    }

    private void reconcileScheduleLine(UserAccount user, PersonalFinanceDebtScheduleLine line) {
        List<PersonalFinancePayment> active = paymentRepository.findByUserAndScheduleLineIdAndStatus(
                user,
                line.getId(),
                PersonalFinancePaymentStatus.ACTIVE
        );
        line.setPaidAmount(sum(active, PersonalFinancePayment::getTotalAmount));
        line.setPaidPrincipalAmount(sum(active, PersonalFinancePayment::getPrincipalAmount));
        line.setPaidInterestAmount(sum(active, PersonalFinancePayment::getInterestAmount));
        line.setPaidInsuranceAmount(sum(active, PersonalFinancePayment::getInsuranceAmount));
        line.setPaidFeeAmount(sum(active, PersonalFinancePayment::getFeeAmount));
        line.setPaidPenaltyAmount(sum(active, PersonalFinancePayment::getPenaltyAmount));
        line.setPaidOtherAmount(sum(active, PersonalFinancePayment::getOtherAmount));
        line.setPaidAt(active.stream()
                .map(PersonalFinancePayment::getPaymentDate)
                .max(LocalDate::compareTo)
                .orElse(null));
        line.setStatus(statusFor(line.getDueDate(), line.getPaidAmount(), line.calculatedTotal()));
        scheduleLineRepository.save(line);
        if (line.getGeneratedObligationId() != null) {
            obligationRepository.findByScheduleLineIdAndUser(line.getId(), user).ifPresent(obligation -> {
                obligation.setAmountPaid(line.getPaidAmount());
                obligation.setStatus(line.getStatus());
                obligationRepository.save(obligation);
            });
        }
        recalculateScheduledDebt(user, line.getDebt());
    }

    private void recalculateScheduledDebt(UserAccount user, PersonalFinanceDebt debt) {
        if (debt == null || debt.getId() == null) {
            return;
        }
        List<PersonalFinanceDebtScheduleLine> lines = scheduleLineRepository
                .findByUserAndDebtOrderByDueDateAscLineNumberAscIdAsc(user, debt);
        if (lines.isEmpty()) {
            return;
        }
        BigDecimal principalTotal = lines.stream()
                .filter(line -> line.getStatus() != PersonalFinanceObligationStatus.CANCELLED)
                .map(PersonalFinanceDebtScheduleLine::getPrincipalAmount)
                .map(this::money)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal principalPending = lines.stream()
                .filter(line -> line.getStatus() != PersonalFinanceObligationStatus.CANCELLED)
                .map(PersonalFinanceDebtScheduleLine::principalPendingAmount)
                .map(this::money)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        debt.setCurrentBalance(principalPending);
        if (money(debt.getOriginalAmount()).compareTo(BigDecimal.ZERO) <= 0 && principalTotal.compareTo(BigDecimal.ZERO) > 0) {
            debt.setOriginalAmount(principalTotal);
        }
        debtRepository.save(debt);
    }

    private void refreshDebtLastPayment(UserAccount user, PersonalFinanceDebt debt) {
        LocalDate lastDate = paymentRepository.findByUserAndDebtOrderByPaymentDateDescIdDesc(user, debt).stream()
                .filter(PersonalFinancePayment::isActive)
                .map(PersonalFinancePayment::getPaymentDate)
                .filter(java.util.Objects::nonNull)
                .max(LocalDate::compareTo)
                .orElse(null);
        debt.setLastPaymentDate(lastDate);
    }

    private PersonalFinancePaymentForm suggestedForm(
            PersonalFinancePaymentObligation obligation,
            PersonalFinanceDebtScheduleLine line,
            PersonalFinanceDebt debt
    ) {
        if (line != null) {
            return suggestedScheduleForm(line);
        }
        PersonalFinancePaymentForm form = new PersonalFinancePaymentForm();
        BigDecimal pending = money(obligation.pendingAmount());
        form.setPaymentDate(LocalDate.now());
        form.setTotalAmount(pending);
        if (debt != null && obligation.getSourceType() == PersonalFinanceObligationSourceType.DEBT_VOLUNTARY_PAYMENT) {
            BigDecimal principal = pending.min(money(debt.outstandingBalance()));
            form.setPrincipalAmount(principal);
            form.setOtherAmount(pending.subtract(principal));
        } else {
            form.setOtherAmount(pending);
        }
        return form;
    }

    private PersonalFinancePaymentForm suggestedScheduleForm(PersonalFinanceDebtScheduleLine line) {
        PersonalFinancePaymentForm form = new PersonalFinancePaymentForm();
        BigDecimal pendingTotal = money(line.pendingAmount());
        BigDecimal principal = money(line.principalPendingAmount()).min(pendingTotal);
        BigDecimal remaining = pendingTotal.subtract(principal);
        BigDecimal interest = money(line.interestPendingAmount()).min(remaining);
        remaining = remaining.subtract(interest);
        BigDecimal insurance = money(line.getInsuranceAmount()).subtract(money(line.getPaidInsuranceAmount())).max(BigDecimal.ZERO).min(remaining);
        remaining = remaining.subtract(insurance);
        BigDecimal fee = money(line.getFeeAmount()).subtract(money(line.getPaidFeeAmount())).max(BigDecimal.ZERO).min(remaining);
        remaining = remaining.subtract(fee);
        form.setPaymentDate(LocalDate.now());
        form.setTotalAmount(pendingTotal);
        form.setPrincipalAmount(principal);
        form.setInterestAmount(interest);
        form.setInsuranceAmount(insurance);
        form.setFeeAmount(fee);
        form.setOtherAmount(remaining.max(BigDecimal.ZERO));
        return form;
    }

    private PersonalFinanceDebtScheduleLine linkedScheduleLine(PersonalFinancePaymentObligation obligation, UserAccount user) {
        if (obligation.getScheduleLineId() == null) {
            return null;
        }
        return scheduleLineRepository.findByIdAndUser(obligation.getScheduleLineId(), user).orElse(null);
    }

    private PersonalFinanceDebt linkedDebt(
            PersonalFinancePaymentObligation obligation,
            PersonalFinanceDebtScheduleLine line,
            UserAccount user
    ) {
        if (line != null && line.getDebt() != null) {
            return line.getDebt();
        }
        if (obligation.getSourceId() == null || !isDebtSource(obligation.getSourceType())) {
            return null;
        }
        return debtRepository.findByIdAndUser(obligation.getSourceId(), user).orElse(null);
    }

    private boolean isDebtSource(PersonalFinanceObligationSourceType sourceType) {
        return sourceType == PersonalFinanceObligationSourceType.DEBT
                || sourceType == PersonalFinanceObligationSourceType.DEBT_SCHEDULE
                || sourceType == PersonalFinanceObligationSourceType.PRIVATE_LENDER_INTEREST
                || sourceType == PersonalFinanceObligationSourceType.AUTO_DEDUCTION
                || sourceType == PersonalFinanceObligationSourceType.DEBT_VOLUNTARY_PAYMENT;
    }

    private void validatePrincipal(
            BigDecimal principal,
            PersonalFinanceDebt debt,
            PersonalFinanceDebtScheduleLine line
    ) {
        if (principal.compareTo(BigDecimal.ZERO) <= 0) {
            return;
        }
        if (line != null) {
            BigDecimal remainingPrincipal = line.principalPendingAmount();
            if (principal.compareTo(money(remainingPrincipal)) > 0) {
                throw new IllegalArgumentException("El capital indicado supera el capital pendiente de la cuota.");
            }
            return;
        }
        if (debt != null && debt.hasKnownBalance() && principal.compareTo(money(debt.outstandingBalance())) > 0) {
            throw new IllegalArgumentException("El capital indicado supera el saldo actual de la deuda.");
        }
    }

    private NormalizedPayment normalizeAndValidate(PersonalFinancePaymentForm form) {
        if (form == null) {
            throw new IllegalArgumentException("Completa los datos del pago.");
        }
        BigDecimal total = nonNegative(form.getTotalAmount(), "El monto total no puede ser negativo.");
        BigDecimal principal = nonNegative(form.getPrincipalAmount(), "El capital no puede ser negativo.");
        BigDecimal interest = nonNegative(form.getInterestAmount(), "El interés no puede ser negativo.");
        BigDecimal insurance = nonNegative(form.getInsuranceAmount(), "El seguro no puede ser negativo.");
        BigDecimal fee = nonNegative(form.getFeeAmount(), "La comisión no puede ser negativa.");
        BigDecimal penalty = nonNegative(form.getPenaltyAmount(), "La mora no puede ser negativa.");
        BigDecimal other = nonNegative(form.getOtherAmount(), "El importe no clasificado no puede ser negativo.");
        if (total.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("El monto total debe ser mayor que cero.");
        }
        BigDecimal components = principal.add(interest).add(insurance).add(fee).add(penalty).add(other);
        if (components.compareTo(total) != 0) {
            throw new IllegalArgumentException("La suma de capital, interés, seguro, comisión, mora y otros debe coincidir con el monto total.");
        }
        return new NormalizedPayment(total, principal, interest, insurance, fee, penalty, other);
    }

    private void validateReceipt(MultipartFile receipt) {
        if (receipt == null || receipt.isEmpty()) {
            return;
        }
        if (receipt.getSize() > MAX_RECEIPT_BYTES) {
            throw new IllegalArgumentException("El comprobante no puede superar 5 MB.");
        }
        String contentType = receipt.getContentType() == null
                ? ""
                : receipt.getContentType().toLowerCase(Locale.ROOT);
        if (!ALLOWED_RECEIPT_TYPES.contains(contentType)) {
            throw new IllegalArgumentException("El comprobante debe ser PDF, JPG, PNG o WEBP.");
        }
    }

    private Path storeReceipt(UserAccount user, PersonalFinancePayment payment, MultipartFile receipt) throws IOException {
        String extension = extensionFor(receipt.getContentType());
        Path userRoot = receiptRoot.resolve(String.valueOf(user.getId())).normalize();
        Path targetDir = userRoot.resolve(String.valueOf(payment.getPaymentDate().getYear())).normalize();
        if (!targetDir.startsWith(receiptRoot)) {
            throw new IOException("Invalid receipt storage path.");
        }
        Files.createDirectories(targetDir);
        String storedName = payment.getPublicId() + extension;
        Path target = targetDir.resolve(storedName).normalize();
        if (!target.startsWith(userRoot)) {
            throw new IOException("Invalid receipt target path.");
        }
        try (InputStream input = receipt.getInputStream()) {
            Files.copy(input, target, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException exception) {
            Files.deleteIfExists(target);
            throw exception;
        }
        String relativePath = receiptRoot.relativize(target).toString().replace('\\', '/');
        payment.setReceiptOriginalName(safeOriginalName(receipt.getOriginalFilename(), extension));
        payment.setReceiptStoredPath(relativePath);
        payment.setReceiptContentType(receipt.getContentType());
        payment.setReceiptSizeBytes(receipt.getSize());
        return target;
    }

    private PersonalFinancePaymentView toView(PersonalFinancePayment payment) {
        BigDecimal charges = money(payment.getInsuranceAmount())
                .add(money(payment.getFeeAmount()))
                .add(money(payment.getPenaltyAmount()));
        return new PersonalFinancePaymentView(
                payment.getId(),
                payment.getPublicId(),
                payment.getObligation() == null ? null : payment.getObligation().getId(),
                payment.getObligationTitle(),
                payment.getDebt() == null ? null : payment.getDebt().getId(),
                payment.getDebtName(),
                payment.getPaymentDate(),
                money(payment.getTotalAmount()),
                money(payment.getPrincipalAmount()),
                money(payment.getInterestAmount()),
                charges,
                money(payment.getOtherAmount()),
                payment.getCurrency(),
                payment.getPaymentMethod(),
                payment.getOrigin(),
                payment.getOperationNumber(),
                payment.getRecipient(),
                payment.getNotes(),
                payment.getStatus(),
                payment.hasReceipt(),
                payment.getReceiptOriginalName(),
                payment.getReversedAt(),
                payment.getReversalReason()
        );
    }

    private void clearSchedulePayment(PersonalFinanceDebtScheduleLine line) {
        line.setPaidAmount(BigDecimal.ZERO);
        line.setPaidPrincipalAmount(BigDecimal.ZERO);
        line.setPaidInterestAmount(BigDecimal.ZERO);
        line.setPaidInsuranceAmount(BigDecimal.ZERO);
        line.setPaidFeeAmount(BigDecimal.ZERO);
        line.setPaidPenaltyAmount(BigDecimal.ZERO);
        line.setPaidOtherAmount(BigDecimal.ZERO);
        line.setPaidAt(null);
        line.setStatus(statusFor(line.getDueDate(), BigDecimal.ZERO, line.calculatedTotal()));
    }

    private PersonalFinanceObligationStatus statusFor(LocalDate dueDate, BigDecimal paid, BigDecimal due) {
        BigDecimal safePaid = money(paid);
        BigDecimal safeDue = money(due);
        if (safeDue.compareTo(BigDecimal.ZERO) > 0 && safePaid.compareTo(safeDue) >= 0) {
            return PersonalFinanceObligationStatus.PAID;
        }
        if (safePaid.compareTo(BigDecimal.ZERO) > 0) {
            return PersonalFinanceObligationStatus.PARTIAL;
        }
        return dueDate != null && dueDate.isBefore(LocalDate.now())
                ? PersonalFinanceObligationStatus.OVERDUE
                : PersonalFinanceObligationStatus.PENDING;
    }

    private BigDecimal sum(List<PersonalFinancePayment> payments, java.util.function.Function<PersonalFinancePayment, BigDecimal> getter) {
        return payments.stream().map(getter).map(this::money).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal nonNegative(BigDecimal value, String message) {
        BigDecimal normalized = money(value);
        if (normalized.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException(message);
        }
        return normalized;
    }

    private BigDecimal money(BigDecimal value) {
        return (value == null ? BigDecimal.ZERO : value).setScale(2, RoundingMode.HALF_UP);
    }

    private String requiredText(String value, String message) {
        String normalized = clean(value);
        if (normalized == null || normalized.isBlank()) {
            throw new IllegalArgumentException(message);
        }
        return normalized;
    }

    private String clean(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
    }

    private String extensionFor(String contentType) {
        if ("application/pdf".equalsIgnoreCase(contentType)) return ".pdf";
        if ("image/png".equalsIgnoreCase(contentType)) return ".png";
        if ("image/webp".equalsIgnoreCase(contentType)) return ".webp";
        return ".jpg";
    }

    private String safeOriginalName(String originalName, String fallbackExtension) {
        String name = originalName == null ? "receipt" + fallbackExtension : originalName.replace('\\', '/');
        int separator = name.lastIndexOf('/');
        if (separator >= 0) {
            name = name.substring(separator + 1);
        }
        name = name.replaceAll("[^a-zA-Z0-9._ -]", "_");
        return name.isBlank() ? "receipt" + fallbackExtension : name;
    }

    private record NormalizedPayment(
            BigDecimal total,
            BigDecimal principal,
            BigDecimal interest,
            BigDecimal insurance,
            BigDecimal fee,
            BigDecimal penalty,
            BigDecimal other
    ) {
    }
}
