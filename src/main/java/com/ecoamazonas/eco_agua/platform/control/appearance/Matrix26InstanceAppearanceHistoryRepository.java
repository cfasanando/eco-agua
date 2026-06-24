package com.ecoamazonas.eco_agua.platform.control.appearance;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface Matrix26InstanceAppearanceHistoryRepository extends JpaRepository<Matrix26InstanceAppearanceHistory, Long> {
    List<Matrix26InstanceAppearanceHistory> findTop20ByInstance_IdOrderByVersionDesc(Long instanceId);
    Optional<Matrix26InstanceAppearanceHistory> findTopByInstance_IdOrderByVersionDesc(Long instanceId);
}
