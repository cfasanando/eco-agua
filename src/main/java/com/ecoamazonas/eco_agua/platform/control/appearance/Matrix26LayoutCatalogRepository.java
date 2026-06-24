package com.ecoamazonas.eco_agua.platform.control.appearance;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface Matrix26LayoutCatalogRepository extends JpaRepository<Matrix26LayoutCatalog, Long> {
    List<Matrix26LayoutCatalog> findAllByOrderByAreaAscDisplayOrderAscNameAsc();
    List<Matrix26LayoutCatalog> findByStatusOrderByAreaAscDisplayOrderAscNameAsc(String status);
    List<Matrix26LayoutCatalog> findByAreaAndStatusOrderByDisplayOrderAscNameAsc(String area, String status);
    Optional<Matrix26LayoutCatalog> findByCode(String code);
}
