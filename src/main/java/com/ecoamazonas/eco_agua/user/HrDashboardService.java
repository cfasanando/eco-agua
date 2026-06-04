package com.ecoamazonas.eco_agua.user;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

@Service
public class HrDashboardService {

    private static final int RECENT_PAYMENT_LIMIT = 8;
    private static final int PENDING_OBLIGATION_LIMIT = 8;

    private final EmployeePaymentService employeePaymentService;

    public HrDashboardService(EmployeePaymentService employeePaymentService) {
        this.employeePaymentService = employeePaymentService;
    }

    @Transactional(readOnly = true)
    public HrDashboardSnapshot buildSnapshot(Integer year, Integer month) {
        YearMonth selectedPeriod = resolvePeriod(year, month);
        List<Employee> employees = employeePaymentService.findActiveEmployees();
        List<HrDashboardEmployeeRow> employeeRows = new ArrayList<>();
        List<HrDashboardPaymentRow> allPayments = new ArrayList<>();
        List<HrDashboardObligationRow> allPendingObligations = new ArrayList<>();

        for (Employee employee : employees) {
            List<EmployeePayment> payments = employeePaymentService.findPaymentsForMonth(
                    employee.getId(),
                    selectedPeriod.getYear(),
                    selectedPeriod.getMonthValue()
            );
            List<EmployeeObligation> activeObligations = employeePaymentService.findActiveObligations(employee.getId());

            employeeRows.add(buildEmployeeRow(employee, payments, activeObligations));
            payments.stream()
                    .map(payment -> buildPaymentRow(employee, payment))
                    .forEach(allPayments::add);
            activeObligations.stream()
                    .map(obligation -> buildObligationRow(employee, obligation))
                    .forEach(allPendingObligations::add);
        }

        employeeRows.sort(Comparator
                .comparing(HrDashboardEmployeeRow::hasPendingObligations).reversed()
                .thenComparing(HrDashboardEmployeeRow::getEmployeeName, String.CASE_INSENSITIVE_ORDER));

        allPayments.sort(Comparator
                .comparing(HrDashboardPaymentRow::getPaymentDate, Comparator.nullsLast(Comparator.reverseOrder()))
                .thenComparing(HrDashboardPaymentRow::getPaymentId, Comparator.nullsLast(Comparator.reverseOrder())));

        allPendingObligations.sort(Comparator
                .comparing(HrDashboardObligationRow::getPendingAmount, Comparator.nullsLast(Comparator.reverseOrder()))
                .thenComparing(HrDashboardObligationRow::getIssueDate, Comparator.nullsLast(Comparator.reverseOrder())));

        HrDashboardSnapshot snapshot = new HrDashboardSnapshot();
        snapshot.setSelectedYear(selectedPeriod.getYear());
        snapshot.setSelectedMonth(selectedPeriod.getMonthValue());
        snapshot.setSelectedMonthName(buildMonthName(selectedPeriod));
        snapshot.setEmployeeRows(employeeRows);
        snapshot.setRecentPayments(limit(allPayments, RECENT_PAYMENT_LIMIT));
        snapshot.setPendingObligations(limit(allPendingObligations, PENDING_OBLIGATION_LIMIT));
        snapshot.setSummary(buildSummary(employeeRows, allPayments, allPendingObligations));

        return snapshot;
    }

