package com.ecoamazonas.eco_agua.personalfinance;

import com.ecoamazonas.eco_agua.user.UserAccount;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Service
public class PersonalFinanceDebtNegotiationService {

    private static final BigDecimal ZERO = new BigDecimal("0.00");
    private static final int MAX_PROJECTION_MONTHS = 600;
    private static final long MAX_EVIDENCE_BYTES = 5L * 1024L * 1024L;
    private static final Set<String> ALLOWED_EVIDENCE_TYPES = Set.of(
            "application/pdf",
            "image/jpeg",
            "image/png",
            "image/webp"
    );

    private final PersonalFinanceDebtNegotiationRepository negotiationRepository;
    private final PersonalFinanceDebtRepository debtRepository;
    private final PersonalFinanceCurrentUserService currentUserService;
    private final Path evidenceRoot;

    public PersonalFinanceDebtNegotiationService(
            PersonalFinanceDebtNegotiationRepository negotiationRepository,
            PersonalFinanceDebtRepository debtRepository,
            PersonalFinanceCurrentUserService currentUserService,
            @Value("${personal.finance.negotiation-evidence-storage-dir:runtime-data/personal-finance/negotiations}") String evidenceStorageDir
    ) {
        this.negotiationRepository = negotiationRepository;
        this.debtRepository = debtRepository;
        this.currentUserService = currentUserService;
        this.evidenceRoot = Path.of(evidenceStorageDir).toAbsolutePath().normalize();
    }

    @Transactional(readOnly = true)
    public List<PersonalFinanceDebtNegotiation> negotiations(Long debtId, PersonalFinanceNegotiationEntryStatus status) {
        UserAccount user = currentUserService.currentUser();
        List<PersonalFinanceDebtNegotiation> entries;
        if (debtId != null) {
            PersonalFinanceDebt debt = debtRepository.findByIdAndUser(debtId, user).orElseThrow();
            entries = negotiationRepository.findByUserAndDebtOrderByConversationDateDescIdDesc(user, debt);
        } else {
            entries = negotiationRepository.findByUserOrderByConversationDateDescIdDesc(user);
        }
        if (status != null) {
            entries = entries.stream().filter(entry -> entry.getStatus() == status).toList();
        }
        return entries;
    }

    @Transactional(readOnly = true)
    public List<PersonalFinanceDebt> debts() {
        UserAccount user = currentUserService.currentUser();
        return debtRepository.findByUserOrderByStatusAscDueDayAscNameAsc(user).stream()
                .filter(debt -> debt.getStatus() != PersonalFinanceDebtStatus.PAID
                        && debt.getStatus() != PersonalFinanceDebtStatus.CANCELLED)
                .toList();
    }

    @Transactional(readOnly = true)
    public PersonalFinanceDebtNegotiationForm newForm(Long debtId) {
        UserAccount user = currentUserService.currentUser();
        PersonalFinanceDebtNegotiationForm form = new PersonalFinanceDebtNegotiationForm();
        if (debtId != null) {
            PersonalFinanceDebt debt = debtRepository.findByIdAndUser(debtId, user).orElseThrow();
            form.setDebtId(debt.getId());
            form.setContactPerson(firstNonBlank(debt.getContactName(), debt.getCreditorName()));
            form.setAffordableAmount(ZERO);
            form.setProposedInstallmentAmount(ZERO);
            form.setProposedMonthlyRate(rate(debt.getInterestRateMonthly()));
            form.setNextActionDate(debt.getNextReviewDate());
        }
        return form;
    }

