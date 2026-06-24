package com.ecoamazonas.eco_agua.platform.control.appearance;

import com.ecoamazonas.eco_agua.platform.PlatformBusinessClient;
import com.ecoamazonas.eco_agua.platform.control.Matrix26ProvisioningJob;
import com.ecoamazonas.eco_agua.platform.control.Matrix26ProvisioningPlanForm;
import com.ecoamazonas.eco_agua.platform.control.Matrix26TargetDatabaseService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@ConditionalOnProperty(name = "matrix26.control-center.enabled", havingValue = "true")
public class Matrix26ProvisioningAppearanceService {

    private static final String ACTIVE = "ACTIVE";
    private static final String DEMO_RESOURCE_ROOT = "classpath:/static/demo/branding/restaurant-lab/";

    private static final Map<String, String> DEMO_ASSETS = Map.ofEntries(
            Map.entry("logo-primary", "logo-primary.png"),
            Map.entry("logo-compact", "logo-compact.png"),
            Map.entry("favicon", "favicon.png"),
            Map.entry("login-cover", "login-cover.jpg"),
            Map.entry("hero-primary", "hero-primary.jpg"),
            Map.entry("hero-secondary", "hero-secondary.jpg"),
            Map.entry("product-placeholder", "product-placeholder.png"),
            Map.entry("social-share", "social-share.jpg")
    );

    private final Matrix26ThemeCatalogRepository themeRepository;
    private final Matrix26LayoutCatalogRepository layoutRepository;
    private final Matrix26InstanceAppearanceRepository appearanceRepository;
    private final Matrix26InstanceAppearanceHistoryRepository historyRepository;
    private final Matrix26TargetDatabaseService targetDatabaseService;
    private final ResourceLoader resourceLoader;
    private final Path dataRoot;

    public Matrix26ProvisioningAppearanceService(
            Matrix26ThemeCatalogRepository themeRepository,
            Matrix26LayoutCatalogRepository layoutRepository,
            Matrix26InstanceAppearanceRepository appearanceRepository,
            Matrix26InstanceAppearanceHistoryRepository historyRepository,
            Matrix26TargetDatabaseService targetDatabaseService,
            ResourceLoader resourceLoader,
            @Value("${matrix26.appearance-data-directory:runtime-data}") String dataDirectory
    ) {
        this.themeRepository = themeRepository;
        this.layoutRepository = layoutRepository;
        this.appearanceRepository = appearanceRepository;
        this.historyRepository = historyRepository;
        this.targetDatabaseService = targetDatabaseService;
        this.resourceLoader = resourceLoader;
        this.dataRoot = Path.of(dataDirectory).toAbsolutePath().normalize();
    }

    public List<Matrix26ProvisioningAppearancePreset> presets() {
        return List.of(
                new Matrix26ProvisioningAppearancePreset(
                        "classic-business",
                        "Matrix26 Classic Business",
                        "Apariencia empresarial neutra, limpia y estable.",
                        "EMPRESARIAL",
                        "matrix26-classic",
                        "public-classic-grid",
                        "matrix26-classic",
                        "admin-sidebar-classic",
                        "login-split",
                        "#2563EB",
                        "#172554",
                        "#0891B2",
                        "#F4F7FB",
                        "#FFFFFF",
                        "#172033",
                        "THEME",
                        "MEDIUM",
                        "COMFORTABLE",
                        "STANDARD",
                        "SYSTEM",
                        false
                ),
                new Matrix26ProvisioningAppearancePreset(
                        "nature-amazon",
                        "Matrix26 Nature Amazon",
                        "Identidad natural con verdes amazónicos y layout editorial.",
                        "NATURALEZA",
                        "matrix26-nature",
                        "public-nature-editorial",
                        "matrix26-nature",
                        "admin-sidebar-classic",
                        "login-split",
                        "#117A57",
                        "#145A4A",
                        "#2AA7A1",
                        "#F3F8F3",
                        "#FFFFFF",
                        "#173C32",
                        "THEME",
                        "LARGE",
                        "COMFORTABLE",
                        "WIDE",
                        "EDITORIAL",
                        false
                ),
                new Matrix26ProvisioningAppearancePreset(
                        "warm-restaurant",
                        "Matrix26 Warm Restaurant",
                        "Experiencia gastronómica cálida con carta visual y workspace compacto.",
                        "RESTAURANTE",
                        "matrix26-warm",
                        "public-restaurant-visual",
                        "matrix26-classic",
                        "admin-compact-workspace",
                        "login-split",
                        "#B4532A",
                        "#5F2D1D",
                        "#D69A36",
                        "#FFF8F1",
                        "#FFFFFF",
                        "#3F241B",
                        "DARK",
                        "LARGE",
                        "COMPACT",
                        "WIDE",
                        "STRONG",
                        true
                )
        );
    }

