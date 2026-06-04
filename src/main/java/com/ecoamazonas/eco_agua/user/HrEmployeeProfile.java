package com.ecoamazonas.eco_agua.user;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class HrEmployeeProfile {

    private Long employeeId;
    private String employeeName;
    private String firstName;
    private String lastName;
    private String dni;
    private String email;
    private String phone;
    private String address;
    private LocalDate birthDate;
    private LocalDate hireDate;
    private boolean active;
    private String jobPositionName;
    private String paymentModeLabel;
    private String salaryPeriodLabel;
    private BigDecimal baseSalary = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
    private BigDecimal salaryAmount = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
    private BigDecimal commissionRate = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
    private Integer selectedYear;
    private Integer selectedMonth;
    private String selectedMonthName;
    private EmployeeMonthlyPaymentSummary summary = new EmployeeMonthlyPaymentSummary();
    private List<HrDashboardPaymentRow> payments = new ArrayList<>();
    private List<HrDashboardObligationRow> obligations = new ArrayList<>();
    private List<HrEmployeeProfileSettlementRow> settlements = new ArrayList<>();

    public Long getEmployeeId() {
        return employeeId;
    }

    public void setEmployeeId(Long employeeId) {
        this.employeeId = employeeId;
    }

    public String getEmployeeName() {
        return employeeName;
    }

    public void setEmployeeName(String employeeName) {
        this.employeeName = employeeName;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getDni() {
        return dni;
    }

    public void setDni(String dni) {
        this.dni = dni;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public LocalDate getBirthDate() {
        return birthDate;
    }

    public void setBirthDate(LocalDate birthDate) {
        this.birthDate = birthDate;
    }

    public LocalDate getHireDate() {
        return hireDate;
    }

    public void setHireDate(LocalDate hireDate) {
        this.hireDate = hireDate;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public String getJobPositionName() {
        return jobPositionName;
    }

    public void setJobPositionName(String jobPositionName) {
        this.jobPositionName = jobPositionName;
    }

    public String getPaymentModeLabel() {
        return paymentModeLabel;
    }

    public void setPaymentModeLabel(String paymentModeLabel) {
        this.paymentModeLabel = paymentModeLabel;
    }

    public String getSalaryPeriodLabel() {
        return salaryPeriodLabel;
    }

    public void setSalaryPeriodLabel(String salaryPeriodLabel) {
        this.salaryPeriodLabel = salaryPeriodLabel;
    }

    public BigDecimal getBaseSalary() {
        return baseSalary;
    }

    public void setBaseSalary(BigDecimal baseSalary) {
        this.baseSalary = normalizeMoney(baseSalary);
    }

    public BigDecimal getSalaryAmount() {
        return salaryAmount;
    }

    public void setSalaryAmount(BigDecimal salaryAmount) {
        this.salaryAmount = normalizeMoney(salaryAmount);
    }

    public BigDecimal getCommissionRate() {
        return commissionRate;
    }

    public void setCommissionRate(BigDecimal commissionRate) {
        this.commissionRate = normalizeMoney(commissionRate);
    }

    public Integer getSelectedYear() {
        return selectedYear;
    }

    public void setSelectedYear(Integer selectedYear) {
        this.selectedYear = selectedYear;
    }

    public Integer getSelectedMonth() {
        return selectedMonth;
    }

    public void setSelectedMonth(Integer selectedMonth) {
        this.selectedMonth = selectedMonth;
    }

    public String getSelectedMonthName() {
        return selectedMonthName;
    }

    public void setSelectedMonthName(String selectedMonthName) {
        this.selectedMonthName = selectedMonthName;
    }

    public EmployeeMonthlyPaymentSummary getSummary() {
        return summary;
    }

    public void setSummary(EmployeeMonthlyPaymentSummary summary) {
        this.summary = summary != null ? summary : new EmployeeMonthlyPaymentSummary();
    }

    public List<HrDashboardPaymentRow> getPayments() {
        return payments;
    }

    public void setPayments(List<HrDashboardPaymentRow> payments) {
        this.payments = payments != null ? payments : new ArrayList<>();
    }

    public List<HrDashboardObligationRow> getObligations() {
        return obligations;
    }

    public void setObligations(List<HrDashboardObligationRow> obligations) {
        this.obligations = obligations != null ? obligations : new ArrayList<>();
    }

    public List<HrEmployeeProfileSettlementRow> getSettlements() {
        return settlements;
    }

    public void setSettlements(List<HrEmployeeProfileSettlementRow> settlements) {
        this.settlements = settlements != null ? settlements : new ArrayList<>();
    }

    public boolean hasContactInfo() {
        return hasText(dni) || hasText(email) || hasText(phone) || hasText(address);
    }

    public boolean hasJobPosition() {
        return hasText(jobPositionName) && !"Sin cargo asignado".equalsIgnoreCase(jobPositionName);
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private BigDecimal normalizeMoney(BigDecimal value) {
        if (value == null) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }

        return value.setScale(2, RoundingMode.HALF_UP);
    }
}
