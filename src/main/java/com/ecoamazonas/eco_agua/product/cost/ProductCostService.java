package com.ecoamazonas.eco_agua.product.cost;

import com.ecoamazonas.eco_agua.expense.ExpenseRepository;
import com.ecoamazonas.eco_agua.order.SaleOrderItemRepository;
import com.ecoamazonas.eco_agua.product.Product;
import com.ecoamazonas.eco_agua.product.ProductRepository;
import com.ecoamazonas.eco_agua.product.ProductSupply;
import com.ecoamazonas.eco_agua.product.ProductSupplyRepository;
import com.ecoamazonas.eco_agua.supply.Supply;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
public class ProductCostService {

    private static final BigDecimal DEFAULT_MARGIN_PERCENT = BigDecimal.valueOf(48); // you can change this
    private static final BigDecimal ZERO = BigDecimal.ZERO;

    // Categories 17–32 are plant, personnel, vehicle and cleaning expenses
    private static final List<Long> OVERHEAD_CATEGORY_IDS = List.of(
            17L, 18L, 19L, 20L, 21L, 22L, 23L, 24L,
            25L, 26L, 27L, 28L, 29L, 30L, 31L, 32L
    );

    private final ProductRepository productRepository;
    private final ProductSupplyRepository productSupplyRepository;
    private final ExpenseRepository expenseRepository;
    private final SaleOrderItemRepository saleOrderItemRepository;

    public ProductCostService(
            ProductRepository productRepository,
            ProductSupplyRepository productSupplyRepository,
            ExpenseRepository expenseRepository,
            SaleOrderItemRepository saleOrderItemRepository
    ) {
        this.productRepository = productRepository;
        this.productSupplyRepository = productSupplyRepository;
        this.expenseRepository = expenseRepository;
        this.saleOrderItemRepository = saleOrderItemRepository;
    }

    // ========= 1) Industrial CVU (solo insumos / servicios directos) =========

