package com.ecoamazonas.eco_agua.product;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProductSupplyRepository extends JpaRepository<ProductSupply, Long> {

    List<ProductSupply> findByProductId(Long productId);
    
    List<ProductSupply> findByProductIdOrderByIdAsc(Long productId);
}
