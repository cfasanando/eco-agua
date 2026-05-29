package com.ecoamazonas.eco_agua.delivery;

import com.ecoamazonas.eco_agua.order.OrderStatus;
import com.ecoamazonas.eco_agua.order.SaleOrder;
import com.ecoamazonas.eco_agua.order.SaleOrderRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

@Service
public class DeliveryDailyService {

    private final SaleOrderRepository saleOrderRepository;
    private final DeliveryZoneRepository deliveryZoneRepository;
    private final SaleOrderDeliveryEventRepository deliveryEventRepository;

    public DeliveryDailyService(SaleOrderRepository saleOrderRepository, DeliveryZoneRepository deliveryZoneRepository, SaleOrderDeliveryEventRepository deliveryEventRepository) {
        this.saleOrderRepository = saleOrderRepository;
        this.deliveryZoneRepository = deliveryZoneRepository;
        this.deliveryEventRepository = deliveryEventRepository;
    }

    @Transactional(readOnly = true)
    public List<DeliveryDailyRow> findRows(LocalDate date, String deliveryPerson, DeliveryStatus deliveryStatus) {
        LocalDate effectiveDate = date != null ? date : LocalDate.now();
        List<SaleOrder> orders = saleOrderRepository.findDeliveryOrdersForDate(effectiveDate);

        return orders.stream()
                .filter(order -> order.getStatus() != OrderStatus.CANCELED)
                .filter(order -> deliveryPerson == null || deliveryPerson.isBlank() || safeEqualsIgnoreCase(order.getDeliveryPerson(), deliveryPerson))
                .filter(order -> deliveryStatus == null || order.getDeliveryStatus() == deliveryStatus)
                .sorted(Comparator.comparing(SaleOrder::getDeliveryOrderIndex, Comparator.nullsLast(Integer::compareTo))
                        .thenComparing(SaleOrder::getOrderNumber, Comparator.nullsLast(Integer::compareTo))
                        .thenComparing(SaleOrder::getId))
                .map(order -> new DeliveryDailyRow(
                        order.getId(),
                        order.getOrderNumber(),
                        order.getOrderDate(),
                        order.getClient() != null ? order.getClient().getName() : "-",
                        order.getClient() != null ? order.getClient().getPhone() : null,
                        order.getClient() != null ? order.getClient().getAddress() : null,
                        order.getClient() != null ? order.getClient().getReference() : null,
                        order.getDeliveryZone() != null ? order.getDeliveryZone().getName() : null,
                        order.getDeliveryPerson(),
                        order.getDeliveryStatus(),
                        order.getTotalAmount(),
                        order.getBorrowedBottles()
                ))
                .toList();
    }

    @Transactional(readOnly = true)
    public SaleOrder findDetailedOrder(Long id) {
        return saleOrderRepository.findDeliveryOrderById(id).orElseThrow(() -> new IllegalArgumentException("Order not found: " + id));
    }

    @Transactional(readOnly = true)
    public List<SaleOrderDeliveryEvent> findEvents(Long orderId) {
        return deliveryEventRepository.findBySaleOrderIdOrderByEventDateDescIdDesc(orderId);
    }

    @Transactional(readOnly = true)
    public List<DeliveryZone> findZones() {
        return deliveryZoneRepository.findAllByOrderByNameAsc();
    }

    @Transactional
    public SaleOrder updateRoute(Long orderId, Long deliveryZoneId, Integer deliveryOrderIndex, String deliveryPerson) {
        SaleOrder order = saleOrderRepository.findById(orderId).orElseThrow(() -> new IllegalArgumentException("Order not found: " + orderId));
        DeliveryZone zone = null;
        if (deliveryZoneId != null) {
            zone = deliveryZoneRepository.findById(deliveryZoneId).orElseThrow(() -> new IllegalArgumentException("Delivery zone not found: " + deliveryZoneId));
        }

        order.setDeliveryZone(zone);
        order.setDeliveryOrderIndex(deliveryOrderIndex);
        order.setDeliveryPerson(clean(deliveryPerson));
        saleOrderRepository.save(order);
        registerEvent(order, DeliveryEventType.NOTE, "Route assignment updated.");
        return order;
    }

    @Transactional public SaleOrder markInRoute(Long orderId, String observation) { return updateDeliveryStatus(orderId, DeliveryStatus.IN_ROUTE, DeliveryEventType.IN_ROUTE, observation, false); }
    @Transactional public SaleOrder markDelivered(Long orderId, String observation) { return updateDeliveryStatus(orderId, DeliveryStatus.DELIVERED, DeliveryEventType.DELIVERED, observation, true); }
    @Transactional public SaleOrder markNotDelivered(Long orderId, String observation) { return updateDeliveryStatus(orderId, DeliveryStatus.NOT_DELIVERED, DeliveryEventType.NOT_DELIVERED, observation, false); }
    @Transactional public SaleOrder markRescheduled(Long orderId, String observation) { return updateDeliveryStatus(orderId, DeliveryStatus.RESCHEDULED, DeliveryEventType.RESCHEDULED, observation, false); }
    @Transactional public SaleOrder markCanceled(Long orderId, String observation) { return updateDeliveryStatus(orderId, DeliveryStatus.CANCELED, DeliveryEventType.CANCELED, observation, false); }

    private SaleOrder updateDeliveryStatus(Long orderId, DeliveryStatus deliveryStatus, DeliveryEventType eventType, String observation, boolean setDeliveredAt) {
        SaleOrder order = saleOrderRepository.findById(orderId).orElseThrow(() -> new IllegalArgumentException("Order not found: " + orderId));
        order.setDeliveryStatus(deliveryStatus);
        order.setDeliveryObservation(clean(observation));

        if (setDeliveredAt) {
            order.setDeliveredAt(LocalDateTime.now());
        } else if (deliveryStatus != DeliveryStatus.DELIVERED) {
            order.setDeliveredAt(null);
        }

        saleOrderRepository.save(order);
        registerEvent(order, eventType, observation);
        return order;
    }

    private void registerEvent(SaleOrder order, DeliveryEventType eventType, String observation) {
        SaleOrderDeliveryEvent event = new SaleOrderDeliveryEvent();
        event.setSaleOrderId(order.getId());
        event.setEventDate(LocalDateTime.now());
        event.setEventType(eventType);
        event.setObservation(clean(observation));
        event.setContainersDeliveredSnapshot(0);
        event.setContainersReturnedSnapshot(0);
        event.setDeliveryPersonSnapshot(order.getDeliveryPerson());
        deliveryEventRepository.save(event);
    }

    private String clean(String value) {
        if (value == null) return null;
        String trimmed = value.trim();
        return trimmed.isBlank() ? null : trimmed;
    }

    private boolean safeEqualsIgnoreCase(String left, String right) {
        if (left == null && right == null) return true;
        if (left == null || right == null) return false;
        return left.equalsIgnoreCase(right);
    }
}
