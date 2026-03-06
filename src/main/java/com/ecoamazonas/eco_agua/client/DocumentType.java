package com.ecoamazonas.eco_agua.client;

public enum DocumentType {
    DNI("DNI"),
    RUC("RUC"),
    CE("Carné extranjería"),
    OTHER("Otro"),
    NONE("Sin documento");

    private final String label;

    DocumentType(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}