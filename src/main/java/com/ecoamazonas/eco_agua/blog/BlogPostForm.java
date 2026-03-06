package com.ecoamazonas.eco_agua.blog;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class BlogPostForm {

    private Long id;

    @NotBlank
    @Size(max = 220)
    private String title;

    @Size(max = 220)
    private String slug;

    @NotBlank
    @Size(max = 400)
    private String summary;

    @NotBlank
    private String content;

    private String coverImagePath;

    private BlogPost.Status status = BlogPost.Status.DRAFT;

    // --- Getters and setters ---

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getSlug() {
        return slug;
    }

    public void setSlug(String slug) {
        this.slug = slug;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getCoverImagePath() {
        return coverImagePath;
    }

    public void setCoverImagePath(String coverImagePath) {
        this.coverImagePath = coverImagePath;
    }

    public BlogPost.Status getStatus() {
        return status;
    }

    public void setStatus(BlogPost.Status status) {
        this.status = status;
    }

    // --- Mapping helpers ---

    public static BlogPostForm fromEntity(BlogPost entity) {
        BlogPostForm form = new BlogPostForm();
        form.setId(entity.getId());
        form.setTitle(entity.getTitle());
        form.setSlug(entity.getSlug());
        form.setSummary(entity.getSummary());
        form.setContent(entity.getContent());
        form.setCoverImagePath(entity.getCoverImagePath());
        form.setStatus(entity.getStatus());
        return form;
    }

    public void updateEntity(BlogPost entity) {
        entity.setTitle(this.title);
        entity.setSlug(this.slug);
        entity.setSummary(this.summary);
        entity.setContent(this.content);
        entity.setCoverImagePath(this.coverImagePath);
        entity.setStatus(this.status);
    }
}
