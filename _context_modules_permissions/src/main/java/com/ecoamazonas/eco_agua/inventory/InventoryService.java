package com.ecoamazonas.eco_agua.inventory;

import com.ecoamazonas.eco_agua.product.Product;
import com.ecoamazonas.eco_agua.product.ProductRepository;
import com.ecoamazonas.eco_agua.supply.Supply;
import com.ecoamazonas.eco_agua.supply.SupplyRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Service
public class InventoryService {

    private final InventoryMovementRepository movementRepository;
    private final ProductRepository productRepository;
    private final SupplyRepository supplyRepository;

    public InventoryService(InventoryMovementRepository movementRepository,
                            ProductRepository productRepository,
                            SupplyRepository supplyRepository) {
        this.movementRepository = movementRepository;
        this.productRepository = productRepository;
        this.supplyRepository = supplyRepository;
    }

    @Transactional
    public void registerProductMovement(
            Long productId,
            BigDecimal quantityIn,
            BigDecimal quantityOut,
            InventoryMovementType type,
            String referenceModule,
            Long referenceId,
            String observation,
            LocalDate movementDate
    ) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("Product not found: " + productId));

        if (quantityIn == null) {
            quantityIn = BigDecimal.ZERO;
        }
        if (quantityOut == null) {
            quantityOut = BigDecimal.ZERO;
        }

        BigDecimal newStock = product.getStock()
                .add(quantityIn)
                .subtract(quantityOut);

        if (newStock.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalStateException("Product stock cannot be negative");
        }

        product.setStock(newStock);
        productRepository.save(product);

        InventoryMovement movement = new InventoryMovement();
        movement.setProduct(product);
        movement.setMovementType(type);
        movement.setQuantityIn(quantityIn);
        movement.setQuantityOut(quantityOut);
        movement.setReferenceModule(referenceModule);
        movement.setReferenceId(referenceId);
        movement.setObservation(observation);

        LocalDateTime movementDateTime = movementDate != null
                ? movementDate.atStartOfDay()
                : LocalDateTime.now();
        movement.setMovementDate(movementDateTime);

        movementRepository.save(movement);
    }

    @Transactional
    public void registerSupplyMovement(
            Long supplyId,
            BigDecimal quantityIn,
            BigDecimal quantityOut,
            InventoryMovementType type,
            String referenceModule,
            Long referenceId,
            String observation,
            LocalDate movementDate
    ) {
        Supply supply = supplyRepository.findById(supplyId)
                .orElseThrow(() -> new IllegalArgumentException("Supply not found: " + supplyId));

        if (quantityIn == null) {
            quantityIn = BigDecimal.ZERO;
        }
        if (quantityOut == null) {
            quantityOut = BigDecimal.ZERO;
        }

        BigDecimal newStock = supply.getStock()
                .add(quantityIn)
                .subtract(quantityOut);

        if (newStock.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalStateException("Supply stock cannot be negative");
        }

        supply.setStock(newStock);
        supplyRepository.save(supply);

        InventoryMovement movement = new InventoryMovement();
        movement.setSupply(supply);
        movement.setMovementType(type);
        movement.setQuantityIn(quantityIn);
        movement.setQuantityOut(quantityOut);
        movement.setReferenceModule(referenceModule);
        movement.setReferenceId(referenceId);
        movement.setObservation(observation);

        LocalDateTime movementDateTime = movementDate != null
                ? movementDate.atStartOfDay()
                : LocalDateTime.now();
        movement.setMovementDate(movementDateTime);

        movementRepository.save(movement);
    }
}
