package com.ecoamazonas.eco_agua.academy;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface AcademyCertificateRepository extends JpaRepository<AcademyCertificate, Long> {

    @Query("""
        select certificate
        from AcademyCertificate certificate
        join fetch certificate.student student
        join fetch certificate.course course
        join fetch certificate.enrollment enrollment
        order by certificate.issuedAt desc, certificate.id desc
        """)
    List<AcademyCertificate> findAllForAdmin();

    @Query("""
        select certificate
        from AcademyCertificate certificate
        join fetch certificate.student student
        join fetch certificate.course course
        join fetch certificate.enrollment enrollment
        where certificate.certificateCode = :code
        """)
    Optional<AcademyCertificate> findByCertificateCodeForVerification(@Param("code") String code);

    Optional<AcademyCertificate> findByCertificateCode(String certificateCode);

    Optional<AcademyCertificate> findByEnrollment(AcademyEnrollment enrollment);

    boolean existsByCertificateCode(String certificateCode);

    long countByCourse(AcademyCourse course);
}
