package com.ecoamazonas.eco_agua.reorder;

import com.ecoamazonas.eco_agua.order.OrderService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.lang.reflect.Method;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Service
public class ReorderAgendaService {

    private final OrderService orderService;
    private final ReorderFollowUpRepository reorderFollowUpRepository;

    public ReorderAgendaService(
            OrderService orderService,
            ReorderFollowUpRepository reorderFollowUpRepository
    ) {
        this.orderService = orderService;
        this.reorderFollowUpRepository = reorderFollowUpRepository;
    }

    @Transactional(readOnly = true)
    public List<ReorderAgendaRow> buildAgenda(
            LocalDate referenceDate,
            ReorderFollowUpStatus followUpStatus,
            Integer minProbability,
            boolean onlyOverdue
    ) {
        LocalDate effectiveDate = referenceDate != null ? referenceDate : LocalDate.now();
        List<?> suggestions = orderService.getPossibleOrderSuggestions(effectiveDate, 0);
        List<ReorderAgendaRow> rows = new ArrayList<>();

        for (Object suggestion : suggestions) {
            Long clientId = getLongAny(suggestion, "getClientId", "clientId");
            if (clientId == null) {
                continue;
            }

            Optional<ReorderFollowUp> followUp = reorderFollowUpRepository
                    .findTopByClientIdAndReferenceDateOrderByUpdatedAtDescIdDesc(clientId, effectiveDate);

            ReorderFollowUpStatus persistedStatus =
                    followUp.map(ReorderFollowUp::getStatus).orElse(ReorderFollowUpStatus.PENDING);
            LocalDate nextContactDate =
                    followUp.map(ReorderFollowUp::getNextContactDate).orElse(null);
            String observation =
                    followUp.map(ReorderFollowUp::getObservation).orElse(null);

            int probabilityPercent = getIntAny(
                    suggestion,
                    "getProbabilityPercent",
                    "probabilityPercent"
            );

            long overdueDays = getLongPrimitiveAny(
                    suggestion,
                    "getOverdueDays",
                    "overdueDays"
            );

            if (followUpStatus != null && persistedStatus != followUpStatus) {
                continue;
            }

            if (minProbability != null && probabilityPercent < minProbability) {
                continue;
            }

            if (onlyOverdue && overdueDays <= 0) {
                continue;
            }

            rows.add(new ReorderAgendaRow(
                    clientId,
                    getStringAny(suggestion, "getClientName", "clientName"),
                    getStringAny(suggestion, "getPhone", "getClientPhone", "phone", "clientPhone"),
                    getStringAny(suggestion, "getProfileName", "getClientProfileName", "profileName", "clientProfileName"),
                    getLocalDateAny(suggestion, "getLastOrderDate", "lastOrderDate"),
                    getLongPrimitiveAny(suggestion, "getDaysSinceLastOrder", "daysSinceLastOrder"),
                    getIntAny(suggestion, "getPurchaseDayCount", "purchaseDayCount"),
                    getIntAny(suggestion, "getAverageIntervalDays", "averageIntervalDays"),
                    getLocalDateAny(suggestion, "getExpectedNextOrderDate", "expectedNextOrderDate"),
                    overdueDays,
                    probabilityPercent,
                    getStringAny(suggestion, "getStatusLabel", "statusLabel"),
                    getStringAny(suggestion, "getStatusClass", "statusClass"),
                    getStringAny(suggestion, "getActionLabel", "actionLabel"),
                    persistedStatus,
                    nextContactDate,
                    observation
            ));
        }

        rows.sort(
                Comparator.comparingInt(ReorderAgendaRow::getProbabilityPercent).reversed()
                        .thenComparingLong(ReorderAgendaRow::getOverdueDays).reversed()
                        .thenComparingLong(ReorderAgendaRow::getDaysSinceLastOrder).reversed()
                        .thenComparing(row -> row.getClientName() != null ? row.getClientName().toLowerCase() : "")
        );

        return rows;
    }

    @Transactional
    public ReorderFollowUp saveFollowUp(
            Long clientId,
            LocalDate referenceDate,
            ReorderFollowUpStatus status,
            LocalDate nextContactDate,
            String observation
    ) {
        if (clientId == null) {
            throw new IllegalArgumentException("Client is required.");
        }

        LocalDate effectiveDate = referenceDate != null ? referenceDate : LocalDate.now();

        ReorderFollowUp followUp = reorderFollowUpRepository
                .findTopByClientIdAndReferenceDateOrderByUpdatedAtDescIdDesc(clientId, effectiveDate)
                .orElseGet(ReorderFollowUp::new);

        followUp.setClientId(clientId);
        followUp.setReferenceDate(effectiveDate);
        followUp.setStatus(status != null ? status : ReorderFollowUpStatus.PENDING);
        followUp.setNextContactDate(nextContactDate);
        followUp.setObservation(clean(observation));

        return reorderFollowUpRepository.save(followUp);
    }

    private String clean(String value) {
        if (value == null) {
            return null;
        }

        String trimmed = value.trim();
        return trimmed.isBlank() ? null : trimmed;
    }

    private Object invokeIfExists(Object target, String methodName) {
        try {
            Method method = target.getClass().getMethod(methodName);
            return method.invoke(target);
        } catch (NoSuchMethodException ex) {
            return null;
        } catch (ReflectiveOperationException ex) {
            throw new IllegalStateException("Unable to read suggestion method: " + methodName, ex);
        }
    }

    private String getStringAny(Object target, String... methodNames) {
        for (String methodName : methodNames) {
            Object value = invokeIfExists(target, methodName);
            if (value != null) {
                return value.toString();
            }
        }

        return null;
    }

    private Integer getIntObjectAny(Object target, String... methodNames) {
        for (String methodName : methodNames) {
            Object value = invokeIfExists(target, methodName);
            if (value instanceof Number number) {
                return number.intValue();
            }
        }

        return null;
    }

    private int getIntAny(Object target, String... methodNames) {
        Integer value = getIntObjectAny(target, methodNames);
        return value != null ? value : 0;
    }

    private Long getLongAny(Object target, String... methodNames) {
        for (String methodName : methodNames) {
            Object value = invokeIfExists(target, methodName);
            if (value instanceof Number number) {
                return number.longValue();
            }
        }

        return null;
    }

    private long getLongPrimitiveAny(Object target, String... methodNames) {
        Long value = getLongAny(target, methodNames);
        return value != null ? value : 0L;
    }

    private LocalDate getLocalDateAny(Object target, String... methodNames) {
        for (String methodName : methodNames) {
            Object value = invokeIfExists(target, methodName);
            if (value instanceof LocalDate localDate) {
                return localDate;
            }
        }

        return null;
    }
}