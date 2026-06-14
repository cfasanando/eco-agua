package com.ecoamazonas.eco_agua.delivery;

import jakarta.persistence.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "delivery_import_batch")
public class DeliveryImportBatch {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "route_date", nullable = false)
    private LocalDate routeDate;

    @Column(name = "title", length = 200, nullable = false)
    private String title;

    @Column(name = "source_filename", length = 255)
    private String sourceFilename;

    @Column(name = "delivery_person", length = 200)
    private String deliveryPerson;

    @Column(name = "total_stops", nullable = false)
    private Integer totalStops = 0;

    @Column(name = "located_stops", nullable = false)
    private Integer locatedStops = 0;

    @Column(name = "missing_location_stops", nullable = false)
    private Integer missingLocationStops = 0;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @OneToMany(mappedBy = "batch", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("routeOrderIndex ASC, id ASC")
    private List<DeliveryImportStop> stops = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }

    public Long getId() {
        return id;
    }

    public LocalDate getRouteDate() {
        return routeDate;
    }

    public void setRouteDate(LocalDate routeDate) {
        this.routeDate = routeDate;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getSourceFilename() {
        return sourceFilename;
    }

    public void setSourceFilename(String sourceFilename) {
        this.sourceFilename = sourceFilename;
    }

    public String getDeliveryPerson() {
        return deliveryPerson;
    }

    public void setDeliveryPerson(String deliveryPerson) {
        this.deliveryPerson = deliveryPerson;
    }

    public Integer getTotalStops() {
        return totalStops;
    }

    public void setTotalStops(Integer totalStops) {
        this.totalStops = totalStops;
    }

    public Integer getLocatedStops() {
        return locatedStops;
    }

    public void setLocatedStops(Integer locatedStops) {
        this.locatedStops = locatedStops;
    }

    public Integer getMissingLocationStops() {
        return missingLocationStops;
    }

    public void setMissingLocationStops(Integer missingLocationStops) {
        this.missingLocationStops = missingLocationStops;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public List<DeliveryImportStop> getStops() {
        return stops;
    }

    public void addStop(DeliveryImportStop stop) {
        stops.add(stop);
        stop.setBatch(this);
    }
}
