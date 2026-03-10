package com.ecoamazonas.eco_agua.expense;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FixedCostTemplateRepository extends JpaRepository<FixedCostTemplate, Long> {

    List<FixedCostTemplate> findAllByOrderByActiveDescCategory_NameAscDescriptionAsc();

    List<FixedCostTemplate> findByActiveTrueOrderByCategory_NameAscDescriptionAsc();
}
