package com.ecoamazonas.eco_agua.container;

import com.ecoamazonas.eco_agua.client.Client;
import com.ecoamazonas.eco_agua.client.ClientRepository;
import com.ecoamazonas.eco_agua.order.OrderStatus;
import com.ecoamazonas.eco_agua.order.SaleOrder;
import com.ecoamazonas.eco_agua.order.SaleOrderRepository;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class ClientContainerService {

    private final ClientContainerMovementRepository movementRepository;
    private final ClientRepository clientRepository;
    private final SaleOrderRepository saleOrderRepository;

    public ClientContainerService(
            ClientContainerMovementRepository movementRepository,
            ClientRepository clientRepository,
            SaleOrderRepository saleOrderRepository
    ) {
        this.movementRepository = movementRepository;
        this.clientRepository = clientRepository;
        this.saleOrderRepository = saleOrderRepository;
    }

    @Transactional(readOnly = true)
    public Map<Long, Integer> getCurrentBalanceMap() {
        List<ClientContainerMovement> allMovements = movementRepository.findAllByOrderByMovementDateAscIdAsc();
        Map<Long, Integer> balances = new LinkedHashMap<>();

        for (ClientContainerMovement movement : allMovements) {
            if (movement.getClientId() == null) {
                continue;
            }

            balances.put(
                    movement.getClientId(),
                    balances.getOrDefault(movement.getClientId(), 0) + movement.getSignedQuantity()
            );
        }

        return balances;
    }

    @Transactional(readOnly = true)
    public List<ClientContainerBalanceRow> buildBalanceRows(Long selectedClientId, boolean includeZero) {
        List<Client> clients = resolveClients(selectedClientId);
        Map<Long, Integer> balanceByClient = new LinkedHashMap<>();
        Map<Long, LocalDate> lastMovementDateByClient = new LinkedHashMap<>();

        for (ClientContainerMovement movement : movementRepository.findAllByOrderByMovementDateAscIdAsc()) {
            Long clientId = movement.getClientId();
            if (clientId == null) {
                continue;
            }

            if (selectedClientId != null && !selectedClientId.equals(clientId)) {
                continue;
            }

            int current = balanceByClient.getOrDefault(clientId, 0);
            int updated = current + movement.getSignedQuantity();
            balanceByClient.put(clientId, updated);

            LocalDate movementDate = movement.getMovementDate();
            LocalDate currentLastDate = lastMovementDateByClient.get(clientId);
            if (currentLastDate == null || (movementDate != null && movementDate.isAfter(currentLastDate))) {
                lastMovementDateByClient.put(clientId, movementDate);
            }
        }

        List<ClientContainerBalanceRow> rows = new ArrayList<>();
        for (Client client : clients) {
            int balance = balanceByClient.getOrDefault(client.getId(), 0);

            if (!includeZero && balance == 0) {
                continue;
            }

            rows.add(new ClientContainerBalanceRow(
                    client.getId(),
                    client.getName(),
                    client.getProfile() != null ? client.getProfile().getName() : null,
                    client.getPhone(),
                    client.getAddress(),
                    balance,
                    lastMovementDateByClient.get(client.getId()),
                    client.isActive()
            ));
        }

        rows.sort(
                Comparator.comparingInt(ClientContainerBalanceRow::getBalance).reversed()
                        .thenComparing(row -> row.getClientName() != null ? row.getClientName().toLowerCase() : "")
        );

        return rows;
    }

    @Transactional(readOnly = true)
    public ClientContainerClientSnapshot getClientSnapshot(Long clientId) {
        Client client = clientRepository.findById(clientId)
                .orElseThrow(() -> new IllegalArgumentException("Client not found: " + clientId));

        List<ClientContainerMovement> movements = movementRepository.findByClientIdOrderByMovementDateDescIdDesc(clientId);
        int balance = computeBalance(movements);

        return new ClientContainerClientSnapshot(client, balance, movements);
    }

    @Transactional
    public ClientContainerMovement registerReturn(
            Long clientId,
            LocalDate movementDate,
            Integer quantity,
            String observation,
            Long saleOrderId,
            boolean allowExcessReturn
    ) {
        validateQuantity(quantity);

        Client client = clientRepository.findById(clientId)
                .orElseThrow(() -> new IllegalArgumentException("Client not found: " + clientId));

        int currentBalance = getCurrentBalance(clientId);
        if (!allowExcessReturn && quantity > currentBalance) {
            throw new IllegalArgumentException(
                    "Return quantity cannot be greater than current balance. Current balance: " + currentBalance
            );
        }

        if (saleOrderId != null) {
            validateRelatedOrder(clientId, saleOrderId);
        }

        ClientContainerMovement movement = new ClientContainerMovement();
        movement.setClientId(client.getId());
        movement.setSaleOrderId(saleOrderId);
        movement.setMovementDate(movementDate != null ? movementDate : LocalDate.now());
        movement.setMovementType(ContainerMovementType.RETURN);
        movement.setQuantity(quantity);
        movement.setObservation(cleanText(observation));

        return movementRepository.save(movement);
    }

    @Transactional
    public ClientContainerMovement registerAdjustment(
            Long clientId,
            LocalDate movementDate,
            ContainerMovementType movementType,
            Integer quantity,
            String observation,
            Long saleOrderId
    ) {
        validateQuantity(quantity);

        if (movementType != ContainerMovementType.ADJUSTMENT_IN
                && movementType != ContainerMovementType.ADJUSTMENT_OUT) {
            throw new IllegalArgumentException("Adjustment movement type is invalid.");
        }

        Client client = clientRepository.findById(clientId)
                .orElseThrow(() -> new IllegalArgumentException("Client not found: " + clientId));

        if (saleOrderId != null) {
            validateRelatedOrder(clientId, saleOrderId);
        }

        ClientContainerMovement movement = new ClientContainerMovement();
        movement.setClientId(client.getId());
        movement.setSaleOrderId(saleOrderId);
        movement.setMovementDate(movementDate != null ? movementDate : LocalDate.now());
        movement.setMovementType(movementType);
        movement.setQuantity(quantity);
        movement.setObservation(cleanText(observation));

        return movementRepository.save(movement);
    }

    @Transactional(readOnly = true)
    public int getCurrentBalance(Long clientId) {
        List<ClientContainerMovement> movements = movementRepository.findByClientIdOrderByMovementDateDescIdDesc(clientId);
        return computeBalance(movements);
    }

    @Transactional
    public boolean syncOrderLoanMovement(SaleOrder order) {
        return syncOrderContainerMovements(order);
    }

    @Transactional
    public boolean syncOrderContainerMovements(SaleOrder order) {
        if (order == null || order.getId() == null || order.getClient() == null) {
            return false;
        }

        List<ClientContainerMovement> orderMovements =
                movementRepository.findBySaleOrderIdOrderByMovementDateAscIdAsc(order.getId());

        int desiredLoan = resolveDesiredLoan(order);
        int desiredReturn = resolveDesiredReturn(order);
        int currentLoan = sumMovementQuantity(orderMovements, ContainerMovementType.LOAN);
        int currentReturn = sumMovementQuantity(orderMovements, ContainerMovementType.RETURN);

        boolean changed = false;
        changed |= syncLoanDelta(order, desiredLoan - currentLoan);
        changed |= syncReturnDelta(order, desiredReturn - currentReturn);

        return changed;
    }

    @Transactional
    public int syncLoansFromOrders(LocalDate startDate, LocalDate endDate) {
        List<SaleOrder> orders = resolveOrders(startDate, endDate);
        int changes = 0;

        for (SaleOrder order : orders) {
            if (syncOrderContainerMovements(order)) {
                changes++;
            }
        }

        return changes;
    }

    private boolean syncLoanDelta(SaleOrder order, int delta) {
        if (delta == 0) {
            return false;
        }

        ClientContainerMovement movement = buildBaseSyncMovement(order);

        if (delta > 0) {
            movement.setMovementType(ContainerMovementType.LOAN);
            movement.setQuantity(delta);
            movement.setObservation("Automatic delivered-container sync from order #" + order.getOrderNumber());
        } else {
            movement.setMovementType(ContainerMovementType.ADJUSTMENT_IN);
            movement.setQuantity(Math.abs(delta));
            movement.setObservation("Automatic loan reduction sync from order #" + order.getOrderNumber());
        }

        movementRepository.save(movement);
        return true;
    }

    private boolean syncReturnDelta(SaleOrder order, int delta) {
        if (delta == 0) {
            return false;
        }

        ClientContainerMovement movement = buildBaseSyncMovement(order);

        if (delta > 0) {
            movement.setMovementType(ContainerMovementType.RETURN);
            movement.setQuantity(delta);
            movement.setObservation("Automatic returned-container sync from order #" + order.getOrderNumber());
        } else {
            movement.setMovementType(ContainerMovementType.ADJUSTMENT_OUT);
            movement.setQuantity(Math.abs(delta));
            movement.setObservation("Automatic return reduction sync from order #" + order.getOrderNumber());
        }

        movementRepository.save(movement);
        return true;
    }

    private ClientContainerMovement buildBaseSyncMovement(SaleOrder order) {
        ClientContainerMovement movement = new ClientContainerMovement();
        movement.setClientId(order.getClient().getId());
        movement.setSaleOrderId(order.getId());
        movement.setMovementDate(order.getOrderDate() != null ? order.getOrderDate() : LocalDate.now());
        return movement;
    }

    private List<Client> resolveClients(Long selectedClientId) {
        if (selectedClientId != null) {
            Optional<Client> client = clientRepository.findById(selectedClientId);
            return client.map(List::of).orElseGet(List::of);
        }

        List<Client> clients = clientRepository.findAll();
        clients.sort(Comparator.comparing(client -> client.getName() != null ? client.getName().toLowerCase() : ""));
        return clients;
    }

    private List<SaleOrder> resolveOrders(LocalDate startDate, LocalDate endDate) {
        List<SaleOrder> orders;

        if (startDate == null && endDate == null) {
            orders = saleOrderRepository.findAll(Sort.by(Sort.Direction.ASC, "orderDate", "id"));
        } else {
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

            orders = saleOrderRepository.findByOrderDateBetween(startDate, endDate);
            orders.sort(Comparator.comparing(SaleOrder::getOrderDate).thenComparing(SaleOrder::getId));
        }

        return orders;
    }

    private void validateRelatedOrder(Long clientId, Long saleOrderId) {
        SaleOrder order = saleOrderRepository.findById(saleOrderId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found: " + saleOrderId));

        if (order.getClient() == null || !clientId.equals(order.getClient().getId())) {
            throw new IllegalArgumentException("The selected order does not belong to the same client.");
        }
    }

    private int resolveDesiredLoan(SaleOrder order) {
        if (order.getStatus() == OrderStatus.CANCELED) {
            return 0;
        }

        Integer delivered = order.getContainersDelivered();
        if (delivered != null && delivered > 0) {
            return delivered;
        }

        Integer borrowedBottles = order.getBorrowedBottles();
        return borrowedBottles != null && borrowedBottles > 0 ? borrowedBottles : 0;
    }

    private int resolveDesiredReturn(SaleOrder order) {
        if (order.getStatus() == OrderStatus.CANCELED) {
            return 0;
        }

        Integer returned = order.getContainersReturned();
        return returned != null && returned > 0 ? returned : 0;
    }

    private int sumMovementQuantity(List<ClientContainerMovement> movements, ContainerMovementType type) {
        int total = 0;

        for (ClientContainerMovement movement : movements) {
            if (movement.getMovementType() == type && movement.getQuantity() != null) {
                total += movement.getQuantity();
            }
        }

        return total;
    }

    private int computeBalance(List<ClientContainerMovement> movements) {
        int balance = 0;

        for (ClientContainerMovement movement : movements) {
            balance += movement.getSignedQuantity();
        }

        return balance;
    }

    private void validateQuantity(Integer quantity) {
        if (quantity == null || quantity <= 0) {
            throw new IllegalArgumentException("Quantity must be greater than zero.");
        }
    }

    private String cleanText(String value) {
        if (value == null) {
            return null;
        }

        String trimmed = value.trim();
        return trimmed.isBlank() ? null : trimmed;
    }
}
