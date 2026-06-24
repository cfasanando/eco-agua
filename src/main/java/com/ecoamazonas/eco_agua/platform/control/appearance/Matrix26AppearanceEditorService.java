package com.ecoamazonas.eco_agua.platform.control.appearance;

import com.ecoamazonas.eco_agua.platform.PlatformBusinessClient;
import com.ecoamazonas.eco_agua.platform.PlatformBusinessClientRepository;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@ConditionalOnProperty(name = "matrix26.control-center.enabled", havingValue = "true")
public class Matrix26AppearanceEditorService {

    private static final String ACTIVE = "ACTIVE";
    private static final Set<String> SIDEBAR_MODES = Set.of("THEME", "LIGHT", "DARK");
    private static final Set<String> BORDER_RADII = Set.of("SMALL", "MEDIUM", "LARGE");
    private static final Set<String> TABLE_DENSITIES = Set.of("COMPACT", "COMFORTABLE", "SPACIOUS");
    private static final Set<String> CONTENT_WIDTHS = Set.of("STANDARD", "WIDE", "FULL");
    private static final Set<String> HEADING_STYLES = Set.of("SYSTEM", "STRONG", "EDITORIAL");

    private final PlatformBusinessClientRepository clientRepository;
    private final Matrix26ThemeCatalogRepository themeRepository;
    private final Matrix26LayoutCatalogRepository layoutRepository;
    private final Matrix26InstanceAppearanceRepository appearanceRepository;
    private final Matrix26InstanceAppearanceDraftRepository draftRepository;
    private final Matrix26InstanceAppearanceHistoryRepository historyRepository;

    public Matrix26AppearanceEditorService(
            PlatformBusinessClientRepository clientRepository,
            Matrix26ThemeCatalogRepository themeRepository,
            Matrix26LayoutCatalogRepository layoutRepository,
            Matrix26InstanceAppearanceRepository appearanceRepository,
            Matrix26InstanceAppearanceDraftRepository draftRepository,
            Matrix26InstanceAppearanceHistoryRepository historyRepository
    ) {
        this.clientRepository = clientRepository;
        this.themeRepository = themeRepository;
        this.layoutRepository = layoutRepository;
        this.appearanceRepository = appearanceRepository;
        this.draftRepository = draftRepository;
        this.historyRepository = historyRepository;
    }

    @Transactional(readOnly = true)
    public Matrix26AppearanceEditorView editorView(Long instanceId) {
        PlatformBusinessClient instance = requireInstance(instanceId);
        Matrix26InstanceAppearance published = requirePublished(instanceId);
        Matrix26InstanceAppearanceDraft draft = draftRepository.findByInstance_Id(instanceId).orElse(null);
        List<Matrix26ThemeCatalog> themes = themeRepository.findByStatusOrderByDisplayOrderAscNameAsc(ACTIVE);
        List<Matrix26LayoutCatalog> layouts = layoutRepository.findByStatusOrderByAreaAscDisplayOrderAscNameAsc(ACTIVE);

        Map<String, String> themeNames = themes.stream().collect(Collectors.toMap(
                Matrix26ThemeCatalog::getCode,
                Matrix26ThemeCatalog::getName,
                (left, right) -> left,
                LinkedHashMap::new
        ));
        Map<String, String> layoutNames = layouts.stream().collect(Collectors.toMap(
                Matrix26LayoutCatalog::getCode,
                Matrix26LayoutCatalog::getName,
                (left, right) -> left,
                LinkedHashMap::new
        ));
        Map<String, Map<String, String>> themePresets = new LinkedHashMap<>();
        for (Matrix26ThemeCatalog theme : themes) {
            themePresets.put(theme.getCode(), themeDefaults(theme.getCode()));
        }

        return new Matrix26AppearanceEditorView(
                instance,
                published,
                draft,
                themes.stream().filter(Matrix26ThemeCatalog::isSupportsPublic).toList(),
                themes.stream().filter(Matrix26ThemeCatalog::isSupportsAdmin).toList(),
                layouts.stream().filter(item -> "PUBLIC".equals(item.getArea())).toList(),
                layouts.stream().filter(item -> "ADMIN".equals(item.getArea())).toList(),
                layouts.stream().filter(item -> "LOGIN".equals(item.getArea())).toList(),
                themeNames,
                layoutNames,
                themePresets
        );
    }

