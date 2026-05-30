package com.ecoamazonas.eco_agua.accounting.service;

import com.ecoamazonas.eco_agua.accounting.dto.SalesRegistryRow;
import com.ecoamazonas.eco_agua.income.OtherIncome;
import com.ecoamazonas.eco_agua.income.OtherIncomeRepository;
import com.ecoamazonas.eco_agua.order.OrderStatus;
import com.ecoamazonas.eco_agua.order.SaleOrder;
import com.ecoamazonas.eco_agua.order.SaleOrderRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
public class SalesRegistryService {

    private final SaleOrderRepository saleOrderRepository;
    private final OtherIncomeRepository otherIncomeRepository;

    public SalesRegistryService(
            SaleOrderRepository saleOrderRepository,
            OtherIncomeRepository otherIncomeRepository
    ) {
        this.saleOrderRepository = saleOrderRepository;
        this.otherIncomeRepository = otherIncomeRepository;
    }

    /**
     * Builds the registry for a given year and optional month.
     * If month is null or <= 0, the whole year is considered.
     */
    public List<SalesRegistryRow> getRegistryForMonth(
            int year,
            Integer month,
            String docTypeFilter,
            String statusFilter
    ) {
        LocalDate start;
        LocalDate end;

        if (month == null || month <= 0) {
            start = LocalDate.of(year, 1, 1);
            end = LocalDate.of(year, 12, 31);
        } else {
            YearMonth ym = YearMonth.of(year, month);
            start = ym.atDay(1);
            end = ym.atEndOfMonth();
        }

        String normalizedDocType = normalizeFilter(docTypeFilter);
        String normalizedStatus = normalizeFilter(statusFilter);

        List<SalesRegistryRow> result = new ArrayList<>();

        // ---------- Sales (sale_order) ----------
        List<SaleOrder> orders = saleOrderRepository.findByOrderDateBetween(start, end);

        for (SaleOrder so : orders) {
            if (!matchesStatusFilter(so, normalizedStatus)) {
                continue;
            }

            if (!matchesDocTypeFilter(so.getDocType(), normalizedDocType)) {
                continue;
            }

            String clientDocType = safeClientDocType(so);
            String clientDocNumber = safeClientDocNumber(so);
            String clientName = safeClientName(so);

            SalesRegistryRow row = new SalesRegistryRow(
                    so.getOrderDate(),
                    so.getDocType(),
                    so.getDocSeries(),
                    so.getDocNumber(),
                    clientDocType,
                    clientDocNumber,
                    clientName,
                    defaultZero(so.getTaxBase()),
                    defaultZero(so.getTaxIgv()),
                    defaultZero(so.getTotalAmount()),
                    mapSaleOrderStatus(so.getStatus()),
                    "SALE_ORDER",
                    so.getId()
            );

            result.add(row);
        }

        // ---------- Other income (only "Ventas" category) ----------
        List<OtherIncome> others = otherIncomeRepository.findByIncomeDateBetweenOrderByIncomeDateAsc(start, end);

        for (OtherIncome oi : others) {
            if (!isSalesCategory(oi)) {
                continue;
            }

            if (!matchesDocTypeFilter(oi.getDocType(), normalizedDocType)) {
                continue;
            }

            SalesRegistryRow row = new SalesRegistryRow(
                    oi.getIncomeDate(),
                    oi.getDocType(),
                    oi.getDocSeries(),
                    oi.getDocNumber(),
                    null,
                    null,
                    oi.getCategory() != null ? oi.getCategory().getName() : null,
                    defaultZero(oi.getTaxBase()),
                    defaultZero(oi.getTaxIgv()),
                    defaultZero(oi.getAmount()),
                    "VALIDO",
                    "OTHER_INCOME",
                    oi.getId()
            );

            result.add(row);
        }

        result.sort(
                Comparator.comparing(SalesRegistryRow::getEmissionDate)
                        .thenComparing(SalesRegistryRow::getDocType, Comparator.nullsLast(String::compareTo))
                        .thenComparing(SalesRegistryRow::getDocSeries, Comparator.nullsLast(String::compareTo))
                        .thenComparing(SalesRegistryRow::getDocNumber, Comparator.nullsLast(String::compareTo))
        );

        return result;
    }

    public BigDecimal getTotalBase(List<SalesRegistryRow> rows) {
        return rows.stream()
                .map(SalesRegistryRow::getTaxBase)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public BigDecimal getTotalIgv(List<SalesRegistryRow> rows) {
        return rows.stream()
                .map(SalesRegistryRow::getTaxIgv)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public BigDecimal getTotalAmount(List<SalesRegistryRow> rows) {
        return rows.stream()
                .map(SalesRegistryRow::getTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private String normalizeFilter(String value) {
        if (value == null) {
            return null;
        }

        String trimmed = value.trim();
        if (trimmed.isEmpty() || "ALL".equalsIgnoreCase(trimmed)) {
            return null;
        }

        return trimmed;
    }

    private BigDecimal defaultZero(BigDecimal value) {
        return value != null ? value : BigDecimal.ZERO;
    }

    private boolean matchesDocTypeFilter(String docType, String filter) {
        if (filter == null) {
            return true;
        }

        if (docType == null || docType.isBlank()) {
            return false;
        }

        return docType.equalsIgnoreCase(filter);
    }

    private boolean matchesStatusFilter(SaleOrder order, String statusFilter) {
        if (statusFilter == null) {
            return true;
        }

        String label = mapSaleOrderStatus(order.getStatus());
        return label.equalsIgnoreCase(statusFilter);
    }

    private String mapSaleOrderStatus(OrderStatus status) {
        if (status == null) {
            return "VALIDO";
        }

        String code = status.name();

        if ("CANCELED".equalsIgnoreCase(code)) {
            return "ANULADO";
        }

        if ("REQUESTED".equalsIgnoreCase(code)) {
            return "BORRADOR";
        }

        return "VALIDO";
    }

    private boolean isSalesCategory(OtherIncome oi) {
        if (oi.getCategory() == null) {
            return false;
        }

        String name = oi.getCategory().getName();
        return name != null && name.equalsIgnoreCase("Ventas");
    }

    private String safeClientDocType(SaleOrder so) {
        try {
            if (so.getClient() == null || so.getClient().getDocType() == null) {
                return null;
            }

            return so.getClient().getDocType().name();
        } catch (EntityNotFoundException ex) {
            return null;
        }
    }

    private String safeClientDocNumber(SaleOrder so) {
        try {
            if (so.getClient() == null) {
                return null;
            }

            return so.getClient().getDocNumber();
        } catch (EntityNotFoundException ex) {
            return null;
        }
    }

    private String safeClientName(SaleOrder so) {
        try {
            if (so.getClient() == null) {
                return null;
            }

            String name = so.getClient().getName();
            return (name == null || name.isBlank()) ? "Cliente sin nombre" : name;
        } catch (EntityNotFoundException ex) {
            return "Cliente eliminado";
        }
    }
}