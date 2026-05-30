package com.ecoamazonas.eco_agua.user;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface EmployeeRepository extends JpaRepository<Employee, Long> {

    Optional<Employee> findByUserId(Integer userId);

    List<Employee> findByActiveTrueOrderByFirstNameAscLastNameAsc();

    @Query("""
        select e
        from Employee e
        left join fetch e.jobPosition jp
        where e.id = :id
        """)
    Optional<Employee> findByIdWithJobPosition(@Param("id") Long id);

    @Query("""
        select e
        from Employee e
        join e.jobPosition jp
        where e.active = true
          and jp.name = :jobName
        order by e.firstName, e.lastName
        """)
    List<Employee> findActiveByJobPositionName(@Param("jobName") String jobName);
}