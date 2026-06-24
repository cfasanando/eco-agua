package com.ecoamazonas.eco_agua.platform.control.appearance;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class Matrix26AppearanceEditorForm {

    private static final String HEX_OR_EMPTY = "^$|^#[0-9A-Fa-f]{6}$";

    @NotBlank(message = "Selecciona un theme público.")
    private String publicThemeCode;

    @NotBlank(message = "Selecciona un layout público.")
    private String publicLayoutCode;

    @NotBlank(message = "Selecciona un theme administrativo.")
    private String adminThemeCode;

    @NotBlank(message = "Selecciona un layout administrativo.")
    private String adminLayoutCode;

    @NotBlank(message = "Selecciona un layout de login.")
    private String loginLayoutCode;

    private boolean customPalette;

    @Pattern(regexp = HEX_OR_EMPTY, message = "Usa un color hexadecimal como #2563EB.")
    private String primaryColor;

    @Pattern(regexp = HEX_OR_EMPTY, message = "Usa un color hexadecimal como #172554.")
    private String secondaryColor;

    @Pattern(regexp = HEX_OR_EMPTY, message = "Usa un color hexadecimal como #0891B2.")
    private String accentColor;

    @Pattern(regexp = HEX_OR_EMPTY, message = "Usa un color hexadecimal como #F4F7FB.")
    private String backgroundColor;

    @Pattern(regexp = HEX_OR_EMPTY, message = "Usa un color hexadecimal como #FFFFFF.")
    private String surfaceColor;

    @Pattern(regexp = HEX_OR_EMPTY, message = "Usa un color hexadecimal como #172033.")
    private String textColor;

    @NotBlank(message = "Selecciona el modo del sidebar.")
    private String sidebarMode = "THEME";

    @NotBlank(message = "Selecciona el radio de bordes.")
    private String borderRadius = "THEME";

    @NotBlank(message = "Selecciona la densidad de tablas.")
    private String tableDensity = "COMFORTABLE";

    @NotBlank(message = "Selecciona el ancho del contenido.")
    private String contentWidth = "STANDARD";

    @NotBlank(message = "Selecciona el estilo de títulos.")
    private String headingStyle = "SYSTEM";

    @Size(max = 500, message = "El motivo puede tener como máximo 500 caracteres.")
    private String reason;

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
    public boolean isCustomPalette() { return customPalette; }
    public void setCustomPalette(boolean customPalette) { this.customPalette = customPalette; }
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
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
}
