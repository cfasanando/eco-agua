package com.ecoamazonas.eco_agua.user;

import com.ecoamazonas.eco_agua.expense.Expense;
import com.ecoamazonas.eco_agua.expense.ExpenseRepository;
import com.ecoamazonas.eco_agua.expense.ExpenseStatus;
import com.ecoamazonas.eco_agua.expense.PersonnelExpenseCalculation;
import com.ecoamazonas.eco_agua.expense.PersonnelExpenseCalculatorService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.Normalizer;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

@Service
public class EmployeePaymentService {

    private static final Long LEGACY_PERSONNEL_CATEGORY_ID = 35L;

    /*
     * Temporary legacy bridge.
     * Based on the provided dump, historical personnel payments for Irvin
     * were stored in expense observations using the marker "Legacy personal #12".
     * This should later be replaced by an explicit migration table or mapping.
     */
    private static final Map<Long, List<String>> LEGACY_EMPLOYEE_MARKERS = Map.of(
            2L, List.of("legacy personal #12")
    );

    private final EmployeeRepository employeeRepository;
    private final EmployeePaymentRepository employeePaymentRepository;
    private final EmployeeObligationRepository employeeObligationRepository;
    private final EmployeeObligationPaymentRepository employeeObligationPaymentRepository;
    private final PersonnelExpenseCalculatorService personnelExpenseCalculatorService;
    private final ExpenseRepository expenseRepository;

    public EmployeePaymentService(
            EmployeeRepository employeeRepository,
            EmployeePaymentRepository employeePaymentRepository,
            EmployeeObligationRepository employeeObligationRepository,
            EmployeeObligationPaymentRepository employeeObligationPaymentRepository,
            PersonnelExpenseCalculatorService personnelExpenseCalculatorService,
            ExpenseRepository expenseRepository
    ) {
        this.employeeRepository = employeeRepository;
        this.employeePaymentRepository = employeePaymentRepository;
        this.employeeObligationRepository = employeeObligationRepository;
        this.employeeObligationPaymentRepository = employeeObligationPaymentRepository;
        this.personnelExpenseCalculatorService = personnelExpenseCalculatorService;
        this.expenseRepository = expenseRepository;
    }

