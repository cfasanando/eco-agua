package com.ecoamazonas.eco_agua.supplier;

import com.ecoamazonas.eco_agua.category.Category;
import com.ecoamazonas.eco_agua.category.CategoryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class SupplierService {

    private final SupplierRepository repository;
    private final CategoryRepository categoryRepository;

    public SupplierService(SupplierRepository repository,
                           CategoryRepository categoryRepository) {
        this.repository = repository;
        this.categoryRepository = categoryRepository;
    }

    @Transactional(readOnly = true)
    public List<Supplier> findAll() {
        return repository.findAllByOrderByNameAsc();
    }

    @Transactional
    public void saveFromForm(
            Long id,
            String name,
            String docType,
            String docNumber,
            String address,
            String phone,
            String contactName,
            String contactPhone,
            boolean active,
            Long categoryId
    ) {
        Supplier supplier;

        if (id == null) {
            supplier = new Supplier();
        } else {
            supplier = repository.findById(id)
                    .orElseThrow(() -> new IllegalArgumentException("Supplier not found with id " + id));
        }

        supplier.setName(name);
        supplier.setDocType(docType);
        supplier.setDocNumber(docNumber);
        supplier.setAddress(address);
        supplier.setPhone(phone);
        supplier.setContactName(contactName);
        supplier.setContactPhone(contactPhone);
        supplier.setActive(active);

        if (categoryId != null) {
            Category category = categoryRepository.findById(categoryId)
                    .orElseThrow(() -> new IllegalArgumentException("Category not found with id " + categoryId));
            supplier.setCategory(category);
        } else {
            supplier.setCategory(null);
        }

        repository.save(supplier);
    }

    @Transactional
    public void delete(Long id) {
        repository.deleteById(id);
    }

    @Transactional
    public void deleteBulk(List<Long> ids) {
        repository.deleteAllById(ids);
    }

    @Transactional(readOnly = true)
    public Supplier findById(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Supplier not found with id " + id));
    }
}
