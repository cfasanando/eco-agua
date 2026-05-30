package com.ecoamazonas.eco_agua.inventory;

import com.ecoamazonas.eco_agua.product.Product;
import com.ecoamazonas.eco_agua.supply.Supply;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface InventoryMovementRepository extends JpaRepository<InventoryMovement, Long> {

    List<InventoryMovement> findByProductOrderByMovementDateDesc(Product product);

    List<InventoryMovement> findBySupplyOrderByMovementDateDesc(Supply supply);
}
