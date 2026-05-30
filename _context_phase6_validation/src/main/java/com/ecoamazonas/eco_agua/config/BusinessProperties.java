package com.ecoamazonas.eco_agua.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "ecoagua.business")
public class BusinessProperties {

    private String profileCode = "aguaeco";
    private String name = "Eco del Amazonas";
    private String shortName = "Eco Agua";
    private String tagline = "Agua de mesa";
    private String type = "water_delivery";
    private String adminTitle = "Sistema integral";
    private String logo = "/img/logo3-transparente.png";
    private String adminLogo = "/img/logo-eco.png";
    private String whatsappNumber = "51980542101";
    private String location = "Reparto en Iquitos y alrededores";
    private String phone = "(065) 000000";
    private String footerRight = "Servicio de agua purificada a domicilio - Iquitos, Perú";
    private String topbarWhatsappLabel = "Pedidos por WhatsApp";
    private String heroPill = "Agua purificada, segura y confiable";
    private String heroTitle = "Refresca tu vida con agua de mesa Eco del Amazonas";
    private String heroSubtitle = "Llevamos bidones y botellas de agua purificada hasta tu hogar u oficina, con reparto programado y atención personalizada.";
    private String heroBullet1 = "Proceso de purificación certificado.";
    private String heroBullet2 = "Entrega puntual en los horarios acordados.";
    private String heroBullet3 = "Atención por WhatsApp, fácil y rápido.";
    private String heroPrimaryCtaLabel = "Pedir ahora por WhatsApp";
    private String heroSecondaryCtaLabel = "Ver catálogo de productos";
    private String heroStat1 = "+500 familias atendidas";
    private String heroStat2 = "Reparto diario en Iquitos";
    private String heroStat3 = "Calidad y confianza";
    private String heroCardTitle = "Agua de mesa Eco del Amazonas";
    private String heroCardSubtitle = "Bidones, botellas y planes para empresas";
    private String heroBadgeLabel = "Servicio destacado";
    private String finalCtaButtonLabel = "Pedir recarga ahora";
    private String finalCtaSchedule = "Atención de lunes a sábado, de 8:00 a.m. a 8:00 p.m.";
    private String catalogWhatsappIntro = "Hola, deseo hacer un pedido desde el catálogo";
    private String featuredCategoryName = "Agua de mesa";

    private String productLabel = "Producto";
    private String productPluralLabel = "Productos";
    private String customerLabel = "Cliente";
    private String customerPluralLabel = "Clientes";
    private String supplierLabel = "Proveedor";
    private String supplierPluralLabel = "Proveedores";
    private String supplyLabel = "Insumo";
    private String supplyPluralLabel = "Insumos";
    private String deliveryPersonLabel = "Repartidor";
    private String deliveryLabel = "Reparto";
    private String containerLabel = "Envase";
    private String containerPluralLabel = "Envases";
    private String productionLabel = "Producción";
    private String reorderLabel = "Reposición";
    private String profilePriceHelpText = "Para productos con precio por perfil, este precio se aplicará automáticamente.";

    public String getProfileCode() {
        return profileCode;
    }

