package com.ecoamazonas.eco_agua.expense;

import com.ecoamazonas.eco_agua.category.Category;
import com.ecoamazonas.eco_agua.category.CategoryRepository;
import com.ecoamazonas.eco_agua.category.CategoryType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Month;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

@Service
public class FixedCostTemplateService {

    private final FixedCostTemplateRepository templateRepository;
    private final FixedCostMonthlyEntryRepository monthlyEntryRepository;
    private final CategoryRepository categoryRepository;
    private final ExpenseService expenseService;

    public FixedCostTemplateService(
            FixedCostTemplateRepository templateRepository,
            FixedCostMonthlyEntryRepository monthlyEntryRepository,
            CategoryRepository categoryRepository,
            ExpenseService expenseService
    ) {
        this.templateRepository = templateRepository;
        this.monthlyEntryRepository = monthlyEntryRepository;
        this.categoryRepository = categoryRepository;
        this.expenseService = expenseService;
    }

    @Transactional(readOnly = true)
    public List<Category> findAvailableCategories() {
        return categoryRepository.findByTypeAndActiveTrueOrderByNameAsc(CategoryType.EXPENSES);
    }

    @Transactional(readOnly = true)
    public List<FixedCostTemplate> findAllTemplates() {
        return templateRepository.findAllByOrderByActiveDescCategory_NameAscDescriptionAsc();
    }

    @Transactional(readOnly = true)
    public FixedCostMonthlySummary buildMonthlySummary(int year, int month) {
        LocalDate start = LocalDate.of(year, month, 1);
        LocalDate end = start.withDayOfMonth(start.lengthOfMonth());

        List<FixedCostTemplate> templates = templateRepository.findAllByOrderByActiveDescCategory_NameAscDescriptionAsc();
        List<FixedCostMonthlyEntry> generatedEntries = monthlyEntryRepository.findDetailedByYearAndMonth(year, month);

        Map<Long, FixedCostMonthlyEntry> generatedByTemplateId = new LinkedHashMap<>();
        for (FixedCostMonthlyEntry entry : generatedEntries) {
            if (entry.getTemplate() != null && entry.getTemplate().getId() != null) {
                generatedByTemplateId.put(entry.getTemplate().getId(), entry);
            }
        }

        BigDecimal expectedTotal = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        BigDecimal expectedBreakEvenTotal = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        BigDecimal generatedTotal = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        BigDecimal generatedBreakEvenTotal = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);

        List<FixedCostMonthlyRow> rows = new ArrayList<>();