    @Transactional(readOnly = true)
    public List<Employee> findActiveEmployees() {
        return employeeRepository.findAll().stream()
                .filter(Employee::isActive)
                .sorted(Comparator
                        .comparing(Employee::getFirstName, String.CASE_INSENSITIVE_ORDER)
                        .thenComparing(Employee::getLastName, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    @Transactional(readOnly = true)
    public Employee findEmployee(Long employeeId) {
        if (employeeId == null || employeeId <= 0) {
            return null;
        }

        return employeeRepository.findById(employeeId)
                .orElseThrow(() -> new IllegalArgumentException("Employee not found: " + employeeId));
    }

    @Transactional(readOnly = true)
    public PersonnelExpenseCalculation getSuggestedCalculation(Long employeeId, LocalDate paymentDate) {
        Employee employee = findEmployee(employeeId);
        return personnelExpenseCalculatorService.calculateSalaryExpense(employee, paymentDate);
    }

    @Transactional(readOnly = true)
    public List<EmployeePayment> findPaymentsForMonth(Long employeeId, int year, int month) {
        if (employeeId == null || employeeId <= 0) {
            return List.of();
        }

        Employee employee = findEmployee(employeeId);
        LocalDate startDate = LocalDate.of(year, month, 1);
        LocalDate endDate = startDate.withDayOfMonth(startDate.lengthOfMonth());

        List<EmployeePayment> registeredPayments =
                employeePaymentRepository.findByEmployeeIdAndPaymentDateBetweenOrderByPaymentDateDescIdDesc(
                        employeeId,
                        startDate,
                        endDate
                );

        List<EmployeePayment> legacyPayments = buildLegacyPaymentsForMonth(employee, startDate, endDate);

        List<EmployeePayment> merged = new ArrayList<>(registeredPayments.size() + legacyPayments.size());
        merged.addAll(registeredPayments);
        merged.addAll(legacyPayments);

        merged.sort(
                Comparator.comparing(
                                EmployeePayment::getPaymentDate,
                                Comparator.nullsLast(Comparator.reverseOrder())
                        )
                        .thenComparing(
                                EmployeePayment::getId,
                                Comparator.nullsLast(Comparator.reverseOrder())
                        )
        );

        return merged;
    }

    @Transactional(readOnly = true)
    public List<EmployeeObligation> findObligations(Long employeeId) {
        if (employeeId == null || employeeId <= 0) {
            return List.of();
        }

        return employeeObligationRepository.findByEmployeeIdOrderByIssueDateDescIdDesc(employeeId);
    }

    @Transactional(readOnly = true)
    public List<EmployeeObligation> findActiveObligations(Long employeeId) {
        if (employeeId == null || employeeId <= 0) {
            return List.of();
        }

        return employeeObligationRepository.findByEmployeeIdAndActiveTrueOrderByIssueDateDescIdDesc(employeeId).stream()
                .filter(item -> normalizeMoney(item.getPendingAmount()).compareTo(BigDecimal.ZERO) > 0)
                .toList();
    }

    @Transactional(readOnly = true)
    public EmployeeMonthlyPaymentSummary buildMonthlySummary(Long employeeId, int year, int month) {
        List<EmployeePayment> payments = findPaymentsForMonth(employeeId, year, month);
        List<EmployeeObligation> activeObligations = findActiveObligations(employeeId);

        EmployeeMonthlyPaymentSummary summary = new EmployeeMonthlyPaymentSummary();
        summary.setPaymentCount(payments.size());
        summary.setTotalGross(sumPaymentsGross(payments));
        summary.setTotalDiscount(sumPaymentsDiscount(payments));
        summary.setTotalNet(sumPaymentsNet(payments));
        summary.setTotalPendingObligations(sumPendingObligations(activeObligations));

        return summary;
    }

    @Transactional(readOnly = true)
    public EmployeeObligationForm buildEditObligationForm(Long employeeId, Long obligationId) {
        EmployeeObligation obligation = findObligationForEmployee(employeeId, obligationId);

        EmployeeObligationForm form = new EmployeeObligationForm();
        form.setId(obligation.getId());
        form.setEmployeeId(employeeId);
        form.setType(obligation.getType());
        form.setIssueDate(obligation.getIssueDate());
        form.setOriginalAmount(normalizeMoney(obligation.getOriginalAmount()));
        form.setPendingAmount(normalizeMoney(obligation.getPendingAmount()));
        form.setDiscountMode(obligation.getDiscountMode());
        form.setFixedDiscountAmount(normalizeMoney(obligation.getFixedDiscountAmount()));
        form.setDiscountPercentage(normalizeMoney(obligation.getDiscountPercentage()));
        form.setDescription(obligation.getDescription());
        form.setActive(obligation.isActive());

        return form;
    }

    @Transactional(readOnly = true)
    public EmployeeObligation findObligationForEmployee(Long employeeId, Long obligationId) {
        if (employeeId == null || employeeId <= 0) {
            throw new IllegalArgumentException("Employee must be selected.");
        }

        if (obligationId == null || obligationId <= 0) {
            throw new IllegalArgumentException("Obligation not found.");
        }

        return employeeObligationRepository.findByIdAndEmployeeId(obligationId, employeeId)
                .orElseThrow(() -> new IllegalArgumentException("Obligation not found for the selected employee."));
    }

    @Transactional(readOnly = true)
    public boolean hasAppliedPayments(Long obligationId) {
        if (obligationId == null || obligationId <= 0) {
            return false;
        }

        return employeeObligationPaymentRepository.existsByEmployeeObligationId(obligationId);
    }

    @Transactional(readOnly = true)
    public BigDecimal getAppliedAmount(Long obligationId) {
        if (obligationId == null || obligationId <= 0) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }

        return normalizeMoney(
                employeeObligationPaymentRepository.findByEmployeeObligationIdOrderByIdDesc(obligationId).stream()
                        .map(EmployeeObligationPayment::getAppliedAmount)
                        .filter(item -> item != null)
                        .reduce(BigDecimal.ZERO, BigDecimal::add)
        );
    }

    @Transactional
    public EmployeePayment registerPayment(EmployeePaymentForm form) {
        if (form == null || form.getEmployeeId() == null) {
            throw new IllegalArgumentException("Employee must be selected.");
        }

        Employee employee = findEmployee(form.getEmployeeId());
        LocalDate paymentDate = form.getPaymentDate() != null ? form.getPaymentDate() : LocalDate.now();
        PersonnelExpenseCalculation calculation =
                personnelExpenseCalculatorService.calculateSalaryExpense(employee, paymentDate);

        BigDecimal grossAmount = normalizeMoney(form.getGrossAmount());
        if (grossAmount.compareTo(BigDecimal.ZERO) <= 0) {
            grossAmount = normalizeMoney(calculation.getSuggestedAmount());
        }

        if (grossAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("The payment amount must be greater than zero.");
        }

        BigDecimal manualDiscountAmount = normalizeMoney(form.getManualDiscountAmount());
        BigDecimal obligationAppliedAmount = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        EmployeeObligation selectedObligation = null;

        if (form.getObligationId() != null) {
            selectedObligation = employeeObligationRepository.findByIdAndEmployeeId(form.getObligationId(), employee.getId())
                    .orElseThrow(() -> new IllegalArgumentException("The selected obligation does not belong to the employee."));

            obligationAppliedAmount = resolveObligationAppliedAmount(selectedObligation, grossAmount, form.getObligationAppliedAmount());
        }

        BigDecimal totalDiscountAmount = normalizeMoney(manualDiscountAmount.add(obligationAppliedAmount));
        if (totalDiscountAmount.compareTo(grossAmount) > 0) {
            throw new IllegalArgumentException("The total discount cannot be greater than the gross amount.");
        }

        BigDecimal netAmount = grossAmount.subtract(totalDiscountAmount).setScale(2, RoundingMode.HALF_UP);

        JobPosition jobPosition = employee.getJobPosition();
        EmployeePayment payment = new EmployeePayment();
        payment.setEmployee(employee);
        payment.setPaymentDate(paymentDate);
        payment.setPeriodYear(paymentDate.getYear());
        payment.setPeriodMonth(paymentDate.getMonthValue());
        payment.setGrossAmount(grossAmount);
        payment.setDiscountAmount(totalDiscountAmount);
        payment.setNetAmount(netAmount);
        payment.setCalculationBaseAmount(normalizeMoney(calculation.getTotalSales()));
        payment.setPaymentModeSnapshot(jobPosition != null && jobPosition.getPaymentMode() != null
                ? jobPosition.getPaymentMode().name()
                : "FIXED");
        payment.setCommissionRateSnapshot(jobPosition != null
                ? normalizeMoney(jobPosition.getCommissionRate())
                : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP));
        payment.setSalaryPeriodSnapshot(jobPosition != null && jobPosition.getSalaryPeriod() != null
                ? jobPosition.getSalaryPeriod().name()
                : null);
        payment.setObservation(cleanText(form.getObservation()));

        EmployeePayment savedPayment = employeePaymentRepository.save(payment);

        if (selectedObligation != null && obligationAppliedAmount.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal updatedPendingAmount = normalizeMoney(selectedObligation.getPendingAmount().subtract(obligationAppliedAmount));
            selectedObligation.setPendingAmount(updatedPendingAmount);
            if (updatedPendingAmount.compareTo(BigDecimal.ZERO) <= 0) {
                selectedObligation.setPendingAmount(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP));
                selectedObligation.setActive(false);
            }
            employeeObligationRepository.save(selectedObligation);

            EmployeeObligationPayment link = new EmployeeObligationPayment();
            link.setEmployeePayment(savedPayment);
            link.setEmployeeObligation(selectedObligation);
            link.setAppliedAmount(obligationAppliedAmount);
            link.setObservation("Applied automatically from payment registration.");
            employeeObligationPaymentRepository.save(link);
        }

        return savedPayment;
    }

