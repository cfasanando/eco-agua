package com.ecoamazonas.eco_agua.product;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProductSupplyCompositionRepository extends JpaRepository<ProductSupplyComposition, Long> {

    List<ProductSupplyComposition> findByProductId(Long productId);
}
