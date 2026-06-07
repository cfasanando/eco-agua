package com.ecoamazonas.eco_agua.product.cost;

import com.ecoamazonas.eco_agua.product.Product;
import com.ecoamazonas.eco_agua.product.ProductRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Service
public class ProductPriceSimulatorService {

    private static final BigDecimal ZERO = BigDecimal.ZERO;
    private static final BigDecimal ONE = BigDecimal.ONE;
    private static final BigDecimal ONE_HUNDRED = BigDecimal.valueOf(100);

    private final ProductRepository productRepository;
    private final ProductCostService productCostService;

    public ProductPriceSimulatorService(
            ProductRepository productRepository,
            ProductCostService productCostService
    ) {
        this.productRepository = productRepository;
        this.productCostService = productCostService;
    }

    @Transactional(readOnly = true)
    public ProductPriceSimulatorSnapshot buildSnapshot(
            Long productId,
            BigDecimal simulatedPrice,
            BigDecimal estimatedQuantity
    ) {
        List<Product> products = productRepository.findByActiveTrueOrderByNameAsc();
        if (productId == null) {
            return new ProductPriceSimulatorSnapshot(products, null, money(simulatedPrice), quantity(estimatedQuantity), null);
        }

        Product product = productRepository.findById(productId)
                .orElse(null);
        if (product == null) {
            return new ProductPriceSimulatorSnapshot(products, null, money(simulatedPrice), quantity(estimatedQuantity), null);
        }

        ProductCostDetail costDetail = productCostService.calculateCostDetail(product.getId());
        BigDecimal currentPrice = money(product.getPrice());
        BigDecimal unitCost = money(costDetail.getCvu());
        BigDecimal resolvedSimulatedPrice = money(simulatedPrice);
        if (resolvedSimulatedPrice.compareTo(ZERO) <= 0) {
            resolvedSimulatedPrice = currentPrice;
        }

        BigDecimal resolvedEstimatedQuantity = quantity(estimatedQuantity);
        if (resolvedEstimatedQuantity.compareTo(ZERO) <= 0) {
            resolvedEstimatedQuantity = ONE.setScale(2, RoundingMode.HALF_UP);
        }

        boolean hasRecipeCost = costDetail.getLines() != null
                && !costDetail.getLines().isEmpty()
                && unitCost.compareTo(ZERO) > 0;

        BigDecimal currentUnitMargin = money(currentPrice.subtract(unitCost));
        BigDecimal simulatedUnitMargin = money(resolvedSimulatedPrice.subtract(unitCost));
        BigDecimal currentMarginPercent = marginPercent(currentPrice, unitCost);
        BigDecimal simulatedMarginPercent = marginPercent(resolvedSimulatedPrice, unitCost);
        BigDecimal currentRevenue = money(currentPrice.multiply(resolvedEstimatedQuantity));
        BigDecimal simulatedRevenue = money(resolvedSimulatedPrice.multiply(resolvedEstimatedQuantity));
        BigDecimal priceDifference = money(resolvedSimulatedPrice.subtract(currentPrice));
        BigDecimal totalVariableCost = money(unitCost.multiply(resolvedEstimatedQuantity));
        BigDecimal currentGrossProfit = money(currentRevenue.subtract(totalVariableCost));
        BigDecimal simulatedGrossProfit = money(simulatedRevenue.subtract(totalVariableCost));
        BigDecimal revenueDifference = money(simulatedRevenue.subtract(currentRevenue));
        BigDecimal profitDifference = money(simulatedGrossProfit.subtract(currentGrossProfit));

        ProductPriceSimulatorResult result = new ProductPriceSimulatorResult(
                product.getId(),
                product.getName(),
                currentPrice,
                unitCost,
                resolvedSimulatedPrice,
                resolvedEstimatedQuantity,
                currentUnitMargin,
                currentMarginPercent,
                simulatedUnitMargin,
                simulatedMarginPercent,
                currentRevenue,
                simulatedRevenue,
                priceDifference,
                totalVariableCost,
                currentGrossProfit,
                simulatedGrossProfit,
                revenueDifference,
                profitDifference,
                unitCost,
                hasRecipeCost,
                resolvedSimulatedPrice.compareTo(unitCost) < 0,
                currentPrice.compareTo(unitCost) < 0
        );

        return new ProductPriceSimulatorSnapshot(
                products,
                product.getId(),
                resolvedSimulatedPrice,
                resolvedEstimatedQuantity,
                result
        );
    }

    private BigDecimal marginPercent(BigDecimal price, BigDecimal unitCost) {
        if (price == null || price.compareTo(ZERO) <= 0) {
            return null;
        }
        return price.subtract(safe(unitCost))
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