    @Transactional
    public EmployeeObligation registerObligation(EmployeeObligationForm form) {
        if (form == null || form.getEmployeeId() == null) {
            throw new IllegalArgumentException("Employee must be selected.");
        }

        Employee employee = findEmployee(form.getEmployeeId());
        BigDecimal originalAmount = normalizeMoney(form.getOriginalAmount());
        if (originalAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("The obligation amount must be greater than zero.");
        }

        EmployeeObligation obligation = new EmployeeObligation();
        obligation.setEmployee(employee);
        obligation.setType(form.getType() != null ? form.getType() : EmployeeObligationType.LOAN);
        obligation.setIssueDate(form.getIssueDate() != null ? form.getIssueDate() : LocalDate.now());
        obligation.setOriginalAmount(originalAmount);
        obligation.setPendingAmount(originalAmount);
        obligation.setDiscountMode(form.getDiscountMode() != null
                ? form.getDiscountMode()
                : EmployeeObligationDiscountMode.MANUAL);
        obligation.setFixedDiscountAmount(normalizeMoney(form.getFixedDiscountAmount()));
        obligation.setDiscountPercentage(normalizeMoney(form.getDiscountPercentage()));
        obligation.setDescription(cleanText(form.getDescription()));
        obligation.setActive(true);

        return employeeObligationRepository.save(obligation);
    }

