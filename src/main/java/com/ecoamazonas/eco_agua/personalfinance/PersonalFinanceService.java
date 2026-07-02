package com.ecoamazonas.eco_agua.personalfinance;

import com.ecoamazonas.eco_agua.user.UserAccount;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

@Service
public class PersonalFinanceService {

    private final PersonalFinanceDebtRepository debtRepository;
    private final PersonalFinanceFixedExpenseRepository fixedExpenseRepository;
    private final PersonalFinanceIncomeSourceRepository incomeSourceRepository;
    private final PersonalFinanceIncomeEventRepository incomeEventRepository;
    private final PersonalFinanceCurrentUserService currentUserService;

    public PersonalFinanceService(
            PersonalFinanceDebtRepository debtRepository,
            PersonalFinanceFixedExpenseRepository fixedExpenseRepository,
            PersonalFinanceIncomeSourceRepository incomeSourceRepository,
            PersonalFinanceIncomeEventRepository incomeEventRepository,
            PersonalFinanceCurrentUserService currentUserService
    ) {
        this.debtRepository = debtRepository;
        this.fixedExpenseRepository = fixedExpenseRepository;
        this.incomeSourceRepository = incomeSourceRepository;
        this.incomeEventRepository = incomeEventRepository;
        this.currentUserService = currentUserService;
    }

