package com.ecoamazonas.eco_agua.personalfinance;

import org.springframework.core.io.FileSystemResource;
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
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Controller
@RequestMapping("/gasto-claro")
public class PersonalFinanceDebtNegotiationController {

    private final PersonalFinanceDebtNegotiationService negotiationService;

    public PersonalFinanceDebtNegotiationController(PersonalFinanceDebtNegotiationService negotiationService) {
        this.negotiationService = negotiationService;
    }

    @GetMapping("/negotiations")
    public String negotiations(
            @RequestParam(name = "debtId", required = false) Long debtId,
            @RequestParam(name = "status", required = false) PersonalFinanceNegotiationEntryStatus status,
            Model model
    ) {
        List<PersonalFinanceDebtNegotiation> entries = negotiationService.negotiations(debtId, status);
        model.addAttribute("activePage", "gasto_claro_negotiations");
        model.addAttribute("entries", entries);
        model.addAttribute("summary", negotiationService.summary(entries));
        model.addAttribute("debts", negotiationService.debts());
        model.addAttribute("selectedDebtId", debtId);
        model.addAttribute("selectedStatus", status);
        model.addAttribute("statuses", PersonalFinanceNegotiationEntryStatus.values());
        model.addAttribute("today", LocalDate.now());
        return "personal_finance/negotiations";
    }

    @GetMapping("/negotiations/new")
    public String newNegotiation(
            @RequestParam(name = "debtId", required = false) Long debtId,
            Model model
    ) {
        populateFormModel(model, negotiationService.newForm(debtId), null);
        return "personal_finance/negotiation_form";
    }

    @GetMapping("/negotiations/{id}/edit")
    public String editNegotiation(@PathVariable Long id, Model model) {
        PersonalFinanceDebtNegotiation entry = negotiationService.negotiation(id);
        populateFormModel(model, negotiationService.editForm(id), entry);
        return "personal_finance/negotiation_form";
    }

    @PostMapping("/negotiations")
    public String saveNegotiation(
            @ModelAttribute PersonalFinanceDebtNegotiationForm negotiationForm,
            @RequestParam(name = "evidence", required = false) MultipartFile evidence,
            RedirectAttributes redirectAttributes
    ) {
        try {
            PersonalFinanceDebtNegotiation saved = negotiationService.save(negotiationForm, evidence);
            redirectAttributes.addFlashAttribute("message", "Registro de negociación guardado. No se modificó el saldo ni el cronograma de la deuda.");
            redirectAttributes.addFlashAttribute("messageType", "success");
            return "redirect:/gasto-claro/negotiations?debtId=" + saved.getDebt().getId();
        } catch (IllegalArgumentException exception) {
            redirectAttributes.addFlashAttribute("message", exception.getMessage());
            redirectAttributes.addFlashAttribute("messageType", "danger");
            String path = negotiationForm.getId() == null
                    ? "/gasto-claro/negotiations/new"
                    : "/gasto-claro/negotiations/" + negotiationForm.getId() + "/edit";
            if (negotiationForm.getId() == null && negotiationForm.getDebtId() != null) {
                path += "?debtId=" + negotiationForm.getDebtId();
            }
            return "redirect:" + path;
        } catch (IOException exception) {
            redirectAttributes.addFlashAttribute("message", "No se guardó la negociación porque no se pudo almacenar el archivo de evidencia.");
            redirectAttributes.addFlashAttribute("messageType", "danger");
            return "redirect:/gasto-claro/negotiations";
        }
    }

    @PostMapping("/negotiations/{id}/close")
    public String closeNegotiation(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        PersonalFinanceDebtNegotiation entry = negotiationService.close(id);
        redirectAttributes.addFlashAttribute("message", "Registro de negociación cerrado. La información histórica fue conservada.");
        redirectAttributes.addFlashAttribute("messageType", "success");
        return "redirect:/gasto-claro/negotiations?debtId=" + entry.getDebt().getId();
    }

    @GetMapping("/negotiations/{publicId}/evidence")
    public ResponseEntity<FileSystemResource> evidence(@PathVariable String publicId) {
        PersonalFinanceDebtNegotiationEvidence evidence = negotiationService.evidence(publicId);
        MediaType mediaType;
        try {
            mediaType = evidence.contentType() == null
                    ? MediaType.APPLICATION_OCTET_STREAM
                    : MediaType.parseMediaType(evidence.contentType());
        } catch (IllegalArgumentException exception) {
            mediaType = MediaType.APPLICATION_OCTET_STREAM;
        }
        HttpHeaders headers = new HttpHeaders();
        headers.setContentDisposition(ContentDisposition.attachment()
                .filename(evidence.originalName(), StandardCharsets.UTF_8)
                .build());
        headers.setContentType(mediaType);
        headers.setContentLength(evidence.sizeBytes());
        return ResponseEntity.ok()
                .headers(headers)
                .body(new FileSystemResource(evidence.path()));
    }

    private void populateFormModel(
            Model model,
            PersonalFinanceDebtNegotiationForm form,
            PersonalFinanceDebtNegotiation entry
    ) {
        model.addAttribute("activePage", "gasto_claro_negotiations");
        model.addAttribute("negotiationForm", form);
        model.addAttribute("entry", entry);
        List<PersonalFinanceDebt> debts = new ArrayList<>(negotiationService.debts());
        if (entry != null && debts.stream().noneMatch(debt -> debt.getId().equals(entry.getDebt().getId()))) {
            debts.add(entry.getDebt());
        }
        model.addAttribute("debts", debts);
        model.addAttribute("channels", PersonalFinanceNegotiationChannel.values());
        model.addAttribute("statuses", PersonalFinanceNegotiationEntryStatus.values());
        if (entry != null) {
            model.addAttribute("comparison", negotiationService.comparison(entry));
        }
    }
}
