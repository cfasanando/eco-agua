package com.ecoamazonas.eco_agua.pricing;

import com.ecoamazonas.eco_agua.client.Client;
import com.ecoamazonas.eco_agua.product.Product;
import com.ecoamazonas.eco_agua.promotion.Promotion;
import com.ecoamazonas.eco_agua.promotion.PromotionProduct;

import java.math.BigDecimal;

public class PriceSimulationResult {

    private Product product;
    private Client client;
    private Promotion promotion;
    private PromotionProduct promotionProduct;

    private BigDecimal basePrice;
    private BigDecimal totalCost;
    private BigDecimal baseMarginAmount;
    private BigDecimal baseMarginPercent;

    private BigDecimal clientSuggestedPrice;
    private BigDecimal clientMarginAmount;
    private BigDecimal clientMarginPercent;

    private BigDecimal promoUnitPrice;
    private BigDecimal promoMarginAmount;
    private BigDecimal promoMarginPercent;

    // Getters & setters

    public Product getProduct() {
        return product;
    }

    public void setProduct(Product product) {
        this.product = product;
    }

    public Client getClient() {
        return client;
    }

    public void setClient(Client client) {
        this.client = client;
    }

    public Promotion getPromotion() {
        return promotion;
    }

    public void setPromotion(Promotion promotion) {
        this.promotion = promotion;
    }

    public PromotionProduct getPromotionProduct() {
        return promotionProduct;
    }

    public void setPromotionProduct(PromotionProduct promotionProduct) {
        this.promotionProduct = promotionProduct;
    }

    public BigDecimal getBasePrice() {
        return basePrice;
    }

    public void setBasePrice(BigDecimal basePrice) {
        this.basePrice = basePrice;
    }

    public BigDecimal getTotalCost() {
        return totalCost;
    }

    public void setTotalCost(BigDecimal totalCost) {
        this.totalCost = totalCost;
    }

    public BigDecimal getBaseMarginAmount() {
        return baseMarginAmount;
    }

    public void setBaseMarginAmount(BigDecimal baseMarginAmount) {
        this.baseMarginAmount = baseMarginAmount;
    }

    public BigDecimal getBaseMarginPercent() {
        return baseMarginPercent;
    }

    public void setBaseMarginPercent(BigDecimal baseMarginPercent) {
        this.baseMarginPercent = baseMarginPercent;
    }

    public BigDecimal getClientSuggestedPrice() {
        return clientSuggestedPrice;
    }

    public void setClientSuggestedPrice(BigDecimal clientSuggestedPrice) {
        this.clientSuggestedPrice = clientSuggestedPrice;
    }

    public BigDecimal getClientMarginAmount() {
        return clientMarginAmount;
    }

    public void setClientMarginAmount(BigDecimal clientMarginAmount) {
        this.clientMarginAmount = clientMarginAmount;
    }

    public BigDecimal getClientMarginPercent() {
        return clientMarginPercent;
    }

    public void setClientMarginPercent(BigDecimal clientMarginPercent) {
        this.clientMarginPercent = clientMarginPercent;
    }

    public BigDecimal getPromoUnitPrice() {
        return promoUnitPrice;
    }

    public void setPromoUnitPrice(BigDecimal promoUnitPrice) {
        this.promoUnitPrice = promoUnitPrice;
    }

    public BigDecimal getPromoMarginAmount() {
        return promoMarginAmount;
    }

    public void setPromoMarginAmount(BigDecimal promoMarginAmount) {
        this.promoMarginAmount = promoMarginAmount;
    }

    public BigDecimal getPromoMarginPercent() {
        return promoMarginPercent;
    }

    public void setPromoMarginPercent(BigDecimal promoMarginPercent) {
        this.promoMarginPercent = promoMarginPercent;
    }
}
