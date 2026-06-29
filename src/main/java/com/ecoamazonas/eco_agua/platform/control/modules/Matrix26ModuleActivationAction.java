package com.ecoamazonas.eco_agua.platform.control.modules;

public enum Matrix26ModuleActivationAction {
    ACTIVATED("Activated", "text-bg-success", "bi-toggle-on"),
    DEACTIVATED("Deactivated", "text-bg-secondary", "bi-toggle-off"),
    SYNCHRONIZED("Synchronized", "text-bg-primary", "bi-arrow-repeat"),
    NO_CHANGE("No change", "text-bg-light", "bi-dash-circle");

    private final String label;
    private final String badgeClass;
    private final String icon;

    Matrix26ModuleActivationAction(String label, String badgeClass, String icon) {
        this.label = label;
        this.badgeClass = badgeClass;
        this.icon = icon;
    }

    public String getLabel() {
        return label;
    }

    public String getBadgeClass() {
        return badgeClass;
    }

    public String getIcon() {
        return icon;
    }
}
