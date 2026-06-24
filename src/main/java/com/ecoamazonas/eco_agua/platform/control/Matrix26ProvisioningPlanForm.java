package com.ecoamazonas.eco_agua.platform.control;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.ArrayList;
import java.util.List;

public class Matrix26ProvisioningPlanForm {

    @NotBlank(message = "El nombre comercial es obligatorio.")
    @Size(max = 160, message = "El nombre comercial no puede superar 160 caracteres.")
    private String businessName;

    @Size(max = 180, message = "La razón social no puede superar 180 caracteres.")
    private String legalName;

    @Size(max = 100, message = "El tipo de negocio no puede superar 100 caracteres.")
    private String businessType;

    @NotBlank(message = "El código de instancia es obligatorio.")
    @Size(max = 80, message = "El código no puede superar 80 caracteres.")
    @Pattern(regexp = "^[a-z0-9]+(?:-[a-z0-9]+)*$", message = "Usa minúsculas, números y guiones, por ejemplo: mi-negocio.")
    private String instanceCode;

    @NotBlank(message = "El nombre de la base es obligatorio.")
    @Size(max = 120, message = "El nombre de la base no puede superar 120 caracteres.")
    @Pattern(regexp = "^[a-zA-Z0-9_]+$", message = "La base solo puede contener letras, números y guion bajo.")
    private String databaseName;

    @NotBlank(message = "El runtime es obligatorio.")
    @Size(max = 120, message = "El runtime no puede superar 120 caracteres.")
    @Pattern(regexp = "^[a-zA-Z0-9_-]+$", message = "El runtime solo puede contener letras, números, guion y guion bajo.")
    private String runtimeProfile;

    @NotNull(message = "El puerto es obligatorio.")
    @Min(value = 1024, message = "El puerto debe ser mayor o igual a 1024.")
    @Max(value = 65535, message = "El puerto no puede superar 65535.")
    private Integer runtimePort;

    @NotBlank(message = "La URL es obligatoria.")
    @Size(max = 500, message = "La URL no puede superar 500 caracteres.")
    @Pattern(regexp = "^https?://.+$", message = "La URL debe comenzar con http:// o https://.")
    private String publicUrl;

    @Size(max = 120, message = "La ciudad no puede superar 120 caracteres.")
    private String city = "Iquitos";

    @NotBlank(message = "El usuario administrador es obligatorio.")
    @Size(min = 3, max = 20, message = "El usuario debe contener entre 3 y 20 caracteres.")
    @Pattern(regexp = "^[a-zA-Z0-9._-]+$", message = "El usuario solo puede contener letras, números, punto, guion y guion bajo.")
    private String adminUsername;

    @Email(message = "Ingresa un correo válido.")
    @Size(max = 180, message = "El correo no puede superar 180 caracteres.")
    private String adminEmail;

    @NotEmpty(message = "Selecciona al menos un módulo funcional.")
    private List<String> selectedModules = new ArrayList<>();

    private boolean demoDataEnabled;

    @NotBlank(message = "Selecciona un preset visual.")
    @Size(max = 80, message = "El preset visual no puede superar 80 caracteres.")
    private String appearancePresetCode;

    @NotBlank(message = "Selecciona un theme público.")
    @Size(max = 80, message = "El theme público no puede superar 80 caracteres.")
    private String publicThemeCode;

    @NotBlank(message = "Selecciona un layout público.")
    @Size(max = 80, message = "El layout público no puede superar 80 caracteres.")
    private String publicLayoutCode;

    @NotBlank(message = "Selecciona un theme administrativo.")
    @Size(max = 80, message = "El theme administrativo no puede superar 80 caracteres.")
    private String adminThemeCode;

    @NotBlank(message = "Selecciona un layout administrativo.")
    @Size(max = 80, message = "El layout administrativo no puede superar 80 caracteres.")
    private String adminLayoutCode;

    @NotBlank(message = "Selecciona un layout de login.")
    @Size(max = 80, message = "El layout de login no puede superar 80 caracteres.")
    private String loginLayoutCode;

    @Pattern(regexp = "^#[0-9A-Fa-f]{6}$", message = "Usa un color hexadecimal como #B4532A.")
    private String primaryColor;

    @Pattern(regexp = "^#[0-9A-Fa-f]{6}$", message = "Usa un color hexadecimal como #5F2D1D.")
    private String secondaryColor;

    @Pattern(regexp = "^#[0-9A-Fa-f]{6}$", message = "Usa un color hexadecimal como #D69A36.")
    private String accentColor;

    @Pattern(regexp = "^#[0-9A-Fa-f]{6}$", message = "Usa un color hexadecimal como #FFF8F1.")
    private String backgroundColor;

    @Pattern(regexp = "^#[0-9A-Fa-f]{6}$", message = "Usa un color hexadecimal como #FFFFFF.")
    private String surfaceColor;

    @Pattern(regexp = "^#[0-9A-Fa-f]{6}$", message = "Usa un color hexadecimal como #3F241B.")
    private String textColor;

