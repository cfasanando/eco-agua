package com.ecoamazonas.eco_agua.dashboard;

import com.ecoamazonas.eco_agua.marketing.MarketingCampaignsService;
import com.ecoamazonas.eco_agua.marketing.MarketingCampaignsSnapshot;
import com.ecoamazonas.eco_agua.product.Product;
import com.ecoamazonas.eco_agua.product.ProductRepository;
import com.ecoamazonas.eco_agua.user.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

@Service
public class DashboardAreaWidgetService {

    private final MarketingCampaignsService marketingCampaignsService;
    private final ProductRepository productRepository;
    private final EmployeeRepository employeeRepository;
    private final JobPositionRepository jobPositionRepository;
    private final UserAccountRepository userAccountRepository;
    private final RoleRepository roleRepository;
    private final EmployeeObligationRepository employeeObligationRepository;

    public DashboardAreaWidgetService(
            MarketingCampaignsService marketingCampaignsService,
            ProductRepository productRepository,
            EmployeeRepository employeeRepository,
            JobPositionRepository jobPositionRepository,
            UserAccountRepository userAccountRepository,
            RoleRepository roleRepository,
            EmployeeObligationRepository employeeObligationRepository
    ) {
        this.marketingCampaignsService = marketingCampaignsService;
        this.productRepository = productRepository;
        this.employeeRepository = employeeRepository;
        this.jobPositionRepository = jobPositionRepository;
        this.userAccountRepository = userAccountRepository;
        this.roleRepository = roleRepository;
        this.employeeObligationRepository = employeeObligationRepository;
    }

    @Transactional(readOnly = true)
    public MarketingWidgetSnapshot buildMarketingSnapshot() {
        MarketingCampaignsSnapshot campaigns = marketingCampaignsService.buildSnapshot();
        List<Product> featuredProducts = new ArrayList<>(productRepository.findTop4ByActiveTrueAndFeaturedTrueOrderByIdDesc());
        if (featuredProducts.isEmpty()) {
            featuredProducts = new ArrayList<>(productRepository.findTop8ByActiveTrueOrderByIdDesc()).stream()
                    .limit(4)
                    .toList();
        }

        return new MarketingWidgetSnapshot(campaigns, featuredProducts);
    }

    @Transactional(readOnly = true)
    public HrWidgetSnapshot buildHrSnapshot() {
        List<Employee> activeEmployees = employeeRepository.findByActiveTrueOrderByFirstNameAscLastNameAsc();
        List<JobPosition> activePositions = jobPositionRepository.findActive();
        List<UserAccount> users = userAccountRepository.findAll();
        List<Role> roles = roleRepository.findAll();
        List<EmployeeObligation> activeObligations = employeeObligationRepository.findAll().stream()
                .filter(EmployeeObligation::isActive)
                .toList();

        long activeUserCount = users.stream().filter(UserAccount::isActive).count();
        BigDecimal pendingObligationAmount = activeObligations.stream()
                .map(EmployeeObligation::getPendingAmount)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        List<EmployeeRow> recentEmployees = activeEmployees.stream()
                .sorted(Comparator.comparing(Employee::getHireDate, Comparator.nullsLast(Comparator.reverseOrder())))
                .limit(5)
                .map(this::toEmployeeRow)
                .toList();

        List<PositionRow> positionRows = activePositions.stream()
                .limit(5)
                .map(position -> new PositionRow(
                        position.getName(),
                        position.getDisplayName(),
                        position.getPaymentModeLabel()
                ))
                .toList();

        return new HrWidgetSnapshot(
                activeEmployees.size(),
                activePositions.size(),
                (int) activeUserCount,
                roles.size(),
                activeObligations.size(),
                pendingObligationAmount,
                recentEmployees,
                positionRows
        );
    }

    private EmployeeRow toEmployeeRow(Employee employee) {
        String fullName = firstNonBlank(joinName(employee.getFirstName(), employee.getLastName()), "Unnamed employee");
        String jobName = employee.getJobPosition() != null ? employee.getJobPosition().getName() : "No position";
        String contact = firstNonBlank(employee.getPhone(), employee.getEmail(), employee.getDni(), "No contact data");
        return new EmployeeRow(fullName, jobName, contact);
    }

    private String joinName(String firstName, String lastName) {
        return ((firstName != null ? firstName : "") + " " + (lastName != null ? lastName : "")).trim();
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value.trim();
            }
        }
        return "";
    }

    public static class MarketingWidgetSnapshot {
        private final MarketingCampaignsSnapshot campaigns;
        private final List<Product> featuredProducts;

        public MarketingWidgetSnapshot(MarketingCampaignsSnapshot campaigns, List<Product> featuredProducts) {
            this.campaigns = campaigns;
            this.featuredProducts = featuredProducts;
        }

        public MarketingCampaignsSnapshot getCampaigns() {
            return campaigns;
        }

        public List<Product> getFeaturedProducts() {
            return featuredProducts;
        }
    }

    public static class HrWidgetSnapshot {
        private final int activeEmployeeCount;
        private final int activePositionCount;
        private final int activeUserCount;
        private final int roleCount;
        private final int activeObligationCount;
        private final BigDecimal pendingObligationAmount;
        private final List<EmployeeRow> recentEmployees;
        private final List<PositionRow> positionRows;

        public HrWidgetSnapshot(
                int activeEmployeeCount,
                int activePositionCount,
                int activeUserCount,
                int roleCount,
                int activeObligationCount,
                BigDecimal pendingObligationAmount,
                List<EmployeeRow> recentEmployees,
                List<PositionRow> positionRows
        ) {
            this.activeEmployeeCount = activeEmployeeCount;
            this.activePositionCount = activePositionCount;
            this.activeUserCount = activeUserCount;
            this.roleCount = roleCount;
            this.activeObligationCount = activeObligationCount;
            this.pendingObligationAmount = pendingObligationAmount;
            this.recentEmployees = recentEmployees;
            this.positionRows = positionRows;
        }

        public int getActiveEmployeeCount() {
            return activeEmployeeCount;
        }

        public int getActivePositionCount() {
            return activePositionCount;
        }

        public int getActiveUserCount() {
            return activeUserCount;
        }

        public int getRoleCount() {
            return roleCount;
        }

        public int getActiveObligationCount() {
            return activeObligationCount;
        }

        public BigDecimal getPendingObligationAmount() {
            return pendingObligationAmount;
        }

        public List<EmployeeRow> getRecentEmployees() {
            return recentEmployees;
        }

        public List<PositionRow> getPositionRows() {
            return positionRows;
        }
    }

    public static class EmployeeRow {
        private final String fullName;
        private final String jobName;
        private final String contact;

        public EmployeeRow(String fullName, String jobName, String contact) {
            this.fullName = fullName;
            this.jobName = jobName;
            this.contact = contact;
        }

        public String getFullName() {
            return fullName;
        }

        public String getJobName() {
            return jobName;
        }

        public String getContact() {
            return contact;
        }
    }

    public static class PositionRow {
        private final String name;
        private final String displayName;
        private final String paymentModeLabel;

        public PositionRow(String name, String displayName, String paymentModeLabel) {
            this.name = name;
            this.displayName = displayName;
            this.paymentModeLabel = paymentModeLabel;
        }

        public String getName() {
            return name;
        }

        public String getDisplayName() {
            return displayName;
        }

        public String getPaymentModeLabel() {
            return paymentModeLabel;
        }
    }
}
