package com.ecoamazonas.eco_agua.marketing;

import com.ecoamazonas.eco_agua.product.Product;
import com.ecoamazonas.eco_agua.product.ProductRepository;
import com.ecoamazonas.eco_agua.promotion.Promotion;
import com.ecoamazonas.eco_agua.promotion.PromotionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
public class MarketingCampaignCalendarService {

    private final MarketingCampaignCalendarRepository campaignRepository;
    private final ProductRepository productRepository;
    private final PromotionRepository promotionRepository;
    private final MarketingStrategyRepository strategyRepository;

    public MarketingCampaignCalendarService(MarketingCampaignCalendarRepository campaignRepository,
                                            ProductRepository productRepository,
                                            PromotionRepository promotionRepository,
                                            MarketingStrategyRepository strategyRepository) {
        this.campaignRepository = campaignRepository;
        this.productRepository = productRepository;
        this.promotionRepository = promotionRepository;
        this.strategyRepository = strategyRepository;
    }

    @Transactional(readOnly = true)
    public List<MarketingCampaignCalendarItem> findAll() {
        return campaignRepository.findAllForAdmin();
    }

    @Transactional(readOnly = true)
    public MarketingCampaignCalendarItem findForm(Long id) {
        if (id == null) {
            MarketingCampaignCalendarItem campaign = new MarketingCampaignCalendarItem();
            campaign.setStatus(MarketingCampaignCalendarItem.Status.PLANNED);
            campaign.setType(MarketingCampaignCalendarItem.CampaignType.SEASONAL);
            campaign.setChannel("WhatsApp / Facebook / TikTok / Portal público");
            campaign.setStartDate(LocalDate.now());
            campaign.setEndDate(LocalDate.now().plusDays(7));
            return campaign;
        }
        return campaignRepository.findByIdForAdmin(id).orElseGet(MarketingCampaignCalendarItem::new);
    }

    @Transactional(readOnly = true)
    public List<Product> findActiveProducts() {
        return productRepository.findByActiveTrueOrderByNameAsc();
    }

    @Transactional(readOnly = true)
    public List<Promotion> findEnabledPromotions() {
        return promotionRepository.findByEnabledTrueOrderByNameAsc();
    }

    @Transactional(readOnly = true)
    public List<MarketingStrategy> findStrategies() {
        return strategyRepository.findAllForAdmin();
    }

    @Transactional
    public MarketingCampaignCalendarItem save(MarketingCampaignCalendarItem campaign,
                                               Long productId,
                                               Long promotionId,
                                               Long strategyId) {
        normalize(campaign);
        campaign.setProduct(resolveProduct(productId));
        campaign.setPromotion(resolvePromotion(promotionId));
        campaign.setStrategy(resolveStrategy(strategyId));
        return campaignRepository.save(campaign);
    }

    @Transactional
    public void activate(Long id) {
        campaignRepository.findById(id).ifPresent(campaign -> {
            campaign.setStatus(MarketingCampaignCalendarItem.Status.ACTIVE);
            campaignRepository.save(campaign);
        });
    }

    @Transactional
    public void finish(Long id) {
        campaignRepository.findById(id).ifPresent(campaign -> {
            campaign.setStatus(MarketingCampaignCalendarItem.Status.FINISHED);
            campaignRepository.save(campaign);
        });
    }

    @Transactional
    public void archive(Long id) {
        campaignRepository.findById(id).ifPresent(campaign -> {
            campaign.setStatus(MarketingCampaignCalendarItem.Status.ARCHIVED);
            campaignRepository.save(campaign);
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

    private MarketingStrategy resolveStrategy(Long strategyId) {
        if (strategyId == null) {
            return null;
        }
        return strategyRepository.findById(strategyId).orElse(null);
    }

    private void normalize(MarketingCampaignCalendarItem campaign) {
        if (campaign.getStatus() == null) {
            campaign.setStatus(MarketingCampaignCalendarItem.Status.PLANNED);
        }
        if (campaign.getType() == null) {
            campaign.setType(MarketingCampaignCalendarItem.CampaignType.SEASONAL);
        }
        if (isBlank(campaign.getName())) {
            campaign.setName("Campaña de marketing");
        }
        if (isBlank(campaign.getChannel())) {
            campaign.setChannel("WhatsApp / Facebook / TikTok / Portal público");
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
