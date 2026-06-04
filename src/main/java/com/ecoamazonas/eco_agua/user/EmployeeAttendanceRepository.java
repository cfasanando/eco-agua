package com.ecoamazonas.eco_agua.user;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface EmployeeAttendanceRepository extends JpaRepository<EmployeeAttendance, Long> {

    Optional<EmployeeAttendance> findByEmployeeIdAndAttendanceDate(Long employeeId, LocalDate attendanceDate);

    @Query("""
        select a
        from EmployeeAttendance a
        join fetch a.employee e
        left join fetch e.jobPosition jp
        where a.attendanceDate = :attendanceDate
        order by e.firstName asc, e.lastName asc, a.id asc
        """)
    List<EmployeeAttendance> findByAttendanceDateWithEmployee(@Param("attendanceDate") LocalDate attendanceDate);
}
