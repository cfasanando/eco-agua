package com.ecoamazonas.eco_agua.platform;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface PlatformBusinessTemplateModuleRepository extends JpaRepository<PlatformBusinessTemplateModule, Long> {

    @Query("""
            select tm
            from PlatformBusinessTemplateModule tm
            join fetch tm.module m
            where tm.template.id = :templateId
            order by tm.displayOrder asc, m.area asc, m.name asc
            """)
    List<PlatformBusinessTemplateModule> findTemplateModules(@Param("templateId") Long templateId);

    @Query("""
            select tm
            from PlatformBusinessTemplateModule tm
            join fetch tm.template t
            join fetch tm.module m
            order by t.displayOrder asc, tm.displayOrder asc, m.area asc, m.name asc
            """)
    List<PlatformBusinessTemplateModule> findAllWithTemplateAndModule();
}
