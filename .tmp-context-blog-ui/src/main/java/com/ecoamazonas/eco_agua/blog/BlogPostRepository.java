package com.ecoamazonas.eco_agua.blog;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface BlogPostRepository extends JpaRepository<BlogPost, Long> {

    Page<BlogPost> findByStatusOrderByPublishedAtDesc(BlogPost.Status status, Pageable pageable);

    List<BlogPost> findAllByStatusOrderByPublishedAtDesc(BlogPost.Status status);

    List<BlogPost> findTop3ByStatusOrderByPublishedAtDesc(BlogPost.Status status);

    Optional<BlogPost> findBySlugAndStatus(String slug, BlogPost.Status status);
}
