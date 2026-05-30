package com.ecoamazonas.eco_agua.category;

import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * Repository for Category persistence operations.
 */
public interface CategoryRepository extends JpaRepository<Category, Long> {

    default List<Category> findAllOrdered() {
        return findAll(Sort.by(Sort.Direction.ASC, "name"));
    }

    List<Category> findByActiveTrueOrderByNameAsc();

    List<Category> findByTypeAndActiveTrueOrderByNameAsc(CategoryType type);

    List<Category> findByTypeInAndActiveTrueOrderByNameAsc(List<CategoryType> types);

    List<Category> findByTypeInAndActiveTrueAndCostBehaviorOrderByNameAsc(
            List<CategoryType> types,
            CostBehavior costBehavior
    );
}
