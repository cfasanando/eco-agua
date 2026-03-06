package com.ecoamazonas.eco_agua.cashflow;

import com.ecoamazonas.eco_agua.category.CategoryType;
import com.ecoamazonas.eco_agua.expense.ExpenseRepository;
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

@Service
public class BreakEvenService {

    private static final BigDecimal ZERO = BigDecimal.ZERO;
    private static final BigDecimal ONE_HUNDRED = BigDecimal.valueOf(100);

    private final ExpenseRepository expenseRepository;
    private final ProductRepository productRepository;
    private final ProductCostService productCostService;
    private final SaleOrderItemRepository saleOrderItemRepository;

    public BreakEvenService(
            ExpenseRepository expenseRepository,
            ProductRepository productRepository,
            ProductCostService productCostService,
            SaleOrderItemRepository saleOrderItemRepository
    ) {
        this.expenseRepository = expenseRepository;
        this.productRepository = productRepository;
        this.productCostService = productCostService;
        this.saleOrderItemRepository = saleOrderItemRepository;
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

        // 1) Fixed costs (same logic as fixed-costs page: categories of type EXPENSES)
        BigDecimal fixedCosts = expenseRepository
                .sumAmountByCategoryTypeAndPeriod(CategoryType.EXPENSES, start, end);
        if (fixedCosts == null) {
            fixedCosts = ZERO;
        }

        // 2) Variable unit cost (CVU) from product cost detail
        ProductCostDetail costDetail = productCostService.calculateCostDetail(productId);
        BigDecimal variableUnitCost = costDetail.getCvu() != null ? costDetail.getCvu() : ZERO;

        // 3) Selling price
        BigDecimal sellingPrice = product.getPrice() != null ? product.getPrice() : ZERO;

        // 4) Contribution margin per unit
        BigDecimal contributionMargin = sellingPrice.subtract(variableUnitCost);
        if (contributionMargin.compareTo(ZERO) <= 0) {
            // No margin -> cannot calculate break-even in a meaningful way
            contributionMargin = ZERO;
        }

        // 5) Break-even units (exact and rounded)
        BigDecimal breakEvenExact = ZERO;
        BigDecimal breakEvenRounded = ZERO;
        if (contributionMargin.compareTo(ZERO) > 0) {
            // Exact units with 4 decimals
            breakEvenExact = fixedCosts
                    .divide(contributionMargin, 4, RoundingMode.HALF_UP);

            // Minimum integer units to cover costs (ceil)
            breakEvenRounded = breakEvenExact.setScale(0, RoundingMode.CEILING);
        }

        // 6) Real units sold in period (confirmed orders)
        BigDecimal unitsSold = saleOrderItemRepository
                .sumQuantitySoldByProductAndPeriod(productId, start, end);
        if (unitsSold == null) {
            unitsSold = ZERO;
        }

        // 7) Status vs break-even
        BreakEvenStatus status = BreakEvenStatus.BEFORE_BREAK_EVEN;
        if (breakEvenRounded.compareTo(ZERO) > 0) {
            int cmp = unitsSold.compareTo(breakEvenRounded);
            if (cmp < 0) {
                status = BreakEvenStatus.BEFORE_BREAK_EVEN;
            } else if (cmp == 0) {
                status = BreakEvenStatus.AT_BREAK_EVEN;
            } else {
                status = BreakEvenStatus.AFTER_BREAK_EVEN;
            }
        }

        // 8) Safety margin (units and %)
        BigDecimal safetyUnits = unitsSold.subtract(breakEvenRounded);
        BigDecimal safetyPercent = ZERO;
        if (breakEvenRounded.compareTo(ZERO) > 0) {
            safetyPercent = safetyUnits
                    .divide(breakEvenRounded, 4, RoundingMode.HALF_UP)
                    .multiply(ONE_HUNDRED)
                    .setScale(2, RoundingMode.HALF_UP);
        }

        return new BreakEvenResult(
                product,
                start,
                end,
                fixedCosts.setScale(2, RoundingMode.HALF_UP),
                variableUnitCost.setScale(4, RoundingMode.HALF_UP),
                sellingPrice.setScale(2, RoundingMode.HALF_UP),
                contributionMargin.setScale(4, RoundingMode.HALF_UP),
                breakEvenExact,
                breakEvenRounded,
                unitsSold,
                status,
                safetyUnits,
                safetyPercent
        );
    }
}
