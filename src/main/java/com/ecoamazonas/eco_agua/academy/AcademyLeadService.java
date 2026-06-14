package com.ecoamazonas.eco_agua.academy;

import com.ecoamazonas.eco_agua.user.UserAccount;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

@Service
public class AcademyLeadService {

    private final AcademyLeadRepository leadRepository;
    private final AcademyCourseRepository courseRepository;
    private final AcademyEnrollmentService enrollmentService;

    public AcademyLeadService(AcademyLeadRepository leadRepository,
                              AcademyCourseRepository courseRepository,
                              AcademyEnrollmentService enrollmentService) {
        this.leadRepository = leadRepository;
        this.courseRepository = courseRepository;
        this.enrollmentService = enrollmentService;
    }

    @Transactional
    public AcademyLead createPublicRequest(String courseSlug, AcademyLeadRequestForm form, String originPath) {
        AcademyCourse course = courseRepository.findBySlugAndActiveTrueAndStatus(courseSlug, AcademyCourse.Status.PUBLISHED)
                .orElseThrow(() -> new IllegalArgumentException("Curso no disponible."));

        AcademyLead lead = new AcademyLead();
        lead.setCourse(course);
        lead.setFullName(required(form != null ? form.getFullName() : null, "Interesado"));
        lead.setPhone(clean(form != null ? form.getPhone() : null));
        lead.setEmail(clean(form != null ? form.getEmail() : null));
        lead.setSource(form != null && form.getSource() != null ? form.getSource() : AcademyLead.Source.CATALOG);
        lead.setStatus(AcademyLead.Status.NEW);
        lead.setPublicMessage(clean(form != null ? form.getPublicMessage() : null));
        lead.setOriginPath(clean(originPath));
        return leadRepository.save(lead);
    }

    @Transactional(readOnly = true)
    public List<LeadAdminRow> findAdminRows() {
        return leadRepository.findAllForAdmin().stream()
                .map(this::toAdminRow)
                .toList();
    }

    @Transactional(readOnly = true)
    public LeadDetailView findDetail(Long id) {
        AcademyLead lead = leadRepository.findByIdForAdmin(id).orElseThrow();
        return new LeadDetailView(toAdminRow(lead), lead.getPublicMessage(), lead.getInternalNotes());
    }

    @Transactional(readOnly = true)
    public List<UserAccount> findActiveUsers() {
        return enrollmentService.findActiveUsers();
    }

