package com.ecoamazonas.eco_agua.academy;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;
import java.util.Map;

@Controller
@RequestMapping("/my-courses")
public class AcademyStudentController {

    private final AcademyEnrollmentService enrollmentService;
    private final AcademyCourseContentService contentService;
    private final AcademyAssessmentService assessmentService;
    private final AcademyCertificateService certificateService;

    public AcademyStudentController(AcademyEnrollmentService enrollmentService,
                                    AcademyCourseContentService contentService,
                                    AcademyAssessmentService assessmentService,
                                    AcademyCertificateService certificateService) {
        this.enrollmentService = enrollmentService;
        this.contentService = contentService;
        this.assessmentService = assessmentService;
        this.certificateService = certificateService;
    }

    @GetMapping
    public String myCourses(Principal principal, Model model) {
        String username = username(principal);
        model.addAttribute("activePage", "my_academy_courses");
        model.addAttribute("enrollments", enrollmentService.findMyCourses(username).stream()
                .map(StudentCourseCard::from)
                .toList());
        return "academy/my_courses";
    }

    @GetMapping("/{courseSlug}")
    public String myCourse(@PathVariable String courseSlug,
                           Principal principal,
                           Model model) {
        AcademyEnrollment enrollment = enrollmentService.findMyEnrollment(username(principal), courseSlug);
        if (enrollment == null) {
            return "redirect:/academy/course/" + courseSlug;
        }

        AcademyCourse course = enrollment.getCourse();
        AcademyLesson firstLesson = enrollmentService.findFirstPublishedLesson(course);

        model.addAttribute("activePage", "my_academy_courses");
        model.addAttribute("enrollment", enrollment);
        model.addAttribute("course", course);
        model.addAttribute("curriculum", contentService.findPublishedModuleViews(course));
        model.addAttribute("completedLessonIds", enrollmentService.findCompletedLessonIds(enrollment));
        model.addAttribute("firstLesson", firstLesson);
        model.addAttribute("assessments", assessmentService.findStudentAssessmentCards(enrollment));
        model.addAttribute("certificateStatus", certificateService.buildStudentStatus(enrollment));
        return "academy/my_course_detail";
    }

    @GetMapping("/{courseSlug}/lesson/{lessonId}")
    public String myLesson(@PathVariable String courseSlug,
                           @PathVariable Long lessonId,
                           Principal principal,
                           Model model,
                           RedirectAttributes redirectAttributes) {
        AcademyEnrollment enrollment = enrollmentService.findMyEnrollment(username(principal), courseSlug);
        if (enrollment == null) {
            return "redirect:/academy/course/" + courseSlug;
        }

        AcademyLesson lesson = enrollmentService.findPublishedLessonForEnrollment(enrollment, lessonId);
        if (lesson == null) {
            redirectAttributes.addFlashAttribute("warningMessage", "La lección no está disponible.");
            return "redirect:/my-courses/" + courseSlug;
        }

        enrollmentService.registerLessonView(enrollment, lesson);

        model.addAttribute("activePage", "my_academy_courses");
        model.addAttribute("enrollment", enrollment);
        model.addAttribute("course", enrollment.getCourse());
        model.addAttribute("selectedLesson", lesson);
        model.addAttribute("curriculum", contentService.findPublishedModuleViews(enrollment.getCourse()));
        model.addAttribute("completedLessonIds", enrollmentService.findCompletedLessonIds(enrollment));
        return "academy/my_lesson";
    }

    @PostMapping("/{courseSlug}/lesson/{lessonId}/complete")
    public String completeLesson(@PathVariable String courseSlug,
                                 @PathVariable Long lessonId,
                                 Principal principal,
                                 RedirectAttributes redirectAttributes) {
        enrollmentService.markLessonCompleted(username(principal), courseSlug, lessonId);
        redirectAttributes.addFlashAttribute("successMessage", "Lección marcada como completada.");
        return "redirect:/my-courses/" + courseSlug + "/lesson/" + lessonId;
    }


    @PostMapping("/{courseSlug}/certificate/generate")
    public String generateCertificate(@PathVariable String courseSlug,
                                      Principal principal,
                                      RedirectAttributes redirectAttributes) {
        try {
            AcademyCertificate certificate = certificateService.issueForStudent(username(principal), courseSlug);
            redirectAttributes.addFlashAttribute("successMessage", "Certificado emitido correctamente.");
            return "redirect:/my-courses/" + courseSlug + "/certificate";
        } catch (IllegalStateException e) {
            redirectAttributes.addFlashAttribute("warningMessage", e.getMessage());
            return "redirect:/my-courses/" + courseSlug;
        }
    }

