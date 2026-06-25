package com.ecoamazonas.eco_agua.platform.control.operations;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;

@Controller
@RequestMapping("/control-center/operations")
@ConditionalOnProperty(name = "matrix26.control-center.enabled", havingValue = "true")
public class Matrix26OperationsController {

    private final Matrix26OperationsInventoryService inventoryService;
    private final Matrix26RuntimeControlService runtimeControlService;

    public Matrix26OperationsController(
            Matrix26OperationsInventoryService inventoryService,
            Matrix26RuntimeControlService runtimeControlService
    ) {
        this.inventoryService = inventoryService;
        this.runtimeControlService = runtimeControlService;
    }

    @GetMapping
    public String dashboard(
            @RequestParam(value = "refresh", defaultValue = "false") boolean refresh,
            Model model
    ) {
        Matrix26OperationsSnapshot snapshot = inventoryService.snapshot(refresh);
        addSnapshotModel(model, snapshot, "matrix26_operations");
        model.addAttribute("recentRuntimeOperations", runtimeControlService.recentOperations());
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
        model.addAttribute("runtimeControl", runtimeControlService.view(runtime));
        model.addAttribute("runtimeStability", runtimeControlService.stability(runtime));
        model.addAttribute("runtimeOperations", runtimeControlService.operationsForInstance(runtime.target().instanceId()));
        model.addAttribute("logTail", inventoryService.logTail(runtimeKey, false));
        model.addAttribute("readOnlyOperations", false);
        return "control_center/operations/runtime_detail";
    }

    @PostMapping("/runtimes/{runtimeKey}/start")
    public String startRuntime(
            @PathVariable String runtimeKey,
            Principal principal,
            RedirectAttributes redirectAttributes
    ) {
        return executeRuntimeAction(
                runtimeKey,
                redirectAttributes,
                () -> runtimeControlService.start(runtimeKey, actor(principal))
        );
    }

    @PostMapping("/runtimes/{runtimeKey}/stop")
    public String stopRuntime(
            @PathVariable String runtimeKey,
            @RequestParam("confirmation") String confirmation,
            Principal principal,
            RedirectAttributes redirectAttributes
    ) {
        return executeRuntimeAction(
                runtimeKey,
                redirectAttributes,
                () -> runtimeControlService.stop(runtimeKey, actor(principal), confirmation)
        );
    }

    @PostMapping("/runtimes/{runtimeKey}/restart")
    public String restartRuntime(
            @PathVariable String runtimeKey,
            @RequestParam("confirmation") String confirmation,
            Principal principal,
            RedirectAttributes redirectAttributes
    ) {
        return executeRuntimeAction(
                runtimeKey,
                redirectAttributes,
                () -> runtimeControlService.restart(runtimeKey, actor(principal), confirmation)
        );
    }


    @PostMapping("/runtimes/{runtimeKey}/force-stop")
    public String forceStopRuntime(
            @PathVariable String runtimeKey,
            @RequestParam("confirmation") String confirmation,
            Principal principal,
            RedirectAttributes redirectAttributes
    ) {
        return executeRuntimeAction(
                runtimeKey,
                redirectAttributes,
                () -> runtimeControlService.forceStop(runtimeKey, actor(principal), confirmation)
        );
    }

    @PostMapping("/runtimes/{runtimeKey}/adopt")
    public String adoptRuntime(
            @PathVariable String runtimeKey,
            @RequestParam("confirmation") String confirmation,
            Principal principal,
            RedirectAttributes redirectAttributes
    ) {
        return executeRuntimeAction(
                runtimeKey,
                redirectAttributes,
                () -> runtimeControlService.adopt(runtimeKey, actor(principal), confirmation)
        );
    }

    @PostMapping("/runtimes/{runtimeKey}/clean-stale-pid")
    public String cleanStalePid(
            @PathVariable String runtimeKey,
            @RequestParam("confirmation") String confirmation,
            Principal principal,
            RedirectAttributes redirectAttributes
    ) {
        return executeRuntimeAction(
                runtimeKey,
                redirectAttributes,
                () -> runtimeControlService.cleanStalePid(runtimeKey, actor(principal), confirmation)
        );
    }

    @PostMapping("/runtimes/{runtimeKey}/rotate-logs")
    public String rotateLogs(
            @PathVariable String runtimeKey,
            @RequestParam("confirmation") String confirmation,
            Principal principal,
            RedirectAttributes redirectAttributes
    ) {
        return executeRuntimeAction(
                runtimeKey,
                redirectAttributes,
                () -> runtimeControlService.rotateLogs(runtimeKey, actor(principal), confirmation)
        );
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
        model.addAttribute("runtimeControls", runtimeControlService.views(snapshot.runtimes()));
        model.addAttribute("runtimeStabilities", runtimeControlService.stabilities(snapshot.runtimes()));
        model.addAttribute("ports", snapshot.ports());
        model.addAttribute("logs", snapshot.logs());
        model.addAttribute("summary", snapshot.summary());
        model.addAttribute("probeWarnings", snapshot.probeWarnings());
        model.addAttribute("readOnlyOperations", false);
    }

    private String executeRuntimeAction(
            String runtimeKey,
            RedirectAttributes redirectAttributes,
            RuntimeAction action
    ) {
        try {
            Matrix26RuntimeControlResult result = action.execute();
            redirectAttributes.addFlashAttribute("runtimeOperationSuccess", result.message());
        } catch (Matrix26RuntimeControlException ex) {
            redirectAttributes.addFlashAttribute("runtimeOperationError", ex.getMessage());
        }
        return "redirect:/control-center/operations/runtimes/" + runtimeKey + "?refresh=true";
    }

    private String actor(Principal principal) {
        return principal == null || principal.getName() == null || principal.getName().isBlank()
                ? "matrix26-system"
                : principal.getName();
    }

    @FunctionalInterface
    private interface RuntimeAction {
        Matrix26RuntimeControlResult execute();
    }
}
