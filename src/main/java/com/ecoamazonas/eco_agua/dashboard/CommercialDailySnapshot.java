package com.ecoamazonas.eco_agua.dashboard;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public class CommercialDailySnapshot {

    private final LocalDate fromDate;
    private final LocalDate toDate;
    private final BigDecimal salesToday;
    private final BigDecimal salesWeek;
    private final BigDecimal salesInRange;
    private final int activeClients;
    private final int buyingClientsToday;
    private final int newClientsInRange;
    private final int clientsToContactToday;
    private final int reorderDueCount;
    private final int coldClientsCount;
    private final int creditRiskCount;
    private final int activePromotionsCount;
    private final int pendingDeliveriesToday;
    private final List<ActionRow> priorityRows;
    private final List<ClientFocusRow> reactivationRows;
    private final List<ClientFocusRow> dormantRows;
    private final List<ClientFocusRow> creditRiskRows;
    private final List<ClientFocusRow> topRevenueRows;
    private final List<CampaignRow> campaignRows;
    private final List<DeliveryIssueRow> deliveryIssueRows;
    private final List<NewClientRow> newClientRows;

    public CommercialDailySnapshot(
            LocalDate fromDate,
            LocalDate toDate,
            BigDecimal salesToday,
            BigDecimal salesWeek,
            BigDecimal salesInRange,
            int activeClients,
            int buyingClientsToday,
            int newClientsInRange,
            int clientsToContactToday,
            int reorderDueCount,
            int coldClientsCount,
            int creditRiskCount,
            int activePromotionsCount,
            int pendingDeliveriesToday,
            List<ActionRow> priorityRows,
            List<ClientFocusRow> reactivationRows,
            List<ClientFocusRow> dormantRows,
            List<ClientFocusRow> creditRiskRows,
            List<ClientFocusRow> topRevenueRows,
            List<CampaignRow> campaignRows,
            List<DeliveryIssueRow> deliveryIssueRows,
            List<NewClientRow> newClientRows
    ) {
        this.fromDate = fromDate;
        this.toDate = toDate;
        this.salesToday = salesToday;
        this.salesWeek = salesWeek;
        this.salesInRange = salesInRange;
        this.activeClients = activeClients;
        this.buyingClientsToday = buyingClientsToday;
        this.newClientsInRange = newClientsInRange;
        this.clientsToContactToday = clientsToContactToday;
        this.reorderDueCount = reorderDueCount;
        this.coldClientsCount = coldClientsCount;
        this.creditRiskCount = creditRiskCount;
        this.activePromotionsCount = activePromotionsCount;
        this.pendingDeliveriesToday = pendingDeliveriesToday;
        this.priorityRows = priorityRows;
        this.reactivationRows = reactivationRows;
        this.dormantRows = dormantRows;
        this.creditRiskRows = creditRiskRows;
        this.topRevenueRows = topRevenueRows;
        this.campaignRows = campaignRows;
        this.deliveryIssueRows = deliveryIssueRows;
        this.newClientRows = newClientRows;
    }

    public LocalDate getFromDate() {
        return fromDate;
    }

    public LocalDate getToDate() {
        return toDate;
    }

    public BigDecimal getSalesToday() {
        return salesToday;
    }

    public BigDecimal getSalesWeek() {
        return salesWeek;
    }

    public BigDecimal getSalesInRange() {
        return salesInRange;
    }

    public int getActiveClients() {
        return activeClients;
    }

    public int getBuyingClientsToday() {
        return buyingClientsToday;
    }

    public int getNewClientsInRange() {
        return newClientsInRange;
    }

    public int getClientsToContactToday() {
        return clientsToContactToday;
    }

    public int getReorderDueCount() {
        return reorderDueCount;
    }

    public int getColdClientsCount() {
        return coldClientsCount;
    }

    public int getCreditRiskCount() {
        return creditRiskCount;
    }

    public int getActivePromotionsCount() {
        return activePromotionsCount;
    }

    public int getPendingDeliveriesToday() {
        return pendingDeliveriesToday;
    }

    public List<ActionRow> getPriorityRows() {
        return priorityRows;
    }

    public List<ClientFocusRow> getReactivationRows() {
        return reactivationRows;
    }

    public List<ClientFocusRow> getDormantRows() {
        return dormantRows;
    }

    public List<ClientFocusRow> getCreditRiskRows() {
        return creditRiskRows;
    }

    public List<ClientFocusRow> getTopRevenueRows() {
        return topRevenueRows;
    }

    public List<CampaignRow> getCampaignRows() {
        return campaignRows;
    }

    public List<DeliveryIssueRow> getDeliveryIssueRows() {
        return deliveryIssueRows;
    }

    public List<NewClientRow> getNewClientRows() {
        return newClientRows;
    }

    public static class ActionRow {
        private final Long clientId;
        private final String clientName;
        private final String profileName;
        private final String phone;
        private final String whatsappUrl;
        private final LocalDate lastOrderDate;
        private final long daysSinceLastOrder;
        private final LocalDate expectedNextOrderDate;
        private final long overdueDays;
        private final int probabilityPercent;
        private final String statusLabel;
        private final String actionLabel;
        private final String followUpStatusLabel;
        private final LocalDate nextContactDate;
        private final String observation;

        public ActionRow(
                Long clientId,
                String clientName,
                String profileName,
                String phone,
                String whatsappUrl,
                LocalDate lastOrderDate,
                long daysSinceLastOrder,
                LocalDate expectedNextOrderDate,
                long overdueDays,
                int probabilityPercent,
                String statusLabel,
                String actionLabel,
                String followUpStatusLabel,
                LocalDate nextContactDate,
                String observation
        ) {
            this.clientId = clientId;
            this.clientName = clientName;
            this.profileName = profileName;
            this.phone = phone;
            this.whatsappUrl = whatsappUrl;
            this.lastOrderDate = lastOrderDate;
            this.daysSinceLastOrder = daysSinceLastOrder;
            this.expectedNextOrderDate = expectedNextOrderDate;
            this.overdueDays = overdueDays;
            this.probabilityPercent = probabilityPercent;
            this.statusLabel = statusLabel;
            this.actionLabel = actionLabel;
            this.followUpStatusLabel = followUpStatusLabel;
            this.nextContactDate = nextContactDate;
            this.observation = observation;
        }

        public Long getClientId() {
            return clientId;
        }

        public String getClientName() {
            return clientName;
        }

        public String getProfileName() {
            return profileName;
        }

        public String getPhone() {
            return phone;
        }

        public String getWhatsappUrl() {
            return whatsappUrl;
        }

        public LocalDate getLastOrderDate() {
            return lastOrderDate;
        }

        public long getDaysSinceLastOrder() {
            return daysSinceLastOrder;
        }

        public LocalDate getExpectedNextOrderDate() {
            return expectedNextOrderDate;
        }

        public long getOverdueDays() {
            return overdueDays;
        }

        public int getProbabilityPercent() {
            return probabilityPercent;
        }

        public String getStatusLabel() {
            return statusLabel;
        }

        public String getActionLabel() {
            return actionLabel;
        }

        public String getFollowUpStatusLabel() {
            return followUpStatusLabel;
        }

        public LocalDate getNextContactDate() {
            return nextContactDate;
        }

        public String getObservation() {
            return observation;
        }
    }

    public static class ClientFocusRow {
        private final Long clientId;
        private final String clientName;
        private final String profileName;
        private final String phone;
        private final String whatsappUrl;
        private final LocalDate lastOrderDate;
        private final long daysSinceLastOrder;
        private final BigDecimal revenueInPeriod;
        private final BigDecimal creditPending;
        private final int borrowedBottles;
        private final String healthLabel;
        private final String healthBadgeClass;
        private final String recommendedAction;
        private final String strategyNote;

        public ClientFocusRow(
                Long clientId,
                String clientName,
                String profileName,
                String phone,
                String whatsappUrl,
                LocalDate lastOrderDate,
                long daysSinceLastOrder,
                BigDecimal revenueInPeriod,
                BigDecimal creditPending,
                int borrowedBottles,
                String healthLabel,
                String healthBadgeClass,
                String recommendedAction,
                String strategyNote
        ) {
            this.clientId = clientId;
            this.clientName = clientName;
            this.profileName = profileName;
            this.phone = phone;
            this.whatsappUrl = whatsappUrl;
            this.lastOrderDate = lastOrderDate;
            this.daysSinceLastOrder = daysSinceLastOrder;
            this.revenueInPeriod = revenueInPeriod;
            this.creditPending = creditPending;
            this.borrowedBottles = borrowedBottles;
            this.healthLabel = healthLabel;
            this.healthBadgeClass = healthBadgeClass;
            this.recommendedAction = recommendedAction;
            this.strategyNote = strategyNote;
        }

        public Long getClientId() {
            return clientId;
        }

        public String getClientName() {
            return clientName;
        }

        public String getProfileName() {
            return profileName;
        }

        public String getPhone() {
            return phone;
        }

        public String getWhatsappUrl() {
            return whatsappUrl;
        }

        public LocalDate getLastOrderDate() {
            return lastOrderDate;
        }

        public long getDaysSinceLastOrder() {
            return daysSinceLastOrder;
        }

        public BigDecimal getRevenueInPeriod() {
            return revenueInPeriod;
        }

        public BigDecimal getCreditPending() {
            return creditPending;
        }

        public int getBorrowedBottles() {
            return borrowedBottles;
        }

        public String getHealthLabel() {
            return healthLabel;
        }

        public String getHealthBadgeClass() {
            return healthBadgeClass;
        }

        public String getRecommendedAction() {
            return recommendedAction;
        }

        public String getStrategyNote() {
            return strategyNote;
        }
    }

    public static class CampaignRow {
        private final Long promotionId;
        private final String name;
        private final String description;
        private final String colorBorder;
        private final LocalDate startDate;
        private final LocalDate endDate;
        private final int assignedClientsCount;
        private final String statusLabel;
        private final String nextAction;

        public CampaignRow(
                Long promotionId,
                String name,
                String description,
                String colorBorder,
                LocalDate startDate,
                LocalDate endDate,
                int assignedClientsCount,
                String statusLabel,
                String nextAction
        ) {
            this.promotionId = promotionId;
            this.name = name;
            this.description = description;
            this.colorBorder = colorBorder;
            this.startDate = startDate;
            this.endDate = endDate;
            this.assignedClientsCount = assignedClientsCount;
            this.statusLabel = statusLabel;
            this.nextAction = nextAction;
        }

        public Long getPromotionId() {
            return promotionId;
        }

        public String getName() {
            return name;
        }

        public String getDescription() {
            return description;
        }

        public String getColorBorder() {
            return colorBorder;
        }

        public LocalDate getStartDate() {
            return startDate;
        }

        public LocalDate getEndDate() {
            return endDate;
        }

        public int getAssignedClientsCount() {
            return assignedClientsCount;
        }

        public String getStatusLabel() {
            return statusLabel;
        }

        public String getNextAction() {
            return nextAction;
        }
    }

    public static class DeliveryIssueRow {
        private final Long orderId;
        private final Integer orderNumber;
        private final String clientName;
        private final String zoneName;
        private final String deliveryPerson;
        private final String deliveryStatusLabel;
        private final String deliveryBadgeClass;
        private final String deliveryObservation;
        private final int containersDelivered;
        private final int containersReturned;
        private final String nextAction;

        public DeliveryIssueRow(
                Long orderId,
                Integer orderNumber,
                String clientName,
                String zoneName,
                String deliveryPerson,
                String deliveryStatusLabel,
                String deliveryBadgeClass,
                String deliveryObservation,
                int containersDelivered,
                int containersReturned,
                String nextAction
        ) {
            this.orderId = orderId;
            this.orderNumber = orderNumber;
            this.clientName = clientName;
            this.zoneName = zoneName;
            this.deliveryPerson = deliveryPerson;
            this.deliveryStatusLabel = deliveryStatusLabel;
            this.deliveryBadgeClass = deliveryBadgeClass;
            this.deliveryObservation = deliveryObservation;
            this.containersDelivered = containersDelivered;
            this.containersReturned = containersReturned;
            this.nextAction = nextAction;
        }

        public Long getOrderId() {
            return orderId;
        }

        public Integer getOrderNumber() {
            return orderNumber;
        }

        public String getClientName() {
            return clientName;
        }

        public String getZoneName() {
            return zoneName;
        }

        public String getDeliveryPerson() {
            return deliveryPerson;
        }

        public String getDeliveryStatusLabel() {
            return deliveryStatusLabel;
        }

        public String getDeliveryBadgeClass() {
            return deliveryBadgeClass;
        }

        public String getDeliveryObservation() {
            return deliveryObservation;
        }

        public int getContainersDelivered() {
            return containersDelivered;
        }

        public int getContainersReturned() {
            return containersReturned;
        }

        public String getNextAction() {
            return nextAction;
        }
    }

    public static class NewClientRow {
        private final Long clientId;
        private final String clientName;
        private final String profileName;
        private final String phone;
        private final String whatsappUrl;
        private final LocalDate registrationDate;

        public NewClientRow(
                Long clientId,
                String clientName,
                String profileName,
                String phone,
                String whatsappUrl,
                LocalDate registrationDate
        ) {
            this.clientId = clientId;
            this.clientName = clientName;
            this.profileName = profileName;
            this.phone = phone;
            this.whatsappUrl = whatsappUrl;
            this.registrationDate = registrationDate;
        }

        public Long getClientId() {
            return clientId;
        }

        public String getClientName() {
            return clientName;
        }

        public String getProfileName() {
            return profileName;
        }

        public String getPhone() {
            return phone;
        }

        public String getWhatsappUrl() {
            return whatsappUrl;
        }

        public LocalDate getRegistrationDate() {
            return registrationDate;
        }
    }
}
