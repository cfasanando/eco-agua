package com.ecoamazonas.eco_agua.platform.control.appearance;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface Matrix26ThemeCatalogRepository extends JpaRepository<Matrix26ThemeCatalog, Long> {
    List<Matrix26ThemeCatalog> findAllByOrderByDisplayOrderAscNameAsc();
    List<Matrix26ThemeCatalog> findByStatusOrderByDisplayOrderAscNameAsc(String status);
    Optional<Matrix26ThemeCatalog> findByCode(String code);
}