    public List<Matrix26ThemeCatalog> activeThemes() {
        return themeRepository.findByStatusOrderByDisplayOrderAscNameAsc(ACTIVE);
    }

    public List<Matrix26LayoutCatalog> publicLayouts() {
        return layoutRepository.findByAreaAndStatusOrderByDisplayOrderAscNameAsc("PUBLIC", ACTIVE);
    }

    public List<Matrix26LayoutCatalog> adminLayouts() {
        return layoutRepository.findByAreaAndStatusOrderByDisplayOrderAscNameAsc("ADMIN", ACTIVE);
    }

    public List<Matrix26LayoutCatalog> loginLayouts() {
        return layoutRepository.findByAreaAndStatusOrderByDisplayOrderAscNameAsc("LOGIN", ACTIVE);
    }

    public void applyDefaultPreset(Matrix26ProvisioningPlanForm form) {
        Matrix26ProvisioningAppearancePreset preset = preset("warm-restaurant");
        applyPreset(form, preset);
        applyBrandingDefaults(form);
    }

    public void applyPreset(Matrix26ProvisioningPlanForm form, String presetCode) {
        applyPreset(form, preset(presetCode));
    }

    public void prepareJob(Matrix26ProvisioningJob job, Matrix26ProvisioningPlanForm form) {
        Matrix26ProvisioningAppearancePreset selected = preset(form.getAppearancePresetCode());
        normalizeBrandingForBusiness(form);
        job.setAppearancePresetCode(selected.code());
        job.setPublicThemeCode(clean(form.getPublicThemeCode()));
        job.setPublicLayoutCode(clean(form.getPublicLayoutCode()));
        job.setAdminThemeCode(clean(form.getAdminThemeCode()));
        job.setAdminLayoutCode(clean(form.getAdminLayoutCode()));
        job.setLoginLayoutCode(clean(form.getLoginLayoutCode()));
        job.setAppearanceOverridesJson(Matrix26JsonCodec.write(overrides(form)));
        job.setBrandingJson(Matrix26JsonCodec.write(branding(form)));
        job.setBrandingDemoAssetsEnabled(form.isBrandingDemoAssetsEnabled());
    }

    public List<String> validate(Matrix26ProvisioningJob job) {
        List<String> errors = new ArrayList<>();
        Matrix26ThemeCatalog publicTheme = themeRepository.findByCode(job.getPublicThemeCode()).orElse(null);
        Matrix26ThemeCatalog adminTheme = themeRepository.findByCode(job.getAdminThemeCode()).orElse(null);
        Matrix26LayoutCatalog publicLayout = layoutRepository.findByCode(job.getPublicLayoutCode()).orElse(null);
        Matrix26LayoutCatalog adminLayout = layoutRepository.findByCode(job.getAdminLayoutCode()).orElse(null);
        Matrix26LayoutCatalog loginLayout = layoutRepository.findByCode(job.getLoginLayoutCode()).orElse(null);

        if (presetOrNull(job.getAppearancePresetCode()) == null) {
            errors.add("El preset visual seleccionado no existe.");
        }
        if (publicTheme == null || !publicTheme.isSupportsPublic() || !ACTIVE.equals(publicTheme.getStatus())) {
            errors.add("El theme público no existe o no admite frontend público.");
        }
        if (adminTheme == null || !adminTheme.isSupportsAdmin() || !ACTIVE.equals(adminTheme.getStatus())) {
            errors.add("El theme administrativo no existe o no admite backoffice.");
        }
        validateLayout(errors, publicLayout, "PUBLIC", job.getPublicThemeCode(), "público");
        validateLayout(errors, adminLayout, "ADMIN", job.getAdminThemeCode(), "administrativo");
        validateLayout(errors, loginLayout, "LOGIN", job.getAdminThemeCode(), "de login");

        Map<String, String> overrides = Matrix26JsonCodec.readFlatObject(job.getAppearanceOverridesJson());
        for (String key : List.of(
                "primaryColor", "secondaryColor", "accentColor",
                "backgroundColor", "surfaceColor", "textColor"
        )) {
            String value = overrides.get(key);
            if (value == null || !value.matches("^#[0-9A-Fa-f]{6}$")) {
                errors.add("El color " + key + " no es válido.");
            }
        }

        Map<String, String> branding = Matrix26JsonCodec.readFlatObject(job.getBrandingJson());
        if (clean(branding.get("displayName")).isBlank()) {
            errors.add("El nombre visible del branding es obligatorio.");
        }
        if (clean(branding.get("shortName")).isBlank()) {
            errors.add("El nombre corto del branding es obligatorio.");
        }
        if (job.isBrandingDemoAssetsEnabled()) {
            for (String file : DEMO_ASSETS.values()) {
                Resource resource = resourceLoader.getResource(DEMO_RESOURCE_ROOT + file);
                if (!resource.exists()) {
                    errors.add("No se encontró el recurso demo " + file + ".");
                }
            }
        }
        return errors;
    }

