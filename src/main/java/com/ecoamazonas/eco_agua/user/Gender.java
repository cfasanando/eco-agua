package com.ecoamazonas.eco_agua.user;

public enum Gender {
    MALE,
    FEMALE,
    OTHER;

    public String getLabel() {
        return switch (this) {
            case MALE -> "Masculino";
            case FEMALE -> "Femenino";
            case OTHER -> "Otro";
        };
    }
}
