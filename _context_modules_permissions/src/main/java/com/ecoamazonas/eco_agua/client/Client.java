package com.ecoamazonas.eco_agua.client;

import com.ecoamazonas.eco_agua.promotion.Promotion;
import jakarta.persistence.*;
import java.math.BigDecimal;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "client")
public class Client {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "doc_type", nullable = false, length = 10)
    private DocumentType docType;

    @Column(name = "doc_number", nullable = false, length = 20)
    private String docNumber;

    @Column(length = 255)
    private String address;

    @Column(length = 255)
    private String reference;

    @Column(length = 30)
    private String phone;

    @Column(name = "map_picture", length = 255)
    private String mapPicture;

    @Column(nullable = false)
    private boolean active = true;

    @Column(name = "registration_date", nullable = false, updatable = false)
    private LocalDateTime registrationDate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "profile_id")
    private ClientProfile profile;

    @ManyToMany
    @JoinTable(
        name = "client_promotion",
        joinColumns = @JoinColumn(name = "client_id"),
        inverseJoinColumns = @JoinColumn(name = "promotion_id")
    )
    private Set<Promotion> promotions = new HashSet<>();
    
    @Column(precision = 10, scale = 7)
    private BigDecimal latitude;

    @Column(precision = 10, scale = 7)
    private BigDecimal longitude;

    @PrePersist
    protected void onCreate() {
        if (this.registrationDate == null) {
            this.registrationDate = LocalDateTime.now();
        }
    }

    // Getters & setters

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public DocumentType getDocType() {
        return docType;
    }

    public String getDocNumber() {
        return docNumber;
    }

    public String getAddress() {
        return address;
    }

    public String getReference() {
        return reference;
    }

    public String getPhone() {
        return phone;
    }

    public String getMapPicture() {
        return mapPicture;
    }

    public boolean isActive() {
        return active;
    }

    public LocalDateTime getRegistrationDate() {
        return registrationDate;
    }

    public ClientProfile getProfile() {
        return profile;
    }

    public Set<Promotion> getPromotions() {
        return promotions;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setDocType(DocumentType docType) {
        this.docType = docType;
    }

    public void setDocNumber(String docNumber) {
        this.docNumber = docNumber;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public void setReference(String reference) {
        this.reference = reference;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public void setMapPicture(String mapPicture) {
        this.mapPicture = mapPicture;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public void setRegistrationDate(LocalDateTime registrationDate) {
        this.registrationDate = registrationDate;
    }

    public void setProfile(ClientProfile profile) {
        this.profile = profile;
    }

    public void setPromotions(Set<Promotion> promotions) {
        this.promotions = promotions;
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
}
