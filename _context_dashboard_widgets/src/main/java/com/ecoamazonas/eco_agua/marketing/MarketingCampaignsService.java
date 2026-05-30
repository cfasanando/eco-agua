package com.ecoamazonas.eco_agua.marketing;

import com.ecoamazonas.eco_agua.blog.BlogPost;
import com.ecoamazonas.eco_agua.blog.BlogPostRepository;
import com.ecoamazonas.eco_agua.promotion.Promotion;
import com.ecoamazonas.eco_agua.promotion.PromotionService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

@Service
public class MarketingCampaignsService {

    private final PromotionService promotionService;
    private final BlogPostRepository blogPostRepository;
    private final TestimonialRepository testimonialRepository;

    public MarketingCampaignsService(
            PromotionService promotionService,
            BlogPostRepository blogPostRepository,
            TestimonialRepository testimonialRepository
    ) {
        this.promotionService = promotionService;
        this.blogPostRepository = blogPostRepository;
        this.testimonialRepository = testimonialRepository;
    }

    @Transactional(readOnly = true)
    public MarketingCampaignsSnapshot buildSnapshot() {
        LocalDate today = LocalDate.now();

        List<Promotion> allPromotions = safePromotionList(promotionService.findAll());
        List<Promotion> activePromotions = allPromotions.stream()
                .filter(this::isPromotionActive)
                .sorted(Comparator
                        .comparing((Promotion promotion) -> promotion.getEndDate() != null ? promotion.getEndDate() : LocalDate.MAX)
                        .thenComparing(Promotion::getName, String.CASE_INSENSITIVE_ORDER))
                .collect(Collectors.toList());

        List<BlogPost> allPosts = safeBlogPostList(blogPostRepository.findAll());
        List<BlogPost> publishedPosts = allPosts.stream()
                .filter(post -> post.getStatus() == BlogPost.Status.PUBLISHED)
                .sorted(Comparator.comparing(BlogPost::getPublishedAt, Comparator.nullsLast(Comparator.naturalOrder())).reversed())
                .collect(Collectors.toList());
        List<BlogPost> draftPosts = allPosts.stream()
                .filter(post -> post.getStatus() == BlogPost.Status.DRAFT)
                .sorted(Comparator.comparing(BlogPost::getCreatedAt, Comparator.nullsLast(Comparator.naturalOrder())).reversed())
                .collect(Collectors.toList());

        List<Testimonial> allTestimonials = safeTestimonialList(testimonialRepository.findAllByOrderByDisplayOrderAscIdAsc());
        List<Testimonial> activeTestimonials = allTestimonials.stream()
                .filter(Testimonial::isActive)
                .collect(Collectors.toList());

        int promotionsEndingSoonCount = (int) activePromotions.stream()
                .filter(promotion -> promotion.getEndDate() != null)
                .filter(promotion -> !promotion.getEndDate().isBefore(today))
                .filter(promotion -> !promotion.getEndDate().isAfter(today.plusDays(7)))
                .count();

        return new MarketingCampaignsSnapshot(
                today,
                activePromotions.size(),
                publishedPosts.size(),
                draftPosts.size(),
                activeTestimonials.size(),
                promotionsEndingSoonCount,
                buildCampaignRows(activePromotions, publishedPosts, activeTestimonials, today),
                buildPromotionAssets(activePromotions, today),
                buildContentAssets(publishedPosts, draftPosts),
                buildTestimonialAssets(activeTestimonials, allTestimonials),
                buildPendingTasks(activePromotions, publishedPosts, draftPosts, activeTestimonials, today),
                buildSuggestedSegments(),
                buildCtaTemplates()
        );
    }