    @Transactional(readOnly = true)
    public PersonalFinanceDebtNegotiationForm editForm(Long id) {
        UserAccount user = currentUserService.currentUser();
        PersonalFinanceDebtNegotiation entry = negotiationRepository.findByIdAndUser(id, user).orElseThrow();
        PersonalFinanceDebtNegotiationForm form = new PersonalFinanceDebtNegotiationForm();
        form.setId(entry.getId());
        form.setDebtId(entry.getDebt().getId());
        form.setConversationDate(entry.getConversationDate());
        form.setChannel(entry.getChannel());
        form.setStatus(entry.getStatus());
        form.setContactPerson(entry.getContactPerson());
        form.setCreditorRequestedAmount(entry.getCreditorRequestedAmount());
        form.setAffordableAmount(entry.getAffordableAmount());
        form.setInitialPaymentAmount(entry.getInitialPaymentAmount());
        form.setInstallmentCount(entry.getInstallmentCount());
        form.setProposedInstallmentAmount(entry.getProposedInstallmentAmount());
        form.setProposedMonthlyRate(entry.getProposedMonthlyRate());
        form.setFirstPaymentDate(entry.getFirstPaymentDate());
        form.setResponseDeadline(entry.getResponseDeadline());
        form.setNextActionDate(entry.getNextActionDate());
        form.setNextAction(entry.getNextAction());
        form.setPrivateNotes(entry.getPrivateNotes());
        return form;
    }

    @Transactional(readOnly = true)
    public PersonalFinanceDebtNegotiation negotiation(Long id) {
        UserAccount user = currentUserService.currentUser();
        return negotiationRepository.findByIdAndUser(id, user).orElseThrow();
    }

    @Transactional(rollbackFor = IOException.class)
    public PersonalFinanceDebtNegotiation save(PersonalFinanceDebtNegotiationForm form, MultipartFile evidence) throws IOException {
        UserAccount user = currentUserService.currentUser();
        PersonalFinanceDebt debt = requireDebt(form.getDebtId(), user);
        validate(form, evidence);

        PersonalFinanceDebtNegotiation entry;
        boolean newEntry = form.getId() == null;
        if (newEntry) {
            entry = new PersonalFinanceDebtNegotiation();
            entry.setPublicId(UUID.randomUUID().toString());
            entry.setUser(user);
            entry.setSnapshotCurrentBalance(money(debt.outstandingBalance()));
            entry.setSnapshotMonthlyPayment(money(debt.monthlyPressure()));
            entry.setSnapshotMonthlyRate(rate(debt.getInterestRateMonthly()));
        } else {
            entry = negotiationRepository.findByIdAndUser(form.getId(), user).orElseThrow();
        }

        entry.setDebt(debt);
        entry.setConversationDate(form.getConversationDate());
        entry.setChannel(form.getChannel());
        entry.setStatus(form.getStatus());
        entry.setContactPerson(clean(form.getContactPerson()));
        entry.setCurrency(debt.getCurrency());
        entry.setCreditorRequestedAmount(money(form.getCreditorRequestedAmount()));
        entry.setAffordableAmount(money(form.getAffordableAmount()));
        entry.setInitialPaymentAmount(money(form.getInitialPaymentAmount()));
        entry.setInstallmentCount(normalizeCount(form.getInstallmentCount()));
        entry.setProposedInstallmentAmount(money(form.getProposedInstallmentAmount()));
        entry.setProposedMonthlyRate(rate(form.getProposedMonthlyRate()));
        entry.setFirstPaymentDate(form.getFirstPaymentDate());
        entry.setResponseDeadline(form.getResponseDeadline());
        entry.setNextActionDate(form.getNextActionDate());
        entry.setNextAction(clean(form.getNextAction()));
        entry.setPrivateNotes(clean(form.getPrivateNotes()));

        Path newEvidencePath = null;
        Path oldEvidencePath = resolveStoredPath(entry.getEvidenceStoredPath());
        if (evidence != null && !evidence.isEmpty()) {
            StoredEvidence stored = storeEvidence(user, entry.getPublicId(), evidence);
            newEvidencePath = stored.path();
            entry.setEvidenceOriginalName(stored.originalName());
            entry.setEvidenceStoredPath(stored.relativePath());
            entry.setEvidenceContentType(stored.contentType());
            entry.setEvidenceSizeBytes(stored.sizeBytes());
        }

        try {
            PersonalFinanceDebtNegotiation saved = negotiationRepository.saveAndFlush(entry);
            synchronizeDebtNegotiationMetadata(debt, saved);
            debtRepository.save(debt);
            if (newEvidencePath != null && oldEvidencePath != null && !oldEvidencePath.equals(newEvidencePath)) {
                deleteQuietly(oldEvidencePath);
            }
            return saved;
        } catch (RuntimeException exception) {
            if (newEvidencePath != null) {
                Files.deleteIfExists(newEvidencePath);
            }
            throw exception;
        }
    }

