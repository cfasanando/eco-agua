package com.ecoamazonas.eco_agua.personalfinance;

import com.ecoamazonas.eco_agua.user.UserAccount;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.TextStyle;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class PersonalFinanceAlertService {

    private static final BigDecimal ZERO = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
    private final PersonalFinanceCurrentUserService currentUserService;
    private final PersonalFinancePaymentObligationRepository obligationRepository;
    private final PersonalFinanceIncomeEventRepository incomeEventRepository;
    private final PersonalFinanceDebtNegotiationRepository negotiationRepository;

    public PersonalFinanceAlertService(
            PersonalFinanceCurrentUserService currentUserService,
            PersonalFinancePaymentObligationRepository obligationRepository,
            PersonalFinanceIncomeEventRepository incomeEventRepository,
            PersonalFinanceDebtNegotiationRepository negotiationRepository
    ) {
        this.currentUserService = currentUserService;
        this.obligationRepository = obligationRepository;
        this.incomeEventRepository = incomeEventRepository;
        this.negotiationRepository = negotiationRepository;
    }

    @Transactional(readOnly = true)
    public PersonalFinanceAlertCenter center(
            YearMonth selectedMonth,
            PersonalFinanceAlertCategory selectedCategory,
            PersonalFinanceAlertScope selectedScope
    ) {
        YearMonth safeMonth = selectedMonth == null ? YearMonth.now() : selectedMonth;
        PersonalFinanceAlertCategory safeCategory = selectedCategory == null
                ? PersonalFinanceAlertCategory.ALL
                : selectedCategory;
        PersonalFinanceAlertScope safeScope = selectedScope == null
                ? PersonalFinanceAlertScope.ALL
                : selectedScope;
        LocalDate today = LocalDate.now();
        UserAccount user = currentUserService.currentUser();

        List<PersonalFinancePaymentObligation> obligations = obligationRepository
                .findByUserOrderByDueDateAscPriorityAscIdAsc(user);
        List<PersonalFinanceIncomeEvent> incomes = incomeEventRepository
                .findByUserOrderByExpectedDateAscIdAsc(user);
        List<PersonalFinanceDebtNegotiation> negotiations = negotiationRepository
                .findByUserOrderByConversationDateDescIdDesc(user);

        String returnTo = "/gasto-claro/alerts?year=" + safeMonth.getYear()
                + "&month=" + safeMonth.getMonthValue()
                + "&category=" + safeCategory.name()
                + "&scope=" + safeScope.name();

        List<PersonalFinanceAlertItem> allAlerts = new ArrayList<>();
        obligations.forEach(obligation -> addObligationAlert(allAlerts, obligation, safeMonth, today, returnTo));
        incomes.forEach(income -> addIncomeAlert(allAlerts, income, safeMonth, today));
        negotiations.forEach(entry -> addNegotiationAlerts(allAlerts, entry, safeMonth, today));

        allAlerts.sort(Comparator
                .comparingInt((PersonalFinanceAlertItem item) -> item.severity().getOrder())
                .thenComparing(PersonalFinanceAlertItem::date, Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(PersonalFinanceAlertItem::title, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)));

        PersonalFinanceAlertSummary summary = summary(allAlerts, obligations, incomes, negotiations, today);
        List<PersonalFinanceAlertItem> filteredAlerts = allAlerts.stream()
                .filter(item -> safeCategory == PersonalFinanceAlertCategory.ALL || item.category() == safeCategory)
                .filter(item -> matchesScope(item, safeScope))
                .toList();

        Map<LocalDate, List<PersonalFinanceCalendarEvent>> eventsByDate = new LinkedHashMap<>();
        obligations.forEach(obligation -> addObligationCalendarEvent(eventsByDate, obligation, safeMonth, returnTo));
        incomes.forEach(income -> addIncomeCalendarEvent(eventsByDate, income, safeMonth));
        negotiations.forEach(entry -> addNegotiationCalendarEvents(eventsByDate, entry, safeMonth));

        if (safeCategory != PersonalFinanceAlertCategory.ALL) {
            eventsByDate.replaceAll((date, events) -> events.stream()
                    .filter(event -> event.category() == safeCategory)
                    .toList());
        }
        List<PersonalFinanceCalendarDay> calendarDays = calendarDays(safeMonth, today, eventsByDate);
        int totalCalendarEvents = calendarDays.stream().mapToInt(day -> day.events().size()).sum();

        return new PersonalFinanceAlertCenter(
                safeMonth,
                safeMonth.minusMonths(1),
                safeMonth.plusMonths(1),
                monthLabel(safeMonth),
                today,
                summary,
                filteredAlerts,
                calendarDays,
                totalCalendarEvents
        );
    }

    private void addObligationAlert(
            List<PersonalFinanceAlertItem> alerts,
            PersonalFinancePaymentObligation obligation,
            YearMonth selectedMonth,
            LocalDate today,
            String returnTo
    ) {
        if (obligation.getDueDate() == null || isClosed(obligation)) {
            return;
        }
        LocalDate dueDate = obligation.getDueDate();
        boolean overdue = dueDate.isBefore(today);
        boolean dueToday = dueDate.equals(today);
        boolean inSelectedMonth = YearMonth.from(dueDate).equals(selectedMonth);
        if (!overdue && !inSelectedMonth) {
            return;
        }

        boolean partial = obligation.getStatus() == PersonalFinanceObligationStatus.PARTIAL
                || (safe(obligation.getAmountPaid()).signum() > 0 && obligation.pendingAmount().signum() > 0);
        PersonalFinanceAlertSeverity severity = overdue
                ? PersonalFinanceAlertSeverity.CRITICAL
                : (dueToday || partial ? PersonalFinanceAlertSeverity.WARNING : PersonalFinanceAlertSeverity.INFO);
        String statusLabel = overdue
                ? (partial ? "Pago parcial vencido" : "Pago vencido")
                : (dueToday ? "Vence hoy" : (partial ? "Pago parcial" : "Próximo pago"));
        String detail = obligation.getGroup() == null
                ? "Compromiso mensual"
                : obligation.getGroup().getLabel();
        if (partial) {
            detail += " · Pagado " + formatMoney(safe(obligation.getAmountPaid()))
                    + " de " + formatMoney(safe(obligation.getAmountDue()));
        }
        String actionUrl = "/gasto-claro/obligations/" + obligation.getId()
                + "/payment?returnTo=" + URLEncoder.encode(returnTo, StandardCharsets.UTF_8);

        alerts.add(new PersonalFinanceAlertItem(
                "payment-" + obligation.getId(),
                PersonalFinanceAlertCategory.PAYMENT,
                severity,
                dueDate,
                obligation.getTitle(),
                detail,
                money(obligation.pendingAmount()),
                obligation.getCurrency(),
                statusLabel,
                actionUrl,
                partial ? "Completar pago" : "Registrar pago",
                overdue,
                dueToday,
                partial,
                ChronoUnit.DAYS.between(today, dueDate)
        ));
    }

    private void addIncomeAlert(
            List<PersonalFinanceAlertItem> alerts,
            PersonalFinanceIncomeEvent income,
            YearMonth selectedMonth,
            LocalDate today
    ) {
        if (income.getExpectedDate() == null
                || income.getStatus() == PersonalFinanceIncomeStatus.RECEIVED
                || income.getStatus() == PersonalFinanceIncomeStatus.CANCELLED) {
            return;
        }
        LocalDate expectedDate = income.getExpectedDate();
        boolean overdue = expectedDate.isBefore(today);
        boolean dueToday = expectedDate.equals(today);
        boolean inSelectedMonth = YearMonth.from(expectedDate).equals(selectedMonth);
        if (!overdue && !inSelectedMonth) {
            return;
        }
        PersonalFinanceAlertSeverity severity = overdue
                ? PersonalFinanceAlertSeverity.CRITICAL
                : (dueToday ? PersonalFinanceAlertSeverity.WARNING : PersonalFinanceAlertSeverity.INFO);
        String statusLabel = overdue ? "Ingreso no recibido" : (dueToday ? "Cobro esperado hoy" : "Ingreso esperado");
        String actionUrl = "/gasto-claro/income-events?year=" + selectedMonth.getYear()
                + "&month=" + selectedMonth.getMonthValue();

        alerts.add(new PersonalFinanceAlertItem(
                "income-" + income.getId(),
                PersonalFinanceAlertCategory.INCOME,
                severity,
                expectedDate,
                income.getTitle(),
                income.getIncomeSource() == null ? "Ingreso registrado" : income.getIncomeSource().getName(),
                money(income.getAmount()),
                income.getCurrency(),
                statusLabel,
                actionUrl,
                "Revisar ingreso",
                overdue,
                dueToday,
                false,
                ChronoUnit.DAYS.between(today, expectedDate)
        ));
    }

    private void addNegotiationAlerts(
            List<PersonalFinanceAlertItem> alerts,
            PersonalFinanceDebtNegotiation entry,
            YearMonth selectedMonth,
            LocalDate today
    ) {
        String debtName = entry.getDebt() == null ? "Deuda" : entry.getDebt().getName();
        String actionUrl = "/gasto-claro/negotiations/" + entry.getId() + "/edit";

        if (!entry.getStatus().isTerminal()) {
            addNegotiationAlert(
                    alerts,
                    "negotiation-action-" + entry.getId(),
                    entry.getNextActionDate(),
                    selectedMonth,
                    today,
                    debtName,
                    entry.getNextAction() == null || entry.getNextAction().isBlank()
                            ? "Seguimiento de negociación pendiente"
                            : entry.getNextAction(),
                    "Próxima acción",
                    actionUrl
            );
            addNegotiationAlert(
                    alerts,
                    "negotiation-response-" + entry.getId(),
                    entry.getResponseDeadline(),
                    selectedMonth,
                    today,
                    debtName,
                    "Fecha límite para recibir o enviar una respuesta",
                    "Respuesta pendiente",
                    actionUrl
            );
        }

        if (entry.getStatus().isAccepted()) {
            LocalDate firstPaymentDate = entry.getFirstPaymentDate();
            if (firstPaymentDate != null) {
                boolean overdue = firstPaymentDate.isBefore(today);
                boolean dueToday = firstPaymentDate.equals(today);
                boolean inSelectedMonth = YearMonth.from(firstPaymentDate).equals(selectedMonth);
                if (overdue || inSelectedMonth) {
                    BigDecimal amount = safe(entry.getInitialPaymentAmount()).signum() > 0
                            ? entry.getInitialPaymentAmount()
                            : entry.getProposedInstallmentAmount();
                    alerts.add(new PersonalFinanceAlertItem(
                            "negotiation-first-payment-" + entry.getId(),
                            PersonalFinanceAlertCategory.NEGOTIATION,
                            overdue ? PersonalFinanceAlertSeverity.CRITICAL
                                    : (dueToday ? PersonalFinanceAlertSeverity.WARNING : PersonalFinanceAlertSeverity.INFO),
                            firstPaymentDate,
                            debtName,
                            "Primer pago del acuerdo aceptado. Confirma el monto antes de registrarlo como pago real.",
                            money(amount),
                            entry.getCurrency(),
                            overdue ? "Primer pago vencido" : (dueToday ? "Primer pago hoy" : "Primer pago próximo"),
                            actionUrl,
                            "Revisar acuerdo",
                            overdue,
                            dueToday,
                            false,
                            ChronoUnit.DAYS.between(today, firstPaymentDate)
                    ));
                }
            }
        }
    }

    private void addNegotiationAlert(
            List<PersonalFinanceAlertItem> alerts,
            String key,
            LocalDate date,
            YearMonth selectedMonth,
            LocalDate today,
            String title,
            String detail,
            String baseStatus,
            String actionUrl
    ) {
        if (date == null) {
            return;
        }
        boolean overdue = date.isBefore(today);
        boolean dueToday = date.equals(today);
        boolean inSelectedMonth = YearMonth.from(date).equals(selectedMonth);
        if (!overdue && !inSelectedMonth) {
            return;
        }
        alerts.add(new PersonalFinanceAlertItem(
                key,
                PersonalFinanceAlertCategory.NEGOTIATION,
                overdue ? PersonalFinanceAlertSeverity.CRITICAL
                        : (dueToday ? PersonalFinanceAlertSeverity.WARNING : PersonalFinanceAlertSeverity.INFO),
                date,
                title,
                detail,
                null,
                null,
                overdue ? baseStatus + " vencida" : (dueToday ? baseStatus + " hoy" : baseStatus),
                actionUrl,
                "Abrir negociación",
                overdue,
                dueToday,
                false,
                ChronoUnit.DAYS.between(today, date)
        ));
    }

    private void addObligationCalendarEvent(
            Map<LocalDate, List<PersonalFinanceCalendarEvent>> eventsByDate,
            PersonalFinancePaymentObligation obligation,
            YearMonth selectedMonth,
            String returnTo
    ) {
        if (obligation.getDueDate() == null
                || !YearMonth.from(obligation.getDueDate()).equals(selectedMonth)
                || obligation.getStatus() == PersonalFinanceObligationStatus.CANCELLED) {
            return;
        }
        boolean paid = obligation.isPaidLike();
        boolean partial = obligation.getStatus() == PersonalFinanceObligationStatus.PARTIAL
                || (safe(obligation.getAmountPaid()).signum() > 0 && obligation.pendingAmount().signum() > 0);
        String cssClass = paid ? "calendar-event-complete" : (partial ? "calendar-event-partial" : "calendar-event-payment");
        String actionUrl = paid
                ? "/gasto-claro/payments"
                : "/gasto-claro/obligations/" + obligation.getId()
                    + "/payment?returnTo=" + URLEncoder.encode(returnTo, StandardCharsets.UTF_8);
        addCalendarEvent(eventsByDate, new PersonalFinanceCalendarEvent(
                "calendar-payment-" + obligation.getId(),
                PersonalFinanceAlertCategory.PAYMENT,
                obligation.getDueDate(),
                obligation.getTitle(),
                obligation.getGroup() == null ? "Compromiso mensual" : obligation.getGroup().getLabel(),
                paid ? money(obligation.getAmountPaid()) : money(obligation.pendingAmount()),
                obligation.getCurrency(),
                paid ? "Pagado" : (partial ? "Parcial" : obligation.getStatus().getLabel()),
                actionUrl,
                cssClass,
                paid ? "bi-check-circle" : "bi-cash-coin"
        ));
    }

    private void addIncomeCalendarEvent(
            Map<LocalDate, List<PersonalFinanceCalendarEvent>> eventsByDate,
            PersonalFinanceIncomeEvent income,
            YearMonth selectedMonth
    ) {
        if (income.getExpectedDate() == null
                || !YearMonth.from(income.getExpectedDate()).equals(selectedMonth)
                || income.getStatus() == PersonalFinanceIncomeStatus.CANCELLED) {
            return;
        }
        boolean received = income.getStatus() == PersonalFinanceIncomeStatus.RECEIVED;
        addCalendarEvent(eventsByDate, new PersonalFinanceCalendarEvent(
                "calendar-income-" + income.getId(),
                PersonalFinanceAlertCategory.INCOME,
                income.getExpectedDate(),
                income.getTitle(),
                income.getIncomeSource() == null ? "Ingreso" : income.getIncomeSource().getName(),
                money(income.getAmount()),
                income.getCurrency(),
                received ? "Recibido" : income.getStatus().getLabel(),
                "/gasto-claro/income-events?year=" + selectedMonth.getYear() + "&month=" + selectedMonth.getMonthValue(),
                received ? "calendar-event-complete" : "calendar-event-income",
                received ? "bi-check-circle" : "bi-wallet2"
        ));
    }

    private void addNegotiationCalendarEvents(
            Map<LocalDate, List<PersonalFinanceCalendarEvent>> eventsByDate,
            PersonalFinanceDebtNegotiation entry,
            YearMonth selectedMonth
    ) {
        String debtName = entry.getDebt() == null ? "Deuda" : entry.getDebt().getName();
        String actionUrl = "/gasto-claro/negotiations/" + entry.getId() + "/edit";
        if (!entry.getStatus().isTerminal()) {
            addNegotiationCalendarEvent(eventsByDate, selectedMonth, entry.getNextActionDate(),
                    "Seguimiento: " + debtName,
                    entry.getNextAction() == null || entry.getNextAction().isBlank() ? "Próxima acción" : entry.getNextAction(),
                    "Seguimiento", actionUrl, "calendar-event-negotiation");
            addNegotiationCalendarEvent(eventsByDate, selectedMonth, entry.getResponseDeadline(),
                    "Respuesta: " + debtName,
                    "Fecha límite de respuesta",
                    "Respuesta", actionUrl, "calendar-event-negotiation");
        }
        if (entry.getStatus().isAccepted()) {
            addNegotiationCalendarEvent(eventsByDate, selectedMonth, entry.getFirstPaymentDate(),
                    "Acuerdo: " + debtName,
                    "Primer pago del acuerdo aceptado",
                    "Primer pago", actionUrl, "calendar-event-agreement");
        }
    }

    private void addNegotiationCalendarEvent(
            Map<LocalDate, List<PersonalFinanceCalendarEvent>> eventsByDate,
            YearMonth selectedMonth,
            LocalDate date,
            String title,
            String detail,
            String statusLabel,
            String actionUrl,
            String cssClass
    ) {
        if (date == null || !YearMonth.from(date).equals(selectedMonth)) {
            return;
        }
        addCalendarEvent(eventsByDate, new PersonalFinanceCalendarEvent(
                "calendar-negotiation-" + title + "-" + date,
                PersonalFinanceAlertCategory.NEGOTIATION,
                date,
                title,
                detail,
                null,
                null,
                statusLabel,
                actionUrl,
                cssClass,
                "bi-chat-left-text"
        ));
    }

    private void addCalendarEvent(
            Map<LocalDate, List<PersonalFinanceCalendarEvent>> eventsByDate,
            PersonalFinanceCalendarEvent event
    ) {
        eventsByDate.computeIfAbsent(event.date(), ignored -> new ArrayList<>()).add(event);
    }

    private List<PersonalFinanceCalendarDay> calendarDays(
            YearMonth month,
            LocalDate today,
            Map<LocalDate, List<PersonalFinanceCalendarEvent>> eventsByDate
    ) {
        LocalDate first = month.atDay(1);
        LocalDate last = month.atEndOfMonth();
        LocalDate gridStart = first.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDate gridEnd = last.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY));
        List<PersonalFinanceCalendarDay> days = new ArrayList<>();
        for (LocalDate date = gridStart; !date.isAfter(gridEnd); date = date.plusDays(1)) {
            List<PersonalFinanceCalendarEvent> events = new ArrayList<>(eventsByDate.getOrDefault(date, List.of()));
            events.sort(calendarEventComparator());
            days.add(new PersonalFinanceCalendarDay(
                    date,
                    YearMonth.from(date).equals(month),
                    date.equals(today),
                    List.copyOf(events)
            ));
        }
        return List.copyOf(days);
    }

    private Comparator<PersonalFinanceCalendarEvent> calendarEventComparator() {
        return Comparator
                .comparingInt((PersonalFinanceCalendarEvent event) -> switch (event.category()) {
                    case PAYMENT -> 0;
                    case INCOME -> 1;
                    case NEGOTIATION -> 2;
                    case ALL -> 3;
                })
                .thenComparing(PersonalFinanceCalendarEvent::title, String.CASE_INSENSITIVE_ORDER);
    }

    private PersonalFinanceAlertSummary summary(
            List<PersonalFinanceAlertItem> alerts,
            List<PersonalFinancePaymentObligation> obligations,
            List<PersonalFinanceIncomeEvent> incomes,
            List<PersonalFinanceDebtNegotiation> negotiations,
            LocalDate today
    ) {
        long overduePayments = obligations.stream()
                .filter(obligation -> !isClosed(obligation))
                .filter(obligation -> obligation.getDueDate() != null && obligation.getDueDate().isBefore(today))
                .count();
        long dueToday = alerts.stream().filter(PersonalFinanceAlertItem::today).count();
        long upcomingSevenDays = alerts.stream()
                .filter(item -> item.daysFromToday() > 0 && item.daysFromToday() <= 7)
                .count();
        long partialPayments = obligations.stream()
                .filter(obligation -> !isClosed(obligation))
                .filter(obligation -> obligation.getStatus() == PersonalFinanceObligationStatus.PARTIAL
                        || (safe(obligation.getAmountPaid()).signum() > 0 && obligation.pendingAmount().signum() > 0))
                .count();
        long pendingIncomes = incomes.stream()
                .filter(income -> income.getStatus() != PersonalFinanceIncomeStatus.RECEIVED
                        && income.getStatus() != PersonalFinanceIncomeStatus.CANCELLED)
                .count();
        long negotiationFollowUps = negotiations.stream()
                .filter(entry -> !entry.getStatus().isTerminal())
                .filter(entry -> (entry.getNextActionDate() != null && !entry.getNextActionDate().isAfter(today))
                        || (entry.getResponseDeadline() != null && !entry.getResponseDeadline().isAfter(today)))
                .count();
        BigDecimal pendingPen = obligations.stream()
                .filter(obligation -> !isClosed(obligation))
                .filter(obligation -> obligation.getCurrency() == PersonalFinanceCurrency.PEN)
                .map(PersonalFinancePaymentObligation::pendingAmount)
                .reduce(ZERO, BigDecimal::add);
        BigDecimal pendingUsd = obligations.stream()
                .filter(obligation -> !isClosed(obligation))
                .filter(obligation -> obligation.getCurrency() == PersonalFinanceCurrency.USD)
                .map(PersonalFinancePaymentObligation::pendingAmount)
                .reduce(ZERO, BigDecimal::add);
        return new PersonalFinanceAlertSummary(
                overduePayments,
                dueToday,
                upcomingSevenDays,
                partialPayments,
                pendingIncomes,
                negotiationFollowUps,
                money(pendingPen),
                money(pendingUsd)
        );
    }

    private boolean matchesScope(PersonalFinanceAlertItem item, PersonalFinanceAlertScope scope) {
        return switch (scope) {
            case ALL -> true;
            case URGENT -> item.overdue() || item.today();
            case NEXT_7_DAYS -> item.daysFromToday() >= 0 && item.daysFromToday() <= 7;
            case NEXT_15_DAYS -> item.daysFromToday() >= 0 && item.daysFromToday() <= 15;
            case PARTIAL -> item.partial();
        };
    }

    private boolean isClosed(PersonalFinancePaymentObligation obligation) {
        return obligation.getStatus() == PersonalFinanceObligationStatus.CANCELLED || obligation.isPaidLike();
    }

    private BigDecimal safe(BigDecimal value) {
        return value == null ? ZERO : value;
    }

    private BigDecimal money(BigDecimal value) {
        return safe(value).setScale(2, RoundingMode.HALF_UP);
    }

    private String formatMoney(BigDecimal value) {
        return money(value).toPlainString();
    }

    private String monthLabel(YearMonth month) {
        Locale locale = Locale.forLanguageTag("es-PE");
        String monthName = month.getMonth().getDisplayName(TextStyle.FULL, locale);
        if (monthName == null || monthName.isBlank()) {
            monthName = month.getMonth().name().toLowerCase(locale);
        }
        return Character.toUpperCase(monthName.charAt(0)) + monthName.substring(1) + " " + month.getYear();
    }
}
