package com.ecoamazonas.eco_agua.promotion;

import com.ecoamazonas.eco_agua.product.Product;
import com.ecoamazonas.eco_agua.product.ProductRepository;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
public class PromotionService {

    private final PromotionRepository promotionRepository;
    private final PromotionProductRepository promotionProductRepository;
    private final ProductRepository productRepository;

    public PromotionService(
            PromotionRepository promotionRepository,
            PromotionProductRepository promotionProductRepository,
            ProductRepository productRepository
    ) {
        this.promotionRepository = promotionRepository;
        this.promotionProductRepository = promotionProductRepository;
        this.productRepository = productRepository;
    }

    @Transactional(readOnly = true)
    public List<Promotion> findAll() {
        return promotionRepository.findAll(Sort.by("name").ascending());
    }

    @Transactional(readOnly = true)
    public List<Promotion> findAllActive() {
        return promotionRepository.findByEnabledTrueOrderByNameAsc();
    }

    @Transactional(readOnly = true)
    public Promotion findById(Long id) {
        return promotionRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Promotion not found with id " + id));
    }

    @Transactional
    public void saveFromForm(
            Long id,
            String name,
            String description,
            LocalDate startDate,
            LocalDate endDate,
            Integer promoNumber,
            String colorBorder,
            boolean enabled,
            Integer maxCounter
    ) {
        Promotion promotion;

        if (id == null) {
            promotion = new Promotion();
        } else {
            promotion = findById(id);
        }

        promotion.setName(name);
        promotion.setDescription(description);
        promotion.setStartDate(startDate);
        promotion.setEndDate(endDate);
        promotion.setPromoNumber(promoNumber);
        promotion.setColorBorder(colorBorder);
        promotion.setEnabled(enabled);
        promotion.setMaxCounter(maxCounter);

        promotionRepository.save(promotion);
    }

    @Transactional
    public void delete(Long id) {
        promotionRepository.deleteById(id);
    }

    @Transactional
    public void saveProductsConfig(
            Long promotionId,
            List<Long> productIds,
            List<Integer> quantities,
            List<BigDecimal> amounts
    ) {
        Promotion promotion = findById(promotionId);

        // Remove current items (orphanRemoval will delete rows)
        promotion.getPromotionProducts().clear();

        if (productIds == null || productIds.isEmpty()) {
            return;
        }

        List<PromotionProduct> newItems = new ArrayList<>();

        for (int i = 0; i < productIds.size(); i++) {
            Long productId = productIds.get(i);
            if (productId == null) {
                continue;
            }

            Integer qty = (quantities != null && quantities.size() > i) ? quantities.get(i) : 0;
            BigDecimal amount = (amounts != null && amounts.size() > i) ? amounts.get(i) : BigDecimal.ZERO;

            Product product = productRepository.findById(productId)
                    .orElseThrow(() -> new IllegalArgumentException("Product not found with id " + productId));

            PromotionProduct pp = new PromotionProduct();
            pp.setPromotion(promotion);
            pp.setProduct(product);
            pp.setQuantity(qty != null ? qty : 0);
            pp.setAmount(amount != null ? amount : BigDecimal.ZERO);

            newItems.add(pp);
        }

        // Persist new items through the promotion entity
        promotion.getPromotionProducts().addAll(newItems);
        promotionRepository.save(promotion);
    }

    @Transactional(readOnly = true)
    public List<Promotion> findApplicablePromotions(Long clientId, LocalDate date) {
        if (date == null) {
            date = LocalDate.now();
        }
        return promotionRepository.findApplicableForClientAndDate(clientId, date);
    }

    /**
     * Returns applicable promotions for the given client and date as DTOs.
     * This is used by the order form to load promotions via JSON.
     */
    @Transactional(readOnly = true)
    public List<ClientPromotionDTO> findApplicablePromotionDtos(Long clientId, LocalDate date) {
        if (date == null) {
            date = LocalDate.now();
        }

        List<Promotion> promotions = findApplicablePromotions(clientId, date);
        List<ClientPromotionDTO> result = new ArrayList<>();

        for (Promotion promotion : promotions) {
            ClientPromotionDTO dto = new ClientPromotionDTO();
            dto.setId(promotion.getId());
            dto.setName(promotion.getName());
            dto.setDescription(promotion.getDescription());

            List<ClientPromotionItemDTO> items = new ArrayList<>();
            if (promotion.getPromotionProducts() != null) {
                for (PromotionProduct pp : promotion.getPromotionProducts()) {
                    if (pp == null || pp.getProduct() == null) {
                        continue;
                    }
                    Product product = pp.getProduct();

                    ClientPromotionItemDTO itemDto = new ClientPromotionItemDTO();
                    itemDto.setProductId(product.getId());
                    itemDto.setProductName(product.getName());

                    Integer qty = pp.getQuantity();
                    itemDto.setQuantity(qty != null ? BigDecimal.valueOf(qty) : BigDecimal.ZERO);

                    BigDecimal amount = pp.getAmount();
                    itemDto.setUnitPrice(amount != null ? amount : BigDecimal.ZERO);

                    items.add(itemDto);
                }
            }

            dto.setItems(items);
            result.add(dto);
        }

        return result;
    }

    // ---------- DTO classes used as JSON payload ----------

    public static class ClientPromotionDTO {
        private Long id;
        private String name;
        private String description;
        private List<ClientPromotionItemDTO> items;

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        public List<ClientPromotionItemDTO> getItems() {
            return items;
        }

        public void setItems(List<ClientPromotionItemDTO> items) {
            this.items = items;
        }
    }

    public static class ClientPromotionItemDTO {
        private Long productId;
        private String productName;
        private BigDecimal quantity;
        private BigDecimal unitPrice;

        public Long getProductId() {
            return productId;
        }

        public void setProductId(Long productId) {
            this.productId = productId;
        }

        public String getProductName() {
            return productName;
        }

        public void setProductName(String productName) {
            this.productName = productName;
        }

        public BigDecimal getQuantity() {
            return quantity;
        }

        public void setQuantity(BigDecimal quantity) {
            this.quantity = quantity;
        }

        public BigDecimal getUnitPrice() {
            return unitPrice;
        }

        public void setUnitPrice(BigDecimal unitPrice) {
            this.unitPrice = unitPrice;
        }
    }
}
