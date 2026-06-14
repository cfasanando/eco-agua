package com.ecoamazonas.eco_agua.academy;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AcademyAssessmentOptionRepository extends JpaRepository<AcademyAssessmentOption, Long> {

    List<AcademyAssessmentOption> findByQuestionOrderByDisplayOrderAscIdAsc(AcademyAssessmentQuestion question);

    List<AcademyAssessmentOption> findByQuestionAndActiveTrueOrderByDisplayOrderAscIdAsc(AcademyAssessmentQuestion question);

    void deleteByQuestion(AcademyAssessmentQuestion question);
}
