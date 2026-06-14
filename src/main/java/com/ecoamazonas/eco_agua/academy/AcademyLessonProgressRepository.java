package com.ecoamazonas.eco_agua.academy;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AcademyLessonProgressRepository extends JpaRepository<AcademyLessonProgress, Long> {

    List<AcademyLessonProgress> findByEnrollment(AcademyEnrollment enrollment);

    Optional<AcademyLessonProgress> findByEnrollmentAndLesson(AcademyEnrollment enrollment, AcademyLesson lesson);

    long countByEnrollmentAndCompletedTrue(AcademyEnrollment enrollment);
}