    private List<MarketingCampaignsSnapshot.CampaignRow> buildCampaignRows(
            List<Promotion> activePromotions,
            List<BlogPost> publishedPosts,
            List<Testimonial> activeTestimonials,
            LocalDate today
    ) {
        List<MarketingCampaignsSnapshot.CampaignRow> rows = new ArrayList<>();

        for (Promotion promotion : activePromotions.stream().limit(5).collect(Collectors.toList())) {
            rows.add(new MarketingCampaignsSnapshot.CampaignRow(
                    promotion.getName(),
                    "Promotion",
                    inferPromotionSegment(promotion),
                    "WhatsApp / Catalog / Home",
                    promotion.getStartDate(),
                    promotion.getEndDate(),
                    buildPromotionStatus(promotion, today),
                    "Drive orders to WhatsApp and catalog",
                    buildPromotionNextAction(promotion, today),
                    "/admin/promotions"
            ));
        }

        for (BlogPost post : publishedPosts.stream().limit(3).collect(Collectors.toList())) {
            rows.add(new MarketingCampaignsSnapshot.CampaignRow(
                    post.getTitle(),
                    "Content",
                    inferContentAudience(post),
                    "Blog / WhatsApp / Social",
                    post.getPublishedAt() != null ? post.getPublishedAt().toLocalDate() : null,
                    null,
                    "Published",
                    "Use post as awareness and trust asset",
                    "Share in WhatsApp and reuse in campaigns",
                    "/admin/blog"
            ));
        }

        if (!activeTestimonials.isEmpty()) {
            rows.add(new MarketingCampaignsSnapshot.CampaignRow(
                    "Social proof rotation",
                    "Testimonials",
                    "Cold leads / first purchase",
                    "Home / WhatsApp / Sales support",
                    null,
                    null,
                    "Ready",
                    "Use testimonials to reduce objections",
                    "Highlight one testimonial in the public home and WhatsApp replies",
                    "/marketing/admin/testimonials"
            ));
        }

        return rows.stream().limit(8).collect(Collectors.toList());
    }

    private List<MarketingCampaignsSnapshot.AssetRow> buildPromotionAssets(List<Promotion> promotions, LocalDate today) {
        List<MarketingCampaignsSnapshot.AssetRow> rows = new ArrayList<>();
        for (Promotion promotion : promotions.stream().limit(6).collect(Collectors.toList())) {
            rows.add(new MarketingCampaignsSnapshot.AssetRow(
                    promotion.getName(),
                    buildPromotionStatus(promotion, today),
                    firstNonBlank(promotion.getDescription(), "Promotion ready to be pushed to WhatsApp and the public home."),
                    "Open promotion",
                    "/admin/promotions",
                    promotion.getStartDate()
            ));
        }
        return rows;
    }

    private List<MarketingCampaignsSnapshot.AssetRow> buildContentAssets(List<BlogPost> publishedPosts, List<BlogPost> draftPosts) {
        List<MarketingCampaignsSnapshot.AssetRow> rows = new ArrayList<>();
        for (BlogPost post : publishedPosts.stream().limit(4).collect(Collectors.toList())) {
            rows.add(new MarketingCampaignsSnapshot.AssetRow(
                    post.getTitle(),
                    "Published",
                    firstNonBlank(post.getSummary(), "Published content ready to support campaigns."),
                    "Manage post",
                    "/admin/blog",
                    post.getPublishedAt() != null ? post.getPublishedAt().toLocalDate() : null
            ));
        }
        for (BlogPost post : draftPosts.stream().limit(2).collect(Collectors.toList())) {
            rows.add(new MarketingCampaignsSnapshot.AssetRow(
                    post.getTitle(),
                    "Draft",
                    firstNonBlank(post.getSummary(), "Draft content pending publication."),
                    "Review draft",
                    "/admin/blog",
                    post.getCreatedAt() != null ? post.getCreatedAt().toLocalDate() : null
            ));
        }
        return rows;
    }

