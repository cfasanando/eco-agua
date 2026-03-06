package com.ecoamazonas.eco_agua.user;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.List;

public interface EmployeeRepository extends JpaRepository<Employee, Long> {

    Optional<Employee> findByUserId(Integer userId);
    
    // Returns active employees with a given job position name
    @Query("select e from Employee e join e.jobPosition jp " +
           "where e.active = true and jp.name = :jobName " +
           "order by e.firstName, e.lastName")
    List<Employee> findActiveByJobPositionName(@Param("jobName") String jobName);
}
