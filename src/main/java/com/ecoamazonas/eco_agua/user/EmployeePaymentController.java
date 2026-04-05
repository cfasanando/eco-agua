package com.ecoamazonas.eco_agua.user;

import com.ecoamazonas.eco_agua.expense.PersonnelExpenseCalculation;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Controller
@RequestMapping("/admin/personnel/payments")
public class EmployeePaymentController {

    private final EmployeePaymentService employeePaymentService;

    public EmployeePaymentController(EmployeePaymentService employeePaymentService) {
        this.employeePaymentService = employeePaymentService;
    }

    @GetMapping
    public String index(
            @RequestParam(name = "employeeId", required = false) Long employeeId,
            @RequestParam(name = "year", required = false) Integer year,
            @RequestParam(name = "month", required = false) Integer month,
            @RequestParam(name = "editObligationId", required = false) Long editObligationId,
            @RequestParam(name = "settleObligationId", required = false) Long settleObligationId,
            Model model
    ) {
        LocalDate today = LocalDate.now();
        int selectedYear = year != null ? year : today.getYear();
        int selectedMonth = month != null ? month : today.getMonthValue();

        List<Employee> employees = employeePaymentService.findActiveEmployees();
        Employee selectedEmployee = employeePaymentService.findEmployee(employeeId);
        PersonnelExpenseCalculation suggestedCalculation = null;
        EmployeeObligationForm editObligationForm = null;
        BigDecimal editObligationAppliedAmount = BigDecimal.ZERO;
        boolean editObligationHasMovements = false;
        EmployeeObligationSettlementForm settlementForm = buildSettlementForm(employeeId, settleObligationId);

        if (selectedEmployee != null) {
            suggestedCalculation = employeePaymentService.getSuggestedCalculation(employeeId, today);

            if (editObligationId != null) {
                editObligationForm = employeePaymentService.buildEditObligationForm(employeeId, editObligationId);
                editObligationAppliedAmount = employeePaymentService.getAppliedAmount(editObligationId);
                editObligationHasMovements = employeePaymentService.hasAppliedMovements(editObligationId);
            }

            if (settleObligationId != null) {
                settlementForm = employeePaymentService.buildSettlementForm(employeeId, settleObligationId);
            }
        }

        model.addAttribute("activePage", "personnel_payments");
        model.addAttribute("employees", employees);
        model.addAttribute("selectedEmployee", selectedEmployee);
        model.addAttribute("selectedEmployeeId", employeeId);
        model.addAttribute("selectedYear", selectedYear);
        model.addAttribute("selectedMonth", selectedMonth);
        model.addAttribute("payments", employeePaymentService.findPaymentsForMonth(employeeId, selectedYear, selectedMonth));
        model.addAttribute("settlements", employeePaymentService.findSettlementsForMonth(employeeId, selectedYear, selectedMonth));
        model.addAttribute("obligations", employeePaymentService.findObligations(employeeId));
        model.addAttribute("activeObligations", employeePaymentService.findActiveObligations(employeeId));
        model.addAttribute("summary", employeePaymentService.buildMonthlySummary(employeeId, selectedYear, selectedMonth));
        model.addAttribute("suggestedCalculation", suggestedCalculation);
        model.addAttribute("paymentForm", buildPaymentForm(employeeId));
        model.addAttribute("obligationForm", buildObligationForm(employeeId));
        model.addAttribute("settlementForm", settlementForm);
        model.addAttribute("editObligationForm", editObligationForm);
        model.addAttribute("editObligationAppliedAmount", editObligationAppliedAmount);
        model.addAttribute("editObligationHasMovements", editObligationHasMovements);
        model.addAttribute("obligationTypes", EmployeeObligationType.values());
        model.addAttribute("obligationDiscountModes", EmployeeObligationDiscountMode.values());

        return "admin/personnel_payments";
    }

    @PostMapping("/save")
    public String savePayment(
            @ModelAttribute("paymentForm") EmployeePaymentForm paymentForm,
            RedirectAttributes redirectAttributes
    ) {
        Long employeeId = paymentForm.getEmployeeId();
        LocalDate paymentDate = paymentForm.getPaymentDate() != null ? paymentForm.getPaymentDate() : LocalDate.now();

        try {
            employeePaymentService.registerPayment(paymentForm);
            redirectAttributes.addFlashAttribute("message", "Pago registrado correctamente.");
            redirectAttributes.addFlashAttribute("messageType", "success");
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("message", "Error al registrar pago: " + ex.getMessage());
            redirectAttributes.addFlashAttribute("messageType", "error");
        }

        return buildRedirectUrl(employeeId, paymentDate.getYear(), paymentDate.getMonthValue(), null, null);
    }

    @PostMapping("/obligations/save")
    public String saveObligation(
            @ModelAttribute("obligationForm") EmployeeObligationForm obligationForm,
            RedirectAttributes redirectAttributes
    ) {
        Long employeeId = obligationForm.getEmployeeId();
        LocalDate issueDate = obligationForm.getIssueDate() != null ? obligationForm.getIssueDate() : LocalDate.now();

        try {
            employeePaymentService.registerObligation(obligationForm);
            redirectAttributes.addFlashAttribute("message", "Obligación registrada correctamente.");
            redirectAttributes.addFlashAttribute("messageType", "success");
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("message", "Error al registrar obligación: " + ex.getMessage());
            redirectAttributes.addFlashAttribute("messageType", "error");
        }

        return buildRedirectUrl(employeeId, issueDate.getYear(), issueDate.getMonthValue(), null, null);
    }

