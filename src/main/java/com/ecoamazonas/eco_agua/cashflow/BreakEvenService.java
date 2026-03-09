package com.ecoamazonas.eco_agua.cashflow;

import com.ecoamazonas.eco_agua.client.ClientProfile;
import com.ecoamazonas.eco_agua.client.ClientProfileRepository;
import com.ecoamazonas.eco_agua.expense.ExpenseRepository;
import com.ecoamazonas.eco_agua.order.SaleOrderItemRepository;
import com.ecoamazonas.eco_agua.product.Product;
import com.ecoamazonas.eco_agua.product.ProductRepository;
import com.ecoamazonas.eco_agua.product.cost.PeriodExpenseLine;
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
import java.util.stream.Collectors;

@Service
public class BreakEvenService {

    private static final BigDecimal ZERO = BigDecimal.ZERO;
    private static final BigDecimal ONE_HUNDRED = BigDecimal.valueOf(100);

    /**
     * Fixed-cost categories aligned with the current business model.
     * Direct variable lines already included inside product_supply (fuel per bottle,
     * delivery commission, direct treatment inputs, etc.) are intentionally excluded here
     * to avoid double counting in the break-even point.
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

    /**
     * Calculate break-even point for one product and one period.
     */
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
        if (fixedCosts == null) {
            fixedCosts = ZERO;
        }
        fixedCosts = fixedCosts.setScale(2, RoundingMode.HALF_UP);

        List<BreakEvenFixedCostLine> fixedCostLines = buildFixedCostLines(start, end);

        ProductCostDetail costDetail = productCostService.calculateCostDetail(productId);
        BigDecimal variableUnitCost = normalizeScale(costDetail.getCvu(), 4);

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

        if (usesProfilePricing && actualAverageSellingPrice.compareTo(ZERO) > 0) {
            scenarios.add(createScenario(
                    "Precio promedio real del período",
                    "Calculado con las ventas cerradas del rango.",
                    actualAverageSellingPrice,
                    actualAverageSellingPrice,
                    fixedCosts,
                    variableUnitCost,
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

            scenarios.add(createScenario(
                    profile.getName(),
                    "Precio sugerido por perfil de cliente.",
                    profilePrice,
                    realizedAverage,
                    fixedCosts,
                    variableUnitCost,
                    profileUnitsSold,
                    profileRevenue,
                    false
            ));
        }

        if (!usesProfilePricing || product.getPrice() != null) {
            BigDecimal productPrice = product.getPrice() != null ? product.getPrice() : ZERO;
            scenarios.add(createScenario(
                    usesProfilePricing ? "Precio actual del producto (legacy)" : "Precio actual del producto",
                    usesProfilePricing
                            ? "Se muestra solo como referencia porque el negocio ya vende por perfil."
                            : "Precio principal actual del producto.",
                    productPrice,
                    actualAverageSellingPrice,
                    fixedCosts,
                    variableUnitCost,
                    totalUnitsSold,
                    totalRevenueInPeriod,
                    !usesProfilePricing && scenarios.isEmpty()
            ));
        }

        final BigDecimal fixedCostsFinal = fixedCosts;
        BreakEvenScenario fallbackScenario = scenarios.isEmpty()
                ? createScenario(
                        "Sin escenario",
                        "No hay precios disponibles para este producto.",
                        ZERO,
                        ZERO,
                        fixedCostsFinal,
                        variableUnitCost,
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
                variableUnitCost,
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

        return new BreakEvenScenario(
                label,
                subtitle,
                safeSellingPrice,
                safeRealizedAveragePrice,
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

    private BigDecimal normalizeScale(BigDecimal value, int scale) {
        if (value == null) {
            return ZERO.setScale(scale, RoundingMode.HALF_UP);
        }
        return value.setScale(scale, RoundingMode.HALF_UP);
    }
}
