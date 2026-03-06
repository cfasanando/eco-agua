package com.ecoamazonas.eco_agua.supplier;

import com.ecoamazonas.eco_agua.category.Category;
import jakarta.persistence.*;

@Entity
@Table(name = "supplier")
public class Supplier {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(name = "doc_type", nullable = false, length = 10)
    private String docType;

    @Column(name = "doc_number", nullable = false, length = 20)
    private String docNumber;

    @Column(length = 255)
    private String address;

    @Column(length = 30)
    private String phone;

    @Column(name = "contact_name", length = 200)
    private String contactName;

    @Column(name = "contact_phone", length = 30)
    private String contactPhone;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private Category category;

    @Column(nullable = false)
    private boolean active = true;

    // --- Getters & setters ---

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDocType() {
        return docType;
    }

    public String getDocNumber() {
        return docNumber;
    }

    public String getAddress() {
        return address;
    }

    public String getPhone() {
        return phone;
    }

    public String getContactName() {
        return contactName;
    }

    public String getContactPhone() {
        return contactPhone;
    }

    public Category getCategory() {
        return category;
    }

    public boolean isActive() {
        return active;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setDocType(String docType) {
        this.docType = docType;
    }

    public void setDocNumber(String docNumber) {
        this.docNumber = docNumber;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public void setContactName(String contactName) {
        this.contactName = contactName;
    }

    public void setContactPhone(String contactPhone) {
        this.contactPhone = contactPhone;
    }

    public void setCategory(Category category) {
        this.category = category;
    }

    public void setActive(boolean active) {
        this.active = active;
    }
}
