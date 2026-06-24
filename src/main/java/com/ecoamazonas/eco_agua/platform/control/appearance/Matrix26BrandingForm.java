package com.ecoamazonas.eco_agua.platform.control.appearance;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class Matrix26BrandingForm {

    @NotBlank
    @Size(max = 180)
    private String displayName;

    @NotBlank
    @Size(max = 100)
    private String shortName;

    @Size(max = 220)
    private String tagline;

    @Size(max = 300)
    private String welcomeMessage;

    @Size(max = 220)
    private String heroTitle;

    @Size(max = 500)
    private String heroSubtitle;

    @Size(max = 80)
    private String primaryCtaLabel;

    @Size(max = 80)
    private String secondaryCtaLabel;

    @Size(max = 80)
    private String contactPhone;

    @Size(max = 40)
    private String whatsapp;

    @Size(max = 160)
    private String location;

    @Size(max = 500)
    private String reason;

    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }
    public String getShortName() { return shortName; }
    public void setShortName(String shortName) { this.shortName = shortName; }
    public String getTagline() { return tagline; }
    public void setTagline(String tagline) { this.tagline = tagline; }
    public String getWelcomeMessage() { return welcomeMessage; }
    public void setWelcomeMessage(String welcomeMessage) { this.welcomeMessage = welcomeMessage; }
    public String getHeroTitle() { return heroTitle; }
    public void setHeroTitle(String heroTitle) { this.heroTitle = heroTitle; }
    public String getHeroSubtitle() { return heroSubtitle; }
    public void setHeroSubtitle(String heroSubtitle) { this.heroSubtitle = heroSubtitle; }
    public String getPrimaryCtaLabel() { return primaryCtaLabel; }
    public void setPrimaryCtaLabel(String primaryCtaLabel) { this.primaryCtaLabel = primaryCtaLabel; }
    public String getSecondaryCtaLabel() { return secondaryCtaLabel; }
    public void setSecondaryCtaLabel(String secondaryCtaLabel) { this.secondaryCtaLabel = secondaryCtaLabel; }
    public String getContactPhone() { return contactPhone; }
    public void setContactPhone(String contactPhone) { this.contactPhone = contactPhone; }
    public String getWhatsapp() { return whatsapp; }
    public void setWhatsapp(String whatsapp) { this.whatsapp = whatsapp; }
    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
}