    @Transactional(readOnly = true)
    public HrEmployeeProfile buildEmployeeProfile(Long employeeId, Integer year, Integer month) {
        YearMonth selectedPeriod = resolvePeriod(year, month);
        Employee employee = employeePaymentService.findEmployee(employeeId);
        if (employee == null) {
            throw new IllegalArgumentException("Employee not found: " + employeeId);
        }

        List<EmployeePayment> payments = employeePaymentService.findPaymentsForMonth(
                employee.getId(),
                selectedPeriod.getYear(),
                selectedPeriod.getMonthValue()
        );
        List<EmployeeObligation> obligations = employeePaymentService.findObligations(employee.getId());
        List<EmployeeObligationSettlement> settlements = employeePaymentService.findSettlementsForMonth(
                employee.getId(),
                selectedPeriod.getYear(),
                selectedPeriod.getMonthValue()
        );

        HrEmployeeProfile profile = new HrEmployeeProfile();
        profile.setEmployeeId(employee.getId());
        profile.setEmployeeName(buildEmployeeName(employee));
        profile.setFirstName(cleanText(employee.getFirstName()));
        profile.setLastName(cleanText(employee.getLastName()));
        profile.setDni(cleanText(employee.getDni()));
        profile.setEmail(cleanText(employee.getEmail()));
        profile.setPhone(cleanText(employee.getPhone()));
        profile.setAddress(cleanText(employee.getAddress()));
        profile.setBirthDate(employee.getBirthDate());
        profile.setHireDate(employee.getHireDate());
        profile.setActive(employee.isActive());
        profile.setSelectedYear(selectedPeriod.getYear());
        profile.setSelectedMonth(selectedPeriod.getMonthValue());
        profile.setSelectedMonthName(buildMonthName(selectedPeriod));
        profile.setSummary(employeePaymentService.buildMonthlySummary(
                employee.getId(),
                selectedPeriod.getYear(),
                selectedPeriod.getMonthValue()
        ));
        profile.setPayments(payments.stream()
                .map(payment -> buildPaymentRow(employee, payment))
                .toList());
        profile.setObligations(obligations.stream()
                .map(obligation -> buildObligationRow(employee, obligation))
                .toList());
        profile.setSettlements(settlements.stream()
                .map(this::buildSettlementRow)
                .toList());

        JobPosition jobPosition = employee.getJobPosition();
        if (jobPosition != null) {
            profile.setJobPositionName(cleanText(jobPosition.getName()));
            profile.setPaymentModeLabel(jobPosition.getPaymentModeLabel());
            profile.setSalaryPeriodLabel(buildSalaryPeriodLabel(jobPosition.getSalaryPeriod()));
            profile.setBaseSalary(jobPosition.getBaseSalary());
            profile.setSalaryAmount(jobPosition.getSalaryAmount());
            profile.setCommissionRate(jobPosition.getCommissionRate());
        } else {
            profile.setJobPositionName("Sin cargo asignado");
            profile.setPaymentModeLabel("Sin regla de pago");
            profile.setSalaryPeriodLabel("Sin periodo definido");
        }

        return profile;
    }

    private HrDashboardEmployeeRow buildEmployeeRow(
            Employee employee,
            List<EmployeePayment> payments,
            List<EmployeeObligation> activeObligations
    ) {
        HrDashboardEmployeeRow row = new HrDashboardEmployeeRow();
        row.setEmployeeId(employee.getId());
        row.setEmployeeName(buildEmployeeName(employee));
        row.setPhone(cleanText(employee.getPhone()));

        JobPosition jobPosition = employee.getJobPosition();
        if (jobPosition != null) {
            row.setJobPositionName(cleanText(jobPosition.getName()));
            row.setPaymentModeLabel(jobPosition.getPaymentModeLabel());
        } else {
            row.setJobPositionName("Sin cargo asignado");
            row.setPaymentModeLabel("Sin regla de pago");
        }

        row.setPaymentCount(payments.size());
        row.setMonthlyGross(sumPaymentGross(payments));
        row.setMonthlyDiscount(sumPaymentDiscount(payments));
        row.setMonthlyNet(sumPaymentNet(payments));
        row.setPendingObligations(sumPendingObligations(activeObligations));
        row.setActiveObligationCount(activeObligations.size());
        row.setLastPaymentDate(payments.stream()
                .map(EmployeePayment::getPaymentDate)
                .filter(date -> date != null)
                .max(LocalDate::compareTo)
                .orElse(null));

        return row;
    }

    private HrDashboardPaymentRow buildPaymentRow(Employee employee, EmployeePayment payment) {
        HrDashboardPaymentRow row = new HrDashboardPaymentRow();
        row.setPaymentId(payment.getId());
        row.setEmployeeId(employee.getId());
        row.setEmployeeName(buildEmployeeName(employee));
        row.setPaymentDate(payment.getPaymentDate());
        row.setGrossAmount(payment.getGrossAmount());
        row.setDiscountAmount(payment.getDiscountAmount());
        row.setNetAmount(payment.getNetAmount());
        row.setObservation(cleanText(payment.getObservation()));
        row.setLegacy(isLegacyPayment(payment));

        return row;
    }

    private HrDashboardObligationRow buildObligationRow(Employee employee, EmployeeObligation obligation) {
        HrDashboardObligationRow row = new HrDashboardObligationRow();
        row.setObligationId(obligation.getId());
        row.setEmployeeId(employee.getId());
        row.setEmployeeName(buildEmployeeName(employee));
        row.setTypeLabel(obligation.getType() != null ? obligation.getType().getLabel() : "Obligación");
        row.setIssueDate(obligation.getIssueDate());
        row.setOriginalAmount(obligation.getOriginalAmount());
        row.setPendingAmount(obligation.getPendingAmount());
        row.setDiscountModeLabel(obligation.getDiscountMode() != null ? obligation.getDiscountMode().getLabel() : "Manual");
        row.setDescription(cleanText(obligation.getDescription()));
        row.setActive(obligation.isActive());

        return row;
    }


