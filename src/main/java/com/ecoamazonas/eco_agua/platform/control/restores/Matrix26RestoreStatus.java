package com.ecoamazonas.eco_agua.platform.control.restores;

public enum Matrix26RestoreStatus {
    DRAFT("Draft", "text-bg-secondary"),
    VALIDATING("Validating", "text-bg-info"),
    RESUMING("Resuming", "text-bg-info"),
    DECRYPTING("Decrypting", "text-bg-info"),
    VERIFYING("Verifying", "text-bg-info"),
    CREATING_DATABASE("Creating database", "text-bg-primary"),
    IMPORTING_DATABASE("Importing database", "text-bg-primary"),
    RESTORING_FILES("Restoring files", "text-bg-primary"),
    GENERATING_RUNTIME("Generating runtime", "text-bg-primary"),
    REGISTERING_INSTANCE("Registering instance", "text-bg-primary"),
    STARTING_RUNTIME("Starting runtime", "text-bg-warning"),
    HEALTH_CHECKING("Health checking", "text-bg-warning"),
    COMPLETED("Completed", "text-bg-success"),
    FAILED("Failed", "text-bg-danger"),
    CLEANUP_REQUIRED("Cleanup required", "text-bg-warning"),
    CANCELLED("Cancelled", "text-bg-secondary");

    private final String label;
    private final String badgeClass;

    Matrix26RestoreStatus(String label, String badgeClass) {
        this.label = label;
        this.badgeClass = badgeClass;
    }

    public String getLabel() { return label; }
    public String getBadgeClass() { return badgeClass; }
}
