package com.ecoamazonas.eco_agua.expense;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface ExpensePaymentRepository extends JpaRepository<ExpensePayment, Long> {

    List<ExpensePayment> findByExpenseIdOrderByPaymentDateAsc(Long expenseId);

    @Query("""
            select p
            from ExpensePayment p
            join fetch p.expense e
            left join fetch e.category c
            left join fetch e.supplier s
            where p.paymentDate between :startDate and :endDate
            order by p.paymentDate asc, p.id asc
            """)
    List<ExpensePayment> findCashflowPaymentsBetween(
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );
}