    @Transactional(readOnly = true)
    public Matrix26AppearanceEditorForm currentForm(Long instanceId) {
        Matrix26InstanceAppearanceDraft draft = draftRepository.findByInstance_Id(instanceId).orElse(null);
        if (draft != null) {
            return fromValues(
                    draft.getPublicThemeCode(),
                    draft.getPublicLayoutCode(),
                    draft.getAdminThemeCode(),
                    draft.getAdminLayoutCode(),
                    draft.getLoginLayoutCode(),
                    draft.getOverridesJson(),
                    draft.getReason()
            );
        }

        Matrix26InstanceAppearance published = requirePublished(instanceId);
        return fromValues(
                published.getPublicThemeCode(),
                published.getPublicLayoutCode(),
                published.getAdminThemeCode(),
                published.getAdminLayoutCode(),
                published.getLoginLayoutCode(),
                published.getOverridesJson(),
                null
        );
    }

    @Transactional(readOnly = true)
    public Matrix26InstanceAppearanceDraft draft(Long instanceId) {
        return draftRepository.findByInstance_Id(instanceId).orElse(null);
    }

    @Transactional
    public Matrix26InstanceAppearanceDraft saveDraft(
            Long instanceId,
            Matrix26AppearanceEditorForm form,
            String actor
    ) {
        validate(form);
        if (form.getReason() == null || form.getReason().isBlank()) {
            throw new IllegalArgumentException("Indica el motivo del cambio antes de guardar el borrador.");
        }

        PlatformBusinessClient instance = requireInstance(instanceId);
        Matrix26InstanceAppearanceDraft draft = draftRepository.findByInstance_Id(instanceId)
                .orElseGet(Matrix26InstanceAppearanceDraft::new);
        int nextVersion = nextHistoryVersion(instanceId);

        draft.setInstance(instance);
        draft.setPublicThemeCode(form.getPublicThemeCode());
        draft.setPublicLayoutCode(form.getPublicLayoutCode());
        draft.setAdminThemeCode(form.getAdminThemeCode());
        draft.setAdminLayoutCode(form.getAdminLayoutCode());
        draft.setLoginLayoutCode(form.getLoginLayoutCode());
        draft.setOverridesJson(writeJson(overrides(form)));
        draft.setStatus("DRAFT");
        draft.setDraftVersion(nextVersion);
        draft.setUpdatedBy(safeActor(actor));
        draft.setReason(form.getReason().trim());
        Matrix26InstanceAppearanceDraft saved = draftRepository.save(draft);

        Matrix26InstanceAppearanceHistory history = new Matrix26InstanceAppearanceHistory();
        history.setInstance(instance);
        history.setVersion(nextVersion);
        history.setStatus("DRAFT");
        history.setSnapshotJson(snapshotJson(form, "DRAFT", nextVersion));
        history.setActorUsername(safeActor(actor));
        history.setReason(form.getReason().trim());
        historyRepository.save(history);

        return saved;
    }

    @Transactional
    public void discardDraft(Long instanceId, String actor) {
        Matrix26InstanceAppearanceDraft draft = draftRepository.findByInstance_Id(instanceId)
                .orElseThrow(() -> new IllegalArgumentException("La instancia no tiene un borrador para descartar."));
        int nextVersion = nextHistoryVersion(instanceId);

        Matrix26AppearanceEditorForm form = fromValues(
                draft.getPublicThemeCode(),
                draft.getPublicLayoutCode(),
                draft.getAdminThemeCode(),
                draft.getAdminLayoutCode(),
                draft.getLoginLayoutCode(),
                draft.getOverridesJson(),
                draft.getReason()
        );

        Matrix26InstanceAppearanceHistory history = new Matrix26InstanceAppearanceHistory();
        history.setInstance(draft.getInstance());
        history.setVersion(nextVersion);
        history.setStatus("SUPERSEDED");
        history.setSnapshotJson(snapshotJson(form, "SUPERSEDED", nextVersion));
        history.setActorUsername(safeActor(actor));
        history.setReason("Draft discarded before publication");
        historyRepository.save(history);
        draftRepository.delete(draft);
    }

