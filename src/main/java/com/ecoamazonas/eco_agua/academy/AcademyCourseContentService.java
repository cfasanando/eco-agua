package com.ecoamazonas.eco_agua.academy;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;

@Service
public class AcademyCourseContentService {

    private final AcademyCourseRepository courseRepository;
    private final AcademyCourseModuleRepository moduleRepository;
    private final AcademyLessonRepository lessonRepository;

    public AcademyCourseContentService(AcademyCourseRepository courseRepository,
                                       AcademyCourseModuleRepository moduleRepository,
                                       AcademyLessonRepository lessonRepository) {
        this.courseRepository = courseRepository;
        this.moduleRepository = moduleRepository;
        this.lessonRepository = lessonRepository;
    }

    @Transactional(readOnly = true)
    public AcademyCourse findCourse(Long courseId) {
        return courseRepository.findById(courseId).orElse(null);
    }

    @Transactional(readOnly = true)
    public AcademyCourseModule findModuleForm(Long courseId, Long moduleId) {
        if (moduleId != null) {
            return moduleRepository.findById(moduleId)
                    .filter(module -> module.getCourse() != null && module.getCourse().getId().equals(courseId))
                    .orElseGet(() -> newModuleForm(courseId));
        }
        return newModuleForm(courseId);
    }

    @Transactional(readOnly = true)
    public AcademyLesson findLessonForm(Long courseId, Long lessonId, Long moduleId) {
        if (lessonId != null) {
            return lessonRepository.findById(lessonId)
                    .filter(lesson -> lesson.getCourse() != null && lesson.getCourse().getId().equals(courseId))
                    .orElseGet(() -> newLessonForm(courseId, moduleId));
        }
        return newLessonForm(courseId, moduleId);
    }

