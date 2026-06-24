package com.ecoamazonas.eco_agua.platform.control;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class Matrix26ProvisioningExecutionForm {

    @NotBlank(message = "Escribe el código de referencia para confirmar.")
    private String confirmationReference;

    @NotBlank(message = "La contraseña inicial es obligatoria.")
    @Size(min = 10, max = 100, message = "La contraseña debe contener entre 10 y 100 caracteres.")
    private String adminPassword;

    @NotBlank(message = "Confirma la contraseña inicial.")
    private String adminPasswordConfirmation;

    @AssertTrue(message = "Debes confirmar que revisaste el plan y comprendes que se crearán recursos reales.")
    private boolean acknowledged;

    public String getConfirmationReference() { return confirmationReference; }
    public void setConfirmationReference(String confirmationReference) { this.confirmationReference = confirmationReference; }
    public String getAdminPassword() { return adminPassword; }
    public void setAdminPassword(String adminPassword) { this.adminPassword = adminPassword; }
    public String getAdminPasswordConfirmation() { return adminPasswordConfirmation; }
    public void setAdminPasswordConfirmation(String adminPasswordConfirmation) { this.adminPasswordConfirmation = adminPasswordConfirmation; }
    public boolean isAcknowledged() { return acknowledged; }
    public void setAcknowledged(boolean acknowledged) { this.acknowledged = acknowledged; }
}