    @Transactional
    public EmployeeObligation updateObligation(EmployeeObligationForm form) {
        if (form == null || form.getEmployeeId() == null || form.getId() == null) {
            throw new IllegalArgumentException("The obligation to edit is invalid.");
        }

        EmployeeObligation obligation = findObligationForEmployee(form.getEmployeeId(), form.getId());
        BigDecimal originalAmount = normalizeMoney(form.getOriginalAmount());
        BigDecimal pendingAmount = normalizeMoney(form.getPendingAmount());
        BigDecimal appliedAmount = getAppliedAmount(obligation.getId());

        if (originalAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("The original amount must be greater than zero.");
        }

        if (pendingAmount.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("The pending balance cannot be negative.");
        }

        if (pendingAmount.compareTo(originalAmount) > 0) {
            throw new IllegalArgumentException("The pending balance cannot be greater than the original amount.");
        }

        if (originalAmount.compareTo(appliedAmount) < 0) {
            throw new IllegalArgumentException("The original amount cannot be lower than the total already discounted.");
        }

        obligation.setType(form.getType() != null ? form.getType() : obligation.getType());
        obligation.setIssueDate(form.getIssueDate() != null ? form.getIssueDate() : obligation.getIssueDate());
        obligation.setOriginalAmount(originalAmount);
        obligation.setPendingAmount(pendingAmount);
        obligation.setDiscountMode(form.getDiscountMode() != null ? form.getDiscountMode() : obligation.getDiscountMode());
        obligation.setFixedDiscountAmount(normalizeMoney(form.getFixedDiscountAmount()));
        obligation.setDiscountPercentage(normalizeMoney(form.getDiscountPercentage()));
        obligation.setDescription(cleanText(form.getDescription()));

        boolean requestedActive = form.getActive() != null && form.getActive();
        obligation.setActive(requestedActive && pendingAmount.compareTo(BigDecimal.ZERO) > 0);

        return employeeObligationRepository.save(obligation);
    }

    @Transactional
    public void closeObligation(Long employeeId, Long obligationId) {
        EmployeeObligation obligation = findObligationForEmployee(employeeId, obligationId);
        obligation.setActive(false);
        employeeObligationRepository.save(obligation);
    }

