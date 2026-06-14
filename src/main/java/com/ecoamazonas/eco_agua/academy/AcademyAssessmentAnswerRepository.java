package com.ecoamazonas.eco_agua.academy;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface AcademyAssessmentAnswerRepository extends JpaRepository<AcademyAssessmentAnswer, Long> {

    @Query("""
        select answer
        from AcademyAssessmentAnswer answer
        join fetch answer.question question
        left join fetch answer.selectedOption option
        where answer.attempt = :attempt
        order by question.displayOrder asc, question.id asc
        """)
    List<AcademyAssessmentAnswer> findByAttemptForReview(@Param("attempt") AcademyAssessmentAttempt attempt);
}
