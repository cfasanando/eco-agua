package com.ecoamazonas.eco_agua.academy;

import com.ecoamazonas.eco_agua.user.UserAccount;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "academy_lead",
        indexes = {
                @Index(name = "idx_academy_lead_course", columnList = "course_id"),
                @Index(name = "idx_academy_lead_status", columnList = "status"),
                @Index(name = "idx_academy_lead_created_at", columnList = "created_at"),
                @Index(name = "idx_academy_lead_student", columnList = "student_id"),
                @Index(name = "idx_academy_lead_enrollment", columnList = "enrollment_id")
        }
)
public class AcademyLead {

    public enum Status {
        NEW("Nuevo"),
        CONTACTED("Contactado"),
        ENROLLED("Inscrito"),
        DISCARDED("Descartado");

        private final String label;

        Status(String label) {
            this.label = label;
        }

        public String getLabel() {
            return label;
        }
    }

    public enum Source {
        CATALOG("Catálogo"),
        WHATSAPP("WhatsApp"),
        CAMPAIGN("Campaña"),
        REFERRAL("Referido"),
        OTHER("Otro");

        private final String label;

        Source(String label) {
            this.label = label;
        }

        public String getLabel() {
            return label;
        }
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "course_id", nullable = false)
    private AcademyCourse course;

    @Column(name = "full_name", nullable = false, length = 180)
    private String fullName;

    @Column(length = 80)
    private String phone;

    @Column(length = 180)
    private String email;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private Status status = Status.NEW;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private Source source = Source.CATALOG;

    @Lob
    @Column(name = "public_message")
    private String publicMessage;

    @Lob
    @Column(name = "internal_notes")
    private String internalNotes;

    @Column(name = "origin_path", length = 500)
    private String originPath;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id")
    private UserAccount student;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "enrollment_id")
    private AcademyEnrollment enrollment;

    @Column(name = "contacted_at")
    private LocalDateTime contactedAt;

    @Column(name = "converted_at")
    private LocalDateTime convertedAt;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        if (createdAt == null) {
            createdAt = now;
        }
        updatedAt = now;
        if (status == null) {
            status = Status.NEW;
        }
        if (source == null) {
            source = Source.CATALOG;
        }
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public String getStatusLabel() {
        return status != null ? status.getLabel() : Status.NEW.getLabel();
    }

    public String getSourceLabel() {
        return source != null ? source.getLabel() : Source.CATALOG.getLabel();
    }

    public boolean isConverted() {
        return enrollment != null || Status.ENROLLED.equals(status);
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public AcademyCourse getCourse() {
        return course;
    }

    public void setCourse(AcademyCourse course) {
        this.course = course;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public Source getSource() {
        return source;
    }

    public void setSource(Source source) {
        this.source = source;
    }

    public String getPublicMessage() {
        return publicMessage;
    }

    public void setPublicMessage(String publicMessage) {
        this.publicMessage = publicMessage;
    }

    public String getInternalNotes() {
        return internalNotes;
    }

    public void setInternalNotes(String internalNotes) {
        this.internalNotes = internalNotes;
    }

    public String getOriginPath() {
        return originPath;
    }

    public void setOriginPath(String originPath) {
        this.originPath = originPath;
    }

    public UserAccount getStudent() {
        return student;
    }

    public void setStudent(UserAccount student) {
        this.student = student;
    }

    public AcademyEnrollment getEnrollment() {
        return enrollment;
    }

    public void setEnrollment(AcademyEnrollment enrollment) {
        this.enrollment = enrollment;
    }

    public LocalDateTime getContactedAt() {
        return contactedAt;
    }

    public void setContactedAt(LocalDateTime contactedAt) {
        this.contactedAt = contactedAt;
    }

    public LocalDateTime getConvertedAt() {
        return convertedAt;
    }

    public void setConvertedAt(LocalDateTime convertedAt) {
        this.convertedAt = convertedAt;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
