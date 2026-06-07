package com.ecoamazonas.eco_agua.product.cost;

import com.ecoamazonas.eco_agua.product.Product;

import java.math.BigDecimal;
import java.util.List;

public class ProductPriceSimulatorSnapshot {

    private final List<Product> products;
    private final Long selectedProductId;
    private final BigDecimal simulatedPrice;
    private final BigDecimal estimatedQuantity;
    private final ProductPriceSimulatorResult result;

    public ProductPriceSimulatorSnapshot(
            List<Product> products,
            Long selectedProductId,
            BigDecimal simulatedPrice,
            BigDecimal estimatedQuantity,
            ProductPriceSimulatorResult result
    ) {
        this.products = products;
        this.selectedProductId = selectedProductId;
        this.simulatedPrice = simulatedPrice;
        this.estimatedQuantity = estimatedQuantity;
        this.result = result;
    }

    public List<Product> getProducts() {
        return products;
    }

    public Long getSelectedProductId() {
        return selectedProductId;
    }

    public BigDecimal getSimulatedPrice() {
        return simulatedPrice;
    }

    public BigDecimal getEstimatedQuantity() {
        return estimatedQuantity;
    }

    public ProductPriceSimulatorResult getResult() {
        return result;
    }

    public boolean isProductSelected() {
        return result != null;
    }
}
