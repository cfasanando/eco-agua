package com.ecoamazonas.eco_agua.academy;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "academy_lesson")
public class AcademyLesson {

    public enum LessonType {
        VIDEO("Video"),
        TEXT("Texto"),
        MATERIAL("Material"),
        LINK("Enlace");

        private final String label;

        LessonType(String label) {
            this.label = label;
        }

        public String getLabel() {
            return label;
        }
    }

    public enum Status {
        DRAFT("Borrador"),
        PUBLISHED("Publicado"),
        ARCHIVED("Archivado");

        private final String label;

        Status(String label) {
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

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "module_id", nullable = false)
    private AcademyCourseModule module;

    @Column(nullable = false, length = 180)
    private String title;

    @Column(length = 500)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "lesson_type", nullable = false, length = 30)
    private LessonType lessonType = LessonType.VIDEO;

    @Lob
    @Column(name = "content_text")
    private String contentText;

    @Column(name = "video_url", length = 500)
    private String videoUrl;

    @Column(name = "material_url", length = 500)
    private String materialUrl;

    @Column(name = "duration_label", length = 80)
    private String durationLabel;

    @Column(name = "display_order", nullable = false)
    private int displayOrder = 1;

    @Column(name = "preview", nullable = false)
    private boolean preview = false;

    @Column(nullable = false)
    private boolean active = true;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private Status status = Status.DRAFT;

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
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public boolean isPublished() {
        return active && Status.PUBLISHED.equals(status);
    }

    public String getTypeLabel() {
        return lessonType != null ? lessonType.getLabel() : LessonType.VIDEO.getLabel();
    }

    public String getStatusLabel() {
        return status != null ? status.getLabel() : Status.DRAFT.getLabel();
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

    public AcademyCourseModule getModule() {
        return module;
    }

    public void setModule(AcademyCourseModule module) {
        this.module = module;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public LessonType getLessonType() {
        return lessonType;
    }

    public void setLessonType(LessonType lessonType) {
        this.lessonType = lessonType;
    }

    public String getContentText() {
        return contentText;
    }

    public void setContentText(String contentText) {
        this.contentText = contentText;
    }

    public String getVideoUrl() {
        return videoUrl;
    }

    public void setVideoUrl(String videoUrl) {
        this.videoUrl = videoUrl;
    }

    public String getMaterialUrl() {
        return materialUrl;
    }

    public void setMaterialUrl(String materialUrl) {
        this.materialUrl = materialUrl;
    }

    public String getDurationLabel() {
        return durationLabel;
    }

    public void setDurationLabel(String durationLabel) {
        this.durationLabel = durationLabel;
    }

    public int getDisplayOrder() {
        return displayOrder;
    }

    public void setDisplayOrder(int displayOrder) {
        this.displayOrder = displayOrder;
    }

    public boolean isPreview() {
        return preview;
    }

    public void setPreview(boolean preview) {
        this.preview = preview;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
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
