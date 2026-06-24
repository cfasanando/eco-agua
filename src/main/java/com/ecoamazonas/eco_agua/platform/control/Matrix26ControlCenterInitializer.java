package com.ecoamazonas.eco_agua.platform.control;

import com.ecoamazonas.eco_agua.config.PlatformSettingService;
import com.ecoamazonas.eco_agua.platform.PlatformBusinessClient;
import com.ecoamazonas.eco_agua.platform.PlatformBusinessClientRepository;
import com.ecoamazonas.eco_agua.platform.PlatformClientModule;
import com.ecoamazonas.eco_agua.platform.PlatformClientModuleRepository;
import com.ecoamazonas.eco_agua.platform.PlatformModuleCatalog;
import com.ecoamazonas.eco_agua.platform.PlatformModuleCatalogRepository;
import com.ecoamazonas.eco_agua.user.Role;
import com.ecoamazonas.eco_agua.user.RoleRepository;
import com.ecoamazonas.eco_agua.user.UserAccount;
import com.ecoamazonas.eco_agua.user.UserAccountRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
@ConditionalOnProperty(name = "matrix26.control-center.enabled", havingValue = "true")
public class Matrix26ControlCenterInitializer implements ApplicationRunner {

    private static final Logger LOGGER = LoggerFactory.getLogger(Matrix26ControlCenterInitializer.class);
    private static final String SUPER_ADMIN_ROLE = "ROLE_SUPER_ADMIN";

    private final JdbcTemplate jdbcTemplate;
    private final Matrix26ControlCenterProperties properties;
    private final RoleRepository roleRepository;
    private final UserAccountRepository userAccountRepository;
    private final PasswordEncoder passwordEncoder;
    private final PlatformSettingService platformSettingService;
    private final PlatformBusinessClientRepository clientRepository;
    private final PlatformModuleCatalogRepository moduleRepository;
    private final PlatformClientModuleRepository clientModuleRepository;

    public Matrix26ControlCenterInitializer(
            JdbcTemplate jdbcTemplate,
            Matrix26ControlCenterProperties properties,
            RoleRepository roleRepository,
            UserAccountRepository userAccountRepository,
            PasswordEncoder passwordEncoder,
            PlatformSettingService platformSettingService,
            PlatformBusinessClientRepository clientRepository,
            PlatformModuleCatalogRepository moduleRepository,
            PlatformClientModuleRepository clientModuleRepository
    ) {
        this.jdbcTemplate = jdbcTemplate;
        this.properties = properties;
        this.roleRepository = roleRepository;
        this.userAccountRepository = userAccountRepository;
        this.passwordEncoder = passwordEncoder;
        this.platformSettingService = platformSettingService;
        this.clientRepository = clientRepository;
        this.moduleRepository = moduleRepository;
        this.clientModuleRepository = clientModuleRepository;
    }

    @Override
    public void run(ApplicationArguments args) {
        createControlSchema();
        seedBranding();
        seedAdministrator();
        Map<String, PlatformModuleCatalog> modules = seedModuleCatalog();
        seedProtectedInstances(modules);

        LOGGER.info("Matrix26 Control Center initialized on {} using database {}.",
                properties.getPortalUrl(), properties.getDatabaseName());
        LOGGER.info("Bootstrap administrator username: {}", properties.getBootstrapAdminUsername());
    }

    private void createControlSchema() {
        for (String statement : schemaStatements()) {
            jdbcTemplate.execute(statement);
        }
    }