    @Transactional(readOnly = true)
    public PersonalFinanceDashboard dashboard(YearMonth yearMonth) {
        UserAccount user = currentUserService.currentUser();
        LocalDate start = yearMonth.atDay(1);
        LocalDate end = yearMonth.atEndOfMonth();

        List<PersonalFinanceIncomeEvent> incomeEvents = incomeEventRepository.findByUserAndExpectedDateBetweenOrderByExpectedDateAscIdAsc(user, start, end);
        List<PersonalFinanceFixedExpense> fixedExpenses = fixedExpenseRepository.findByUserAndActiveTrueOrderByDueDayAscNameAsc(user);
        List<PersonalFinanceDebt> activeDebts = debtRepository.findByUserAndStatusOrderByDueDayAscNameAsc(user, PersonalFinanceDebtStatus.ACTIVE);

        BigDecimal expectedIncome = incomeEvents.stream()
                .filter(event -> event.getStatus() != PersonalFinanceIncomeStatus.CANCELLED && event.getStatus() != PersonalFinanceIncomeStatus.MISSED)
                .map(PersonalFinanceIncomeEvent::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal receivedIncome = incomeEvents.stream()
                .filter(event -> event.getStatus() == PersonalFinanceIncomeStatus.RECEIVED)
                .map(PersonalFinanceIncomeEvent::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal fixedExpenseTotal = fixedExpenses.stream()
                .filter(expense -> expense.getFrequency() == PersonalFinanceFrequency.MONTHLY)
                .map(PersonalFinanceFixedExpense::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal debtPressureTotal = activeDebts.stream()
                .map(PersonalFinanceDebt::monthlyPressure)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal projectedBalance = expectedIncome.subtract(fixedExpenseTotal).subtract(debtPressureTotal);

        return new PersonalFinanceDashboard(
                expectedIncome,
                receivedIncome,
                fixedExpenseTotal,
                debtPressureTotal,
                projectedBalance,
                activeDebts.size(),
                fixedExpenses.size(),
                incomeEvents.size(),
                yearMonth.getYear(),
                yearMonth.getMonthValue()
        );
    }

    @Transactional(readOnly = true)
    public List<PersonalFinanceDebt> debts() {
        return debtRepository.findByUserOrderByStatusAscDueDayAscNameAsc(currentUserService.currentUser());
    }

    @Transactional
    public void saveDebt(PersonalFinanceDebt debt) {
        UserAccount user = currentUserService.currentUser();
        PersonalFinanceDebt entity = debt.getId() == null
                ? new PersonalFinanceDebt()
                : debtRepository.findByIdAndUser(debt.getId(), user).orElseThrow();
        entity.setUser(user);
        entity.setName(clean(debt.getName()));
        entity.setCreditorName(clean(debt.getCreditorName()));
        entity.setDebtType(defaultEnum(debt.getDebtType(), PersonalFinanceDebtType.CREDIT_CARD));
        entity.setCurrency(defaultEnum(debt.getCurrency(), PersonalFinanceCurrency.PEN));
        entity.setOriginalAmount(defaultAmount(debt.getOriginalAmount()));
        entity.setCurrentBalance(defaultAmount(debt.getCurrentBalance()));
        entity.setMonthlyDueAmount(defaultAmount(debt.getMonthlyDueAmount()));
        entity.setMinimumPayment(defaultAmount(debt.getMinimumPayment()));
        entity.setInterestRateMonthly(defaultAmount(debt.getInterestRateMonthly()));
        entity.setDueDay(normalizeDueDay(debt.getDueDay()));
        entity.setStatus(defaultEnum(debt.getStatus(), PersonalFinanceDebtStatus.ACTIVE));
        entity.setFixedPayment(debt.isFixedPayment());
        entity.setNotes(clean(debt.getNotes()));
        debtRepository.save(entity);
    }

    @Transactional(readOnly = true)
    public PersonalFinanceDebt debt(Long id) {
        return debtRepository.findByIdAndUser(id, currentUserService.currentUser()).orElseThrow();
    }

    @Transactional
    public void deleteDebt(Long id) {
        debtRepository.findByIdAndUser(id, currentUserService.currentUser()).ifPresent(debtRepository::delete);
    }

    @Transactional(readOnly = true)
    public List<PersonalFinanceFixedExpense> fixedExpenses() {
        return fixedExpenseRepository.findByUserOrderByActiveDescDueDayAscNameAsc(currentUserService.currentUser());
    }

    @Transactional
    public void saveFixedExpense(PersonalFinanceFixedExpense expense) {
        UserAccount user = currentUserService.currentUser();
        PersonalFinanceFixedExpense entity = expense.getId() == null
                ? new PersonalFinanceFixedExpense()
                : fixedExpenseRepository.findByIdAndUser(expense.getId(), user).orElseThrow();
        entity.setUser(user);
        entity.setName(clean(expense.getName()));
        entity.setCategory(defaultEnum(expense.getCategory(), PersonalFinanceExpenseCategory.OTHER));
        entity.setAmount(defaultAmount(expense.getAmount()));
        entity.setCurrency(defaultEnum(expense.getCurrency(), PersonalFinanceCurrency.PEN));
        entity.setDueDay(normalizeDueDay(expense.getDueDay()));
        entity.setFrequency(defaultEnum(expense.getFrequency(), PersonalFinanceFrequency.MONTHLY));
        entity.setMandatory(expense.isMandatory());
        entity.setActive(expense.isActive());
        entity.setNotes(clean(expense.getNotes()));
        fixedExpenseRepository.save(entity);
    }

    @Transactional(readOnly = true)
    public PersonalFinanceFixedExpense fixedExpense(Long id) {
        return fixedExpenseRepository.findByIdAndUser(id, currentUserService.currentUser()).orElseThrow();
    }

    @Transactional
    public void deleteFixedExpense(Long id) {
        fixedExpenseRepository.findByIdAndUser(id, currentUserService.currentUser()).ifPresent(fixedExpenseRepository::delete);
    }

    @Transactional(readOnly = true)
    public List<PersonalFinanceIncomeSource> incomeSources() {
        return incomeSourceRepository.findByUserOrderByActiveDescNameAsc(currentUserService.currentUser());
    }

    @Transactional(readOnly = true)
    public List<PersonalFinanceIncomeSource> activeIncomeSources() {
        return incomeSourceRepository.findByUserAndActiveTrueOrderByNameAsc(currentUserService.currentUser());
    }

    @Transactional
    public void saveIncomeSource(PersonalFinanceIncomeSource source) {
        UserAccount user = currentUserService.currentUser();
        PersonalFinanceIncomeSource entity = source.getId() == null
                ? new PersonalFinanceIncomeSource()
                : incomeSourceRepository.findByIdAndUser(source.getId(), user).orElseThrow();
        entity.setUser(user);
        entity.setName(clean(source.getName()));
        entity.setType(defaultEnum(source.getType(), PersonalFinanceIncomeType.OTHER));
        entity.setDefaultAmount(defaultAmount(source.getDefaultAmount()));
        entity.setCurrency(defaultEnum(source.getCurrency(), PersonalFinanceCurrency.PEN));
        entity.setActive(source.isActive());
        entity.setNotes(clean(source.getNotes()));
        incomeSourceRepository.save(entity);
    }

    @Transactional(readOnly = true)
    public PersonalFinanceIncomeSource incomeSource(Long id) {
        return incomeSourceRepository.findByIdAndUser(id, currentUserService.currentUser()).orElseThrow();
    }

    @Transactional
    public void deleteIncomeSource(Long id) {
        incomeSourceRepository.findByIdAndUser(id, currentUserService.currentUser()).ifPresent(incomeSourceRepository::delete);
    }

    @Transactional(readOnly = true)
    public List<PersonalFinanceIncomeEvent> incomeEvents(YearMonth yearMonth) {
        LocalDate start = yearMonth.atDay(1);
        LocalDate end = yearMonth.atEndOfMonth();
        return incomeEventRepository.findByUserAndExpectedDateBetweenOrderByExpectedDateAscIdAsc(currentUserService.currentUser(), start, end);
    }

    @Transactional
    public void saveIncomeEvent(PersonalFinanceIncomeEvent event, Long sourceId) {
        UserAccount user = currentUserService.currentUser();
        PersonalFinanceIncomeEvent entity = event.getId() == null
                ? new PersonalFinanceIncomeEvent()
                : incomeEventRepository.findByIdAndUser(event.getId(), user).orElseThrow();
        entity.setUser(user);
        entity.setTitle(clean(event.getTitle()));
        entity.setAmount(defaultAmount(event.getAmount()));
        entity.setCurrency(defaultEnum(event.getCurrency(), PersonalFinanceCurrency.PEN));
        entity.setExpectedDate(event.getExpectedDate());
        entity.setReceivedDate(event.getReceivedDate());
        entity.setStatus(defaultEnum(event.getStatus(), PersonalFinanceIncomeStatus.PLANNED));
        entity.setNotes(clean(event.getNotes()));
        if (sourceId != null) {
            entity.setIncomeSource(incomeSourceRepository.findByIdAndUser(sourceId, user).orElse(null));
        } else {
            entity.setIncomeSource(null);
        }
        incomeEventRepository.save(entity);
    }

    @Transactional(readOnly = true)
    public PersonalFinanceIncomeEvent incomeEvent(Long id) {
        return incomeEventRepository.findByIdAndUser(id, currentUserService.currentUser()).orElseThrow();
    }

    @Transactional
    public void deleteIncomeEvent(Long id) {
        incomeEventRepository.findByIdAndUser(id, currentUserService.currentUser()).ifPresent(incomeEventRepository::delete);
    }

    private String clean(String value) {
        return value == null ? null : value.trim();
    }

    private BigDecimal defaultAmount(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private Integer normalizeDueDay(Integer dueDay) {
        if (dueDay == null) {
            return null;
        }
        if (dueDay < 1) {
            return 1;
        }
        if (dueDay > 31) {
            return 31;
        }
        return dueDay;
    }

    private <T> T defaultEnum(T value, T fallback) {
        return value == null ? fallback : value;
    }
}
