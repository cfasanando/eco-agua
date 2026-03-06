package com.ecoamazonas.eco_agua.product;

import com.ecoamazonas.eco_agua.category.Category;
import com.ecoamazonas.eco_agua.category.CategoryRepository;
import com.ecoamazonas.eco_agua.supply.Supply;
import com.ecoamazonas.eco_agua.supply.SupplyRepository;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Service
public class ProductService {

    private final ProductRepository repository;
    private final ProductSupplyRepository productSupplyRepository;
    private final SupplyRepository supplyRepository;
    private final CategoryRepository categoryRepository;

    public ProductService(
            ProductRepository repository,
            ProductSupplyRepository productSupplyRepository,
            SupplyRepository supplyRepository,
            CategoryRepository categoryRepository
    ) {
        this.repository = repository;
        this.productSupplyRepository = productSupplyRepository;
        this.supplyRepository = supplyRepository;
        this.categoryRepository = categoryRepository;
    }

    @Transactional(readOnly = true)
    public List<Product> findAll() {
        return repository.findAll(Sort.by("name").ascending());
    }

    @Transactional
    public void saveFromForm(
            Long id,
            String name,
            String description,
            BigDecimal price,
            boolean active,
            boolean featured,
            Long categoryId,
            String imagePath
    ) {
        Product product;

        if (id == null) {
            product = new Product();
        } else {
            product = repository.findById(id)
                    .orElseThrow(() -> new IllegalArgumentException("Product not found with id " + id));
        }

        product.setName(name);
        product.setDescription(description);
        product.setPrice(price);
        product.setActive(active);
        product.setFeatured(featured); // <-- IMPORTANT: store featured flag

        // Category (optional)
        if (categoryId != null) {
            Category category = categoryRepository.findById(categoryId)
                    .orElseThrow(() -> new IllegalArgumentException("Category not found with id " + categoryId));
            product.setCategory(category);
        } else {
            product.setCategory(null);
        }

        // Image path: when editing, keep the old one if no new image is provided
        if (imagePath != null && !imagePath.isBlank()) {
            product.setImagePath(imagePath);
        } else if (id == null) {
            product.setImagePath(null);
        }

        repository.save(product);
    }

    @Transactional
    public void delete(Long id) {
        repository.deleteById(id);
    }

    @Transactional
    public void deleteBulk(List<Long> ids) {
        repository.deleteAllById(ids);
    }

    /**
     * Save supplies composition for a product.
     */
    @Transactional
    public void saveSuppliesConfig(
            Long productId,
            List<Long> supplyIds,
            List<BigDecimal> quantitiesUsed
    ) {
        Product product = repository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("Product not found with id " + productId));

        // Remove old composition
        productSupplyRepository.deleteAll(product.getSuppliesComposition());
        product.getSuppliesComposition().clear();

        if (supplyIds == null || supplyIds.isEmpty()) {
            return;
        }

        List<ProductSupply> newItems = new ArrayList<>();

        for (int i = 0; i < supplyIds.size(); i++) {
            Long supplyId = supplyIds.get(i);
            if (supplyId == null) {
                continue;
            }

            BigDecimal quantity = null;
            if (quantitiesUsed != null && quantitiesUsed.size() > i) {
                quantity = quantitiesUsed.get(i);
            }
            if (quantity == null) {
                quantity = BigDecimal.ZERO;
            }

            Supply supply = supplyRepository.findById(supplyId)
                    .orElseThrow(() -> new IllegalArgumentException("Supply not found with id " + supplyId));

            ProductSupply ps = new ProductSupply();
            ps.setProduct(product);
            ps.setSupply(supply);
            ps.setQuantityUsed(quantity);

            newItems.add(ps);
        }

        productSupplyRepository.saveAll(newItems);
        product.getSuppliesComposition().addAll(newItems);
    }

    @Transactional(readOnly = true)
    public Product findById(Long id) {
        return repository.findById(id)
                .orElseThrow(() ->
                        new IllegalArgumentException("Product not found with id " + id));
    }
}