    @Transactional
    public PersonalFinanceDebtNegotiation close(Long id) {
        UserAccount user = currentUserService.currentUser();
        PersonalFinanceDebtNegotiation entry = negotiationRepository.findByIdAndUser(id, user).orElseThrow();
        entry.setStatus(PersonalFinanceNegotiationEntryStatus.CLOSED);
        entry.setNextActionDate(null);
        entry.setNextAction(null);
        PersonalFinanceDebtNegotiation saved = negotiationRepository.save(entry);
        synchronizeDebtFromHistory(user, entry.getDebt());
        return saved;
    }

    @Transactional(readOnly = true)
    public PersonalFinanceDebtNegotiationComparison comparison(PersonalFinanceDebtNegotiation entry) {
        CurrentProjection current = projectCurrentPlan(entry);
        BigDecimal proposalTotal = money(entry.proposalTotal());
        BigDecimal proposalInterest = proposalTotal.subtract(money(entry.getSnapshotCurrentBalance())).max(ZERO);
        BigDecimal difference = proposalTotal.subtract(money(entry.getSnapshotCurrentBalance()));
        BigDecimal savings = current.projectedTotal() == null
                ? ZERO
                : current.projectedTotal().subtract(proposalTotal).max(ZERO);
        return new PersonalFinanceDebtNegotiationComparison(
                money(entry.getSnapshotCurrentBalance()),
                money(entry.getSnapshotMonthlyPayment()),
                rate(entry.getSnapshotMonthlyRate()),
                current.months(),
                current.endDate(),
                current.projectedInterest() == null ? ZERO : money(current.projectedInterest()),
                current.projectedTotal() == null ? ZERO : money(current.projectedTotal()),
                current.amortizes(),
                proposalTotal,
                proposalInterest,
                money(difference),
                money(savings),
                money(entry.monthlyRelief()),
                entry.proposedEndDate()
        );
    }

    @Transactional(readOnly = true)
    public PersonalFinanceDebtNegotiationSummary summary(List<PersonalFinanceDebtNegotiation> entries) {
        LocalDate today = LocalDate.now();
        long open = entries.stream().filter(entry -> !entry.getStatus().isTerminal()).count();
        long accepted = entries.stream().filter(entry -> entry.getStatus().isAccepted()).count();
        long followUpsDue = entries.stream()
                .filter(entry -> !entry.getStatus().isTerminal())
                .filter(entry -> entry.getNextActionDate() != null && !entry.getNextActionDate().isAfter(today))
                .count();
        return new PersonalFinanceDebtNegotiationSummary(entries.size(), open, accepted, followUpsDue);
    }

    @Transactional(readOnly = true)
    public PersonalFinanceDebtNegotiationEvidence evidence(String publicId) {
        UserAccount user = currentUserService.currentUser();
        PersonalFinanceDebtNegotiation entry = negotiationRepository.findByPublicIdAndUser(publicId, user).orElseThrow();
        if (!entry.hasEvidence()) {
            throw new IllegalArgumentException("Esta negociación no tiene evidencia.");
        }
        Path path = resolveStoredPath(entry.getEvidenceStoredPath());
        if (path == null || !Files.isRegularFile(path)) {
            throw new IllegalArgumentException("La evidencia de la negociación no está disponible.");
        }
        return new PersonalFinanceDebtNegotiationEvidence(
                path,
                entry.getEvidenceOriginalName(),
                entry.getEvidenceContentType(),
                entry.getEvidenceSizeBytes() == null ? 0L : entry.getEvidenceSizeBytes()
        );
    }

