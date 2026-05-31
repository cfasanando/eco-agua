package com.ecoamazonas.eco_agua.marketing;

import com.ecoamazonas.eco_agua.product.Product;
import com.ecoamazonas.eco_agua.product.ProductRepository;
import com.ecoamazonas.eco_agua.promotion.Promotion;
import com.ecoamazonas.eco_agua.promotion.PromotionRepository;
import com.ecoamazonas.eco_agua.promotion.PromotionService;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class MarketingSeasonalPromotionService {

    private final PromotionRepository promotionRepository;
    private final PromotionService promotionService;
    private final ProductRepository productRepository;

    public MarketingSeasonalPromotionService(PromotionRepository promotionRepository,
                                             PromotionService promotionService,
                                             ProductRepository productRepository) {
        this.promotionRepository = promotionRepository;
        this.promotionService = promotionService;
        this.productRepository = productRepository;
    }

    @Transactional(readOnly = true)
    public List<MarketingSeasonalPromotionRow> findRows() {
        LocalDate today = LocalDate.now();
        return promotionRepository.findAll(Sort.by(Sort.Direction.DESC, "startDate"))
                .stream()
                .sorted(Comparator
                        .comparing((Promotion promotion) -> promotion.getStartDate() != null ? promotion.getStartDate() : LocalDate.MIN)
                        .reversed()
                        .thenComparing(Promotion::getName, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)))
                .map(promotion -> MarketingSeasonalPromotionRow.from(promotion, today))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Promotion findForm(Long id) {
        if (id == null) {
            Promotion promotion = new Promotion();
            promotion.setEnabled(true);
            promotion.setStartDate(LocalDate.now());
            promotion.setEndDate(LocalDate.now().plusDays(14));
            promotion.setColorBorder("#166534");
            promotion.setDescription("Consulta disponibilidad, precio actualizado y coordinación por WhatsApp.");
            return promotion;
        }
        return promotionRepository.findById(id).orElseGet(Promotion::new);
    }

    @Transactional(readOnly = true)
    public List<Product> findActiveProducts() {
        return productRepository.findByActiveTrueOrderByNameAsc();
    }

    @Transactional
    public void save(Long id,
                     String name,
                     String description,
                     LocalDate startDate,
                     LocalDate endDate,
                     Integer promoNumber,
                     String colorBorder,
                     Boolean enabledParam,
                     Integer maxCounter) {
        promotionService.saveFromForm(
                id,
                normalizeText(name, "Promoción de temporada"),
                normalizeText(description, "Consulta disponibilidad, precio actualizado y coordinación por WhatsApp."),
                startDate,
                endDate,
                promoNumber,
                normalizeText(colorBorder, "#166534"),
                Boolean.TRUE.equals(enabledParam),
                maxCounter
        );
    }

    @Transactional
    public void configureProducts(Long promotionId,
                                  List<Long> productIds,
                                  List<Integer> quantities,
                                  List<BigDecimal> amounts) {
        promotionService.saveProductsConfig(promotionId, productIds, quantities, amounts);
    }

    @Transactional
    public void activate(Long id) {
        promotionRepository.findById(id).ifPresent(promotion -> {
            promotion.setEnabled(true);
            if (promotion.getStartDate() == null) {
                promotion.setStartDate(LocalDate.now());
            }
            promotionRepository.save(promotion);
        });
    }

    @Transactional
    public void pause(Long id) {
        promotionRepository.findById(id).ifPresent(promotion -> {
            promotion.setEnabled(false);
            promotionRepository.save(promotion);
        });
    }

    @Transactional
    public void finish(Long id) {
        promotionRepository.findById(id).ifPresent(promotion -> {
            promotion.setEnabled(false);
            promotion.setEndDate(LocalDate.now());
            promotionRepository.save(promotion);
        });
    }

    private String normalizeText(String value, String fallback) {
        if (value == null || value.trim().isEmpty()) {
            return fallback;
        }
        return value.trim();
    }
}
