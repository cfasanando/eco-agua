package com.ecoamazonas.eco_agua.marketing;

import com.ecoamazonas.eco_agua.marketing.Testimonial;
import com.ecoamazonas.eco_agua.marketing.TestimonialRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/marketing/admin")
public class MarketingAdminController {

    private final TestimonialRepository testimonialRepository;

    public MarketingAdminController(TestimonialRepository testimonialRepository) {
        this.testimonialRepository = testimonialRepository;
    }

    @GetMapping("/testimonials")
    public String listTestimonials(@RequestParam(value = "id", required = false) Long id,
                                   Model model) {

        List<Testimonial> testimonials =
                testimonialRepository.findAllByOrderByDisplayOrderAscIdAsc();

        Testimonial formObject;
        boolean isEdit = false;

        if (id != null) {
            formObject = testimonialRepository.findById(id).orElseGet(Testimonial::new);
            isEdit = formObject.getId() != null;
        } else {
            formObject = new Testimonial();
        }

        model.addAttribute("testimonials", testimonials);
        model.addAttribute("testimonialForm", formObject);
        model.addAttribute("isEdit", isEdit);

        // Same admin layout, but under "marketing"
        return "marketing/admin_testimonials";
    }

    @PostMapping("/testimonials/save")
    public String saveTestimonial(@ModelAttribute("testimonialForm") Testimonial form,
                                  RedirectAttributes redirectAttributes) {

        if (form.getDisplayOrder() == null) {
            form.setDisplayOrder(0);
        }

        testimonialRepository.save(form);

        redirectAttributes.addFlashAttribute(
                "successMessage",
                "Testimonial saved successfully."
        );

        return "redirect:/marketing/admin/testimonials";
    }

    @PostMapping("/testimonials/{id}/delete")
    public String deleteTestimonial(@PathVariable Long id,
                                    RedirectAttributes redirectAttributes) {

        testimonialRepository.deleteById(id);

        redirectAttributes.addFlashAttribute(
                "successMessage",
                "Testimonial deleted successfully."
        );

        return "redirect:/marketing/admin/testimonials";
    }
}
