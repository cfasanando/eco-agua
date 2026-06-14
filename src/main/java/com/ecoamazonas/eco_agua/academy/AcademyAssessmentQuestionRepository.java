package com.ecoamazonas.eco_agua.academy;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AcademyAssessmentQuestionRepository extends JpaRepository<AcademyAssessmentQuestion, Long> {

    List<AcademyAssessmentQuestion> findByAssessmentOrderByDisplayOrderAscIdAsc(AcademyAssessment assessment);

    List<AcademyAssessmentQuestion> findByAssessmentAndActiveTrueOrderByDisplayOrderAscIdAsc(AcademyAssessment assessment);

    long countByAssessmentAndActiveTrue(AcademyAssessment assessment);
}
