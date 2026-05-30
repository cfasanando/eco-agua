package com.ecoamazonas.eco_agua.reorder;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.util.List;

@Controller
@RequestMapping("/reorder-agenda")
public class ReorderAgendaController {

    private final ReorderAgendaService reorderAgendaService;

    public ReorderAgendaController(ReorderAgendaService reorderAgendaService) {
        this.reorderAgendaService = reorderAgendaService;
    }

    @GetMapping
    public String index(
            @RequestParam(value = "referenceDate", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate referenceDate,
            @RequestParam(value = "followUpStatus", required = false) ReorderFollowUpStatus followUpStatus,
            @RequestParam(value = "minProbability", required = false) Integer minProbability,
            @RequestParam(value = "onlyOverdue", required = false, defaultValue = "false") boolean onlyOverdue,
            Model model
    ) {
        LocalDate effectiveDate = referenceDate != null ? referenceDate : LocalDate.now();
        List<ReorderAgendaRow> rows = reorderAgendaService.buildAgenda(
                effectiveDate,
                followUpStatus,
                minProbability,
                onlyOverdue
        );

        model.addAttribute("activePage", "reorder_agenda");
        model.addAttribute("referenceDate", effectiveDate);
        model.addAttribute("rows", rows);
        model.addAttribute("followUpStatuses", ReorderFollowUpStatus.values());
        model.addAttribute("selectedFollowUpStatus", followUpStatus);
        model.addAttribute("selectedMinProbability", minProbability);
        model.addAttribute("onlyOverdue", onlyOverdue);
        model.addAttribute("forTodayCount", rows.stream().filter(r -> r.getOverdueDays() > 0 || r.getProbabilityPercent() >= 70).count());
        model.addAttribute("contactedCount", rows.stream().filter(r -> r.getFollowUpStatus() == ReorderFollowUpStatus.CONTACTED).count());
        model.addAttribute("createdOrderCount", rows.stream().filter(r -> r.getFollowUpStatus() == ReorderFollowUpStatus.ORDER_CREATED).count());

        return "reorder/agenda";
    }

    @PostMapping("/{clientId}/follow-up")
    public String saveFollowUp(
            @PathVariable Long clientId,
            @RequestParam("referenceDate")
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate referenceDate,
            @RequestParam("status") ReorderFollowUpStatus status,
            @RequestParam(value = "nextContactDate", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate nextContactDate,
            @RequestParam(value = "observation", required = false) String observation,
            RedirectAttributes redirectAttributes
    ) {
        try {
            reorderAgendaService.saveFollowUp(clientId, referenceDate, status, nextContactDate, observation);
            redirectAttributes.addFlashAttribute("successMessage", "Follow-up saved successfully.");
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }

        return "redirect:/reorder-agenda?referenceDate=" + referenceDate;
    }
}