    private List<MarketingCampaignsSnapshot.AssetRow> buildTestimonialAssets(List<Testimonial> activeTestimonials, List<Testimonial> allTestimonials) {
        List<MarketingCampaignsSnapshot.AssetRow> rows = new ArrayList<>();
        for (Testimonial testimonial : activeTestimonials.stream().limit(4).collect(Collectors.toList())) {
            rows.add(new MarketingCampaignsSnapshot.AssetRow(
                    firstNonBlank(testimonial.getAuthorName(), "Anonymous testimonial"),
                    "Active",
                    firstNonBlank(testimonial.getContent(), "Customer trust asset."),
                    "Manage testimonial",
                    "/marketing/admin/testimonials",
                    testimonial.getCreatedAt() != null ? testimonial.getCreatedAt().toLocalDate() : null
            ));
        }
        if (rows.isEmpty() && !allTestimonials.isEmpty()) {
            Testimonial testimonial = allTestimonials.get(0);
            rows.add(new MarketingCampaignsSnapshot.AssetRow(
                    firstNonBlank(testimonial.getAuthorName(), "Testimonial"),
                    "Inactive",
                    "There are testimonials, but none is active for the public home.",
                    "Activate testimonial",
                    "/marketing/admin/testimonials",
                    testimonial.getCreatedAt() != null ? testimonial.getCreatedAt().toLocalDate() : null
            ));
        }
        return rows;
    }

    private List<MarketingCampaignsSnapshot.TaskRow> buildPendingTasks(
            List<Promotion> activePromotions,
            List<BlogPost> publishedPosts,
            List<BlogPost> draftPosts,
            List<Testimonial> activeTestimonials,
            LocalDate today
    ) {
        List<MarketingCampaignsSnapshot.TaskRow> rows = new ArrayList<>();

        activePromotions.stream()
                .filter(promotion -> promotion.getEndDate() != null)
                .filter(promotion -> !promotion.getEndDate().isBefore(today))
                .filter(promotion -> !promotion.getEndDate().isAfter(today.plusDays(7)))
                .findFirst()
                .ifPresent(promotion -> rows.add(new MarketingCampaignsSnapshot.TaskRow(
                        "High",
                        "Promotion is ending soon",
                        "Review and reinforce the promotion '" + promotion.getName() + "' before it expires.",
                        "Open promotions",
                        "/admin/promotions"
                )));

        activePromotions.stream()
                .filter(promotion -> isBlank(promotion.getBannerImagePath()))
                .findFirst()
                .ifPresent(promotion -> rows.add(new MarketingCampaignsSnapshot.TaskRow(
                        "Medium",
                        "Promotion without banner",
                        "Add a banner image to '" + promotion.getName() + "' so it stands out in the public home and client channels.",
                        "Edit promotion",
                        "/admin/promotions"
                )));

        if (!draftPosts.isEmpty()) {
            rows.add(new MarketingCampaignsSnapshot.TaskRow(
                    "Medium",
                    "Draft content pending publication",
                    "You have " + draftPosts.size() + " draft post(s) ready to review and publish.",
                    "Open blog",
                    "/admin/blog"
            ));
        }

        if (publishedPosts.isEmpty()) {
            rows.add(new MarketingCampaignsSnapshot.TaskRow(
                    "High",
                    "No published content",
                    "Publish at least one blog post to support trust and awareness campaigns.",
                    "Create post",
                    "/admin/blog/new"
            ));
        }

        if (activeTestimonials.isEmpty()) {
            rows.add(new MarketingCampaignsSnapshot.TaskRow(
                    "Medium",
                    "No active testimonials",
                    "Activate testimonials so sales can use social proof in the public home and WhatsApp conversations.",
                    "Open testimonials",
                    "/marketing/admin/testimonials"
            ));
        }

        if (rows.isEmpty()) {
            rows.add(new MarketingCampaignsSnapshot.TaskRow(
                    "Normal",
                    "Marketing area looks healthy",
                    "Keep promotions, content and testimonials aligned with this week's sales focus.",
                    "Review campaigns",
                    "/marketing/admin/campaigns"
            ));
        }

        return rows;
    }

    private List<String> buildSuggestedSegments() {
        return List.of(
                "New clients - first purchase",
                "Clients due for reorder",
                "Dormant clients to reactivate",
                "Bodega / store segment",
                "Restaurant / bar segment",
                "Home delivery by zone"
        );
    }

