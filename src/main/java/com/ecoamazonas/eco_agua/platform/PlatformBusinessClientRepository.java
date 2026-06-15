package com.ecoamazonas.eco_agua.platform;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PlatformBusinessClientRepository extends JpaRepository<PlatformBusinessClient, Long> {

    List<PlatformBusinessClient> findAllByOrderByCreatedAtDescIdDesc();

    List<PlatformBusinessClient> findByMonitorVisibleTrueOrderByCreatedAtDescIdDesc();

    Optional<PlatformBusinessClient> findByCode(String code);

    boolean existsByCode(String code);
}
