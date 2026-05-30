package com.ecoamazonas.eco_agua.pricing;

import com.ecoamazonas.eco_agua.client.Client;
import com.ecoamazonas.eco_agua.client.ClientProfile;
import com.ecoamazonas.eco_agua.product.Product;
import com.ecoamazonas.eco_agua.promotion.Promotion;
import com.ecoamazonas.eco_agua.promotion.PromotionProduct;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
public class PriceSimulationService {

    public PriceSimulationResult simulate(
            Product product,
            Client client,
            Promotion promotion,
            PromotionProduct promotionProduct
    ) {
        PriceSimulationResult result = new PriceSimulationResult();
        result.setProduct(product);
        result.setClient(client);
        result.setPromotion(promotion);
        result.setPromotionProduct(promotionProduct);

        BigDecimal cost = safe(product.getTotalCostFromSupplies());
        BigDecimal basePrice = safe(product.getPrice());

        // Base values
        result.setTotalCost(cost);
        result.setBasePrice(basePrice);

        BigDecimal baseMargin = basePrice.subtract(cost);
        result.setBaseMarginAmount(baseMargin);
        result.setBaseMarginPercent(calcMarginPercent(basePrice, cost));

        // Profile suggested price
        BigDecimal suggestedPrice = null;
        if (client != null && client.getProfile() != null) {
            ClientProfile profile = client.getProfile();
            suggestedPrice = profile.getSuggestedPrice();
        }
        if (suggestedPrice != null) {
            BigDecimal clientMargin = suggestedPrice.subtract(cost);
            result.setClientSuggestedPrice(suggestedPrice);
            result.setClientMarginAmount(clientMargin);
            result.setClientMarginPercent(calcMarginPercent(suggestedPrice, cost));
        }

        // Promotion price: interpret amount as total for "quantity" units
        if (promotionProduct != null
                && promotionProduct.getAmount() != null
                && promotionProduct.getQuantity() != null
                && promotionProduct.getQuantity() > 0) {

            BigDecimal qty = BigDecimal.valueOf(promotionProduct.getQuantity());
            BigDecimal promoUnitPrice = promotionProduct.getAmount()
                    .divide(qty, 2, RoundingMode.HALF_UP);

            BigDecimal promoMargin = promoUnitPrice.subtract(cost);

            result.setPromoUnitPrice(promoUnitPrice);
            result.setPromoMarginAmount(promoMargin);
            result.setPromoMarginPercent(calcMarginPercent(promoUnitPrice, cost));
        }

        return result;
    }

    private BigDecimal safe(BigDecimal value) {
        return value != null ? value : BigDecimal.ZERO;
    }

    private BigDecimal calcMarginPercent(BigDecimal price, BigDecimal cost) {
        if (price == null || BigDecimal.ZERO.compareTo(price) == 0) {
            return null;
        }
        if (cost == null) {
            cost = BigDecimal.ZERO;
        }
        BigDecimal margin = price.subtract(cost);
        return margin.multiply(BigDecimal.valueOf(100))
                .divide(price, 1, RoundingMode.HALF_UP);
    }
}
