package com.ecoamazonas.eco_agua.delivery;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class DeliveryZoneForm {

    private Long id;

    @NotBlank
    @Size(max = 120)
    private String name;

    @NotNull
    @Min(-90)
    @Max(90)
    private Double latitude;

    @NotNull
    @Min(-180)
    @Max(180)
    private Double longitude;

    @NotNull
    @Min(100)
    @Max(30000)
    private Integer radiusMeters;

    @Size(max = 255)
    private String note;

    // --- Getters and setters ---

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public Double getLatitude() {
        return latitude;
    }

    public Double getLongitude() {
        return longitude;
    }

    public Integer getRadiusMeters() {
        return radiusMeters;
    }

    public String getNote() {
        return note;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setLatitude(Double latitude) {
        this.latitude = latitude;
    }

    public void setLongitude(Double longitude) {
        this.longitude = longitude;
    }

    public void setRadiusMeters(Integer radiusMeters) {
        this.radiusMeters = radiusMeters;
    }

    public void setNote(String note) {
        this.note = note;
    }

    // --- Mapping helpers ---

    public static DeliveryZoneForm fromEntity(DeliveryZone zone) {
        DeliveryZoneForm form = new DeliveryZoneForm();
        form.setId(zone.getId());
        form.setName(zone.getName());
        form.setLatitude(zone.getLatitude());
        form.setLongitude(zone.getLongitude());
        form.setRadiusMeters(zone.getRadiusMeters());
        form.setNote(zone.getNote());
        return form;
    }

    public void updateEntity(DeliveryZone zone) {
        zone.setName(this.name);
        zone.setLatitude(this.latitude);
        zone.setLongitude(this.longitude);
        zone.setRadiusMeters(this.radiusMeters);
        zone.setNote(this.note);
    }

    public DeliveryZone toNewEntity() {
        DeliveryZone zone = new DeliveryZone();
        updateEntity(zone);
        return zone;
    }
}
