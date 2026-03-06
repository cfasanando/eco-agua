package com.ecoamazonas.eco_agua.blog;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

//@Controller
@RequestMapping("/blog")
public class BlogController {

    private final BlogPostRepository blogPostRepository;

    public BlogController(BlogPostRepository blogPostRepository) {
        this.blogPostRepository = blogPostRepository;
    }

    @GetMapping
    public String list(@RequestParam(defaultValue = "0") int page, Model model) {
        Page<BlogPost> posts = blogPostRepository.findByStatusOrderByPublishedAtDesc(
                BlogPost.Status.PUBLISHED,
                PageRequest.of(page, 6)
        );

        model.addAttribute("page", posts);
        return "public/blog_list";
    }

    @GetMapping("/{slug}")
    public String detail(@PathVariable String slug, Model model) {
        BlogPost post = blogPostRepository
                .findBySlugAndStatus(slug, BlogPost.Status.PUBLISHED)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        model.addAttribute("post", post);
        return "public/blog_detail";
    }
}
