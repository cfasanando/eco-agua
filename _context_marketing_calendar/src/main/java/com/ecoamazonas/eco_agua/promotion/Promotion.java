package com.ecoamazonas.eco_agua.promotion;

import com.ecoamazonas.eco_agua.client.Client;
import jakarta.persistence.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Entity
@Table(name = "promotion")
public class Promotion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 250)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @Column(name = "promo_number")
    private Integer promoNumber;

    @Column(nullable = false)
    private boolean enabled = true;

    @Column(name = "max_counter")
    private Integer maxCounter;

    @Column(name = "color_border", length = 45)
    private String colorBorder;

    @Column(name = "banner_image_path", length = 255)
    private String bannerImagePath;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "promotion", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PromotionProduct> promotionProducts = new ArrayList<>();

    @ManyToMany(mappedBy = "promotions")
    private Set<Client> clients = new HashSet<>();

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        if (this.createdAt == null) {
            this.createdAt = now;
        }
        this.updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
        this.updatedAt = LocalDateTime.now();
    }

    // Getters & setters

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public Integer getPromoNumber() {
        return promoNumber;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public Integer getMaxCounter() {
        return maxCounter;
    }

    public String getColorBorder() {
        return colorBorder;
    }

    public String getBannerImagePath() {
        return bannerImagePath;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public List<PromotionProduct> getPromotionProducts() {
        return promotionProducts;
    }

    public Set<Client> getClients() {
        return clients;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }

    public void setEndDate(LocalDate endDate) {
        this.endDate = endDate;
    }

    public void setPromoNumber(Integer promoNumber) {
        this.promoNumber = promoNumber;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public void setMaxCounter(Integer maxCounter) {
        this.maxCounter = maxCounter;
    }

    public void setColorBorder(String colorBorder) {
        this.colorBorder = colorBorder;
    }

    public void setBannerImagePath(String bannerImagePath) {
        this.bannerImagePath = bannerImagePath;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public void setPromotionProducts(List<PromotionProduct> promotionProducts) {
        this.promotionProducts = promotionProducts;
    }

    public void setClients(Set<Client> clients) {
        this.clients = clients;
    }
}
