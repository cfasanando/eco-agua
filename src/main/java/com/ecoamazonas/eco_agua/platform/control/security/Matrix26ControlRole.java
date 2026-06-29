package com.ecoamazonas.eco_agua.platform.control.security;

import java.util.Arrays;
import java.util.List;

public enum Matrix26ControlRole {
    VIEWER(
            "MATRIX26_VIEWER",
            "Matrix26 Viewer",
            "Read-only access to Matrix26 dashboards and operational evidence.",
            Matrix26ControlPermission.VIEW
    ),
    OPERATOR(
            "MATRIX26_OPERATOR",
            "Matrix26 Operator",
            "Runtime control and operational alert handling.",
            Matrix26ControlPermission.VIEW,
            Matrix26ControlPermission.MANAGE_ALERTS,
            Matrix26ControlPermission.CONTROL_RUNTIMES
    ),
    BACKUP_MANAGER(
            "MATRIX26_BACKUP_MANAGER",
            "Matrix26 Backup Manager",
            "Backup execution, verification, schedule and retention management.",
            Matrix26ControlPermission.VIEW,
            Matrix26ControlPermission.MANAGE_BACKUPS
    ),
    RESTORE_MANAGER(
            "MATRIX26_RESTORE_MANAGER",
            "Matrix26 Restore Manager",
            "Restore clone and in-place recovery management.",
            Matrix26ControlPermission.VIEW,
            Matrix26ControlPermission.MANAGE_RESTORES
    ),
    LIFECYCLE_MANAGER(
            "MATRIX26_LIFECYCLE_MANAGER",
            "Matrix26 Lifecycle Manager",
            "Suspend, reactivate, archive and decommission instances.",
            Matrix26ControlPermission.VIEW,
            Matrix26ControlPermission.MANAGE_LIFECYCLE
    ),
    PURGE_MANAGER(
            "MATRIX26_PURGE_MANAGER",
            "Matrix26 Purge Manager",
            "Controlled purge and archive destruction management.",
            Matrix26ControlPermission.VIEW,
            Matrix26ControlPermission.MANAGE_PURGE
    ),
    ADMIN(
            "MATRIX26_ADMIN",
            "Matrix26 Administrator",
            "Full Matrix26 control center administration.",
            Matrix26ControlPermission.values()
    );

    private final String code;
    private final String title;
    private final String description;
    private final List<Matrix26ControlPermission> permissions;

    Matrix26ControlRole(String code, String title, String description, Matrix26ControlPermission... permissions) {
        this.code = code;
        this.title = title;
        this.description = description;
        this.permissions = List.copyOf(Arrays.asList(permissions));
    }

    public String code() {
        return code;
    }

    public String title() {
        return title;
    }

    public String description() {
        return description;
    }

    public List<Matrix26ControlPermission> permissions() {
        return permissions;
    }
}
