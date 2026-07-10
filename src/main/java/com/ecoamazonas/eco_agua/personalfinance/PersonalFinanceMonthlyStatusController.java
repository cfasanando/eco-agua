package com.ecoamazonas.eco_agua.personalfinance;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.time.YearMonth;
import java.util.LinkedHashMap;
import java.util.Map;

@Controller
@RequestMapping("/gasto-claro/monthly-plan")
public class PersonalFinanceMonthlyStatusController {

    private final PersonalFinanceService service;

    public PersonalFinanceMonthlyStatusController(PersonalFinanceService service) {
        this.service = service;
    }

    @PostMapping("/obligations/{id}/payment-status")
    @ResponseBody
    public Map<String, Object> updatePaymentStatus(
            @PathVariable("id") Long id,
            @RequestParam("paid") boolean paid,
            @RequestParam(name = "year", required = false) Integer year,
            @RequestParam(name = "month", required = false) Integer month
    ) {
        Map<String, Object> response = new LinkedHashMap<>();
        try {
            PersonalFinancePaymentObligation obligation = service.setPaymentObligationPaid(id, paid);
            YearMonth selectedMonth = selectedMonth(year, month);
            PersonalFinanceMonthlyPlan plan = service.monthlyPlan(selectedMonth);

            response.put("success", true);
            response.put("id", obligation.getId());
            response.put("paid", obligation.isPaidLike());
            response.put("status", obligation.getStatus().name());
            response.put("statusLabel", obligation.getStatus().getLabel());
            response.put("amountPaid", obligation.getAmountPaid());
            response.put("pendingAmount", obligation.pendingAmount());

            findPlanItem(plan, obligation.getId()).ifPresent(item -> appendDebtItem(response, item));
            appendDebtPortfolioSummary(response, plan.debtPortfolio());
            appendLiveSummary(response, service.monthlyLiveSummary(selectedMonth, PersonalFinanceCurrency.PEN));
        } catch (IllegalArgumentException exception) {
            response.put("success", false);
            response.put("message", exception.getMessage());
        }
        return response;
    }

    @PostMapping("/incomes/{id}/received-status")
    @ResponseBody
    public Map<String, Object> updateIncomeStatus(
            @PathVariable("id") Long id,
            @RequestParam("received") boolean received,
            @RequestParam(name = "year", required = false) Integer year,
            @RequestParam(name = "month", required = false) Integer month
    ) {
        Map<String, Object> response = new LinkedHashMap<>();
        try {
            PersonalFinanceIncomeEvent event = service.setIncomeEventReceived(id, received);
            response.put("success", true);
            response.put("id", event.getId());
            response.put("received", event.getStatus() == PersonalFinanceIncomeStatus.RECEIVED);
            response.put("status", event.getStatus().name());
            response.put("statusLabel", event.getStatus().getLabel());
            response.put("receivedDate", event.getReceivedDate());
            appendLiveSummary(
                    response,
                    service.monthlyLiveSummary(selectedMonth(year, month), PersonalFinanceCurrency.PEN)
            );
        } catch (IllegalArgumentException exception) {
            response.put("success", false);
            response.put("message", exception.getMessage());
        }
        return response;
    }

    private java.util.Optional<PersonalFinanceMonthlyPlanItem> findPlanItem(
            PersonalFinanceMonthlyPlan plan,
            Long obligationId
    ) {
        return java.util.stream.Stream.of(
                        plan.basicLivingItems(),
                        plan.debtItems(),
                        plan.otherItems()
                )
                .flatMap(java.util.Collection::stream)
                .filter(item -> item.id().equals(obligationId))
                .findFirst();
    }

    private void appendDebtItem(Map<String, Object> response, PersonalFinanceMonthlyPlanItem item) {
        response.put("debtId", item.debtId());
        response.put("debtBalanceKnown", item.debtBalanceKnown());
        response.put("debtOutstandingBalance", item.debtOutstandingBalance());
        response.put("settlementOpportunity", item.settlementOpportunity());
        response.put("settlementGap", item.settlementGap());
    }

    private void appendDebtPortfolioSummary(
            Map<String, Object> response,
            PersonalFinanceDebtPortfolioSummary summary
    ) {
        response.put("debtBankPenTotal", summary.bankPenTotal());
        response.put("debtLenderPenTotal", summary.lenderPenTotal());
        response.put("debtDirectPenTotal", summary.directPenTotal());
        response.put("debtCommitmentPenTotal", summary.commitmentPenTotal());
        response.put("debtOtherPenTotal", summary.otherPenTotal());
        response.put("debtKnownPenTotal", summary.knownPenTotal());
        response.put("debtKnownUsdTotal", summary.knownUsdTotal());
        response.put("debtKnownBalanceCount", summary.knownBalanceCount());
        response.put("debtUndefinedBalanceCount", summary.undefinedBalanceCount());
        response.put("debtSettlementOpportunityCount", summary.settlementOpportunityCount());
    }

    private void appendLiveSummary(Map<String, Object> response, PersonalFinanceMonthlyLiveSummary summary) {
        response.put("expectedIncome", summary.expectedIncome());
        response.put("receivedIncome", summary.receivedIncome());
        response.put("paidTotal", summary.paidTotal());
        response.put("pendingTotal", summary.pendingTotal());
        response.put("realBalance", summary.realBalance());
        response.put("projectedBalance", summary.projectedBalance());
        response.put("totalPayments", summary.totalPayments());
        response.put("paidPayments", summary.paidPayments());
        response.put("pendingPayments", summary.pendingPayments());
        response.put("totalIncomes", summary.totalIncomes());
        response.put("receivedIncomes", summary.receivedIncomes());
        response.put("completionPercentage", summary.completionPercentage());
    }

    private YearMonth selectedMonth(Integer year, Integer month) {
        YearMonth now = YearMonth.now();
        int safeYear = year == null ? now.getYear() : year;
        int safeMonth = month == null ? now.getMonthValue() : Math.max(1, Math.min(12, month));
        return YearMonth.of(safeYear, safeMonth);
    }
}
