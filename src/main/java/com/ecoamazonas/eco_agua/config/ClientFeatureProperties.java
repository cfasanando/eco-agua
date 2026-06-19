package com.ecoamazonas.eco_agua.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "ecoagua.features")
public class ClientFeatureProperties {

    private boolean containers = true;
    private boolean delivery = true;
    private boolean production = true;
    private boolean reorder = true;
    private boolean marketing = true;
    private boolean blog = true;
    private boolean testimonials = true;
    private boolean publicCatalog = true;
    private boolean restaurant = false;
    private boolean supplies = true;
    private boolean fixedCosts = true;
    private boolean breakEven = true;
    private boolean priceSimulator = true;

    public boolean isContainers() {
        return containers;
    }

    public void setContainers(boolean containers) {
        this.containers = containers;
    }

    public boolean isDelivery() {
        return delivery;
    }

    public void setDelivery(boolean delivery) {
        this.delivery = delivery;
    }

    public boolean isProduction() {
        return production;
    }

    public void setProduction(boolean production) {
        this.production = production;
    }

    public boolean isReorder() {
        return reorder;
    }

    public void setReorder(boolean reorder) {
        this.reorder = reorder;
    }

    public boolean isMarketing() {
        return marketing;
    }

    public void setMarketing(boolean marketing) {
        this.marketing = marketing;
    }

    public boolean isBlog() {
        return blog;
    }

    public void setBlog(boolean blog) {
        this.blog = blog;
    }

    public boolean isTestimonials() {
        return testimonials;
    }

    public void setTestimonials(boolean testimonials) {
        this.testimonials = testimonials;
    }

    public boolean isPublicCatalog() {
        return publicCatalog;
    }

    public void setPublicCatalog(boolean publicCatalog) {
        this.publicCatalog = publicCatalog;
    }

    public boolean isRestaurant() {
        return restaurant;
    }

    public void setRestaurant(boolean restaurant) {
        this.restaurant = restaurant;
    }

    public boolean isSupplies() {
        return supplies;
    }

    public void setSupplies(boolean supplies) {
        this.supplies = supplies;
    }

    public boolean isFixedCosts() {
        return fixedCosts;
    }

    public void setFixedCosts(boolean fixedCosts) {
        this.fixedCosts = fixedCosts;
    }

    public boolean isBreakEven() {
        return breakEven;
    }

    public void setBreakEven(boolean breakEven) {
        this.breakEven = breakEven;
    }

    public boolean isPriceSimulator() {
        return priceSimulator;
    }

    public void setPriceSimulator(boolean priceSimulator) {
        this.priceSimulator = priceSimulator;
    }

    public boolean isMarketingSectionEnabled() {
        return marketing || blog || testimonials;
    }
}