    @Transactional(readOnly = true)
    public void validate(Matrix26AppearanceEditorForm form) {
        Matrix26ThemeCatalog publicTheme = requireTheme(form.getPublicThemeCode());
        Matrix26ThemeCatalog adminTheme = requireTheme(form.getAdminThemeCode());
        if (!publicTheme.isSupportsPublic()) {
            throw new IllegalArgumentException("El theme público seleccionado no admite frontend público.");
        }
        if (!adminTheme.isSupportsAdmin()) {
            throw new IllegalArgumentException("El theme administrativo seleccionado no admite backoffice.");
        }

        validateLayout(form.getPublicLayoutCode(), "PUBLIC", publicTheme.getCode());
        validateLayout(form.getAdminLayoutCode(), "ADMIN", adminTheme.getCode());
        Matrix26LayoutCatalog login = requireLayout(form.getLoginLayoutCode());
        if (!"LOGIN".equals(login.getArea())) {
            throw new IllegalArgumentException("El layout de login seleccionado no pertenece al área LOGIN.");
        }
        if (!compatible(login, adminTheme.getCode()) && !compatible(login, publicTheme.getCode())) {
            throw new IllegalArgumentException("El layout de login no es compatible con los themes seleccionados.");
        }

        requireAllowed("modo del sidebar", form.getSidebarMode(), SIDEBAR_MODES);
        requireAllowed("radio de bordes", form.getBorderRadius(), BORDER_RADII);
        requireAllowed("densidad de tablas", form.getTableDensity(), TABLE_DENSITIES);
        requireAllowed("ancho del contenido", form.getContentWidth(), CONTENT_WIDTHS);
        requireAllowed("estilo de títulos", form.getHeadingStyle(), HEADING_STYLES);

        Map<String, String> values = effectiveColors(form);
        requireContrast(values.get("textColor"), values.get("backgroundColor"), 4.5,
                "El texto y el fondo general no alcanzan un contraste accesible.");
        requireContrast(values.get("textColor"), values.get("surfaceColor"), 4.5,
                "El texto y las tarjetas no alcanzan un contraste accesible.");
        requireContrast("#FFFFFF", values.get("primaryColor"), 3.0,
                "El color principal es demasiado claro para botones con texto blanco.");
    }

    @Transactional(readOnly = true)
    public Map<String, String> previewVariables(Matrix26AppearanceEditorForm form) {
        validate(form);
        Map<String, String> values = effectiveColors(form);
        values.put("radiusValue", radiusValue(form.getBorderRadius()));
        values.put("tableDensityValue", densityValue(form.getTableDensity()));
        values.put("contentWidthValue", contentWidthValue(form.getContentWidth()));
        values.put("sidebarBackground", sidebarBackground(form.getSidebarMode(), values));
        values.put("sidebarText", "LIGHT".equals(form.getSidebarMode()) ? "#172033" : "#FFFFFF");
        return values;
    }

    private Matrix26AppearanceEditorForm fromValues(
            String publicTheme,
            String publicLayout,
            String adminTheme,
            String adminLayout,
            String loginLayout,
            String overridesJson,
            String reason
    ) {
        Matrix26AppearanceEditorForm form = new Matrix26AppearanceEditorForm();
        form.setPublicThemeCode(publicTheme);
        form.setPublicLayoutCode(publicLayout);
        form.setAdminThemeCode(adminTheme);
        form.setAdminLayoutCode(adminLayout);
        form.setLoginLayoutCode(loginLayout);
        form.setReason(reason);

        Map<String, String> overrides = readMap(overridesJson);
        Map<String, String> defaults = themeDefaults(adminTheme);
        form.setPrimaryColor(value(overrides, "primaryColor", defaults.get("primaryColor")));
        form.setSecondaryColor(value(overrides, "secondaryColor", defaults.get("secondaryColor")));
        form.setAccentColor(value(overrides, "accentColor", defaults.get("accentColor")));
        form.setBackgroundColor(value(overrides, "backgroundColor", defaults.get("backgroundColor")));
        form.setSurfaceColor(value(overrides, "surfaceColor", defaults.get("surfaceColor")));
        form.setTextColor(value(overrides, "textColor", defaults.get("textColor")));
        form.setSidebarMode(value(overrides, "sidebarMode", "THEME"));
        form.setBorderRadius(value(overrides, "borderRadius", radiusOption(defaults.get("radius"))));
        form.setTableDensity(value(overrides, "tableDensity", "COMFORTABLE"));
        form.setContentWidth(value(overrides, "contentWidth", "STANDARD"));
        form.setHeadingStyle(value(overrides, "headingStyle", "SYSTEM"));
        return form;
    }

