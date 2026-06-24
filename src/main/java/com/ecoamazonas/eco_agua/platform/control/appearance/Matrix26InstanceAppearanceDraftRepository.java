package com.ecoamazonas.eco_agua.platform.control.appearance;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface Matrix26InstanceAppearanceDraftRepository extends JpaRepository<Matrix26InstanceAppearanceDraft, Long> {
    Optional<Matrix26InstanceAppearanceDraft> findByInstance_Id(Long instanceId);
    boolean existsByInstance_Id(Long instanceId);
    void deleteByInstance_Id(Long instanceId);
}