    private PersonalFinanceDebt requireDebt(Long debtId, UserAccount user) {
        if (debtId == null) {
            throw new IllegalArgumentException("Selecciona una deuda.");
        }
        return debtRepository.findByIdAndUser(debtId, user).orElseThrow();
    }

    private void validate(PersonalFinanceDebtNegotiationForm form, MultipartFile evidence) {
        if (form.getConversationDate() == null) {
            throw new IllegalArgumentException("Ingresa la fecha de conversación.");
        }
        if (form.getChannel() == null) {
            throw new IllegalArgumentException("Selecciona el canal de contacto.");
        }
        if (form.getStatus() == null) {
            throw new IllegalArgumentException("Selecciona el estado de la negociación.");
        }
        validateNonNegative(form.getCreditorRequestedAmount(), "Monto solicitado por el acreedor");
        validateNonNegative(form.getAffordableAmount(), "Monto máximo disponible");
        validateNonNegative(form.getInitialPaymentAmount(), "Pago inicial");
        validateNonNegative(form.getProposedInstallmentAmount(), "Cuota propuesta");
        validateNonNegative(form.getProposedMonthlyRate(), "Tasa mensual propuesta");

        int installments = form.getInstallmentCount() == null ? 0 : form.getInstallmentCount();
        if (installments < 0 || installments > 600) {
            throw new IllegalArgumentException("El número de cuotas debe estar entre 0 y 600.");
        }
        boolean hasInstallment = money(form.getProposedInstallmentAmount()).compareTo(ZERO) > 0;
        if ((installments > 0) != hasInstallment) {
            throw new IllegalArgumentException("Ingresa tanto el número de cuotas como el importe de la cuota propuesta.");
        }
        if (installments > 0 && form.getFirstPaymentDate() == null) {
            throw new IllegalArgumentException("Ingresa la fecha del primer pago de las cuotas propuestas.");
        }
        if (form.getResponseDeadline() != null && form.getResponseDeadline().isBefore(form.getConversationDate())) {
            throw new IllegalArgumentException("La fecha límite de respuesta no puede ser anterior a la fecha de conversación.");
        }
        if (form.getNextActionDate() != null && form.getNextActionDate().isBefore(form.getConversationDate())) {
            throw new IllegalArgumentException("La fecha de próxima acción no puede ser anterior a la fecha de conversación.");
        }
        BigDecimal proposalTotal = money(form.getInitialPaymentAmount())
                .add(money(form.getProposedInstallmentAmount()).multiply(BigDecimal.valueOf(installments)));
        if (form.getStatus() == PersonalFinanceNegotiationEntryStatus.ACCEPTED && proposalTotal.compareTo(ZERO) <= 0) {
            throw new IllegalArgumentException("Un acuerdo aceptado debe incluir un pago inicial o cuotas.");
        }
        validateEvidence(evidence);
    }

    private void validateEvidence(MultipartFile evidence) {
        if (evidence == null || evidence.isEmpty()) {
            return;
        }
        if (evidence.getSize() > MAX_EVIDENCE_BYTES) {
            throw new IllegalArgumentException("La evidencia no debe superar los 5 MB.");
        }
        String contentType = evidence.getContentType() == null
                ? ""
                : evidence.getContentType().toLowerCase(Locale.ROOT);
        if (!ALLOWED_EVIDENCE_TYPES.contains(contentType)) {
            throw new IllegalArgumentException("La evidencia debe ser un archivo PDF, JPG, PNG o WEBP.");
        }
    }