    @NotBlank(message = "Selecciona el modo del sidebar.")
    @Pattern(regexp = "^(THEME|LIGHT|DARK)$", message = "Modo de sidebar no válido.")
    private String sidebarMode = "THEME";

    @NotBlank(message = "Selecciona el radio de bordes.")
    @Pattern(regexp = "^(SMALL|MEDIUM|LARGE)$", message = "Radio de bordes no válido.")
    private String borderRadius = "MEDIUM";

    @NotBlank(message = "Selecciona la densidad de tablas.")
    @Pattern(regexp = "^(COMPACT|COMFORTABLE|SPACIOUS)$", message = "Densidad de tablas no válida.")
    private String tableDensity = "COMFORTABLE";

    @NotBlank(message = "Selecciona el ancho del contenido.")
    @Pattern(regexp = "^(STANDARD|WIDE|FULL)$", message = "Ancho de contenido no válido.")
    private String contentWidth = "STANDARD";

    @NotBlank(message = "Selecciona el estilo de títulos.")
    @Pattern(regexp = "^(SYSTEM|STRONG|EDITORIAL)$", message = "Estilo de títulos no válido.")
    private String headingStyle = "SYSTEM";

    @NotBlank(message = "El nombre visible es obligatorio.")
    @Size(max = 160, message = "El nombre visible no puede superar 160 caracteres.")
    private String brandingDisplayName;

    @NotBlank(message = "El nombre corto es obligatorio.")
    @Size(max = 100, message = "El nombre corto no puede superar 100 caracteres.")
    private String brandingShortName;

    @Size(max = 220, message = "El eslogan no puede superar 220 caracteres.")
    private String brandingTagline;

    @Size(max = 300, message = "El mensaje de bienvenida no puede superar 300 caracteres.")
    private String brandingWelcomeMessage;

    @Size(max = 220, message = "El título del hero no puede superar 220 caracteres.")
    private String brandingHeroTitle;

    @Size(max = 500, message = "El subtítulo del hero no puede superar 500 caracteres.")
    private String brandingHeroSubtitle;

    @Size(max = 80, message = "El CTA principal no puede superar 80 caracteres.")
    private String brandingPrimaryCtaLabel;

    @Size(max = 80, message = "El CTA secundario no puede superar 80 caracteres.")
    private String brandingSecondaryCtaLabel;

    @Size(max = 80, message = "El teléfono no puede superar 80 caracteres.")
    private String brandingContactPhone;

    @Size(max = 80, message = "El WhatsApp no puede superar 80 caracteres.")
    private String brandingWhatsapp;

    @Size(max = 180, message = "La ubicación no puede superar 180 caracteres.")
    private String brandingLocation;

    private boolean brandingDemoAssetsEnabled;

    @Size(max = 4000, message = "Las notas no pueden superar 4000 caracteres.")
    private String notes;

    public String getBusinessName() {
        return businessName;
    }

    public void setBusinessName(String businessName) {
        this.businessName = businessName;
    }

    public String getLegalName() {
        return legalName;
    }

    public void setLegalName(String legalName) {
        this.legalName = legalName;
    }

    public String getBusinessType() {
        return businessType;
    }

    public void setBusinessType(String businessType) {
        this.businessType = businessType;
    }

    public String getInstanceCode() {
        return instanceCode;
    }

    public void setInstanceCode(String instanceCode) {
        this.instanceCode = instanceCode;
    }

    public String getDatabaseName() {
        return databaseName;
    }

    public void setDatabaseName(String databaseName) {
        this.databaseName = databaseName;
    }

    public String getRuntimeProfile() {
        return runtimeProfile;
    }

    public void setRuntimeProfile(String runtimeProfile) {
        this.runtimeProfile = runtimeProfile;
    }

    public Integer getRuntimePort() {
        return runtimePort;
    }

    public void setRuntimePort(Integer runtimePort) {
        this.runtimePort = runtimePort;
    }

    public String getPublicUrl() {
        return publicUrl;
    }

