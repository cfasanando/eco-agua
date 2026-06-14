package com.ecoamazonas.eco_agua.platform;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface PlatformClientModuleRepository extends JpaRepository<PlatformClientModule, Long> {

    @Query("""
            select cm
            from PlatformClientModule cm
            join fetch cm.module m
            where cm.client.id = :clientId
            order by m.area asc, m.displayOrder asc, m.name asc
            """)
    List<PlatformClientModule> findClientModules(@Param("clientId") Long clientId);
}