    private HrEmployeeProfileSettlementRow buildSettlementRow(EmployeeObligationSettlement settlement) {
        HrEmployeeProfileSettlementRow row = new HrEmployeeProfileSettlementRow();
        row.setSettlementId(settlement.getId());
        row.setSettlementDate(settlement.getSettlementDate());
        row.setAppliedAmount(settlement.getAppliedAmount());
        row.setObservation(cleanText(settlement.getObservation()));

        EmployeeObligation obligation = settlement.getEmployeeObligation();
        if (obligation != null) {
            row.setObligationId(obligation.getId());
            String description = cleanText(obligation.getDescription());
            String typeLabel = obligation.getType() != null ? obligation.getType().getLabel() : "Obligación";
            row.setObligationLabel(description.isBlank() ? typeLabel : description);
        } else {
            row.setObligationLabel("Obligación");
        }

        return row;
    }

    private String buildSalaryPeriodLabel(SalaryPeriod salaryPeriod) {
        if (salaryPeriod == null) {
            return "Sin periodo definido";
        }

        return switch (salaryPeriod) {
            case DAILY -> "Diario";
            case WEEKLY -> "Semanal";
            case BIWEEKLY -> "Quincenal";
            case MONTHLY -> "Mensual";
            case HOURLY -> "Por hora";
        };
    }

    private HrDashboardSummary buildSummary(
            List<HrDashboardEmployeeRow> employeeRows,
            List<HrDashboardPaymentRow> allPayments,
            List<HrDashboardObligationRow> allPendingObligations
    ) {
        HrDashboardSummary summary = new HrDashboardSummary();
        summary.setActiveEmployeeCount(employeeRows.size());
        summary.setPaidEmployeeCount((int) employeeRows.stream().filter(HrDashboardEmployeeRow::hasPayments).count());
        summary.setPaymentCount(allPayments.size());
        summary.setActiveObligationCount(allPendingObligations.size());
        summary.setTotalGross(employeeRows.stream()
                .map(HrDashboardEmployeeRow::getMonthlyGross)
                .reduce(BigDecimal.ZERO, BigDecimal::add));
        summary.setTotalDiscount(employeeRows.stream()
                .map(HrDashboardEmployeeRow::getMonthlyDiscount)
                .reduce(BigDecimal.ZERO, BigDecimal::add));
        summary.setTotalNet(employeeRows.stream()
                .map(HrDashboardEmployeeRow::getMonthlyNet)
                .reduce(BigDecimal.ZERO, BigDecimal::add));
        summary.setTotalPendingObligations(employeeRows.stream()
                .map(HrDashboardEmployeeRow::getPendingObligations)
                .reduce(BigDecimal.ZERO, BigDecimal::add));

        return summary;
    }

    private YearMonth resolvePeriod(Integer year, Integer month) {
        LocalDate today = LocalDate.now();
        int safeYear = year != null && year >= 2000 && year <= 2100 ? year : today.getYear();
        int safeMonth = month != null && month >= 1 && month <= 12 ? month : today.getMonthValue();

        return YearMonth.of(safeYear, safeMonth);
    }

    private String buildMonthName(YearMonth period) {
        Locale locale = Locale.forLanguageTag("es-PE");
        String monthName = period.getMonth().getDisplayName(TextStyle.FULL, locale);
        String normalizedMonthName = monthName == null || monthName.isBlank()
                ? String.valueOf(period.getMonthValue())
                : monthName.substring(0, 1).toUpperCase(locale) + monthName.substring(1);

        return normalizedMonthName + " " + period.getYear();
    }

    private BigDecimal sumPaymentGross(List<EmployeePayment> payments) {
        return payments.stream()
                .map(EmployeePayment::getGrossAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal sumPaymentDiscount(List<EmployeePayment> payments) {
        return payments.stream()
                .map(EmployeePayment::getDiscountAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal sumPaymentNet(List<EmployeePayment> payments) {
        return payments.stream()
                .map(EmployeePayment::getNetAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal sumPendingObligations(List<EmployeeObligation> obligations) {
        return obligations.stream()
                .map(EmployeeObligation::getPendingAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);
    }

    private String buildEmployeeName(Employee employee) {
        String firstName = cleanText(employee.getFirstName());
        String lastName = cleanText(employee.getLastName());
        String fullName = (firstName + " " + lastName).trim();

        return fullName.isBlank() ? "Trabajador sin nombre" : fullName;
    }

    private boolean isLegacyPayment(EmployeePayment payment) {
        String observation = cleanText(payment.getObservation());
        return observation.toLowerCase(Locale.ROOT).startsWith("legacy expense");
    }

    private String cleanText(String value) {
        if (value == null) {
            return "";
        }

        String trimmed = value.trim();
        return trimmed.isBlank() ? "" : trimmed;
    }

    private <T> List<T> limit(List<T> source, int limit) {
        if (source.size() <= limit) {
            return source;
        }

        return source.subList(0, limit);
    }
}