    public void setProfileCode(String profileCode) {
        this.profileCode = profileCode;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getShortName() {
        return shortName;
    }

    public void setShortName(String shortName) {
        this.shortName = shortName;
    }

    public String getTagline() {
        return tagline;
    }

    public void setTagline(String tagline) {
        this.tagline = tagline;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getAdminTitle() {
        return adminTitle;
    }

    public void setAdminTitle(String adminTitle) {
        this.adminTitle = adminTitle;
    }

    public String getLogo() {
        return logo;
    }

    public void setLogo(String logo) {
        this.logo = logo;
    }

    public String getAdminLogo() {
        return adminLogo;
    }

    public void setAdminLogo(String adminLogo) {
        this.adminLogo = adminLogo;
    }

    public String getWhatsappNumber() {
        return whatsappNumber;
    }

    public void setWhatsappNumber(String whatsappNumber) {
        this.whatsappNumber = whatsappNumber;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getFooterRight() {
        return footerRight;
    }

    public void setFooterRight(String footerRight) {
        this.footerRight = footerRight;
    }

    public String getTopbarWhatsappLabel() {
        return topbarWhatsappLabel;
    }

    public void setTopbarWhatsappLabel(String topbarWhatsappLabel) {
        this.topbarWhatsappLabel = topbarWhatsappLabel;
    }

    public String getHeroPill() {
        return heroPill;
    }

    public void setHeroPill(String heroPill) {
        this.heroPill = heroPill;
    }

    public String getHeroTitle() {
        return heroTitle;
    }

    public void setHeroTitle(String heroTitle) {
        this.heroTitle = heroTitle;
    }

    public String getHeroSubtitle() {
        return heroSubtitle;
    }

    public void setHeroSubtitle(String heroSubtitle) {
        this.heroSubtitle = heroSubtitle;
    }

    public String getHeroBullet1() {
        return heroBullet1;
    }

    public void setHeroBullet1(String heroBullet1) {
        this.heroBullet1 = heroBullet1;
    }

    public String getHeroBullet2() {
        return heroBullet2;
    }

    public void setHeroBullet2(String heroBullet2) {
        this.heroBullet2 = heroBullet2;
    }

    public String getHeroBullet3() {
        return heroBullet3;
    }

    public void setHeroBullet3(String heroBullet3) {
        this.heroBullet3 = heroBullet3;
    }

    public String getHeroPrimaryCtaLabel() {
        return heroPrimaryCtaLabel;
    }

    public void setHeroPrimaryCtaLabel(String heroPrimaryCtaLabel) {
        this.heroPrimaryCtaLabel = heroPrimaryCtaLabel;
    }

    public String getHeroSecondaryCtaLabel() {
        return heroSecondaryCtaLabel;
    }

    public void setHeroSecondaryCtaLabel(String heroSecondaryCtaLabel) {
        this.heroSecondaryCtaLabel = heroSecondaryCtaLabel;
    }

    public String getHeroStat1() {
        return heroStat1;
    }

    public void setHeroStat1(String heroStat1) {
        this.heroStat1 = heroStat1;
    }

    public String getHeroStat2() {
        return heroStat2;
    }

    public void setHeroStat2(String heroStat2) {
        this.heroStat2 = heroStat2;
    }

    public String getHeroStat3() {
        return heroStat3;
    }

    public void setHeroStat3(String heroStat3) {
        this.heroStat3 = heroStat3;
    }

    public String getHeroCardTitle() {
        return heroCardTitle;
    }

    public void setHeroCardTitle(String heroCardTitle) {
        this.heroCardTitle = heroCardTitle;
    }

    public String getHeroCardSubtitle() {
        return heroCardSubtitle;
    }

    public void setHeroCardSubtitle(String heroCardSubtitle) {
        this.heroCardSubtitle = heroCardSubtitle;
    }

    public String getHeroBadgeLabel() {
        return heroBadgeLabel;
    }

    public void setHeroBadgeLabel(String heroBadgeLabel) {
        this.heroBadgeLabel = heroBadgeLabel;
    }

    public String getFinalCtaButtonLabel() {
        return finalCtaButtonLabel;
    }

    public void setFinalCtaButtonLabel(String finalCtaButtonLabel) {
        this.finalCtaButtonLabel = finalCtaButtonLabel;
    }

    public String getFinalCtaSchedule() {
        return finalCtaSchedule;
    }

    public void setFinalCtaSchedule(String finalCtaSchedule) {
        this.finalCtaSchedule = finalCtaSchedule;
    }

    public String getCatalogWhatsappIntro() {
        return catalogWhatsappIntro;
    }

    public void setCatalogWhatsappIntro(String catalogWhatsappIntro) {
        this.catalogWhatsappIntro = catalogWhatsappIntro;
    }

    public String getFeaturedCategoryName() {
        return featuredCategoryName;
    }

    public void setFeaturedCategoryName(String featuredCategoryName) {
        this.featuredCategoryName = featuredCategoryName;
    }
    public String getProductLabel() {
        return productLabel;
    }

    public void setProductLabel(String productLabel) {
        this.productLabel = productLabel;
    }

    public String getProductPluralLabel() {
        return productPluralLabel;
    }

    public void setProductPluralLabel(String productPluralLabel) {
        this.productPluralLabel = productPluralLabel;
    }

    public String getCustomerLabel() {
        return customerLabel;
    }

    public void setCustomerLabel(String customerLabel) {
        this.customerLabel = customerLabel;
    }

    public String getCustomerPluralLabel() {
        return customerPluralLabel;
    }

    public void setCustomerPluralLabel(String customerPluralLabel) {
        this.customerPluralLabel = customerPluralLabel;
    }

    public String getSupplierLabel() {
        return supplierLabel;
    }

    public void setSupplierLabel(String supplierLabel) {
        this.supplierLabel = supplierLabel;
    }

    public String getSupplierPluralLabel() {
        return supplierPluralLabel;
    }

    public void setSupplierPluralLabel(String supplierPluralLabel) {
        this.supplierPluralLabel = supplierPluralLabel;
    }

    public String getSupplyLabel() {
        return supplyLabel;
    }

    public void setSupplyLabel(String supplyLabel) {
        this.supplyLabel = supplyLabel;
    }

    public String getSupplyPluralLabel() {
        return supplyPluralLabel;
    }

    public void setSupplyPluralLabel(String supplyPluralLabel) {
        this.supplyPluralLabel = supplyPluralLabel;
    }

    public String getDeliveryPersonLabel() {
        return deliveryPersonLabel;
    }

    public void setDeliveryPersonLabel(String deliveryPersonLabel) {
        this.deliveryPersonLabel = deliveryPersonLabel;
    }

    public String getDeliveryLabel() {
        return deliveryLabel;
    }

    public void setDeliveryLabel(String deliveryLabel) {
        this.deliveryLabel = deliveryLabel;
    }

    public String getContainerLabel() {
        return containerLabel;
    }

    public void setContainerLabel(String containerLabel) {
        this.containerLabel = containerLabel;
    }

    public String getContainerPluralLabel() {
        return containerPluralLabel;
    }

    public void setContainerPluralLabel(String containerPluralLabel) {
        this.containerPluralLabel = containerPluralLabel;
    }

    public String getProductionLabel() {
        return productionLabel;
    }

    public void setProductionLabel(String productionLabel) {
        this.productionLabel = productionLabel;
    }

    public String getReorderLabel() {
        return reorderLabel;
    }

    public void setReorderLabel(String reorderLabel) {
        this.reorderLabel = reorderLabel;
    }

    public String getProfilePriceHelpText() {
        return profilePriceHelpText;
    }

    public void setProfilePriceHelpText(String profilePriceHelpText) {
        this.profilePriceHelpText = profilePriceHelpText;
    }
}
