package com.ecoamazonas.eco_agua.personalfinance;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record PersonalFinancePaymentView(
        Long id,
        String publicId,
        Long obligationId,
        String obligationTitle,
        Long debtId,
        String debtName,
        LocalDate paymentDate,
        BigDecimal totalAmount,
        BigDecimal principalAmount,
        BigDecimal interestAmount,
        BigDecimal chargesAmount,
        BigDecimal otherAmount,
        PersonalFinanceCurrency currency,
        PersonalFinancePaymentMethod paymentMethod,
        PersonalFinancePaymentOrigin origin,
        String operationNumber,
        String recipient,
        String notes,
        PersonalFinancePaymentStatus status,
        boolean hasReceipt,
        String receiptOriginalName,
        LocalDateTime reversedAt,
        String reversalReason
) {
}
