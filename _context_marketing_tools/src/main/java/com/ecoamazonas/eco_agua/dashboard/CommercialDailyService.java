package com.ecoamazonas.eco_agua.dashboard;

import com.ecoamazonas.eco_agua.client.Client;
import com.ecoamazonas.eco_agua.client.ClientPortfolioRow;
import com.ecoamazonas.eco_agua.client.ClientPortfolioService;
import com.ecoamazonas.eco_agua.client.ClientPortfolioSnapshot;
import com.ecoamazonas.eco_agua.client.ClientRepository;
import com.ecoamazonas.eco_agua.delivery.DeliveryStatus;
import com.ecoamazonas.eco_agua.order.OrderStatus;
import com.ecoamazonas.eco_agua.order.SaleOrder;
import com.ecoamazonas.eco_agua.order.SaleOrderRepository;
import com.ecoamazonas.eco_agua.promotion.Promotion;
import com.ecoamazonas.eco_agua.promotion.PromotionService;
import com.ecoamazonas.eco_agua.reorder.ReorderAgendaRow;
import com.ecoamazonas.eco_agua.reorder.ReorderAgendaService;
import com.ecoamazonas.eco_agua.reorder.ReorderFollowUpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class CommercialDailyService {

    private final ClientPortfolioService clientPortfolioService;
    private final ReorderAgendaService reorderAgendaService;
    private final PromotionService promotionService;
    private final SaleOrderRepository saleOrderRepository;
    private final ClientRepository clientRepository;

    public CommercialDailyService(
            ClientPortfolioService clientPortfolioService,
            ReorderAgendaService reorderAgendaService,
            PromotionService promotionService,
            SaleOrderRepository saleOrderRepository,
            ClientRepository clientRepository
    ) {
        this.clientPortfolioService = clientPortfolioService;
        this.reorderAgendaService = reorderAgendaService;
        this.promotionService = promotionService;
        this.saleOrderRepository = saleOrderRepository;
        this.clientRepository = clientRepository;
    }

    @Transactional(readOnly = true)
    public CommercialDailySnapshot buildSnapshot(LocalDate fromDate, LocalDate toDate) {
        LocalDate effectiveToDate = toDate != null ? toDate : LocalDate.now();
        LocalDate effectiveFromDate = fromDate != null ? fromDate : effectiveToDate.minusDays(29);

        if (effectiveFromDate.isAfter(effectiveToDate)) {
            LocalDate tmp = effectiveFromDate;
            effectiveFromDate = effectiveToDate;
            effectiveToDate = tmp;
        }

        final LocalDate rangeFromDate = effectiveFromDate;
        final LocalDate rangeToDate = effectiveToDate;

        ClientPortfolioSnapshot portfolioSnapshot = clientPortfolioService.buildSnapshot(rangeFromDate, rangeToDate);
        List<ReorderAgendaRow> agendaRows = reorderAgendaService.buildAgenda(rangeToDate, null, 45, false);
        List<Promotion> activePromotions = promotionService.findAllActive();
        List<Client> activeClients = clientRepository.findByActiveTrueOrderByNameAsc();
        List<SaleOrder> todayOrders = saleOrderRepository.findByOrderDate(rangeToDate);
        List<SaleOrder> weekOrders = saleOrderRepository.findByOrderDateBetween(rangeToDate.minusDays(6), rangeToDate);
        List<SaleOrder> deliveryOrdersToday = saleOrderRepository.findDeliveryOrdersForDate(rangeToDate);

        BigDecimal salesToday = sumCommercialSales(todayOrders);
        BigDecimal salesWeek = sumCommercialSales(weekOrders);
        BigDecimal salesInRange = scale(portfolioSnapshot.getTotalRevenueInPeriod());

        int buyingClientsToday = countDistinctCommercialClients(todayOrders);
        int newClientsInRange = (int) activeClients.stream()
                .filter(client -> isRegisteredBetween(client, rangeFromDate, rangeToDate))
                .count();

        List<CommercialDailySnapshot.ActionRow> priorityRows = agendaRows.stream()
                .limit(12)
                .map(this::mapActionRow)
                .toList();

        int reorderDueCount = (int) agendaRows.stream()
                .filter(row -> row.getOverdueDays() > 0 || row.getProbabilityPercent() >= 70)
                .count();

        List<CommercialDailySnapshot.ClientFocusRow> reactivationRows = portfolioSnapshot.getReactivationCandidates().stream()
                .limit(8)
                .map(this::mapClientFocusRow)
                .toList();

        List<CommercialDailySnapshot.ClientFocusRow> dormantRows = portfolioSnapshot.getDormantClients().stream()
                .limit(8)
                .map(this::mapClientFocusRow)
                .toList();

        List<CommercialDailySnapshot.ClientFocusRow> creditRiskRows = portfolioSnapshot.getCreditRiskClients().stream()
                .limit(8)
                .map(this::mapClientFocusRow)
                .toList();

        List<CommercialDailySnapshot.ClientFocusRow> topRevenueRows = portfolioSnapshot.getTopRevenueClients().stream()
                .limit(6)
                .map(this::mapClientFocusRow)
                .toList();

        int coldClientsCount = countDistinctClients(
                portfolioSnapshot.getReactivationCandidates(),
                portfolioSnapshot.getDormantClients()
        );

        List<CommercialDailySnapshot.CampaignRow> campaignRows = activePromotions.stream()
                .sorted(Comparator
                        .comparing((Promotion promotion) -> promotion.getEndDate() != null ? promotion.getEndDate() : LocalDate.MAX)
                        .thenComparing(Promotion::getName, String.CASE_INSENSITIVE_ORDER))
                .limit(8)
                .map(this::mapCampaignRow)
                .toList();

        List<CommercialDailySnapshot.DeliveryIssueRow> deliveryIssueRows = deliveryOrdersToday.stream()
                .filter(this::hasCommercialDeliveryIssue)
                .limit(8)
                .map(this::mapDeliveryIssueRow)
                .toList();

        int pendingDeliveriesToday = (int) deliveryOrdersToday.stream()
                .filter(order -> order.getStatus() != OrderStatus.CANCELED)
                .filter(order -> order.getDeliveryStatus() != DeliveryStatus.DELIVERED)
                .filter(order -> order.getDeliveryStatus() != DeliveryStatus.CANCELED)
                .count();

        List<CommercialDailySnapshot.NewClientRow> newClientRows = activeClients.stream()
                .filter(client -> isRegisteredBetween(client, rangeFromDate, rangeToDate))
                .sorted(Comparator
                        .comparing(Client::getRegistrationDate, Comparator.nullsLast(LocalDateTime::compareTo))
                        .reversed())
                .limit(8)
                .map(this::mapNewClientRow)
                .toList();

        return new CommercialDailySnapshot(
                rangeFromDate,
                rangeToDate,
                salesToday,
                salesWeek,
                salesInRange,
                portfolioSnapshot.getTotalActiveClients(),
                buyingClientsToday,
                newClientsInRange,
                priorityRows.size(),
                reorderDueCount,
                coldClientsCount,
                creditRiskRows.size(),
                campaignRows.size(),
                pendingDeliveriesToday,
                priorityRows,
                reactivationRows,
                dormantRows,
                creditRiskRows,
                topRevenueRows,
                campaignRows,
                deliveryIssueRows,
                newClientRows
        );
    }

    private CommercialDailySnapshot.ActionRow mapActionRow(ReorderAgendaRow row) {
        String actionLabel = row.getActionLabel();
        if (row.getFollowUpStatus() == ReorderFollowUpStatus.CONTACTED && row.getNextContactDate() != null) {
            actionLabel = "Follow up " + row.getNextContactDate();
        }

        String followUpStatusLabel = row.getFollowUpStatus() != null ? row.getFollowUpStatus().name() : "PENDING";

        return new CommercialDailySnapshot.ActionRow(
                row.getClientId(),
                row.getClientName(),
                row.getProfileName(),
                row.getPhone(),
                buildWhatsappUrl(row.getPhone()),
                row.getLastOrderDate(),
                row.getDaysSinceLastOrder(),
                row.getExpectedNextOrderDate(),
                row.getOverdueDays(),
                row.getProbabilityPercent(),
                row.getStatusLabel(),
                actionLabel,
                followUpStatusLabel,
                row.getNextContactDate(),
                row.getFollowUpObservation()
        );
    }

    private CommercialDailySnapshot.ClientFocusRow mapClientFocusRow(ClientPortfolioRow row) {
        return new CommercialDailySnapshot.ClientFocusRow(
                row.getClientId(),
                row.getClientName(),
                row.getProfileName(),
                row.getPhone(),
                buildWhatsappUrl(row.getPhone()),
                row.getLastOrderDate(),
                row.getDaysSinceLastOrder(),
                scale(row.getTotalRevenueInPeriod()),
                scale(row.getCreditPendingAllTime()),
                row.getBorrowedBottlesAllTime(),
                row.getHealthLabel(),
                row.getHealthBadgeClass(),
                row.getRecommendedAction(),
                row.getStrategyNote()
        );
    }

    private CommercialDailySnapshot.CampaignRow mapCampaignRow(Promotion promotion) {
        int assignedClientsCount = promotion.getClients() != null ? promotion.getClients().size() : 0;
        LocalDate today = LocalDate.now();
        String statusLabel;
        String nextAction;

        if (promotion.getEndDate() != null && !promotion.getEndDate().isAfter(today.plusDays(3))) {
            statusLabel = "Closing soon";
            nextAction = "Push the last follow-up round and close pending orders.";
        } else if (assignedClientsCount == 0) {
            statusLabel = "Open segment";
            nextAction = "Assign target clients or apply it in today orders.";
        } else {
            statusLabel = "Running";
            nextAction = "Contact assigned clients and track response today.";
        }

        return new CommercialDailySnapshot.CampaignRow(
                promotion.getId(),
                promotion.getName(),
                promotion.getDescription(),
                promotion.getColorBorder(),
                promotion.getStartDate(),
                promotion.getEndDate(),
                assignedClientsCount,
                statusLabel,
                nextAction
        );
    }

    private CommercialDailySnapshot.DeliveryIssueRow mapDeliveryIssueRow(SaleOrder order) {
        String zoneName = order.getDeliveryZone() != null ? order.getDeliveryZone().getName() : "No zone";
        String observation = clean(order.getDeliveryObservation());
        String nextAction;
        String badgeClass;

        if (order.getDeliveryStatus() == DeliveryStatus.NOT_DELIVERED || order.getDeliveryStatus() == DeliveryStatus.RESCHEDULED) {
            nextAction = "Call the client and reschedule before losing the sale.";
            badgeClass = "text-bg-danger";
        } else if (order.getDeliveryStatus() == DeliveryStatus.IN_ROUTE || order.getDeliveryStatus() == DeliveryStatus.PENDING) {
            nextAction = "Track the delivery and confirm the arrival window.";
            badgeClass = "text-bg-warning";
        } else {
            nextAction = "Review the observation and close the post-sale follow-up.";
            badgeClass = "text-bg-secondary";
        }

        return new CommercialDailySnapshot.DeliveryIssueRow(
                order.getId(),
                order.getOrderNumber(),
                order.getClient() != null ? order.getClient().getName() : "No client",
                zoneName,
                order.getDeliveryPerson(),
                order.getDeliveryStatus() != null ? order.getDeliveryStatus().name() : "UNKNOWN",
                badgeClass,
                observation,
                safeInt(order.getContainersDelivered()),
                safeInt(order.getContainersReturned()),
                nextAction
        );
    }

    private CommercialDailySnapshot.NewClientRow mapNewClientRow(Client client) {
        return new CommercialDailySnapshot.NewClientRow(
                client.getId(),
                client.getName(),
                client.getProfile() != null ? client.getProfile().getName() : "-",
                client.getPhone(),
                buildWhatsappUrl(client.getPhone()),
                client.getRegistrationDate() != null ? client.getRegistrationDate().toLocalDate() : null
        );
    }

    private boolean hasCommercialDeliveryIssue(SaleOrder order) {
        if (order == null || order.getStatus() == OrderStatus.CANCELED) {
            return false;
        }

        if (order.getDeliveryStatus() == DeliveryStatus.NOT_DELIVERED
                || order.getDeliveryStatus() == DeliveryStatus.RESCHEDULED
                || order.getDeliveryStatus() == DeliveryStatus.PENDING
                || order.getDeliveryStatus() == DeliveryStatus.IN_ROUTE) {
            return true;
        }

        return clean(order.getDeliveryObservation()) != null;
    }

    private BigDecimal sumCommercialSales(List<SaleOrder> orders) {
        if (orders == null || orders.isEmpty()) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }

        return orders.stream()
                .filter(order -> order.getStatus() == OrderStatus.PAID || order.getStatus() == OrderStatus.CREDIT)
                .map(SaleOrder::getTotalAmount)
                .filter(amount -> amount != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);
    }

    private int countDistinctCommercialClients(List<SaleOrder> orders) {
        if (orders == null || orders.isEmpty()) {
            return 0;
        }

        Set<Long> clientIds = orders.stream()
                .filter(order -> order.getStatus() == OrderStatus.PAID || order.getStatus() == OrderStatus.CREDIT)
                .map(SaleOrder::getClient)
                .filter(client -> client != null && client.getId() != null)
                .map(Client::getId)
                .collect(Collectors.toCollection(LinkedHashSet::new));

        return clientIds.size();
    }

    private int countDistinctClients(List<ClientPortfolioRow> first, List<ClientPortfolioRow> second) {
        Set<Long> ids = new LinkedHashSet<>();

        if (first != null) {
            first.stream()
                    .map(ClientPortfolioRow::getClientId)
                    .filter(id -> id != null)
                    .forEach(ids::add);
        }

        if (second != null) {
            second.stream()
                    .map(ClientPortfolioRow::getClientId)
                    .filter(id -> id != null)
                    .forEach(ids::add);
        }

        return ids.size();
    }

    private boolean isRegisteredBetween(Client client, LocalDate fromDate, LocalDate toDate) {
        if (client == null || client.getRegistrationDate() == null) {
            return false;
        }

        LocalDate registrationDate = client.getRegistrationDate().toLocalDate();
        return !registrationDate.isBefore(fromDate) && !registrationDate.isAfter(toDate);
    }

    private BigDecimal scale(BigDecimal value) {
        if (value == null) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }

        return value.setScale(2, RoundingMode.HALF_UP);
    }

    private int safeInt(Integer value) {
        return value != null ? value : 0;
    }

    private String clean(String value) {
        if (value == null) {
            return null;
        }

        String trimmed = value.trim();
        return trimmed.isBlank() ? null : trimmed;
    }

    private String buildWhatsappUrl(String phone) {
        String cleaned = cleanPhone(phone);
        if (cleaned == null) {
            return null;
        }

        if (cleaned.length() == 9) {
            cleaned = "51" + cleaned;
        }

        return "https://wa.me/" + cleaned;
    }

    private String cleanPhone(String phone) {
        if (phone == null) {
            return null;
        }

        String digits = phone.replaceAll("\\D", "");
        return digits.isBlank() ? null : digits;
    }
}