    @Transactional(readOnly = true)
    public List<AcademyCourse> findCoursesForEnrollment() {
        return courseRepository.findPublishedForCatalog().stream()
                .sorted(Comparator.comparing(AcademyCourse::getTitle, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    @Transactional(readOnly = true)
    public AcademyLeadSummary summary() {
        return new AcademyLeadSummary(
                leadRepository.count(),
                leadRepository.countByStatus(AcademyLead.Status.NEW),
                leadRepository.countByStatus(AcademyLead.Status.CONTACTED),
                leadRepository.countByStatus(AcademyLead.Status.ENROLLED),
                leadRepository.countByStatus(AcademyLead.Status.DISCARDED)
        );
    }

    @Transactional
    public void updateStatus(Long id, AcademyLead.Status status, String notes) {
        AcademyLead lead = leadRepository.findByIdForAdmin(id).orElseThrow();
        AcademyLead.Status nextStatus = status != null ? status : AcademyLead.Status.NEW;
        lead.setStatus(nextStatus);
        if (AcademyLead.Status.CONTACTED.equals(nextStatus) && lead.getContactedAt() == null) {
            lead.setContactedAt(LocalDateTime.now());
        }
        if (notes != null) {
            lead.setInternalNotes(clean(notes));
        }
        leadRepository.save(lead);
    }

    @Transactional
    public void saveNotes(Long id, String notes) {
        AcademyLead lead = leadRepository.findByIdForAdmin(id).orElseThrow();
        lead.setInternalNotes(clean(notes));
        leadRepository.save(lead);
    }

    @Transactional
    public AcademyEnrollment convertToEnrollment(Long id, Integer studentId, Long courseId, String notes) {
        AcademyLead lead = leadRepository.findByIdForAdmin(id).orElseThrow();
        Long finalCourseId = courseId != null ? courseId : (lead.getCourse() != null ? lead.getCourse().getId() : null);
        if (studentId == null || finalCourseId == null) {
            throw new IllegalArgumentException("Selecciona usuario y curso para convertir el interesado.");
        }

        AcademyEnrollment enrollment = enrollmentService.enroll(studentId, finalCourseId, buildEnrollmentNotes(lead, notes));
        lead.setEnrollment(enrollment);
        lead.setStudent(enrollment.getStudent());
        lead.setCourse(enrollment.getCourse());
        lead.setStatus(AcademyLead.Status.ENROLLED);
        lead.setConvertedAt(LocalDateTime.now());
        if (lead.getContactedAt() == null) {
            lead.setContactedAt(LocalDateTime.now());
        }
        if (notes != null && !notes.isBlank()) {
            lead.setInternalNotes(clean(notes));
        }
        leadRepository.save(lead);
        return enrollment;
    }

    public String buildWhatsappUrl(LeadAdminRow row) {
        if (row == null || row.cleanPhone().isBlank()) {
            return "";
        }
        String message = "Hola " + row.fullName() + ", te escribimos por tu solicitud de información sobre el curso " + row.courseTitle() + ".";
        return "https://wa.me/" + row.cleanPhone() + "?text=" + URLEncoder.encode(message, StandardCharsets.UTF_8);
    }

    private LeadAdminRow toAdminRow(AcademyLead lead) {
        AcademyCourse course = lead.getCourse();
        UserAccount student = lead.getStudent();
        AcademyLead.Status status = lead.getStatus() != null ? lead.getStatus() : AcademyLead.Status.NEW;
        AcademyLead.Source source = lead.getSource() != null ? lead.getSource() : AcademyLead.Source.CATALOG;
        return new LeadAdminRow(
                lead.getId(),
                valueOrDefault(lead.getFullName(), "Interesado"),
                valueOrDefault(lead.getPhone(), ""),
                valueOrDefault(lead.getEmail(), ""),
                course != null ? course.getId() : null,
                course != null ? valueOrDefault(course.getTitle(), "Curso") : "Curso",
                course != null ? valueOrDefault(course.getSlug(), "") : "",
                course != null ? valueOrDefault(course.getCategory(), "-") : "-",
                status.name(),
                status.getLabel(),
                source.name(),
                source.getLabel(),
                student != null ? valueOrDefault(student.getUsername(), "") : "",
                lead.getEnrollment() != null ? lead.getEnrollment().getId() : null,
                lead.getCreatedAt(),
                lead.getContactedAt(),
                lead.getConvertedAt(),
                lead.isConverted()
        );
    }

    private String buildEnrollmentNotes(AcademyLead lead, String notes) {
        StringBuilder builder = new StringBuilder();
        builder.append("Inscripción generada desde solicitud de Academia.");
        if (lead.getFullName() != null && !lead.getFullName().isBlank()) {
            builder.append(" Interesado: ").append(lead.getFullName()).append('.');
        }
        if (lead.getPhone() != null && !lead.getPhone().isBlank()) {
            builder.append(" Teléfono: ").append(lead.getPhone()).append('.');
        }
        if (notes != null && !notes.isBlank()) {
            builder.append(" Nota: ").append(notes.trim());
        }
        return builder.toString();
    }

    private String clean(String value) {
        return value != null ? value.trim() : "";
    }

    private String required(String value, String fallback) {
        String cleanValue = clean(value);
        return cleanValue.isBlank() ? fallback : cleanValue;
    }

    private String valueOrDefault(String value, String fallback) {
        String cleanValue = clean(value);
        return cleanValue.isBlank() ? fallback : cleanValue;
    }

    public record LeadAdminRow(Long id,
                               String fullName,
                               String phone,
                               String email,
                               Long courseId,
                               String courseTitle,
                               String courseSlug,
                               String courseCategory,
                               String statusValue,
                               String statusLabel,
                               String sourceValue,
                               String sourceLabel,
                               String studentUsername,
                               Long enrollmentId,
                               LocalDateTime createdAt,
                               LocalDateTime contactedAt,
                               LocalDateTime convertedAt,
                               boolean converted) {
        public String cleanPhone() {
            return phone != null ? phone.replaceAll("[^0-9]", "") : "";
        }

        public boolean hasPhone() {
            return !cleanPhone().isBlank();
        }
    }

    public record LeadDetailView(LeadAdminRow row, String publicMessage, String internalNotes) {
    }

    public record AcademyLeadSummary(long total, long news, long contacted, long enrolled, long discarded) {
    }
}