    @PostMapping("/obligations/settlements/save")
    public String saveDirectSettlement(
            @ModelAttribute("settlementForm") EmployeeObligationSettlementForm settlementForm,
            RedirectAttributes redirectAttributes
    ) {
        Long employeeId = settlementForm.getEmployeeId();
        LocalDate settlementDate = settlementForm.getSettlementDate() != null ? settlementForm.getSettlementDate() : LocalDate.now();

        try {
            BigDecimal totalApplied = employeePaymentService.registerDirectSettlement(settlementForm);
            redirectAttributes.addFlashAttribute("message", "Abono registrado correctamente. Total aplicado: S/. " + totalApplied.setScale(2));
            redirectAttributes.addFlashAttribute("messageType", "success");
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("message", "Error al registrar abono: " + ex.getMessage());
            redirectAttributes.addFlashAttribute("messageType", "error");
        }

        return buildRedirectUrl(employeeId, settlementDate.getYear(), settlementDate.getMonthValue(), null, null);
    }

    @PostMapping("/obligations/update")
    public String updateObligation(
            @ModelAttribute("editObligationForm") EmployeeObligationForm obligationForm,
            @RequestParam(name = "year", required = false) Integer year,
            @RequestParam(name = "month", required = false) Integer month,
            RedirectAttributes redirectAttributes
    ) {
        Long employeeId = obligationForm.getEmployeeId();

        try {
            employeePaymentService.updateObligation(obligationForm);
            redirectAttributes.addFlashAttribute("message", "Obligación actualizada correctamente.");
            redirectAttributes.addFlashAttribute("messageType", "success");
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("message", "Error al actualizar obligación: " + ex.getMessage());
            redirectAttributes.addFlashAttribute("messageType", "error");
        }

        return buildRedirectUrl(employeeId, safeYear(year), safeMonth(month), null, null);
    }

    @PostMapping("/obligations/{id}/close")
    public String closeObligation(
            @PathVariable("id") Long obligationId,
            @RequestParam("employeeId") Long employeeId,
            @RequestParam(name = "year", required = false) Integer year,
            @RequestParam(name = "month", required = false) Integer month,
            RedirectAttributes redirectAttributes
    ) {
        try {
            employeePaymentService.closeObligation(employeeId, obligationId);
            redirectAttributes.addFlashAttribute("message", "Obligación cerrada correctamente.");
            redirectAttributes.addFlashAttribute("messageType", "success");
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("message", "Error al cerrar obligación: " + ex.getMessage());
            redirectAttributes.addFlashAttribute("messageType", "error");
        }

        return buildRedirectUrl(employeeId, safeYear(year), safeMonth(month), null, null);
    }

    @PostMapping("/obligations/{id}/delete")
    public String deleteObligation(
            @PathVariable("id") Long obligationId,
            @RequestParam("employeeId") Long employeeId,
            @RequestParam(name = "year", required = false) Integer year,
            @RequestParam(name = "month", required = false) Integer month,
            RedirectAttributes redirectAttributes
    ) {
        try {
            employeePaymentService.deleteObligation(employeeId, obligationId);
            redirectAttributes.addFlashAttribute("message", "Obligación eliminada correctamente.");
            redirectAttributes.addFlashAttribute("messageType", "success");
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("message", "Error al eliminar obligación: " + ex.getMessage());
            redirectAttributes.addFlashAttribute("messageType", "error");
        }

        return buildRedirectUrl(employeeId, safeYear(year), safeMonth(month), null, null);
    }

    private EmployeePaymentForm buildPaymentForm(Long employeeId) {
        EmployeePaymentForm form = new EmployeePaymentForm();
        form.setEmployeeId(employeeId);
        form.setPaymentDate(LocalDate.now());
        return form;
    }

    private EmployeeObligationForm buildObligationForm(Long employeeId) {
        EmployeeObligationForm form = new EmployeeObligationForm();
        form.setEmployeeId(employeeId);
        form.setIssueDate(LocalDate.now());
        form.setActive(true);
        return form;
    }

    private EmployeeObligationSettlementForm buildSettlementForm(Long employeeId, Long obligationId) {
        EmployeeObligationSettlementForm form = new EmployeeObligationSettlementForm();
        form.setEmployeeId(employeeId);
        form.setSettlementDate(LocalDate.now());
        form.setObligationId(obligationId);
        return form;
    }

    private int safeYear(Integer year) {
        return year != null ? year : LocalDate.now().getYear();
    }

    private int safeMonth(Integer month) {
        return month != null ? month : LocalDate.now().getMonthValue();
    }

    private String buildRedirectUrl(Long employeeId, int year, int month, Long editObligationId, Long settleObligationId) {
        StringBuilder builder = new StringBuilder();
        builder.append("redirect:/admin/personnel/payments?employeeId=")
                .append(employeeId != null ? employeeId : 0L)
                .append("&year=")
                .append(year)
                .append("&month=")
                .append(month);

        if (editObligationId != null && editObligationId > 0) {
            builder.append("&editObligationId=").append(editObligationId);
        }

        if (settleObligationId != null && settleObligationId > 0) {
            builder.append("&settleObligationId=").append(settleObligationId);
        }

        return builder.toString();
    }
}
