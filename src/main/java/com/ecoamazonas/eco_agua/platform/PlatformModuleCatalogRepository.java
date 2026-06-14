package com.ecoamazonas.eco_agua.platform;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PlatformModuleCatalogRepository extends JpaRepository<PlatformModuleCatalog, Long> {

    List<PlatformModuleCatalog> findAllByActiveTrueOrderByAreaAscDisplayOrderAscNameAsc();

    Optional<PlatformModuleCatalog> findByModuleKey(String moduleKey);
}
