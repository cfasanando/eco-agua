package com.ecoamazonas.eco_agua.platform.control.demo;

import com.ecoamazonas.eco_agua.platform.control.operations.Matrix26OperationsSnapshot;
import com.ecoamazonas.eco_agua.platform.control.operations.Matrix26RuntimeControlService;
import com.ecoamazonas.eco_agua.platform.control.operations.Matrix26RuntimeControlView;
import com.ecoamazonas.eco_agua.platform.control.operations.Matrix26RuntimeInventoryItem;
import com.ecoamazonas.eco_agua.platform.control.operations.Matrix26RuntimeState;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

@Service
public class Matrix26DemoCenterService {

    private final ObjectProvider<com.ecoamazonas.eco_agua.platform.control.operations.Matrix26OperationsInventoryService> inventoryServiceProvider;
    private final ObjectProvider<Matrix26RuntimeControlService> runtimeControlServiceProvider;
    private final Matrix26DemoRuntimeService demoRuntimeService;

    public Matrix26DemoCenterService(
            ObjectProvider<com.ecoamazonas.eco_agua.platform.control.operations.Matrix26OperationsInventoryService> inventoryServiceProvider,
            ObjectProvider<Matrix26RuntimeControlService> runtimeControlServiceProvider,
            Matrix26DemoRuntimeService demoRuntimeService
    ) {
        this.inventoryServiceProvider = inventoryServiceProvider;
        this.runtimeControlServiceProvider = runtimeControlServiceProvider;
        this.demoRuntimeService = demoRuntimeService;
    }

    public Matrix26DemoCenterDashboard dashboard(boolean refresh) {
        com.ecoamazonas.eco_agua.platform.control.operations.Matrix26OperationsInventoryService inventoryService = inventoryServiceProvider.getIfAvailable();
        Matrix26RuntimeControlService runtimeControlService = runtimeControlServiceProvider.getIfAvailable();

        List<Matrix26RuntimeInventoryItem> runtimes = List.of();
        Map<String, Matrix26RuntimeControlView> controls = Map.of();
        List<String> warnings = new ArrayList<>();

        if (inventoryService == null) {
            warnings.add("Modo Demo Center local: los módulos internos se abren directo y los runtimes conocidos se administran con control seguro por PID propio.");
        } else {
            Matrix26OperationsSnapshot snapshot = inventoryService.snapshot(refresh);
            runtimes = snapshot.runtimes() == null ? List.of() : snapshot.runtimes();
            warnings.addAll(snapshot.probeWarnings() == null ? List.of() : snapshot.probeWarnings());
            if (runtimeControlService == null) {
                warnings.add("El control de inicio/detención de runtimes no está activo en este perfil. Usa Matrix26 Control Center para administrar puertos externos.");
            } else {
                controls = runtimeControlService.views(runtimes);
            }
        }

        final List<Matrix26RuntimeInventoryItem> runtimeInventory = runtimes;
        final Map<String, Matrix26RuntimeControlView> runtimeControls = controls;

        List<Matrix26DemoPortalView> portals = definitions().stream()
                .sorted(Comparator.comparingInt(Matrix26DemoPortalDefinition::priority))
                .map(definition -> view(definition, runtimeInventory, runtimeControls))
                .toList();

        Matrix26DemoCenterSummary summary = new Matrix26DemoCenterSummary(
                portals.size(),
                (int) portals.stream().filter(portal -> portal.definition().readiness() == Matrix26DemoReadiness.READY).count(),
                (int) portals.stream().filter(Matrix26DemoPortalView::online).count(),
                (int) portals.stream().filter(portal -> portal.definition().runtimeManaged()).count(),
                (int) portals.stream().filter(portal -> portal.definition().mode() == Matrix26DemoPortalMode.INTERNAL).count(),
                (int) portals.stream().filter(portal -> portal.definition().readiness() == Matrix26DemoReadiness.PREPARE_DATA).count()
        );

        return new Matrix26DemoCenterDashboard(summary, portals, warnings);
    }

