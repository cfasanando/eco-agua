package com.ecoamazonas.eco_agua.marketing;

import com.ecoamazonas.eco_agua.product.Product;
import com.ecoamazonas.eco_agua.promotion.Promotion;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "marketing_strategy")
public class MarketingStrategy {

    public enum Status {
        DRAFT,
        ACTIVE,
        ARCHIVED
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String title;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Status status = Status.DRAFT;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id")
    private Product product;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "promotion_id")
    private Promotion promotion;

    @Column(name = "campaign_month", length = 20)
    private String campaignMonth;

    @Column(length = 255)
    private String objective;

    @Column(name = "product_value", columnDefinition = "TEXT")
    private String productValue;

    @Column(name = "product_differentiator", columnDefinition = "TEXT")
    private String productDifferentiator;

    @Column(name = "price_positioning", columnDefinition = "TEXT")
    private String pricePositioning;

    @Column(name = "wholesale_note", columnDefinition = "TEXT")
    private String wholesaleNote;

    @Column(name = "place_channel", columnDefinition = "TEXT")
    private String placeChannel;

    @Column(name = "delivery_area", columnDefinition = "TEXT")
    private String deliveryArea;

    @Column(name = "promotion_message", columnDefinition = "TEXT")
    private String promotionMessage;

    @Column(name = "content_channel", length = 120)
    private String contentChannel;

    @Column(name = "audience_segment", columnDefinition = "TEXT")
    private String audienceSegment;

    @Column(name = "customer_need", columnDefinition = "TEXT")
    private String customerNeed;

    @Column(name = "customer_objection", columnDefinition = "TEXT")
    private String customerObjection;

    @Column(name = "call_to_action", length = 255)
    private String callToAction;

    @Column(name = "next_action", length = 255)
    private String nextAction;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        if (createdAt == null) {
            createdAt = now;
        }
        updatedAt = now;
        if (status == null) {
            status = Status.DRAFT;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
        if (status == null) {
            status = Status.DRAFT;
        }
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public Product getProduct() {
        return product;
    }

    public void setProduct(Product product) {
        this.product = product;
    }

    public Promotion getPromotion() {
        return promotion;
    }

    public void setPromotion(Promotion promotion) {
        this.promotion = promotion;
    }

    public String getCampaignMonth() {
        return campaignMonth;
    }

    public void setCampaignMonth(String campaignMonth) {
        this.campaignMonth = campaignMonth;
    }

    public String getObjective() {
        return objective;
    }

    public void setObjective(String objective) {
        this.objective = objective;
    }

    public String getProductValue() {
        return productValue;
    }

    public void setProductValue(String productValue) {
        this.productValue = productValue;
    }

    public String getProductDifferentiator() {
        return productDifferentiator;
    }

    public void setProductDifferentiator(String productDifferentiator) {
        this.productDifferentiator = productDifferentiator;
    }

    public String getPricePositioning() {
        return pricePositioning;
    }

    public void setPricePositioning(String pricePositioning) {
        this.pricePositioning = pricePositioning;
    }

    public String getWholesaleNote() {
        return wholesaleNote;
    }

    public void setWholesaleNote(String wholesaleNote) {
        this.wholesaleNote = wholesaleNote;
    }

    public String getPlaceChannel() {
        return placeChannel;
    }

    public void setPlaceChannel(String placeChannel) {
        this.placeChannel = placeChannel;
    }

    public String getDeliveryArea() {
        return deliveryArea;
    }

    public void setDeliveryArea(String deliveryArea) {
        this.deliveryArea = deliveryArea;
    }

    public String getPromotionMessage() {
        return promotionMessage;
    }

    public void setPromotionMessage(String promotionMessage) {
        this.promotionMessage = promotionMessage;
    }

    public String getContentChannel() {
        return contentChannel;
    }

    public void setContentChannel(String contentChannel) {
        this.contentChannel = contentChannel;
    }

    public String getAudienceSegment() {
        return audienceSegment;
    }

    public void setAudienceSegment(String audienceSegment) {
        this.audienceSegment = audienceSegment;
    }

    public String getCustomerNeed() {
        return customerNeed;
    }

    public void setCustomerNeed(String customerNeed) {
        this.customerNeed = customerNeed;
    }

    public String getCustomerObjection() {
        return customerObjection;
    }

    public void setCustomerObjection(String customerObjection) {
        this.customerObjection = customerObjection;
    }

    public String getCallToAction() {
        return callToAction;
    }

    public void setCallToAction(String callToAction) {
        this.callToAction = callToAction;
    }

    public String getNextAction() {
        return nextAction;
    }

    public void setNextAction(String nextAction) {
        this.nextAction = nextAction;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
