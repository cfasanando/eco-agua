package com.ecoamazonas.eco_agua.platform.control.appearance;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface Matrix26InstanceAppearanceRepository extends JpaRepository<Matrix26InstanceAppearance, Long> {
    Optional<Matrix26InstanceAppearance> findByInstance_Id(Long instanceId);
    List<Matrix26InstanceAppearance> findAllByOrderByInstance_BusinessNameAsc();
}
