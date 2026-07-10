package com.ecoamazonas.eco_agua.personalfinance;

import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.YearMonth;
import java.util.List;

@Controller
@RequestMapping("/gasto-claro")
public class PersonalFinancePaymentController {

    private final PersonalFinancePaymentService paymentService;
    private final PersonalFinanceService personalFinanceService;

    public PersonalFinancePaymentController(
            PersonalFinancePaymentService paymentService,
            PersonalFinanceService personalFinanceService
    ) {
        this.paymentService = paymentService;
        this.personalFinanceService = personalFinanceService;
    }

    @GetMapping("/payments")
    public String payments(
            @RequestParam(name = "year", required = false) Integer year,
            @RequestParam(name = "month", required = false) Integer month,
            @RequestParam(name = "debtId", required = false) Long debtId,
            @RequestParam(name = "all", defaultValue = "false") boolean includeAllMonths,
            Model model
    ) {
        YearMonth selectedMonth = selectedMonth(year, month);
        List<PersonalFinancePaymentView> payments = paymentService.payments(selectedMonth, debtId, includeAllMonths);
        model.addAttribute("activePage", "gasto_claro_payments");
        model.addAttribute("selectedYear", selectedMonth.getYear());
        model.addAttribute("selectedMonth", selectedMonth.getMonthValue());
        model.addAttribute("selectedDebtId", debtId);
        model.addAttribute("includeAllMonths", includeAllMonths);
        model.addAttribute("payments", payments);
        model.addAttribute("summary", paymentService.summary(payments));
        model.addAttribute("debts", personalFinanceService.debts());
        return "personal_finance/payments";
    }

    @GetMapping("/debts/{id}/payments")
    public String debtPayments(
            @PathVariable("id") Long debtId,
            @RequestParam(name = "year", required = false) Integer year,
            @RequestParam(name = "month", required = false) Integer month,
            @RequestParam(name = "all", defaultValue = "true") boolean includeAllMonths
    ) {
        YearMonth selectedMonth = selectedMonth(year, month);
        return "redirect:/gasto-claro/payments?debtId=" + debtId
                + "&year=" + selectedMonth.getYear()
                + "&month=" + selectedMonth.getMonthValue()
                + "&all=" + includeAllMonths;
    }

    @GetMapping("/obligations/{id}/payment")
    public String paymentForm(
            @PathVariable("id") Long obligationId,
            @RequestParam(name = "returnTo", required = false) String returnTo,
            Model model
    ) {
        PersonalFinancePaymentContext context = paymentService.paymentContext(obligationId);
        model.addAttribute("activePage", "gasto_claro_payments");
        model.addAttribute("paymentContext", context);
        model.addAttribute("paymentForm", context.form());
        model.addAttribute("paymentMethods", PersonalFinancePaymentMethod.values());
        model.addAttribute("recentPayments", paymentService.obligationPayments(obligationId));
        model.addAttribute("returnTo", safeReturnTo(returnTo));
        return "personal_finance/payment_form";
    }

    @PostMapping("/obligations/{id}/payment")
    public String registerPayment(
            @PathVariable("id") Long obligationId,
            @ModelAttribute("paymentForm") PersonalFinancePaymentForm form,
            @RequestParam(name = "receipt", required = false) MultipartFile receipt,
            @RequestParam(name = "returnTo", required = false) String returnTo,
            RedirectAttributes redirectAttributes
    ) {
        String safeReturnTo = safeReturnTo(returnTo);
        try {
            paymentService.registerPayment(obligationId, form, receipt);
            redirectAttributes.addFlashAttribute("message", "Pago registrado y saldos actualizados correctamente.");
            redirectAttributes.addFlashAttribute("messageType", "success");
            return "redirect:" + safeReturnTo;
        } catch (IllegalArgumentException | IOException exception) {
            redirectAttributes.addFlashAttribute("message", exception.getMessage());
            redirectAttributes.addFlashAttribute("messageType", "danger");
            return "redirect:/gasto-claro/obligations/" + obligationId + "/payment?returnTo=" + encodeReturnTo(safeReturnTo);
        }
    }

    @PostMapping("/payments/{id}/reverse")
    public String reversePayment(
            @PathVariable("id") Long paymentId,
            @RequestParam("reason") String reason,
            @RequestParam(name = "returnTo", required = false) String returnTo,
            RedirectAttributes redirectAttributes
    ) {
        String safeReturnTo = safeReturnTo(returnTo);
        try {
            paymentService.reversePayment(paymentId, reason);
            redirectAttributes.addFlashAttribute("message", "Pago revertido. El historial se conserva y los saldos fueron restaurados.");
            redirectAttributes.addFlashAttribute("messageType", "success");
        } catch (IllegalArgumentException exception) {
            redirectAttributes.addFlashAttribute("message", exception.getMessage());
            redirectAttributes.addFlashAttribute("messageType", "danger");
        }
        return "redirect:" + safeReturnTo;
    }

    @GetMapping("/payments/{publicId}/receipt")
    public ResponseEntity<Resource> receipt(@PathVariable("publicId") String publicId) {
        PersonalFinancePaymentReceipt receipt = paymentService.receipt(publicId);
        MediaType mediaType;
        try {
            mediaType = receipt.contentType() == null
                    ? MediaType.APPLICATION_OCTET_STREAM
                    : MediaType.parseMediaType(receipt.contentType());
        } catch (IllegalArgumentException exception) {
            mediaType = MediaType.APPLICATION_OCTET_STREAM;
        }
        ContentDisposition disposition = ContentDisposition.attachment()
                .filename(receipt.originalName(), StandardCharsets.UTF_8)
                .build();
        return ResponseEntity.ok()
                .contentType(mediaType)
                .contentLength(receipt.sizeBytes())
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .body(new FileSystemResource(receipt.path()));
    }

    private YearMonth selectedMonth(Integer year, Integer month) {
        YearMonth current = YearMonth.now();
        int selectedYear = year == null ? current.getYear() : year;
        int selectedMonth = month == null ? current.getMonthValue() : Math.max(1, Math.min(12, month));
        return YearMonth.of(selectedYear, selectedMonth);
    }

    private String safeReturnTo(String returnTo) {
        if (returnTo == null || returnTo.isBlank() || !returnTo.startsWith("/gasto-claro/")) {
            return "/gasto-claro/payments";
        }
        if (returnTo.contains("\r") || returnTo.contains("\n") || returnTo.startsWith("//")) {
            return "/gasto-claro/payments";
        }
        return returnTo;
    }

    private String encodeReturnTo(String returnTo) {
        return java.net.URLEncoder.encode(returnTo, StandardCharsets.UTF_8);
    }
}
