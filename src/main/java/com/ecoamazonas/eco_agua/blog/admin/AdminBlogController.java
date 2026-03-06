package com.ecoamazonas.eco_agua.blog.admin;

import com.ecoamazonas.eco_agua.blog.BlogPost;
import com.ecoamazonas.eco_agua.blog.BlogPostForm;
import com.ecoamazonas.eco_agua.blog.BlogPostRepository;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.UUID;

@Controller
@RequestMapping("/admin/blog")
public class AdminBlogController {

    private final BlogPostRepository blogPostRepository;

    @Value("${app.blog.upload-dir:uploads/blog}")
    private String blogUploadDir;

    public AdminBlogController(BlogPostRepository blogPostRepository) {
        this.blogPostRepository = blogPostRepository;
    }

    private void setActivePage(Model model) {
        model.addAttribute("activePage", "admin_blog");
    }

    @GetMapping
    public String list(Model model) {
        setActivePage(model);
        model.addAttribute("posts", blogPostRepository.findAll());
        return "admin/blog/list";
    }

    @GetMapping("/new")
    public String createForm(Model model) {
        setActivePage(model);
        BlogPostForm form = new BlogPostForm();
        form.setStatus(BlogPost.Status.DRAFT);
        model.addAttribute("form", form);
        return "admin/blog/form";
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        setActivePage(model);
        BlogPost post = blogPostRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Post not found: " + id));

        model.addAttribute("form", BlogPostForm.fromEntity(post));
        return "admin/blog/form";
    }

    @PostMapping("/save")
    public String save(@Valid @ModelAttribute("form") BlogPostForm form,
                       BindingResult bindingResult,
                       @RequestParam(value = "coverFile", required = false) MultipartFile coverFile,
                       Model model) throws IOException {

        setActivePage(model);

        if (bindingResult.hasErrors()) {
            return "admin/blog/form";
        }

        BlogPost entity;
        if (form.getId() != null) {
            entity = blogPostRepository.findById(form.getId())
                    .orElseThrow(() -> new IllegalArgumentException("Post not found: " + form.getId()));
        } else {
            entity = new BlogPost();
        }

        // Generate slug if empty
        if (!StringUtils.hasText(form.getSlug()) && StringUtils.hasText(form.getTitle())) {
            form.setSlug(generateSlug(form.getTitle()));
        }

        // Handle cover upload
        if (coverFile != null && !coverFile.isEmpty()) {
            String storedPath = storeCoverFile(coverFile);
            form.setCoverImagePath(storedPath);
        } else if (entity.getCoverImagePath() != null && !StringUtils.hasText(form.getCoverImagePath())) {
            // Preserve existing cover path if not changed
            form.setCoverImagePath(entity.getCoverImagePath());
        }

        // Update entity from form
        form.updateEntity(entity);

        // Handle publish date
        if (entity.getStatus() == BlogPost.Status.PUBLISHED && entity.getPublishedAt() == null) {
            entity.setPublishedAt(LocalDateTime.now());
        }

        blogPostRepository.save(entity);

        return "redirect:/admin/blog";
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id) {
        blogPostRepository.deleteById(id);
        return "redirect:/admin/blog";
    }

    private String generateSlug(String title) {
        String base = title.toLowerCase()
                .replaceAll("[^a-z0-9\\s-]", "")
                .replaceAll("\\s+", "-")
                .replaceAll("-{2,}", "-")
                .replaceAll("^-|-$", "");
        if (!StringUtils.hasText(base)) {
            base = UUID.randomUUID().toString();
        }
        return base;
    }

    private String storeCoverFile(MultipartFile file) throws IOException {
        String originalName = StringUtils.cleanPath(file.getOriginalFilename());
        String extension = "";
        int dotIndex = originalName.lastIndexOf('.');
        if (dotIndex >= 0) {
            extension = originalName.substring(dotIndex);
        }

        String filename = UUID.randomUUID() + extension;

        Path uploadRoot = Paths.get(blogUploadDir).toAbsolutePath().normalize();
        Files.createDirectories(uploadRoot);

        Path target = uploadRoot.resolve(filename);
        file.transferTo(target.toFile());

        String webPath = "/" + blogUploadDir.replace("\\", "/") + "/" + filename;
        return webPath;
    }
}
