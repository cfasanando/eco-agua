package com.ecoamazonas.eco_agua.platform.control;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class Matrix26InstanceForm {

    @NotBlank(message = "El código es obligatorio.")
    @Size(max = 80, message = "El código no puede superar 80 caracteres.")
    @Pattern(regexp = "^[a-z0-9]+(?:-[a-z0-9]+)*$", message = "Usa minúsculas, números y guiones, por ejemplo: mi-negocio.")
    private String code;

    @NotBlank(message = "El nombre comercial es obligatorio.")
    @Size(max = 160, message = "El nombre comercial no puede superar 160 caracteres.")
    private String businessName;

    @Size(max = 180, message = "La razón social no puede superar 180 caracteres.")
    private String legalName;

    @Size(max = 100, message = "El tipo de negocio no puede superar 100 caracteres.")
    private String businessType;

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

    @Size(max = 500, message = "El comando no puede superar 500 caracteres.")
    private String runtimeCommand;

    @NotBlank(message = "El estado es obligatorio.")
    @Pattern(regexp = "ACTIVE|INACTIVE|MAINTENANCE", message = "Selecciona un estado válido.")
    private String status = "ACTIVE";

    @NotBlank(message = "El modo de gestión es obligatorio.")
    @Pattern(regexp = "PROTECTED|REGISTERED|EXTERNAL", message = "Selecciona un modo válido.")
    private String managementMode = "REGISTERED";

    @Size(max = 120, message = "La ciudad no puede superar 120 caracteres.")
    private String city = "Iquitos";

    @Pattern(regexp = "^$|^#[0-9a-fA-F]{6}$", message = "Usa un color hexadecimal, por ejemplo #2563eb.")
    private String primaryColor = "#2563eb";

    private boolean monitorVisible = true;
    private boolean protectedInstance;

    @Size(max = 4000, message = "Las notas no pueden superar 4000 caracteres.")
    private String notes;

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

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

    public String getRuntimeCommand() {
        return runtimeCommand;
    }

    public void setRuntimeCommand(String runtimeCommand) {
        this.runtimeCommand = runtimeCommand;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getManagementMode() {
        return managementMode;
    }

    public void setManagementMode(String managementMode) {
        this.managementMode = managementMode;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getPrimaryColor() {
        return primaryColor;
    }

    public void setPrimaryColor(String primaryColor) {
        this.primaryColor = primaryColor;
    }

    public boolean isMonitorVisible() {
        return monitorVisible;
    }

    public void setMonitorVisible(boolean monitorVisible) {
        this.monitorVisible = monitorVisible;
    }

    public boolean isProtectedInstance() {
        return protectedInstance;
    }

    public void setProtectedInstance(boolean protectedInstance) {
        this.protectedInstance = protectedInstance;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }
}
