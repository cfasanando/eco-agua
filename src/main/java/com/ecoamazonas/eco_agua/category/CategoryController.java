package com.ecoamazonas.eco_agua.category;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * MVC controller for the Categories admin section.
 */
@Controller
@RequestMapping("/admin/categories")
public class CategoryController {

    private final CategoryService service;

    public CategoryController(CategoryService service) {
        this.service = service;
    }

    @GetMapping
    public String listCategories(Model model,
                                 @ModelAttribute("message") String message,
                                 @ModelAttribute("messageType") String messageType) {
        model.addAttribute("categories", service.findAll());
        model.addAttribute("types", CategoryType.values());
        // flash already on model: message, messageType
        return "admin/categories";
    }

    @PostMapping("/save")
    public String saveCategory(@RequestParam(value = "id", required = false) Long id,
                               @RequestParam("name") String name,
                               @RequestParam(value = "description", required = false) String description,
                               @RequestParam("type") CategoryType type,
                               RedirectAttributes redirectAttributes) {

        Category category;
        if (id != null) {
            category = service.getOrThrow(id);
        } else {
            category = new Category();
        }

        category.setName(name.trim());
        category.setDescription((description != null && !description.isBlank()) ? description.trim() : null);
        category.setType(type);

        service.save(category);

        redirectAttributes.addFlashAttribute("message", "Categoría guardada correctamente.");
        redirectAttributes.addFlashAttribute("messageType", "success");

        return "redirect:/admin/categories";
    }

    @PostMapping("/{id}/delete")
    public String deleteSingle(@PathVariable("id") Long id,
                               RedirectAttributes redirectAttributes) {

        service.deleteById(id);

        redirectAttributes.addFlashAttribute("message", "Categoría eliminada correctamente.");
        redirectAttributes.addFlashAttribute("messageType", "success");

        return "redirect:/admin/categories";
    }

    @PostMapping("/bulk-delete")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> bulkDelete(@RequestParam("ids") List<Long> ids) {
        service.deleteByIds(ids);

        Map<String, Object> body = new HashMap<>();
        body.put("status", "OK");
        body.put("deleted", ids.size());

        return ResponseEntity.ok(body);
    }
}
