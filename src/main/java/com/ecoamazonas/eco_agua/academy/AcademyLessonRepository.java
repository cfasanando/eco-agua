package com.ecoamazonas.eco_agua.academy;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AcademyLessonRepository extends JpaRepository<AcademyLesson, Long> {

    List<AcademyLesson> findByCourseOrderByDisplayOrderAscIdAsc(AcademyCourse course);

    List<AcademyLesson> findByModuleOrderByDisplayOrderAscIdAsc(AcademyCourseModule module);

    List<AcademyLesson> findByModuleAndActiveTrueAndStatusOrderByDisplayOrderAscIdAsc(AcademyCourseModule module, AcademyLesson.Status status);

    List<AcademyLesson> findByCourseAndActiveTrueAndStatusOrderByDisplayOrderAscIdAsc(AcademyCourse course, AcademyLesson.Status status);

    long countByCourseAndActiveTrueAndStatus(AcademyCourse course, AcademyLesson.Status status);

    long countByCourseAndPreviewTrueAndActiveTrueAndStatus(AcademyCourse course, AcademyLesson.Status status);
}
