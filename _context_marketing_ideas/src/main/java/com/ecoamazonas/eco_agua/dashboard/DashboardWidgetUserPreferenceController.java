package com.ecoamazonas.eco_agua.dashboard;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/dashboard/widget-preferences")
public class DashboardWidgetUserPreferenceController {

    private final DashboardWidgetUserPreferenceService preferenceService;

    public DashboardWidgetUserPreferenceController(DashboardWidgetUserPreferenceService preferenceService) {
        this.preferenceService = preferenceService;
    }

    @GetMapping
    public DashboardWidgetUserPreferenceService.DashboardWidgetPreferenceResponse getPreferences(Authentication authentication) {
        return preferenceService.getPreferences(authentication);
    }

    @PostMapping
    public Map<String, Object> savePreferences(
            @RequestBody DashboardWidgetUserPreferenceService.DashboardWidgetPreferenceRequest request,
            Authentication authentication
    ) {
        preferenceService.savePreferences(authentication, request);
        return Map.of("saved", true);
    }

    @PostMapping("/reset")
    public Map<String, Object> resetPreferences(Authentication authentication) {
        preferenceService.resetPreferences(authentication);
        return Map.of("reset", true);
    }
}
