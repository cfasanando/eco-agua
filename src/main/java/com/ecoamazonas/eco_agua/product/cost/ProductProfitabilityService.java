package com.ecoamazonas.eco_agua.product.cost;

import com.ecoamazonas.eco_agua.order.SaleOrderItemRepository;
import com.ecoamazonas.eco_agua.product.Product;
import com.ecoamazonas.eco_agua.product.ProductRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
public class ProductProfitabilityService {

    private static final BigDecimal ZERO = BigDecimal.ZERO;
    private static final BigDecimal ONE_HUNDRED = BigDecimal.valueOf(100);
    private static final BigDecimal LOW_MARGIN_THRESHOLD = BigDecimal.valueOf(20);

    private final ProductRepository productRepository;
    private final ProductCostService productCostService;
    private final SaleOrderItemRepository saleOrderItemRepository;

    public ProductProfitabilityService(
            ProductRepository productRepository,
            ProductCostService productCostService,
            SaleOrderItemRepository saleOrderItemRepository
    ) {
        this.productRepository = productRepository;
        this.productCostService = productCostService;
        this.saleOrderItemRepository = saleOrderItemRepository;
    }

    @Transactional(readOnly = true)
    public ProductProfitabilitySnapshot buildSnapshot(LocalDate startDate, LocalDate endDate) {
        LocalDate resolvedStart = startDate;
        LocalDate resolvedEnd = endDate;

        if (resolvedStart == null || resolvedEnd == null) {
            LocalDate today = LocalDate.now();
            resolvedStart = today.withDayOfMonth(1);
            resolvedEnd = today.withDayOfMonth(today.lengthOfMonth());
        }

        if (resolvedStart.isAfter(resolvedEnd)) {
            LocalDate temp = resolvedStart;
            resolvedStart = resolvedEnd;
            resolvedEnd = temp;
        }

        List<ProductProfitabilityRow> rows = new ArrayList<>();
        for (Product product : productRepository.findByActiveTrueOrderByNameAsc()) {
            rows.add(buildRow(product, resolvedStart, resolvedEnd));
        }

        rows.sort(Comparator
                .comparing(ProductProfitabilityRow::getGrossProfit, Comparator.nullsLast(Comparator.reverseOrder()))
                .thenComparing(ProductProfitabilityRow::getRevenue, Comparator.nullsLast(Comparator.reverseOrder()))
                .thenComparing(ProductProfitabilityRow::getProductName, String.CASE_INSENSITIVE_ORDER));

        ProductProfitabilitySummary summary = buildSummary(rows);
        return new ProductProfitabilitySnapshot(resolvedStart, resolvedEnd, summary, rows);
    }

    private ProductProfitabilityRow buildRow(Product product, LocalDate startDate, LocalDate endDate) {
        BigDecimal salePrice = money(product.getPrice());
        ProductCostDetail costDetail = productCostService.calculateCostDetail(product.getId());
        BigDecimal unitCost = money(costDetail.getCvu());
        boolean hasRecipeCost = costDetail.getLines() != null && !costDetail.getLines().isEmpty() && unitCost.compareTo(ZERO) > 0;

        BigDecimal quantitySold = quantity(saleOrderItemRepository.sumQuantitySoldByProductAndPeriod(product.getId(), startDate, endDate));
        BigDecimal revenue = money(saleOrderItemRepository.sumRevenueSoldByProductAndPeriod(product.getId(), startDate, endDate));
        BigDecimal totalCost = money(unitCost.multiply(quantitySold));
        BigDecimal grossProfit = money(revenue.subtract(totalCost));
        BigDecimal unitMargin = money(salePrice.subtract(unitCost));
        BigDecimal marginPercent = marginPercent(salePrice, unitCost);

        boolean lowMargin = marginPercent != null && marginPercent.compareTo(LOW_MARGIN_THRESHOLD) < 0;
        boolean loss = grossProfit.compareTo(ZERO) < 0 || unitMargin.compareTo(ZERO) < 0;

        return new ProductProfitabilityRow(
                product.getId(),
                product.getName(),
                salePrice,
                unitCost,
                unitMargin,
                marginPercent,
                quantitySold,
                revenue,
                totalCost,
                grossProfit,
                hasRecipeCost,
                lowMargin,
                loss
        );
    }

    private ProductProfitabilitySummary buildSummary(List<ProductProfitabilityRow> rows) {
        BigDecimal totalRevenue = ZERO;
        BigDecimal totalCost = ZERO;
        BigDecimal totalGrossProfit = ZERO;
        ProductProfitabilityRow mostProfitableProduct = null;
        ProductProfitabilityRow topSellingProduct = null;
        ProductProfitabilityRow topSellingLowMarginProduct = null;

        for (ProductProfitabilityRow row : rows) {
            totalRevenue = totalRevenue.add(safe(row.getRevenue()));
            totalCost = totalCost.add(safe(row.getTotalCost()));
            totalGrossProfit = totalGrossProfit.add(safe(row.getGrossProfit()));

            if (row.getQuantitySold().compareTo(ZERO) > 0) {
                if (mostProfitableProduct == null || row.getGrossProfit().compareTo(mostProfitableProduct.getGrossProfit()) > 0) {
                    mostProfitableProduct = row;
                }
                if (topSellingProduct == null || row.getQuantitySold().compareTo(topSellingProduct.getQuantitySold()) > 0) {
                    topSellingProduct = row;
                }
                if (row.isLowMargin()) {
                    if (topSellingLowMarginProduct == null
                            || row.getQuantitySold().compareTo(topSellingLowMarginProduct.getQuantitySold()) > 0) {
                        topSellingLowMarginProduct = row;
                    }
                }
            }
        }

        BigDecimal averageMarginPercent = ZERO;
        if (totalRevenue.compareTo(ZERO) > 0) {
            averageMarginPercent = totalGrossProfit
                    .multiply(ONE_HUNDRED)
                    .divide(totalRevenue, 2, RoundingMode.HALF_UP);
        }

        return new ProductProfitabilitySummary(
                rows.size(),
                money(totalRevenue),
                money(totalCost),
                money(totalGrossProfit),
                averageMarginPercent,
                mostProfitableProduct,
                topSellingProduct,
                topSellingLowMarginProduct
        );
    }

    private BigDecimal marginPercent(BigDecimal price, BigDecimal unitCost) {
        if (price == null || price.compareTo(ZERO) <= 0) {
            return null;
        }
        BigDecimal margin = price.subtract(safe(unitCost));
        return margin
                .multiply(ONE_HUNDRED)
                .divide(price, 2, RoundingMode.HALF_UP);
    }

    private BigDecimal safe(BigDecimal value) {
        return value != null ? value : ZERO;
    }

    private BigDecimal money(BigDecimal value) {
        return safe(value).setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal quantity(BigDecimal value) {
        return safe(value).setScale(2, RoundingMode.HALF_UP);
    }
}