    @Transactional(readOnly = true)
    public ProductCostDetail calculateCostDetail(Long productId) {
        Product product = productRepository.findById(productId)
            .orElseThrow(() -> new IllegalArgumentException("Product not found: " + productId));

        List<ProductSupply> productSupplies =
            productSupplyRepository.findByProductIdOrderByIdAsc(productId);

        List<ProductCostLine> lines = new ArrayList<>();
        BigDecimal cvu = BigDecimal.ZERO;

        for (ProductSupply ps : productSupplies) {
            Supply supply = ps.getSupply();

            BigDecimal baseQuantity = supply.getBaseQuantity();
            BigDecimal baseCost = supply.getBaseCost();

            if (baseQuantity == null || baseQuantity.compareTo(BigDecimal.ZERO) <= 0) {
                continue; // avoid division by zero and invalid base quantities
            }

            // Cost per unit of supply (4 decimals is enough)
            BigDecimal unitCost = baseCost
                .divide(baseQuantity, 4, RoundingMode.HALF_UP);

            BigDecimal quantityUsed = ps.getQuantityUsed() != null
                ? ps.getQuantityUsed()
                : BigDecimal.ZERO;

            // Total cost for this line (quantityUsed * unitCost)
            BigDecimal totalCost = unitCost
                .multiply(quantityUsed)
                .setScale(2, RoundingMode.HALF_UP);

            cvu = cvu.add(totalCost);

            String groupLabel = supply.getGroupLabel();
            if (groupLabel == null || groupLabel.isBlank()) {
                groupLabel = "Otros";
            }

            lines.add(new ProductCostLine(
                groupLabel,
                supply.getName(),
                supply.getUnit(),
                quantityUsed,
                unitCost,
                totalCost
            ));
        }

        // Sort lines by group then by supply name so the table looks nicer
        lines.sort(Comparator
            .comparing(ProductCostLine::getGroupLabel, String.CASE_INSENSITIVE_ORDER)
            .thenComparing(ProductCostLine::getSupplyName, String.CASE_INSENSITIVE_ORDER));

        cvu = cvu.setScale(4, RoundingMode.HALF_UP);

        BigDecimal productPrice = product.getPrice();
        BigDecimal marginPercent;
        BigDecimal suggestedPrice;

        if (productPrice != null
            && productPrice.compareTo(BigDecimal.ZERO) > 0
            && cvu.compareTo(BigDecimal.ZERO) > 0) {

            // marginPercent = (price - cvu) / cvu * 100
            marginPercent = productPrice
                .subtract(cvu)
                .divide(cvu, 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100))
                .setScale(2, RoundingMode.HALF_UP);

            // Suggested price is exactly the current product price
            suggestedPrice = productPrice.setScale(2, RoundingMode.HALF_UP);
        } else {
            // Fallback: keep default behavior using DEFAULT_MARGIN_PERCENT
            marginPercent = DEFAULT_MARGIN_PERCENT;

            suggestedPrice = cvu
                .multiply(
                    BigDecimal.ONE.add(
                        marginPercent.divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP)
                    )
                )
                .setScale(2, RoundingMode.HALF_UP);
        }

        return new ProductCostDetail(product, lines, cvu, marginPercent, suggestedPrice);
    }

    // ========= 2) CVU total: industrial + gastos operativos del periodo =========

    /**
     * Calculate a full cost detail for a product:
     * - industrial CVU (direct supplies / services)
     * - allocated overheads per unit (plant, personnel, vehicle, cleaning)
     * using expenses and real units sold in the given period.
     */
    @Transactional(readOnly = true)
    public FullProductCostDetail calculateFullCostDetail(
            Long productId,
            LocalDate start,
            LocalDate end
    ) {
        Product product = productRepository.findById(productId)
            .orElseThrow(() -> new IllegalArgumentException("Product not found: " + productId));

        // Base industrial cost
        ProductCostDetail industrialDetail = calculateCostDetail(productId);
        BigDecimal industrialCvu = industrialDetail.getCvu(); // if ProductCostDetail is a record, change to industrialDetail.cvu()

        // Real delivered units in the period
        BigDecimal totalUnits = saleOrderItemRepository
            .sumQuantitySoldByProductAndPeriod(productId, start, end);
        if (totalUnits == null) {
            totalUnits = ZERO;
        }

        // Overhead expenses for plant, personnel, vehicle, etc.
        BigDecimal totalExpenses = expenseRepository
            .sumAmountByCategoryIdsAndPeriod(OVERHEAD_CATEGORY_IDS, start, end);
        if (totalExpenses == null) {
            totalExpenses = ZERO;
        }

        // Overhead cost per unit (if there are units)
        BigDecimal overheadUnitCost = ZERO;
        if (totalUnits.compareTo(ZERO) > 0) {
            overheadUnitCost = totalExpenses
                .divide(totalUnits, 4, RoundingMode.HALF_UP);
        }

        // Final full unit cost
        BigDecimal fullUnitCost = industrialCvu
            .add(overheadUnitCost)
            .setScale(4, RoundingMode.HALF_UP);

        // Detail by expense category
        List<PeriodExpenseLine> rawLines = expenseRepository
            .sumAmountByCategoryIdsGrouped(OVERHEAD_CATEGORY_IDS, start, end);

        List<PeriodExpenseLine> expenseLines = new ArrayList<>();
        if (totalUnits.compareTo(ZERO) > 0) {
            for (PeriodExpenseLine line : rawLines) {
                BigDecimal unitCost = line.getTotalAmount()
                    .divide(totalUnits, 4, RoundingMode.HALF_UP);
                expenseLines.add(new PeriodExpenseLine(
                    line.getCategoryName(),
                    line.getTotalAmount(),
                    unitCost
                ));
            }
        } else {
            // No units in period: unit cost is zero but we keep total amounts
            for (PeriodExpenseLine line : rawLines) {
                expenseLines.add(new PeriodExpenseLine(
                    line.getCategoryName(),
                    line.getTotalAmount(),
                    ZERO
                ));
            }
        }

        return new FullProductCostDetail(
            product,
            start,
            end,
            industrialDetail,
            totalUnits,
            totalExpenses,
            overheadUnitCost,
            fullUnitCost,
            expenseLines
        );
    }
}
