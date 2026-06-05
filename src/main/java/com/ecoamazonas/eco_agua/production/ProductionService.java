package com.ecoamazonas.eco_agua.production;

import com.ecoamazonas.eco_agua.inventory.InventoryMovementType;
import com.ecoamazonas.eco_agua.inventory.InventoryService;
import com.ecoamazonas.eco_agua.product.Product;
import com.ecoamazonas.eco_agua.product.ProductRepository;
import com.ecoamazonas.eco_agua.product.ProductSupply;
import com.ecoamazonas.eco_agua.supply.Supply;
import com.ecoamazonas.eco_agua.supply.SupplyRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class ProductionService {

    private final ProductionOrderRepository productionOrderRepository;
    private final ProductRepository productRepository;
    private final SupplyRepository supplyRepository;
    private final InventoryService inventoryService;

    public ProductionService(
            ProductionOrderRepository productionOrderRepository,
            ProductRepository productRepository,
            SupplyRepository supplyRepository,
            InventoryService inventoryService
    ) {
        this.productionOrderRepository = productionOrderRepository;
        this.productRepository = productRepository;
        this.supplyRepository = supplyRepository;
        this.inventoryService = inventoryService;
    }

    @Transactional(readOnly = true)
    public List<ProductionOrder> findByDateRange(LocalDate startDate, LocalDate endDate, ProductionStatus status) {
        LocalDate effectiveEnd = endDate != null ? endDate : LocalDate.now();
        LocalDate effectiveStart = startDate != null ? startDate : effectiveEnd.minusDays(30);

        if (effectiveEnd.isBefore(effectiveStart)) {
            LocalDate tmp = effectiveStart;
            effectiveStart = effectiveEnd;
            effectiveEnd = tmp;
        }

        return productionOrderRepository.findByDateRangeAndStatus(effectiveStart, effectiveEnd, status);
    }

    @Transactional(readOnly = true)
    public ProductionOverviewSnapshot buildOverview(LocalDate startDate, LocalDate endDate) {
        LocalDate effectiveEnd = endDate != null ? endDate : LocalDate.now();
        LocalDate effectiveStart = startDate != null ? startDate : effectiveEnd.minusDays(30);

        if (effectiveEnd.isBefore(effectiveStart)) {
            LocalDate tmp = effectiveStart;
            effectiveStart = effectiveEnd;
            effectiveEnd = tmp;
        }

        List<ProductionOrder> orders = productionOrderRepository.findByDateRangeAndStatus(effectiveStart, effectiveEnd, null);

        long confirmedOrders = 0;
        long draftOrders = 0;
        long canceledOrders = 0;
        BigDecimal confirmedQuantityExpected = BigDecimal.ZERO;
        BigDecimal confirmedQuantityProduced = BigDecimal.ZERO;
        BigDecimal confirmedQuantityLoss = BigDecimal.ZERO;
        BigDecimal confirmedInputCost = BigDecimal.ZERO;
        Map<Long, ProductionOverviewProductRow> products = new LinkedHashMap<>();
        List<ProductionOrder> pendingDrafts = new ArrayList<>();

        for (ProductionOrder order : orders) {
            if (order.getStatus() == ProductionStatus.CONFIRMED) {
                confirmedOrders++;
                confirmedQuantityExpected = confirmedQuantityExpected.add(valueOrZero(order.getQuantityExpected()));
                confirmedQuantityProduced = confirmedQuantityProduced.add(valueOrZero(order.getQuantityProduced()));
                confirmedQuantityLoss = confirmedQuantityLoss.add(valueOrZero(order.getQuantityLoss()));
                confirmedInputCost = confirmedInputCost.add(valueOrZero(order.getTotalInputCost()));

                Long productId = order.getProductId();
                ProductionOverviewProductRow row = products.computeIfAbsent(
                        productId,
                        id -> new ProductionOverviewProductRow(id, resolveProductName(order))
                );
                row.addOrder(order);
            } else if (order.getStatus() == ProductionStatus.DRAFT) {
                draftOrders++;
                if (pendingDrafts.size() < 8) {
                    pendingDrafts.add(order);
                }
            } else if (order.getStatus() == ProductionStatus.CANCELED) {
                canceledOrders++;
            }
        }

        BigDecimal averageUnitCost = BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP);
        if (confirmedQuantityProduced.compareTo(BigDecimal.ZERO) > 0) {
            averageUnitCost = confirmedInputCost.divide(confirmedQuantityProduced, 4, RoundingMode.HALF_UP);
        }

        ProductionOverviewSummary summary = new ProductionOverviewSummary(
                effectiveStart,
                effectiveEnd,
                orders.size(),
                confirmedOrders,
                draftOrders,
                canceledOrders,
                confirmedQuantityExpected.setScale(2, RoundingMode.HALF_UP),
                confirmedQuantityProduced.setScale(2, RoundingMode.HALF_UP),
                confirmedQuantityLoss.setScale(2, RoundingMode.HALF_UP),
                confirmedInputCost.setScale(2, RoundingMode.HALF_UP),
                averageUnitCost
        );

        List<ProductionOrder> latestOrders = orders.stream()
                .limit(8)
                .toList();

        return new ProductionOverviewSnapshot(
                summary,
                new ArrayList<>(products.values()),
                latestOrders,
                pendingDrafts
        );
    }


    @Transactional(readOnly = true)
    public ProductionReportSnapshot buildReport(LocalDate startDate, LocalDate endDate) {
        LocalDate effectiveEnd = endDate != null ? endDate : LocalDate.now();
        LocalDate effectiveStart = startDate != null ? startDate : effectiveEnd.minusDays(30);

        if (effectiveEnd.isBefore(effectiveStart)) {
            LocalDate tmp = effectiveStart;
            effectiveStart = effectiveEnd;
            effectiveEnd = tmp;
        }

        List<ProductionOrder> orders = productionOrderRepository.findByDateRangeAndStatus(effectiveStart, effectiveEnd, null);

        long confirmedOrders = 0;
        long draftOrders = 0;
        long canceledOrders = 0;
        BigDecimal confirmedQuantityExpected = BigDecimal.ZERO;
        BigDecimal confirmedQuantityProduced = BigDecimal.ZERO;
        BigDecimal confirmedQuantityLoss = BigDecimal.ZERO;
        BigDecimal confirmedInputCost = BigDecimal.ZERO;
        Map<Long, ProductionReportProductRow> products = new LinkedHashMap<>();
        List<ProductionOrder> latestConfirmedOrders = new ArrayList<>();
        List<ProductionOrder> latestCanceledOrders = new ArrayList<>();

        for (ProductionOrder order : orders) {
            if (order.getStatus() == ProductionStatus.CONFIRMED) {
                confirmedOrders++;
                confirmedQuantityExpected = confirmedQuantityExpected.add(valueOrZero(order.getQuantityExpected()));
                confirmedQuantityProduced = confirmedQuantityProduced.add(valueOrZero(order.getQuantityProduced()));
                confirmedQuantityLoss = confirmedQuantityLoss.add(valueOrZero(order.getQuantityLoss()));
                confirmedInputCost = confirmedInputCost.add(valueOrZero(order.getTotalInputCost()));

                Long productId = order.getProductId();
                ProductionReportProductRow row = products.computeIfAbsent(
                        productId,
                        id -> new ProductionReportProductRow(id, resolveProductName(order))
                );
                row.addOrder(order);

                if (latestConfirmedOrders.size() < 20) {
                    latestConfirmedOrders.add(order);
                }
            } else if (order.getStatus() == ProductionStatus.DRAFT) {
                draftOrders++;
            } else if (order.getStatus() == ProductionStatus.CANCELED) {
                canceledOrders++;
                if (latestCanceledOrders.size() < 10) {
                    latestCanceledOrders.add(order);
                }
            }
        }

        BigDecimal averageRealUnitCost = BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP);
        if (confirmedQuantityProduced.compareTo(BigDecimal.ZERO) > 0) {
            averageRealUnitCost = confirmedInputCost.divide(confirmedQuantityProduced, 4, RoundingMode.HALF_UP);
        }

        ProductionReportSummary summary = new ProductionReportSummary(
                effectiveStart,
                effectiveEnd,
                orders.size(),
                confirmedOrders,
                draftOrders,
                canceledOrders,
                confirmedQuantityExpected.setScale(2, RoundingMode.HALF_UP),
                confirmedQuantityProduced.setScale(2, RoundingMode.HALF_UP),
                confirmedQuantityLoss.setScale(2, RoundingMode.HALF_UP),
                confirmedInputCost.setScale(2, RoundingMode.HALF_UP),
                averageRealUnitCost
        );

        List<ProductionReportProductRow> productRows = new ArrayList<>(products.values());
        productRows.sort(Comparator
                .comparing(ProductionReportProductRow::getQuantityProduced, Comparator.nullsLast(Comparator.naturalOrder()))
                .reversed()
                .thenComparing(ProductionReportProductRow::getProductName, Comparator.nullsLast(String::compareToIgnoreCase)));

        List<ProductionReportProductRow> highWasteProductRows = productRows.stream()
                .filter(row -> row.getQuantityLoss().compareTo(BigDecimal.ZERO) > 0)
                .sorted((left, right) -> {
                    int byQuantityLoss = right.getQuantityLoss().compareTo(left.getQuantityLoss());
                    if (byQuantityLoss != 0) {
                        return byQuantityLoss;
                    }
                    return right.getLossRatePercent().compareTo(left.getLossRatePercent());
                })
                .limit(8)
                .toList();

        return new ProductionReportSnapshot(
                summary,
                productRows,
                highWasteProductRows,
                latestConfirmedOrders,
                latestCanceledOrders
        );
    }





    @Transactional(readOnly = true)
    public ProductionTraceabilitySnapshot buildTraceability(
            LocalDate startDate,
            LocalDate endDate,
            Long productId,
            String batchCode,
            ProductionStatus selectedStatus,
            ProductionQualityStatus selectedQualityStatus
    ) {
        LocalDate effectiveEnd = endDate != null ? endDate : LocalDate.now();
        LocalDate effectiveStart = startDate != null ? startDate : effectiveEnd.minusDays(30);

        if (effectiveEnd.isBefore(effectiveStart)) {
            LocalDate tmp = effectiveStart;
            effectiveStart = effectiveEnd;
            effectiveEnd = tmp;
        }

        String normalizedBatch = clean(batchCode);
        String normalizedBatchLower = normalizedBatch != null ? normalizedBatch.toLowerCase() : null;

        List<ProductionOrder> sourceRows = productionOrderRepository.findByDateRangeAndStatus(
                effectiveStart,
                effectiveEnd,
                selectedStatus
        );

        List<ProductionOrder> filteredRows = new ArrayList<>();
        List<ProductionOrder> qualityAttentionRows = new ArrayList<>();
        List<ProductionOrder> latestConfirmedRows = new ArrayList<>();

        long confirmedOrders = 0;
        long draftOrders = 0;
        long canceledOrders = 0;
        long approvedOrders = 0;
        long pendingQualityOrders = 0;
        long attentionQualityOrders = 0;
        BigDecimal quantityExpected = BigDecimal.ZERO;
        BigDecimal quantityProduced = BigDecimal.ZERO;
        BigDecimal quantityLoss = BigDecimal.ZERO;

        for (ProductionOrder order : sourceRows) {
            if (productId != null && !productId.equals(order.getProductId())) {
                continue;
            }

            if (normalizedBatchLower != null) {
                String orderBatch = order.getBatchCode() != null ? order.getBatchCode().toLowerCase() : "";
                if (!orderBatch.contains(normalizedBatchLower)) {
                    continue;
                }
            }

            ProductionQualityStatus qualityStatus = order.getQualityStatus();
            if (selectedQualityStatus != null && selectedQualityStatus != qualityStatus) {
                continue;
            }

            filteredRows.add(order);

            if (order.getStatus() == ProductionStatus.CONFIRMED) {
                confirmedOrders++;
                quantityExpected = quantityExpected.add(valueOrZero(order.getQuantityExpected()));
                quantityProduced = quantityProduced.add(valueOrZero(order.getQuantityProduced()));
                quantityLoss = quantityLoss.add(valueOrZero(order.getQuantityLoss()));

                if (latestConfirmedRows.size() < 8) {
                    latestConfirmedRows.add(order);
                }
            } else if (order.getStatus() == ProductionStatus.DRAFT) {
                draftOrders++;
            } else if (order.getStatus() == ProductionStatus.CANCELED) {
                canceledOrders++;
            }

            if (qualityStatus == ProductionQualityStatus.APPROVED) {
                approvedOrders++;
            } else if (qualityStatus == ProductionQualityStatus.OBSERVED || qualityStatus == ProductionQualityStatus.REJECTED) {
                attentionQualityOrders++;
                if (qualityAttentionRows.size() < 8) {
                    qualityAttentionRows.add(order);
                }
            } else {
                pendingQualityOrders++;
            }
        }

        BigDecimal lossRatePercent = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        if (quantityExpected.compareTo(BigDecimal.ZERO) > 0) {
            lossRatePercent = quantityLoss.multiply(BigDecimal.valueOf(100))
                    .divide(quantityExpected, 2, RoundingMode.HALF_UP);
        }

        ProductionTraceabilitySummary summary = new ProductionTraceabilitySummary(
                effectiveStart,
                effectiveEnd,
                filteredRows.size(),
                confirmedOrders,
                draftOrders,
                canceledOrders,
                approvedOrders,
                pendingQualityOrders,
                attentionQualityOrders,
                quantityExpected.setScale(2, RoundingMode.HALF_UP),
                quantityProduced.setScale(2, RoundingMode.HALF_UP),
                quantityLoss.setScale(2, RoundingMode.HALF_UP),
                lossRatePercent
        );

        return new ProductionTraceabilitySnapshot(
                summary,
                filteredRows,
                qualityAttentionRows,
                latestConfirmedRows
        );
    }

    @Transactional(readOnly = true)
    public ProductionExpirySnapshot buildExpiryDashboard(
            LocalDate startDate,
            LocalDate endDate,
            Long productId,
            String batchCode,
            ProductionStatus selectedStatus,
            ProductionExpiryStatus selectedExpiryStatus
    ) {
        LocalDate effectiveEnd = endDate != null ? endDate : LocalDate.now();
        LocalDate effectiveStart = startDate != null ? startDate : effectiveEnd.minusDays(30);

        if (effectiveEnd.isBefore(effectiveStart)) {
            LocalDate tmp = effectiveStart;
            effectiveStart = effectiveEnd;
            effectiveEnd = tmp;
        }

        String normalizedBatch = clean(batchCode);
        String normalizedBatchLower = normalizedBatch != null ? normalizedBatch.toLowerCase() : null;
        LocalDate today = LocalDate.now();

        List<ProductionOrder> sourceRows = productionOrderRepository.findByDateRangeAndStatus(
                effectiveStart,
                effectiveEnd,
                selectedStatus
        );

        List<ProductionOrder> filteredRows = new ArrayList<>();
        List<ProductionOrder> expiredRows = new ArrayList<>();
        List<ProductionOrder> expiringSoonRows = new ArrayList<>();
        List<ProductionOrder> noDateRows = new ArrayList<>();

        long withExpiryDate = 0;
        long noExpiryDate = 0;
        long validOrders = 0;
        long expiringSoonOrders = 0;
        long expiredOrders = 0;

        for (ProductionOrder order : sourceRows) {
            if (productId != null && !productId.equals(order.getProductId())) {
                continue;
            }

            if (normalizedBatchLower != null) {
                String orderBatch = order.getBatchCode() != null ? order.getBatchCode().toLowerCase() : "";
                if (!orderBatch.contains(normalizedBatchLower)) {
                    continue;
                }
            }

            ProductionExpiryStatus expiryStatus = ProductionExpiryStatus.fromDate(order.getExpiryDate(), today);
            if (selectedExpiryStatus != null && selectedExpiryStatus != expiryStatus) {
                continue;
            }

            filteredRows.add(order);

            if (order.getExpiryDate() == null) {
                noExpiryDate++;
                if (noDateRows.size() < 8) {
                    noDateRows.add(order);
                }
            } else {
                withExpiryDate++;
            }

            if (expiryStatus == ProductionExpiryStatus.EXPIRED) {
                expiredOrders++;
                if (expiredRows.size() < 8) {
                    expiredRows.add(order);
                }
            } else if (expiryStatus == ProductionExpiryStatus.EXPIRING_SOON) {
                expiringSoonOrders++;
                if (expiringSoonRows.size() < 8) {
                    expiringSoonRows.add(order);
                }
            } else if (expiryStatus == ProductionExpiryStatus.VALID) {
                validOrders++;
            }
        }

        ProductionExpirySummary summary = new ProductionExpirySummary(
                effectiveStart,
                effectiveEnd,
                filteredRows.size(),
                withExpiryDate,
                noExpiryDate,
                validOrders,
                expiringSoonOrders,
                expiredOrders
        );

        return new ProductionExpirySnapshot(
                summary,
                filteredRows,
                expiredRows,
                expiringSoonRows,
                noDateRows
        );
    }

    @Transactional(readOnly = true)
    public ProductionQualitySnapshot buildQualityDashboard(
            LocalDate startDate,
            LocalDate endDate,
            ProductionQualityStatus selectedQualityStatus
    ) {
        LocalDate effectiveEnd = endDate != null ? endDate : LocalDate.now();
        LocalDate effectiveStart = startDate != null ? startDate : effectiveEnd.minusDays(30);

        if (effectiveEnd.isBefore(effectiveStart)) {
            LocalDate tmp = effectiveStart;
            effectiveStart = effectiveEnd;
            effectiveEnd = tmp;
        }

        List<ProductionOrder> orders = productionOrderRepository.findByDateRangeAndStatus(effectiveStart, effectiveEnd, null);

        long pendingOrders = 0;
        long approvedOrders = 0;
        long observedOrders = 0;
        long rejectedOrders = 0;
        List<ProductionOrder> filteredRows = new ArrayList<>();
        List<ProductionOrder> pendingRows = new ArrayList<>();
        List<ProductionOrder> attentionRows = new ArrayList<>();

        for (ProductionOrder order : orders) {
            ProductionQualityStatus qualityStatus = order.getQualityStatus();

            if (qualityStatus == ProductionQualityStatus.APPROVED) {
                approvedOrders++;
            } else if (qualityStatus == ProductionQualityStatus.OBSERVED) {
                observedOrders++;
            } else if (qualityStatus == ProductionQualityStatus.REJECTED) {
                rejectedOrders++;
            } else {
                pendingOrders++;
            }

            if (selectedQualityStatus == null || selectedQualityStatus == qualityStatus) {
                filteredRows.add(order);
            }

            if (qualityStatus == ProductionQualityStatus.PENDING && pendingRows.size() < 8) {
                pendingRows.add(order);
            }

            if ((qualityStatus == ProductionQualityStatus.OBSERVED || qualityStatus == ProductionQualityStatus.REJECTED)
                    && attentionRows.size() < 8) {
                attentionRows.add(order);
            }
        }

        ProductionQualitySummary summary = new ProductionQualitySummary(
                effectiveStart,
                effectiveEnd,
                orders.size(),
                pendingOrders,
                approvedOrders,
                observedOrders,
                rejectedOrders
        );

        return new ProductionQualitySnapshot(summary, filteredRows, pendingRows, attentionRows);
    }


    @Transactional(readOnly = true)
    public ProductionPlanningSnapshot buildPlanning(Long productId, BigDecimal quantity) {
        BigDecimal plannedQuantity = normalizePositiveQuantity(quantity);

        ProductionPlanningSummary emptySummary = new ProductionPlanningSummary(
                null,
                null,
                plannedQuantity,
                0,
                0,
                0,
                BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP),
                true
        );

        if (productId == null) {
            return new ProductionPlanningSnapshot(emptySummary, List.of());
        }

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("Product not found: " + productId));

        List<ProductionPlanningRequirementRow> rows = new ArrayList<>();
        BigDecimal totalEstimatedCost = BigDecimal.ZERO;
        int linesWithEnoughStock = 0;
        int linesWithShortage = 0;

        for (ProductSupply composition : product.getSuppliesComposition()) {
            if (composition == null || composition.getSupply() == null) {
                continue;
            }

            Supply supply = composition.getSupply();
            BigDecimal quantityPerUnit = composition.getQuantityUsed() != null
                    ? composition.getQuantityUsed()
                    : BigDecimal.ZERO;

            if (quantityPerUnit.compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }

            BigDecimal requiredQuantity = quantityPerUnit.multiply(plannedQuantity)
                    .setScale(4, RoundingMode.HALF_UP);
            BigDecimal availableQuantity = supply.getStock() != null
                    ? supply.getStock()
                    : BigDecimal.ZERO;
            BigDecimal shortageQuantity = requiredQuantity.subtract(availableQuantity);

            if (shortageQuantity.compareTo(BigDecimal.ZERO) < 0) {
                shortageQuantity = BigDecimal.ZERO;
            }

            boolean enoughStock = shortageQuantity.compareTo(BigDecimal.ZERO) == 0;
            if (enoughStock) {
                linesWithEnoughStock++;
            } else {
                linesWithShortage++;
            }

            BigDecimal unitCost = supply.getUnitCost() != null
                    ? supply.getUnitCost()
                    : BigDecimal.ZERO.setScale(6, RoundingMode.HALF_UP);
            BigDecimal requiredCost = requiredQuantity.multiply(unitCost)
                    .setScale(2, RoundingMode.HALF_UP);
            totalEstimatedCost = totalEstimatedCost.add(requiredCost);

            rows.add(new ProductionPlanningRequirementRow(
                    supply.getId(),
                    supply.getName(),
                    supply.getUnit(),
                    quantityPerUnit.setScale(4, RoundingMode.HALF_UP),
                    requiredQuantity.setScale(4, RoundingMode.HALF_UP),
                    availableQuantity.setScale(4, RoundingMode.HALF_UP),
                    shortageQuantity.setScale(4, RoundingMode.HALF_UP),
                    unitCost,
                    requiredCost,
                    enoughStock
            ));
        }

        rows.sort(Comparator
                .comparing(ProductionPlanningRequirementRow::isEnoughStock)
                .thenComparing(ProductionPlanningRequirementRow::getSupplyName, Comparator.nullsLast(String::compareToIgnoreCase)));

        ProductionPlanningSummary summary = new ProductionPlanningSummary(
                product.getId(),
                product.getName(),
                plannedQuantity,
                rows.size(),
                linesWithEnoughStock,
                linesWithShortage,
                totalEstimatedCost.setScale(2, RoundingMode.HALF_UP),
                linesWithShortage == 0
        );

        return new ProductionPlanningSnapshot(summary, rows);
    }

    @Transactional(readOnly = true)
    public ProductionOrder findDetailedById(Long id) {
        return productionOrderRepository.findDetailedById(id)
                .orElseThrow(() -> new IllegalArgumentException("Production order not found: " + id));
    }

    @Transactional(readOnly = true)
    public List<Product> findActiveProducts() {
        return productRepository.findByActiveTrueOrderByNameAsc();
    }

    @Transactional(readOnly = true)
    public List<ProductionRecipeLine> buildRecipeLines(Long productId, BigDecimal quantityProduced) {
        if (productId == null) {
            throw new IllegalArgumentException("Product is required.");
        }

        BigDecimal effectiveQuantity = normalizePositiveQuantity(quantityProduced);
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("Product not found: " + productId));

        List<ProductionRecipeLine> rows = new ArrayList<>();

        for (ProductSupply composition : product.getSuppliesComposition()) {
            if (composition == null || composition.getSupply() == null) {
                continue;
            }

            Supply supply = composition.getSupply();
            BigDecimal recipeQuantity = composition.getQuantityUsed() != null
                    ? composition.getQuantityUsed()
                    : BigDecimal.ZERO;

            BigDecimal calculatedQuantity = recipeQuantity.multiply(effectiveQuantity)
                    .setScale(4, RoundingMode.HALF_UP);

            BigDecimal unitCost = supply.getUnitCost() != null
                    ? supply.getUnitCost()
                    : BigDecimal.ZERO.setScale(6, RoundingMode.HALF_UP);

            BigDecimal lineTotal = calculatedQuantity.multiply(unitCost)
                    .setScale(2, RoundingMode.HALF_UP);

            rows.add(new ProductionRecipeLine(
                    supply.getId(),
                    supply.getName(),
                    supply.getUnit(),
                    recipeQuantity.setScale(4, RoundingMode.HALF_UP),
                    calculatedQuantity,
                    unitCost,
                    lineTotal
            ));
        }

        return rows;
    }

    @Transactional
    public ProductionOrder createDraft(
            LocalDate productionDate,
            Long productId,
            BigDecimal quantityProduced,
            String observation,
            List<Long> supplyIds,
            List<BigDecimal> quantitiesUsed
    ) {
        return createDraft(
                productionDate,
                productId,
                null,
                quantityProduced,
                null,
                null,
                null,
                observation,
                null,
                supplyIds,
                quantitiesUsed
        );
    }

    @Transactional
    public ProductionOrder createDraft(
            LocalDate productionDate,
            Long productId,
            BigDecimal quantityExpected,
            BigDecimal quantityProduced,
            String batchCode,
            LocalDate expiryDate,
            String expiryObservation,
            String observation,
            String lossReason,
            List<Long> supplyIds,
            List<BigDecimal> quantitiesUsed
    ) {
        if (productId == null) {
            throw new IllegalArgumentException("Product is required.");
        }

        if (quantityProduced == null || quantityProduced.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Produced quantity must be greater than zero.");
        }

        BigDecimal effectiveProducedQuantity = quantityProduced.setScale(2, RoundingMode.HALF_UP);
        BigDecimal effectiveExpectedQuantity = quantityExpected != null && quantityExpected.compareTo(BigDecimal.ZERO) > 0
                ? quantityExpected.setScale(2, RoundingMode.HALF_UP)
                : effectiveProducedQuantity;

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("Product not found: " + productId));

        ProductionOrder order = new ProductionOrder();
        order.setProductionDate(productionDate != null ? productionDate : LocalDate.now());
        order.setBatchCode(clean(batchCode));
        order.setExpiryDate(expiryDate);
        order.setExpiryObservation(clean(expiryObservation));
        order.setProduct(product);
        order.setQuantityExpected(effectiveExpectedQuantity);
        order.setQuantityProduced(effectiveProducedQuantity);
        order.setStatus(ProductionStatus.DRAFT);
        order.setObservation(clean(observation));
        order.setLossReason(clean(lossReason));

        BigDecimal totalInputCost = BigDecimal.ZERO;
        List<ProductionOrderSupply> lines = buildManualLines(supplyIds, quantitiesUsed);

        if (lines.isEmpty()) {
            lines = buildLinesFromRecipe(product, effectiveExpectedQuantity);
        }

        if (lines.isEmpty()) {
            throw new IllegalArgumentException("Production must contain at least one supply line.");
        }

        for (ProductionOrderSupply line : lines) {
            totalInputCost = totalInputCost.add(line.getLineTotal());
            order.addSupplyLine(line);
        }

        order.setTotalInputCost(totalInputCost.setScale(2, RoundingMode.HALF_UP));
        order.refreshBatchCostFields();

        return productionOrderRepository.save(order);
    }



    @Transactional
    public ProductionOrder updateQualityControl(
            Long productionOrderId,
            ProductionQualityStatus qualityStatus,
            boolean qualityCleaningOk,
            boolean qualityPackagingOk,
            boolean qualityLabelingOk,
            boolean qualityProductOk,
            String qualityCheckedBy,
            String qualityObservation
    ) {
        ProductionOrder order = findDetailedById(productionOrderId);

        order.setQualityStatus(qualityStatus != null ? qualityStatus : ProductionQualityStatus.PENDING);
        order.setQualityCleaningOk(qualityCleaningOk);
        order.setQualityPackagingOk(qualityPackagingOk);
        order.setQualityLabelingOk(qualityLabelingOk);
        order.setQualityProductOk(qualityProductOk);
        order.setQualityCheckedBy(clean(qualityCheckedBy));
        order.setQualityObservation(clean(qualityObservation));
        order.setQualityCheckedAt(LocalDateTime.now());

        return productionOrderRepository.save(order);
    }

    @Transactional
    public ProductionOrder confirm(Long productionOrderId) {
        ProductionOrder order = findDetailedById(productionOrderId);

        if (order.getStatus() == ProductionStatus.CONFIRMED) {
            return order;
        }

        if (order.getStatus() == ProductionStatus.CANCELED) {
            throw new IllegalArgumentException("Canceled production cannot be confirmed.");
        }

        if (order.getSupplies() == null || order.getSupplies().isEmpty()) {
            throw new IllegalArgumentException("Production must contain at least one supply line.");
        }

        order.refreshBatchCostFields();

        for (ProductionOrderSupply line : order.getSupplies()) {
            Supply supply = line.getSupply();
            if (supply == null) {
                supply = supplyRepository.findById(line.getSupplyId())
                        .orElseThrow(() -> new IllegalArgumentException("Supply not found: " + line.getSupplyId()));
                line.setSupply(supply);
            }

            BigDecimal available = supply.getStock() != null ? supply.getStock() : BigDecimal.ZERO;
            BigDecimal required = line.getQuantityUsed() != null ? line.getQuantityUsed() : BigDecimal.ZERO;

            if (available.compareTo(required) < 0) {
                throw new IllegalArgumentException(
                        "Insufficient supply stock for: " + supply.getName()
                                + " (available: " + available + ", required: " + required + ")"
                );
            }
        }

        for (ProductionOrderSupply line : order.getSupplies()) {
            inventoryService.registerSupplyMovement(
                    line.getSupplyId(),
                    BigDecimal.ZERO,
                    line.getQuantityUsed(),
                    InventoryMovementType.PRODUCTION,
                    "PRODUCTION_ORDER",
                    order.getId(),
                    "Supply consumption for production #" + order.getId(),
                    order.getProductionDate()
            );
        }

        inventoryService.registerProductMovement(
                order.getProductId(),
                order.getQuantityProduced(),
                BigDecimal.ZERO,
                InventoryMovementType.PRODUCTION,
                "PRODUCTION_ORDER",
                order.getId(),
                "Finished product from production #" + order.getId(),
                order.getProductionDate()
        );

        order.setStatus(ProductionStatus.CONFIRMED);
        return productionOrderRepository.save(order);
    }

    @Transactional
    public ProductionOrder cancel(Long productionOrderId) {
        ProductionOrder order = findDetailedById(productionOrderId);

        if (order.getStatus() == ProductionStatus.CANCELED) {
            return order;
        }

        if (order.getStatus() == ProductionStatus.CONFIRMED) {
            for (ProductionOrderSupply line : order.getSupplies()) {
                inventoryService.registerSupplyMovement(
                        line.getSupplyId(),
                        line.getQuantityUsed(),
                        BigDecimal.ZERO,
                        InventoryMovementType.ADJUSTMENT,
                        "PRODUCTION_ORDER",
                        order.getId(),
                        "Reverse supply consumption from canceled production #" + order.getId(),
                        order.getProductionDate()
                );
            }

            inventoryService.registerProductMovement(
                    order.getProductId(),
                    BigDecimal.ZERO,
                    order.getQuantityProduced(),
                    InventoryMovementType.ADJUSTMENT,
                    "PRODUCTION_ORDER",
                    order.getId(),
                    "Reverse finished product from canceled production #" + order.getId(),
                    order.getProductionDate()
            );
        }

        order.setStatus(ProductionStatus.CANCELED);
        return productionOrderRepository.save(order);
    }

    @Transactional
    public void deleteDraft(Long productionOrderId) {
        ProductionOrder order = findDetailedById(productionOrderId);

        if (order.getStatus() != ProductionStatus.DRAFT) {
            throw new IllegalArgumentException("Only draft production orders can be deleted.");
        }

        productionOrderRepository.delete(order);
    }

    private List<ProductionOrderSupply> buildManualLines(List<Long> supplyIds, List<BigDecimal> quantitiesUsed) {
        List<ProductionOrderSupply> lines = new ArrayList<>();

        if (supplyIds == null || quantitiesUsed == null) {
            return lines;
        }

        int max = Math.min(supplyIds.size(), quantitiesUsed.size());

        for (int i = 0; i < max; i++) {
            Long supplyId = supplyIds.get(i);
            BigDecimal quantityUsed = quantitiesUsed.get(i);

            if (supplyId == null || quantityUsed == null || quantityUsed.compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }

            Supply supply = supplyRepository.findById(supplyId)
                    .orElseThrow(() -> new IllegalArgumentException("Supply not found: " + supplyId));

            lines.add(buildSupplyLine(supply, quantityUsed));
        }

        return lines;
    }

    private List<ProductionOrderSupply> buildLinesFromRecipe(Product product, BigDecimal quantityExpected) {
        List<ProductionOrderSupply> lines = new ArrayList<>();
        BigDecimal effectiveQuantity = normalizePositiveQuantity(quantityExpected);

        for (ProductSupply composition : product.getSuppliesComposition()) {
            if (composition == null || composition.getSupply() == null) {
                continue;
            }

            BigDecimal recipeQuantity = composition.getQuantityUsed() != null
                    ? composition.getQuantityUsed()
                    : BigDecimal.ZERO;

            BigDecimal requiredQuantity = recipeQuantity.multiply(effectiveQuantity)
                    .setScale(4, RoundingMode.HALF_UP);

            if (requiredQuantity.compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }

            lines.add(buildSupplyLine(composition.getSupply(), requiredQuantity));
        }

        return lines;
    }

    private ProductionOrderSupply buildSupplyLine(Supply supply, BigDecimal quantityUsed) {
        ProductionOrderSupply line = new ProductionOrderSupply();
        line.setSupply(supply);
        line.setQuantityUsed(quantityUsed.setScale(4, RoundingMode.HALF_UP));

        BigDecimal unitCost = supply.getUnitCost() != null
                ? supply.getUnitCost()
                : BigDecimal.ZERO.setScale(6, RoundingMode.HALF_UP);

        line.setUnitCostSnapshot(unitCost);
        line.setLineTotal(quantityUsed.multiply(unitCost).setScale(2, RoundingMode.HALF_UP));

        return line;
    }

    private BigDecimal normalizePositiveQuantity(BigDecimal quantity) {
        if (quantity == null || quantity.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ONE.setScale(2, RoundingMode.HALF_UP);
        }

        return quantity.setScale(2, RoundingMode.HALF_UP);
    }

    private String resolveProductName(ProductionOrder order) {
        if (order == null) {
            return "Producto";
        }

        if (order.getProduct() != null && order.getProduct().getName() != null && !order.getProduct().getName().isBlank()) {
            return order.getProduct().getName();
        }

        if (order.getProductId() != null) {
            return "Producto #" + order.getProductId();
        }

        return "Producto";
    }

    private BigDecimal valueOrZero(BigDecimal value) {
        return value != null ? value : BigDecimal.ZERO;
    }

    private String clean(String value) {
        if (value == null) {
            return null;
        }

        String trimmed = value.trim();
        return trimmed.isBlank() ? null : trimmed;
    }
}
