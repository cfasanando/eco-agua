package com.ecoamazonas.eco_agua.user;

public enum EmployeeAttendanceStatus {
    PRESENT("Presente"),
    LATE("Tardanza"),
    ABSENT("Falta"),
    PERMISSION("Permiso"),
    REST("Descanso");

    private final String label;

    EmployeeAttendanceStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
