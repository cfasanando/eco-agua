package com.ecoamazonas.eco_agua.academy;

import com.ecoamazonas.eco_agua.user.UserAccount;
import com.ecoamazonas.eco_agua.user.UserAccountRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
public class AcademyEnrollmentService {

    private final AcademyEnrollmentRepository enrollmentRepository;
    private final AcademyLessonProgressRepository progressRepository;
    private final AcademyCourseRepository courseRepository;
    private final AcademyLessonRepository lessonRepository;
    private final UserAccountRepository userAccountRepository;
    private final AcademyCourseContentService contentService;

    public AcademyEnrollmentService(AcademyEnrollmentRepository enrollmentRepository,
                                    AcademyLessonProgressRepository progressRepository,
                                    AcademyCourseRepository courseRepository,
                                    AcademyLessonRepository lessonRepository,
                                    UserAccountRepository userAccountRepository,
                                    AcademyCourseContentService contentService) {
        this.enrollmentRepository = enrollmentRepository;
        this.progressRepository = progressRepository;
        this.courseRepository = courseRepository;
        this.lessonRepository = lessonRepository;
        this.userAccountRepository = userAccountRepository;
        this.contentService = contentService;
    }

    @Transactional(readOnly = true)
    public List<AcademyEnrollment> findAllForAdmin() {
        return enrollmentRepository.findAllForAdmin();
    }

