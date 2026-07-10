package com.ecoamazonas.eco_agua.personalfinance;

public enum PersonalFinanceNegotiationChannel {
    PHONE("Llamada telefónica"),
    WHATSAPP("WhatsApp"),
    EMAIL("Correo electrónico"),
    IN_PERSON("Presencial"),
    WEB_PORTAL("Portal del acreedor"),
    LETTER("Carta"),
    OTHER("Otro");

    private final String label;

    PersonalFinanceNegotiationChannel(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