    public Matrix26ProvisioningAppearanceSummary summary(Matrix26ProvisioningJob job) {
        Matrix26ProvisioningAppearancePreset selected = presetOrNull(job.getAppearancePresetCode());
        if (selected == null) {
            selected = preset("classic-business");
        }
        String publicTheme = defaultValue(job.getPublicThemeCode(), selected.publicThemeCode());
        String publicLayout = defaultValue(job.getPublicLayoutCode(), selected.publicLayoutCode());
        String adminTheme = defaultValue(job.getAdminThemeCode(), selected.adminThemeCode());
        String adminLayout = defaultValue(job.getAdminLayoutCode(), selected.adminLayoutCode());
        String loginLayout = defaultValue(job.getLoginLayoutCode(), selected.loginLayoutCode());
        Map<String, String> overrides = Matrix26JsonCodec.readFlatObject(job.getAppearanceOverridesJson());
        if (overrides.isEmpty()) {
            overrides = presetOverrides(selected);
        }
        Map<String, String> branding = Matrix26JsonCodec.readFlatObject(job.getBrandingJson());
        if (branding.isEmpty()) {
            branding = Map.of(
                    "displayName", defaultValue(job.getBusinessName(), "Nueva instancia"),
                    "heroTitle", "Descubre " + defaultValue(job.getBusinessName(), "tu negocio")
            );
        }
        return new Matrix26ProvisioningAppearanceSummary(
                selected.name(),
                themeName(publicTheme),
                layoutName(publicLayout),
                themeName(adminTheme),
                layoutName(adminLayout),
                layoutName(loginLayout),
                overrides,
                branding,
                job.isBrandingDemoAssetsEnabled()
        );
    }

