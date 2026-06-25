package com.ecoamazonas.eco_agua.platform.control.operations;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequestMapping("/control-center/operations")
@ConditionalOnProperty(name = "matrix26.control-center.enabled", havingValue = "true")
public class Matrix26OperationsController {

    private final Matrix26OperationsInventoryService inventoryService;

    public Matrix26OperationsController(Matrix26OperationsInventoryService inventoryService) {
        this.inventoryService = inventoryService;
    }

    @GetMapping
    public String dashboard(
            @RequestParam(value = "refresh", defaultValue = "false") boolean refresh,
            Model model
    ) {
        Matrix26OperationsSnapshot snapshot = inventoryService.snapshot(refresh);
        addSnapshotModel(model, snapshot, "matrix26_operations");
        return "control_center/operations/dashboard";
    }

    @GetMapping("/runtimes")
    public String runtimes(
            @RequestParam(value = "refresh", defaultValue = "false") boolean refresh,
            Model model
    ) {
        Matrix26OperationsSnapshot snapshot = inventoryService.snapshot(refresh);
        addSnapshotModel(model, snapshot, "matrix26_operations_runtimes");
        return "control_center/operations/runtimes";
    }

    @GetMapping("/runtimes/{runtimeKey}")
    public String runtimeDetail(
            @PathVariable String runtimeKey,
            @RequestParam(value = "refresh", defaultValue = "false") boolean refresh,
            Model model
    ) {
        Matrix26RuntimeInventoryItem runtime = inventoryService.runtime(runtimeKey, refresh);
        model.addAttribute("activePage", "matrix26_operations_runtimes");
        model.addAttribute("runtime", runtime);
        model.addAttribute("logTail", inventoryService.logTail(runtimeKey, false));
        model.addAttribute("readOnlyOperations", true);
        return "control_center/operations/runtime_detail";
    }

    @GetMapping("/ports")
    public String ports(
            @RequestParam(value = "refresh", defaultValue = "false") boolean refresh,
            Model model
    ) {
        Matrix26OperationsSnapshot snapshot = inventoryService.snapshot(refresh);
        addSnapshotModel(model, snapshot, "matrix26_operations_ports");
        return "control_center/operations/ports";
    }

    @GetMapping("/logs")
    public String logs(
            @RequestParam(value = "refresh", defaultValue = "false") boolean refresh,
            Model model
    ) {
        Matrix26OperationsSnapshot snapshot = inventoryService.snapshot(refresh);
        addSnapshotModel(model, snapshot, "matrix26_operations_logs");
        return "control_center/operations/logs";
    }

    private void addSnapshotModel(
            Model model,
            Matrix26OperationsSnapshot snapshot,
            String activePage
    ) {
        model.addAttribute("activePage", activePage);
        model.addAttribute("operations", snapshot);
        model.addAttribute("runtimes", snapshot.runtimes());
        model.addAttribute("ports", snapshot.ports());
        model.addAttribute("logs", snapshot.logs());
        model.addAttribute("summary", snapshot.summary());
        model.addAttribute("probeWarnings", snapshot.probeWarnings());
        model.addAttribute("readOnlyOperations", true);
    }
}
