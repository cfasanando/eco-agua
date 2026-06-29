package com.ecoamazonas.eco_agua.platform.control.modules;

import com.ecoamazonas.eco_agua.platform.PlatformModuleCatalog;
import com.ecoamazonas.eco_agua.platform.PlatformModuleCatalogRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 45)
@ConditionalOnProperty(name = "matrix26.control-center.enabled", havingValue = "true")
public class Matrix26ModuleActivationInitializer implements ApplicationRunner {

    private static final Logger LOGGER = LoggerFactory.getLogger(Matrix26ModuleActivationInitializer.class);

    private final JdbcTemplate jdbcTemplate;
    private final PlatformModuleCatalogRepository moduleRepository;

    public Matrix26ModuleActivationInitializer(
            JdbcTemplate jdbcTemplate,
            PlatformModuleCatalogRepository moduleRepository
    ) {
        this.jdbcTemplate = jdbcTemplate;
        this.moduleRepository = moduleRepository;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        createEventTable();
        seedExtendedCatalog();
        LOGGER.info("Matrix26 module activation catalog and event table are ready.");
    }

    private void createEventTable() {
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS matrix26_instance_module_activation_event (
                    id BIGINT NOT NULL AUTO_INCREMENT,
                    instance_id BIGINT NULL,
                    module_key VARCHAR(80) NOT NULL,
                    action VARCHAR(40) NOT NULL,
                    before_enabled BIT NOT NULL DEFAULT 0,
                    after_enabled BIT NOT NULL DEFAULT 0,
                    actor_username VARCHAR(120) NOT NULL,
                    source VARCHAR(80) NOT NULL,
                    notes VARCHAR(500) NULL,
                    created_at DATETIME(6) NOT NULL,
                    PRIMARY KEY (id),
                    KEY idx_matrix26_module_event_instance_created (instance_id, created_at),
                    KEY idx_matrix26_module_event_module_created (module_key, created_at),
                    KEY idx_matrix26_module_event_created (created_at),
                    CONSTRAINT fk_matrix26_module_event_instance FOREIGN KEY (instance_id) REFERENCES platform_business_client (id)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
                """);
    }

    private void seedExtendedCatalog() {
        for (ModuleSeed seed : seeds()) {
            PlatformModuleCatalog module = moduleRepository.findByModuleKey(seed.key()).orElseGet(PlatformModuleCatalog::new);
            module.setModuleKey(seed.key());
            module.setName(seed.name());
            module.setArea(seed.area());
            module.setDescription(seed.description());
            module.setDefaultEnabled(seed.defaultEnabled());
            module.setConfigurable(true);
            module.setActive(true);
            module.setDisplayOrder(seed.displayOrder());
            moduleRepository.save(module);
        }
    }

    private List<ModuleSeed> seeds() {
        return List.of(
                new ModuleSeed("core", "Núcleo empresarial", "00 Core", "Security, settings and base business features.", true, 10),
                new ModuleSeed("sales", "Ventas y clientes", "10 Comercial", "Orders, customers, quotes and receivables.", true, 20),
                new ModuleSeed("delivery", "Delivery y reparto", "10 Comercial", "Delivery zones, routes and daily delivery tracking.", false, 30),
                new ModuleSeed("reorder", "Agenda de reposición", "10 Comercial", "Recurring customer reorder and follow-up agenda.", false, 40),

                new ModuleSeed("inventory", "Inventario y catálogo", "20 Operaciones", "Products, categories and warehouse stock.", true, 100),
                new ModuleSeed("warehouse", "Almacén", "20 Operaciones", "Warehouse views and stock movements.", true, 110),
                new ModuleSeed("supplies", "Insumos", "20 Operaciones", "Supplies inventory and movement control.", false, 120),
                new ModuleSeed("containers", "Envases retornables", "20 Operaciones", "Returnable bottles and container balances.", false, 130),
                new ModuleSeed("production", "Producción", "20 Operaciones", "Production orders, recipes, quality and traceability.", false, 140),

                new ModuleSeed("finance", "Finanzas", "30 Finanzas", "Cashflow, expenses, pricing and profitability control.", true, 200),
                new ModuleSeed("accounting", "Contabilidad", "30 Finanzas", "Sales registry, purchase registry and journal evidence.", true, 210),
                new ModuleSeed("cashflow", "Flujo de caja", "30 Finanzas", "Monthly cashflow, break-even and daily cash controls.", true, 220),
                new ModuleSeed("fixed_costs", "Costos fijos", "30 Finanzas", "Fixed cost planning and cost control.", false, 230),
                new ModuleSeed("break_even", "Punto de equilibrio", "30 Finanzas", "Break-even analysis and margin controls.", false, 240),
                new ModuleSeed("price_simulator", "Simulador de precios", "30 Finanzas", "Product pricing simulator and margin scenarios.", false, 250),

                new ModuleSeed("marketing", "Marketing", "40 Crecimiento", "Campaigns, promotions, ideas and image library.", true, 300),
                new ModuleSeed("public_catalog", "Catálogo público", "40 Crecimiento", "Public catalog, product pages and WhatsApp CTA.", true, 310),
                new ModuleSeed("blog", "Blog / contenidos", "40 Crecimiento", "Public blog, recipes, advice and content marketing.", false, 320),
                new ModuleSeed("testimonials", "Testimonios", "40 Crecimiento", "Testimonials and social proof blocks.", false, 330),
                new ModuleSeed("academy", "Academia", "40 Crecimiento", "Course catalog, assessments and certificates.", false, 340),

                new ModuleSeed("hr", "Recursos Humanos", "50 Gestión", "Employees, attendance, payroll and obligations.", true, 400),

                new ModuleSeed("restaurant", "Restaurante", "60 Vertical restaurante", "Restaurant dashboard, menu, tables and kitchen operations.", false, 500),
                new ModuleSeed("restaurant_cash", "Caja restaurante", "60 Vertical restaurante", "Restaurant cash sessions, receipts and daily close.", false, 510),
                new ModuleSeed("restaurant_qr", "Pedidos QR", "60 Vertical restaurante", "QR menu, external orders and approval workflow.", false, 520),
                new ModuleSeed("restaurant_recipes", "Recetas y costos", "60 Vertical restaurante", "Restaurant recipes, ingredient stock and costing.", false, 530),
                new ModuleSeed("restaurant_reservations", "Reservas", "60 Vertical restaurante", "Restaurant reservations and table requests.", false, 540),

                new ModuleSeed("matrix26_operations", "Matrix26 Operations", "90 Matrix26", "Runtime inventory, ports, dashboard and alert center.", false, 900),
                new ModuleSeed("matrix26_backups", "Matrix26 Backups", "90 Matrix26", "Backup jobs, schedules, policies, retention and verification.", false, 910),
                new ModuleSeed("matrix26_restore", "Matrix26 Restore", "90 Matrix26", "Clone restore and in-place restore management.", false, 920),
                new ModuleSeed("matrix26_lifecycle", "Matrix26 Lifecycle", "90 Matrix26", "Suspend, archive and decommission workflows.", false, 930),
                new ModuleSeed("matrix26_purge", "Matrix26 Purge", "90 Matrix26", "Controlled purge and archive destruction workflows.", false, 940),
                new ModuleSeed("matrix26_security", "Matrix26 Security", "90 Matrix26", "Roles, permissions and governance for the control center.", false, 950)
        );
    }

    private record ModuleSeed(
            String key,
            String name,
            String area,
            String description,
            boolean defaultEnabled,
            int displayOrder
    ) {
    }
}
