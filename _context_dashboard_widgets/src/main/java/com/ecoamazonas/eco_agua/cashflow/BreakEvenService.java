package com.ecoamazonas.eco_agua.cashflow;

import com.ecoamazonas.eco_agua.client.ClientProfile;
import com.ecoamazonas.eco_agua.client.ClientProfileRepository;
import com.ecoamazonas.eco_agua.expense.ExpenseService;
import com.ecoamazonas.eco_agua.order.SaleOrderItemRepository;
import com.ecoamazonas.eco_agua.product.Product;
import com.ecoamazonas.eco_agua.product.ProductRepository;
import com.ecoamazonas.eco_agua.product.cost.ProductCostDetail;
import com.ecoamazonas.eco_agua.product.cost.ProductCostService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class BreakEvenService {

    private static final BigDecimal ZERO = BigDecimal.ZERO;
    private static final BigDecimal ONE_HUNDRED = BigDecimal.valueOf(100);

    private final ExpenseService expenseService;
    private final ProductRepository productRepository;
    private final ProductCostService productCostService;
    private final SaleOrderItemRepository saleOrderItemRepository;
    private final ClientProfileRepository clientProfileRepository;

    public BreakEvenService(
            ExpenseService expenseService,
            ProductRepository productRepository,
            ProductCostService productCostService,
            SaleOrderItemRepository saleOrderItemRepository,
            ClientProfileRepository clientProfileRepository
    ) {
        this.expenseService = expenseService;
        this.productRepository = productRepository;
        this.productCostService = productCostService;
        this.saleOrderItemRepository = saleOrderItemRepository;
        this.clientProfileRepository = clientProfileRepository;
    }

    @Transactional(readOnly = true)
    public BreakEvenResult calculateForProductAndPeriod(Long productId, LocalDate start, LocalDate end) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("Product not found: " + productId));

        Map<String, BigDecimal> fixedCostBreakdown = expenseService.getFixedCostsByCategory(start, end);
        BigDecimal fixedCosts = normalizeScale(expenseService.getTotalFixedCosts(start, end), 2);
        List<BreakEvenFixedCostLine> fixedCostLines = buildFixedCostLines(fixedCostBreakdown);

        ProductCostDetail costDetail = productCostService.calculateCostDetail(productId);
        BigDecimal baseIndustrialUnitCost = normalizeScale(costDetail.getCvu(), 4);

        BigDecimal totalUnitsSold = normalizeScale(
                saleOrderItemRepository.sumQuantitySoldByProductAndPeriod(productId, start, end),
                2
        );
        BigDecimal totalRevenueInPeriod = normalizeScale(
                saleOrderItemRepository.sumRevenueSoldByProductAndPeriod(productId, start, end),
                2
        );
        BigDecimal actualAverageSellingPrice = calculateAveragePrice(totalRevenueInPeriod, totalUnitsSold);

        BigDecimal operationalVariablePersonnelTotal = normalizeScale(
                expenseService.getOperationalVariablePersonnelTotal(start, end),
                2
        );
        BigDecimal operationalVariableNonPersonnelTotal = normalizeScale(
                expenseService.getOperationalVariableNonPersonnelTotal(start, end),
                2
        );
        BigDecimal operationalVariableTotal = operationalVariablePersonnelTotal
                .add(operationalVariableNonPersonnelTotal)
                .setScale(2, RoundingMode.HALF_UP);
        BigDecimal operationalVariableUnitCost = calculateAveragePrice(operationalVariableTotal, totalUnitsSold)
                .setScale(4, RoundingMode.HALF_UP);

        boolean usesProfilePricing = product.usesClientProfilePrice();
        List<BreakEvenScenario> scenarios = buildStructuralScenarios(
                product,
                usesProfilePricing,
                baseIndustrialUnitCost,
                fixedCosts,
                totalUnitsSold,
                totalRevenueInPeriod,
                actualAverageSellingPrice,
                start,
                end
        );

        BreakEvenScenario fallbackScenario = scenarios.isEmpty()
                ? createScenario(
                        "Sin escenario",
                        "No hay precios disponibles para este producto.",
                        ZERO,
                        actualAverageSellingPrice,
                        fixedCosts,
                        baseIndustrialUnitCost,
                        totalUnitsSold,
                        totalRevenueInPeriod,
                        true
                )
                : scenarios.get(0);

        BreakEvenScenario primaryScenario = scenarios.stream()
                .filter(BreakEvenScenario::isPrimary)
                .findFirst()
                .orElse(fallbackScenario);

        BigDecimal structuralGapUnits = positiveDifference(primaryScenario.getBreakEvenUnitsRounded(), totalUnitsSold, 2);
        BigDecimal structuralGapSales = structuralGapUnits.multiply(primaryScenario.getSellingPrice())
                .setScale(2, RoundingMode.HALF_UP);

        BigDecimal operationalContributionMargin = actualAverageSellingPrice
                .subtract(baseIndustrialUnitCost)
                .subtract(operationalVariableUnitCost)
                .setScale(4, RoundingMode.HALF_UP);

        BigDecimal operationalBreakEvenUnitsExact = ZERO.setScale(4, RoundingMode.HALF_UP);
        BigDecimal operationalBreakEvenUnitsRounded = ZERO.setScale(0, RoundingMode.HALF_UP);
        if (operationalContributionMargin.compareTo(ZERO) > 0) {
            operationalBreakEvenUnitsExact = fixedCosts.divide(operationalContributionMargin, 4, RoundingMode.HALF_UP);
            operationalBreakEvenUnitsRounded = operationalBreakEvenUnitsExact.setScale(0, RoundingMode.CEILING);
        }

        BigDecimal operationalGapUnits = positiveDifference(operationalBreakEvenUnitsRounded, totalUnitsSold, 2);
        BigDecimal operationalGapSales = operationalGapUnits.multiply(actualAverageSellingPrice)
                .setScale(2, RoundingMode.HALF_UP);

        BigDecimal operationalContributionTotalBeforeFixed = operationalContributionMargin
                .multiply(totalUnitsSold)
                .setScale(2, RoundingMode.HALF_UP);
        BigDecimal operationalResultAfterFixed = operationalContributionTotalBeforeFixed
                .subtract(fixedCosts)
                .setScale(2, RoundingMode.HALF_UP);

        boolean operationalReadingAvailable = totalUnitsSold.compareTo(ZERO) > 0;

        return new BreakEvenResult(
                product,
                start,
                end,
                fixedCosts,
                baseIndustrialUnitCost,
                baseIndustrialUnitCost,
                totalUnitsSold,
                totalRevenueInPeriod,
                actualAverageSellingPrice,
                usesProfilePricing,
                primaryScenario.getLabel(),
                primaryScenario.getSellingPrice(),
                primaryScenario.getContributionMargin(),
                primaryScenario.getBreakEvenUnitsExact(),
                primaryScenario.getBreakEvenUnitsRounded(),
                primaryScenario.getUnitsSold(),
                primaryScenario.getStatus(),
                primaryScenario.getSafetyMarginUnits(),
                primaryScenario.getSafetyMarginPercent(),
                structuralGapUnits,
                structuralGapSales,
                operationalVariableTotal,
                operationalVariablePersonnelTotal,
                operationalVariableNonPersonnelTotal,
                operationalVariableUnitCost,
                operationalContributionMargin,
                operationalBreakEvenUnitsExact,
                operationalBreakEvenUnitsRounded,
                operationalGapUnits,
                operationalGapSales,
                operationalContributionTotalBeforeFixed,
                operationalResultAfterFixed,
                operationalReadingAvailable,
                expenseService.getOperationalVariableByCategory(start, end),
                scenarios,
                fixedCostLines
        );
    }

    private List<BreakEvenScenario> buildStructuralScenarios(
            Product product,
            boolean usesProfilePricing,
            BigDecimal baseIndustrialUnitCost,
            BigDecimal fixedCosts,
            BigDecimal totalUnitsSold,
            BigDecimal totalRevenueInPeriod,
            BigDecimal actualAverageSellingPrice,
            LocalDate start,
            LocalDate end
    ) {
        List<BreakEvenScenario> scenarios = new ArrayList<>();

        if (usesProfilePricing && actualAverageSellingPrice.compareTo(ZERO) > 0) {
            scenarios.add(createScenario(
                    "Precio promedio real del período",
                    "Calculado con las ventas cerradas del rango.",
                    actualAverageSellingPrice,
                    actualAverageSellingPrice,
                    fixedCosts,
                    baseIndustrialUnitCost,
                    totalUnitsSold,
                    totalRevenueInPeriod,
                    true
            ));
        }

        List<ClientProfile> activeProfiles = clientProfileRepository.findAll()
                .stream()
                .filter(ClientProfile::isActive)
                .sorted(Comparator
                        .comparing(ClientProfile::getSuggestedPrice, Comparator.nullsLast(BigDecimal::compareTo))
                        .thenComparing(ClientProfile::getName, String.CASE_INSENSITIVE_ORDER))
                .collect(Collectors.toList());

        for (ClientProfile profile : activeProfiles) {
            BigDecimal profilePrice = profile.getSuggestedPrice() != null ? profile.getSuggestedPrice() : ZERO;
            BigDecimal profileUnitsSold = normalizeScale(
                    saleOrderItemRepository.sumQuantitySoldByProductAndProfileAndPeriod(
                            product.getId(),
                            profile.getId(),
                            start,
                            end
                    ),
                    2
            );
            BigDecimal profileRevenue = normalizeScale(
                    saleOrderItemRepository.sumRevenueSoldByProductAndProfileAndPeriod(
                            product.getId(),
                            profile.getId(),
                            start,
                            end
                    ),
                    2
            );
            BigDecimal realizedAverage = calculateAveragePrice(profileRevenue, profileUnitsSold);

            scenarios.add(createScenario(
                    profile.getName(),
                    "Escenario estructural usando el precio sugerido del perfil.",
                    profilePrice,
                    realizedAverage,
                    fixedCosts,
                    baseIndustrialUnitCost,
                    profileUnitsSold,
                    profileRevenue,
                    false
            ));
        }

        if (!usesProfilePricing || product.getPrice() != null) {
            BigDecimal productPrice = product.getPrice() != null ? product.getPrice() : ZERO;
            scenarios.add(createScenario(
                    usesProfilePricing ? "Precio actual del producto (referencia)" : "Precio actual del producto",
                    usesProfilePricing
                            ? "Referencia legacy. El negocio principal vende por perfil."
                            : "Escenario estructural con el precio principal actual.",
                    productPrice,
                    actualAverageSellingPrice,
                    fixedCosts,
                    baseIndustrialUnitCost,
                    totalUnitsSold,
                    totalRevenueInPeriod,
                    !usesProfilePricing && scenarios.isEmpty()
            ));
        }

        return scenarios;
    }

    private List<BreakEvenFixedCostLine> buildFixedCostLines(Map<String, BigDecimal> breakdown) {
        List<BreakEvenFixedCostLine> lines = new ArrayList<>();
        for (Map.Entry<String, BigDecimal> entry : breakdown.entrySet()) {
            lines.add(new BreakEvenFixedCostLine(entry.getKey(), normalizeScale(entry.getValue(), 2)));
        }
        return lines;
    }

    private BreakEvenScenario createScenario(
            String label,
            String subtitle,
            BigDecimal sellingPrice,
            BigDecimal realizedAveragePrice,
            BigDecimal fixedCosts,
            BigDecimal variableUnitCost,
            BigDecimal unitsSold,
            BigDecimal revenueInPeriod,
            boolean primary
    ) {
        BigDecimal safeSellingPrice = normalizeScale(sellingPrice, 2);
        BigDecimal safeRealizedAveragePrice = normalizeScale(realizedAveragePrice, 2);
        BigDecimal safeUnitsSold = normalizeScale(unitsSold, 2);
        BigDecimal safeRevenue = normalizeScale(revenueInPeriod, 2);

        BigDecimal contributionMargin = safeSellingPrice.subtract(variableUnitCost).setScale(4, RoundingMode.HALF_UP);
        if (contributionMargin.compareTo(ZERO) < 0) {
            contributionMargin = ZERO.setScale(4, RoundingMode.HALF_UP);
        }

        BigDecimal breakEvenExact = ZERO.setScale(4, RoundingMode.HALF_UP);
        BigDecimal breakEvenRounded = ZERO.setScale(0, RoundingMode.HALF_UP);
        if (contributionMargin.compareTo(ZERO) > 0) {
            breakEvenExact = fixedCosts.divide(contributionMargin, 4, RoundingMode.HALF_UP);
            breakEvenRounded = breakEvenExact.setScale(0, RoundingMode.CEILING);
        }

        BreakEvenStatus status = BreakEvenStatus.BEFORE_BREAK_EVEN;
        if (breakEvenRounded.compareTo(ZERO) > 0) {
            int cmp = safeUnitsSold.compareTo(breakEvenRounded);
            if (cmp < 0) {
                status = BreakEvenStatus.BEFORE_BREAK_EVEN;
            } else if (cmp == 0) {
                status = BreakEvenStatus.AT_BREAK_EVEN;
            } else {
                status = BreakEvenStatus.AFTER_BREAK_EVEN;
            }
        }

        BigDecimal safetyUnits = safeUnitsSold.subtract(breakEvenRounded).setScale(2, RoundingMode.HALF_UP);
        BigDecimal safetyPercent = ZERO.setScale(2, RoundingMode.HALF_UP);
        if (breakEvenRounded.compareTo(ZERO) > 0) {
            safetyPercent = safetyUnits
                    .divide(breakEvenRounded, 4, RoundingMode.HALF_UP)
                    .multiply(ONE_HUNDRED)
                    .setScale(2, RoundingMode.HALF_UP);
        }

        BigDecimal safeVariableUnitCost = normalizeScale(variableUnitCost, 4);

        return new BreakEvenScenario(
                label,
                subtitle,
                safeSellingPrice,
                safeRealizedAveragePrice,
                safeVariableUnitCost,
                ZERO.setScale(4, RoundingMode.HALF_UP),
                ZERO.setScale(2, RoundingMode.HALF_UP),
                ZERO.setScale(4, RoundingMode.HALF_UP),
                ZERO.setScale(4, RoundingMode.HALF_UP),
                safeVariableUnitCost,
                contributionMargin,
                breakEvenExact,
                breakEvenRounded,
                safeUnitsSold,
                safeRevenue,
                status,
                safetyUnits,
                safetyPercent,
                primary
        );
    }

    private BigDecimal calculateAveragePrice(BigDecimal revenue, BigDecimal units) {
        if (revenue == null || units == null || units.compareTo(ZERO) <= 0) {
            return ZERO.setScale(2, RoundingMode.HALF_UP);
        }

        return revenue.divide(units, 2, RoundingMode.HALF_UP);
    }

    private BigDecimal positiveDifference(BigDecimal target, BigDecimal current, int scale) {
        BigDecimal safeTarget = normalizeScale(target, scale);
        BigDecimal safeCurrent = normalizeScale(current, scale);
        BigDecimal diff = safeTarget.subtract(safeCurrent).setScale(scale, RoundingMode.HALF_UP);
        return diff.compareTo(ZERO) > 0 ? diff : ZERO.setScale(scale, RoundingMode.HALF_UP);
    }

    private BigDecimal normalizeScale(BigDecimal value, int scale) {
        if (value == null) {
            return ZERO.setScale(scale, RoundingMode.HALF_UP);
        }
        return value.setScale(scale, RoundingMode.HALF_UP);
    }
}
