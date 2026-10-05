package com.ecoamazonas.eco_agua.platform.control.demo;

public enum Matrix26DemoPortalMode {
    INTERNAL("Módulo interno", "text-bg-primary", "bi-window-sidebar"),
    MANAGED_RUNTIME("Runtime separado", "text-bg-success", "bi-hdd-network"),
    CONTROL_CENTER("Control Center", "text-bg-dark", "bi-command"),
    PLANNED("Pendiente", "text-bg-secondary", "bi-hourglass-split");

    private final String label;
    private final String badgeClass;
    private final String iconClass;

    Matrix26DemoPortalMode(String label, String badgeClass, String iconClass) {
        this.label = label;
        this.badgeClass = badgeClass;
        this.iconClass = iconClass;
    }

    public String getLabel() {
        return label;
    }

    public String getBadgeClass() {
        return badgeClass;
    }

    public String getIconClass() {
        return iconClass;
    }
}
