package com.ecoamazonas.eco_agua.config;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PlatformSettingRepository extends JpaRepository<PlatformSetting, Long> {

    Optional<PlatformSetting> findByVariable(String variable);

    // List only platform and public site settings (used by admin UI)
    List<PlatformSetting> findByCategoryInOrderByCategoryAscVariableAsc(List<String> categories);
}
