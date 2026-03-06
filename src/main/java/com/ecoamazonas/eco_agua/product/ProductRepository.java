package com.ecoamazonas.eco_agua.product;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProductRepository extends JpaRepository<Product, Long> {

    // Public home: featured products
    List<Product> findTop8ByActiveTrueOrderByIdDesc();

    // Catalog: all active products grouped by category and name
    List<Product> findByActiveTrueOrderByCategoryNameAscNameAsc();

    List<Product> findTop4ByActiveTrueAndFeaturedTrueOrderByIdDesc();

    List<Product> findTop4ByActiveTrueAndCategoryNameOrderByIdDesc(String categoryName);
    
    List<Product> findByActiveTrueOrderByNameAsc();
}
