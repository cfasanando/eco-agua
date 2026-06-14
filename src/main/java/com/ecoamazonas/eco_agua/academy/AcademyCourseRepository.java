package com.ecoamazonas.eco_agua.academy;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.List;
import java.util.Optional;

public interface AcademyCourseRepository extends JpaRepository<AcademyCourse, Long> {

    @Query("""
        select course
        from AcademyCourse course
        order by course.featured desc, course.updatedAt desc, course.id desc
        """)
    List<AcademyCourse> findAllForAdmin();

    @Query("""
        select course
        from AcademyCourse course
        where course.active = true
          and course.status = com.ecoamazonas.eco_agua.academy.AcademyCourse.Status.PUBLISHED
        order by course.featured desc, course.publishedAt desc, course.id desc
        """)
    List<AcademyCourse> findPublishedForCatalog();

    @Query("""
        select course
        from AcademyCourse course
        where course.active = true
          and course.featured = true
          and course.status = com.ecoamazonas.eco_agua.academy.AcademyCourse.Status.PUBLISHED
        order by course.publishedAt desc, course.id desc
        """)
    List<AcademyCourse> findFeaturedPublished();

    Optional<AcademyCourse> findBySlugAndActiveTrueAndStatus(String slug, AcademyCourse.Status status);

    boolean existsBySlug(String slug);

    boolean existsBySlugAndIdNot(String slug, Long id);

    @Query("""
        select distinct course.category
        from AcademyCourse course
        where course.category is not null
          and course.category <> ''
        order by course.category asc
        """)
    List<String> findDistinctCategories();
}
