package com.ecoamazonas.eco_agua.academy;

import com.ecoamazonas.eco_agua.user.UserAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface AcademyEnrollmentRepository extends JpaRepository<AcademyEnrollment, Long> {

    @Query("""
        select enrollment
        from AcademyEnrollment enrollment
        join fetch enrollment.student student
        join fetch enrollment.course course
        order by enrollment.updatedAt desc, enrollment.id desc
        """)
    List<AcademyEnrollment> findAllForAdmin();

    @Query("""
        select enrollment
        from AcademyEnrollment enrollment
        join fetch enrollment.student student
        join fetch enrollment.course course
        where student.username = :username
        order by enrollment.updatedAt desc, enrollment.id desc
        """)
    List<AcademyEnrollment> findByStudentUsernameForStudent(@Param("username") String username);

    Optional<AcademyEnrollment> findByStudentAndCourse(UserAccount student, AcademyCourse course);

    @Query("""
        select enrollment
        from AcademyEnrollment enrollment
        join fetch enrollment.student student
        join fetch enrollment.course course
        where student.username = :username
          and course.slug = :slug
        """)
    Optional<AcademyEnrollment> findByStudentUsernameAndCourseSlug(@Param("username") String username,
                                                                   @Param("slug") String slug);

    long countByCourse(AcademyCourse course);
}
