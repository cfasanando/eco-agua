package com.ecoamazonas.eco_agua.academy;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.format.DateTimeFormatter;
import java.util.Arrays;

@Controller
@RequestMapping("/admin/academy/enrollments")
public class AcademyEnrollmentAdminController {

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final AcademyEnrollmentService enrollmentService;

    public AcademyEnrollmentAdminController(AcademyEnrollmentService enrollmentService) {
        this.enrollmentService = enrollmentService;
    }

    @GetMapping
    public String enrollments(Model model) {
        model.addAttribute("activePage", "academy_enrollments");
        model.addAttribute("enrollments", enrollmentService.findAllForAdmin().stream()
                .map(EnrollmentRow::from)
                .toList());
        model.addAttribute("students", enrollmentService.findActiveUsers());
        model.addAttribute("courses", enrollmentService.findCoursesForEnrollment());
        model.addAttribute("statuses", Arrays.stream(AcademyEnrollment.Status.values())
                .map(StatusOption::from)
                .toList());
        return "admin/academy/enrollments";
    }

    @PostMapping("/save")
    public String save(@RequestParam("studentId") Integer studentId,
                       @RequestParam("courseId") Long courseId,
                       @RequestParam(value = "notes", required = false) String notes,
                       RedirectAttributes redirectAttributes) {
        AcademyEnrollment enrollment = enrollmentService.enroll(studentId, courseId, notes);
        String username = enrollment.getStudent() != null ? enrollment.getStudent().getUsername() : "el alumno";
        redirectAttributes.addFlashAttribute("successMessage", "Inscripción guardada para " + username + ".");
        return "redirect:/admin/academy/enrollments";
    }

    @PostMapping("/{id}/status")
    public String updateStatus(@PathVariable Long id,
                               @RequestParam("status") AcademyEnrollment.Status status,
                               RedirectAttributes redirectAttributes) {
        enrollmentService.updateStatus(id, status);
        redirectAttributes.addFlashAttribute("successMessage", "Estado de inscripción actualizado.");
        return "redirect:/admin/academy/enrollments";
    }

    public record EnrollmentRow(
            Long id,
            String studentUsername,
            String notes,
            Long courseId,
            String courseTitle,
            String courseSlug,
            String courseCategory,
            String statusValue,
            String statusLabel,
            int progressPercent,
            int completedLessons,
            int totalLessons,
            String updatedAtLabel
    ) {
        static EnrollmentRow from(AcademyEnrollment enrollment) {
            AcademyCourse course = enrollment.getCourse();
            AcademyEnrollment.Status status = enrollment.getStatus() != null
                    ? enrollment.getStatus()
                    : AcademyEnrollment.Status.ENROLLED;

            return new EnrollmentRow(
                    enrollment.getId(),
                    enrollment.getStudent() != null ? enrollment.getStudent().getUsername() : "-",
                    valueOrEmpty(enrollment.getNotes()),
                    course != null ? course.getId() : null,
                    course != null ? valueOrEmpty(course.getTitle()) : "Curso no disponible",
                    course != null ? valueOrEmpty(course.getSlug()) : "",
                    course != null ? valueOrEmpty(course.getCategory()) : "-",
                    status.name(),
                    status.getLabel(),
                    enrollment.getProgressPercent(),
                    enrollment.getCompletedLessons(),
                    enrollment.getTotalLessons(),
                    enrollment.getUpdatedAt() != null ? enrollment.getUpdatedAt().format(DATE_TIME_FORMATTER) : "-"
            );
        }
    }

    public record StatusOption(String value, String label) {
        static StatusOption from(AcademyEnrollment.Status status) {
            return new StatusOption(status.name(), status.getLabel());
        }
    }

    private static String valueOrEmpty(String value) {
        return value != null ? value : "";
    }
}
