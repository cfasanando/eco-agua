package com.ecoamazonas.eco_agua.platform.control;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface Matrix26ProvisioningStepRepository extends JpaRepository<Matrix26ProvisioningStep, Long> {

    List<Matrix26ProvisioningStep> findByJob_IdOrderByDisplayOrderAscIdAsc(Long jobId);

    Optional<Matrix26ProvisioningStep> findByJob_IdAndStepCode(Long jobId, String stepCode);

    void deleteByJob_Id(Long jobId);
}
