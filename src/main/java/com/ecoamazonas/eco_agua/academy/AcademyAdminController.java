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
@RequestMapping("/admin/academy/courses")
public class AcademyAdminController {

    private final AcademyCourseService courseService;

    public AcademyAdminController(AcademyCourseService courseService) {
        this.courseService = courseService;
    }

    @GetMapping
    public String courses(@RequestParam(value = "id", required = false) Long id, Model model) {
        model.addAttribute("activePage", "academy_courses");
        model.addAttribute("courseForm", courseService.findForm(id));
        model.addAttribute("courses", courseService.findAllForAdmin());
        model.addAttribute("courseStatuses", AcademyCourse.Status.values());
        model.addAttribute("courseLevels", AcademyCourse.Level.values());
        model.addAttribute("courseCategories", courseService.findCategories());
        model.addAttribute("isCourseEdit", id != null);
        return "admin/academy/courses";
    }

    @PostMapping("/save")
    public String save(@ModelAttribute("courseForm") AcademyCourse courseForm,
                       RedirectAttributes redirectAttributes) {
        AcademyCourse saved = courseService.save(courseForm);
        redirectAttributes.addFlashAttribute("successMessage", "Curso guardado correctamente.");
        return "redirect:/admin/academy/courses?id=" + saved.getId();
    }

    @PostMapping("/{id}/publish")
    public String publish(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        courseService.publish(id);
        redirectAttributes.addFlashAttribute("successMessage", "Curso publicado correctamente.");
        return "redirect:/admin/academy/courses";
    }

    @PostMapping("/{id}/draft")
    public String draft(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        courseService.draft(id);
        redirectAttributes.addFlashAttribute("successMessage", "Curso movido a borrador.");
        return "redirect:/admin/academy/courses";
    }

    @PostMapping("/{id}/featured")
    public String toggleFeatured(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        courseService.toggleFeatured(id);
        redirectAttributes.addFlashAttribute("successMessage", "Curso destacado actualizado.");
        return "redirect:/admin/academy/courses";
    }

    @PostMapping("/{id}/archive")
    public String archive(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        courseService.archive(id);
        redirectAttributes.addFlashAttribute("successMessage", "Curso archivado correctamente.");
        return "redirect:/admin/academy/courses";
    }
}
