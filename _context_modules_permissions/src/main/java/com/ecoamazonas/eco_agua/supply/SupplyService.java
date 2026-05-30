package com.ecoamazonas.eco_agua.supply;

import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
public class SupplyService {

    private final SupplyRepository repository;

    public SupplyService(SupplyRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public List<Supply> findAll() {
        return repository.findAll(Sort.by("name").ascending());
    }

    @Transactional
    public void saveFromForm(
            Long id,
            String name,
            String description,
            String unit,
            BigDecimal baseQuantity,
            BigDecimal baseCost,
            BigDecimal stock,
            boolean active
    ) {
        Supply supply;

        if (id == null) {
            supply = new Supply();
        } else {
            supply = repository.findById(id)
                    .orElseThrow(() -> new IllegalArgumentException("Supply not found with id " + id));
        }

        supply.setName(name);
        supply.setDescription(description);
        supply.setUnit(unit);
        supply.setBaseQuantity(baseQuantity);
        supply.setBaseCost(baseCost);
        supply.setStock(stock != null ? stock : BigDecimal.ZERO);
        supply.setActive(active);

        repository.save(supply);
    }

    @Transactional
    public void delete(Long id) {
        repository.deleteById(id);
    }

    @Transactional
    public void deleteBulk(List<Long> ids) {
        repository.deleteAllById(ids);
    }
}
