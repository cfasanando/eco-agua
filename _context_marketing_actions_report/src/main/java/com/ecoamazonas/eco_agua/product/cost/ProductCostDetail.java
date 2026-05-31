package com.ecoamazonas.eco_agua.product.cost;

import com.ecoamazonas.eco_agua.product.Product;

import java.math.BigDecimal;
import java.util.List;

public class ProductCostDetail {

    private final Product product;
    private final List<ProductCostLine> lines;
    private final BigDecimal cvu;
    private final BigDecimal marginPercent;
    private final BigDecimal suggestedPrice;

    public ProductCostDetail(
            Product product,
            List<ProductCostLine> lines,
            BigDecimal cvu,
            BigDecimal marginPercent,
            BigDecimal suggestedPrice
    ) {
        this.product = product;
        this.lines = lines;
        this.cvu = cvu;
        this.marginPercent = marginPercent;
        this.suggestedPrice = suggestedPrice;
    }

    public Product getProduct() {
        return product;
    }

    public List<ProductCostLine> getLines() {
        return lines;
    }

    public BigDecimal getCvu() {
        return cvu;
    }

    public BigDecimal getMarginPercent() {
        return marginPercent;
    }

    public BigDecimal getSuggestedPrice() {
        return suggestedPrice;
    }
}
