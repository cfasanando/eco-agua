package com.ecoamazonas.eco_agua.platform;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PlatformBusinessTemplateRepository extends JpaRepository<PlatformBusinessTemplate, Long> {

    List<PlatformBusinessTemplate> findAllByActiveTrueOrderByDisplayOrderAscNameAsc();

    Optional<PlatformBusinessTemplate> findByCode(String code);
}
