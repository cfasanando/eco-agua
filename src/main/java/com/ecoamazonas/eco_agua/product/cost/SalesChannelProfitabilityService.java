package com.ecoamazonas.eco_agua.product.cost;

import com.ecoamazonas.eco_agua.order.OrderStatus;
import com.ecoamazonas.eco_agua.order.SaleOrder;
import com.ecoamazonas.eco_agua.order.SaleOrderItem;
import com.ecoamazonas.eco_agua.order.SaleOrderRepository;
import com.ecoamazonas.eco_agua.order.SalesChannel;
import com.ecoamazonas.eco_agua.product.Product;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class SalesChannelProfitabilityService {

    private static final BigDecimal ZERO = BigDecimal.ZERO;

    private final SaleOrderRepository saleOrderRepository;
    private final ProductCostService productCostService;

    public SalesChannelProfitabilityService(
            SaleOrderRepository saleOrderRepository,
            ProductCostService productCostService
    ) {
        this.saleOrderRepository = saleOrderRepository;
        this.productCostService = productCostService;
    }

    @Transactional(readOnly = true)
    public SalesChannelProfitabilitySnapshot buildSnapshot(LocalDate startDate, LocalDate endDate) {
        LocalDate resolvedStart = startDate;
        LocalDate resolvedEnd = endDate;

        if (resolvedStart == null || resolvedEnd == null) {
            LocalDate today = LocalDate.now();
            resolvedStart = today.withDayOfMonth(1);
            resolvedEnd = today;
        }

        if (resolvedStart.isAfter(resolvedEnd)) {
            LocalDate temp = resolvedStart;
            resolvedStart = resolvedEnd;
            resolvedEnd = temp;
        }

        Map<SalesChannel, SalesChannelProfitabilityRow> rowsByChannel = new LinkedHashMap<>();
        Arrays.stream(SalesChannel.values())
                .forEach(channel -> rowsByChannel.put(channel, new SalesChannelProfitabilityRow(channel)));

        Map<Long, BigDecimal> unitCostByProductId = new LinkedHashMap<>();
        List<SaleOrder> orders = saleOrderRepository.findByOrderDateBetween(resolvedStart, resolvedEnd).stream()
                .filter(this::isCommercialOrder)
                .toList();

        for (SaleOrder order : orders) {
            SalesChannel channel = order.getSalesChannel() != null ? order.getSalesChannel() : SalesChannel.WHATSAPP;
            SalesChannelProfitabilityRow row = rowsByChannel.computeIfAbsent(channel, SalesChannelProfitabilityRow::new);
            row.addOrder(order.getStatus());

            if (order.getItems() == null) {
                continue;
            }

            for (SaleOrderItem item : order.getItems()) {
                BigDecimal quantity = quantity(item.getQuantity());
                BigDecimal revenue = resolveItemRevenue(item, quantity);
                BigDecimal unitCost = resolveUnitCost(item.getProduct(), unitCostByProductId);
                BigDecimal totalCost = money(unitCost.multiply(quantity));
                row.addItem(quantity, revenue, totalCost);
            }
        }

        List<SalesChannelProfitabilityRow> rows = rowsByChannel.values().stream()
                .sorted(Comparator
                        .comparing(SalesChannelProfitabilityRow::hasMovement).reversed()
                        .thenComparing(SalesChannelProfitabilityRow::getGrossProfit, Comparator.reverseOrder())
                        .thenComparing(SalesChannelProfitabilityRow::getRevenue, Comparator.reverseOrder())
                        .thenComparing(SalesChannelProfitabilityRow::getChannelLabel, String.CASE_INSENSITIVE_ORDER))
                .toList();

        SalesChannelProfitabilitySummary summary = new SalesChannelProfitabilitySummary(rows);
        return new SalesChannelProfitabilitySnapshot(resolvedStart, resolvedEnd, summary, rows);
    }

    private boolean isCommercialOrder(SaleOrder order) {
        if (order == null || order.getStatus() == null) {
            return false;
        }
        return order.getStatus() == OrderStatus.PAID || order.getStatus() == OrderStatus.CREDIT;
    }

    private BigDecimal resolveItemRevenue(SaleOrderItem item, BigDecimal quantity) {
        if (item == null) {
            return ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        if (item.getTotal() != null) {
            return money(item.getTotal());
        }
        return money(quantity.multiply(safe(item.getUnitPrice())));
    }

    private BigDecimal resolveUnitCost(Product product, Map<Long, BigDecimal> unitCostByProductId) {
        if (product == null || product.getId() == null) {
            return ZERO.setScale(4, RoundingMode.HALF_UP);
        }
        return unitCostByProductId.computeIfAbsent(product.getId(), productId -> {
            ProductCostDetail detail = productCostService.calculateCostDetail(productId);
            return safe(detail.getCvu()).setScale(4, RoundingMode.HALF_UP);
        });
    }

    private BigDecimal safe(BigDecimal value) {
        return value != null ? value : ZERO;
    }

    private BigDecimal money(BigDecimal value) {
        return safe(value).setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal quantity(BigDecimal value) {
        return safe(value).setScale(2, RoundingMode.HALF_UP);
    }
}
