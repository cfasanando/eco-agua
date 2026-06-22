package com.ecoamazonas.eco_agua.security;

import com.ecoamazonas.eco_agua.config.SystemModuleAccessFilter;
import com.ecoamazonas.eco_agua.config.ClientFeatureProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.intercept.AuthorizationFilter;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;

import java.io.IOException;
import java.util.LinkedHashSet;
import java.util.Set;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private static final int REMEMBER_ME_VALIDITY_SECONDS = 60 * 60 * 24 * 30;

    private static final String ROLE_SUPER_ADMIN = "ROLE_SUPER_ADMIN";
    private static final String ROLE_OWNER = "ROLE_OWNER";
    private static final String ROLE_MANAGEMENT = "ROLE_MANAGEMENT";
    private static final String ROLE_MARKETING = "ROLE_MARKETING";
    private static final String ROLE_SALES = "ROLE_SALES";
    private static final String ROLE_FINANCE = "ROLE_FINANCE";
    private static final String ROLE_LOGISTICS = "ROLE_LOGISTICS";
    private static final String ROLE_PRODUCTION = "ROLE_PRODUCTION";
    private static final String ROLE_HR = "ROLE_HR";
    private static final String ROLE_READONLY = "ROLE_READONLY";
    private static final String ROLE_RESTAURANT_WAITER = "ROLE_RESTAURANT_WAITER";
    private static final String ROLE_RESTAURANT_KITCHEN = "ROLE_RESTAURANT_KITCHEN";
    private static final String ROLE_RESTAURANT_CASHIER = "ROLE_RESTAURANT_CASHIER";

    private static final String LEGACY_ADMIN_PRINC = "ADMIN_PRINC";
    private static final String LEGACY_ADMIN = "ADMIN";
    private static final String LEGACY_ADMIN_CONT = "ADMIN_CONT";
    private static final String LEGACY_ADMIN_MKT = "ADMIN_MKT";
    private static final String LEGACY_ADMIN_RRHH = "ADMIN_RRHH";
    private static final String LEGACY_SUPERVISOR = "SUPERVISOR";
    private static final String LEGACY_OPERARIO = "OPERARIO";

    private static final String[] ADMINISTRATORS = authorities(
            ROLE_SUPER_ADMIN, ROLE_OWNER,
            LEGACY_ADMIN_PRINC,
            LEGACY_ADMIN
    );

    private static final String[] DASHBOARD_VIEW = authorities(
            ROLE_SUPER_ADMIN, ROLE_OWNER, ROLE_MANAGEMENT, ROLE_MARKETING, ROLE_SALES, ROLE_FINANCE,
            ROLE_LOGISTICS, ROLE_PRODUCTION, ROLE_HR, ROLE_READONLY,
            ROLE_RESTAURANT_WAITER, ROLE_RESTAURANT_KITCHEN, ROLE_RESTAURANT_CASHIER,
            LEGACY_ADMIN_PRINC, LEGACY_ADMIN, LEGACY_ADMIN_CONT, LEGACY_ADMIN_MKT,
            LEGACY_ADMIN_RRHH, LEGACY_SUPERVISOR, LEGACY_OPERARIO,
            "ver_dashboard"
    );

    private static final String[] REPORTS_VIEW = authorities(
            ROLE_OWNER, ROLE_MANAGEMENT, ROLE_FINANCE, ROLE_READONLY,
            LEGACY_ADMIN_PRINC, LEGACY_ADMIN, LEGACY_ADMIN_CONT,
            "ver_reportes", "ver_finanzas"
    );

    private static final String[] CLIENTS_VIEW = authorities(
            ROLE_OWNER, ROLE_MANAGEMENT, ROLE_MARKETING, ROLE_SALES, ROLE_READONLY,
            LEGACY_ADMIN_PRINC, LEGACY_ADMIN, LEGACY_ADMIN_MKT, LEGACY_SUPERVISOR, LEGACY_OPERARIO,
            "ver_clientes", "administra_clientes"
    );

    private static final String[] CLIENTS_WRITE = authorities(
            ROLE_OWNER, ROLE_SALES,
            LEGACY_ADMIN_PRINC, LEGACY_ADMIN,
            "crear_clientes", "editar_clientes", "borrar_clientes", "administra_clientes"
    );

    private static final String[] SALES_VIEW = authorities(
            ROLE_OWNER, ROLE_MANAGEMENT, ROLE_MARKETING, ROLE_SALES, ROLE_FINANCE, ROLE_LOGISTICS, ROLE_READONLY,
            LEGACY_ADMIN_PRINC, LEGACY_ADMIN, LEGACY_ADMIN_CONT, LEGACY_ADMIN_MKT, LEGACY_SUPERVISOR, LEGACY_OPERARIO,
            "ver_ventas", "ver_ingresos"
    );

    private static final String[] SALES_WRITE = authorities(
            ROLE_OWNER, ROLE_SALES,
            LEGACY_ADMIN_PRINC, LEGACY_ADMIN, LEGACY_OPERARIO,
            "crear_ventas", "editar_ventas", "borrar_ventas"
    );

    private static final String[] RECEIVABLES_WRITE = authorities(
            ROLE_OWNER, ROLE_SALES, ROLE_FINANCE,
            LEGACY_ADMIN_PRINC, LEGACY_ADMIN, LEGACY_ADMIN_CONT,
            "gestionar_cuentas_cobrar"
    );

    private static final String[] INCOME_VIEW = authorities(
            ROLE_OWNER, ROLE_MANAGEMENT, ROLE_FINANCE, ROLE_READONLY,
            LEGACY_ADMIN_PRINC, LEGACY_ADMIN, LEGACY_ADMIN_CONT,
            "ver_ingresos", "ver_ventas"
    );

    private static final String[] INCOME_WRITE = authorities(
            ROLE_OWNER, ROLE_FINANCE,
            LEGACY_ADMIN_PRINC, LEGACY_ADMIN, LEGACY_ADMIN_CONT,
            "crear_ingresos", "editar_ingresos", "borrar_ingresos"
    );

    private static final String[] EXPENSE_VIEW = authorities(
            ROLE_OWNER, ROLE_MANAGEMENT, ROLE_FINANCE, ROLE_READONLY,
            LEGACY_ADMIN_PRINC, LEGACY_ADMIN, LEGACY_ADMIN_CONT,
            "ver_egresos"
    );

    private static final String[] EXPENSE_WRITE = authorities(
            ROLE_OWNER, ROLE_FINANCE,
            LEGACY_ADMIN_PRINC, LEGACY_ADMIN, LEGACY_ADMIN_CONT,
            "crear_egresos", "editar_egresos", "borrar_egresos"
    );

    private static final String[] FINANCE_VIEW = authorities(
            ROLE_OWNER, ROLE_MANAGEMENT, ROLE_FINANCE, ROLE_READONLY,
            LEGACY_ADMIN_PRINC, LEGACY_ADMIN, LEGACY_ADMIN_CONT,
            "ver_cashflow", "ver_contabilidad", "ver_punto_equilibrio", "ver_finanzas"
    );

    private static final String[] FINANCE_WRITE = authorities(
            ROLE_OWNER, ROLE_FINANCE,
            LEGACY_ADMIN_PRINC, LEGACY_ADMIN, LEGACY_ADMIN_CONT
    );

    private static final String[] PRODUCTS_VIEW = authorities(
            ROLE_OWNER, ROLE_MANAGEMENT, ROLE_MARKETING, ROLE_SALES, ROLE_FINANCE, ROLE_LOGISTICS,
            ROLE_PRODUCTION, ROLE_READONLY,
            LEGACY_ADMIN_PRINC, LEGACY_ADMIN, LEGACY_ADMIN_MKT, LEGACY_SUPERVISOR, LEGACY_OPERARIO,
            "ver_productos", "administra_productos"
    );

    private static final String[] PROFITABILITY_VIEW = authorities(
            ROLE_OWNER, ROLE_MANAGEMENT, ROLE_FINANCE, ROLE_READONLY,
            LEGACY_ADMIN_PRINC, LEGACY_ADMIN, LEGACY_ADMIN_CONT,
            "ver_reportes", "ver_finanzas"
    );

    private static final String[] PRODUCTS_WRITE = authorities(
            ROLE_OWNER, ROLE_LOGISTICS,
            LEGACY_ADMIN_PRINC, LEGACY_ADMIN, LEGACY_SUPERVISOR,
            "administra_productos"
    );

    private static final String[] CATEGORIES_VIEW = authorities(
            ROLE_OWNER, ROLE_MANAGEMENT, ROLE_LOGISTICS, ROLE_READONLY,
            LEGACY_ADMIN_PRINC, LEGACY_ADMIN, LEGACY_SUPERVISOR,
            "ver_categorias", "administra_categorias"
    );

    private static final String[] CATEGORIES_WRITE = authorities(
            ROLE_OWNER, ROLE_LOGISTICS,
            LEGACY_ADMIN_PRINC, LEGACY_ADMIN,
            "administra_categorias"
    );

    private static final String[] STOCK_VIEW = authorities(
            ROLE_OWNER, ROLE_MANAGEMENT, ROLE_LOGISTICS, ROLE_PRODUCTION, ROLE_READONLY,
            LEGACY_ADMIN_PRINC, LEGACY_ADMIN, LEGACY_SUPERVISOR, LEGACY_OPERARIO,
            "ver_stock_productos", "control_stock_productos", "ver_stock_insumos", "control_stock_insumos"
    );

    private static final String[] STOCK_WRITE = authorities(
            ROLE_OWNER, ROLE_LOGISTICS, ROLE_PRODUCTION,
            LEGACY_ADMIN_PRINC, LEGACY_ADMIN, LEGACY_SUPERVISOR, LEGACY_OPERARIO,
            "control_stock_productos", "control_stock_insumos"
    );

    private static final String[] SUPPLIES_VIEW = authorities(
            ROLE_OWNER, ROLE_LOGISTICS, ROLE_PRODUCTION, ROLE_READONLY,
            LEGACY_ADMIN_PRINC, LEGACY_ADMIN, LEGACY_SUPERVISOR,
            "ver_insumos", "administra_insumos", "ver_stock_insumos", "control_stock_insumos"
    );

    private static final String[] SUPPLIES_WRITE = authorities(
            ROLE_OWNER, ROLE_LOGISTICS, ROLE_PRODUCTION,
            LEGACY_ADMIN_PRINC, LEGACY_ADMIN, LEGACY_SUPERVISOR,
            "administra_insumos", "control_stock_insumos"
    );

    private static final String[] SUPPLIERS_VIEW = authorities(
            ROLE_OWNER, ROLE_MANAGEMENT, ROLE_LOGISTICS, ROLE_READONLY,
            LEGACY_ADMIN_PRINC, LEGACY_ADMIN, LEGACY_SUPERVISOR,
            "ver_proveedores", "administra_proveedores"
    );

    private static final String[] SUPPLIERS_WRITE = authorities(
            ROLE_OWNER, ROLE_LOGISTICS,
            LEGACY_ADMIN_PRINC, LEGACY_ADMIN,
            "administra_proveedores"
    );

    private static final String[] DELIVERY_VIEW = authorities(
            ROLE_OWNER, ROLE_MANAGEMENT, ROLE_SALES, ROLE_LOGISTICS, ROLE_READONLY,
            LEGACY_ADMIN_PRINC, LEGACY_ADMIN, LEGACY_SUPERVISOR, LEGACY_OPERARIO,
            "ver_delivery", "administra_delivery"
    );

    private static final String[] DELIVERY_WRITE = authorities(
            ROLE_OWNER, ROLE_LOGISTICS,
            LEGACY_ADMIN_PRINC, LEGACY_ADMIN, LEGACY_SUPERVISOR, LEGACY_OPERARIO,
            "administra_delivery"
    );

    private static final String[] MARKETING_VIEW = authorities(
            ROLE_OWNER, ROLE_MANAGEMENT, ROLE_MARKETING, ROLE_READONLY,
            LEGACY_ADMIN_PRINC, LEGACY_ADMIN, LEGACY_ADMIN_MKT,
            "ver_marketing", "ver_promociones", "ver_blog", "ver_testimonios"
    );

    private static final String[] MARKETING_WRITE = authorities(
            ROLE_OWNER, ROLE_MARKETING,
            LEGACY_ADMIN_PRINC, LEGACY_ADMIN, LEGACY_ADMIN_MKT,
            "administra_marketing", "administra_promos", "administra_blog", "administra_testimonios"
    );

    private static final String[] ACADEMY_VIEW = authorities(
            ROLE_OWNER, ROLE_MANAGEMENT, ROLE_MARKETING, ROLE_READONLY,
            LEGACY_ADMIN_PRINC, LEGACY_ADMIN, LEGACY_ADMIN_MKT,
            "ver_academia", "administra_academia"
    );

    private static final String[] ACADEMY_WRITE = authorities(
            ROLE_OWNER, ROLE_MARKETING,
            LEGACY_ADMIN_PRINC, LEGACY_ADMIN, LEGACY_ADMIN_MKT,
            "administra_academia"
    );

    private static final String[] RESTAURANT_ADMIN = authorities(
            ROLE_OWNER, ROLE_MANAGEMENT,
            LEGACY_ADMIN_PRINC, LEGACY_ADMIN, LEGACY_SUPERVISOR,
            "administra_restaurante"
    );

    private static final String[] RESTAURANT_DASHBOARD_VIEW = authorities(
            ROLE_OWNER, ROLE_MANAGEMENT, ROLE_RESTAURANT_WAITER,
            LEGACY_ADMIN_PRINC, LEGACY_ADMIN, LEGACY_SUPERVISOR,
            "ver_restaurante", "administra_restaurante", "restaurante_mozo"
    );

    private static final String[] RESTAURANT_TABLES_VIEW = authorities(
            ROLE_OWNER, ROLE_MANAGEMENT, ROLE_RESTAURANT_WAITER,
            LEGACY_ADMIN_PRINC, LEGACY_ADMIN, LEGACY_SUPERVISOR,
            "ver_restaurante", "administra_restaurante", "restaurante_mozo"
    );

    private static final String[] RESTAURANT_TABLES_WRITE = authorities(
            ROLE_OWNER, ROLE_MANAGEMENT, ROLE_RESTAURANT_WAITER,
            LEGACY_ADMIN_PRINC, LEGACY_ADMIN, LEGACY_SUPERVISOR,
            "administra_restaurante", "restaurante_mozo"
    );


    private static final String[] RESTAURANT_MENU_MANAGE = authorities(
            ROLE_OWNER, ROLE_MANAGEMENT,
            LEGACY_ADMIN_PRINC, LEGACY_ADMIN, LEGACY_SUPERVISOR,
            "administra_restaurante"
    );

    private static final String[] RESTAURANT_ORDER_VIEW = authorities(
            ROLE_OWNER, ROLE_MANAGEMENT, ROLE_RESTAURANT_WAITER, ROLE_RESTAURANT_KITCHEN, ROLE_RESTAURANT_CASHIER,
            LEGACY_ADMIN_PRINC, LEGACY_ADMIN, LEGACY_SUPERVISOR,
            "ver_restaurante", "administra_restaurante", "restaurante_mozo", "restaurante_cocina", "restaurante_caja"
    );

    private static final String[] RESTAURANT_ORDER_WRITE = authorities(
            ROLE_OWNER, ROLE_MANAGEMENT, ROLE_RESTAURANT_WAITER,
            LEGACY_ADMIN_PRINC, LEGACY_ADMIN, LEGACY_SUPERVISOR,
            "administra_restaurante", "restaurante_mozo"
    );

    private static final String[] RESTAURANT_EXTERNAL_STATUS_WRITE = authorities(
            ROLE_OWNER, ROLE_MANAGEMENT, ROLE_RESTAURANT_WAITER, ROLE_RESTAURANT_KITCHEN,
            LEGACY_ADMIN_PRINC, LEGACY_ADMIN, LEGACY_SUPERVISOR,
            "administra_restaurante", "restaurante_mozo", "restaurante_cocina"
    );

    private static final String[] RESTAURANT_KITCHEN_VIEW = authorities(
            ROLE_OWNER, ROLE_MANAGEMENT, ROLE_RESTAURANT_WAITER, ROLE_RESTAURANT_KITCHEN,
            LEGACY_ADMIN_PRINC, LEGACY_ADMIN, LEGACY_SUPERVISOR,
            "ver_restaurante", "administra_restaurante", "restaurante_mozo", "restaurante_cocina"
    );

    private static final String[] RESTAURANT_KITCHEN_WRITE = authorities(
            ROLE_OWNER, ROLE_MANAGEMENT, ROLE_RESTAURANT_KITCHEN,
            LEGACY_ADMIN_PRINC, LEGACY_ADMIN, LEGACY_SUPERVISOR,
            "administra_restaurante", "restaurante_cocina"
    );

    private static final String[] RESTAURANT_CASH_VIEW = authorities(
            ROLE_OWNER, ROLE_MANAGEMENT, ROLE_FINANCE, ROLE_RESTAURANT_CASHIER,
            LEGACY_ADMIN_PRINC, LEGACY_ADMIN, LEGACY_ADMIN_CONT,
            "ver_restaurante", "administra_restaurante", "restaurante_caja"
    );

    private static final String[] RESTAURANT_CASH_WRITE = authorities(
            ROLE_OWNER, ROLE_MANAGEMENT, ROLE_FINANCE, ROLE_RESTAURANT_CASHIER,
            LEGACY_ADMIN_PRINC, LEGACY_ADMIN, LEGACY_ADMIN_CONT,
            "administra_restaurante", "restaurante_caja"
    );

    private static final String[] RESTAURANT_VIEW = authorities(
            ROLE_OWNER, ROLE_MANAGEMENT, ROLE_SALES, ROLE_LOGISTICS, ROLE_READONLY,
            ROLE_RESTAURANT_WAITER, ROLE_RESTAURANT_KITCHEN, ROLE_RESTAURANT_CASHIER,
            LEGACY_ADMIN_PRINC, LEGACY_ADMIN, LEGACY_SUPERVISOR, LEGACY_OPERARIO,
            "ver_restaurante", "administra_restaurante", "restaurante_mozo", "restaurante_cocina", "restaurante_caja"
    );

    private static final String[] RESTAURANT_WRITE = authorities(
            ROLE_OWNER, ROLE_SALES, ROLE_LOGISTICS, ROLE_RESTAURANT_WAITER, ROLE_RESTAURANT_KITCHEN, ROLE_RESTAURANT_CASHIER,
            LEGACY_ADMIN_PRINC, LEGACY_ADMIN, LEGACY_SUPERVISOR, LEGACY_OPERARIO,
            "administra_restaurante", "restaurante_mozo", "restaurante_cocina", "restaurante_caja"
    );

    private static final String[] PRODUCTION_VIEW = authorities(
            ROLE_OWNER, ROLE_MANAGEMENT, ROLE_PRODUCTION, ROLE_READONLY,
            LEGACY_ADMIN_PRINC, LEGACY_ADMIN,
            "ver_produccion", "administra_produccion"
    );

    private static final String[] PRODUCTION_WRITE = authorities(
            ROLE_OWNER, ROLE_PRODUCTION,
            LEGACY_ADMIN_PRINC, LEGACY_ADMIN,
            "administra_produccion"
    );

    private static final String[] CONTAINERS_VIEW = authorities(
            ROLE_OWNER, ROLE_MANAGEMENT, ROLE_LOGISTICS, ROLE_READONLY,
            LEGACY_ADMIN_PRINC, LEGACY_ADMIN, LEGACY_SUPERVISOR, LEGACY_OPERARIO,
            "ver_envases", "control_botellones"
    );

    private static final String[] CONTAINERS_WRITE = authorities(
            ROLE_OWNER, ROLE_LOGISTICS,
            LEGACY_ADMIN_PRINC, LEGACY_ADMIN, LEGACY_SUPERVISOR, LEGACY_OPERARIO,
            "control_botellones"
    );

    private static final String[] REORDER_VIEW = authorities(
            ROLE_OWNER, ROLE_MANAGEMENT, ROLE_SALES, ROLE_LOGISTICS, ROLE_READONLY,
            LEGACY_ADMIN_PRINC, LEGACY_ADMIN, LEGACY_SUPERVISOR, LEGACY_OPERARIO,
            "ver_recompras", "administra_recompras"
    );

    private static final String[] REORDER_WRITE = authorities(
            ROLE_OWNER, ROLE_SALES, ROLE_LOGISTICS,
            LEGACY_ADMIN_PRINC, LEGACY_ADMIN, LEGACY_OPERARIO,
            "administra_recompras"
    );

    private static final String[] HR_VIEW = authorities(
            ROLE_OWNER, ROLE_MANAGEMENT, ROLE_HR, ROLE_READONLY,
            LEGACY_ADMIN_PRINC, LEGACY_ADMIN, LEGACY_ADMIN_RRHH,
            "ver_personal", "administra_personal", "ver_pagos_personal"
    );

    private static final String[] HR_WRITE = authorities(
            ROLE_OWNER, ROLE_HR,
            LEGACY_ADMIN_PRINC, LEGACY_ADMIN, LEGACY_ADMIN_RRHH,
            "administra_personal", "administra_pagos_personal"
    );

    private static final String[] USERS_ADMIN = authorities(
            ROLE_OWNER, ROLE_HR,
            LEGACY_ADMIN_PRINC, LEGACY_ADMIN,
            "administra_usuarios"
    );

    private static final String[] ROLES_ADMIN = authorities(
            ROLE_OWNER,
            LEGACY_ADMIN_PRINC, LEGACY_ADMIN,
            "administra_roles"
    );

    private static final String[] PLATFORM_ADMIN = authorities(
            ROLE_SUPER_ADMIN, ROLE_OWNER,
            LEGACY_ADMIN_PRINC, LEGACY_ADMIN,
            "admin_config"
    );

    private static final String[] PLATFORM_MANAGER_VIEW = authorities(
            ROLE_SUPER_ADMIN, ROLE_OWNER, ROLE_MANAGEMENT,
            LEGACY_ADMIN_PRINC, LEGACY_ADMIN,
            "ver_plataforma", "administra_plataforma", "admin_config"
    );

    private static final String[] PLATFORM_MANAGER_WRITE = authorities(
            ROLE_SUPER_ADMIN, ROLE_OWNER,
            LEGACY_ADMIN_PRINC, LEGACY_ADMIN,
            "administra_plataforma", "admin_config"
    );

    private final DatabaseUserDetailsService userDetailsService;
    private final SystemModuleAccessFilter systemModuleAccessFilter;
    private final ClientFeatureProperties clientFeatureProperties;

    public SecurityConfig(DatabaseUserDetailsService userDetailsService,
                          SystemModuleAccessFilter systemModuleAccessFilter,
                          ClientFeatureProperties clientFeatureProperties) {
        this.userDetailsService = userDetailsService;
        this.systemModuleAccessFilter = systemModuleAccessFilter;
        this.clientFeatureProperties = clientFeatureProperties;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .addFilterBefore(systemModuleAccessFilter, AuthorizationFilter.class)
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/", "/portal", "/catalogo", "/catalogo/**", "/order/whatsapp", "/robots.txt", "/sitemap.xml").permitAll()
                .requestMatchers("/blog", "/blog/**").permitAll()
                .requestMatchers("/academy", "/academy/**").permitAll()
                .requestMatchers("/restaurant", "/restaurant/**").permitAll()
                .requestMatchers("/css/**", "/js/**", "/img/**", "/uploads/**", "/webjars/**").permitAll()
                .requestMatchers("/login", "/error").permitAll()
                .requestMatchers(HttpMethod.GET, "/logout").permitAll()
                .requestMatchers(HttpMethod.GET, "/password-reset/request", "/password-reset").permitAll()
                .requestMatchers(HttpMethod.POST, "/password-reset/request", "/password-reset").permitAll()

                .requestMatchers("/home").hasAnyAuthority(DASHBOARD_VIEW)
                .requestMatchers("/dashboard/widget-preferences/**").hasAnyAuthority(DASHBOARD_VIEW)
                .requestMatchers("/dashboard/business", "/dashboard/areas").hasAnyAuthority(REPORTS_VIEW)
                .requestMatchers("/dashboard/business-overview", "/dashboard/monthly-followup").hasAnyAuthority(REPORTS_VIEW)
                .requestMatchers("/dashboard/commercial-daily").hasAnyAuthority(SALES_VIEW)

                .requestMatchers(HttpMethod.POST, "/orders/**").hasAnyAuthority(SALES_WRITE)
                .requestMatchers("/orders/**").hasAnyAuthority(SALES_VIEW)
                .requestMatchers(HttpMethod.POST, "/income/sales", "/income/sales/**").hasAnyAuthority(SALES_WRITE)
                .requestMatchers("/income/sales", "/income/sales/**").hasAnyAuthority(SALES_VIEW)
                .requestMatchers(HttpMethod.POST, "/income/credit/**").hasAnyAuthority(RECEIVABLES_WRITE)
                .requestMatchers("/income/credit/**").hasAnyAuthority(SALES_VIEW)
                .requestMatchers(HttpMethod.POST, "/income/others/**").hasAnyAuthority(INCOME_WRITE)
                .requestMatchers("/income/others/**").hasAnyAuthority(INCOME_VIEW)
                .requestMatchers("/income/**").hasAnyAuthority(INCOME_VIEW)

                .requestMatchers(HttpMethod.POST, "/expenses/**").hasAnyAuthority(EXPENSE_WRITE)
                .requestMatchers("/expenses/**").hasAnyAuthority(EXPENSE_VIEW)

                .requestMatchers(HttpMethod.POST, "/accounting/**", "/cashflow/**", "/admin/price-simulator/**").hasAnyAuthority(FINANCE_WRITE)
                .requestMatchers("/accounting/**", "/cashflow/**", "/admin/price-simulator/**").hasAnyAuthority(FINANCE_VIEW)

                .requestMatchers(HttpMethod.POST, "/admin/clients/**", "/admin/client-profiles/**").hasAnyAuthority(CLIENTS_WRITE)
                .requestMatchers("/admin/clients/**", "/admin/client-profiles/**").hasAnyAuthority(CLIENTS_VIEW)

                .requestMatchers("/admin/products/profitability", "/admin/products/profitability/**",
                        "/admin/products/channel-profitability", "/admin/products/channel-profitability/**",
                        "/admin/products/price-simulator", "/admin/products/price-simulator/**")
                    .hasAnyAuthority(PROFITABILITY_VIEW)
                .requestMatchers(HttpMethod.POST, "/admin/products/**").hasAnyAuthority(PRODUCTS_WRITE)
                .requestMatchers("/admin/products/**").hasAnyAuthority(PRODUCTS_VIEW)
                .requestMatchers(HttpMethod.POST, "/admin/categories/**").hasAnyAuthority(CATEGORIES_WRITE)
                .requestMatchers("/admin/categories/**").hasAnyAuthority(CATEGORIES_VIEW)
                .requestMatchers(HttpMethod.POST, "/warehouse/products-stock/**").hasAnyAuthority(STOCK_WRITE)
                .requestMatchers("/warehouse/products-stock/**").hasAnyAuthority(STOCK_VIEW)
                .requestMatchers(HttpMethod.POST, "/warehouse/supplies-stock/**").hasAnyAuthority(STOCK_WRITE)
                .requestMatchers("/warehouse/supplies-stock/**").hasAnyAuthority(STOCK_VIEW)
                .requestMatchers(HttpMethod.POST, "/admin/supplies/**").hasAnyAuthority(SUPPLIES_WRITE)
                .requestMatchers("/admin/supplies/**").hasAnyAuthority(SUPPLIES_VIEW)
                .requestMatchers(HttpMethod.POST, "/admin/suppliers/**").hasAnyAuthority(SUPPLIERS_WRITE)
                .requestMatchers("/admin/suppliers/**").hasAnyAuthority(SUPPLIERS_VIEW)

                .requestMatchers(HttpMethod.POST, "/delivery/**", "/admin/delivery-zones/**").hasAnyAuthority(DELIVERY_WRITE)
                .requestMatchers("/delivery/**", "/admin/delivery-zones/**").hasAnyAuthority(DELIVERY_VIEW)
                .requestMatchers(HttpMethod.POST, "/containers/**").hasAnyAuthority(CONTAINERS_WRITE)
                .requestMatchers("/containers/**").hasAnyAuthority(CONTAINERS_VIEW)
                .requestMatchers(HttpMethod.POST, "/reorder-agenda/**").hasAnyAuthority(REORDER_WRITE)
                .requestMatchers("/reorder-agenda/**").hasAnyAuthority(REORDER_VIEW)

                .requestMatchers(HttpMethod.POST, "/production/**").hasAnyAuthority(PRODUCTION_WRITE)
                .requestMatchers("/production/**").hasAnyAuthority(PRODUCTION_VIEW)

                .requestMatchers("/my-courses/**").hasAnyAuthority(ACADEMY_VIEW)
                .requestMatchers(HttpMethod.POST, "/admin/academy/**").hasAnyAuthority(ACADEMY_WRITE)
                .requestMatchers("/admin/academy/**").hasAnyAuthority(ACADEMY_VIEW)

                .requestMatchers("/admin/restaurant/settings").hasAnyAuthority(RESTAURANT_ADMIN)
                .requestMatchers(HttpMethod.POST, "/admin/restaurant/external-orders/*/status").hasAnyAuthority(RESTAURANT_EXTERNAL_STATUS_WRITE)
                .requestMatchers(HttpMethod.POST, "/admin/restaurant/external-orders/*/cancel").hasAnyAuthority(RESTAURANT_ORDER_WRITE)
                .requestMatchers(HttpMethod.POST, "/admin/restaurant/external-orders").hasAnyAuthority(RESTAURANT_ORDER_WRITE)
                .requestMatchers(HttpMethod.GET, "/admin/restaurant/external-orders/new").hasAnyAuthority(RESTAURANT_ORDER_WRITE)
                .requestMatchers(HttpMethod.GET, "/admin/restaurant/external-orders", "/admin/restaurant/external-orders/**").hasAnyAuthority(RESTAURANT_ORDER_VIEW)
                .requestMatchers(HttpMethod.POST, "/admin/restaurant/reservations/**").hasAnyAuthority(RESTAURANT_TABLES_WRITE)
                .requestMatchers(HttpMethod.GET, "/admin/restaurant/reservations", "/admin/restaurant/reservations/**").hasAnyAuthority(RESTAURANT_TABLES_VIEW)
                .requestMatchers("/admin/restaurant/ingredients", "/admin/restaurant/ingredients/**").hasAnyAuthority(RESTAURANT_MENU_MANAGE)
                .requestMatchers("/admin/restaurant/menu-items", "/admin/restaurant/menu-items/**").hasAnyAuthority(RESTAURANT_MENU_MANAGE)
                .requestMatchers(HttpMethod.GET, "/admin/restaurant/qr-orders", "/admin/restaurant/qr-orders/**").hasAnyAuthority(RESTAURANT_DASHBOARD_VIEW)
                .requestMatchers(HttpMethod.POST, "/admin/restaurant/qr-orders/**").hasAnyAuthority(RESTAURANT_ORDER_WRITE)
                .requestMatchers(HttpMethod.POST, "/admin/restaurant/cash-sessions/**").hasAnyAuthority(RESTAURANT_CASH_WRITE)
                .requestMatchers(HttpMethod.GET, "/admin/restaurant/cash-sessions", "/admin/restaurant/cash-sessions/**", "/admin/restaurant/reports", "/admin/restaurant/reports/**").hasAnyAuthority(RESTAURANT_CASH_VIEW)
                .requestMatchers(HttpMethod.POST, "/admin/restaurant/orders/*/pay").hasAnyAuthority(RESTAURANT_CASH_WRITE)
                .requestMatchers(HttpMethod.POST, "/admin/restaurant/orders/*/status").hasAnyAuthority(RESTAURANT_KITCHEN_WRITE)
                .requestMatchers(HttpMethod.POST, "/admin/restaurant/orders/**").hasAnyAuthority(RESTAURANT_ORDER_WRITE)
                .requestMatchers(HttpMethod.POST, "/admin/restaurant/tables/**").hasAnyAuthority(RESTAURANT_TABLES_WRITE)
                .requestMatchers(HttpMethod.GET, "/admin/restaurant/cash", "/admin/restaurant/reports/daily").hasAnyAuthority(RESTAURANT_CASH_VIEW)
                .requestMatchers(HttpMethod.GET, "/admin/restaurant/kitchen").hasAnyAuthority(RESTAURANT_KITCHEN_VIEW)
                .requestMatchers(HttpMethod.GET, "/admin/restaurant/orders/new").hasAnyAuthority(RESTAURANT_ORDER_WRITE)
                .requestMatchers(HttpMethod.GET, "/admin/restaurant/orders/**").hasAnyAuthority(RESTAURANT_ORDER_VIEW)
                .requestMatchers(HttpMethod.GET, "/admin/restaurant/tables/**").hasAnyAuthority(RESTAURANT_TABLES_VIEW)
                .requestMatchers(HttpMethod.GET, "/admin/restaurant", "/admin/restaurant/dashboard").hasAnyAuthority(RESTAURANT_DASHBOARD_VIEW)
                .requestMatchers(HttpMethod.POST, "/admin/restaurant/**").hasAnyAuthority(RESTAURANT_WRITE)
                .requestMatchers("/admin/restaurant/**").hasAnyAuthority(RESTAURANT_VIEW)

                .requestMatchers(HttpMethod.POST, "/admin/promotions/**", "/marketing/admin/**", "/admin/blog/**").hasAnyAuthority(MARKETING_WRITE)
                .requestMatchers("/admin/promotions/**", "/marketing/admin/**", "/admin/blog/**").hasAnyAuthority(MARKETING_VIEW)

                .requestMatchers(HttpMethod.POST, "/admin/personnel", "/admin/personnel/**", "/admin/job-positions/**").hasAnyAuthority(HR_WRITE)
                .requestMatchers("/admin/personnel", "/admin/personnel/**", "/admin/job-positions/**").hasAnyAuthority(HR_VIEW)
                .requestMatchers("/admin/users/**").hasAnyAuthority(USERS_ADMIN)
                .requestMatchers("/admin/roles-permissions/**").hasAnyAuthority(ROLES_ADMIN)
                .requestMatchers(HttpMethod.POST, "/admin/platform/**").hasAnyAuthority(PLATFORM_MANAGER_WRITE)
                .requestMatchers("/admin/platform/**").hasAnyAuthority(PLATFORM_MANAGER_VIEW)
                .requestMatchers("/admin/system-modules/**", "/admin/dashboard-widgets/**").hasAnyAuthority(PLATFORM_ADMIN)
                .requestMatchers("/admin/platform-settings/**").hasAnyAuthority(PLATFORM_ADMIN)

                .requestMatchers("/admin/**").hasAnyAuthority(ADMINISTRATORS)
                .anyRequest().authenticated()
            )
            .formLogin(form -> form
                .loginPage("/login")
                .loginProcessingUrl("/login")
                .successHandler(this::handleLoginSuccess)
                .failureUrl("/login?error=true")
                .permitAll()
            )
            .rememberMe(remember -> remember
                .rememberMeParameter("remember-me")
                .rememberMeCookieName("remember-me")
                .tokenValiditySeconds(REMEMBER_ME_VALIDITY_SECONDS)
                .key("eco-agua-remember-me-v1")
                .userDetailsService(userDetailsService)
            )
            .logout(logout -> logout
                .logoutRequestMatcher(PathPatternRequestMatcher.pathPattern(HttpMethod.POST, "/logout"))
                .logoutSuccessUrl("/")
                .invalidateHttpSession(true)
                .clearAuthentication(true)
                .deleteCookies("JSESSIONID", "remember-me")
                .permitAll()
            )
            .userDetailsService(userDetailsService);

        return http.build();
    }

    private void handleLoginSuccess(jakarta.servlet.http.HttpServletRequest request,
                                    jakarta.servlet.http.HttpServletResponse response,
                                    Authentication authentication) throws IOException {
        response.sendRedirect(loginSuccessUrl(authentication));
    }

    private String loginSuccessUrl(Authentication authentication) {
        Set<String> authorities = new LinkedHashSet<>();
        authentication.getAuthorities().forEach(authority -> authorities.add(authority.getAuthority()));

        if (clientFeatureProperties.isRestaurant()) {
            if (hasAnyAuthority(authorities, RESTAURANT_ADMIN)) {
                return "/admin/restaurant/dashboard";
            }
            if (hasAnyAuthority(authorities, ROLE_RESTAURANT_KITCHEN, "restaurante_cocina")) {
                return "/admin/restaurant/kitchen";
            }
            if (hasAnyAuthority(authorities, ROLE_RESTAURANT_CASHIER, "restaurante_caja")) {
                return "/admin/restaurant/cash";
            }
            if (hasAnyAuthority(authorities, ROLE_RESTAURANT_WAITER, "restaurante_mozo")) {
                return "/admin/restaurant/dashboard";
            }
        }

        if (hasAnyAuthority(authorities, ROLE_RESTAURANT_KITCHEN, "restaurante_cocina")) {
            return "/admin/restaurant/kitchen";
        }
        if (hasAnyAuthority(authorities, ROLE_RESTAURANT_CASHIER, "restaurante_caja")) {
            return "/admin/restaurant/cash";
        }
        if (hasAnyAuthority(authorities, ROLE_RESTAURANT_WAITER, "restaurante_mozo")) {
            return "/admin/restaurant/dashboard";
        }
        return "/home";
    }

    private boolean hasAnyAuthority(Set<String> userAuthorities, String... allowedAuthorities) {
        for (String authority : allowedAuthorities) {
            if (userAuthorities.contains(authority)) {
                return true;
            }
        }
        return false;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    private static String[] authorities(String... values) {
        return values;
    }
}
