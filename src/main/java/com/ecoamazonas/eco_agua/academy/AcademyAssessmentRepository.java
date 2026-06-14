package com.ecoamazonas.eco_agua.academy;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface AcademyAssessmentRepository extends JpaRepository<AcademyAssessment, Long> {

    List<AcademyAssessment> findByCourseOrderByDisplayOrderAscIdAsc(AcademyCourse course);

    List<AcademyAssessment> findByCourseAndActiveTrueAndStatusOrderByDisplayOrderAscIdAsc(AcademyCourse course,
                                                                                           AcademyAssessment.Status status);

    @Query("""
        select assessment
        from AcademyAssessment assessment
        join fetch assessment.course course
        order by course.title asc, assessment.displayOrder asc, assessment.id asc
        """)
    List<AcademyAssessment> findAllForAdmin();

    @Query("""
        select assessment
        from AcademyAssessment assessment
        join fetch assessment.course course
        where assessment.id = :id
        """)
    Optional<AcademyAssessment> findByIdForAdmin(@Param("id") Long id);

    long countByCourse(AcademyCourse course);

    long countByCourseAndActiveTrueAndStatus(AcademyCourse course, AcademyAssessment.Status status);
}
