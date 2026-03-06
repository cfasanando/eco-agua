package com.ecoamazonas.eco_agua.user;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface JobPositionRepository extends JpaRepository<JobPosition, Long> {

    @Query("SELECT jp FROM JobPosition jp ORDER BY jp.name ASC")
    List<JobPosition> findAllOrdered();

    @Query("SELECT jp FROM JobPosition jp WHERE jp.active = true ORDER BY jp.name ASC")
    List<JobPosition> findActive();
}
