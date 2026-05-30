package com.ecoamazonas.eco_agua.dashboard;

import com.ecoamazonas.eco_agua.user.Role;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.*;

@Controller
@RequestMapping("/admin/dashboard-widgets")
public class DashboardWidgetAdminController {

    private final DashboardWidgetAccessService dashboardWidgetAccessService;

    public DashboardWidgetAdminController(DashboardWidgetAccessService dashboardWidgetAccessService) {
        this.dashboardWidgetAccessService = dashboardWidgetAccessService;
    }

    @GetMapping
    public String index(Model model) {
        model.addAttribute("activePage", "dashboard_widgets");
        model.addAttribute("roles", dashboardWidgetAccessService.getConfigurableRoles());
        model.addAttribute("widgetGroups", dashboardWidgetAccessService.getWidgetGroups());
        model.addAttribute("widgetDefinitionCount", dashboardWidgetAccessService.getWidgetDefinitions().size());
        model.addAttribute("roleWidgetMatrix", dashboardWidgetAccessService.getRoleWidgetMatrix());
        return "admin/dashboard_widgets";
    }

    @PostMapping
    public String save(
            @RequestParam Map<String, String> requestParams,
            RedirectAttributes redirectAttributes
    ) {
        Map<String, Set<String>> enabledWidgetsByRole = new LinkedHashMap<>();
        List<Role> roles = dashboardWidgetAccessService.getConfigurableRoles();

        for (Role role : roles) {
            enabledWidgetsByRole.put(role.getCode(), new LinkedHashSet<>());
        }

        for (String paramName : requestParams.keySet()) {
            if (!paramName.startsWith("widget_")) {
                continue;
            }

            String[] parts = paramName.split("__", 2);
            if (parts.length != 2) {
                continue;
            }

            String roleCode = parts[0].substring("widget_".length());
            String widgetKey = parts[1];
            enabledWidgetsByRole.computeIfAbsent(roleCode, ignored -> new LinkedHashSet<>()).add(widgetKey);
        }

        dashboardWidgetAccessService.updateRoleWidgetSettings(enabledWidgetsByRole);
        redirectAttributes.addFlashAttribute("successMessage", "Dashboard widget visibility was updated successfully.");
        return "redirect:/admin/dashboard-widgets";
    }
}
