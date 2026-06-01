package com.ecoamazonas.eco_agua.supplier;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

public class SupplierListRow {

    private final Supplier supplier;
    private final SupplierPurchaseMetrics metrics;

    public SupplierListRow(Supplier supplier, SupplierPurchaseMetrics metrics) {
        this.supplier = supplier;
        this.metrics = metrics != null
                ? metrics
                : SupplierPurchaseMetrics.empty(supplier != null ? supplier.getId() : null);
    }

    public Supplier getSupplier() {
        return supplier;
    }

    public SupplierPurchaseMetrics getMetrics() {
        return metrics;
    }

    public String getMainPhone() {
        if (supplier == null) {
            return "";
        }
        if (hasText(supplier.getContactPhone())) {
            return supplier.getContactPhone();
        }
        if (hasText(supplier.getPhone())) {
            return supplier.getPhone();
        }
        return "";
    }

    public boolean isWhatsappAvailable() {
        return !normalizePhone(getMainPhone()).isBlank();
    }

    public String getWhatsappUrl() {
        String phone = normalizePhone(getMainPhone());
        if (phone.isBlank()) {
            return "#";
        }

        String supplierName = supplier != null && hasText(supplier.getName()) ? supplier.getName().trim() : "proveedor";
        String message = "Hola, quisiera consultar disponibilidad y precios actualizados con " + supplierName + ".";
        return "https://wa.me/" + phone + "?text=" + URLEncoder.encode(message, StandardCharsets.UTF_8);
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private String normalizePhone(String value) {
        if (value == null) {
            return "";
        }

        String digits = value.replaceAll("\\D", "");
        if (digits.length() == 9) {
            return "51" + digits;
        }
        if (digits.length() >= 10) {
            return digits;
        }
        return "";
    }
}