    @Transactional
    public void deleteObligation(Long employeeId, Long obligationId) {
        EmployeeObligation obligation = findObligationForEmployee(employeeId, obligationId);

        if (hasAppliedPayments(obligationId)) {
            throw new IllegalArgumentException("This obligation already has discounts applied and cannot be deleted. Close it or adjust its balance instead.");
        }

        employeeObligationRepository.delete(obligation);
    }

    private List<EmployeePayment> buildLegacyPaymentsForMonth(Employee employee, LocalDate startDate, LocalDate endDate) {
        if (employee == null) {
            return List.of();
        }

        return expenseRepository.findByExpenseDateBetweenOrderByExpenseDateAsc(startDate, endDate).stream()
                .filter(expense -> isLegacyPersonnelExpenseForEmployee(expense, employee))
                .map(expense -> buildLegacyPaymentSnapshot(employee, expense))
                .toList();
    }

    private boolean isLegacyPersonnelExpenseForEmployee(Expense expense, Employee employee) {
        if (expense == null || employee == null) {
            return false;
        }

        if (expense.getExpenseDate() == null) {
            return false;
        }

        if (expense.isDebt()) {
            return false;
        }

        if (expense.getStatus() != ExpenseStatus.PAID) {
            return false;
        }

        if (normalizeMoney(expense.getAmount()).compareTo(BigDecimal.ZERO) <= 0) {
            return false;
        }

        boolean isPersonnelCategory = false;
        if (expense.getCategory() != null) {
            String categoryName = normalizeText(expense.getCategory().getName());
            Long categoryId = expense.getCategory().getId();

            isPersonnelCategory = "personal".equals(categoryName)
                    || LEGACY_PERSONNEL_CATEGORY_ID.equals(categoryId);
        }

        if (!isPersonnelCategory) {
            return false;
        }

        String normalizedObservation = normalizeText(expense.getObservation());
        if (normalizedObservation.isBlank()) {
            return false;
        }

        String fullName = normalizeText(buildEmployeeDisplayName(employee));
        if (!fullName.isBlank() && normalizedObservation.contains(fullName)) {
            return true;
        }

        List<String> configuredMarkers = LEGACY_EMPLOYEE_MARKERS.get(employee.getId());
        if (configuredMarkers == null || configuredMarkers.isEmpty()) {
            return false;
        }

        return configuredMarkers.stream()
                .map(this::normalizeText)
                .anyMatch(normalizedObservation::contains);
    }

