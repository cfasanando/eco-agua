package com.ecoamazonas.eco_agua.platform.control.security;

public enum Matrix26ControlPermission {
    VIEW("matrix26.view", "View Control Center", "General", "View Matrix26 dashboards, inventory and read-only operational pages."),
    MANAGE_ALERTS("matrix26.alerts.manage", "Manage alerts", "Operations", "Acknowledge, resolve, ignore and reopen operation alerts."),
    CONTROL_RUNTIMES("matrix26.runtimes.control", "Control runtimes", "Operations", "Start, stop, restart, adopt and maintain managed runtimes."),
    MANAGE_BACKUPS("matrix26.backups.manage", "Manage backups", "Backups", "Create backups, verify packages, schedules, policies, alerts and retention."),
    MANAGE_RESTORES("matrix26.restores.manage", "Manage restores", "Restores", "Create, verify, resume, switch, confirm, rollback and cleanup restores."),
    MANAGE_LIFECYCLE("matrix26.lifecycle.manage", "Manage lifecycle", "Lifecycle", "Suspend, reactivate, archive and decommission instances."),
    MANAGE_PURGE("matrix26.purge.manage", "Manage purge", "Purge", "Prepare purge plans, execute purge and manage archive destruction."),
    MANAGE_APPEARANCE("matrix26.appearance.manage", "Manage appearance", "Appearance", "Edit, publish and rollback themes, layouts and branding."),
    MANAGE_PROVISIONING("matrix26.provisioning.manage", "Manage provisioning", "Provisioning", "Create, validate and execute provisioning jobs."),
    MANAGE_MODULES("matrix26.modules.manage", "Manage module activation", "Modules", "View and update Matrix26 module activation declarations by instance."),
    ADMINISTER_SECURITY("matrix26.security.admin", "Administer security", "Security", "View and administer Matrix26 roles, permissions and security settings."),
    ADMINISTER_SETTINGS("matrix26.settings.admin", "Administer settings", "Governance", "Manage central settings, modules and governance options.");

    private final String code;
    private final String label;
    private final String category;
    private final String description;

    Matrix26ControlPermission(String code, String label, String category, String description) {
        this.code = code;
        this.label = label;
        this.category = category;
        this.description = description;
    }

    public String code() {
        return code;
    }

    public String label() {
        return label;
    }

    public String category() {
        return category;
    }

    public String description() {
        return description;
    }
}
