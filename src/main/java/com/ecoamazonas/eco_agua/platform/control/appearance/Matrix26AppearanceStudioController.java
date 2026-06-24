package com.ecoamazonas.eco_agua.platform.control.appearance;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/control-center")
@ConditionalOnProperty(name = "matrix26.control-center.enabled", havingValue = "true")
public class Matrix26AppearanceStudioController {

    private final Matrix26AppearanceCatalogService appearanceService;
    private final Matrix26AppearanceEditorService editorService;
    private final Matrix26AppearancePublicationService publicationService;

    public Matrix26AppearanceStudioController(
            Matrix26AppearanceCatalogService appearanceService,
            Matrix26AppearanceEditorService editorService,
            Matrix26AppearancePublicationService publicationService
    ) {
        this.appearanceService = appearanceService;
        this.editorService = editorService;
        this.publicationService = publicationService;
    }

    @GetMapping("/appearance")
    public String appearanceStudio(Model model) {
        model.addAttribute("activePage", "matrix26_appearance");
        model.addAttribute("overview", appearanceService.overview());
        model.addAttribute("themes", appearanceService.themeUsage());
        model.addAttribute("layoutsByArea", appearanceService.layoutsByArea());
        model.addAttribute("recentAppearances", appearanceService.recentAppearances());
        return "control_center/appearance/index";
    }

    @GetMapping("/themes")
    public String themes(Model model) {
        model.addAttribute("activePage", "matrix26_themes");
        model.addAttribute("themes", appearanceService.themeUsage());
        return "control_center/appearance/themes";
    }

    @GetMapping("/themes/{code}")
    public String themeDetail(@PathVariable String code, Model model) {
        Matrix26ThemeCatalog theme = appearanceService.getTheme(code);
        model.addAttribute("activePage", "matrix26_themes");
        model.addAttribute("theme", theme);
        model.addAttribute("compatibleLayouts", appearanceService.activeLayouts().stream()
                .filter(layout -> layout.getCompatibleThemes() == null
                        || layout.getCompatibleThemes().isBlank()
                        || layout.getCompatibleThemes().contains(theme.getCode()))
                .toList());
        return "control_center/appearance/theme_detail";
    }

    @GetMapping("/layouts")
    public String layouts(Model model) {
        model.addAttribute("activePage", "matrix26_layouts");
        model.addAttribute("layoutsByArea", appearanceService.layoutsByArea());
        model.addAttribute("layoutUsage", appearanceService.layoutUsage());
        return "control_center/appearance/layouts";
    }

    @GetMapping("/appearance/instances")
    public String instanceAppearances(Model model) {
        model.addAttribute("activePage", "matrix26_instance_appearance");
        model.addAttribute("appearances", appearanceService.instanceAppearances());
        return "control_center/appearance/instances";
    }

    @GetMapping("/instances/{id}/appearance")
    public String instanceAppearance(@PathVariable Long id, Model model) {
        Matrix26InstanceAppearanceView view = appearanceService.instanceAppearance(id);
        model.addAttribute("activePage", "matrix26_instance_appearance");
        model.addAttribute("view", view);
        model.addAttribute("history", appearanceService.history(id));
        model.addAttribute("draft", editorService.draft(id));
        model.addAttribute("publicationState", publicationService.state(id));
        return "control_center/appearance/instance_detail";
    }
}