    public Matrix26DemoPortalView portal(String code, boolean refresh) {
        Matrix26DemoCenterDashboard dashboard = dashboard(refresh);
        return dashboard.portals().stream()
                .filter(portal -> portal.definition().code().equalsIgnoreCase(code))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("El portal demo solicitado no existe."));
    }

    public Matrix26RuntimeControlService runtimeControlService() {
        Matrix26RuntimeControlService service = runtimeControlServiceProvider.getIfAvailable();
        if (service == null) {
            throw new IllegalStateException("El control de runtimes no está activo en este perfil. Abre Matrix26 Control Center o habilita matrix26.control-center.enabled=true.");
        }
        return service;
    }

    public Matrix26DemoRuntimeActionResult startPortal(String code, String actor) {
        Matrix26DemoPortalDefinition definition = definition(code);
        Matrix26DemoRuntimeStatus localRuntime = demoRuntimeService.status(definition);
        if (localRuntime.applicable() && localRuntime.configured()) {
            return demoRuntimeService.start(definition, actor);
        }

        Matrix26RuntimeControlService service = runtimeControlServiceProvider.getIfAvailable();
        if (service == null) {
            return Matrix26DemoRuntimeActionResult.failure("No hay runtime local configurado ni control Matrix26 activo para iniciar este portal.");
        }
        String runtimeKey = portal(code, true).runtimeKey();
        if (runtimeKey == null || runtimeKey.isBlank()) {
            return Matrix26DemoRuntimeActionResult.failure("Este portal demo no tiene runtime administrable registrado.");
        }
        return Matrix26DemoRuntimeActionResult.success(service.start(runtimeKey, actor).message());
    }

    public Matrix26DemoRuntimeActionResult stopPortal(String code, String actor) {
        Matrix26DemoPortalDefinition definition = definition(code);
        Matrix26DemoRuntimeStatus localRuntime = demoRuntimeService.status(definition);
        if (localRuntime.applicable() && localRuntime.configured()) {
            return demoRuntimeService.stop(definition, actor);
        }

        Matrix26RuntimeControlService service = runtimeControlServiceProvider.getIfAvailable();
        if (service == null) {
            return Matrix26DemoRuntimeActionResult.failure("No hay runtime local configurado ni control Matrix26 activo para detener este portal.");
        }
        String runtimeKey = portal(code, true).runtimeKey();
        if (runtimeKey == null || runtimeKey.isBlank()) {
            return Matrix26DemoRuntimeActionResult.failure("Este portal demo no tiene runtime administrable registrado.");
        }
        return Matrix26DemoRuntimeActionResult.success(service.stop(runtimeKey, actor, "STOP " + definition.code()).message());
    }

    public Matrix26DemoRuntimeActionResult restartPortal(String code, String actor) {
        Matrix26DemoPortalDefinition definition = definition(code);
        Matrix26DemoRuntimeStatus localRuntime = demoRuntimeService.status(definition);
        if (localRuntime.applicable() && localRuntime.configured()) {
            return demoRuntimeService.restart(definition, actor);
        }

        Matrix26RuntimeControlService service = runtimeControlServiceProvider.getIfAvailable();
        if (service == null) {
            return Matrix26DemoRuntimeActionResult.failure("No hay runtime local configurado ni control Matrix26 activo para reiniciar este portal.");
        }
        String runtimeKey = portal(code, true).runtimeKey();
        if (runtimeKey == null || runtimeKey.isBlank()) {
            return Matrix26DemoRuntimeActionResult.failure("Este portal demo no tiene runtime administrable registrado.");
        }
        return Matrix26DemoRuntimeActionResult.success(service.restart(runtimeKey, actor, "RESTART " + definition.code()).message());
    }

    public Matrix26DemoRuntimeLogView logs(String code) {
        return demoRuntimeService.log(portal(code, true));
    }

    private Matrix26DemoPortalDefinition definition(String code) {
        return definitions().stream()
                .filter(definition -> definition.code().equalsIgnoreCase(code))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("El portal demo solicitado no existe."));
    }

    private Matrix26DemoPortalView view(
            Matrix26DemoPortalDefinition definition,
            List<Matrix26RuntimeInventoryItem> runtimes,
            Map<String, Matrix26RuntimeControlView> controls
    ) {
        Matrix26RuntimeInventoryItem runtime = findRuntime(definition, runtimes).orElse(null);
        Matrix26RuntimeControlView control = runtime == null ? null : controls.get(runtime.target().key());
        Matrix26DemoRuntimeStatus localRuntime = demoRuntimeService.status(definition);
        boolean online = definition.mode() == Matrix26DemoPortalMode.INTERNAL
                ? true
                : (runtime != null && runtime.online()) || localRuntime.httpReachable() || localRuntime.portListening();

        String statusLabel;
        String statusDetail;
        String statusBadgeClass;
        String runtimeKey = null;
        String operationDetailUrl = "";

        if (definition.mode() == Matrix26DemoPortalMode.INTERNAL) {
            statusLabel = "Disponible en el portal actual";
            statusDetail = "No requiere levantar otro puerto.";
            statusBadgeClass = "text-bg-primary";
        } else if (runtime == null && localRuntime.configured()) {
            statusLabel = localRuntime.statusLabel();
            statusDetail = localRuntime.detail();
            statusBadgeClass = localRuntime.badgeClass();
            operationDetailUrl = "/control-center/demo-center/" + definition.code() + "/logs";
        } else if (runtime == null) {
            statusLabel = "Runtime no configurado";
            statusDetail = "Existe ficha comercial, pero no se encontró configuración local ni inventario Matrix26 para este portal.";
            statusBadgeClass = "text-bg-secondary";
        } else {
            runtimeKey = runtime.target().key();
            operationDetailUrl = "/control-center/operations/runtimes/" + runtimeKey + "?refresh=true";
            statusLabel = spanishState(runtime.state());
            statusDetail = runtime.stateDetail();
            if (control != null && !control.manageable() && control.reason() != null && !control.reason().isBlank()) {
                statusDetail = statusDetail + " Control: " + control.reason();
            }
            statusBadgeClass = runtime.state().getBadgeClass();
        }

        return new Matrix26DemoPortalView(
                definition,
                runtime,
                control,
                localRuntime,
                online,
                statusLabel,
                statusDetail,
                statusBadgeClass,
                runtimeKey,
                definition.localUrl(),
                operationDetailUrl
        );
    }

    private Optional<Matrix26RuntimeInventoryItem> findRuntime(
            Matrix26DemoPortalDefinition definition,
            List<Matrix26RuntimeInventoryItem> runtimes
    ) {
        if (!definition.runtimeManaged()) {
            return Optional.empty();
        }

        List<String> candidates = new ArrayList<>();
        candidates.add(normalize(definition.code()));
        candidates.add(normalize(definition.runtimeProfile()));
        candidates.add(normalize(definition.instanceCode()));

        return runtimes.stream()
                .filter(runtime -> matches(definition, runtime, candidates))
                .findFirst();
    }

    private boolean matches(
            Matrix26DemoPortalDefinition definition,
            Matrix26RuntimeInventoryItem runtime,
            List<String> candidates
    ) {
        String runtimeProfile = normalize(runtime.target().runtimeProfile());
        String instanceCode = normalize(runtime.target().code());
        String key = normalize(runtime.target().key());
        boolean byText = candidates.stream()
                .filter(value -> value != null && !value.isBlank())
                .anyMatch(value -> value.equals(runtimeProfile) || value.equals(instanceCode) || value.equals(key));
        boolean byPort = definition.port() != null
                && runtime.target().expectedPort() != null
                && definition.port().equals(runtime.target().expectedPort());
        return byText || byPort;
    }

    private String normalize(String value) {
        if (value == null) {
            return "";
        }
        return value.trim().toLowerCase(Locale.ROOT).replace('_', '-');
    }

    private String spanishState(Matrix26RuntimeState state) {
        if (state == null) {
            return "Desconocido";
        }
        return switch (state) {
            case ONLINE -> "Activo";
            case OFFLINE -> "Apagado";
            case PORT_OCCUPIED -> "Puerto ocupado";
            case PROCESS_FOUND -> "Proceso detectado";
            case PROCESS_NOT_FOUND -> "Proceso no detectado";
            case CONFIGURATION_MISSING -> "Configuración faltante";
            case RUNTIME_MISSING -> "Runtime faltante";
            case LOG_MISSING -> "Log no disponible";
            case DEGRADED -> "Degradado";
            case UNKNOWN -> "Desconocido";
        };
    }

    private List<Matrix26DemoPortalDefinition> definitions() {
        List<Matrix26DemoPortalDefinition> list = new ArrayList<>();
        list.add(new Matrix26DemoPortalDefinition(
                "restaurante-buen-sabor",
                "Restaurante El Buen Sabor",
                "Restaurantes",
                Matrix26DemoPortalMode.MANAGED_RUNTIME,
                Matrix26DemoReadiness.READY,
                "http://localhost:8084/admin/restaurant",
                "/admin/restaurant",
                8084,
                "demo_restaurante_buen_sabor",
                "demo-restaurante-buen-sabor",
                "restaurante_buen_sabor",
                "demo_restaurante",
                "demo123",
                "bi-egg-fried",
                "#f97316",
                "Controla carta, mesas, pedidos, cocina, caja, reservas, delivery, recetas y stock desde un solo portal.",
                "Mostrar una venta desde mesa o QR, pasar a cocina, cobrar y cerrar caja con reporte diario.",
                "Es el demo comercial más visual y más fácil de vender a restaurantes pequeños.",
                List.of("Menú", "Mesas", "Pedidos", "Cocina", "Caja", "Reservas", "Delivery", "Recetas", "Stock"),
                10
        ));
        list.add(new Matrix26DemoPortalDefinition(
                "gasto-claro-personal",
                "GastoClaro Personal",
                "Finanzas personales",
                Matrix26DemoPortalMode.INTERNAL,
                Matrix26DemoReadiness.READY,
                "http://localhost:8081/gasto-claro",
                "/gasto-claro",
                8081,
                "dev",
                "gasto-claro-personal",
                "eco_agua_dev",
                "usuario actual",
                "según login local",
                "bi-wallet2",
                "#2563eb",
                "Organiza ingresos, deudas, pagos, cronogramas, alertas, reservas y negociación de acreedores.",
                "Mostrar gratificación, sobres reservados, pago parcial, deuda con cronograma y alerta de vencimiento.",
                "Demo ideal para explicar el valor rápido porque resuelve un dolor personal claro.",
                List.of("Ingresos", "Deudas", "Pagos", "Reservas", "Alertas", "Negociaciones", "Simulador"),
                20
        ));
        list.add(new Matrix26DemoPortalDefinition(
                "academy-online",
                "Academia Online",
                "Educación",
                Matrix26DemoPortalMode.INTERNAL,
                Matrix26DemoReadiness.READY,
                "http://localhost:8081/academy",
                "/academy",
                8081,
                "dev",
                "academy-online",
                "eco_agua_dev",
                "usuario demo",
                "según login local",
                "bi-mortarboard",
                "#7c3aed",
                "Portal de cursos con catálogo, lecciones, evaluaciones, matrículas, leads y certificados verificables.",
                "Mostrar catálogo, entrar a un curso, revisar una lección, resolver evaluación y verificar certificado.",
                "Útil para academias, capacitadores, institutos pequeños y negocios que venden formación.",
                List.of("Catálogo", "Cursos", "Lecciones", "Evaluaciones", "Matrículas", "Leads", "Certificados"),
                30
        ));
        list.add(new Matrix26DemoPortalDefinition(
                "tienda-express",
                "Tienda Express / Catálogo WhatsApp",
                "Comercio",
                Matrix26DemoPortalMode.MANAGED_RUNTIME,
                Matrix26DemoReadiness.PREPARE_DATA,
                "http://localhost:8083/catalogo",
                "/catalogo",
                8083,
                "demo_tienda_china_temu",
                "demo-tienda-china-temu",
                "tienda_china_express",
                "demo_tienda",
                "demo123",
                "bi-bag-check",
                "#dc2626",
                "Catálogo público con productos, promociones, consulta por WhatsApp, stock y rentabilidad por canal.",
                "Mostrar catálogo, producto destacado, promoción y envío de consulta por WhatsApp.",
                "Conviene renombrar la marca demo antes de vender; el runtime actual usa Tienda China Express.",
                List.of("Catálogo", "Productos", "Promociones", "WhatsApp", "Stock", "Marketing", "Rentabilidad"),
                40
        ));
        list.add(new Matrix26DemoPortalDefinition(
                "agua-eco-delivery",
                "Agua Eco Delivery",
                "Reparto local",
                Matrix26DemoPortalMode.INTERNAL,
                Matrix26DemoReadiness.PREPARE_DATA,
                "http://localhost:8081/delivery/dashboard",
                "/delivery/dashboard",
                8081,
                "dev",
                "agua-eco-delivery",
                "eco_agua_dev",
                "usuario actual",
                "según login local",
                "bi-truck",
                "#0891b2",
                "Control para negocios de reparto: pedidos, clientes, rutas, mapa, envases, cobranzas y recompra.",
                "Mostrar ruta diaria, entrega móvil, cuentas por cobrar, envases pendientes y agenda de recompra.",
                "Es el demo base del negocio Agua Eco, pero necesita una historia comercial limpia.",
                List.of("Clientes", "Pedidos", "Rutas", "Mapa", "Móvil", "Envases", "Cobranza", "Recompra"),
                50
        ));
        list.add(new Matrix26DemoPortalDefinition(
                "matrix26-control",
                "Matrix26 Control Center",
                "Administración central",
                Matrix26DemoPortalMode.CONTROL_CENTER,
                Matrix26DemoReadiness.INTERNAL_ONLY,
                "http://localhost:8091/control-center",
                "/control-center",
                8091,
                "matrix26_control",
                "matrix26-control",
                "matrix26_platform_control",
                "matrix_admin",
                "Matrix26Demo123!",
                "bi-command",
                "#0f172a",
                "Panel maestro para instancias, módulos, aprovisionamiento, runtimes, backups, restores, lifecycle y apariencia.",
                "Mostrar inventario de instancias, estado de runtimes, backups y publicación de apariencia por instancia.",
                "No vender directo a clientes pequeños; usarlo como tu centro interno para operar demos y futuros clientes.",
                List.of("Instancias", "Módulos", "Runtimes", "Backups", "Restores", "Appearance", "Lifecycle"),
                60
        ));
        list.add(new Matrix26DemoPortalDefinition(
                "produccion-inventario",
                "Producción e Inventario",
                "Operaciones",
                Matrix26DemoPortalMode.INTERNAL,
                Matrix26DemoReadiness.REVIEW,
                "http://localhost:8081/production",
                "/production",
                8081,
                "dev",
                "produccion-inventario",
                "eco_agua_dev",
                "usuario actual",
                "según login local",
                "bi-box-seam",
                "#16a34a",
                "Planificación, recetas, requerimientos de materiales, capacidad, calidad, vencimientos y trazabilidad.",
                "Mostrar receta, requerimiento de materiales, capacidad disponible y control de calidad.",
                "Buen demo para productos naturales, alimentos o producción artesanal; revisar datos antes de vender.",
                List.of("Planificación", "Recetas", "Materiales", "Capacidad", "Calidad", "Vencimiento", "Trazabilidad"),
                70
        ));
        list.add(new Matrix26DemoPortalDefinition(
                "marketing-center",
                "Marketing Center",
                "Marketing",
                Matrix26DemoPortalMode.INTERNAL,
                Matrix26DemoReadiness.REVIEW,
                "http://localhost:8081/marketing/admin/strategy",
                "/marketing/admin/strategy",
                8081,
                "dev",
                "marketing-center",
                "eco_agua_dev",
                "usuario actual",
                "según login local",
                "bi-megaphone",
                "#db2777",
                "Estrategia, campañas, ideas, plan de publicaciones, promociones, productos destacados y reporte de acciones.",
                "Mostrar matriz de estrategia, campaña mensual, banco de ideas y plan de publicaciones.",
                "Complemento fuerte para restaurante y tienda, no necesariamente portal independiente al inicio.",
                List.of("Estrategia", "Campañas", "Ideas", "Publicaciones", "Promociones", "Imágenes", "Reportes"),
                80
        ));
        list.add(new Matrix26DemoPortalDefinition(
                "rrhh-asistencia",
                "RRHH y Asistencia",
                "Personal",
                Matrix26DemoPortalMode.INTERNAL,
                Matrix26DemoReadiness.REVIEW,
                "http://localhost:8081/admin/personnel",
                "/admin/personnel",
                8081,
                "dev",
                "rrhh-asistencia",
                "eco_agua_dev",
                "usuario actual",
                "según login local",
                "bi-person-badge",
                "#0d9488",
                "Control básico de personal, asistencia, pagos, obligaciones, planilla mensual y puestos.",
                "Mostrar asistencia del día, perfil de trabajador, pagos y resumen mensual.",
                "Puede convertirse en producto pequeño si se pule como control de asistencia simple.",
                List.of("Personal", "Asistencia", "Pagos", "Planilla", "Puestos", "Roles"),
                90
        ));
        list.add(new Matrix26DemoPortalDefinition(
                "contabilidad-finanzas",
                "Contabilidad y Finanzas",
                "Contabilidad",
                Matrix26DemoPortalMode.INTERNAL,
                Matrix26DemoReadiness.INTERNAL_ONLY,
                "http://localhost:8081/accounting/control-panel",
                "/accounting/control-panel",
                8081,
                "dev",
                "contabilidad-finanzas",
                "eco_agua_dev",
                "usuario actual",
                "según login local",
                "bi-calculator",
                "#334155",
                "Plan contable, reglas, asientos, diario, mayor, balance, estado de resultados y revisión de borradores.",
                "Mostrar panel contable, asiento automático y reportes básicos.",
                "Módulo potente pero delicado; mejor demo avanzada o interna hasta validar reglas contables.",
                List.of("Plan contable", "Reglas", "Asientos", "Libro diario", "Mayor", "Balance", "Resultados"),
                100
        ));
        return list;
    }
}
