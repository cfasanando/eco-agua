package com.ecoamazonas.eco_agua.platform.control.modules;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface Matrix26ModuleActivationEventRepository extends JpaRepository<Matrix26ModuleActivationEvent, Long> {

    List<Matrix26ModuleActivationEvent> findTop100ByOrderByCreatedAtDescIdDesc();

    List<Matrix26ModuleActivationEvent> findTop50ByInstance_IdOrderByCreatedAtDescIdDesc(Long instanceId);
}