    private EmployeePayment buildLegacyPaymentSnapshot(Employee employee, Expense expense) {
        EmployeePayment payment = new EmployeePayment();
        JobPosition jobPosition = employee.getJobPosition();
        BigDecimal grossAmount = normalizeMoney(expense.getAmount());

        payment.setId(expense.getId());
        payment.setEmployee(employee);
        payment.setPaymentDate(expense.getExpenseDate());
        payment.setPeriodYear(expense.getExpenseDate().getYear());
        payment.setPeriodMonth(expense.getExpenseDate().getMonthValue());
        payment.setGrossAmount(grossAmount);
        payment.setDiscountAmount(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP));
        payment.setNetAmount(grossAmount);
        payment.setCalculationBaseAmount(inferLegacyCalculationBaseAmount(jobPosition, grossAmount));
        payment.setPaymentModeSnapshot(jobPosition != null && jobPosition.getPaymentMode() != null
                ? jobPosition.getPaymentMode().name()
                : "LEGACY");
        payment.setCommissionRateSnapshot(jobPosition != null
                ? normalizeMoney(jobPosition.getCommissionRate())
                : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP));
        payment.setSalaryPeriodSnapshot(jobPosition != null && jobPosition.getSalaryPeriod() != null
                ? jobPosition.getSalaryPeriod().name()
                : null);
        payment.setObservation(buildLegacyObservation(expense.getObservation()));

        return payment;
    }

    private BigDecimal inferLegacyCalculationBaseAmount(JobPosition jobPosition, BigDecimal grossAmount) {
        if (jobPosition == null || jobPosition.getPaymentMode() == null) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }

        if (jobPosition.getPaymentMode() != PaymentMode.COMMISSION) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }

        BigDecimal commissionRate = normalizeMoney(jobPosition.getCommissionRate());
        if (commissionRate.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }

        return grossAmount.multiply(new BigDecimal("100"))
                .divide(commissionRate, 2, RoundingMode.HALF_UP);
    }

    private String buildLegacyObservation(String originalObservation) {
        String cleanObservation = cleanText(originalObservation);
        if (cleanObservation == null) {
            return "Legacy expense | Historical personnel payment imported from expense.";
        }

        return "Legacy expense | " + cleanObservation;
    }

    private BigDecimal resolveObligationAppliedAmount(
            EmployeeObligation obligation,
            BigDecimal grossAmount,
            BigDecimal requestedAmount
    ) {
        if (obligation == null) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }

        BigDecimal pendingAmount = normalizeMoney(obligation.getPendingAmount());
        if (pendingAmount.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }

        BigDecimal appliedAmount = normalizeMoney(requestedAmount);
        if (appliedAmount.compareTo(BigDecimal.ZERO) <= 0) {
            appliedAmount = switch (obligation.getDiscountMode()) {
                case FIXED_PER_PAYMENT -> normalizeMoney(obligation.getFixedDiscountAmount());
                case PERCENTAGE -> normalizeMoney(
                        grossAmount.multiply(normalizeMoney(obligation.getDiscountPercentage()))
                                .divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP)
                );
                case MANUAL -> BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
            };
        }

        if (appliedAmount.compareTo(pendingAmount) > 0) {
            appliedAmount = pendingAmount;
        }

        if (appliedAmount.compareTo(grossAmount) > 0) {
            appliedAmount = grossAmount;
        }

        return normalizeMoney(appliedAmount);
    }

    private BigDecimal sumPaymentsGross(List<EmployeePayment> payments) {
        return normalizeMoney(
                payments.stream()
                        .map(EmployeePayment::getGrossAmount)
                        .filter(item -> item != null)
                        .reduce(BigDecimal.ZERO, BigDecimal::add)
        );
    }

    private BigDecimal sumPaymentsDiscount(List<EmployeePayment> payments) {
        return normalizeMoney(
                payments.stream()
                        .map(EmployeePayment::getDiscountAmount)
                        .filter(item -> item != null)
                        .reduce(BigDecimal.ZERO, BigDecimal::add)
        );
    }

    private BigDecimal sumPaymentsNet(List<EmployeePayment> payments) {
        return normalizeMoney(
                payments.stream()
                        .map(EmployeePayment::getNetAmount)
                        .filter(item -> item != null)
                        .reduce(BigDecimal.ZERO, BigDecimal::add)
        );
    }

    private BigDecimal sumPendingObligations(List<EmployeeObligation> obligations) {
        return normalizeMoney(
                obligations.stream()
                        .map(EmployeeObligation::getPendingAmount)
                        .filter(item -> item != null)
                        .reduce(BigDecimal.ZERO, BigDecimal::add)
        );
    }

    private String buildEmployeeDisplayName(Employee employee) {
        if (employee == null) {
            return "";
        }

        String firstName = employee.getFirstName() != null ? employee.getFirstName().trim() : "";
        String lastName = employee.getLastName() != null ? employee.getLastName().trim() : "";

        return (firstName + " " + lastName).trim();
    }

    private String normalizeText(String value) {
        if (value == null) {
            return "";
        }

        return Normalizer.normalize(value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase()
                .trim();
    }

    private String cleanText(String value) {
        if (value == null) {
            return null;
        }

        String trimmed = value.trim();
        return trimmed.isBlank() ? null : trimmed;
    }

    private BigDecimal normalizeMoney(BigDecimal value) {
        if (value == null) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }

        return value.setScale(2, RoundingMode.HALF_UP);
    }
}
