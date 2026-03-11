package com.ecoamazonas.eco_agua.dashboard;

import com.ecoamazonas.eco_agua.cashflow.BreakEvenResult;
import com.ecoamazonas.eco_agua.cashflow.BreakEvenService;
import com.ecoamazonas.eco_agua.product.Product;
import com.ecoamazonas.eco_agua.product.ProductRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class MonthlyFollowupService {

    private static final BigDecimal ZERO = BigDecimal.ZERO;
    private static final BigDecimal ONE_HUNDRED = new BigDecimal("100");

    private final BreakEvenService breakEvenService;
    private final ProductRepository productRepository;

    public MonthlyFollowupService(BreakEvenService breakEvenService, ProductRepository productRepository) {
        this.breakEvenService = breakEvenService;
        this.productRepository = productRepository;
    }

    @Transactional(readOnly = true)
    public List<Product> findActiveProducts() {
        return productRepository.findByActiveTrueOrderByNameAsc();
    }

    @Transactional(readOnly = true)
    public MonthlyFollowupSnapshot buildSnapshot(Long productId, int year, int month) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("Product not found: " + productId));

        YearMonth yearMonth = YearMonth.of(year, month);
        LocalDate start = yearMonth.atDay(1);
        LocalDate end = yearMonth.atEndOfMonth();
        LocalDate today = LocalDate.now();

        BreakEvenResult result = breakEvenService.calculateForProductAndPeriod(productId, start, end);

        int daysInMonth = yearMonth.lengthOfMonth();
        boolean isCurrentMonth = today.getYear() == year && today.getMonthValue() == month;
        int elapsedDays;
        int remainingDays;

        if (today.isBefore(start)) {
            elapsedDays = 0;
            remainingDays = daysInMonth;
        } else if (today.isAfter(end)) {
            elapsedDays = daysInMonth;
            remainingDays = 0;
        } else {
            elapsedDays = today.getDayOfMonth();
            remainingDays = Math.max(daysInMonth - elapsedDays, 0);
        }

        BigDecimal baseFixedMonth = nvl(result.getFixedCosts(), 2);
        BigDecimal breakEvenUnits = nvl(result.getBreakEvenUnitsRounded(), 0);
        BigDecimal soldUnits = nvl(result.getTotalUnitsSold(), 2);
        BigDecimal missingUnits = positiveDifference(breakEvenUnits, soldUnits, 2);
        BigDecimal soldProgressPercent = percentage(soldUnits, breakEvenUnits, 1);
        BigDecimal expectedUnitsByToday = elapsedDays <= 0
                ? ZERO.setScale(2, RoundingMode.HALF_UP)
                : breakEvenUnits.multiply(BigDecimal.valueOf(elapsedDays))
                    .divide(BigDecimal.valueOf(daysInMonth), 2, RoundingMode.HALF_UP);
        BigDecimal paceGapUnits = soldUnits.subtract(expectedUnitsByToday).setScale(2, RoundingMode.HALF_UP);
        BigDecimal averageSellingPrice = nvl(result.getActualAverageSellingPrice(), 2);
        BigDecimal totalRevenue = nvl(result.getTotalRevenueInPeriod(), 2);
        BigDecimal contributionMarginUnit = nvl(result.getContributionMargin(), 4);
        BigDecimal targetUnitsPerRemainingDay = remainingDays > 0
                ? missingUnits.divide(BigDecimal.valueOf(remainingDays), 2, RoundingMode.HALF_UP)
                : ZERO.setScale(2, RoundingMode.HALF_UP);
        BigDecimal targetSalesPerRemainingDay = targetUnitsPerRemainingDay.multiply(averageSellingPrice)
                .setScale(2, RoundingMode.HALF_UP);

        Pace pace = resolvePace(isCurrentMonth, breakEvenUnits, soldUnits, expectedUnitsByToday, targetUnitsPerRemainingDay);

        Map<String, BigDecimal> operationalVariableByCategory = result.getOperationalVariableByCategory() != null
                ? new LinkedHashMap<>(result.getOperationalVariableByCategory())
                : new LinkedHashMap<>();

        return new MonthlyFollowupSnapshot(
                product,
                start,
                end,
                daysInMonth,
                elapsedDays,
                remainingDays,
                isCurrentMonth,
                baseFixedMonth,
                breakEvenUnits,
                soldUnits,
                missingUnits,
                soldProgressPercent,
                expectedUnitsByToday,
                paceGapUnits,
                averageSellingPrice,
                totalRevenue,
                contributionMarginUnit,
                targetUnitsPerRemainingDay,
                targetSalesPerRemainingDay,
                pace.label,
                pace.badgeClass,
                buildPaceSummary(isCurrentMonth, soldUnits, expectedUnitsByToday, paceGapUnits, remainingDays, targetUnitsPerRemainingDay),
                buildRecommendation(isCurrentMonth, missingUnits, remainingDays, targetUnitsPerRemainingDay, averageSellingPrice, result),
                nvl(result.getOperationalVariableTotal(), 2),
                nvl(result.getOperationalVariablePersonnelTotal(), 2),
                nvl(result.getOperationalVariableNonPersonnelTotal(), 2),
                nvl(result.getOperationalVariableUnitCost(), 4),
                nvl(result.getOperationalContributionMargin(), 4),
                nvl(result.getOperationalBreakEvenUnitsRounded(), 0),
                nvl(result.getOperationalGapUnits(), 2),
                nvl(result.getOperationalGapSales(), 2),
                nvl(result.getOperationalContributionTotalBeforeFixed(), 2),
                nvl(result.getOperationalResultAfterFixed(), 2),
                result.isOperationalReadingAvailable(),
                operationalVariableByCategory,
                result
        );
    }

    private Pace resolvePace(
            boolean isCurrentMonth,
            BigDecimal breakEvenUnits,
            BigDecimal soldUnits,
            BigDecimal expectedUnitsByToday,
            BigDecimal targetUnitsPerRemainingDay
    ) {
        if (breakEvenUnits.compareTo(ZERO) <= 0) {
            return new Pace("Sin meta", " text-bg-secondary");
        }
        if (soldUnits.compareTo(breakEvenUnits) >= 0) {
            return new Pace("Cubierto", " text-bg-success");
        }
        if (!isCurrentMonth) {
            return new Pace("Referencia", " text-bg-primary");
        }
        if (expectedUnitsByToday.compareTo(ZERO) <= 0) {
            return new Pace("Arranque", " text-bg-info");
        }

        BigDecimal ratio = soldUnits.divide(expectedUnitsByToday, 4, RoundingMode.HALF_UP);
        if (ratio.compareTo(new BigDecimal("0.95")) >= 0) {
            return new Pace("En ritmo", " text-bg-success");
        }
        if (ratio.compareTo(new BigDecimal("0.75")) >= 0) {
            return new Pace("Ajustar", " text-bg-warning");
        }
        if (targetUnitsPerRemainingDay.compareTo(new BigDecimal("50")) >= 0) {
            return new Pace("Crítico", " text-bg-danger");
        }
        return new Pace("Atrasado", " text-bg-danger");
    }

    private String buildPaceSummary(
            boolean isCurrentMonth,
            BigDecimal soldUnits,
            BigDecimal expectedUnitsByToday,
            BigDecimal paceGapUnits,
            int remainingDays,
            BigDecimal targetUnitsPerRemainingDay
    ) {
        if (!isCurrentMonth) {
            return "Vista de referencia del mes seleccionado. Úsala para comparar el cierre del período.";
        }
        if (expectedUnitsByToday.compareTo(ZERO) <= 0) {
            return "El mes recién empieza. Usa esta pantalla para fijar una meta diaria desde el primer día.";
        }
        if (paceGapUnits.compareTo(ZERO) >= 0) {
            return "Vas en ritmo frente al punto de equilibrio del mes. Mantén el promedio diario actual.";
        }
        return "Vas por debajo del ritmo esperado. En " + remainingDays + " día(s) necesitas promediar "
                + targetUnitsPerRemainingDay.setScale(2, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString()
                + " unidades por día para recuperar el faltante.";
    }

    private String buildRecommendation(
            boolean isCurrentMonth,
            BigDecimal missingUnits,
            int remainingDays,
            BigDecimal targetUnitsPerRemainingDay,
            BigDecimal averageSellingPrice,
            BreakEvenResult result
    ) {
        if (missingUnits.compareTo(ZERO) <= 0) {
            return "Ya cubriste el punto de equilibrio estructural del mes. Desde aquí, lo adicional fortalece la utilidad.";
        }
        if (!isCurrentMonth) {
            return "Usa este cierre como referencia para planificar un mejor ritmo mensual en el próximo período.";
        }
        if (remainingDays <= 0) {
            return "El mes ya cerró y todavía quedó faltante. Revisa la mezcla de clientes y la base fija del mes.";
        }

        StringBuilder builder = new StringBuilder();
        builder.append("Te faltan ")
                .append(missingUnits.setScale(0, RoundingMode.CEILING).toPlainString())
                .append(" unidades. Para cubrir el mes, apunta a un promedio de ")
                .append(targetUnitsPerRemainingDay.setScale(2, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString())
                .append(" botellones por día restante.");

        if (averageSellingPrice.compareTo(ZERO) > 0) {
            BigDecimal dailySales = targetUnitsPerRemainingDay.multiply(averageSellingPrice)
                    .setScale(2, RoundingMode.HALF_UP);
            builder.append(" Eso equivale a unas ventas diarias objetivo de S/ ")
                    .append(dailySales.toPlainString())
                    .append(" manteniendo el precio promedio actual.");
        }

        if (result.isUsesProfilePricing()) {
            builder.append(" Revisa la mezcla de perfiles: vender más en perfiles de mayor contribución reduce la presión de unidades.");
        }
        return builder.toString();
    }

    private BigDecimal nvl(BigDecimal value, int scale) {
        if (value == null) {
            return ZERO.setScale(scale, RoundingMode.HALF_UP);
        }
        return value.setScale(scale, RoundingMode.HALF_UP);
    }

    private BigDecimal positiveDifference(BigDecimal target, BigDecimal current, int scale) {
        BigDecimal safeTarget = target != null ? target : ZERO;
        BigDecimal safeCurrent = current != null ? current : ZERO;
        BigDecimal diff = safeTarget.subtract(safeCurrent);
        if (diff.compareTo(ZERO) < 0) {
            diff = ZERO;
        }
        return diff.setScale(scale, RoundingMode.HALF_UP);
    }

    private BigDecimal percentage(BigDecimal value, BigDecimal base, int scale) {
        if (base == null || base.compareTo(ZERO) <= 0) {
            return ZERO.setScale(scale, RoundingMode.HALF_UP);
        }
        return value.multiply(ONE_HUNDRED).divide(base, scale, RoundingMode.HALF_UP);
    }

    private record Pace(String label, String badgeClass) {
    }
}
