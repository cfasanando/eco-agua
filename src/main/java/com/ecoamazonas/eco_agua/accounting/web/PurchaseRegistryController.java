package com.ecoamazonas.eco_agua.accounting.web;

import com.ecoamazonas.eco_agua.accounting.dto.PurchaseRegistryRow;
import com.ecoamazonas.eco_agua.accounting.service.PurchaseRegistryService;
import com.ecoamazonas.eco_agua.expense.ExpenseStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Controller
@RequestMapping("/accounting")
public class PurchaseRegistryController {

    private final PurchaseRegistryService purchaseRegistryService;

    public PurchaseRegistryController(PurchaseRegistryService purchaseRegistryService) {
        this.purchaseRegistryService = purchaseRegistryService;
    }

    @GetMapping("/purchase-registry")
    public String showPurchaseRegistry(
            @RequestParam(name = "year", required = false) Integer year,
            @RequestParam(name = "month", required = false) Integer month,
            @RequestParam(name = "docType", required = false, defaultValue = "ALL") String docType,
            @RequestParam(name = "status", required = false, defaultValue = "ALL") String status,
            Model model
    ) {
        LocalDate now = LocalDate.now();
        int selectedYear = (year != null) ? year : now.getYear();
        int selectedMonth = (month != null) ? month : now.getMonthValue();

        List<Integer> years = IntStream.rangeClosed(selectedYear - 2, selectedYear)
                .boxed()
                .collect(Collectors.toList());

        List<Integer> months = IntStream.rangeClosed(1, 12)
                .boxed()
                .collect(Collectors.toList());

        Map<Integer, String> monthNames = buildMonthNames();

        List<String> docTypes = Arrays.asList("ALL", "01", "03", "07", "08");

        List<String> statusOptions = new ArrayList<>();
        statusOptions.add("ALL");
        statusOptions.addAll(Arrays.stream(ExpenseStatus.values())
                .map(Enum::name)
                .collect(Collectors.toList()));

        List<PurchaseRegistryRow> rows = purchaseRegistryService.getMonthlyRows(
                selectedYear,
                selectedMonth,
                docType,
                status
        );

        BigDecimal totalBase = rows.stream()
                .map(PurchaseRegistryRow::getTaxBase)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalIgv = rows.stream()
                .map(PurchaseRegistryRow::getTaxIgv)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalAmount = rows.stream()
                .map(PurchaseRegistryRow::getTotal)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        model.addAttribute("rows", rows);
        model.addAttribute("totalBase", totalBase);
        model.addAttribute("totalIgv", totalIgv);
        model.addAttribute("totalAmount", totalAmount);

        model.addAttribute("years", years);
        model.addAttribute("months", months);
        model.addAttribute("monthNames", monthNames);

        model.addAttribute("docTypes", docTypes);
        model.addAttribute("statusOptions", statusOptions);

        model.addAttribute("selectedYear", selectedYear);
        model.addAttribute("selectedMonth", selectedMonth);
        model.addAttribute("selectedDocType", docType);
        model.addAttribute("selectedStatus", status);

        model.addAttribute("activePage", "accounting_purchase_registry");

        return "accounting/purchase_registry";
    }

    private Map<Integer, String> buildMonthNames() {
        Map<Integer, String> map = new LinkedHashMap<>();
        map.put(1, "Enero");
        map.put(2, "Febrero");
        map.put(3, "Marzo");
        map.put(4, "Abril");
        map.put(5, "Mayo");
        map.put(6, "Junio");
        map.put(7, "Julio");
        map.put(8, "Agosto");
        map.put(9, "Septiembre");
        map.put(10, "Octubre");
        map.put(11, "Noviembre");
        map.put(12, "Diciembre");
        return map;
    }
}
