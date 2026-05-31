package com.ecoamazonas.eco_agua.marketing;

import com.ecoamazonas.eco_agua.product.Product;
import com.ecoamazonas.eco_agua.product.ProductRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
public class MarketingContentIdeaService {

    private final MarketingContentIdeaRepository ideaRepository;
    private final ProductRepository productRepository;
    private final MarketingCampaignCalendarRepository campaignRepository;
    private final MarketingStrategyRepository strategyRepository;

    public MarketingContentIdeaService(MarketingContentIdeaRepository ideaRepository,
                                       ProductRepository productRepository,
                                       MarketingCampaignCalendarRepository campaignRepository,
                                       MarketingStrategyRepository strategyRepository) {
        this.ideaRepository = ideaRepository;
        this.productRepository = productRepository;
        this.campaignRepository = campaignRepository;
        this.strategyRepository = strategyRepository;
    }

    @Transactional(readOnly = true)
    public List<MarketingContentIdea> findAll() {
        return ideaRepository.findAllForAdmin();
    }

    @Transactional(readOnly = true)
    public MarketingContentIdea findForm(Long id) {
        if (id == null) {
            MarketingContentIdea idea = new MarketingContentIdea();
            idea.setChannel(MarketingContentIdea.Channel.TIKTOK_REELS);
            idea.setContentType(MarketingContentIdea.ContentType.SHORT_VIDEO);
            idea.setStatus(MarketingContentIdea.Status.NEW);
            idea.setPriority(MarketingContentIdea.Priority.MEDIUM);
            idea.setSuggestedDate(LocalDate.now().plusDays(1));
            idea.setCallToAction("Escríbenos por WhatsApp.");
            idea.setNextAction("Validar stock y preparar contenido.");
            return idea;
        }
        return ideaRepository.findByIdForAdmin(id).orElseGet(MarketingContentIdea::new);
    }

    @Transactional(readOnly = true)
    public List<Product> findActiveProducts() {
        return productRepository.findByActiveTrueOrderByNameAsc();
    }

    @Transactional(readOnly = true)
    public List<MarketingCampaignCalendarItem> findCampaigns() {
        return campaignRepository.findAllForAdmin();
    }

    @Transactional(readOnly = true)
    public List<MarketingStrategy> findStrategies() {
        return strategyRepository.findAllForAdmin();
    }

    @Transactional
    public MarketingContentIdea save(MarketingContentIdea idea,
                                     Long productId,
                                     Long campaignId,
                                     Long strategyId) {
        MarketingContentIdea target = idea;
        if (idea.getId() != null) {
            target = ideaRepository.findById(idea.getId()).orElse(idea);
            copyEditableFields(idea, target);
        }

        normalize(target);
        target.setProduct(resolveProduct(productId));
        target.setCampaign(resolveCampaign(campaignId));
        target.setStrategy(resolveStrategy(strategyId));
        return ideaRepository.save(target);
    }

    @Transactional
    public void select(Long id) {
        updateStatus(id, MarketingContentIdea.Status.SELECTED);
    }

    @Transactional
    public void prepare(Long id) {
        updateStatus(id, MarketingContentIdea.Status.PREPARING);
    }

    @Transactional
    public void publish(Long id) {
        updateStatus(id, MarketingContentIdea.Status.PUBLISHED);
    }

    @Transactional
    public void discard(Long id) {
        updateStatus(id, MarketingContentIdea.Status.DISCARDED);
    }

    private void updateStatus(Long id, MarketingContentIdea.Status status) {
        ideaRepository.findById(id).ifPresent(idea -> {
            idea.setStatus(status);
            ideaRepository.save(idea);
        });
    }


    private void copyEditableFields(MarketingContentIdea source, MarketingContentIdea target) {
        target.setTitle(source.getTitle());
        target.setChannel(source.getChannel());
        target.setContentType(source.getContentType());
        target.setStatus(source.getStatus());
        target.setPriority(source.getPriority());
        target.setSuggestedDate(source.getSuggestedDate());
        target.setTargetSegment(source.getTargetSegment());
        target.setHook(source.getHook());
        target.setMainMessage(source.getMainMessage());
        target.setCallToAction(source.getCallToAction());
        target.setNextAction(source.getNextAction());
        target.setObservations(source.getObservations());
    }

    private Product resolveProduct(Long productId) {
        if (productId == null) {
            return null;
        }
        return productRepository.findById(productId).orElse(null);
    }

    private MarketingCampaignCalendarItem resolveCampaign(Long campaignId) {
        if (campaignId == null) {
            return null;
        }
        return campaignRepository.findById(campaignId).orElse(null);
    }

    private MarketingStrategy resolveStrategy(Long strategyId) {
        if (strategyId == null) {
            return null;
        }
        return strategyRepository.findById(strategyId).orElse(null);
    }

    private void normalize(MarketingContentIdea idea) {
        if (idea.getChannel() == null) {
            idea.setChannel(MarketingContentIdea.Channel.TIKTOK_REELS);
        }
        if (idea.getContentType() == null) {
            idea.setContentType(MarketingContentIdea.ContentType.SHORT_VIDEO);
        }
        if (idea.getStatus() == null) {
            idea.setStatus(MarketingContentIdea.Status.NEW);
        }
        if (idea.getPriority() == null) {
            idea.setPriority(MarketingContentIdea.Priority.MEDIUM);
        }
        if (isBlank(idea.getTitle())) {
            idea.setTitle("Idea de contenido");
        }
        if (isBlank(idea.getCallToAction())) {
            idea.setCallToAction("Escríbenos por WhatsApp.");
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
