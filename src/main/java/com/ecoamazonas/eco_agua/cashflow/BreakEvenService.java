package com.ecoamazonas.eco_agua.cashflow;

import com.ecoamazonas.eco_agua.client.ClientProfile;
import com.ecoamazonas.eco_agua.client.ClientProfileRepository;
import com.ecoamazonas.eco_agua.expense.ExpenseRepository;
import com.ecoamazonas.eco_agua.order.SaleOrderItemRepository;
import com.ecoamazonas.eco_agua.product.Product;
import com.ecoamazonas.eco_agua.product.ProductRepository;
import com.ecoamazonas.eco_agua.product.cost.PeriodExpenseLine;
import com.ecoamazonas.eco_agua.product.cost.ProductCostDetail;
import com.ecoamazonas.eco_agua.product.cost.ProductCostLine;
import com.ecoamazonas.eco_agua.product.cost.ProductCostService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.Normalizer;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class BreakEvenService {

    private static final BigDecimal ZERO = BigDecimal.ZERO;
    private static final BigDecimal ONE_HUNDRED = BigDecimal.valueOf(100);
    private static final BigDecimal DEFAULT_DELIVERY_COMMISSION_PERCENT = BigDecimal.valueOf(25);

    /**
     * Fixed-cost categories aligned with the current business model.
     * Fuel and variable personnel are intentionally excluded.
     */
    private static final List<Long> FIXED_COST_CATEGORY_IDS = List.of(
            17L, // Luz local
            18L, // Agua local
            19L, // Alquiler local
            20L, // Contador
            21L, // Sunat
            22L, // Boletas y facturas
            28L, // Cochera
            29L, // Mantenimiento y aceite furgón
            31L, // Detergente
            32L, // Escobilla
            34L  // Declaración IE
    );

    private final ExpenseRepository expenseRepository;
    private final ProductRepository productRepository;
    private final ProductCostService productCostService;
    private final SaleOrderItemRepository saleOrderItemRepository;
    private final ClientProfileRepository clientProfileRepository;

    public BreakEvenService(
            ExpenseRepository expenseRepository,
            ProductRepository productRepository,
            ProductCostService productCostService,
            SaleOrderItemRepository saleOrderItemRepository,
            ClientProfileRepository clientProfileRepository
    ) {
        this.expenseRepository = expenseRepository;
        this.productRepository = productRepository;
        this.productCostService = productCostService;
        this.saleOrderItemRepository = saleOrderItemRepository;
        this.clientProfileRepository = clientProfileRepository;
    }

    @Transactional(readOnly = true)
    public BreakEvenResult calculateForProductAndPeriod(
            Long productId,
            LocalDate start,
            LocalDate end
    ) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("Product not found: " + productId));

        BigDecimal fixedCosts = expenseRepository
                .sumAmountByCategoryIdsAndPeriod(FIXED_COST_CATEGORY_IDS, start, end);
        fixedCosts = normalizeScale(fixedCosts, 2);

        List<BreakEvenFixedCostLine> fixedCostLines = buildFixedCostLines(start, end);

        ProductCostDetail costDetail = productCostService.calculateCostDetail(productId);
        DirectVariableBreakdown variableBreakdown = buildDirectVariableBreakdown(product, costDetail);

        BigDecimal totalUnitsSold = normalizeScale(
                saleOrderItemRepository.sumQuantitySoldByProductAndPeriod(productId, start, end),
                2
        );
        BigDecimal totalRevenueInPeriod = normalizeScale(
                saleOrderItemRepository.sumRevenueSoldByProductAndPeriod(productId, start, end),
                2
        );
        BigDecimal actualAverageSellingPrice = calculateAveragePrice(totalRevenueInPeriod, totalUnitsSold);

        boolean usesProfilePricing = product.usesClientProfilePrice();

        List<BreakEvenScenario> scenarios = new ArrayList<>();
        boolean primaryAssigned = false;

        if (usesProfilePricing && actualAverageSellingPrice.compareTo(ZERO) > 0) {
            scenarios.add(createScenario(
                    "Precio promedio real del período",
                    "Calculado con las ventas cerradas del rango.",
                    actualAverageSellingPrice,
                    actualAverageSellingPrice,
                    fixedCosts,
                    variableBreakdown,
                    totalUnitsSold,
                    totalRevenueInPeriod,
                    true
            ));
            primaryAssigned = true;
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
                            productId,
                            profile.getId(),
                            start,
                            end
                    ),
                    2
            );
            BigDecimal profileRevenue = normalizeScale(
                    saleOrderItemRepository.sumRevenueSoldByProductAndProfileAndPeriod(
                            productId,
                            profile.getId(),
                            start,
                            end
                    ),
                    2
            );
            BigDecimal realizedAverage = calculateAveragePrice(profileRevenue, profileUnitsSold);

            boolean scenarioIsPrimary = !primaryAssigned && profilePrice.compareTo(ZERO) > 0;

            scenarios.add(createScenario(
                    profile.getName(),
                    "Precio sugerido por perfil de cliente.",
                    profilePrice,
                    realizedAverage,
                    fixedCosts,
                    variableBreakdown,
                    profileUnitsSold,
                    profileRevenue,
                    scenarioIsPrimary
            ));

            if (scenarioIsPrimary) {
                primaryAssigned = true;
            }
        }

        if (!usesProfilePricing || product.getPrice() != null) {
            BigDecimal productPrice = product.getPrice() != null ? product.getPrice() : ZERO;
            boolean scenarioIsPrimary = !primaryAssigned;

            scenarios.add(createScenario(
                    usesProfilePricing ? "Precio actual del producto (legacy)" : "Precio actual del producto",
                    usesProfilePricing
                            ? "Se muestra solo como referencia porque el negocio ya vende por perfil."
                            : "Precio principal actual del producto.",
                    productPrice,
                    actualAverageSellingPrice,
                    fixedCosts,
                    variableBreakdown,
                    totalUnitsSold,
                    totalRevenueInPeriod,
                    scenarioIsPrimary
            ));

            if (scenarioIsPrimary) {
                primaryAssigned = true;
            }
        }

        BreakEvenScenario fallbackScenario = scenarios.isEmpty()
                ? createScenario(
                        "Sin escenario",
                        "No hay precios disponibles para este producto.",
                        ZERO,
                        ZERO,
                        fixedCosts,
                        variableBreakdown,
                        ZERO,
                        ZERO,
                        true
                )
                : scenarios.get(0);

        BreakEvenScenario primaryScenario = scenarios.stream()
                .filter(BreakEvenScenario::isPrimary)
                .findFirst()
                .orElse(fallbackScenario);

        return new BreakEvenResult(
                product,
                start,
                end,
                fixedCosts,
                variableBreakdown.baseIndustrialUnitCost(),
                variableBreakdown.fuelVariableUnitCost(),
                primaryScenario.getDeliveryCommissionPercent(),
                primaryScenario.getDeliveryCommissionUnitCost(),
                primaryScenario.getOperationalVariableUnitCost(),
                primaryScenario.getTotalVariableUnitCost(),
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
                scenarios,
                fixedCostLines
        );
    }

    private List<BreakEvenFixedCostLine> buildFixedCostLines(LocalDate start, LocalDate end) {
        List<PeriodExpenseLine> rawLines = expenseRepository
                .sumAmountByCategoryIdsGrouped(FIXED_COST_CATEGORY_IDS, start, end);

        List<BreakEvenFixedCostLine> lines = new ArrayList<>();
        for (PeriodExpenseLine line : rawLines) {
            lines.add(new BreakEvenFixedCostLine(
                    line.getCategoryName(),
                    normalizeScale(line.getTotalAmount(), 2)
            ));
        }
        return lines;
    }

    private BreakEvenScenario createScenario(
            String label,
            String subtitle,
            BigDecimal sellingPrice,
            BigDecimal realizedAveragePrice,
            BigDecimal fixedCosts,
            DirectVariableBreakdown breakdown,
            BigDecimal unitsSold,
            BigDecimal revenueInPeriod,
            boolean primary
    ) {
        BigDecimal safeSellingPrice = normalizeScale(sellingPrice, 2);
        BigDecimal safeRealizedAveragePrice = normalizeScale(realizedAveragePrice, 2);
        BigDecimal safeUnitsSold = normalizeScale(unitsSold, 2);
        BigDecimal safeRevenue = normalizeScale(revenueInPeriod, 2);

        BigDecimal commissionUnitCost = ZERO.setScale(4, RoundingMode.HALF_UP);
        if (breakdown.deliveryCommissionPercent().compareTo(ZERO) > 0 && safeSellingPrice.compareTo(ZERO) > 0) {
            commissionUnitCost = safeSellingPrice
                    .multiply(breakdown.deliveryCommissionPercent())
                    .divide(ONE_HUNDRED, 4, RoundingMode.HALF_UP);
        }

        BigDecimal operationalVariableUnitCost = breakdown.fuelVariableUnitCost()
                .add(commissionUnitCost)
                .setScale(4, RoundingMode.HALF_UP);

        BigDecimal totalVariableUnitCost = breakdown.baseIndustrialUnitCost()
                .add(operationalVariableUnitCost)
                .setScale(4, RoundingMode.HALF_UP);

        BigDecimal contributionMargin = safeSellingPrice
                .subtract(totalVariableUnitCost)
                .setScale(4, RoundingMode.HALF_UP);

        BigDecimal contributionForBreakEven = contributionMargin.compareTo(ZERO) > 0
                ? contributionMargin
                : ZERO.setScale(4, RoundingMode.HALF_UP);

        BigDecimal breakEvenExact = ZERO.setScale(4, RoundingMode.HALF_UP);
        BigDecimal breakEvenRounded = ZERO.setScale(0, RoundingMode.HALF_UP);
        if (contributionForBreakEven.compareTo(ZERO) > 0) {
            breakEvenExact = fixedCosts.divide(contributionForBreakEven, 4, RoundingMode.HALF_UP);
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

        return new BreakEvenScenario(
                label,
                subtitle,
                safeSellingPrice,
                safeRealizedAveragePrice,
                breakdown.baseIndustrialUnitCost(),
                breakdown.fuelVariableUnitCost(),
                breakdown.deliveryCommissionPercent(),
                commissionUnitCost,
                operationalVariableUnitCost,
                totalVariableUnitCost,
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

    private DirectVariableBreakdown buildDirectVariableBreakdown(Product product, ProductCostDetail costDetail) {
        BigDecimal baseIndustrialUnitCost = ZERO.setScale(4, RoundingMode.HALF_UP);
        BigDecimal fuelVariableUnitCost = ZERO.setScale(4, RoundingMode.HALF_UP);
        BigDecimal legacyCommissionUnitCost = ZERO.setScale(4, RoundingMode.HALF_UP);

        for (ProductCostLine line : costDetail.getLines()) {
            BigDecimal lineCost = normalizeScale(line.getTotalCost(), 4);
            String normalizedLabel = normalizeText(line.getSupplyName() + " " + line.getGroupLabel());

            if (isFuelLine(normalizedLabel)) {
                fuelVariableUnitCost = fuelVariableUnitCost.add(lineCost).setScale(4, RoundingMode.HALF_UP);
                continue;
            }

            if (isDeliveryCommissionLine(normalizedLabel)) {
                legacyCommissionUnitCost = legacyCommissionUnitCost.add(lineCost).setScale(4, RoundingMode.HALF_UP);
                continue;
            }

            baseIndustrialUnitCost = baseIndustrialUnitCost.add(lineCost).setScale(4, RoundingMode.HALF_UP);
        }

        BigDecimal deliveryCommissionPercent = ZERO.setScale(2, RoundingMode.HALF_UP);
        if (product.usesClientProfilePrice()) {
            deliveryCommissionPercent = DEFAULT_DELIVERY_COMMISSION_PERCENT.setScale(2, RoundingMode.HALF_UP);
        } else {
            baseIndustrialUnitCost = baseIndustrialUnitCost.add(legacyCommissionUnitCost).setScale(4, RoundingMode.HALF_UP);
        }

        return new DirectVariableBreakdown(
                baseIndustrialUnitCost,
                fuelVariableUnitCost,
                deliveryCommissionPercent
        );
    }

    private boolean isFuelLine(String normalizedLabel) {
        return normalizedLabel.contains("gasolina")
                || normalizedLabel.contains("combustible")
                || normalizedLabel.contains("fuel");
    }

    private boolean isDeliveryCommissionLine(String normalizedLabel) {
        boolean hasCommissionToken = normalizedLabel.contains("comision") || normalizedLabel.contains("commission");
        boolean hasDeliveryToken = normalizedLabel.contains("repartidor") || normalizedLabel.contains("delivery");
        return hasCommissionToken && hasDeliveryToken;
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

    private BigDecimal calculateAveragePrice(BigDecimal revenue, BigDecimal units) {
        if (revenue == null || units == null || units.compareTo(ZERO) <= 0) {
            return ZERO.setScale(2, RoundingMode.HALF_UP);
        }

        return revenue.divide(units, 2, RoundingMode.HALF_UP);
    }

    private BigDecimal normalizeScale(BigDecimal value, int scale) {
        if (value == null) {
            return ZERO.setScale(scale, RoundingMode.HALF_UP);
        }
        return value.setScale(scale, RoundingMode.HALF_UP);
    }

    private record DirectVariableBreakdown(
            BigDecimal baseIndustrialUnitCost,
            BigDecimal fuelVariableUnitCost,
            BigDecimal deliveryCommissionPercent
    ) {
    }
}