    @Transactional(readOnly = true)
    public List<AcademyCourseModuleView> findModuleViewsForAdmin(AcademyCourse course) {
        if (course == null) {
            return List.of();
        }
        return moduleRepository.findByCourseOrderByDisplayOrderAscIdAsc(course).stream()
                .map(module -> new AcademyCourseModuleView(module, lessonRepository.findByModuleOrderByDisplayOrderAscIdAsc(module)))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<AcademyCourseModuleView> findPublishedModuleViews(AcademyCourse course) {
        if (course == null) {
            return List.of();
        }
        return moduleRepository.findByCourseAndActiveTrueOrderByDisplayOrderAscIdAsc(course).stream()
                .map(module -> new AcademyCourseModuleView(
                        module,
                        lessonRepository.findByModuleAndActiveTrueAndStatusOrderByDisplayOrderAscIdAsc(module, AcademyLesson.Status.PUBLISHED)))
                .filter(view -> !view.getLessons().isEmpty())
                .toList();
    }

    @Transactional(readOnly = true)
    public List<AcademyLesson> findPublishedLessons(AcademyCourse course) {
        if (course == null) {
            return List.of();
        }
        return lessonRepository.findByCourseAndActiveTrueAndStatusOrderByDisplayOrderAscIdAsc(course, AcademyLesson.Status.PUBLISHED).stream()
                .sorted(Comparator.comparing((AcademyLesson lesson) -> lesson.getModule().getDisplayOrder())
                        .thenComparing(AcademyLesson::getDisplayOrder)
                        .thenComparing(AcademyLesson::getId))
                .toList();
    }

    @Transactional(readOnly = true)
    public long countPublishedLessons(AcademyCourse course) {
        if (course == null) {
            return 0;
        }
        return lessonRepository.countByCourseAndActiveTrueAndStatus(course, AcademyLesson.Status.PUBLISHED);
    }

    @Transactional(readOnly = true)
    public long countPreviewLessons(AcademyCourse course) {
        if (course == null) {
            return 0;
        }
        return lessonRepository.countByCourseAndPreviewTrueAndActiveTrueAndStatus(course, AcademyLesson.Status.PUBLISHED);
    }

    @Transactional(readOnly = true)
    public AcademyLesson findPublicPreviewLesson(AcademyCourse course, Long lessonId) {
        List<AcademyLesson> previewLessons = findPublishedLessons(course).stream()
                .filter(AcademyLesson::isPreview)
                .toList();
        if (previewLessons.isEmpty()) {
            return null;
        }
        if (lessonId != null) {
            return previewLessons.stream()
                    .filter(lesson -> lesson.getId().equals(lessonId))
                    .findFirst()
                    .orElse(previewLessons.get(0));
        }
        return previewLessons.get(0);
    }

    @Transactional
    public AcademyCourseModule saveModule(Long courseId, AcademyCourseModule form) {
        AcademyCourse course = courseRepository.findById(courseId).orElseThrow();
        AcademyCourseModule target = form;
        if (form.getId() != null) {
            target = moduleRepository.findById(form.getId())
                    .filter(module -> module.getCourse() != null && module.getCourse().getId().equals(courseId))
                    .orElse(form);
            target.setTitle(form.getTitle());
            target.setDescription(form.getDescription());
            target.setDisplayOrder(form.getDisplayOrder());
            target.setActive(form.isActive());
        }
        target.setCourse(course);
        normalizeModule(target);
        return moduleRepository.save(target);
    }

    @Transactional
    public void archiveModule(Long courseId, Long moduleId) {
        moduleRepository.findById(moduleId)
                .filter(module -> module.getCourse() != null && module.getCourse().getId().equals(courseId))
                .ifPresent(module -> {
                    module.setActive(false);
                    moduleRepository.save(module);
                });
    }

    @Transactional
    public AcademyLesson saveLesson(Long courseId, Long moduleId, AcademyLesson form) {
        AcademyCourse course = courseRepository.findById(courseId).orElseThrow();
        AcademyCourseModule module = moduleRepository.findById(moduleId)
                .filter(item -> item.getCourse() != null && item.getCourse().getId().equals(courseId))
                .orElseThrow();

        AcademyLesson target = form;
        if (form.getId() != null) {
            target = lessonRepository.findById(form.getId())
                    .filter(lesson -> lesson.getCourse() != null && lesson.getCourse().getId().equals(courseId))
                    .orElse(form);
            copyLessonFields(form, target);
        }
        target.setCourse(course);
        target.setModule(module);
        normalizeLesson(target);
        return lessonRepository.save(target);
    }

    @Transactional
    public void publishLesson(Long courseId, Long lessonId) {
        updateLessonStatus(courseId, lessonId, AcademyLesson.Status.PUBLISHED, true);
    }

    @Transactional
    public void draftLesson(Long courseId, Long lessonId) {
        updateLessonStatus(courseId, lessonId, AcademyLesson.Status.DRAFT, true);
    }

    @Transactional
    public void archiveLesson(Long courseId, Long lessonId) {
        updateLessonStatus(courseId, lessonId, AcademyLesson.Status.ARCHIVED, false);
    }

    @Transactional
    public void togglePreview(Long courseId, Long lessonId) {
        lessonRepository.findById(lessonId)
                .filter(lesson -> lesson.getCourse() != null && lesson.getCourse().getId().equals(courseId))
                .ifPresent(lesson -> {
                    lesson.setPreview(!lesson.isPreview());
                    lessonRepository.save(lesson);
                });
    }

    private AcademyCourseModule newModuleForm(Long courseId) {
        AcademyCourseModule module = new AcademyCourseModule();
        courseRepository.findById(courseId).ifPresent(module::setCourse);
        module.setTitle("Nueva unidad");
        module.setDisplayOrder(1);
        module.setActive(true);
        return module;
    }

    private AcademyLesson newLessonForm(Long courseId, Long moduleId) {
        AcademyLesson lesson = new AcademyLesson();
        courseRepository.findById(courseId).ifPresent(lesson::setCourse);
        if (moduleId != null) {
            moduleRepository.findById(moduleId).ifPresent(lesson::setModule);
        }
        lesson.setTitle("Nueva lección");
        lesson.setLessonType(AcademyLesson.LessonType.VIDEO);
        lesson.setStatus(AcademyLesson.Status.DRAFT);
        lesson.setDisplayOrder(1);
        lesson.setDurationLabel("10 min");
        lesson.setActive(true);
        return lesson;
    }

    private void copyLessonFields(AcademyLesson source, AcademyLesson target) {
        target.setTitle(source.getTitle());
        target.setDescription(source.getDescription());
        target.setLessonType(source.getLessonType());
        target.setContentText(source.getContentText());
        target.setVideoUrl(source.getVideoUrl());
        target.setMaterialUrl(source.getMaterialUrl());
        target.setDurationLabel(source.getDurationLabel());
        target.setDisplayOrder(source.getDisplayOrder());
        target.setPreview(source.isPreview());
        target.setActive(source.isActive());
        target.setStatus(source.getStatus());
    }

    private void updateLessonStatus(Long courseId, Long lessonId, AcademyLesson.Status status, boolean active) {
        lessonRepository.findById(lessonId)
                .filter(lesson -> lesson.getCourse() != null && lesson.getCourse().getId().equals(courseId))
                .ifPresent(lesson -> {
                    lesson.setStatus(status);
                    lesson.setActive(active);
                    lessonRepository.save(lesson);
                });
    }

    private void normalizeModule(AcademyCourseModule module) {
        module.setTitle(defaultIfBlank(module.getTitle(), "Unidad"));
        module.setDescription(clean(module.getDescription()));
        if (module.getDisplayOrder() <= 0) {
            module.setDisplayOrder(1);
        }
    }

    private void normalizeLesson(AcademyLesson lesson) {
        lesson.setTitle(defaultIfBlank(lesson.getTitle(), "Lección"));
        lesson.setDescription(limit(lesson.getDescription(), 500));
        lesson.setContentText(clean(lesson.getContentText()));
        lesson.setVideoUrl(clean(lesson.getVideoUrl()));
        lesson.setMaterialUrl(clean(lesson.getMaterialUrl()));
        lesson.setDurationLabel(defaultIfBlank(lesson.getDurationLabel(), "Por definir"));
        if (lesson.getDisplayOrder() <= 0) {
            lesson.setDisplayOrder(1);
        }
        if (lesson.getLessonType() == null) {
            lesson.setLessonType(AcademyLesson.LessonType.VIDEO);
        }
        if (lesson.getStatus() == null) {
            lesson.setStatus(AcademyLesson.Status.DRAFT);
        }
    }

    private String defaultIfBlank(String value, String fallback) {
        String cleanValue = clean(value);
        return cleanValue.isBlank() ? fallback : cleanValue;
    }

    private String clean(String value) {
        return value != null ? value.trim() : "";
    }

    private String limit(String value, int maxLength) {
        String cleanValue = clean(value);
        if (cleanValue.length() <= maxLength) {
            return cleanValue;
        }
        return cleanValue.substring(0, maxLength).trim();
    }
}