    public String installOnTarget(Matrix26ProvisioningJob job, String actor) {
        List<String> errors = validate(job);
        if (!errors.isEmpty()) {
            throw new IllegalArgumentException(String.join(" ", errors));
        }

        Map<String, String> manifest = job.isBrandingDemoAssetsEnabled()
                ? publishDemoAssets(job, 1)
                : Map.of();

        JdbcTemplate target = targetDatabaseService.targetJdbcTemplate(job.getDatabaseName());
        target.execute("""
                CREATE TABLE IF NOT EXISTS matrix26_instance_appearance_config (
                    id SMALLINT NOT NULL,
                    instance_code VARCHAR(80) NOT NULL,
                    public_theme_code VARCHAR(80) NOT NULL,
                    public_layout_code VARCHAR(80) NOT NULL,
                    admin_theme_code VARCHAR(80) NOT NULL,
                    admin_layout_code VARCHAR(80) NOT NULL,
                    login_layout_code VARCHAR(80) NOT NULL,
                    overrides_json TEXT NULL,
                    branding_json TEXT NULL,
                    asset_manifest_json TEXT NULL,
                    published_version INT NOT NULL,
                    published_at DATETIME(6) NOT NULL,
                    published_by VARCHAR(120) NOT NULL,
                    updated_at DATETIME(6) NOT NULL,
                    PRIMARY KEY (id)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
                """);
        ensureTargetColumn(
                target,
                "branding_json",
                "ALTER TABLE matrix26_instance_appearance_config ADD COLUMN branding_json TEXT NULL AFTER overrides_json"
        );
        ensureTargetColumn(
                target,
                "asset_manifest_json",
                "ALTER TABLE matrix26_instance_appearance_config ADD COLUMN asset_manifest_json TEXT NULL AFTER branding_json"
        );
        target.update(
                """
                INSERT INTO matrix26_instance_appearance_config (
                    id, instance_code, public_theme_code, public_layout_code,
                    admin_theme_code, admin_layout_code, login_layout_code,
                    overrides_json, branding_json, asset_manifest_json,
                    published_version, published_at, published_by, updated_at
                ) VALUES (
                    1, ?, ?, ?, ?, ?, ?, ?, ?, ?, 1, NOW(6), ?, NOW(6)
                )
                ON DUPLICATE KEY UPDATE
                    instance_code = VALUES(instance_code),
                    public_theme_code = VALUES(public_theme_code),
                    public_layout_code = VALUES(public_layout_code),
                    admin_theme_code = VALUES(admin_theme_code),
                    admin_layout_code = VALUES(admin_layout_code),
                    login_layout_code = VALUES(login_layout_code),
                    overrides_json = VALUES(overrides_json),
                    branding_json = VALUES(branding_json),
                    asset_manifest_json = VALUES(asset_manifest_json),
                    published_version = VALUES(published_version),
                    published_at = VALUES(published_at),
                    published_by = VALUES(published_by),
                    updated_at = NOW(6)
                """,
                job.getInstanceCode(),
                job.getPublicThemeCode(),
                job.getPublicLayoutCode(),
                job.getAdminThemeCode(),
                job.getAdminLayoutCode(),
                job.getLoginLayoutCode(),
                job.getAppearanceOverridesJson(),
                job.getBrandingJson(),
                Matrix26JsonCodec.write(manifest),
                safeActor(actor)
        );

        return job.isBrandingDemoAssetsEnabled()
                ? "Apariencia inicial y kit visual demo instalados como versión v1."
                : "Apariencia inicial instalada como versión v1.";
    }

    @Transactional
    public void registerCentralAppearance(
            PlatformBusinessClient instance,
            Matrix26ProvisioningJob job,
            String actor
    ) {
        Matrix26InstanceAppearance appearance = appearanceRepository.findByInstance_Id(instance.getId())
                .orElseGet(Matrix26InstanceAppearance::new);
        appearance.setInstance(instance);
        appearance.setPublicThemeCode(job.getPublicThemeCode());
        appearance.setPublicLayoutCode(job.getPublicLayoutCode());
        appearance.setAdminThemeCode(job.getAdminThemeCode());
        appearance.setAdminLayoutCode(job.getAdminLayoutCode());
        appearance.setLoginLayoutCode(job.getLoginLayoutCode());
        appearance.setOverridesJson(job.getAppearanceOverridesJson());
        appearance.setStatus("PUBLISHED");
        appearance.setPublishedVersion(1);
        appearance.setPublishedAt(LocalDateTime.now());
        appearance.setPublishedBy(safeActor(actor));
        appearanceRepository.save(appearance);

        boolean versionOneExists = historyRepository.findTop20ByInstance_IdOrderByVersionDesc(instance.getId())
                .stream()
                .anyMatch(item -> item.getVersion() == 1);
        if (!versionOneExists) {
            Map<String, Object> snapshot = new LinkedHashMap<>();
            snapshot.put("status", "PUBLISHED");
            snapshot.put("version", 1);
            snapshot.put("publicTheme", job.getPublicThemeCode());
            snapshot.put("publicLayout", job.getPublicLayoutCode());
            snapshot.put("adminTheme", job.getAdminThemeCode());
            snapshot.put("adminLayout", job.getAdminLayoutCode());
            snapshot.put("loginLayout", job.getLoginLayoutCode());
            snapshot.put("overrides", Matrix26JsonCodec.readObject(job.getAppearanceOverridesJson()));
            snapshot.put("branding", Matrix26JsonCodec.readObject(job.getBrandingJson()));
            snapshot.put("assets", demoManifest(job));
            snapshot.put("assetVersion", 1);

            Matrix26InstanceAppearanceHistory history = new Matrix26InstanceAppearanceHistory();
            history.setInstance(instance);
            history.setVersion(1);
            history.setStatus("PUBLISHED");
            history.setSnapshotJson(Matrix26JsonCodec.write(snapshot));
            history.setActorUsername(safeActor(actor));
            history.setReason("Initial appearance published during Matrix26 provisioning");
            historyRepository.save(history);
        }
    }

