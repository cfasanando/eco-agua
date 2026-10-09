package com.ecoamazonas.eco_agua.platform.control.demo;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.LinkedHashMap;
import java.util.Map;

@Controller
@RequestMapping("/control-center")
public class Matrix26LocalControlCenterFallbackController {

    private static final Map<String, String> ROUTE_LABELS = new LinkedHashMap<>();

    static {
        ROUTE_LABELS.put("/control-center", "Dashboard Matrix26");
        ROUTE_LABELS.put("/control-center/dashboard", "Dashboard Matrix26");
        ROUTE_LABELS.put("/control-center/instances", "Instancias");
        ROUTE_LABELS.put("/control-center/provisioning", "Aprovisionamiento");
        ROUTE_LABELS.put("/control-center/modules", "Módulos");
        ROUTE_LABELS.put("/control-center/modules/activation", "Activación de módulos");
        ROUTE_LABELS.put("/control-center/modules/acceptance", "Feature Flags QA");
        ROUTE_LABELS.put("/control-center/operations/dashboard", "Operations dashboard");
        ROUTE_LABELS.put("/control-center/operations/alerts", "Alert Center");
        ROUTE_LABELS.put("/control-center/operations/acceptance", "Acceptance Matrix");
        ROUTE_LABELS.put("/control-center/operations/runtimes", "Runtimes");
        ROUTE_LABELS.put("/control-center/operations/ports", "Ports");
        ROUTE_LABELS.put("/control-center/operations/logs", "Logs");
        ROUTE_LABELS.put("/control-center/backups", "Backups");
        ROUTE_LABELS.put("/control-center/backups/schedules", "Backup schedules");
        ROUTE_LABELS.put("/control-center/backups/calendar", "Backup calendar");
        ROUTE_LABELS.put("/control-center/backups/executions", "Backup executions");
        ROUTE_LABELS.put("/control-center/backups/alerts", "Backup alerts");
        ROUTE_LABELS.put("/control-center/backups/policies", "Backup policies");
        ROUTE_LABELS.put("/control-center/backups/retention", "Backup retention");
        ROUTE_LABELS.put("/control-center/restores", "Restore Manager");
        ROUTE_LABELS.put("/control-center/lifecycle", "Lifecycle Manager");
        ROUTE_LABELS.put("/control-center/lifecycle/decommission", "Decommission");
        ROUTE_LABELS.put("/control-center/lifecycle/decommission/decommissioned", "Decommissioned");
        ROUTE_LABELS.put("/control-center/lifecycle/archive", "Final archives");
        ROUTE_LABELS.put("/control-center/lifecycle/archive/restores", "Archive restores");
        ROUTE_LABELS.put("/control-center/purge", "Purge dry run");
        ROUTE_LABELS.put("/control-center/purge/archive-destruction", "Archive destruction");
        ROUTE_LABELS.put("/control-center/appearance", "Appearance Studio");
        ROUTE_LABELS.put("/control-center/themes", "Themes");
        ROUTE_LABELS.put("/control-center/layouts", "Layouts");
        ROUTE_LABELS.put("/control-center/appearance/instances", "Appearance por instancia");
        ROUTE_LABELS.put("/control-center/appearance/quality-lab", "Quality Lab");
        ROUTE_LABELS.put("/control-center/audit", "Auditoría");
        ROUTE_LABELS.put("/control-center/security", "Security");
        ROUTE_LABELS.put("/control-center/settings", "Configuración");
    }

    @GetMapping({"", "/", "/dashboard"})
    public String dashboardFallback() {
        return "redirect:/control-center/demo-center";
    }

    @GetMapping("/**")
    public String fallback(HttpServletRequest request, Model model) {
        String path = request.getRequestURI();
        model.addAttribute("activePage", activePage(path));
        model.addAttribute("requestedPath", path);
        model.addAttribute("routeLabel", label(path));
        model.addAttribute("matrix26RuntimeUrl", "http://localhost:8091" + path);
        model.addAttribute("demoCenterUrl", "/control-center/demo-center");
        return "control_center/local_module_unavailable";
    }

    private String label(String path) {
        String label = ROUTE_LABELS.get(path);
        if (label != null) {
            return label;
        }
        return "Módulo Matrix26 completo";
    }

    private String activePage(String path) {
        if (path == null) {
            return "matrix26_demo_center";
        }
        if (path.contains("/appearance") || path.contains("/themes") || path.contains("/layouts")) {
            return "matrix26_appearance";
        }
        if (path.contains("/operations")) {
            return "matrix26_operations_dashboard";
        }
        if (path.contains("/backups")) {
            return "matrix26_backups";
        }
        if (path.contains("/restores")) {
            return "matrix26_restores";
        }
        if (path.contains("/lifecycle")) {
            return "matrix26_lifecycle";
        }
        if (path.contains("/purge")) {
            return "matrix26_purge";
        }
        if (path.contains("/modules")) {
            return "matrix26_modules";
        }
        if (path.contains("/instances")) {
            return "matrix26_instances";
        }
        if (path.contains("/provisioning")) {
            return "matrix26_provisioning";
        }
        return "matrix26_demo_center";
    }
}
