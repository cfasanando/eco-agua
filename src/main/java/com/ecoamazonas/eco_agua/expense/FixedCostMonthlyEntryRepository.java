package com.ecoamazonas.eco_agua.expense;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface FixedCostMonthlyEntryRepository extends JpaRepository<FixedCostMonthlyEntry, Long> {

    boolean existsByTemplateIdAndYearValueAndMonthValue(Long templateId, int yearValue, int monthValue);

    Optional<FixedCostMonthlyEntry> findByTemplateIdAndYearValueAndMonthValue(Long templateId, int yearValue, int monthValue);

    @Query("""
            select e
            from FixedCostMonthlyEntry e
            join fetch e.template t
            join fetch t.category c
            join fetch e.expense ex
            where e.yearValue = :year and e.monthValue = :month
            order by c.name asc, t.description asc
            """)
    List<FixedCostMonthlyEntry> findDetailedByYearAndMonth(
            @Param("year") int year,
            @Param("month") int month
    );
}
