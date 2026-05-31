package com.ecoamazonas.eco_agua.marketing;

import com.ecoamazonas.eco_agua.product.Product;
import com.ecoamazonas.eco_agua.product.ProductRepository;
import com.ecoamazonas.eco_agua.promotion.Promotion;
import com.ecoamazonas.eco_agua.promotion.PromotionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class MarketingStrategyService {

    private final MarketingStrategyRepository marketingStrategyRepository;
    private final ProductRepository productRepository;
    private final PromotionRepository promotionRepository;

    public MarketingStrategyService(MarketingStrategyRepository marketingStrategyRepository,
                                    ProductRepository productRepository,
                                    PromotionRepository promotionRepository) {
        this.marketingStrategyRepository = marketingStrategyRepository;
        this.productRepository = productRepository;
        this.promotionRepository = promotionRepository;
    }

    @Transactional(readOnly = true)
    public List<MarketingStrategy> findAll() {
        return marketingStrategyRepository.findAllForAdmin();
    }

    @Transactional(readOnly = true)
    public MarketingStrategy findForm(Long id) {
        if (id == null) {
            MarketingStrategy strategy = new MarketingStrategy();
            strategy.setStatus(MarketingStrategy.Status.DRAFT);
            strategy.setContentChannel("WhatsApp / Facebook / TikTok / Public portal");
            strategy.setCallToAction("Ask for availability on WhatsApp");
            return strategy;
        }
        return marketingStrategyRepository.findById(id).orElseGet(MarketingStrategy::new);
    }

    @Transactional(readOnly = true)
    public List<Product> findActiveProducts() {
        return productRepository.findByActiveTrueOrderByNameAsc();
    }

    @Transactional(readOnly = true)
    public List<Promotion> findEnabledPromotions() {
        return promotionRepository.findByEnabledTrueOrderByNameAsc();
    }

    @Transactional
    public MarketingStrategy save(MarketingStrategy strategy, Long productId, Long promotionId) {
        normalize(strategy);
        strategy.setProduct(resolveProduct(productId));
        strategy.setPromotion(resolvePromotion(promotionId));
        return marketingStrategyRepository.save(strategy);
    }

    @Transactional
    public void archive(Long id) {
        marketingStrategyRepository.findById(id).ifPresent(strategy -> {
            strategy.setStatus(MarketingStrategy.Status.ARCHIVED);
            marketingStrategyRepository.save(strategy);
        });
    }

    @Transactional
    public void activate(Long id) {
        marketingStrategyRepository.findById(id).ifPresent(strategy -> {
            strategy.setStatus(MarketingStrategy.Status.ACTIVE);
            marketingStrategyRepository.save(strategy);
        });
    }

    private Product resolveProduct(Long productId) {
        if (productId == null) {
            return null;
        }
        return productRepository.findById(productId).orElse(null);
    }

    private Promotion resolvePromotion(Long promotionId) {
        if (promotionId == null) {
            return null;
        }
        return promotionRepository.findById(promotionId).orElse(null);
    }

    private void normalize(MarketingStrategy strategy) {
        if (strategy.getStatus() == null) {
            strategy.setStatus(MarketingStrategy.Status.DRAFT);
        }
        if (isBlank(strategy.getTitle())) {
            strategy.setTitle("Estrategia de marketing 5P");
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
