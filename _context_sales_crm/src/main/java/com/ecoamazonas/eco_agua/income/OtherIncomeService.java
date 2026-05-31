package com.ecoamazonas.eco_agua.income;

import com.ecoamazonas.eco_agua.category.Category;
import com.ecoamazonas.eco_agua.category.CategoryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Service
public class OtherIncomeService {

    private final OtherIncomeRepository repository;
    private final CategoryRepository categoryRepository;

    public OtherIncomeService(
            OtherIncomeRepository repository,
            CategoryRepository categoryRepository
    ) {
        this.repository = repository;
        this.categoryRepository = categoryRepository;
    }

    @Transactional(readOnly = true)
    public List<OtherIncome> findByDateRange(LocalDate startDate, LocalDate endDate) {
        if (startDate == null && endDate == null) {
            throw new IllegalArgumentException("At least one date must be provided.");
        }

        if (startDate == null) {
            startDate = endDate;
        }
        if (endDate == null) {
            endDate = startDate;
        }
        if (endDate.isBefore(startDate)) {
            LocalDate tmp = startDate;
            startDate = endDate;
            endDate = tmp;
        }

        return repository.findByIncomeDateBetweenOrderByIncomeDateAsc(startDate, endDate);
    }

    @Transactional
    public OtherIncome saveFromForm(
            Long categoryId,
            BigDecimal amount,
            LocalDate incomeDate,
            String observation
    ) {
        if (categoryId == null) {
            throw new IllegalArgumentException("Category must be selected.");
        }
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Amount must be greater than zero.");
        }
        if (incomeDate == null) {
            incomeDate = LocalDate.now();
        }

        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new IllegalArgumentException("Category not found: " + categoryId));

        OtherIncome income = new OtherIncome();
        income.setCategory(category);
        income.setAmount(amount);
        income.setIncomeDate(incomeDate);
        income.setObservation(observation);

        return repository.save(income);
    }

    @Transactional
    public void deleteByIds(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return;
        }
        ids.forEach(repository::deleteById);
    }
}