    private StoredEvidence storeEvidence(UserAccount user, String publicId, MultipartFile evidence) throws IOException {
        String contentType = evidence.getContentType() == null
                ? "application/octet-stream"
                : evidence.getContentType().toLowerCase(Locale.ROOT);
        String extension = extensionFor(contentType);
        String userFolder = String.valueOf(user.getId());
        Path directory = evidenceRoot.resolve(userFolder).normalize();
        if (!directory.startsWith(evidenceRoot)) {
            throw new IllegalStateException("Ruta de almacenamiento de evidencia no válida.");
        }
        Files.createDirectories(directory);
        String storedName = publicId + "-" + UUID.randomUUID() + extension;
        Path finalPath = directory.resolve(storedName).normalize();
        Path temporaryPath = directory.resolve(storedName + ".tmp").normalize();
        if (!finalPath.startsWith(evidenceRoot) || !temporaryPath.startsWith(evidenceRoot)) {
            throw new IllegalStateException("Ruta de almacenamiento de evidencia no válida.");
        }
        try (var input = evidence.getInputStream()) {
            Files.copy(input, temporaryPath, StandardCopyOption.REPLACE_EXISTING);
        }
        Files.move(temporaryPath, finalPath, StandardCopyOption.REPLACE_EXISTING);
        String originalName = sanitizeFileName(evidence.getOriginalFilename());
        String relative = evidenceRoot.relativize(finalPath).toString().replace('\\', '/');
        return new StoredEvidence(finalPath, relative, originalName, contentType, evidence.getSize());
    }

    private Path resolveStoredPath(String storedPath) {
        if (storedPath == null || storedPath.isBlank()) {
            return null;
        }
        Path path = evidenceRoot.resolve(storedPath).normalize();
        if (!path.startsWith(evidenceRoot)) {
            return null;
        }
        return path;
    }

    private void synchronizeDebtNegotiationMetadata(PersonalFinanceDebt debt, PersonalFinanceDebtNegotiation entry) {
        debt.setNegotiationStatus(mapStatus(entry.getStatus()));
        LocalDate reviewDate = entry.getNextActionDate() != null
                ? entry.getNextActionDate()
                : entry.getResponseDeadline();
        debt.setNextReviewDate(reviewDate);
    }

    private void synchronizeDebtFromHistory(UserAccount user, PersonalFinanceDebt debt) {
        List<PersonalFinanceDebtNegotiation> entries = negotiationRepository
                .findByUserAndDebtOrderByConversationDateDescIdDesc(user, debt);
        PersonalFinanceDebtNegotiation active = entries.stream()
                .filter(entry -> entry.getStatus() != PersonalFinanceNegotiationEntryStatus.CLOSED)
                .max(Comparator.comparing(PersonalFinanceDebtNegotiation::getConversationDate)
                        .thenComparing(PersonalFinanceDebtNegotiation::getId))
                .orElse(null);
        if (active == null) {
            debt.setNegotiationStatus(PersonalFinanceNegotiationStatus.CLOSED);
            debt.setNextReviewDate(null);
        } else {
            synchronizeDebtNegotiationMetadata(debt, active);
        }
        debtRepository.save(debt);
    }

    private PersonalFinanceNegotiationStatus mapStatus(PersonalFinanceNegotiationEntryStatus status) {
        return switch (status) {
            case DRAFT, CONTACT_PENDING -> PersonalFinanceNegotiationStatus.PENDING_CONTACT;
            case CONTACTED, PROPOSAL_SENT -> PersonalFinanceNegotiationStatus.IN_PROGRESS;
            case COUNTER_OFFER -> PersonalFinanceNegotiationStatus.PROPOSAL_RECEIVED;
            case ACCEPTED -> PersonalFinanceNegotiationStatus.AGREEMENT_REACHED;
            case REJECTED, EXPIRED, PAUSED -> PersonalFinanceNegotiationStatus.PAUSED;
            case CLOSED -> PersonalFinanceNegotiationStatus.CLOSED;
        };
    }

