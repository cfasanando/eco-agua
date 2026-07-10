package com.ecoamazonas.eco_agua.personalfinance;

import java.nio.file.Path;

public record PersonalFinancePaymentReceipt(
        Path path,
        String originalName,
        String contentType,
        long sizeBytes
) {
}
