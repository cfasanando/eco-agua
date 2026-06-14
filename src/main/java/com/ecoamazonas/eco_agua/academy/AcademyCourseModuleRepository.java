package com.ecoamazonas.eco_agua.academy;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AcademyCourseModuleRepository extends JpaRepository<AcademyCourseModule, Long> {

    List<AcademyCourseModule> findByCourseOrderByDisplayOrderAscIdAsc(AcademyCourse course);

    List<AcademyCourseModule> findByCourseAndActiveTrueOrderByDisplayOrderAscIdAsc(AcademyCourse course);
}
