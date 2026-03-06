package com.ecoamazonas.eco_agua.config;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.Optional;

@Controller
@RequestMapping("/admin/platform-settings")
public class PlatformSettingAdminController {

    private final PlatformSettingRepository platformSettingRepository;
    private final PlatformSettingService platformSettingService;

    public PlatformSettingAdminController(PlatformSettingRepository platformSettingRepository,
                                          PlatformSettingService platformSettingService) {
        this.platformSettingRepository = platformSettingRepository;
        this.platformSettingService = platformSettingService;
    }

    @GetMapping
    public String showSettings(Model model) {
        // Ensure all defaults used in public home exist
        platformSettingService.ensureDefaultsForPublicSite();

        // Now list all platform + public_site settings (they will include hero, final CTA, etc.)
        List<PlatformSetting> settings = platformSettingRepository
                .findByCategoryInOrderByCategoryAscVariableAsc(List.of("platform", "public_site"));

        model.addAttribute("settings", settings);
        return "admin/platform_settings";
    }

    @PostMapping
    public String updateSettings(
            @RequestParam("id") List<Long> ids,
            @RequestParam("value") List<String> values,
            RedirectAttributes redirectAttributes
    ) {
        for (int i = 0; i < ids.size(); i++) {
            Long id = ids.get(i);
            String newValue = values.get(i);

            Optional<PlatformSetting> opt = platformSettingRepository.findById(id);
            if (opt.isEmpty()) {
                continue;
            }

            PlatformSetting setting = opt.get();
            setting.setValue(newValue != null ? newValue.trim() : "");
            platformSettingRepository.save(setting);
        }

        redirectAttributes.addFlashAttribute("successMessage", "Settings updated successfully.");

        return "redirect:/admin/platform-settings";
    }
}