    private Map<String, String> overrides(Matrix26AppearanceEditorForm form) {
        Map<String, String> result = new LinkedHashMap<>();
        result.put("primaryColor", normalizeColor(form.getPrimaryColor()));
        result.put("secondaryColor", normalizeColor(form.getSecondaryColor()));
        result.put("accentColor", normalizeColor(form.getAccentColor()));
        result.put("backgroundColor", normalizeColor(form.getBackgroundColor()));
        result.put("surfaceColor", normalizeColor(form.getSurfaceColor()));
        result.put("textColor", normalizeColor(form.getTextColor()));
        result.put("sidebarMode", normalizeOption(form.getSidebarMode()));
        result.put("borderRadius", normalizeOption(form.getBorderRadius()));
        result.put("tableDensity", normalizeOption(form.getTableDensity()));
        result.put("contentWidth", normalizeOption(form.getContentWidth()));
        result.put("headingStyle", normalizeOption(form.getHeadingStyle()));
        result.put("source", "matrix26-appearance-studio-phase3c2");
        return result;
    }

    private Map<String, String> effectiveColors(Matrix26AppearanceEditorForm form) {
        Map<String, String> defaults = themeDefaults(form.getAdminThemeCode());
        Map<String, String> result = new LinkedHashMap<>();
        result.put("primaryColor", colorOrDefault(form.getPrimaryColor(), defaults.get("primaryColor")));
        result.put("secondaryColor", colorOrDefault(form.getSecondaryColor(), defaults.get("secondaryColor")));
        result.put("accentColor", colorOrDefault(form.getAccentColor(), defaults.get("accentColor")));
        result.put("backgroundColor", colorOrDefault(form.getBackgroundColor(), defaults.get("backgroundColor")));
        result.put("surfaceColor", colorOrDefault(form.getSurfaceColor(), defaults.get("surfaceColor")));
        result.put("textColor", colorOrDefault(form.getTextColor(), defaults.get("textColor")));
        return result;
    }

    private Map<String, String> themeDefaults(String themeCode) {
        Matrix26ThemeCatalog theme = requireTheme(themeCode);
        Map<String, String> raw = readMap(theme.getTokensJson());
        Map<String, String> defaults = new LinkedHashMap<>();
        defaults.put("primaryColor", value(raw, "primary", "#2563EB"));
        defaults.put("secondaryColor", value(raw, "secondary", "#172554"));
        defaults.put("accentColor", value(raw, "accent", fallbackAccent(themeCode)));
        defaults.put("backgroundColor", value(raw, "background", "#F4F7FB"));
        defaults.put("surfaceColor", value(raw, "surface", "#FFFFFF"));
        defaults.put("textColor", value(raw, "text", "#172033"));
        defaults.put("radius", value(raw, "radius", "16px"));
        return defaults;
    }

    private String snapshotJson(Matrix26AppearanceEditorForm form, String status, int version) {
        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("status", status);
        snapshot.put("version", version);
        snapshot.put("publicTheme", form.getPublicThemeCode());
        snapshot.put("publicLayout", form.getPublicLayoutCode());
        snapshot.put("adminTheme", form.getAdminThemeCode());
        snapshot.put("adminLayout", form.getAdminLayoutCode());
        snapshot.put("loginLayout", form.getLoginLayoutCode());
        snapshot.put("overrides", overrides(form));
        return writeJson(snapshot);
    }

    private int nextHistoryVersion(Long instanceId) {
        return historyRepository.findTopByInstance_IdOrderByVersionDesc(instanceId)
                .map(item -> item.getVersion() + 1)
                .orElseGet(() -> requirePublished(instanceId).getPublishedVersion() + 1);
    }

