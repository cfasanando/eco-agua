package com.ecoamazonas.eco_agua.accounting.web;

import com.ecoamazonas.eco_agua.accounting.dto.SalesRegistryRow;
import com.ecoamazonas.eco_agua.accounting.service.SalesRegistryService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDate;
import java.time.Month;
import java.time.format.TextStyle;
import java.util.*;
import java.util.stream.IntStream;

@Controller
@RequestMapping("/accounting")
public class SalesRegistryController {

    private final SalesRegistryService salesRegistryService;

    public SalesRegistryController(SalesRegistryService salesRegistryService) {
        this.salesRegistryService = salesRegistryService;
    }

    @GetMapping("/sales-registry")
    public String salesRegistry(
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer month,
            @RequestParam(required = false) String docType,
            @RequestParam(required = false) String status,
            Model model
    ) {
        LocalDate today = LocalDate.now();
        int currentYear = today.getYear();
        int currentMonth = today.getMonthValue();

        int y = (year != null) ? year : currentYear;
        Integer m = month;

        // When no filter is provided at all, default to current month
        boolean noFiltersProvided =
                (year == null && month == null && docType == null && status == null);

        if (noFiltersProvided) {
            m = currentMonth;
        }

        List<SalesRegistryRow> rows =
                salesRegistryService.getRegistryForMonth(y, m, docType, status);

        model.addAttribute("rows", rows);
        model.addAttribute("selectedYear", y);
        model.addAttribute("selectedMonth", m == null ? 0 : m);
        model.addAttribute("selectedDocType", docType == null ? "ALL" : docType);
        model.addAttribute("selectedStatus", status == null ? "ALL" : status);

        // Years combo (example: currentYear - 2 ... currentYear + 2)
        int startYear = currentYear - 2;
        int endYear = currentYear + 2;
        List<Integer> years = IntStream.rangeClosed(startYear, endYear).boxed().toList();
        model.addAttribute("years", years);

        // Months 1..12
        List<Integer> months = IntStream.rangeClosed(1, 12).boxed().toList();
        model.addAttribute("months", months);

        // Month names map (1 -> Enero, 2 -> Febrero, ...)
        Map<Integer, String> monthNames = new LinkedHashMap<>();
        Locale locale = new Locale("es", "PE");
        for (Integer mm : months) {
            String label = Month.of(mm).getDisplayName(TextStyle.FULL, locale);
            if (!label.isEmpty()) {
                label = label.substring(0, 1).toUpperCase(locale) + label.substring(1);
            }
            monthNames.put(mm, label);
        }
        model.addAttribute("monthNames", monthNames);

        // Doc types for filter (adjust values according to your own mapping)
        model.addAttribute("docTypes", Arrays.asList("ALL", "FA", "BO", "TK", "SC"));

        // Status options
        model.addAttribute("statusOptions", Arrays.asList("ALL", "VALIDO", "ANULADO", "BORRADOR"));

        // Totals
        model.addAttribute("totalBase", salesRegistryService.getTotalBase(rows));
        model.addAttribute("totalIgv", salesRegistryService.getTotalIgv(rows));
        model.addAttribute("totalAmount", salesRegistryService.getTotalAmount(rows));

        // Sidebar highlight
        model.addAttribute("activePage", "accounting_sales_registry");

        return "accounting/sales_registry";
    }
}