    @Transactional(readOnly = true)
    public List<UserAccount> findActiveUsers() {
        return userAccountRepository.findAll().stream()
                .filter(UserAccount::isActive)
                .sorted(Comparator.comparing(UserAccount::getUsername, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<AcademyCourse> findCoursesForEnrollment() {
        return courseRepository.findAllForAdmin().stream()
                .filter(AcademyCourse::isPublished)
                .toList();
    }

    @Transactional
    public AcademyEnrollment enroll(Integer studentId, Long courseId, String notes) {
        UserAccount student = userAccountRepository.findById(studentId).orElseThrow();
        AcademyCourse course = courseRepository.findById(courseId).orElseThrow();

        AcademyEnrollment enrollment = enrollmentRepository.findByStudentAndCourse(student, course)
                .orElseGet(AcademyEnrollment::new);
        enrollment.setStudent(student);
        enrollment.setCourse(course);
        enrollment.setNotes(clean(notes));
        if (enrollment.getStatus() == AcademyEnrollment.Status.CANCELLED) {
            enrollment.setStatus(AcademyEnrollment.Status.ENROLLED);
            enrollment.setStartedAt(LocalDateTime.now());
            enrollment.setCompletedAt(null);
        }
        if (enrollment.getStatus() == null) {
            enrollment.setStatus(AcademyEnrollment.Status.ENROLLED);
        }
        enrollment = enrollmentRepository.save(enrollment);
        return recalculateProgress(enrollment);
    }

    @Transactional
    public void updateStatus(Long enrollmentId, AcademyEnrollment.Status status) {
        enrollmentRepository.findById(enrollmentId).ifPresent(enrollment -> {
            enrollment.setStatus(status != null ? status : AcademyEnrollment.Status.ENROLLED);
            if (AcademyEnrollment.Status.COMPLETED.equals(enrollment.getStatus())) {
                enrollment.setProgressPercent(100);
                enrollment.setCompletedAt(LocalDateTime.now());
            } else if (AcademyEnrollment.Status.CANCELLED.equals(enrollment.getStatus())) {
                enrollment.setCompletedAt(null);
            } else if (enrollment.getStartedAt() == null) {
                enrollment.setStartedAt(LocalDateTime.now());
            }
            enrollmentRepository.save(enrollment);
        });
    }

    @Transactional(readOnly = true)
    public List<AcademyEnrollment> findMyCourses(String username) {
        return enrollmentRepository.findByStudentUsernameForStudent(username).stream()
                .filter(AcademyEnrollment::isActiveEnrollment)
                .toList();
    }

    @Transactional(readOnly = true)
    public AcademyEnrollment findMyEnrollment(String username, String courseSlug) {
        return enrollmentRepository.findByStudentUsernameAndCourseSlug(username, courseSlug)
                .filter(AcademyEnrollment::isActiveEnrollment)
                .orElse(null);
    }

    @Transactional(readOnly = true)
    public Set<Long> findCompletedLessonIds(AcademyEnrollment enrollment) {
        if (enrollment == null) {
            return Set.of();
        }
        Set<Long> completedIds = new LinkedHashSet<>();
        progressRepository.findByEnrollment(enrollment).stream()
                .filter(AcademyLessonProgress::isCompleted)
                .map(AcademyLessonProgress::getLesson)
                .filter(lesson -> lesson != null && lesson.getId() != null)
                .forEach(lesson -> completedIds.add(lesson.getId()));
        return completedIds;
    }

    @Transactional(readOnly = true)
    public AcademyLesson findFirstPublishedLesson(AcademyCourse course) {
        List<AcademyLesson> lessons = contentService.findPublishedLessons(course);
        return lessons.isEmpty() ? null : lessons.get(0);
    }

    @Transactional(readOnly = true)
    public AcademyLesson findPublishedLessonForEnrollment(AcademyEnrollment enrollment, Long lessonId) {
        if (enrollment == null || lessonId == null || enrollment.getCourse() == null) {
            return null;
        }
        return lessonRepository.findById(lessonId)
                .filter(AcademyLesson::isPublished)
                .filter(lesson -> lesson.getCourse() != null && lesson.getCourse().getId().equals(enrollment.getCourse().getId()))
                .orElse(null);
    }

    @Transactional
    public AcademyLessonProgress registerLessonView(AcademyEnrollment enrollment, AcademyLesson lesson) {
        AcademyLessonProgress progress = progressRepository.findByEnrollmentAndLesson(enrollment, lesson)
                .orElseGet(AcademyLessonProgress::new);
        progress.setEnrollment(enrollment);
        progress.setLesson(lesson);
        progress.setLastViewedAt(LocalDateTime.now());
        return progressRepository.save(progress);
    }

    @Transactional
    public void markLessonCompleted(String username, String courseSlug, Long lessonId) {
        AcademyEnrollment enrollment = enrollmentRepository.findByStudentUsernameAndCourseSlug(username, courseSlug)
                .filter(AcademyEnrollment::isActiveEnrollment)
                .orElseThrow();
        AcademyLesson lesson = findPublishedLessonForEnrollment(enrollment, lessonId);
        if (lesson == null) {
            return;
        }

        AcademyLessonProgress progress = progressRepository.findByEnrollmentAndLesson(enrollment, lesson)
                .orElseGet(AcademyLessonProgress::new);
        progress.setEnrollment(enrollment);
        progress.setLesson(lesson);
        progress.setLastViewedAt(LocalDateTime.now());
        progress.setCompleted(true);
        if (progress.getCompletedAt() == null) {
            progress.setCompletedAt(LocalDateTime.now());
        }
        progressRepository.save(progress);
        recalculateProgress(enrollment);
    }

    @Transactional
    public AcademyEnrollment recalculateProgress(AcademyEnrollment enrollment) {
        if (enrollment == null || enrollment.getCourse() == null) {
            return enrollment;
        }
        long totalLessons = contentService.countPublishedLessons(enrollment.getCourse());
        long completedLessons = progressRepository.countByEnrollmentAndCompletedTrue(enrollment);
        int total = Math.toIntExact(Math.min(totalLessons, Integer.MAX_VALUE));
        int completed = Math.toIntExact(Math.min(completedLessons, Integer.MAX_VALUE));
        int percent = total > 0 ? (int) Math.round((completed * 100.0d) / total) : 0;

        enrollment.setTotalLessons(total);
        enrollment.setCompletedLessons(completed);
        enrollment.setProgressPercent(Math.min(100, Math.max(0, percent)));

        if (!AcademyEnrollment.Status.CANCELLED.equals(enrollment.getStatus())) {
            if (total > 0 && completed >= total) {
                enrollment.setStatus(AcademyEnrollment.Status.COMPLETED);
                if (enrollment.getCompletedAt() == null) {
                    enrollment.setCompletedAt(LocalDateTime.now());
                }
            } else if (completed > 0) {
                enrollment.setStatus(AcademyEnrollment.Status.IN_PROGRESS);
                enrollment.setCompletedAt(null);
            } else if (enrollment.getStatus() == null || AcademyEnrollment.Status.COMPLETED.equals(enrollment.getStatus())) {
                enrollment.setStatus(AcademyEnrollment.Status.ENROLLED);
                enrollment.setCompletedAt(null);
            }
        }

        return enrollmentRepository.save(enrollment);
    }

    private String clean(String value) {
        return value != null ? value.trim() : "";
    }
}
