package com.ecoamazonas.eco_agua.academy;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface AcademyAssessmentAttemptRepository extends JpaRepository<AcademyAssessmentAttempt, Long> {

    @Query("""
        select attempt
        from AcademyAssessmentAttempt attempt
        join fetch attempt.student student
        join fetch attempt.course course
        join fetch attempt.assessment assessment
        order by attempt.submittedAt desc, attempt.id desc
        """)
    List<AcademyAssessmentAttempt> findAllForAdmin();

    @Query("""
        select attempt
        from AcademyAssessmentAttempt attempt
        join fetch attempt.student student
        join fetch attempt.course course
        join fetch attempt.assessment assessment
        where student.username = :username
        order by attempt.submittedAt desc, attempt.id desc
        """)
    List<AcademyAssessmentAttempt> findByStudentUsername(@Param("username") String username);

    List<AcademyAssessmentAttempt> findByEnrollmentAndAssessmentOrderByAttemptNumberDescIdDesc(AcademyEnrollment enrollment,
                                                                                                AcademyAssessment assessment);

    long countByEnrollmentAndAssessment(AcademyEnrollment enrollment, AcademyAssessment assessment);

    long countByAssessment(AcademyAssessment assessment);

    @Query("""
        select attempt
        from AcademyAssessmentAttempt attempt
        join fetch attempt.student student
        join fetch attempt.course course
        join fetch attempt.assessment assessment
        where attempt.id = :id
          and student.username = :username
        """)
    Optional<AcademyAssessmentAttempt> findByIdAndStudentUsername(@Param("id") Long id,
                                                                  @Param("username") String username);
}
