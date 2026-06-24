package com.ecoamazonas.eco_agua.platform.control;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface Matrix26ProvisioningJobRepository extends JpaRepository<Matrix26ProvisioningJob, Long> {

    List<Matrix26ProvisioningJob> findAllByOrderByCreatedAtDescIdDesc();

    List<Matrix26ProvisioningJob> findTop6ByOrderByCreatedAtDescIdDesc();

    Optional<Matrix26ProvisioningJob> findByReferenceCode(String referenceCode);

    long countByStatus(String status);
}
