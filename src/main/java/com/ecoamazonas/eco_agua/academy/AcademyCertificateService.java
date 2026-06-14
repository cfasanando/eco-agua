package com.ecoamazonas.eco_agua.academy;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

@Service
public class AcademyCertificateService {

    private static final int REQUIRED_PROGRESS = 100;

    private final AcademyCertificateRepository certificateRepository;
    private final AcademyEnrollmentRepository enrollmentRepository;
    private final AcademyAssessmentRepository assessmentRepository;
    private final AcademyAssessmentAttemptRepository attemptRepository;

    public AcademyCertificateService(AcademyCertificateRepository certificateRepository,
                                     AcademyEnrollmentRepository enrollmentRepository,
                                     AcademyAssessmentRepository assessmentRepository,
                                     AcademyAssessmentAttemptRepository attemptRepository) {
        this.certificateRepository = certificateRepository;
        this.enrollmentRepository = enrollmentRepository;
        this.assessmentRepository = assessmentRepository;
        this.attemptRepository = attemptRepository;
    }

    @Transactional(readOnly = true)
    public List<CertificateAdminRow> findAdminRows() {
        return certificateRepository.findAllForAdmin().stream()
                .map(this::toAdminRow)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<CertificateCandidateRow> findCandidateRows() {
        return enrollmentRepository.findAllForAdmin().stream()
                .filter(AcademyEnrollment::isActiveEnrollment)
                .map(enrollment -> new CertificateCandidateRow(
                        enrollment.getId(),
                        username(enrollment),
                        courseTitle(enrollment),
                        courseSlug(enrollment),
                        enrollment.getStatusLabel(),
                        enrollment.getProgressPercent(),
                        buildEligibility(enrollment),
                        certificateRepository.findByEnrollment(enrollment).map(AcademyCertificate::getCertificateCode).orElse("")))
                .toList();
    }

    @Transactional(readOnly = true)
    public StudentCertificateStatus buildStudentStatus(AcademyEnrollment enrollment) {
        if (enrollment == null) {
            return StudentCertificateStatus.unavailable("Inscripción no disponible.");
        }
        Optional<AcademyCertificate> existing = certificateRepository.findByEnrollment(enrollment);
        CertificateEligibility eligibility = buildEligibility(enrollment);
        return new StudentCertificateStatus(
                existing.map(AcademyCertificate::getCertificateCode).orElse(""),
                existing.map(AcademyCertificate::getStatusLabel).orElse(""),
                existing.map(AcademyCertificate::isActiveCertificate).orElse(false),
                existing.map(AcademyCertificate::getIssuedAt).orElse(null),
                eligibility.eligible(),
                eligibility.message(),
                eligibility.progressPercent(),
                eligibility.requiredAssessments(),
                eligibility.passedAssessments());
    }

    @Transactional
    public AcademyCertificate issueForStudent(String username, String courseSlug) {
        AcademyEnrollment enrollment = enrollmentRepository.findByStudentUsernameAndCourseSlug(username, courseSlug)
                .filter(AcademyEnrollment::isActiveEnrollment)
                .orElseThrow();
        return issue(enrollment);
    }

    @Transactional
    public AcademyCertificate issueForEnrollment(Long enrollmentId) {
        AcademyEnrollment enrollment = enrollmentRepository.findById(enrollmentId).orElseThrow();
        return issue(enrollment);
    }

    @Transactional
    public void revoke(Long certificateId, String notes) {
        certificateRepository.findById(certificateId).ifPresent(certificate -> {
            certificate.setStatus(AcademyCertificate.Status.REVOKED);
            certificate.setRevokedAt(LocalDateTime.now());
            certificate.setVerificationNotes(clean(notes));
            certificateRepository.save(certificate);
        });
    }

    @Transactional(readOnly = true)
    public CertificateVerificationView findVerification(String code) {
        if (code == null || code.isBlank()) {
            return CertificateVerificationView.invalid(code);
        }
        return certificateRepository.findByCertificateCodeForVerification(code.trim())
                .map(this::toVerificationView)
                .orElseGet(() -> CertificateVerificationView.invalid(code));
    }

    @Transactional(readOnly = true)
    public Optional<AcademyCertificate> findStudentCertificate(String username, String courseSlug) {
        AcademyEnrollment enrollment = enrollmentRepository.findByStudentUsernameAndCourseSlug(username, courseSlug)
                .filter(AcademyEnrollment::isActiveEnrollment)
                .orElse(null);
        if (enrollment == null) {
            return Optional.empty();
        }
        return certificateRepository.findByEnrollment(enrollment);
    }

    private AcademyCertificate issue(AcademyEnrollment enrollment) {
        AcademyCertificate existing = certificateRepository.findByEnrollment(enrollment).orElse(null);
        if (existing != null) {
            if (existing.getStatus() == AcademyCertificate.Status.REVOKED) {
                existing.setStatus(AcademyCertificate.Status.ACTIVE);
                existing.setRevokedAt(null);
                existing.setVerificationNotes("Certificado reactivado.");
                return certificateRepository.save(existing);
            }
            return existing;
        }

        CertificateEligibility eligibility = buildEligibility(enrollment);
        if (!eligibility.eligible()) {
            throw new IllegalStateException(eligibility.message());
        }

        AcademyCertificate certificate = new AcademyCertificate();
        certificate.setEnrollment(enrollment);
        certificate.setStudent(enrollment.getStudent());
        certificate.setCourse(enrollment.getCourse());
        certificate.setCertificateCode(generateCode(enrollment));
        certificate.setStatus(AcademyCertificate.Status.ACTIVE);
        certificate.setIssuedAt(LocalDateTime.now());
        certificate.setVerificationNotes("Certificado emitido automáticamente por cumplimiento del curso.");
        return certificateRepository.save(certificate);
    }

    private CertificateEligibility buildEligibility(AcademyEnrollment enrollment) {
        if (enrollment == null || enrollment.getCourse() == null) {
            return new CertificateEligibility(false, "No hay inscripción válida.", 0, 0, 0);
        }
        int progress = enrollment.getProgressPercent();
        long requiredAssessments = assessmentRepository.countByCourseAndActiveTrueAndStatus(
                enrollment.getCourse(), AcademyAssessment.Status.PUBLISHED);
        long passedAssessments = attemptRepository.countPassedAssessmentsByEnrollment(enrollment);

        if (progress < REQUIRED_PROGRESS) {
            return new CertificateEligibility(
                    false,
                    "Completa el 100% de las lecciones para emitir el certificado.",
                    progress,
                    Math.toIntExact(Math.min(requiredAssessments, Integer.MAX_VALUE)),
                    Math.toIntExact(Math.min(passedAssessments, Integer.MAX_VALUE)));
        }
        if (requiredAssessments > 0 && passedAssessments < requiredAssessments) {
            return new CertificateEligibility(
                    false,
                    "Aprueba todas las evaluaciones publicadas del curso para emitir el certificado.",
                    progress,
                    Math.toIntExact(Math.min(requiredAssessments, Integer.MAX_VALUE)),
                    Math.toIntExact(Math.min(passedAssessments, Integer.MAX_VALUE)));
        }
        return new CertificateEligibility(
                true,
                "Requisitos cumplidos. El certificado puede emitirse.",
                progress,
                Math.toIntExact(Math.min(requiredAssessments, Integer.MAX_VALUE)),
                Math.toIntExact(Math.min(passedAssessments, Integer.MAX_VALUE)));
    }

    private String generateCode(AcademyEnrollment enrollment) {
        String yearMonth = DateTimeFormatter.ofPattern("yyyyMM").format(LocalDateTime.now());
        String coursePart = enrollment.getCourse() != null && enrollment.getCourse().getSlug() != null
                ? sanitizeCodePart(enrollment.getCourse().getSlug())
                : "CURSO";
        coursePart = coursePart.length() > 12 ? coursePart.substring(0, 12) : coursePart;
        for (int i = 0; i < 10; i++) {
            String random = UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase(Locale.ROOT);
            String code = "ACA-" + yearMonth + "-" + coursePart + "-" + random;
            if (!certificateRepository.existsByCertificateCode(code)) {
                return code;
            }
        }
        return "ACA-" + yearMonth + "-" + UUID.randomUUID().toString().replace("-", "").substring(0, 16).toUpperCase(Locale.ROOT);
    }

    private String sanitizeCodePart(String value) {
        return value == null ? "CURSO" : value.toUpperCase(Locale.ROOT).replaceAll("[^A-Z0-9]", "");
    }

    private CertificateAdminRow toAdminRow(AcademyCertificate certificate) {
        return new CertificateAdminRow(
                certificate.getId(),
                certificate.getCertificateCode(),
                username(certificate.getStudent()),
                courseTitle(certificate.getCourse()),
                courseSlug(certificate.getCourse()),
                certificate.getStatusLabel(),
                certificate.isActiveCertificate(),
                certificate.getIssuedAt(),
                certificate.getRevokedAt(),
                certificate.getPublicVerificationPath());
    }

    private CertificateVerificationView toVerificationView(AcademyCertificate certificate) {
        boolean valid = certificate.isActiveCertificate();
        return new CertificateVerificationView(
                certificate.getCertificateCode(),
                valid,
                valid ? "Certificado válido" : "Certificado anulado",
                username(certificate.getStudent()),
                courseTitle(certificate.getCourse()),
                certificate.getCourse() != null ? certificate.getCourse().getInstructor() : "",
                certificate.getIssuedAt(),
                certificate.getStatusLabel(),
                certificate.getVerificationNotes());
    }

    private String username(AcademyEnrollment enrollment) {
        return enrollment != null ? username(enrollment.getStudent()) : "-";
    }

    private String username(com.ecoamazonas.eco_agua.user.UserAccount user) {
        return user != null ? defaultIfBlank(user.getUsername(), "-") : "-";
    }

    private String courseTitle(AcademyEnrollment enrollment) {
        return enrollment != null ? courseTitle(enrollment.getCourse()) : "-";
    }

    private String courseTitle(AcademyCourse course) {
        return course != null ? defaultIfBlank(course.getTitle(), "Curso") : "Curso";
    }

    private String courseSlug(AcademyEnrollment enrollment) {
        return enrollment != null ? courseSlug(enrollment.getCourse()) : "";
    }

    private String courseSlug(AcademyCourse course) {
        return course != null ? defaultIfBlank(course.getSlug(), "") : "";
    }

    private String clean(String value) {
        return value != null ? value.trim() : "";
    }

    private String defaultIfBlank(String value, String fallback) {
        return value != null && !value.isBlank() ? value.trim() : fallback;
    }

    public record CertificateEligibility(boolean eligible,
                                         String message,
                                         int progressPercent,
                                         int requiredAssessments,
                                         int passedAssessments) {
    }

    public record StudentCertificateStatus(String certificateCode,
                                           String certificateStatusLabel,
                                           boolean certificateActive,
                                           LocalDateTime issuedAt,
                                           boolean eligible,
                                           String message,
                                           int progressPercent,
                                           int requiredAssessments,
                                           int passedAssessments) {
        static StudentCertificateStatus unavailable(String message) {
            return new StudentCertificateStatus("", "", false, null, false, message, 0, 0, 0);
        }

        public boolean hasCertificate() {
            return certificateCode != null && !certificateCode.isBlank();
        }
    }

    public record CertificateAdminRow(Long certificateId,
                                      String certificateCode,
                                      String username,
                                      String courseTitle,
                                      String courseSlug,
                                      String statusLabel,
                                      boolean active,
                                      LocalDateTime issuedAt,
                                      LocalDateTime revokedAt,
                                      String publicPath) {
    }

    public record CertificateCandidateRow(Long enrollmentId,
                                          String username,
                                          String courseTitle,
                                          String courseSlug,
                                          String enrollmentStatusLabel,
                                          int progressPercent,
                                          CertificateEligibility eligibility,
                                          String certificateCode) {
        public boolean hasCertificate() {
            return certificateCode != null && !certificateCode.isBlank();
        }
    }

    public record CertificateVerificationView(String certificateCode,
                                              boolean valid,
                                              String statusMessage,
                                              String username,
                                              String courseTitle,
                                              String instructor,
                                              LocalDateTime issuedAt,
                                              String statusLabel,
                                              String notes) {
        static CertificateVerificationView invalid(String code) {
            return new CertificateVerificationView(code != null ? code : "", false, "Certificado no encontrado", "", "", "", null, "No válido", "");
        }
    }
}
