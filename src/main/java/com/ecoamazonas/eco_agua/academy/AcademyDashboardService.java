package com.ecoamazonas.eco_agua.academy;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class AcademyDashboardService {

    private final AcademyCourseRepository courseRepository;
    private final AcademyLessonRepository lessonRepository;
    private final AcademyEnrollmentRepository enrollmentRepository;
    private final AcademyAssessmentAttemptRepository attemptRepository;
    private final AcademyCertificateRepository certificateRepository;
    private final AcademyLeadRepository leadRepository;

    public AcademyDashboardService(AcademyCourseRepository courseRepository,
                                   AcademyLessonRepository lessonRepository,
                                   AcademyEnrollmentRepository enrollmentRepository,
                                   AcademyAssessmentAttemptRepository attemptRepository,
                                   AcademyCertificateRepository certificateRepository,
                                   AcademyLeadRepository leadRepository) {
        this.courseRepository = courseRepository;
        this.lessonRepository = lessonRepository;
        this.enrollmentRepository = enrollmentRepository;
        this.attemptRepository = attemptRepository;
        this.certificateRepository = certificateRepository;
        this.leadRepository = leadRepository;
    }

    @Transactional(readOnly = true)
    public AcademyDashboardSummary buildSummary() {
        List<AcademyCourse> courses = courseRepository.findAllForAdmin();
        List<AcademyEnrollment> enrollments = enrollmentRepository.findAllForAdmin();
        List<AcademyAssessmentAttempt> attempts = attemptRepository.findAllForAdmin();
        List<AcademyCertificate> certificates = certificateRepository.findAllForAdmin();
        List<AcademyLead> leads = leadRepository.findAllForAdmin();
        List<AcademyLesson> lessons = lessonRepository.findAll();

        AcademyCourseStats courseStats = buildCourseStats(courses, lessons);
        AcademyEnrollmentStats enrollmentStats = buildEnrollmentStats(enrollments);
        AcademyAssessmentStats assessmentStats = buildAssessmentStats(attempts);
        AcademyCertificateStats certificateStats = buildCertificateStats(certificates);
        AcademyLeadStats leadStats = buildLeadStats(leads);

        Map<Long, List<AcademyEnrollment>> enrollmentsByCourse = enrollments.stream()
                .filter(enrollment -> enrollment.getCourse() != null && enrollment.getCourse().getId() != null)
                .collect(Collectors.groupingBy(enrollment -> enrollment.getCourse().getId()));
        Map<Long, Long> lessonsByCourse = lessons.stream()
                .filter(lesson -> lesson.getCourse() != null && lesson.getCourse().getId() != null)
                .collect(Collectors.groupingBy(lesson -> lesson.getCourse().getId(), Collectors.counting()));
        Map<Long, Long> leadsByCourse = leads.stream()
                .filter(lead -> lead.getCourse() != null && lead.getCourse().getId() != null)
                .collect(Collectors.groupingBy(lead -> lead.getCourse().getId(), Collectors.counting()));
        Map<Long, Long> certificatesByCourse = certificates.stream()
                .filter(certificate -> certificate.getCourse() != null && certificate.getCourse().getId() != null)
                .collect(Collectors.groupingBy(certificate -> certificate.getCourse().getId(), Collectors.counting()));

        List<AcademyCoursePerformanceRow> topCourses = courses.stream()
                .map(course -> buildCoursePerformanceRow(course, enrollmentsByCourse, lessonsByCourse, leadsByCourse, certificatesByCourse))
                .sorted(Comparator.comparingLong(AcademyCoursePerformanceRow::enrollments).reversed()
                        .thenComparing(Comparator.comparingLong(AcademyCoursePerformanceRow::leads).reversed())
                        .thenComparing(AcademyCoursePerformanceRow::title, String.CASE_INSENSITIVE_ORDER))
                .limit(8)
                .toList();

        List<AcademyRecentActivityRow> recentActivity = buildRecentActivity(enrollments, attempts, certificates, leads);
        List<AcademyLeadStatusRow> leadStatusRows = buildLeadStatusRows(leads);
        List<AcademyEnrollmentStatusRow> enrollmentStatusRows = buildEnrollmentStatusRows(enrollments);

        return new AcademyDashboardSummary(
                courseStats,
                enrollmentStats,
                assessmentStats,
                certificateStats,
                leadStats,
                topCourses,
                leadStatusRows,
                enrollmentStatusRows,
                recentActivity
        );
    }

    private AcademyCourseStats buildCourseStats(List<AcademyCourse> courses, List<AcademyLesson> lessons) {
        long published = courses.stream().filter(course -> course.getStatus() == AcademyCourse.Status.PUBLISHED).count();
        long draft = courses.stream().filter(course -> course.getStatus() == AcademyCourse.Status.DRAFT).count();
        long archived = courses.stream().filter(course -> course.getStatus() == AcademyCourse.Status.ARCHIVED).count();
        long featured = courses.stream().filter(AcademyCourse::isFeatured).count();
        long previewLessons = lessons.stream().filter(AcademyLesson::isPreview).count();
        return new AcademyCourseStats(courses.size(), published, draft, archived, featured, lessons.size(), previewLessons);
    }

    private AcademyEnrollmentStats buildEnrollmentStats(List<AcademyEnrollment> enrollments) {
        long inProgress = enrollments.stream().filter(enrollment -> enrollment.getStatus() == AcademyEnrollment.Status.IN_PROGRESS).count();
        long completed = enrollments.stream().filter(enrollment -> enrollment.getStatus() == AcademyEnrollment.Status.COMPLETED).count();
        long cancelled = enrollments.stream().filter(enrollment -> enrollment.getStatus() == AcademyEnrollment.Status.CANCELLED).count();
        long enrolled = enrollments.stream().filter(enrollment -> enrollment.getStatus() == AcademyEnrollment.Status.ENROLLED).count();
        int averageProgress = enrollments.isEmpty()
                ? 0
                : (int) Math.round(enrollments.stream().mapToInt(AcademyEnrollment::getProgressPercent).average().orElse(0));
        return new AcademyEnrollmentStats(enrollments.size(), enrolled, inProgress, completed, cancelled, averageProgress);
    }

    private AcademyAssessmentStats buildAssessmentStats(List<AcademyAssessmentAttempt> attempts) {
        long submitted = attempts.stream().filter(attempt -> attempt.getStatus() == AcademyAssessmentAttempt.Status.SUBMITTED).count();
        long passed = attempts.stream().filter(attempt -> attempt.getStatus() == AcademyAssessmentAttempt.Status.SUBMITTED && attempt.isPassed()).count();
        long failed = submitted - passed;
        int passRate = submitted > 0 ? (int) Math.round((passed * 100.0d) / submitted) : 0;
        int averageScore = attempts.stream()
                .filter(attempt -> attempt.getStatus() == AcademyAssessmentAttempt.Status.SUBMITTED)
                .mapToInt(AcademyAssessmentAttempt::getPercentScore)
                .filter(score -> score >= 0)
                .average()
                .stream()
                .mapToInt(value -> (int) Math.round(value))
                .findFirst()
                .orElse(0);
        return new AcademyAssessmentStats(attempts.size(), submitted, passed, failed, passRate, averageScore);
    }

    private AcademyCertificateStats buildCertificateStats(List<AcademyCertificate> certificates) {
        long active = certificates.stream().filter(certificate -> certificate.getStatus() == AcademyCertificate.Status.ACTIVE).count();
        long revoked = certificates.stream().filter(certificate -> certificate.getStatus() == AcademyCertificate.Status.REVOKED).count();
        return new AcademyCertificateStats(certificates.size(), active, revoked);
    }

    private AcademyLeadStats buildLeadStats(List<AcademyLead> leads) {
        long fresh = leads.stream().filter(lead -> lead.getStatus() == AcademyLead.Status.NEW).count();
        long contacted = leads.stream().filter(lead -> lead.getStatus() == AcademyLead.Status.CONTACTED).count();
        long enrolled = leads.stream().filter(lead -> lead.getStatus() == AcademyLead.Status.ENROLLED).count();
        long discarded = leads.stream().filter(lead -> lead.getStatus() == AcademyLead.Status.DISCARDED).count();
        int conversionRate = leads.isEmpty() ? 0 : (int) Math.round((enrolled * 100.0d) / leads.size());
        return new AcademyLeadStats(leads.size(), fresh, contacted, enrolled, discarded, conversionRate);
    }

    private AcademyCoursePerformanceRow buildCoursePerformanceRow(AcademyCourse course,
                                                                  Map<Long, List<AcademyEnrollment>> enrollmentsByCourse,
                                                                  Map<Long, Long> lessonsByCourse,
                                                                  Map<Long, Long> leadsByCourse,
                                                                  Map<Long, Long> certificatesByCourse) {
        Long courseId = course.getId();
        List<AcademyEnrollment> courseEnrollments = enrollmentsByCourse.getOrDefault(courseId, List.of());
        long completed = courseEnrollments.stream().filter(AcademyEnrollment::isCompleted).count();
        int averageProgress = courseEnrollments.isEmpty()
                ? 0
                : (int) Math.round(courseEnrollments.stream().mapToInt(AcademyEnrollment::getProgressPercent).average().orElse(0));
        return new AcademyCoursePerformanceRow(
                courseId,
                text(course.getTitle(), "Curso sin título"),
                text(course.getCategory(), "Sin categoría"),
                course.getStatusLabel(),
                course.isFeatured(),
                lessonsByCourse.getOrDefault(courseId, 0L),
                courseEnrollments.size(),
                completed,
                averageProgress,
                leadsByCourse.getOrDefault(courseId, 0L),
                certificatesByCourse.getOrDefault(courseId, 0L),
                course.getSlug()
        );
    }

    private List<AcademyRecentActivityRow> buildRecentActivity(List<AcademyEnrollment> enrollments,
                                                               List<AcademyAssessmentAttempt> attempts,
                                                               List<AcademyCertificate> certificates,
                                                               List<AcademyLead> leads) {
        List<AcademyRecentActivityRow> rows = new ArrayList<>();

        enrollments.stream().limit(25).forEach(enrollment -> rows.add(new AcademyRecentActivityRow(
                safeDate(enrollment.getUpdatedAt(), enrollment.getStartedAt()),
                "Inscripción",
                icon("person-check"),
                text(enrollment.getStudent() != null ? enrollment.getStudent().getUsername() : null, "Alumno"),
                text(enrollment.getCourse() != null ? enrollment.getCourse().getTitle() : null, "Curso"),
                enrollment.getStatusLabel() + " · avance " + enrollment.getProgressPercent() + "%"
        )));

        attempts.stream().limit(25).forEach(attempt -> rows.add(new AcademyRecentActivityRow(
                safeDate(attempt.getSubmittedAt(), attempt.getStartedAt()),
                "Evaluación",
                icon("ui-checks"),
                text(attempt.getStudent() != null ? attempt.getStudent().getUsername() : null, "Alumno"),
                text(attempt.getAssessment() != null ? attempt.getAssessment().getTitle() : null, "Evaluación"),
                (attempt.isPassed() ? "Aprobado" : "Revisado") + " · " + attempt.getPercentScore() + "%"
        )));

        certificates.stream().limit(25).forEach(certificate -> rows.add(new AcademyRecentActivityRow(
                certificate.getIssuedAt(),
                "Certificado",
                icon("award"),
                text(certificate.getStudent() != null ? certificate.getStudent().getUsername() : null, "Alumno"),
                text(certificate.getCourse() != null ? certificate.getCourse().getTitle() : null, "Curso"),
                certificate.getStatusLabel() + " · " + text(certificate.getCertificateCode(), "Sin código")
        )));

        leads.stream().limit(25).forEach(lead -> rows.add(new AcademyRecentActivityRow(
                safeDate(lead.getUpdatedAt(), lead.getCreatedAt()),
                "Interesado",
                icon("person-lines-fill"),
                text(lead.getFullName(), "Interesado"),
                text(lead.getCourse() != null ? lead.getCourse().getTitle() : null, "Curso"),
                lead.getStatusLabel() + " · " + lead.getSourceLabel()
        )));

        return rows.stream()
                .filter(row -> row.date() != null)
                .sorted(Comparator.comparing(AcademyRecentActivityRow::date).reversed())
                .limit(12)
                .toList();
    }

    private List<AcademyLeadStatusRow> buildLeadStatusRows(List<AcademyLead> leads) {
        Map<AcademyLead.Status, Long> counts = new HashMap<>();
        for (AcademyLead.Status status : AcademyLead.Status.values()) {
            counts.put(status, leads.stream().filter(lead -> lead.getStatus() == status).count());
        }
        long total = leads.size();
        List<AcademyLeadStatusRow> rows = new ArrayList<>();
        for (AcademyLead.Status status : AcademyLead.Status.values()) {
            long count = counts.getOrDefault(status, 0L);
            int percent = total > 0 ? (int) Math.round((count * 100.0d) / total) : 0;
            rows.add(new AcademyLeadStatusRow(status.name(), status.getLabel(), count, percent));
        }
        return rows;
    }

    private List<AcademyEnrollmentStatusRow> buildEnrollmentStatusRows(List<AcademyEnrollment> enrollments) {
        long total = enrollments.size();
        List<AcademyEnrollmentStatusRow> rows = new ArrayList<>();
        for (AcademyEnrollment.Status status : AcademyEnrollment.Status.values()) {
            long count = enrollments.stream().filter(enrollment -> enrollment.getStatus() == status).count();
            int percent = total > 0 ? (int) Math.round((count * 100.0d) / total) : 0;
            rows.add(new AcademyEnrollmentStatusRow(status.name(), status.getLabel(), count, percent));
        }
        return rows;
    }

    private String text(String value, String fallback) {
        return value != null && !value.isBlank() ? value : fallback;
    }

    private String icon(String name) {
        return "bi bi-" + name;
    }

    private LocalDateTime safeDate(LocalDateTime preferred, LocalDateTime fallback) {
        return Objects.requireNonNullElse(preferred, fallback);
    }

    public record AcademyDashboardSummary(
            AcademyCourseStats courseStats,
            AcademyEnrollmentStats enrollmentStats,
            AcademyAssessmentStats assessmentStats,
            AcademyCertificateStats certificateStats,
            AcademyLeadStats leadStats,
            List<AcademyCoursePerformanceRow> topCourses,
            List<AcademyLeadStatusRow> leadStatusRows,
            List<AcademyEnrollmentStatusRow> enrollmentStatusRows,
            List<AcademyRecentActivityRow> recentActivity
    ) {
    }

    public record AcademyCourseStats(long totalCourses, long publishedCourses, long draftCourses, long archivedCourses,
                                     long featuredCourses, long totalLessons, long previewLessons) {
    }

    public record AcademyEnrollmentStats(long totalEnrollments, long enrolled, long inProgress, long completed,
                                         long cancelled, int averageProgress) {
    }

    public record AcademyAssessmentStats(long totalAttempts, long submittedAttempts, long passedAttempts,
                                         long failedAttempts, int passRate, int averageScore) {
    }

    public record AcademyCertificateStats(long totalCertificates, long activeCertificates, long revokedCertificates) {
    }

    public record AcademyLeadStats(long totalLeads, long freshLeads, long contactedLeads, long enrolledLeads,
                                   long discardedLeads, int conversionRate) {
    }

    public record AcademyCoursePerformanceRow(Long courseId, String title, String category, String statusLabel,
                                              boolean featured, long lessons, long enrollments, long completed,
                                              int averageProgress, long leads, long certificates, String slug) {
    }

    public record AcademyLeadStatusRow(String status, String label, long count, int percent) {
    }

    public record AcademyEnrollmentStatusRow(String status, String label, long count, int percent) {
    }

    public record AcademyRecentActivityRow(LocalDateTime date, String type, String iconClass, String actor,
                                           String target, String detail) {
    }
}