    private void applyPreset(
            Matrix26ProvisioningPlanForm form,
            Matrix26ProvisioningAppearancePreset preset
    ) {
        form.setAppearancePresetCode(preset.code());
        form.setPublicThemeCode(preset.publicThemeCode());
        form.setPublicLayoutCode(preset.publicLayoutCode());
        form.setAdminThemeCode(preset.adminThemeCode());
        form.setAdminLayoutCode(preset.adminLayoutCode());
        form.setLoginLayoutCode(preset.loginLayoutCode());
        form.setPrimaryColor(preset.primaryColor());
        form.setSecondaryColor(preset.secondaryColor());
        form.setAccentColor(preset.accentColor());
        form.setBackgroundColor(preset.backgroundColor());
        form.setSurfaceColor(preset.surfaceColor());
        form.setTextColor(preset.textColor());
        form.setSidebarMode(preset.sidebarMode());
        form.setBorderRadius(preset.borderRadius());
        form.setTableDensity(preset.tableDensity());
        form.setContentWidth(preset.contentWidth());
        form.setHeadingStyle(preset.headingStyle());
        form.setBrandingDemoAssetsEnabled(preset.demoAssetsEnabled());
    }

    private void normalizeBrandingForBusiness(Matrix26ProvisioningPlanForm form) {
        String businessName = defaultValue(form.getBusinessName(), "Nueva instancia Matrix26");
        if (clean(form.getBrandingDisplayName()).isBlank()
                || "Nueva instancia Matrix26".equals(form.getBrandingDisplayName())) {
            form.setBrandingDisplayName(businessName);
        }
        if (clean(form.getBrandingShortName()).isBlank()
                || "Nueva instancia Matrix26".equals(form.getBrandingShortName())) {
            form.setBrandingShortName(shorten(businessName, 100));
        }
        if (clean(form.getBrandingWelcomeMessage()).isBlank()
                || form.getBrandingWelcomeMessage().contains("Nueva instancia Matrix26")) {
            form.setBrandingWelcomeMessage("Bienvenido al sistema de gestión de " + shorten(businessName, 100) + ".");
        }
        if (clean(form.getBrandingHeroTitle()).isBlank()
                || form.getBrandingHeroTitle().contains("Nueva instancia Matrix26")) {
            form.setBrandingHeroTitle("Descubre " + shorten(businessName, 100));
        }
        if (clean(form.getBrandingLocation()).isBlank()) {
            form.setBrandingLocation(defaultValue(form.getCity(), "Iquitos, Loreto"));
        }
    }

    private void applyBrandingDefaults(Matrix26ProvisioningPlanForm form) {
        String name = clean(form.getBusinessName());
        if (name.isBlank()) {
            name = "Nueva instancia Matrix26";
        }
        form.setBrandingDisplayName(name);
        form.setBrandingShortName(shorten(name, 100));
        form.setBrandingTagline("Una experiencia propia administrada por Matrix26");
        form.setBrandingWelcomeMessage("Bienvenido al sistema de gestión de " + shorten(name, 100) + ".");
        form.setBrandingHeroTitle("Descubre " + shorten(name, 100));
        form.setBrandingHeroSubtitle("Productos, servicios y atención desde una experiencia visual personalizada.");
        form.setBrandingPrimaryCtaLabel("Ver catálogo");
        form.setBrandingSecondaryCtaLabel("Contactar");
        form.setBrandingLocation("Iquitos, Loreto");
    }

