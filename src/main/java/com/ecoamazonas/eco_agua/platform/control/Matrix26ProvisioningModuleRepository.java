package com.ecoamazonas.eco_agua.platform.control;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface Matrix26ProvisioningModuleRepository extends JpaRepository<Matrix26ProvisioningModule, Long> {

    List<Matrix26ProvisioningModule> findByJob_IdOrderByModuleNameAscIdAsc(Long jobId);

    void deleteByJob_Id(Long jobId);
}
