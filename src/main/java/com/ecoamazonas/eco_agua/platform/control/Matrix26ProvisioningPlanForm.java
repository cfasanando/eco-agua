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

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }
}
