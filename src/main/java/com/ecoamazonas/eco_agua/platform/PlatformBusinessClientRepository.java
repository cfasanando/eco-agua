package com.ecoamazonas.eco_agua.platform;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PlatformBusinessClientRepository extends JpaRepository<PlatformBusinessClient, Long> {

    List<PlatformBusinessClient> findAllByOrderByCreatedAtDescIdDesc();

    List<PlatformBusinessClient> findAllByOrderByBusinessNameAsc();

    List<PlatformBusinessClient> findByMonitorVisibleTrueOrderByCreatedAtDescIdDesc();

    Optional<PlatformBusinessClient> findByCode(String code);

    Optional<PlatformBusinessClient> findByCodeIgnoreCase(String code);

    boolean existsByCode(String code);

    boolean existsByCodeIgnoreCase(String code);

    boolean existsByCodeIgnoreCaseAndIdNot(String code, Long id);

    boolean existsByRuntimePort(Integer runtimePort);

    boolean existsByRuntimePortAndIdNot(Integer runtimePort, Long id);

    boolean existsByDatabaseNameIgnoreCase(String databaseName);

    boolean existsByDatabaseNameIgnoreCaseAndIdNot(String databaseName, Long id);

    boolean existsByRuntimeProfileIgnoreCase(String runtimeProfile);

    boolean existsByRuntimeProfileIgnoreCaseAndIdNot(String runtimeProfile, Long id);
}
