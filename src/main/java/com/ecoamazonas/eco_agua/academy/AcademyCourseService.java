package com.ecoamazonas.eco_agua.academy;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.text.Normalizer;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Service
public class AcademyCourseService {

    private final AcademyCourseRepository courseRepository;

    public AcademyCourseService(AcademyCourseRepository courseRepository) {
        this.courseRepository = courseRepository;
    }

    @Transactional(readOnly = true)
    public List<AcademyCourse> findAllForAdmin() {
        return courseRepository.findAllForAdmin();
    }

    @Transactional(readOnly = true)
    public List<AcademyCourse> findPublishedForCatalog(String query, String category) {
        String cleanQuery = clean(query).toLowerCase(Locale.ROOT);
        String cleanCategory = clean(category);

        return courseRepository.findPublishedForCatalog().stream()
                .filter(course -> matchesQuery(course, cleanQuery))
                .filter(course -> cleanCategory.isBlank() || cleanCategory.equalsIgnoreCase(clean(course.getCategory())))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<AcademyCourse> findFeaturedPublished() {
        return courseRepository.findFeaturedPublished();
    }

    @Transactional(readOnly = true)
    public AcademyCourse findForm(Long id) {
        if (id == null) {
            AcademyCourse course = new AcademyCourse();
            course.setStatus(AcademyCourse.Status.DRAFT);
            course.setLevel(AcademyCourse.Level.GENERAL);
            course.setActive(true);
            course.setDurationLabel("4 semanas");
            course.setInstructor("Christian Fasanando");
            course.setWhatsappMessage("Hola, deseo información sobre este curso.");
            return course;
        }

        return courseRepository.findById(id).orElseGet(AcademyCourse::new);
    }

    @Transactional(readOnly = true)
    public AcademyCourse findPublishedBySlug(String slug) {
        return courseRepository.findBySlugAndActiveTrueAndStatus(slug, AcademyCourse.Status.PUBLISHED)
                .orElse(null);
    }

    @Transactional(readOnly = true)
    public List<String> findCategories() {
        return courseRepository.findDistinctCategories();
    }

    @Transactional
    public AcademyCourse save(AcademyCourse form) {
        AcademyCourse target = form;
        if (form.getId() != null) {
            target = courseRepository.findById(form.getId()).orElse(form);
            copyEditableFields(form, target);
        }

        normalize(target);
        return courseRepository.save(target);
    }

    @Transactional
    public void publish(Long id) {
        courseRepository.findById(id).ifPresent(course -> {
            course.setStatus(AcademyCourse.Status.PUBLISHED);
            course.setActive(true);
            if (course.getPublishedAt() == null) {
                course.setPublishedAt(LocalDateTime.now());
            }
            courseRepository.save(course);
        });
    }

    @Transactional
    public void draft(Long id) {
        courseRepository.findById(id).ifPresent(course -> {
            course.setStatus(AcademyCourse.Status.DRAFT);
            courseRepository.save(course);
        });
    }

    @Transactional
    public void toggleFeatured(Long id) {
        courseRepository.findById(id).ifPresent(course -> {
            course.setFeatured(!course.isFeatured());
            courseRepository.save(course);
        });
    }

    @Transactional
    public void archive(Long id) {
        courseRepository.findById(id).ifPresent(course -> {
            course.setStatus(AcademyCourse.Status.ARCHIVED);
            course.setActive(false);
            courseRepository.save(course);
        });
    }

    public String buildWhatsappMessage(AcademyCourse course) {
        String configuredMessage = clean(course.getWhatsappMessage());
        if (!configuredMessage.isBlank()) {
            return configuredMessage + "\nCurso: " + clean(course.getTitle());
        }
        return "Hola, deseo información sobre el curso: " + clean(course.getTitle());
    }

    private boolean matchesQuery(AcademyCourse course, String cleanQuery) {
        if (cleanQuery == null || cleanQuery.isBlank()) {
            return true;
        }

        List<String> searchableValues = new ArrayList<>();
        searchableValues.add(course.getTitle());
        searchableValues.add(course.getCategory());
        searchableValues.add(course.getInstructor());
        searchableValues.add(course.getShortDescription());
        searchableValues.add(course.getLongDescription());

        return searchableValues.stream()
                .map(this::clean)
                .map(value -> value.toLowerCase(Locale.ROOT))
                .anyMatch(value -> value.contains(cleanQuery));
    }

    private void copyEditableFields(AcademyCourse source, AcademyCourse target) {
        target.setTitle(source.getTitle());
        target.setSlug(source.getSlug());
        target.setCategory(source.getCategory());
        target.setInstructor(source.getInstructor());
        target.setShortDescription(source.getShortDescription());
        target.setLongDescription(source.getLongDescription());
        target.setCoverImageUrl(source.getCoverImageUrl());
        target.setPromoVideoUrl(source.getPromoVideoUrl());
        target.setDurationLabel(source.getDurationLabel());
        target.setPrice(source.getPrice());
        target.setFeatured(source.isFeatured());
        target.setActive(source.isActive());
        target.setStatus(source.getStatus());
        target.setLevel(source.getLevel());
        target.setWhatsappMessage(source.getWhatsappMessage());
    }

    private void normalize(AcademyCourse course) {
        course.setTitle(defaultIfBlank(course.getTitle(), "Nuevo curso"));
        course.setCategory(defaultIfBlank(course.getCategory(), "General"));
        course.setInstructor(defaultIfBlank(course.getInstructor(), "Instructor"));
        course.setShortDescription(limit(defaultIfBlank(course.getShortDescription(), "Curso práctico con contenido actualizado."), 500));
        course.setDurationLabel(defaultIfBlank(course.getDurationLabel(), "Por definir"));
        course.setCoverImageUrl(clean(course.getCoverImageUrl()));
        course.setPromoVideoUrl(clean(course.getPromoVideoUrl()));
        course.setWhatsappMessage(defaultIfBlank(course.getWhatsappMessage(), "Hola, deseo información sobre este curso."));

        if (course.getStatus() == null) {
            course.setStatus(AcademyCourse.Status.DRAFT);
        }
        if (course.getLevel() == null) {
            course.setLevel(AcademyCourse.Level.GENERAL);
        }
        if (course.getPrice() != null && course.getPrice().compareTo(BigDecimal.ZERO) < 0) {
            course.setPrice(BigDecimal.ZERO);
        }

        String slugBase = clean(course.getSlug()).isBlank() ? course.getTitle() : course.getSlug();
        course.setSlug(uniqueSlug(slugify(slugBase), course.getId()));
    }

    private String uniqueSlug(String baseSlug, Long currentId) {
        String base = clean(baseSlug).isBlank() ? "curso" : baseSlug;
        String candidate = base;
        int counter = 2;
        while (slugExistsForAnotherCourse(candidate, currentId)) {
            candidate = base + "-" + counter;
            counter++;
        }
        return candidate;
    }

    private boolean slugExistsForAnotherCourse(String slug, Long currentId) {
        if (currentId == null) {
            return courseRepository.existsBySlug(slug);
        }
        return courseRepository.existsBySlugAndIdNot(slug, currentId);
    }

    private String slugify(String value) {
        String normalized = Normalizer.normalize(clean(value), Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("^-+|-+$", "");
        return normalized.isBlank() ? "curso" : normalized;
    }

    private String defaultIfBlank(String value, String fallback) {
        String cleanValue = clean(value);
        return cleanValue.isBlank() ? fallback : cleanValue;
    }

    private String clean(String value) {
        return value != null ? value.trim() : "";
    }

    private String limit(String value, int length) {
        String cleanValue = clean(value);
        if (cleanValue.length() <= length) {
            return cleanValue;
        }
        return cleanValue.substring(0, length).trim();
    }
}