    public void setPublicUrl(String publicUrl) {
        this.publicUrl = publicUrl;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getAdminUsername() {
        return adminUsername;
    }

    public void setAdminUsername(String adminUsername) {
        this.adminUsername = adminUsername;
    }

    public String getAdminEmail() {
        return adminEmail;
    }

    public void setAdminEmail(String adminEmail) {
        this.adminEmail = adminEmail;
    }

    public List<String> getSelectedModules() {
        return selectedModules;
    }

    public void setSelectedModules(List<String> selectedModules) {
        this.selectedModules = selectedModules == null ? new ArrayList<>() : new ArrayList<>(selectedModules);
    }

    public boolean isDemoDataEnabled() {
        return demoDataEnabled;
    }

    public void setDemoDataEnabled(boolean demoDataEnabled) {
        this.demoDataEnabled = demoDataEnabled;
    }


    public String getAppearancePresetCode() { return appearancePresetCode; }
    public void setAppearancePresetCode(String appearancePresetCode) { this.appearancePresetCode = appearancePresetCode; }
    public String getPublicThemeCode() { return publicThemeCode; }
    public void setPublicThemeCode(String publicThemeCode) { this.publicThemeCode = publicThemeCode; }
    public String getPublicLayoutCode() { return publicLayoutCode; }
    public void setPublicLayoutCode(String publicLayoutCode) { this.publicLayoutCode = publicLayoutCode; }
    public String getAdminThemeCode() { return adminThemeCode; }
    public void setAdminThemeCode(String adminThemeCode) { this.adminThemeCode = adminThemeCode; }
    public String getAdminLayoutCode() { return adminLayoutCode; }
    public void setAdminLayoutCode(String adminLayoutCode) { this.adminLayoutCode = adminLayoutCode; }
    public String getLoginLayoutCode() { return loginLayoutCode; }
    public void setLoginLayoutCode(String loginLayoutCode) { this.loginLayoutCode = loginLayoutCode; }
    public String getPrimaryColor() { return primaryColor; }
    public void setPrimaryColor(String primaryColor) { this.primaryColor = primaryColor; }
    public String getSecondaryColor() { return secondaryColor; }
    public void setSecondaryColor(String secondaryColor) { this.secondaryColor = secondaryColor; }
    public String getAccentColor() { return accentColor; }
    public void setAccentColor(String accentColor) { this.accentColor = accentColor; }
    public String getBackgroundColor() { return backgroundColor; }
    public void setBackgroundColor(String backgroundColor) { this.backgroundColor = backgroundColor; }
    public String getSurfaceColor() { return surfaceColor; }
    public void setSurfaceColor(String surfaceColor) { this.surfaceColor = surfaceColor; }
    public String getTextColor() { return textColor; }
    public void setTextColor(String textColor) { this.textColor = textColor; }
    public String getSidebarMode() { return sidebarMode; }
    public void setSidebarMode(String sidebarMode) { this.sidebarMode = sidebarMode; }
    public String getBorderRadius() { return borderRadius; }
    public void setBorderRadius(String borderRadius) { this.borderRadius = borderRadius; }
    public String getTableDensity() { return tableDensity; }
    public void setTableDensity(String tableDensity) { this.tableDensity = tableDensity; }
    public String getContentWidth() { return contentWidth; }
    public void setContentWidth(String contentWidth) { this.contentWidth = contentWidth; }
    public String getHeadingStyle() { return headingStyle; }
    public void setHeadingStyle(String headingStyle) { this.headingStyle = headingStyle; }
    public String getBrandingDisplayName() { return brandingDisplayName; }
    public void setBrandingDisplayName(String brandingDisplayName) { this.brandingDisplayName = brandingDisplayName; }
    public String getBrandingShortName() { return brandingShortName; }
    public void setBrandingShortName(String brandingShortName) { this.brandingShortName = brandingShortName; }
    public String getBrandingTagline() { return brandingTagline; }
    public void setBrandingTagline(String brandingTagline) { this.brandingTagline = brandingTagline; }
    public String getBrandingWelcomeMessage() { return brandingWelcomeMessage; }
    public void setBrandingWelcomeMessage(String brandingWelcomeMessage) { this.brandingWelcomeMessage = brandingWelcomeMessage; }
    public String getBrandingHeroTitle() { return brandingHeroTitle; }
    public void setBrandingHeroTitle(String brandingHeroTitle) { this.brandingHeroTitle = brandingHeroTitle; }
    public String getBrandingHeroSubtitle() { return brandingHeroSubtitle; }
    public void setBrandingHeroSubtitle(String brandingHeroSubtitle) { this.brandingHeroSubtitle = brandingHeroSubtitle; }
    public String getBrandingPrimaryCtaLabel() { return brandingPrimaryCtaLabel; }
    public void setBrandingPrimaryCtaLabel(String brandingPrimaryCtaLabel) { this.brandingPrimaryCtaLabel = brandingPrimaryCtaLabel; }
    public String getBrandingSecondaryCtaLabel() { return brandingSecondaryCtaLabel; }
    public void setBrandingSecondaryCtaLabel(String brandingSecondaryCtaLabel) { this.brandingSecondaryCtaLabel = brandingSecondaryCtaLabel; }
    public String getBrandingContactPhone() { return brandingContactPhone; }
    public void setBrandingContactPhone(String brandingContactPhone) { this.brandingContactPhone = brandingContactPhone; }
    public String getBrandingWhatsapp() { return brandingWhatsapp; }
    public void setBrandingWhatsapp(String brandingWhatsapp) { this.brandingWhatsapp = brandingWhatsapp; }
    public String getBrandingLocation() { return brandingLocation; }
    public void setBrandingLocation(String brandingLocation) { this.brandingLocation = brandingLocation; }
    public boolean isBrandingDemoAssetsEnabled() { return brandingDemoAssetsEnabled; }
    public void setBrandingDemoAssetsEnabled(boolean brandingDemoAssetsEnabled) { this.brandingDemoAssetsEnabled = brandingDemoAssetsEnabled; }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }
}
