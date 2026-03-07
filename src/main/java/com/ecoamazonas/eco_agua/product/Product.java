package com.ecoamazonas.eco_agua.product;

import com.ecoamazonas.eco_agua.category.Category;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "product")
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "image_path", length = 255)
    private String imagePath;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private Category category;

    @Column(precision = 10, scale = 2, nullable = false)
    private BigDecimal price;

    @Column(nullable = false)
    private boolean active = true;

    @Column(name = "featured", nullable = false)
    private boolean featured = false;

    @Column(precision = 10, scale = 2, nullable = false)
    private BigDecimal stock = BigDecimal.ZERO;

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ProductSupply> suppliesComposition = new ArrayList<>();

    public boolean isFeatured() {
        return featured;
    }

    public void setFeatured(boolean featured) {
        this.featured = featured;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public String getImagePath() {
        return imagePath;
    }

    public Category getCategory() {
        return category;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public boolean isActive() {
        return active;
    }

    public List<ProductSupply> getSuppliesComposition() {
        return suppliesComposition;
    }

    public BigDecimal getStock() {
        return stock;
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

    public void setImagePath(String imagePath) {
        this.imagePath = imagePath;
    }

    public void setCategory(Category category) {
        this.category = category;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public void setSuppliesComposition(List<ProductSupply> suppliesComposition) {
        this.suppliesComposition = suppliesComposition;
    }

    public void setStock(BigDecimal stock) {
        this.stock = stock;
    }

    @Transient
    public boolean usesClientProfilePrice() {
        if (category == null || category.getName() == null) {
            return false;
        }

        if (!"Agua de mesa".equalsIgnoreCase(category.getName().trim())) {
            return false;
        }

        return suppliesComposition != null && !suppliesComposition.isEmpty();
    }

    /**
     * Total cost of supplies for one unit of this product.
     */
    @Transient
    public BigDecimal getSuppliesCost() {
        if (suppliesComposition == null || suppliesComposition.isEmpty()) {
            return BigDecimal.ZERO;
        }

        BigDecimal total = BigDecimal.ZERO;
        for (ProductSupply ps : suppliesComposition) {
            if (ps == null) {
                continue;
            }
            BigDecimal contribution = ps.getCostContribution();
            if (contribution != null) {
                total = total.add(contribution);
            }
        }
        return total;
    }

    /**
     * Convenience alias for the total cost based on supplies.
     */
    @Transient
    public BigDecimal getTotalCostFromSupplies() {
        return getSuppliesCost();
    }

    /**
     * Margin amount = product price - supplies cost.
     * Returns null if price is not set.
     */
    @Transient
    public BigDecimal getMarginAmountFromSupplies() {
        if (price == null) {
            return null;
        }
        BigDecimal suppliesCost = getSuppliesCost();
        if (suppliesCost == null) {
            suppliesCost = BigDecimal.ZERO;
        }
        return price.subtract(suppliesCost);
    }

    /**
     * Margin percent vs supplies cost (based on product price).
     * Returns null if price is null or zero.
     */
    @Transient
    public BigDecimal getMarginPercentFromSupplies() {
        if (price == null || price.compareTo(BigDecimal.ZERO) == 0) {
            return null;
        }
        BigDecimal margin = getMarginAmountFromSupplies();
        if (margin == null) {
            return null;
        }

        return margin
                .multiply(BigDecimal.valueOf(100))
                .divide(price, 1, RoundingMode.HALF_UP);
    }
}