    private CurrentProjection projectCurrentPlan(PersonalFinanceDebtNegotiation entry) {
        BigDecimal balance = money(entry.getSnapshotCurrentBalance());
        BigDecimal payment = money(entry.getSnapshotMonthlyPayment());
        BigDecimal monthlyRate = rate(entry.getSnapshotMonthlyRate()).divide(new BigDecimal("100"), 10, RoundingMode.HALF_UP);
        if (balance.compareTo(ZERO) <= 0) {
            return new CurrentProjection(0, entry.getConversationDate(), ZERO, ZERO, true);
        }
        if (payment.compareTo(ZERO) <= 0) {
            return new CurrentProjection(null, null, null, null, false);
        }

        BigDecimal remaining = balance;
        BigDecimal totalInterest = ZERO;
        BigDecimal totalPaid = ZERO;
        int months = 0;
        while (remaining.compareTo(ZERO) > 0 && months < MAX_PROJECTION_MONTHS) {
            BigDecimal interest = remaining.multiply(monthlyRate).setScale(2, RoundingMode.HALF_UP);
            BigDecimal amountDue = remaining.add(interest);
            BigDecimal actualPayment = payment.min(amountDue);
            BigDecimal principal = actualPayment.subtract(interest);
            if (principal.compareTo(ZERO) <= 0) {
                return new CurrentProjection(null, null, null, null, false);
            }
            remaining = remaining.subtract(principal).max(ZERO).setScale(2, RoundingMode.HALF_UP);
            totalInterest = totalInterest.add(interest);
            totalPaid = totalPaid.add(actualPayment);
            months++;
        }
        if (remaining.compareTo(ZERO) > 0) {
            return new CurrentProjection(null, null, null, null, false);
        }
        LocalDate base = entry.getConversationDate() == null ? LocalDate.now() : entry.getConversationDate();
        LocalDate endDate = YearMonth.from(base).plusMonths(Math.max(0, months - 1L)).atEndOfMonth();
        return new CurrentProjection(months, endDate, money(totalInterest), money(totalPaid), true);
    }

    private void validateNonNegative(BigDecimal value, String label) {
        if (value != null && value.compareTo(ZERO) < 0) {
            throw new IllegalArgumentException(label + " no puede ser negativo.");
        }
    }

    private int normalizeCount(Integer value) {
        return value == null || value <= 0 ? 0 : value;
    }

    private BigDecimal money(BigDecimal value) {
        return (value == null ? ZERO : value).setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal rate(BigDecimal value) {
        return (value == null ? BigDecimal.ZERO : value).setScale(4, RoundingMode.HALF_UP);
    }

    private String clean(String value) {
        if (value == null) return null;
        String cleaned = value.trim();
        return cleaned.isBlank() ? null : cleaned;
    }

    private String firstNonBlank(String first, String second) {
        String cleanedFirst = clean(first);
        return cleanedFirst != null ? cleanedFirst : clean(second);
    }

    private String sanitizeFileName(String filename) {
        String value = filename == null ? "evidencia" : filename.replace('\\', '/');
        int separator = value.lastIndexOf('/');
        if (separator >= 0) {
            value = value.substring(separator + 1);
        }
        value = value.replaceAll("[\r\n\t]", "_").trim();
        return value.isBlank() ? "evidencia" : value;
    }

    private void deleteQuietly(Path path) {
        try {
            Files.deleteIfExists(path);
        } catch (IOException ignored) {
            // File cleanup must not invalidate a successfully stored negotiation record.
        }
    }

    private String extensionFor(String contentType) {
        return switch (contentType) {
            case "application/pdf" -> ".pdf";
            case "image/jpeg" -> ".jpg";
            case "image/png" -> ".png";
            case "image/webp" -> ".webp";
            default -> ".bin";
        };
    }

    private record StoredEvidence(
            Path path,
            String relativePath,
            String originalName,
            String contentType,
            long sizeBytes
    ) {
    }

    private record CurrentProjection(
            Integer months,
            LocalDate endDate,
            BigDecimal projectedInterest,
            BigDecimal projectedTotal,
            boolean amortizes
    ) {
    }
}
