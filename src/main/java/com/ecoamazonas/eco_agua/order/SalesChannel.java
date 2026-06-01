package com.ecoamazonas.eco_agua.order;

public enum SalesChannel {
    WHATSAPP("WhatsApp"),
    IN_PERSON("Presencial"),
    FACEBOOK("Facebook"),
    TIKTOK("TikTok"),
    WEBSITE("Portal web"),
    REFERRAL("Referido"),
    OTHER("Otro");

    private final String label;

    SalesChannel(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