    private void seedBranding() {
        ensureSetting("platform.name", "Matrix26 Control Center", "string", "platform", "Control center display name");
        ensureSetting("platform.short_name", "Matrix26", "string", "platform", "Control center short name");
        ensureSetting("platform.tagline", "Administración central de instancias empresariales", "string", "platform", "Control center tagline");
        ensureSetting("platform.logo", "/img/matrix26-mark.svg", "string", "platform", "Matrix26 logo");
        ensureSetting("admin.brand.title", "Matrix26", "string", "admin", "Admin brand title");
        ensureSetting("admin.brand.subtitle", "Control Center", "string", "admin", "Admin brand subtitle");
        ensureSetting("admin.brand.logo", "/img/matrix26-mark.svg", "string", "admin", "Admin brand logo");
        ensureSetting("login.logo", "/img/matrix26-mark.svg", "string", "login", "Login logo");
        ensureSetting("login.title", "Matrix26 Control Center", "string", "login", "Login title");
        ensureSetting("login.subtitle", "Acceso exclusivo para la administración central de instancias.", "string", "login", "Login subtitle");
        ensureSetting("login.primary_color", "#172554", "string", "login", "Login primary color");
        ensureSetting("login.primary_hover_color", "#0f172a", "string", "login", "Login hover color");
        ensureSetting("login.back_to_public_label", "Ir al Control Center", "string", "login", "Login back label");
        ensureSetting("login.password_reset.show_dev_link", "true", "boolean", "login", "Show local password reset link");
    }

    private void seedAdministrator() {
        Role role = roleRepository.findByCode(SUPER_ADMIN_ROLE).orElseGet(() -> {
            Role created = new Role();
            created.setCode(SUPER_ADMIN_ROLE);
            created.setTitle("Matrix26 Super Administrator");
            return roleRepository.save(created);
        });

        String username = properties.getBootstrapAdminUsername();
        if (username == null || username.isBlank() || username.length() > 20) {
            throw new IllegalStateException("Matrix26 bootstrap administrator username must contain 1 to 20 characters.");
        }

        UserAccount user = userAccountRepository.findByUsername(username).orElseGet(() -> {
            UserAccount created = new UserAccount();
            created.setUsername(username);
            created.setPassword(passwordEncoder.encode(properties.getBootstrapAdminPassword()));
            created.setActive(1);
            created.setLegacyRol(1);
            created.setRegistrationDate(LocalDateTime.now());
            return created;
        });

        if (user.getRoles().stream().noneMatch(item -> SUPER_ADMIN_ROLE.equals(item.getCode()))) {
            user.getRoles().add(role);
        }
        userAccountRepository.save(user);
    }

    private Map<String, PlatformModuleCatalog> seedModuleCatalog() {
        List<ModuleSeed> seeds = List.of(
                new ModuleSeed("core", "Núcleo empresarial", "Plataforma", "Seguridad, configuración y funciones base.", 10),
                new ModuleSeed("sales", "Ventas y clientes", "Comercial", "Pedidos, clientes, cotizaciones y cuentas por cobrar.", 20),
                new ModuleSeed("inventory", "Inventario y logística", "Operaciones", "Productos, stock, almacén, insumos y entregas.", 30),
                new ModuleSeed("finance", "Finanzas y contabilidad", "Finanzas", "Caja, egresos, rentabilidad, contabilidad y reportes.", 40),
                new ModuleSeed("marketing", "Marketing", "Crecimiento", "Campañas, promociones, contenidos y biblioteca multimedia.", 50),
                new ModuleSeed("hr", "Recursos Humanos", "Gestión", "Empleados, puestos, pagos y obligaciones.", 60),
                new ModuleSeed("production", "Producción", "Operaciones", "Órdenes de producción y control de calidad.", 70),
                new ModuleSeed("public_catalog", "Portal y catálogo público", "Canales", "Catálogo, contenidos y canales de atención.", 80),
                new ModuleSeed("delivery", "Delivery", "Operaciones", "Zonas, rutas y seguimiento de entregas.", 90),
                new ModuleSeed("restaurant", "Restaurante", "Verticales", "Mesas, comandas, cocina, caja, reservas y carta QR.", 100)
        );

        Map<String, PlatformModuleCatalog> result = new LinkedHashMap<>();
        for (ModuleSeed seed : seeds) {
            PlatformModuleCatalog module = moduleRepository.findByModuleKey(seed.key()).orElseGet(PlatformModuleCatalog::new);
            module.setModuleKey(seed.key());
            module.setName(seed.name());
            module.setArea(seed.area());
            module.setDescription(seed.description());
            module.setDefaultEnabled(false);
            module.setConfigurable(true);
            module.setActive(true);
            module.setDisplayOrder(seed.displayOrder());
            result.put(seed.key(), moduleRepository.save(module));
        }
        return result;
    }

