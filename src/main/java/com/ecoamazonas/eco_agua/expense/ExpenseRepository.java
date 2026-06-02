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


    @Query("""
        select distinct e
        from Expense e
        left join fetch e.category c
        left join fetch e.supplier s
        where e.expenseDate between :start and :end
          and e.debt = false
        order by e.expenseDate asc, e.id asc
        """)
    List<Expense> findCashflowPaidExpensesBetween(
            @Param("start") LocalDate start,
            @Param("end") LocalDate end
    );

    List<Expense> findByExpenseDateAndDebtFalseOrderByExpenseDateAsc(LocalDate date);

    List<Expense> findByDebtTrueAndStatusInOrderByDueDateAsc(List<ExpenseStatus> statuses);

    List<Expense> findByDebtTrueAndExpenseDateBetweenOrderByExpenseDateAsc(
            LocalDate start,
            LocalDate end
    );

    @Query("""
        select distinct e
        from Expense e
        left join fetch e.category c
        left join fetch e.supplier s
        left join fetch e.payments p
        where e.debt = true
          and e.status in :statuses
          and e.expenseDate between :start and :end
        order by e.dueDate asc, e.expenseDate asc, e.id asc
        """)
    List<Expense> findOpenDebtsDetailedByPeriod(
            @Param("start") LocalDate start,
            @Param("end") LocalDate end,
            @Param("statuses") List<ExpenseStatus> statuses
    );

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

    @Query("""
        select distinct e
        from Expense e
        left join fetch e.category c
        left join fetch e.supplier s
        left join fetch e.payments p
        where e.id = :expenseId
        """)
    Expense findDetailedById(@Param("expenseId") Long expenseId);


    @Query("""
        select distinct e
        from Expense e
        left join fetch e.category c
        left join fetch e.supplier s
        where s.id in :supplierIds
        order by e.expenseDate desc, e.id desc
        """)
    List<Expense> findSupplierExpensesBySupplierIds(@Param("supplierIds") List<Long> supplierIds);

    @Query("""
        select distinct e
        from Expense e
        left join fetch e.category c
        left join fetch e.supplier s
        where s.id = :supplierId
          and e.expenseDate between :start and :end
        order by e.expenseDate desc, e.id desc
        """)
    List<Expense> findSupplierExpensesBySupplierAndPeriod(
            @Param("supplierId") Long supplierId,
            @Param("start") LocalDate start,
            @Param("end") LocalDate end
    );

    @Query("""
        select distinct e
        from Expense e
        left join fetch e.category c
        left join fetch e.supplier s
        where e.id in :expenseIds
        """)
    List<Expense> findAllDetailedByIdIn(@Param("expenseIds") List<Long> expenseIds);
}
