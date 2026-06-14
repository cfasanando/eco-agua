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
@RequestMapping("/admin/academy/courses/{courseId}/content")
public class AcademyContentAdminController {

    private final AcademyCourseContentService contentService;

    public AcademyContentAdminController(AcademyCourseContentService contentService) {
        this.contentService = contentService;
    }

    @GetMapping
    public String content(@PathVariable Long courseId,
                          @RequestParam(value = "moduleId", required = false) Long moduleId,
                          @RequestParam(value = "lessonId", required = false) Long lessonId,
                          @RequestParam(value = "moduleForLessonId", required = false) Long moduleForLessonId,
                          Model model) {
        AcademyCourse course = contentService.findCourse(courseId);
        if (course == null) {
            return "redirect:/admin/academy/courses";
        }

        Long selectedModuleForLesson = moduleForLessonId;
        AcademyLesson lessonForm = contentService.findLessonForm(courseId, lessonId, selectedModuleForLesson);
        if (selectedModuleForLesson == null && lessonForm.getModule() != null) {
            selectedModuleForLesson = lessonForm.getModule().getId();
        }

        model.addAttribute("activePage", "academy_courses");
        model.addAttribute("course", course);
        model.addAttribute("moduleViews", contentService.findModuleViewsForAdmin(course));
        model.addAttribute("moduleForm", contentService.findModuleForm(courseId, moduleId));
        model.addAttribute("lessonForm", lessonForm);
        model.addAttribute("selectedModuleForLesson", selectedModuleForLesson);
        model.addAttribute("lessonTypes", AcademyLesson.LessonType.values());
        model.addAttribute("lessonStatuses", AcademyLesson.Status.values());
        model.addAttribute("isModuleEdit", moduleId != null);
        model.addAttribute("isLessonEdit", lessonId != null);
        return "admin/academy/content";
    }

    @PostMapping("/modules/save")
    public String saveModule(@PathVariable Long courseId,
                             @ModelAttribute("moduleForm") AcademyCourseModule moduleForm,
                             RedirectAttributes redirectAttributes) {
        AcademyCourseModule module = contentService.saveModule(courseId, moduleForm);
        redirectAttributes.addFlashAttribute("successMessage", "Unidad guardada correctamente.");
        return "redirect:/admin/academy/courses/" + courseId + "/content?moduleId=" + module.getId();
    }

    @PostMapping("/modules/{moduleId}/archive")
    public String archiveModule(@PathVariable Long courseId,
                                @PathVariable Long moduleId,
                                RedirectAttributes redirectAttributes) {
        contentService.archiveModule(courseId, moduleId);
        redirectAttributes.addFlashAttribute("successMessage", "Unidad desactivada correctamente.");
        return "redirect:/admin/academy/courses/" + courseId + "/content";
    }

    @PostMapping("/lessons/save")
    public String saveLesson(@PathVariable Long courseId,
                             @RequestParam("moduleId") Long moduleId,
                             @ModelAttribute("lessonForm") AcademyLesson lessonForm,
                             RedirectAttributes redirectAttributes) {
        AcademyLesson lesson = contentService.saveLesson(courseId, moduleId, lessonForm);
        redirectAttributes.addFlashAttribute("successMessage", "Lección guardada correctamente.");
        return "redirect:/admin/academy/courses/" + courseId + "/content?lessonId=" + lesson.getId();
    }

    @PostMapping("/lessons/{lessonId}/publish")
    public String publishLesson(@PathVariable Long courseId,
                                @PathVariable Long lessonId,
                                RedirectAttributes redirectAttributes) {
        contentService.publishLesson(courseId, lessonId);
        redirectAttributes.addFlashAttribute("successMessage", "Lección publicada correctamente.");
        return "redirect:/admin/academy/courses/" + courseId + "/content";
    }

    @PostMapping("/lessons/{lessonId}/draft")
    public String draftLesson(@PathVariable Long courseId,
                              @PathVariable Long lessonId,
                              RedirectAttributes redirectAttributes) {
        contentService.draftLesson(courseId, lessonId);
        redirectAttributes.addFlashAttribute("successMessage", "Lección movida a borrador.");
        return "redirect:/admin/academy/courses/" + courseId + "/content";
    }

    @PostMapping("/lessons/{lessonId}/preview")
    public String togglePreview(@PathVariable Long courseId,
                                @PathVariable Long lessonId,
                                RedirectAttributes redirectAttributes) {
        contentService.togglePreview(courseId, lessonId);
        redirectAttributes.addFlashAttribute("successMessage", "Vista previa actualizada.");
        return "redirect:/admin/academy/courses/" + courseId + "/content";
    }

    @PostMapping("/lessons/{lessonId}/archive")
    public String archiveLesson(@PathVariable Long courseId,
                                @PathVariable Long lessonId,
                                RedirectAttributes redirectAttributes) {
        contentService.archiveLesson(courseId, lessonId);
        redirectAttributes.addFlashAttribute("successMessage", "Lección archivada correctamente.");
        return "redirect:/admin/academy/courses/" + courseId + "/content";
    }
}
