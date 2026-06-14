package com.ecoamazonas.eco_agua.platform;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface PlatformProvisioningLogRepository extends JpaRepository<PlatformProvisioningLog, Long> {

    @Query("""
            select log
            from PlatformProvisioningLog log
            where log.client.id = :clientId
            order by log.createdAt desc, log.id desc
            """)
    List<PlatformProvisioningLog> findByClient(@Param("clientId") Long clientId);
}
