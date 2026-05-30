package com.ecoamazonas.eco_agua.supply;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SupplyRepository extends JpaRepository<Supply, Long> {

    List<Supply> findByActiveTrueOrderByNameAsc();
}
