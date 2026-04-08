package com.ecoamazonas.eco_agua.marketing;

import java.time.LocalDate;
import java.util.List;

public class MarketingCampaignsSnapshot {

    private final LocalDate today;
    private final int activePromotionCount;
    private final int publishedPostCount;
    private final int draftPostCount;
    private final int activeTestimonialCount;
    private final int promotionsEndingSoonCount;
    private final List<CampaignRow> campaignRows;
    private final List<AssetRow> promotionAssets;
    private final List<AssetRow> contentAssets;
    private final List<AssetRow> testimonialAssets;
    private final List<TaskRow> pendingTasks;
    private final List<String> suggestedSegments;
    private final List<CtaTemplateRow> ctaTemplates;

    public MarketingCampaignsSnapshot(
            LocalDate today,
            int activePromotionCount,
            int publishedPostCount,
            int draftPostCount,
            int activeTestimonialCount,
            int promotionsEndingSoonCount,
            List<CampaignRow> campaignRows,
            List<AssetRow> promotionAssets,
            List<AssetRow> contentAssets,
            List<AssetRow> testimonialAssets,
            List<TaskRow> pendingTasks,
            List<String> suggestedSegments,
            List<CtaTemplateRow> ctaTemplates
    ) {
        this.today = today;
        this.activePromotionCount = activePromotionCount;
        this.publishedPostCount = publishedPostCount;
        this.draftPostCount = draftPostCount;
        this.activeTestimonialCount = activeTestimonialCount;
        this.promotionsEndingSoonCount = promotionsEndingSoonCount;
        this.campaignRows = campaignRows;
        this.promotionAssets = promotionAssets;
        this.contentAssets = contentAssets;
        this.testimonialAssets = testimonialAssets;
        this.pendingTasks = pendingTasks;
        this.suggestedSegments = suggestedSegments;
        this.ctaTemplates = ctaTemplates;
    }

    public LocalDate getToday() { return today; }
    public int getActivePromotionCount() { return activePromotionCount; }
    public int getPublishedPostCount() { return publishedPostCount; }
    public int getDraftPostCount() { return draftPostCount; }
    public int getActiveTestimonialCount() { return activeTestimonialCount; }
    public int getPromotionsEndingSoonCount() { return promotionsEndingSoonCount; }
    public List<CampaignRow> getCampaignRows() { return campaignRows; }
    public List<AssetRow> getPromotionAssets() { return promotionAssets; }
    public List<AssetRow> getContentAssets() { return contentAssets; }
    public List<AssetRow> getTestimonialAssets() { return testimonialAssets; }
    public List<TaskRow> getPendingTasks() { return pendingTasks; }
    public List<String> getSuggestedSegments() { return suggestedSegments; }
    public List<CtaTemplateRow> getCtaTemplates() { return ctaTemplates; }

    public static class CampaignRow {
        private final String name;
        private final String type;
        private final String targetSegment;
        private final String channel;
        private final LocalDate startDate;
        private final LocalDate endDate;
        private final String status;
        private final String cta;
        private final String nextAction;
        private final String adminUrl;

        public CampaignRow(String name, String type, String targetSegment, String channel,
                           LocalDate startDate, LocalDate endDate, String status,
                           String cta, String nextAction, String adminUrl) {
            this.name = name;
            this.type = type;
            this.targetSegment = targetSegment;
            this.channel = channel;
            this.startDate = startDate;
            this.endDate = endDate;
            this.status = status;
            this.cta = cta;
            this.nextAction = nextAction;
            this.adminUrl = adminUrl;
        }

        public String getName() { return name; }
        public String getType() { return type; }
        public String getTargetSegment() { return targetSegment; }
        public String getChannel() { return channel; }
        public LocalDate getStartDate() { return startDate; }
        public LocalDate getEndDate() { return endDate; }
        public String getStatus() { return status; }
        public String getCta() { return cta; }
        public String getNextAction() { return nextAction; }
        public String getAdminUrl() { return adminUrl; }
    }

    public static class AssetRow {
        private final String title;
        private final String status;
        private final String description;
        private final String actionLabel;
        private final String actionUrl;
        private final LocalDate publishedDate;

        public AssetRow(String title, String status, String description,
                        String actionLabel, String actionUrl, LocalDate publishedDate) {
            this.title = title;
            this.status = status;
            this.description = description;
            this.actionLabel = actionLabel;
            this.actionUrl = actionUrl;
            this.publishedDate = publishedDate;
        }

        public String getTitle() { return title; }
        public String getStatus() { return status; }
        public String getDescription() { return description; }
        public String getActionLabel() { return actionLabel; }
        public String getActionUrl() { return actionUrl; }
        public LocalDate getPublishedDate() { return publishedDate; }
    }

    public static class TaskRow {
        private final String priority;
        private final String title;
        private final String description;
        private final String actionLabel;
        private final String actionUrl;

        public TaskRow(String priority, String title, String description, String actionLabel, String actionUrl) {
            this.priority = priority;
            this.title = title;
            this.description = description;
            this.actionLabel = actionLabel;
            this.actionUrl = actionUrl;
        }

        public String getPriority() { return priority; }
        public String getTitle() { return title; }
        public String getDescription() { return description; }
        public String getActionLabel() { return actionLabel; }
        public String getActionUrl() { return actionUrl; }
    }

    public static class CtaTemplateRow {
        private final String title;
        private final String audience;
        private final String message;

        public CtaTemplateRow(String title, String audience, String message) {
            this.title = title;
            this.audience = audience;
            this.message = message;
        }

        public String getTitle() { return title; }
        public String getAudience() { return audience; }
        public String getMessage() { return message; }
    }
}
