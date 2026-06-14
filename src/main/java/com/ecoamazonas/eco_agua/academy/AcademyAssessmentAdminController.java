package com.ecoamazonas.eco_agua.academy;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin/academy")
public class AcademyAssessmentAdminController {

    private final AcademyAssessmentService assessmentService;

    public AcademyAssessmentAdminController(AcademyAssessmentService assessmentService) {
        this.assessmentService = assessmentService;
    }

    @GetMapping("/courses/{courseId}/assessments")
    public String assessments(@PathVariable Long courseId,
                              @RequestParam(required = false) Long assessmentId,
                              @RequestParam(required = false) Long questionId,
                              Model model) {
        AcademyCourse course = assessmentService.findCourse(courseId);
        if (course == null) {
            return "redirect:/admin/academy/courses";
        }

        AcademyAssessment selectedAssessment = assessmentService.findAssessment(assessmentId);
        if (selectedAssessment == null || selectedAssessment.getCourse() == null || !selectedAssessment.getCourse().getId().equals(courseId)) {
            selectedAssessment = null;
        }

        AcademyAssessmentQuestion questionForm = selectedAssessment != null
                ? assessmentService.findQuestionForm(selectedAssessment.getId(), questionId)
                : new AcademyAssessmentQuestion();

        model.addAttribute("activePage", "academy_assessments");
        model.addAttribute("course", course);
        model.addAttribute("assessmentForm", assessmentService.findAssessmentForm(courseId, assessmentId));
        model.addAttribute("assessmentStatuses", AcademyAssessment.Status.values());
        model.addAttribute("questionTypes", AcademyAssessmentQuestion.QuestionType.values());
        model.addAttribute("assessments", assessmentService.findAssessmentAdminViews(course));
        model.addAttribute("selectedAssessment", selectedAssessment);
        model.addAttribute("questionForm", questionForm);
        model.addAttribute("optionsText", assessmentService.optionsTextForQuestion(questionForm));
        model.addAttribute("questionViews", assessmentService.findQuestionAdminViews(selectedAssessment));
        return "admin/academy/assessments";
    }

    @PostMapping("/courses/{courseId}/assessments/save")
    public String saveAssessment(@PathVariable Long courseId,
                                 @ModelAttribute("assessmentForm") AcademyAssessment form,
                                 RedirectAttributes redirectAttributes) {
        AcademyAssessment saved = assessmentService.saveAssessment(courseId, form);
        redirectAttributes.addFlashAttribute("successMessage", "Evaluación guardada correctamente.");
        return "redirect:/admin/academy/courses/" + courseId + "/assessments?assessmentId=" + saved.getId();
    }

    @PostMapping("/courses/{courseId}/assessments/{assessmentId}/publish")
    public String publishAssessment(@PathVariable Long courseId,
                                    @PathVariable Long assessmentId,
                                    RedirectAttributes redirectAttributes) {
        assessmentService.publishAssessment(courseId, assessmentId);
        redirectAttributes.addFlashAttribute("successMessage", "Evaluación publicada.");
        return redirectToAssessment(courseId, assessmentId);
    }

    @PostMapping("/courses/{courseId}/assessments/{assessmentId}/draft")
    public String draftAssessment(@PathVariable Long courseId,
                                  @PathVariable Long assessmentId,
                                  RedirectAttributes redirectAttributes) {
        assessmentService.draftAssessment(courseId, assessmentId);
        redirectAttributes.addFlashAttribute("successMessage", "Evaluación enviada a borrador.");
        return redirectToAssessment(courseId, assessmentId);
    }

    @PostMapping("/courses/{courseId}/assessments/{assessmentId}/archive")
    public String archiveAssessment(@PathVariable Long courseId,
                                    @PathVariable Long assessmentId,
                                    RedirectAttributes redirectAttributes) {
        assessmentService.archiveAssessment(courseId, assessmentId);
        redirectAttributes.addFlashAttribute("successMessage", "Evaluación archivada.");
        return "redirect:/admin/academy/courses/" + courseId + "/assessments";
    }

    @PostMapping("/courses/{courseId}/assessments/{assessmentId}/questions/save")
    public String saveQuestion(@PathVariable Long courseId,
                               @PathVariable Long assessmentId,
                               @ModelAttribute("questionForm") AcademyAssessmentQuestion form,
                               @RequestParam(name = "optionsText", required = false) String optionsText,
                               RedirectAttributes redirectAttributes) {
        assessmentService.saveQuestion(assessmentId, form, optionsText);
        redirectAttributes.addFlashAttribute("successMessage", "Pregunta guardada correctamente.");
        return redirectToAssessment(courseId, assessmentId);
    }

    @PostMapping("/courses/{courseId}/assessments/{assessmentId}/questions/{questionId}/toggle")
    public String toggleQuestion(@PathVariable Long courseId,
                                 @PathVariable Long assessmentId,
                                 @PathVariable Long questionId,
                                 RedirectAttributes redirectAttributes) {
        assessmentService.toggleQuestion(assessmentId, questionId);
        redirectAttributes.addFlashAttribute("successMessage", "Estado de la pregunta actualizado.");
        return redirectToAssessment(courseId, assessmentId);
    }

    @GetMapping("/assessment-results")
    public String results(Model model) {
        model.addAttribute("activePage", "academy_assessment_results");
        model.addAttribute("results", assessmentService.findAdminResults());
        return "admin/academy/assessment_results";
    }

    private String redirectToAssessment(Long courseId, Long assessmentId) {
        return "redirect:/admin/academy/courses/" + courseId + "/assessments?assessmentId=" + assessmentId;
    }
}
