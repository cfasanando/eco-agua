package com.ecoamazonas.eco_agua.expense;

import com.ecoamazonas.eco_agua.accounting.dto.PurchaseRegistryRow;
import com.ecoamazonas.eco_agua.category.CategoryType;
import com.ecoamazonas.eco_agua.product.cost.PeriodExpenseLine;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public interface ExpenseRepository extends JpaRepository<Expense, Long> {

    List<Expense> findByExpenseDateBetweenOrderByExpenseDateAsc(LocalDate start, LocalDate end);

    List<Expense> findByExpenseDateAndDebtFalseOrderByExpenseDateAsc(LocalDate date);

    List<Expense> findByDebtTrueAndStatusInOrderByDueDateAsc(List<ExpenseStatus> statuses);

    List<Expense> findByDebtTrueAndExpenseDateBetweenOrderByExpenseDateAsc(
            LocalDate start,
            LocalDate end
    );

    /**
     * Sum all expenses by category type in a given period.
     */
    @Query("""
        select coalesce(sum(e.amount), 0)
        from Expense e
        where e.expenseDate between :start and :end
          and e.category.type = :categoryType
        """)
    BigDecimal sumAmountByCategoryTypeAndPeriod(
            @Param("categoryType") CategoryType categoryType,
            @Param("start") LocalDate start,
            @Param("end") LocalDate end
    );

    /**
     * Sum expenses grouped by category name for a given category type and period.
     */
    @Query("""
        select e.category.name, coalesce(sum(e.amount), 0)
        from Expense e
        where e.expenseDate between :start and :end
          and e.category.type = :categoryType
        group by e.category.name
        order by e.category.name
        """)
    List<Object[]> sumAmountByCategoryNameForTypeAndPeriod(
            @Param("categoryType") CategoryType categoryType,
            @Param("start") LocalDate start,
            @Param("end") LocalDate end
    );

    // -------------------------------------------------------------------------
    // Costs by list of categories (already used by product cost module)
    // -------------------------------------------------------------------------

    @Query("""
        select coalesce(sum(e.amount), 0)
        from Expense e
        where e.expenseDate between :start and :end
          and e.category.id in :categoryIds
        """)
    BigDecimal sumAmountByCategoryIdsAndPeriod(
            @Param("categoryIds") List<Long> categoryIds,
            @Param("start") LocalDate start,
            @Param("end") LocalDate end
    );

    @Query("""
        select new com.ecoamazonas.eco_agua.product.cost.PeriodExpenseLine(
            e.category.name,
            coalesce(sum(e.amount), 0),
            null
        )
        from Expense e
        where e.expenseDate between :start and :end
          and e.category.id in :categoryIds
        group by e.category.name
        order by e.category.name
        """)
    List<PeriodExpenseLine> sumAmountByCategoryIdsGrouped(
            @Param("categoryIds") List<Long> categoryIds,
            @Param("start") LocalDate start,
            @Param("end") LocalDate end
    );

    // -------------------------------------------------------------------------
    // Purchase registry rows (SUNAT style)
    // -------------------------------------------------------------------------

    @Query("""
        select new com.ecoamazonas.eco_agua.accounting.dto.PurchaseRegistryRow(
            e.expenseDate,
            e.docType,
            e.docSeries,
            e.docNumber,
            s.docType,
            s.docNumber,
            s.name,
            e.taxBase,
            e.taxIgv,
            e.amount,
            e.status,
            e.category.name,
            e.igvCreditUsable,
            e.docOrigin
        )
        from Expense e
        left join e.supplier s
        where e.expenseDate between :start and :end
          and (:docType is null or e.docType = :docType)
          and (:status is null or e.status = :status)
        order by e.expenseDate, e.docType, e.docSeries, e.docNumber
        """)
    List<PurchaseRegistryRow> findPurchaseRegistryRows(
            @Param("start") LocalDate start,
            @Param("end") LocalDate end,
            @Param("docType") String docType,
            @Param("status") ExpenseStatus status
    );
}