    private void seedProtectedInstances(Map<String, PlatformModuleCatalog> modules) {
        seedInstance(
                new InstanceSeed(
                        "eco-agua-amazonas",
                        "Eco Agua del Amazonas",
                        "water_delivery",
                        "eco_agua",
                        "aguaeco",
                        8081,
                        "http://localhost:8081",
                        "bash scripts/run-dev.sh",
                        "#0f766e",
                        Set.of("core", "sales", "inventory", "finance", "marketing", "hr", "production", "public_catalog", "delivery")
                ),
                modules
        );
        seedInstance(
                new InstanceSeed(
                        "productos-selva-belen",
                        "Productos de la Selva Belén",
                        "jungle_products",
                        "productos_selva_belen",
                        "belen",
                        8082,
                        "http://localhost:8082",
                        "bash scripts/run-belen.sh",
                        "#166534",
                        Set.of("core", "sales", "inventory", "finance", "marketing", "public_catalog", "delivery")
                ),
                modules
        );
        seedInstance(
                new InstanceSeed(
                        "restaurante-buen-sabor",
                        "Restaurante El Buen Sabor",
                        "restaurant",
                        "restaurante_buen_sabor",
                        "demo_restaurante_buen_sabor",
                        8084,
                        "http://localhost:8084",
                        "bash scripts/run-restaurant-demo.sh",
                        "#ea580c",
                        Set.of("core", "sales", "inventory", "finance", "restaurant", "delivery")
                ),
                modules
        );
    }

    private void seedInstance(InstanceSeed seed, Map<String, PlatformModuleCatalog> modules) {
        if (clientRepository.findByCode(seed.code()).isPresent()) {
            return;
        }

        PlatformBusinessClient client = new PlatformBusinessClient();
        client.setCode(seed.code());
        client.setBusinessName(seed.name());
        client.setLegalName(seed.name());
        client.setBusinessType(seed.businessType());
        client.setDatabaseName(seed.databaseName());
        client.setDatabaseStatus("READY");
        client.setStatus("ACTIVE");
        client.setCity("Iquitos");
        client.setCurrency("PEN");
        client.setPrimaryColor(seed.primaryColor());
        client.setPublicSlug(seed.code());
        client.setDemoDataEnabled(false);
        client.setRuntimeProfile(seed.runtimeProfile());
        client.setRuntimePort(seed.port());
        client.setPublicUrl(seed.url());
        client.setRuntimeStatus("EXTERNAL");
        client.setManagementMode("PROTECTED");
        client.setMonitorVisible(true);
        client.setProtectedInstance(true);
        client.setRuntimeCommand(seed.runCommand());
        client.setNotes("Existing protected instance registered by Matrix26 Control Center. Operational data remains isolated in its own database.");
        PlatformBusinessClient saved = clientRepository.save(client);

        for (String moduleKey : seed.moduleKeys()) {
            PlatformModuleCatalog module = modules.get(moduleKey);
            if (module == null) {
                continue;
            }
            PlatformClientModule assignment = new PlatformClientModule();
            assignment.setClient(saved);
            assignment.setModule(module);
            assignment.setEnabled(true);
            assignment.setSelectionSource("MATRIX26_PHASE1_SEED");
            assignment.setNotes("Initial Matrix26 module declaration for the protected instance.");
            clientModuleRepository.save(assignment);
        }
    }

    private void ensureSetting(String variable, String value, String type, String category, String description) {
        platformSettingService.ensure(variable, value, type, category, description);
    }