    private PlatformBusinessClient requireInstance(Long id) {
        return clientRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("La instancia no existe."));
    }

    private Matrix26InstanceAppearance requirePublished(Long instanceId) {
        return appearanceRepository.findByInstance_Id(instanceId)
                .orElseThrow(() -> new IllegalArgumentException("La instancia todavía no tiene apariencia publicada."));
    }

    private Matrix26ThemeCatalog requireTheme(String code) {
        return themeRepository.findByCode(code)
                .filter(item -> ACTIVE.equals(item.getStatus()))
                .orElseThrow(() -> new IllegalArgumentException("El theme seleccionado no está disponible: " + code));
    }

    private Matrix26LayoutCatalog requireLayout(String code) {
        return layoutRepository.findByCode(code)
                .filter(item -> ACTIVE.equals(item.getStatus()))
                .orElseThrow(() -> new IllegalArgumentException("El layout seleccionado no está disponible: " + code));
    }

    private void validateLayout(String code, String area, String themeCode) {
        Matrix26LayoutCatalog layout = requireLayout(code);
        if (!area.equals(layout.getArea())) {
            throw new IllegalArgumentException("El layout " + layout.getName() + " no pertenece al área " + area + ".");
        }
        if (!compatible(layout, themeCode)) {
            throw new IllegalArgumentException("El layout " + layout.getName() + " no es compatible con el theme seleccionado.");
        }
    }

    private boolean compatible(Matrix26LayoutCatalog layout, String themeCode) {
        if (layout.getCompatibleThemes() == null || layout.getCompatibleThemes().isBlank()) {
            return true;
        }
        for (String candidate : layout.getCompatibleThemes().split(",")) {
            if (themeCode.equals(candidate.trim())) {
                return true;
            }
        }
        return false;
    }

    private void requireAllowed(String label, String value, Set<String> allowed) {
        String normalized = normalizeOption(value);
        if (!allowed.contains(normalized)) {
            throw new IllegalArgumentException("El valor seleccionado para " + label + " no es válido.");
        }
    }

    private void requireContrast(String foreground, String background, double minimum, String message) {
        if (contrastRatio(foreground, background) < minimum) {
            throw new IllegalArgumentException(message);
        }
    }

    private double contrastRatio(String first, String second) {
        double firstLum = luminance(first);
        double secondLum = luminance(second);
        double light = Math.max(firstLum, secondLum);
        double dark = Math.min(firstLum, secondLum);
        return (light + 0.05) / (dark + 0.05);
    }

    private double luminance(String color) {
        int r = Integer.parseInt(color.substring(1, 3), 16);
        int g = Integer.parseInt(color.substring(3, 5), 16);
        int b = Integer.parseInt(color.substring(5, 7), 16);
        return 0.2126 * channel(r) + 0.7152 * channel(g) + 0.0722 * channel(b);
    }

    private double channel(int value) {
        double normalized = value / 255.0;
        return normalized <= 0.03928 ? normalized / 12.92 : Math.pow((normalized + 0.055) / 1.055, 2.4);
    }

    private String colorOrDefault(String value, String fallback) {
        return value == null || value.isBlank() ? normalizeColor(fallback) : normalizeColor(value);
    }

    private String normalizeColor(String value) {
        return value == null ? "" : value.trim().toUpperCase(Locale.ROOT);
    }

    private String normalizeOption(String value) {
        return value == null ? "" : value.trim().toUpperCase(Locale.ROOT);
    }

    private String radiusValue(String option) {
        return switch (normalizeOption(option)) {
            case "SMALL" -> "10px";
            case "LARGE" -> "24px";
            default -> "16px";
        };
    }

    private String densityValue(String option) {
        return switch (normalizeOption(option)) {
            case "COMPACT" -> "8px";
            case "SPACIOUS" -> "18px";
            default -> "13px";
        };
    }

    private String contentWidthValue(String option) {
        return switch (normalizeOption(option)) {
            case "WIDE" -> "1800px";
            case "FULL" -> "100%";
            default -> "1600px";
        };
    }

    private String sidebarBackground(String option, Map<String, String> values) {
        return switch (normalizeOption(option)) {
            case "LIGHT" -> values.get("surfaceColor");
            case "DARK" -> "#111827";
            default -> values.get("secondaryColor");
        };
    }

    private String radiusOption(String radius) {
        if (radius == null) {
            return "MEDIUM";
        }
        String digits = radius.replaceAll("[^0-9]", "");
        if (digits.isBlank()) {
            return "MEDIUM";
        }
        int value = Integer.parseInt(digits);
        if (value <= 12) {
            return "SMALL";
        }
        if (value >= 20) {
            return "LARGE";
        }
        return "MEDIUM";
    }

    private String fallbackAccent(String themeCode) {
        return switch (themeCode) {
            case "matrix26-nature" -> "#2AA7A1";
            case "matrix26-warm" -> "#D69A36";
            default -> "#0891B2";
        };
    }

    private String value(Map<String, String> values, String key, String fallback) {
        String value = values.get(key);
        return value == null || value.isBlank() ? fallback : value;
    }

    private Map<String, String> readMap(String json) {
        return Matrix26JsonCodec.readFlatObject(json);
    }

    private String writeJson(Object value) {
        return Matrix26JsonCodec.write(value);
    }

    private String safeActor(String actor) {
        if (actor == null || actor.isBlank()) {
            return "matrix26-system";
        }
        return actor.length() > 120 ? actor.substring(0, 120) : actor;
    }
}
