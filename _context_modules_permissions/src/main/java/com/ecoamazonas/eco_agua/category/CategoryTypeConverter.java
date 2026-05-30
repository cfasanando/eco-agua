package com.ecoamazonas.eco_agua.category;

import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

/**
 * Converter for binding request parameters to CategoryType enum.
 * Accepts multiple string / numeric representations.
 */
@Component
public class CategoryTypeConverter implements Converter<String, CategoryType> {

    @Override
    public CategoryType convert(String source) {
        if (source == null) {
            return null;
        }

        String value = source.trim().toUpperCase();

        // Optional: support numeric codes if you ever used them
        switch (value) {
            case "1":
                return CategoryType.PRODUCT;
            case "2":
                return CategoryType.INCOME;
            case "3":
                return CategoryType.EXPENSES;
            case "4":
                return CategoryType.SUPPLIER;
            default:
                // Text values (accept both EXPENSE and EXPENSES)
                return switch (value) {
                    case "PRODUCT" -> CategoryType.PRODUCT;
                    case "INCOME" -> CategoryType.INCOME;
                    case "EXPENSE", "EXPENSES" -> CategoryType.EXPENSES;
                    case "SUPPLIER" -> CategoryType.SUPPLIER;
                    default -> throw new IllegalArgumentException("Unknown CategoryType: " + source);
                };
        }
    }
}