    private List<String> schemaStatements() {
        return List.of(
                """
                CREATE TABLE IF NOT EXISTS roles (
                    id INT NOT NULL AUTO_INCREMENT,
                    variable VARCHAR(200) NOT NULL,
                    title VARCHAR(250) NOT NULL,
                    PRIMARY KEY (id),
                    UNIQUE KEY uk_matrix26_roles_variable (variable)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
                """,
                """
                CREATE TABLE IF NOT EXISTS permission (
                    id INT NOT NULL AUTO_INCREMENT,
                    variable VARCHAR(200) NOT NULL,
                    description VARCHAR(255) NOT NULL,
                    PRIMARY KEY (id),
                    UNIQUE KEY uk_matrix26_permission_variable (variable)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
                """,
                """
                CREATE TABLE IF NOT EXISTS `user` (
                    id INT NOT NULL AUTO_INCREMENT,
                    username VARCHAR(20) NOT NULL,
                    password VARCHAR(100) NOT NULL,
                    active INT NOT NULL DEFAULT 1,
                    rol INT NULL,
                    registration_date DATETIME(6) NOT NULL,
                    PRIMARY KEY (id),
                    UNIQUE KEY uk_matrix26_user_username (username)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
                """,
                """
                CREATE TABLE IF NOT EXISTS user_roles (
                    user_id INT NOT NULL,
                    rol_id INT NOT NULL,
                    PRIMARY KEY (user_id, rol_id),
                    CONSTRAINT fk_matrix26_user_roles_user FOREIGN KEY (user_id) REFERENCES `user` (id),
                    CONSTRAINT fk_matrix26_user_roles_role FOREIGN KEY (rol_id) REFERENCES roles (id)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
                """,
                """
                CREATE TABLE IF NOT EXISTS role_permission (
                    role_id INT NOT NULL,
                    permission_id INT NOT NULL,
                    PRIMARY KEY (role_id, permission_id),
                    CONSTRAINT fk_matrix26_role_permission_role FOREIGN KEY (role_id) REFERENCES roles (id),
                    CONSTRAINT fk_matrix26_role_permission_permission FOREIGN KEY (permission_id) REFERENCES permission (id)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
                """,
                """
                CREATE TABLE IF NOT EXISTS platform_setting (
                    id BIGINT NOT NULL AUTO_INCREMENT,
                    variable VARCHAR(100) NOT NULL,
                    value VARCHAR(4000) NOT NULL,
                    type VARCHAR(50) NULL,
                    category VARCHAR(50) NULL,
                    description VARCHAR(255) NULL,
                    PRIMARY KEY (id),
                    UNIQUE KEY uk_matrix26_platform_setting_variable (variable)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
                """,
                """
                CREATE TABLE IF NOT EXISTS password_reset_token (
                    id BIGINT NOT NULL AUTO_INCREMENT,
                    user_id INT NOT NULL,
                    token_hash VARCHAR(64) NOT NULL,
                    expires_at DATETIME(6) NOT NULL,
                    used_at DATETIME(6) NULL,
                    created_at DATETIME(6) NOT NULL,
                    PRIMARY KEY (id),
                    UNIQUE KEY uk_matrix26_password_reset_hash (token_hash),
                    KEY idx_matrix26_password_reset_user (user_id),
                    CONSTRAINT fk_matrix26_password_reset_user FOREIGN KEY (user_id) REFERENCES `user` (id)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
                """,
                """
                CREATE TABLE IF NOT EXISTS platform_business_client (
                    id BIGINT NOT NULL AUTO_INCREMENT,
                    code VARCHAR(80) NOT NULL,
                    business_name VARCHAR(160) NOT NULL,
                    legal_name VARCHAR(180) NULL,
                    business_type VARCHAR(100) NULL,
                    template_id BIGINT NULL,
                    database_name VARCHAR(120) NULL,
                    database_status VARCHAR(50) NULL,
                    status VARCHAR(50) NULL,
                    owner_name VARCHAR(150) NULL,
                    contact_phone VARCHAR(50) NULL,
                    contact_email VARCHAR(150) NULL,
                    city VARCHAR(120) NULL,
                    currency VARCHAR(10) NULL,
                    whatsapp VARCHAR(50) NULL,
                    primary_color VARCHAR(30) NULL,
                    logo_url VARCHAR(500) NULL,
                    public_slug VARCHAR(120) NULL,
                    demo_data_enabled BIT NOT NULL DEFAULT 0,
                    runtime_profile VARCHAR(120) NULL,
                    runtime_port INT NULL,
                    public_url VARCHAR(500) NULL,
                    runtime_status VARCHAR(50) NULL,
                    management_mode VARCHAR(50) NULL,
                    monitor_visible BIT NOT NULL DEFAULT 1,
                    protected_instance BIT NOT NULL DEFAULT 0,
                    runtime_command VARCHAR(500) NULL,
                    last_health_status VARCHAR(50) NULL,
                    last_health_checked_at DATETIME(6) NULL,
                    last_health_message VARCHAR(500) NULL,
                    last_runtime_generated_at DATETIME(6) NULL,
                    notes TEXT NULL,
                    created_at DATETIME(6) NULL,
                    updated_at DATETIME(6) NULL,
                    PRIMARY KEY (id),
                    UNIQUE KEY uk_matrix26_business_client_code (code)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
                """,
                """
                CREATE TABLE IF NOT EXISTS platform_module_catalog (
                    id BIGINT NOT NULL AUTO_INCREMENT,
                    module_key VARCHAR(80) NOT NULL,
                    name VARCHAR(150) NOT NULL,
                    area VARCHAR(120) NOT NULL,
                    description TEXT NULL,
                    default_enabled BIT NOT NULL DEFAULT 0,
                    configurable BIT NOT NULL DEFAULT 1,
                    active BIT NOT NULL DEFAULT 1,
                    display_order INT NOT NULL DEFAULT 100,
                    created_at DATETIME(6) NULL,
                    updated_at DATETIME(6) NULL,
                    PRIMARY KEY (id),
                    UNIQUE KEY uk_matrix26_module_key (module_key)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
                """,
                """
                CREATE TABLE IF NOT EXISTS platform_client_module (
                    id BIGINT NOT NULL AUTO_INCREMENT,
                    client_id BIGINT NOT NULL,
                    module_id BIGINT NOT NULL,
                    enabled BIT NOT NULL DEFAULT 1,
                    selection_source VARCHAR(50) NULL,
                    notes VARCHAR(255) NULL,
                    PRIMARY KEY (id),
                    UNIQUE KEY uk_platform_client_module (client_id, module_id),
                    KEY idx_matrix26_client_module_module (module_id),
                    CONSTRAINT fk_matrix26_client_module_client FOREIGN KEY (client_id) REFERENCES platform_business_client (id),
                    CONSTRAINT fk_matrix26_client_module_module FOREIGN KEY (module_id) REFERENCES platform_module_catalog (id)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
                """,
                """
                CREATE TABLE IF NOT EXISTS matrix26_instance_audit_log (
                    id BIGINT NOT NULL AUTO_INCREMENT,
                    instance_id BIGINT NULL,
                    action VARCHAR(80) NOT NULL,
                    actor_username VARCHAR(120) NOT NULL,
                    summary VARCHAR(500) NOT NULL,
                    before_snapshot TEXT NULL,
                    after_snapshot TEXT NULL,
                    created_at DATETIME(6) NOT NULL,
                    PRIMARY KEY (id),
                    KEY idx_matrix26_audit_instance_created (instance_id, created_at),
                    KEY idx_matrix26_audit_created (created_at),
                    CONSTRAINT fk_matrix26_audit_instance FOREIGN KEY (instance_id) REFERENCES platform_business_client (id)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
                """,
                """
                CREATE TABLE IF NOT EXISTS matrix26_instance_health_check (
                    id BIGINT NOT NULL AUTO_INCREMENT,
                    instance_id BIGINT NOT NULL,
                    online BIT NOT NULL DEFAULT 0,
                    http_status INT NULL,
                    response_time_ms BIGINT NULL,
                    message VARCHAR(500) NULL,
                    checked_at DATETIME(6) NOT NULL,
                    PRIMARY KEY (id),
                    KEY idx_matrix26_health_instance_checked (instance_id, checked_at),
                    KEY idx_matrix26_health_checked (checked_at),
                    CONSTRAINT fk_matrix26_health_instance FOREIGN KEY (instance_id) REFERENCES platform_business_client (id)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
                """
        );
    }

    private record ModuleSeed(String key, String name, String area, String description, int displayOrder) {
    }

    private record InstanceSeed(
            String code,
            String name,
            String businessType,
            String databaseName,
            String runtimeProfile,
            int port,
            String url,
            String runCommand,
            String primaryColor,
            Set<String> moduleKeys
    ) {
    }
}
