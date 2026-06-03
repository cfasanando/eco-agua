package com.ecoamazonas.eco_agua.income;

import com.ecoamazonas.eco_agua.accounting.AccountingAutomationEvent;
import com.ecoamazonas.eco_agua.accounting.service.AccountingAutoJournalEntryService;
import com.ecoamazonas.eco_agua.category.Category;
import com.ecoamazonas.eco_agua.category.CategoryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
public class OtherIncomeService {

    private static final BigDecimal ONE_HUNDRED = new BigDecimal("100.00");

    private final OtherIncomeRepository repository;
    private final CategoryRepository categoryRepository;
    private final AccountingAutoJournalEntryService accountingAutoJournalEntryService;

    public OtherIncomeService(
            OtherIncomeRepository repository,
            CategoryRepository categoryRepository,
            AccountingAutoJournalEntryService accountingAutoJournalEntryService
    ) {
        this.repository = repository;
        this.categoryRepository = categoryRepository;
        this.accountingAutoJournalEntryService = accountingAutoJournalEntryService;
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

    @Transactional(readOnly = true)
    public OtherIncomeSummary buildSummary(List<OtherIncome> incomes) {
        OtherIncomeSummary summary = new OtherIncomeSummary();
        if (incomes == null || incomes.isEmpty()) {
            return summary;
        }

        BigDecimal total = incomes.stream()
                .map(OtherIncome::getAmount)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        long daysWithIncome = incomes.stream()
                .map(OtherIncome::getIncomeDate)
                .filter(Objects::nonNull)
                .distinct()
                .count();

        LocalDate firstDate = incomes.stream()
                .map(OtherIncome::getIncomeDate)
                .filter(Objects::nonNull)
                .min(LocalDate::compareTo)
                .orElse(null);

        LocalDate lastDate = incomes.stream()
                .map(OtherIncome::getIncomeDate)
                .filter(Objects::nonNull)
                .max(LocalDate::compareTo)
                .orElse(null);

        Map<Long, OtherIncomeCategorySummary> byCategory = new LinkedHashMap<>();
        for (OtherIncome income : incomes) {
            Category category = income.getCategory();
            Long categoryId = category != null ? category.getId() : 0L;
            String categoryName = category != null ? category.getName() : "Sin categoría";
            byCategory.computeIfAbsent(categoryId, id -> new OtherIncomeCategorySummary(id, categoryName))
                    .add(income.getAmount(), income.getIncomeDate());
        }

        List<OtherIncomeCategorySummary> categorySummaries = byCategory.values().stream()
                .sorted(Comparator.comparing(OtherIncomeCategorySummary::getTotalAmount).reversed())
                .toList();

        summary.setRecordCount(incomes.size());
        summary.setDaysWithIncome(daysWithIncome);
        summary.setTotalAmount(total);
        summary.setAverageAmount(total.divide(new BigDecimal(incomes.size()), 2, RoundingMode.HALF_UP));
        summary.setFirstIncomeDate(firstDate);
        summary.setLastIncomeDate(lastDate);
        summary.setCategorySummaries(categorySummaries);
        return summary;
    }

    @Transactional
    public OtherIncome saveFromForm(
            Long categoryId,
            BigDecimal amount,
            LocalDate incomeDate,
            String observation
    ) {
        return saveFromForm(categoryId, amount, incomeDate, observation, null, null, null, BigDecimal.ZERO);
    }

    @Transactional
    public OtherIncome saveFromForm(
            Long categoryId,
            BigDecimal amount,
            LocalDate incomeDate,
            String observation,
            String docType,
            String docSeries,
            String docNumber,
            BigDecimal taxRatePercent
    ) {
        if (categoryId == null) {
            throw new IllegalArgumentException("Debe seleccionar una categoría.");
        }
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("El monto debe ser mayor que cero.");
        }
        if (incomeDate == null) {
            incomeDate = LocalDate.now();
        }

        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new IllegalArgumentException("No se encontró la categoría seleccionada."));

        BigDecimal normalizedAmount = amount.setScale(2, RoundingMode.HALF_UP);
        BigDecimal normalizedTaxRate = normalizeTaxRate(taxRatePercent);
        BigDecimal taxBase = calculateTaxBase(normalizedAmount, normalizedTaxRate);
        BigDecimal taxIgv = normalizedAmount.subtract(taxBase).setScale(2, RoundingMode.HALF_UP);

        OtherIncome income = new OtherIncome();
        income.setCategory(category);
        income.setAmount(normalizedAmount);
        income.setIncomeDate(incomeDate);
        income.setObservation(clean(observation));
        income.setDocType(clean(docType));
        income.setDocSeries(clean(docSeries));
        income.setDocNumber(clean(docNumber));
        income.setTaxRate(normalizedTaxRate);
        income.setTaxBase(taxBase);
        income.setTaxIgv(taxIgv);

        OtherIncome savedIncome = repository.save(income);
        accountingAutoJournalEntryService.generateForOtherIncome(savedIncome);
        return savedIncome;
    }

    @Transactional
    public void deleteByIds(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return;
        }
        ids.forEach(id -> {
            accountingAutoJournalEntryService.cancelDraftEntries(AccountingAutomationEvent.OTHER_INCOME, id);
            repository.deleteById(id);
        });
    }

    private BigDecimal normalizeTaxRate(BigDecimal taxRatePercent) {
        if (taxRatePercent == null || taxRatePercent.compareTo(BigDecimal.ZERO) < 0) {
            return BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP);
        }
        return taxRatePercent.divide(ONE_HUNDRED, 4, RoundingMode.HALF_UP);
    }

    private BigDecimal calculateTaxBase(BigDecimal amount, BigDecimal taxRate) {
        if (taxRate == null || taxRate.compareTo(BigDecimal.ZERO) <= 0) {
            return amount.setScale(2, RoundingMode.HALF_UP);
        }
        return amount.divide(BigDecimal.ONE.add(taxRate), 2, RoundingMode.HALF_UP);
    }

    private String clean(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
