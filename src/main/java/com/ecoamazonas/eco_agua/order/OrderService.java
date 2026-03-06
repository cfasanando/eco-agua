package com.ecoamazonas.eco_agua.order;

import com.ecoamazonas.eco_agua.client.Client;
import com.ecoamazonas.eco_agua.client.ClientRepository;
import com.ecoamazonas.eco_agua.inventory.InventoryMovementType;
import com.ecoamazonas.eco_agua.inventory.InventoryService;
import com.ecoamazonas.eco_agua.product.Product;
import com.ecoamazonas.eco_agua.product.ProductRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;

@Service
public class OrderService {

    private final SaleOrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final ClientRepository clientRepository;
    private final InventoryService inventoryService;

    public OrderService(
            SaleOrderRepository orderRepository,
            ProductRepository productRepository,
            ClientRepository clientRepository,
            InventoryService inventoryService
    ) {
        this.orderRepository = orderRepository;
        this.productRepository = productRepository;
        this.clientRepository = clientRepository;
        this.inventoryService = inventoryService;
    }

    @Transactional
    public SaleOrder createOrderFromForm(
            LocalDate orderDate,
            Long clientId,
            String deliveryPerson,
            Integer borrowedBottles,
            String comment,
            List<Long> productIds,
            List<BigDecimal> quantities,
            List<BigDecimal> unitPrices,
            List<BigDecimal> lineTotals,
            OrderStatus initialStatus
    ) {
        if (clientId == null) {
            throw new IllegalArgumentException("Client must be selected.");
        }

        if (productIds == null || productIds.isEmpty()) {
            throw new IllegalArgumentException("Order must contain at least one product line.");
        }

        Client client = clientRepository.findById(clientId)
                .orElseThrow(() -> new IllegalArgumentException("Client not found: " + clientId));

        LocalDate effectiveDate = (orderDate != null ? orderDate : LocalDate.now());

        SaleOrder order = new SaleOrder();
        order.setClient(client);
        order.setOrderDate(effectiveDate);
        order.setStatus(initialStatus != null ? initialStatus : OrderStatus.REQUESTED);
        order.setDeliveryPerson(deliveryPerson);
        order.setBorrowedBottles(borrowedBottles != null ? borrowedBottles : 0);
        order.setComment(comment);

        long countForDay = orderRepository.countByOrderDate(effectiveDate);
        order.setOrderNumber((int) (countForDay + 1));

        BigDecimal total = BigDecimal.ZERO;

        for (int i = 0; i < productIds.size(); i++) {
            Long productId = productIds.get(i);
            if (productId == null) {
                continue;
            }

            BigDecimal qty = (quantities != null && quantities.size() > i)
                    ? quantities.get(i)
                    : null;
            BigDecimal price = (unitPrices != null && unitPrices.size() > i)
                    ? unitPrices.get(i)
                    : null;

            if (qty == null || price == null) {
                continue;
            }
            if (qty.compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }

            Product product = productRepository.findById(productId)
                    .orElseThrow(() -> new IllegalArgumentException("Product not found: " + productId));

            BigDecimal availableStock = product.getStock() != null ? product.getStock() : BigDecimal.ZERO;
            if (availableStock.compareTo(qty) < 0) {
                throw new IllegalArgumentException(
                        "Insufficient stock for product: " + product.getName() +
                                " (available: " + availableStock + ", required: " + qty + ")"
                );
            }

            SaleOrderItem item = new SaleOrderItem();
            item.setProduct(product);
            item.setDescription(product.getName());
            item.setQuantity(qty);
            item.setUnitPrice(price);

            BigDecimal lineTotal = null;
            if (lineTotals != null && lineTotals.size() > i) {
                lineTotal = lineTotals.get(i);
            }
            if (lineTotal == null) {
                lineTotal = qty.multiply(price);
            }
            lineTotal = lineTotal.setScale(2, RoundingMode.HALF_UP);
            item.setTotal(lineTotal);

            order.addItem(item);
            total = total.add(lineTotal);
        }

        if (order.getItems().isEmpty()) {
            throw new IllegalArgumentException("Order has no valid lines.");
        }

        order.setTotalAmount(total);

        SaleOrder saved = orderRepository.save(order);

        // Decrease stock when order is created
        saved.getItems().forEach(item -> inventoryService.registerProductMovement(
                item.getProduct().getId(),
                BigDecimal.ZERO,
                item.getQuantity(),
                InventoryMovementType.SALE,
                "SALE_ORDER",
                saved.getId(),
                "Order #" + saved.getOrderNumber(),
                saved.getOrderDate()
        ));

        return saved;
    }

    @Transactional
    public SaleOrder changeStatus(Long orderId, OrderStatus newStatus) {
        return changeStatus(orderId, newStatus, false, null, null);
    }

    @Transactional
    public SaleOrder changeStatus(
            Long orderId,
            OrderStatus newStatus,
            Boolean returnToStock,
            String cancelReason,
            LocalDate cancelDate
    ) {
        SaleOrder order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found: " + orderId));

        if (order.getStatus() == newStatus) {
            return order;
        }

        // Special handling when canceling an order
        if (newStatus == OrderStatus.CANCELED && order.getStatus() != OrderStatus.CANCELED) {
            boolean effectiveReturn = Boolean.TRUE.equals(returnToStock);
            LocalDate movementDate = (cancelDate != null ? cancelDate : LocalDate.now());

            if (effectiveReturn) {
                String observation = "Order canceled - stock returned";
                if (cancelReason != null && !cancelReason.isBlank()) {
                    observation = observation + ". Reason: " + cancelReason;
                }

                final String observationText = observation;
                final LocalDate finalMovementDate = movementDate;

                order.getItems().forEach(item -> inventoryService.registerProductMovement(
                        item.getProduct().getId(),
                        item.getQuantity(),
                        BigDecimal.ZERO,
                        InventoryMovementType.ADJUSTMENT,
                        "SALE_ORDER",
                        order.getId(),
                        observationText,
                        finalMovementDate
                ));
            }
        }

        order.setStatus(newStatus);
        return order;
    }

    @Transactional(readOnly = true)
    public List<SaleOrder> findOrdersForDate(LocalDate date) {
        return orderRepository.findByOrderDate(date);
    }

    @Transactional(readOnly = true)
    public List<SaleOrder> findOrdersForDateAndStatus(LocalDate date, OrderStatus status) {
        return orderRepository.findByOrderDateAndStatus(date, status);
    }

    @Transactional(readOnly = true)
    public List<SaleOrder> findOrdersBetweenDatesAndStatus(
            LocalDate startDate,
            LocalDate endDate,
            OrderStatus status
    ) {
        if (startDate == null && endDate == null) {
            throw new IllegalArgumentException("At least one date must be provided.");
        }

        if (startDate == null) {
            startDate = endDate;
        }
        if (endDate == null) {
            endDate = startDate;
        }
        if (endDate.isBefore(startDate)) {
            LocalDate tmp = startDate;
            startDate = endDate;
            endDate = tmp;
        }

        return orderRepository.findByOrderDateBetweenAndStatus(startDate, endDate, status);
    }

    @Transactional(readOnly = true)
    public SaleOrder findById(Long id) {
        return orderRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Order not found: " + id));
    }

    // >>> NEW: helper used by cashflow to get paid orders in a period
    @Transactional(readOnly = true)
    public List<SaleOrder> findPaidOrdersBetween(LocalDate startDate, LocalDate endDate) {
        return findOrdersBetweenDatesAndStatus(startDate, endDate, OrderStatus.PAID);
    }
}
