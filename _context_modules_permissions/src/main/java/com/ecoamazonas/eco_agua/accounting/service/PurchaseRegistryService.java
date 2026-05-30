package com.ecoamazonas.eco_agua.accounting.service;

import com.ecoamazonas.eco_agua.accounting.dto.PurchaseRegistryRow;
import com.ecoamazonas.eco_agua.expense.ExpenseRepository;
import com.ecoamazonas.eco_agua.expense.ExpenseStatus;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

@Service
public class PurchaseRegistryService {

    private final ExpenseRepository expenseRepository;

    public PurchaseRegistryService(ExpenseRepository expenseRepository) {
        this.expenseRepository = expenseRepository;
    }

    public List<PurchaseRegistryRow> getMonthlyRows(
            int year,
            int month,
            String docTypeFilter,
            String statusFilter
    ) {
        YearMonth ym = YearMonth.of(year, month);
        LocalDate start = ym.atDay(1);
        LocalDate end = ym.atEndOfMonth();

        String docType = (docTypeFilter == null || "ALL".equalsIgnoreCase(docTypeFilter))
                ? null
                : docTypeFilter;

        ExpenseStatus status = null;
        if (statusFilter != null && !"ALL".equalsIgnoreCase(statusFilter)) {
            status = ExpenseStatus.valueOf(statusFilter);
        }

        return expenseRepository.findPurchaseRegistryRows(start, end, docType, status);
    }
}
