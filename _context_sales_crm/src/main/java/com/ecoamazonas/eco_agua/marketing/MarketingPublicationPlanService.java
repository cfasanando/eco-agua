package com.ecoamazonas.eco_agua.marketing;

import com.ecoamazonas.eco_agua.product.Product;
import com.ecoamazonas.eco_agua.product.ProductRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class MarketingPublicationPlanService {

    private final MarketingPublicationPlanRepository publicationPlanRepository;
    private final MarketingContentIdeaRepository ideaRepository;
    private final MarketingCampaignCalendarRepository campaignRepository;
    private final MarketingStrategyRepository strategyRepository;
    private final ProductRepository productRepository;

    public MarketingPublicationPlanService(MarketingPublicationPlanRepository publicationPlanRepository,
                                           MarketingContentIdeaRepository ideaRepository,
                                           MarketingCampaignCalendarRepository campaignRepository,
                                           MarketingStrategyRepository strategyRepository,
                                           ProductRepository productRepository) {
        this.publicationPlanRepository = publicationPlanRepository;
        this.ideaRepository = ideaRepository;
        this.campaignRepository = campaignRepository;
        this.strategyRepository = strategyRepository;
        this.productRepository = productRepository;
    }

    @Transactional(readOnly = true)
    public List<MarketingPublicationPlanRow> findRows() {
        return publicationPlanRepository.findAllForAdmin().stream()
                .map(MarketingPublicationPlanRow::from)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<MarketingPublicationPlanItem> findAll() {
        return publicationPlanRepository.findAllForAdmin();
    }

    @Transactional(readOnly = true)
    public MarketingPublicationPlanItem findForm(Long id) {
        if (id == null) {
            MarketingPublicationPlanItem item = new MarketingPublicationPlanItem();
            item.setPublicationDate(LocalDate.now().plusDays(1));
            item.setChannel(MarketingPublicationPlanItem.Channel.TIKTOK_REELS);
            item.setPublicationType(MarketingPublicationPlanItem.PublicationType.SHORT_VIDEO);
            item.setStatus(MarketingPublicationPlanItem.Status.PENDING);
            item.setResponsible("Marketing");
            item.setBaseText("Consulta disponibilidad por WhatsApp antes de publicar precios o stock.");
            return item;
        }
        return publicationPlanRepository.findByIdForAdmin(id).orElseGet(MarketingPublicationPlanItem::new);
    }

    @Transactional(readOnly = true)
    public List<MarketingContentIdea> findPlanningIdeas() {
        return ideaRepository.findAllForAdmin().stream()
                .filter(idea -> idea.getStatus() == null || idea.getStatus() != MarketingContentIdea.Status.DISCARDED)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<MarketingCampaignCalendarItem> findCampaigns() {
        return campaignRepository.findAllForAdmin();
    }

    @Transactional(readOnly = true)
    public List<MarketingStrategy> findStrategies() {
        return strategyRepository.findAllForAdmin();
    }

    @Transactional(readOnly = true)
    public List<Product> findActiveProducts() {
        return productRepository.findByActiveTrueOrderByNameAsc();
    }

    @Transactional
    public MarketingPublicationPlanItem save(MarketingPublicationPlanItem form,
                                             Long ideaId,
                                             Long campaignId,
                                             Long strategyId,
                                             Long productId) {
        MarketingPublicationPlanItem target = form;
        if (form.getId() != null) {
            target = publicationPlanRepository.findById(form.getId()).orElse(form);
            copyEditableFields(form, target);
        }

        normalize(target);
        target.setIdea(resolveIdea(ideaId));
        target.setCampaign(resolveCampaign(campaignId));
        target.setStrategy(resolveStrategy(strategyId));
        target.setProduct(resolveProduct(productId));
        return publicationPlanRepository.save(target);
    }

    @Transactional
    public void prepare(Long id) {
        updateStatus(id, MarketingPublicationPlanItem.Status.PREPARING);
    }

    @Transactional
    public void markReady(Long id) {
        updateStatus(id, MarketingPublicationPlanItem.Status.READY);
    }

    @Transactional
    public void publish(Long id) {
        updateStatus(id, MarketingPublicationPlanItem.Status.PUBLISHED);
    }

    @Transactional
    public void cancel(Long id) {
        updateStatus(id, MarketingPublicationPlanItem.Status.CANCELLED);
    }

    private void updateStatus(Long id, MarketingPublicationPlanItem.Status status) {
        publicationPlanRepository.findById(id).ifPresent(item -> {
            item.setStatus(status);
            publicationPlanRepository.save(item);
        });
    }

    private void copyEditableFields(MarketingPublicationPlanItem source, MarketingPublicationPlanItem target) {
        target.setPublicationDate(source.getPublicationDate());
        target.setChannel(source.getChannel());
        target.setPublicationType(source.getPublicationType());
        target.setStatus(source.getStatus());
        target.setTitle(source.getTitle());
        target.setBaseText(source.getBaseText());
        target.setResponsible(source.getResponsible());
        target.setResultNote(source.getResultNote());
        target.setObservations(source.getObservations());
    }

    private MarketingContentIdea resolveIdea(Long ideaId) {
        if (ideaId == null) {
            return null;
        }
        return ideaRepository.findById(ideaId).orElse(null);
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

    private Product resolveProduct(Long productId) {
        if (productId == null) {
            return null;
        }
        return productRepository.findById(productId).orElse(null);
    }

    private void normalize(MarketingPublicationPlanItem item) {
        if (item.getChannel() == null) {
            item.setChannel(MarketingPublicationPlanItem.Channel.TIKTOK_REELS);
        }
        if (item.getPublicationType() == null) {
            item.setPublicationType(MarketingPublicationPlanItem.PublicationType.SHORT_VIDEO);
        }
        if (item.getStatus() == null) {
            item.setStatus(MarketingPublicationPlanItem.Status.PENDING);
        }
        if (item.getPublicationDate() == null) {
            item.setPublicationDate(LocalDate.now().plusDays(1));
        }
        if (isBlank(item.getTitle())) {
            item.setTitle("Publicación de marketing");
        }
        if (isBlank(item.getResponsible())) {
            item.setResponsible("Marketing");
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