    @GetMapping("/{courseSlug}/certificate")
    public String myCertificate(@PathVariable String courseSlug,
                                Principal principal,
                                Model model,
                                RedirectAttributes redirectAttributes) {
        AcademyEnrollment enrollment = enrollmentService.findMyEnrollment(username(principal), courseSlug);
        if (enrollment == null) {
            return "redirect:/academy/course/" + courseSlug;
        }
        AcademyCertificate certificate = certificateService.findStudentCertificate(username(principal), courseSlug)
                .orElse(null);
        if (certificate == null) {
            redirectAttributes.addFlashAttribute("warningMessage", "Aún no tienes certificado emitido para este curso.");
            return "redirect:/my-courses/" + courseSlug;
        }
        model.addAttribute("activePage", "my_academy_courses");
        model.addAttribute("course", enrollment.getCourse());
        model.addAttribute("enrollment", enrollment);
        model.addAttribute("certificate", certificate);
        return "academy/my_certificate";
    }

    @GetMapping("/{courseSlug}/assessment")
    public String myAssessments(@PathVariable String courseSlug,
                                Principal principal,
                                Model model) {
        AcademyEnrollment enrollment = enrollmentService.findMyEnrollment(username(principal), courseSlug);
        if (enrollment == null) {
            return "redirect:/academy/course/" + courseSlug;
        }
        model.addAttribute("activePage", "my_academy_courses");
        model.addAttribute("course", enrollment.getCourse());
        model.addAttribute("enrollment", enrollment);
        model.addAttribute("assessments", assessmentService.findStudentAssessmentCards(enrollment));
        return "academy/my_assessments";
    }

    @GetMapping("/{courseSlug}/assessment/{assessmentId}")
    public String takeAssessment(@PathVariable String courseSlug,
                                 @PathVariable Long assessmentId,
                                 @RequestParam(required = false) Long attemptId,
                                 Principal principal,
                                 Model model,
                                 RedirectAttributes redirectAttributes) {
        AcademyEnrollment enrollment = enrollmentService.findMyEnrollment(username(principal), courseSlug);
        if (enrollment == null) {
            return "redirect:/academy/course/" + courseSlug;
        }
        AcademyAssessmentService.AssessmentTakeView assessmentView = assessmentService.buildTakeView(enrollment, assessmentId, attemptId);
        if (assessmentView == null) {
            redirectAttributes.addFlashAttribute("warningMessage", "La evaluación no está disponible.");
            return "redirect:/my-courses/" + courseSlug;
        }
        model.addAttribute("activePage", "my_academy_courses");
        model.addAttribute("course", enrollment.getCourse());
        model.addAttribute("enrollment", enrollment);
        model.addAttribute("assessmentView", assessmentView);
        return "academy/my_assessment";
    }

    @PostMapping("/{courseSlug}/assessment/{assessmentId}/submit")
    public String submitAssessment(@PathVariable String courseSlug,
                                   @PathVariable Long assessmentId,
                                   @RequestParam Map<String, String> parameters,
                                   Principal principal,
                                   RedirectAttributes redirectAttributes) {
        try {
            Long attemptId = assessmentService.submitAssessment(username(principal), courseSlug, assessmentId, parameters);
            redirectAttributes.addFlashAttribute("successMessage", "Evaluación enviada correctamente.");
            return "redirect:/my-courses/" + courseSlug + "/assessment/" + assessmentId + "?attemptId=" + attemptId;
        } catch (IllegalStateException e) {
            redirectAttributes.addFlashAttribute("warningMessage", "Ya no tienes intentos disponibles para esta evaluación.");
            return "redirect:/my-courses/" + courseSlug + "/assessment/" + assessmentId;
        }
    }

    private String username(Principal principal) {
        return principal != null ? principal.getName() : "";
    }

    public record StudentCourseCard(
            String courseSlug,
            String courseTitle,
            String courseCategory,
            String shortDescription,
            String coverImageUrl,
            String statusLabel,
            int progressPercent,
            int completedLessons,
            int totalLessons
    ) {
        static StudentCourseCard from(AcademyEnrollment enrollment) {
            AcademyCourse course = enrollment.getCourse();
            AcademyEnrollment.Status status = enrollment.getStatus() != null
                    ? enrollment.getStatus()
                    : AcademyEnrollment.Status.ENROLLED;
            return new StudentCourseCard(
                    course != null ? valueOrEmpty(course.getSlug()) : "",
                    course != null ? valueOrEmpty(course.getTitle()) : "Curso no disponible",
                    course != null ? valueOrEmpty(course.getCategory()) : "-",
                    course != null ? valueOrEmpty(course.getShortDescription()) : "",
                    course != null ? valueOrEmpty(course.getCoverImageUrl()) : "",
                    status.getLabel(),
                    enrollment.getProgressPercent(),
                    enrollment.getCompletedLessons(),
                    enrollment.getTotalLessons()
            );
        }
    }

    private static String valueOrEmpty(String value) {
        return value != null ? value : "";
    }
}
