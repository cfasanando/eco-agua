package com.ecoamazonas.eco_agua.expense;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ExpensePaymentRepository extends JpaRepository<ExpensePayment, Long> {

    List<ExpensePayment> findByExpenseIdOrderByPaymentDateAsc(Long expenseId);
}
