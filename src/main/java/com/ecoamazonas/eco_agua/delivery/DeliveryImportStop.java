package com.ecoamazonas.eco_agua.delivery;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "delivery_import_stop")
public class DeliveryImportStop {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "batch_id", nullable = false)
    private DeliveryImportBatch batch;

    @Column(name = "route_order_index", nullable = false)
    private Integer routeOrderIndex;

    @Column(name = "client_name", length = 200, nullable = false)
    private String clientName;

    @Column(name = "phone", length = 50)
    private String phone;

    @Column(name = "address", length = 255)
    private String address;

    @Column(name = "reference", length = 255)
    private String reference;

    @Column(name = "amount", precision = 10, scale = 2)
    private BigDecimal amount;

    @Column(name = "observation", length = 500)
    private String observation;

    @Column(name = "latitude", precision = 10, scale = 7)
    private BigDecimal latitude;

    @Column(name = "longitude", precision = 10, scale = 7)
    private BigDecimal longitude;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 30, nullable = false)
    private DeliveryImportStopStatus status = DeliveryImportStopStatus.PENDING;

    @Column(name = "status_observation", length = 500)
    private String statusObservation;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public Long getId() {
        return id;
    }

    public DeliveryImportBatch getBatch() {
        return batch;
    }

    public void setBatch(DeliveryImportBatch batch) {
        this.batch = batch;
    }

    public Integer getRouteOrderIndex() {
        return routeOrderIndex;
    }

    public void setRouteOrderIndex(Integer routeOrderIndex) {
        this.routeOrderIndex = routeOrderIndex;
    }

    public String getClientName() {
        return clientName;
    }

    public void setClientName(String clientName) {
        this.clientName = clientName;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getReference() {
        return reference;
    }

    public void setReference(String reference) {
        this.reference = reference;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getObservation() {
        return observation;
    }

    public void setObservation(String observation) {
        this.observation = observation;
    }

    public BigDecimal getLatitude() {
        return latitude;
    }

    public void setLatitude(BigDecimal latitude) {
        this.latitude = latitude;
    }

    public BigDecimal getLongitude() {
        return longitude;
    }

    public void setLongitude(BigDecimal longitude) {
        this.longitude = longitude;
    }

    public DeliveryImportStopStatus getStatus() {
        return status;
    }

    public void setStatus(DeliveryImportStopStatus status) {
        this.status = status;
    }

    public String getStatusObservation() {
        return statusObservation;
    }

    public void setStatusObservation(String statusObservation) {
        this.statusObservation = statusObservation;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    @Transient
    public boolean hasLocation() {
        return latitude != null && longitude != null;
    }

    @Transient
    public String getWhatsappUrl() {
        String normalized = normalizePhone(phone);
        if (normalized == null) {
            return null;
        }
        String text = "Hola, te escribimos para coordinar tu entrega de hoy.";
        return "https://wa.me/" + normalized + "?text=" + java.net.URLEncoder.encode(text, java.nio.charset.StandardCharsets.UTF_8);
    }

    @Transient
    public String getOpenStreetMapUrl() {
        if (!hasLocation()) {
            return null;
        }
        return "https://www.openstreetmap.org/?mlat=" + latitude + "&mlon=" + longitude + "#map=18/" + latitude + "/" + longitude;
    }

    private String normalizePhone(String rawPhone) {
        if (rawPhone == null || rawPhone.isBlank()) {
            return null;
        }
        String digits = rawPhone.replaceAll("\\D", "");
        if (digits.isBlank()) {
            return null;
        }
        if (digits.length() == 9) {
            return "51" + digits;
        }
        return digits;
    }
}