        for (FixedCostTemplate template : templates) {
            BigDecimal monthlyTotal = normalizeMoney(template.getMonthlyTotal());
            FixedCostMonthlyEntry entry = generatedByTemplateId.get(template.getId());

            if (template.isActive()) {
                expectedTotal = expectedTotal.add(monthlyTotal);
                if (template.isIncludeInBreakEven()) {
                    expectedBreakEvenTotal = expectedBreakEvenTotal.add(monthlyTotal);
                }
            }

            if (entry != null) {
                BigDecimal entryAmount = normalizeMoney(entry.getGeneratedAmount());
                generatedTotal = generatedTotal.add(entryAmount);
                if (entry.isIncludedInBreakEven()) {
                    generatedBreakEvenTotal = generatedBreakEvenTotal.add(entryAmount);
                }
            }

            FixedCostMonthlyRow row = new FixedCostMonthlyRow();
            row.setTemplateId(template.getId());
            row.setDescription(template.getDescription());
            row.setCategoryName(template.getCategory() != null ? template.getCategory().getName() : "-");
            row.setUnitOfMeasure(template.getUnitOfMeasure());
            row.setQuantity(normalizeMoney(template.getQuantity()));
            row.setUnitValue(normalizeMoney(template.getUnitValue()));
            row.setMonthlyTotal(monthlyTotal);
            row.setIncludeInBreakEven(template.isIncludeInBreakEven());
            row.setActive(template.isActive());
            row.setGenerated(entry != null);
            if (entry != null) {
                row.setGeneratedExpenseId(entry.getExpense() != null ? entry.getExpense().getId() : null);
                row.setGeneratedAmount(normalizeMoney(entry.getGeneratedAmount()));
                row.setGeneratedAt(entry.getGeneratedAt());
            } else {
                row.setGeneratedAmount(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP));
            }
            rows.add(row);
        }

        FixedCostMonthlySummary summary = new FixedCostMonthlySummary();
        summary.setYear(year);
        summary.setMonth(month);
        summary.setFromDate(start);
        summary.setToDate(end);
        summary.setExpectedTotal(normalizeMoney(expectedTotal));
        summary.setExpectedBreakEvenTotal(normalizeMoney(expectedBreakEvenTotal));
        summary.setGeneratedTotal(normalizeMoney(generatedTotal));
        summary.setGeneratedBreakEvenTotal(normalizeMoney(generatedBreakEvenTotal));
        summary.setPendingTotal(normalizeMoney(expectedTotal.subtract(generatedTotal)));
        summary.setPendingBreakEvenTotal(normalizeMoney(expectedBreakEvenTotal.subtract(generatedBreakEvenTotal)));
        summary.setActualRegisteredTotal(normalizeMoney(expenseService.getTotalFixedCosts(start, end)));
        summary.setActualBreakdown(expenseService.getFixedCostsByCategory(start, end));
        summary.setRows(rows);

        return summary;
    }

    @Transactional
    public FixedCostTemplate saveTemplate(FixedCostTemplateForm form) {
        if (form == null) {
            throw new IllegalArgumentException("Template data is required.");
        }

        String description = trimToNull(form.getDescription());
        if (description == null) {
            throw new IllegalArgumentException("Description is required.");
        }

        if (form.getCategoryId() == null) {
            throw new IllegalArgumentException("Category is required.");
        }

        BigDecimal quantity = normalizeMoney(form.getQuantity() != null ? form.getQuantity() : BigDecimal.ONE);
        BigDecimal unitValue = normalizeMoney(form.getUnitValue());
        if (quantity.signum() <= 0) {
            throw new IllegalArgumentException("Quantity must be greater than zero.");
        }
        if (unitValue.signum() < 0) {
            throw new IllegalArgumentException("Unit value cannot be negative.");
        }

        Category category = categoryRepository.findById(form.getCategoryId())
                .orElseThrow(() -> new IllegalArgumentException("Category not found."));

        FixedCostTemplate template = form.getId() != null
                ? templateRepository.findById(form.getId()).orElseThrow(() -> new IllegalArgumentException("Template not found."))
                : new FixedCostTemplate();

        template.setDescription(description);
        template.setCategory(category);
        template.setUnitOfMeasure(trimToNull(form.getUnitOfMeasure()));
        template.setQuantity(quantity);
        template.setUnitValue(unitValue);
        template.setIncludeInBreakEven(form.isIncludeInBreakEven());
        template.setActive(form.isActive());
        template.setNotes(trimToNull(form.getNotes()));

        return templateRepository.save(template);
    }

    @Transactional
    public void deleteTemplate(Long templateId) {
        FixedCostTemplate template = templateRepository.findById(templateId)
                .orElseThrow(() -> new IllegalArgumentException("Template not found."));

        if (monthlyEntryRepository.existsByTemplateIdAndYearValueAndMonthValue(templateId, LocalDate.now().getYear(), LocalDate.now().getMonthValue())) {
            // Intentionally allow delete only when there is no generated movement for the current month.
        }

        if (hasAnyGeneratedEntry(templateId)) {
            throw new IllegalArgumentException("This template already generated monthly expenses. Deactivate it instead of deleting it.");
        }

        templateRepository.delete(template);
    }

    @Transactional
    public void toggleTemplate(Long templateId) {
        FixedCostTemplate template = templateRepository.findById(templateId)
                .orElseThrow(() -> new IllegalArgumentException("Template not found."));

        template.setActive(!template.isActive());
        templateRepository.save(template);
    }

    @Transactional
    public int generateMonth(int year, int month) {
        LocalDate firstDay = LocalDate.of(year, month, 1);
        List<FixedCostTemplate> templates = templateRepository.findByActiveTrueOrderByCategory_NameAscDescriptionAsc();

        int created = 0;

        for (FixedCostTemplate template : templates) {
            if (monthlyEntryRepository.existsByTemplateIdAndYearValueAndMonthValue(template.getId(), year, month)) {
                continue;
            }

            BigDecimal amount = normalizeMoney(template.getMonthlyTotal());
            if (amount.signum() <= 0) {
                continue;
            }

            Expense expense = expenseService.registerSimpleExpense(
                    firstDay,
                    template.getCategory().getId(),
                    buildGeneratedObservation(template, year, month),
                    null,
                    amount
            );

            FixedCostMonthlyEntry entry = new FixedCostMonthlyEntry();
            entry.setTemplate(template);
            entry.setExpense(expense);
            entry.setYearValue(year);
            entry.setMonthValue(month);
            entry.setGeneratedAmount(amount);
            entry.setIncludedInBreakEven(template.isIncludeInBreakEven());
            entry.setGeneratedAt(LocalDateTime.now());
            monthlyEntryRepository.save(entry);
            created++;
        }

        return created;
    }

    private boolean hasAnyGeneratedEntry(Long templateId) {
        return monthlyEntryRepository.findAll().stream()
                .anyMatch(entry -> entry.getTemplate() != null && templateId.equals(entry.getTemplate().getId()));
    }

    private String buildGeneratedObservation(FixedCostTemplate template, int year, int month) {
        String monthName = Month.of(month).getDisplayName(TextStyle.FULL, new Locale("es", "PE"));
        return "Generated from fixed cost template: " + template.getDescription() +
                " | Period: " + monthName + " " + year;
    }

    private BigDecimal normalizeMoney(BigDecimal value) {
        if (value == null) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }

        return value.setScale(2, RoundingMode.HALF_UP);
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }

        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
