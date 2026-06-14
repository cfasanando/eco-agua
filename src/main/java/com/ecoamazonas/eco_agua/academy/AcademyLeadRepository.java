package com.ecoamazonas.eco_agua.academy;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface AcademyLeadRepository extends JpaRepository<AcademyLead, Long> {

    @Query("""
        select lead
        from AcademyLead lead
        join fetch lead.course course
        left join fetch lead.student student
        left join fetch lead.enrollment enrollment
        order by lead.createdAt desc, lead.id desc
        """)
    List<AcademyLead> findAllForAdmin();

    @Query("""
        select lead
        from AcademyLead lead
        join fetch lead.course course
        left join fetch lead.student student
        left join fetch lead.enrollment enrollment
        where lead.id = :id
        """)
    Optional<AcademyLead> findByIdForAdmin(@Param("id") Long id);

    long countByStatus(AcademyLead.Status status);
}
