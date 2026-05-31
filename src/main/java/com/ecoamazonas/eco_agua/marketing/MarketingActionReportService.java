package com.ecoamazonas.eco_agua.marketing;

import com.ecoamazonas.eco_agua.product.Product;
import com.ecoamazonas.eco_agua.product.ProductRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Collectors;

@Service
public class MarketingActionReportService {

    private final MarketingActionReportRepository actionReportRepository;
    private final MarketingCampaignCalendarRepository campaignRepository;
    private final MarketingPublicationPlanRepository publicationPlanRepository;
    private final MarketingContentIdeaRepository ideaRepository;
    private final ProductRepository productRepository;

    public MarketingActionReportService(MarketingActionReportRepository actionReportRepository,
                                        MarketingCampaignCalendarRepository campaignRepository,
                                        MarketingPublicationPlanRepository publicationPlanRepository,
                                        MarketingContentIdeaRepository ideaRepository,
                                        ProductRepository productRepository) {
        this.actionReportRepository = actionReportRepository;
        this.campaignRepository = campaignRepository;
        this.publicationPlanRepository = publicationPlanRepository;
        this.ideaRepository = ideaRepository;
        this.productRepository = productRepository;
    }

    @Transactional(readOnly = true)
    public List<MarketingActionReportRow> findRows() {
        return actionReportRepository.findAllForAdmin().stream()
                .map(MarketingActionReportRow::from)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public MarketingActionReportItem findForm(Long id) {
        if (id == null) {
            MarketingActionReportItem item = new MarketingActionReportItem();
            item.setActionDate(LocalDate.now());
            item.setActionType(MarketingActionReportItem.ActionType.SOCIAL_POST);
            item.setChannel(MarketingActionReportItem.Channel.TIKTOK_REELS);
            item.setStatus(MarketingActionReportItem.Status.REGISTERED);
            item.setResponsible("Marketing");
            item.setGeneratedInquiries(0);
            item.setEstimatedSales(BigDecimal.ZERO);
            item.setDescription("Registrar la acción realizada y validar stock antes de publicar precios.");
            return item;
        }
        return actionReportRepository.findByIdForAdmin(id).orElseGet(MarketingActionReportItem::new);
    }

    @Transactional(readOnly = true)
    public List<MarketingCampaignCalendarItem> findCampaigns() {
        return campaignRepository.findAllForAdmin();
    }

    @Transactional(readOnly = true)
    public List<MarketingPublicationPlanItem> findPublicationPlans() {
        return publicationPlanRepository.findAllForAdmin();
    }

    @Transactional(readOnly = true)
    public List<MarketingContentIdea> findIdeas() {
        return ideaRepository.findAllForAdmin().stream()
                .filter(idea -> idea.getStatus() == null || idea.getStatus() != MarketingContentIdea.Status.DISCARDED)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<Product> findActiveProducts() {
        return productRepository.findByActiveTrueOrderByNameAsc();
    }

    @Transactional
    public MarketingActionReportItem save(MarketingActionReportItem form,
                                          Long campaignId,
                                          Long publicationPlanId,
                                          Long ideaId,
                                          Long productId) {
        MarketingActionReportItem target = form;
        if (form.getId() != null) {
            target = actionReportRepository.findById(form.getId()).orElse(form);
            copyEditableFields(form, target);
        }

        normalize(target);
        target.setCampaign(resolveCampaign(campaignId));
        target.setPublicationPlan(resolvePublicationPlan(publicationPlanId));
        target.setIdea(resolveIdea(ideaId));
        target.setProduct(resolveProduct(productId));
        return actionReportRepository.save(target);
    }

    @Transactional
    public void followUp(Long id) {
        updateStatus(id, MarketingActionReportItem.Status.FOLLOW_UP);
    }

    @Transactional
    public void markWithResult(Long id) {
        updateStatus(id, MarketingActionReportItem.Status.WITH_RESULT);
    }

    @Transactional
    public void markWithoutResult(Long id) {
        updateStatus(id, MarketingActionReportItem.Status.WITHOUT_RESULT);
    }

    @Transactional
    public void archive(Long id) {
        updateStatus(id, MarketingActionReportItem.Status.ARCHIVED);
    }

    public Long selectedCampaignId(MarketingActionReportItem item) {
        return safeId(() -> item.getCampaign() != null ? item.getCampaign().getId() : null);
    }

    public Long selectedPublicationPlanId(MarketingActionReportItem item) {
        return safeId(() -> item.getPublicationPlan() != null ? item.getPublicationPlan().getId() : null);
    }

    public Long selectedIdeaId(MarketingActionReportItem item) {
        return safeId(() -> item.getIdea() != null ? item.getIdea().getId() : null);
    }

    public Long selectedProductId(MarketingActionReportItem item) {
        return safeId(() -> item.getProduct() != null ? item.getProduct().getId() : null);
    }

    private void updateStatus(Long id, MarketingActionReportItem.Status status) {
        actionReportRepository.findById(id).ifPresent(item -> {
            item.setStatus(status);
            actionReportRepository.save(item);
        });
    }

    private void copyEditableFields(MarketingActionReportItem source, MarketingActionReportItem target) {
        target.setActionDate(source.getActionDate());
        target.setActionType(source.getActionType());
        target.setChannel(source.getChannel());
        target.setStatus(source.getStatus());
        target.setDescription(source.getDescription());
        target.setObservedResult(source.getObservedResult());
        target.setGeneratedInquiries(source.getGeneratedInquiries());
        target.setEstimatedSales(source.getEstimatedSales());
        target.setResponsible(source.getResponsible());
        target.setObservations(source.getObservations());
    }

    private MarketingCampaignCalendarItem resolveCampaign(Long campaignId) {
        if (campaignId == null) {
            return null;
        }
        return campaignRepository.findById(campaignId).orElse(null);
    }

    private MarketingPublicationPlanItem resolvePublicationPlan(Long publicationPlanId) {
        if (publicationPlanId == null) {
            return null;
        }
        return publicationPlanRepository.findById(publicationPlanId).orElse(null);
    }

    private MarketingContentIdea resolveIdea(Long ideaId) {
        if (ideaId == null) {
            return null;
        }
        return ideaRepository.findById(ideaId).orElse(null);
    }

    private Product resolveProduct(Long productId) {
        if (productId == null) {
            return null;
        }
        return productRepository.findById(productId).orElse(null);
    }

    private void normalize(MarketingActionReportItem item) {
        if (item.getActionType() == null) {
            item.setActionType(MarketingActionReportItem.ActionType.SOCIAL_POST);
        }
        if (item.getChannel() == null) {
            item.setChannel(MarketingActionReportItem.Channel.TIKTOK_REELS);
        }
        if (item.getStatus() == null) {
            item.setStatus(MarketingActionReportItem.Status.REGISTERED);
        }
        if (item.getActionDate() == null) {
            item.setActionDate(LocalDate.now());
        }
        if (item.getGeneratedInquiries() == null || item.getGeneratedInquiries() < 0) {
            item.setGeneratedInquiries(0);
        }
        if (item.getEstimatedSales() == null || item.getEstimatedSales().compareTo(BigDecimal.ZERO) < 0) {
            item.setEstimatedSales(BigDecimal.ZERO);
        }
        if (isBlank(item.getDescription())) {
            item.setDescription("Acción de marketing registrada.");
        }
        if (isBlank(item.getResponsible())) {
            item.setResponsible("Marketing");
        }
    }

    private Long safeId(Supplier<Long> supplier) {
        try {
            return supplier.get();
        } catch (RuntimeException exception) {
            return null;
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