    private List<MarketingCampaignsSnapshot.CtaTemplateRow> buildCtaTemplates() {
        return List.of(
                new MarketingCampaignsSnapshot.CtaTemplateRow(
                        "First purchase",
                        "New leads / home clients",
                        "Hello! We have a special first-order option for purified water delivery. Would you like prices and available schedules?"
                ),
                new MarketingCampaignsSnapshot.CtaTemplateRow(
                        "Reorder reminder",
                        "Frequent clients",
                        "Hello! It looks like your next water reorder may be due. Would you like us to schedule your delivery for today or tomorrow?"
                ),
                new MarketingCampaignsSnapshot.CtaTemplateRow(
                        "Cold client recovery",
                        "Dormant clients",
                        "Hello! We have a campaign for returning clients this week. Would you like to see the current refill and bottle options?"
                )
        );
    }

    private boolean isPromotionActive(Promotion promotion) {
        if (promotion == null || !promotion.isEnabled()) {
            return false;
        }
        LocalDate today = LocalDate.now();
        if (promotion.getStartDate() != null && promotion.getStartDate().isAfter(today)) {
            return false;
        }
        return promotion.getEndDate() == null || !promotion.getEndDate().isBefore(today);
    }

    private String buildPromotionStatus(Promotion promotion, LocalDate today) {
        if (!promotion.isEnabled()) {
            return "Disabled";
        }
        if (promotion.getStartDate() != null && promotion.getStartDate().isAfter(today)) {
            return "Scheduled";
        }
        if (promotion.getEndDate() != null && promotion.getEndDate().isBefore(today)) {
            return "Expired";
        }
        if (promotion.getEndDate() != null && !promotion.getEndDate().isAfter(today.plusDays(7))) {
            return "Ending soon";
        }
        return "Active";
    }

    private String buildPromotionNextAction(Promotion promotion, LocalDate today) {
        if (promotion.getEndDate() != null && !promotion.getEndDate().isAfter(today.plusDays(7))) {
            return "Push this promotion in WhatsApp and the public home before it expires";
        }
        if (isBlank(promotion.getBannerImagePath())) {
            return "Add banner and highlight it in the public home";
        }
        return "Keep it visible in WhatsApp and client-facing channels";
    }

    private String inferPromotionSegment(Promotion promotion) {
        String text = (firstNonBlank(promotion.getName(), "") + " " + firstNonBlank(promotion.getDescription(), "")).toLowerCase(Locale.ROOT);
        if (text.contains("restaurant") || text.contains("bar")) {
            return "Restaurants / bars";
        }
        if (text.contains("bodega") || text.contains("store") || text.contains("tienda")) {
            return "Stores / bodegas";
        }
        if (text.contains("refill") || text.contains("recarga")) {
            return "Frequent refill clients";
        }
        return "General audience / reorder push";
    }

    private String inferContentAudience(BlogPost post) {
        String text = (firstNonBlank(post.getTitle(), "") + " " + firstNonBlank(post.getSummary(), "")).toLowerCase(Locale.ROOT);
        if (text.contains("children") || text.contains("hijos") || text.contains("famil")) {
            return "Families / home clients";
        }
        if (text.contains("office") || text.contains("trabajo") || text.contains("home office")) {
            return "Workers / offices";
        }
        return "Trust and awareness campaigns";
    }

    private String firstNonBlank(String value, String fallback) {
        return isBlank(value) ? fallback : value;
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    @SuppressWarnings("unchecked")
    private List<Promotion> safePromotionList(List<?> promotions) {
        return promotions == null ? List.of() : (List<Promotion>) promotions;
    }

    @SuppressWarnings("unchecked")
    private List<BlogPost> safeBlogPostList(List<?> posts) {
        return posts == null ? List.of() : (List<BlogPost>) posts;
    }

    @SuppressWarnings("unchecked")
    private List<Testimonial> safeTestimonialList(List<?> testimonials) {
        return testimonials == null ? List.of() : (List<Testimonial>) testimonials;
    }
}