    private Map<String, String> presetOverrides(Matrix26ProvisioningAppearancePreset preset) {
        Map<String, String> result = new LinkedHashMap<>();
        result.put("primaryColor", preset.primaryColor());
        result.put("secondaryColor", preset.secondaryColor());
        result.put("accentColor", preset.accentColor());
        result.put("backgroundColor", preset.backgroundColor());
        result.put("surfaceColor", preset.surfaceColor());
        result.put("textColor", preset.textColor());
        result.put("sidebarMode", preset.sidebarMode());
        result.put("borderRadius", preset.borderRadius());
        result.put("tableDensity", preset.tableDensity());
        result.put("contentWidth", preset.contentWidth());
        result.put("headingStyle", preset.headingStyle());
        return result;
    }

    private Map<String, String> overrides(Matrix26ProvisioningPlanForm form) {
        Map<String, String> result = new LinkedHashMap<>();
        result.put("customPalette", "true");
        result.put("primaryColor", upper(form.getPrimaryColor()));
        result.put("secondaryColor", upper(form.getSecondaryColor()));
        result.put("accentColor", upper(form.getAccentColor()));
        result.put("backgroundColor", upper(form.getBackgroundColor()));
        result.put("surfaceColor", upper(form.getSurfaceColor()));
        result.put("textColor", upper(form.getTextColor()));
        result.put("sidebarMode", upper(form.getSidebarMode()));
        result.put("borderRadius", upper(form.getBorderRadius()));
        result.put("tableDensity", upper(form.getTableDensity()));
        result.put("contentWidth", upper(form.getContentWidth()));
        result.put("headingStyle", upper(form.getHeadingStyle()));
        result.put("source", "matrix26-provisioning-phase3c7");
        return result;
    }

    private Map<String, String> branding(Matrix26ProvisioningPlanForm form) {
        Map<String, String> result = new LinkedHashMap<>();
        result.put("displayName", clean(form.getBrandingDisplayName()));
        result.put("shortName", clean(form.getBrandingShortName()));
        result.put("tagline", clean(form.getBrandingTagline()));
        result.put("welcomeMessage", clean(form.getBrandingWelcomeMessage()));
        result.put("heroTitle", clean(form.getBrandingHeroTitle()));
        result.put("heroSubtitle", clean(form.getBrandingHeroSubtitle()));
        result.put("primaryCtaLabel", clean(form.getBrandingPrimaryCtaLabel()));
        result.put("secondaryCtaLabel", clean(form.getBrandingSecondaryCtaLabel()));
        result.put("contactPhone", clean(form.getBrandingContactPhone()));
        result.put("whatsapp", clean(form.getBrandingWhatsapp()));
        result.put("location", clean(form.getBrandingLocation()));
        return result;
    }

    private Map<String, String> publishDemoAssets(Matrix26ProvisioningJob job, int version) {
        Path current = dataRoot.resolve(safeCode(job.getInstanceCode()))
                .resolve("appearance")
                .resolve("current")
                .normalize();
        Path history = dataRoot.resolve(safeCode(job.getInstanceCode()))
                .resolve("appearance")
                .resolve("history")
                .resolve("v" + version)
                .normalize();
        assertInside(dataRoot, current);
        assertInside(dataRoot, history);

        try {
            deleteDirectory(current);
            deleteDirectory(history);
            Files.createDirectories(current);
            Files.createDirectories(history);

            Map<String, String> manifest = new LinkedHashMap<>();
            for (Map.Entry<String, String> entry : DEMO_ASSETS.entrySet()) {
                String extension = extension(entry.getValue());
                String targetName = entry.getKey() + "-v" + version + "." + extension;
                Resource resource = resourceLoader.getResource(DEMO_RESOURCE_ROOT + entry.getValue());
                if (!resource.exists()) {
                    throw new IllegalStateException("No se encontró el recurso demo " + entry.getValue() + ".");
                }
                try (InputStream input = resource.getInputStream()) {
                    Files.copy(input, current.resolve(targetName), StandardCopyOption.REPLACE_EXISTING);
                }
                Files.copy(current.resolve(targetName), history.resolve(targetName), StandardCopyOption.REPLACE_EXISTING);
                manifest.put(entry.getKey(), targetName);
            }
            return manifest;
        } catch (IOException ex) {
            throw new IllegalStateException("No se pudieron publicar los recursos visuales iniciales.", ex);
        }
    }

    private Map<String, String> demoManifest(Matrix26ProvisioningJob job) {
        if (!job.isBrandingDemoAssetsEnabled()) {
            return Map.of();
        }
        Map<String, String> manifest = new LinkedHashMap<>();
        DEMO_ASSETS.forEach((key, file) -> manifest.put(key, key + "-v1." + extension(file)));
        return manifest;
    }

    private void ensureTargetColumn(JdbcTemplate target, String column, String alterSql) {
        Integer count = target.queryForObject(
                """
                SELECT COUNT(*)
                FROM information_schema.COLUMNS
                WHERE TABLE_SCHEMA = DATABASE()
                  AND TABLE_NAME = 'matrix26_instance_appearance_config'
                  AND COLUMN_NAME = ?
                """,
                Integer.class,
                column
        );
        if (count == null || count == 0) {
            target.execute(alterSql);
        }
    }

    private void validateLayout(
            List<String> errors,
            Matrix26LayoutCatalog layout,
            String expectedArea,
            String themeCode,
            String label
    ) {
        if (layout == null || !expectedArea.equals(layout.getArea()) || !ACTIVE.equals(layout.getStatus())) {
            errors.add("El layout " + label + " no existe o no está activo.");
            return;
        }
        String compatible = clean(layout.getCompatibleThemes());
        if (!compatible.isBlank() && !"*".equals(compatible)) {
            Set<String> allowed = Arrays.stream(compatible.split(","))
                    .map(this::clean)
                    .map(value -> value.toLowerCase(Locale.ROOT))
                    .collect(Collectors.toSet());
            if (!allowed.contains(clean(themeCode).toLowerCase(Locale.ROOT))) {
                errors.add("El layout " + label + " no es compatible con el theme seleccionado.");
            }
        }
    }

    private Matrix26ProvisioningAppearancePreset preset(String code) {
        Matrix26ProvisioningAppearancePreset preset = presetOrNull(code);
        if (preset == null) {
            throw new IllegalArgumentException("El preset visual seleccionado no existe.");
        }
        return preset;
    }

    private Matrix26ProvisioningAppearancePreset presetOrNull(String code) {
        String clean = clean(code);
        return presets().stream()
                .filter(item -> item.code().equalsIgnoreCase(clean))
                .findFirst()
                .orElse(null);
    }

    private String themeName(String code) {
        return themeRepository.findByCode(code).map(Matrix26ThemeCatalog::getName).orElse(code);
    }

    private String layoutName(String code) {
        return layoutRepository.findByCode(code).map(Matrix26LayoutCatalog::getName).orElse(code);
    }

    private String safeCode(String value) {
        String clean = clean(value).toLowerCase(Locale.ROOT);
        if (!clean.matches("^[a-z0-9]+(?:-[a-z0-9]+)*$")) {
            throw new IllegalArgumentException("Código de instancia no válido para recursos visuales.");
        }
        return clean;
    }

    private String extension(String fileName) {
        int dot = fileName.lastIndexOf('.');
        return dot < 0 ? "" : fileName.substring(dot + 1).toLowerCase(Locale.ROOT);
    }

    private void assertInside(Path root, Path path) {
        if (!path.toAbsolutePath().normalize().startsWith(root.toAbsolutePath().normalize())) {
            throw new IllegalArgumentException("Ruta de recursos visuales no válida.");
        }
    }

    private void deleteDirectory(Path directory) throws IOException {
        if (!Files.exists(directory)) {
            return;
        }
        try (var paths = Files.walk(directory)) {
            for (Path path : paths.sorted((left, right) -> right.compareTo(left)).toList()) {
                Files.deleteIfExists(path);
            }
        }
    }

    private String safeActor(String actor) {
        String clean = clean(actor);
        return clean.isBlank() ? "system" : shorten(clean, 120);
    }

    private String defaultValue(String value, String fallback) {
        String clean = clean(value);
        return clean.isBlank() ? fallback : clean;
    }

    private String upper(String value) {
        return clean(value).toUpperCase(Locale.ROOT);
    }

    private String clean(String value) {
        return value == null ? "" : value.trim();
    }

    private String shorten(String value, int max) {
        String clean = clean(value);
        return clean.length() <= max ? clean : clean.substring(0, max);
    }
}
