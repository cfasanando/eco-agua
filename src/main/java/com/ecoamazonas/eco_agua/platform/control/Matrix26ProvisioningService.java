package com.ecoamazonas.eco_agua.platform.control;

import com.ecoamazonas.eco_agua.platform.PlatformBusinessClientRepository;
import com.ecoamazonas.eco_agua.platform.PlatformModuleCatalog;
import com.ecoamazonas.eco_agua.platform.PlatformModuleCatalogRepository;
import com.ecoamazonas.eco_agua.platform.module.PlatformModuleInstaller;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
@ConditionalOnProperty(name = "matrix26.control-center.enabled", havingValue = "true")
public class Matrix26ProvisioningService {

    private static final String STATUS_READY = "READY";
    private static final String STATUS_BLOCKED = "BLOCKED";
    private static final String CORE_MODULE_KEY = "core";

    private final PlatformBusinessClientRepository clientRepository;
    private final PlatformModuleCatalogRepository moduleRepository;
    private final Matrix26ProvisioningJobRepository jobRepository;
    private final Matrix26ProvisioningStepRepository stepRepository;
    private final Matrix26ProvisioningModuleRepository provisioningModuleRepository;
    private final Matrix26InstanceAuditLogRepository auditRepository;
    private final Matrix26ControlCenterProperties properties;
    private final Map<String, PlatformModuleInstaller> installers;

    public Matrix26ProvisioningService(
            PlatformBusinessClientRepository clientRepository,
            PlatformModuleCatalogRepository moduleRepository,
            Matrix26ProvisioningJobRepository jobRepository,
            Matrix26ProvisioningStepRepository stepRepository,
            Matrix26ProvisioningModuleRepository provisioningModuleRepository,
            Matrix26InstanceAuditLogRepository auditRepository,
            Matrix26ControlCenterProperties properties,
            List<PlatformModuleInstaller> installerList
    ) {
        this.clientRepository = clientRepository;
        this.moduleRepository = moduleRepository;
        this.jobRepository = jobRepository;
        this.stepRepository = stepRepository;
        this.provisioningModuleRepository = provisioningModuleRepository;
        this.auditRepository = auditRepository;
        this.properties = properties;
        this.installers = new LinkedHashMap<>();
        for (PlatformModuleInstaller installer : installerList) {
            installers.put(normalizeKey(installer.moduleKey()), installer);
        }
    }

    public Matrix26ProvisioningPlanForm newForm() {
        Matrix26ProvisioningPlanForm form = new Matrix26ProvisioningPlanForm();
        form.setCity("Iquitos");
        form.setRuntimePort(nextSuggestedPort());
        form.setPublicUrl("http://localhost:" + form.getRuntimePort());
        form.setAdminUsername("admin_demo");
        if (installers.containsKey("restaurant")) {
            form.setSelectedModules(List.of("restaurant"));
        }
        return form;
    }

    public List<Matrix26ProvisioningJob> listJobs() {
        return jobRepository.findAllByOrderByCreatedAtDescIdDesc();
    }

    public List<Matrix26ProvisioningJob> recentJobs() {
        return jobRepository.findTop6ByOrderByCreatedAtDescIdDesc();
    }

    public Matrix26ProvisioningSummary summary() {
        return new Matrix26ProvisioningSummary(
                jobRepository.count(),
                jobRepository.countByStatus(STATUS_READY),
                jobRepository.countByStatus(STATUS_BLOCKED)
        );
    }

    public Matrix26ProvisioningPlanView getPlan(Long id) {
        Matrix26ProvisioningJob job = getJob(id);
        return new Matrix26ProvisioningPlanView(
                job,
                stepRepository.findByJob_IdOrderByDisplayOrderAscIdAsc(id),
                provisioningModuleRepository.findByJob_IdOrderByModuleNameAscIdAsc(id)
        );
    }

    public Map<String, List<Matrix26ProvisioningModuleOption>> groupedModuleOptions() {
        Map<String, List<Matrix26ProvisioningModuleOption>> grouped = new LinkedHashMap<>();
        for (PlatformModuleCatalog module : moduleRepository.findAllByActiveTrueOrderByAreaAscDisplayOrderAscNameAsc()) {
            if (CORE_MODULE_KEY.equals(normalizeKey(module.getModuleKey()))) {
                continue;
            }
            PlatformModuleInstaller installer = installers.get(normalizeKey(module.getModuleKey()));
            boolean available = installer != null;
            Matrix26ProvisioningModuleOption option = new Matrix26ProvisioningModuleOption(
                    module.getModuleKey(),
                    module.getName(),
                    module.getArea(),
                    module.getDescription(),
                    available,
                    available ? clean(installer.currentVersion()) : null,
                    available ? "Instalador disponible" : "Pendiente de instalador"
            );
            grouped.computeIfAbsent(module.getArea(), ignored -> new ArrayList<>()).add(option);
        }
        return grouped;
    }

    @Transactional
    public Matrix26ProvisioningJob createDryRun(Matrix26ProvisioningPlanForm form, String actor) {
        Matrix26ProvisioningJob job = new Matrix26ProvisioningJob();
        job.setReferenceCode(generateReference());
        job.setRequestedBy(defaultValue(actor, "system"));
        applyForm(job, form);
        Matrix26ProvisioningJob saved = jobRepository.save(job);
        validateAndBuild(saved, normalizedModuleKeys(form.getSelectedModules()));
        saveAudit(saved, "PROVISIONING_DRY_RUN_CREATED", actor);
        return saved;
    }

    @Transactional
    public Matrix26ProvisioningJob revalidate(Long id, String actor) {
        Matrix26ProvisioningJob job = getJob(id);
        Set<String> moduleKeys = provisioningModuleRepository.findByJob_IdOrderByModuleNameAscIdAsc(id).stream()
                .map(Matrix26ProvisioningModule::getModuleKey)
                .filter(key -> !CORE_MODULE_KEY.equals(normalizeKey(key)))
                .collect(LinkedHashSet::new, Set::add, Set::addAll);
        validateAndBuild(job, moduleKeys);
        saveAudit(job, "PROVISIONING_DRY_RUN_REVALIDATED", actor);
        return job;
    }

    private void validateAndBuild(Matrix26ProvisioningJob job, Set<String> selectedModuleKeys) {
        stepRepository.deleteByJob_Id(job.getId());
        provisioningModuleRepository.deleteByJob_Id(job.getId());

        List<ValidationIssue> issues = validate(job, selectedModuleKeys);
        boolean ready = issues.isEmpty();
        job.setStatus(ready ? STATUS_READY : STATUS_BLOCKED);
        job.setValidatedAt(LocalDateTime.now());
        job.setValidationSummary(ready
                ? "Plan validado. No se ejecutó ninguna operación sobre bases o runtimes."
                : issues.stream().map(ValidationIssue::message).distinct().reduce((left, right) -> left + "\n" + right).orElse("Plan bloqueado."));
        Matrix26ProvisioningJob saved = jobRepository.save(job);

        saveModuleSnapshot(saved, selectedModuleKeys, issues);
        saveStepPlan(saved, selectedModuleKeys, issues);
    }

    private List<ValidationIssue> validate(Matrix26ProvisioningJob job, Set<String> selectedModuleKeys) {
        List<ValidationIssue> issues = new ArrayList<>();

        if (clientRepository.existsByCodeIgnoreCase(job.getInstanceCode())) {
            issues.add(new ValidationIssue("IDENTITY", "Ya existe una instancia con el código " + job.getInstanceCode() + "."));
        }
        if (clientRepository.existsByDatabaseNameIgnoreCase(job.getDatabaseName())) {
            issues.add(new ValidationIssue("DATABASE", "La base " + job.getDatabaseName() + " ya está asignada a otra instancia."));
        }
        if (clientRepository.existsByRuntimeProfileIgnoreCase(job.getRuntimeProfile())) {
            issues.add(new ValidationIssue("RUNTIME", "El runtime " + job.getRuntimeProfile() + " ya está registrado."));
        }
        if (clientRepository.existsByRuntimePort(job.getRuntimePort())) {
            issues.add(new ValidationIssue("PORT", "El puerto " + job.getRuntimePort() + " ya está registrado en otra instancia."));
        }

        if (properties.getDatabaseName().equalsIgnoreCase(job.getDatabaseName())) {
            issues.add(new ValidationIssue("DATABASE", "La base del Control Center no puede utilizarse como base operativa."));
        }
        if (properties.getRuntimeProfile().equalsIgnoreCase(job.getRuntimeProfile())) {
            issues.add(new ValidationIssue("RUNTIME", "El runtime del Control Center está reservado."));
        }
        if (sameUrl(properties.getPortalUrl(), job.getPublicUrl())) {
            issues.add(new ValidationIssue("URL", "La URL del Control Center está reservada."));
        }

        Integer controlPort = portOf(properties.getPortalUrl());
        if (controlPort != null && controlPort.equals(job.getRuntimePort())) {
            issues.add(new ValidationIssue("PORT", "El puerto " + controlPort + " está reservado para Matrix26 Control Center."));
        }

        if (!isValidHttpUrl(job.getPublicUrl())) {
            issues.add(new ValidationIssue("URL", "La URL propuesta no es una dirección HTTP o HTTPS válida."));
        }

        Integer urlPort = portOf(job.getPublicUrl());
        if (urlPort != null && !urlPort.equals(job.getRuntimePort())) {
            issues.add(new ValidationIssue("URL", "El puerto de la URL (" + urlPort + ") no coincide con el puerto del runtime (" + job.getRuntimePort() + ")."));
        }

        if (selectedModuleKeys.isEmpty()) {
            issues.add(new ValidationIssue("MODULE", "Selecciona al menos un módulo funcional."));
        }

        Map<String, PlatformModuleCatalog> activeModules = activeModulesByKey();
        for (String key : selectedModuleKeys) {
            PlatformModuleCatalog module = activeModules.get(key);
            if (module == null) {
                issues.add(new ValidationIssue("MODULE:" + key, "El módulo " + key + " no existe o está inactivo."));
                continue;
            }
            if (!installers.containsKey(key)) {
                issues.add(new ValidationIssue(
                        "MODULE:" + key,
                        "El módulo " + module.getName() + " todavía no tiene un instalador ejecutable registrado."
                ));
            }
        }
        return issues;
    }

    private void saveModuleSnapshot(
            Matrix26ProvisioningJob job,
            Set<String> selectedModuleKeys,
            List<ValidationIssue> issues
    ) {
        Matrix26ProvisioningModule core = new Matrix26ProvisioningModule();
        core.setJob(job);
        core.setModuleKey(CORE_MODULE_KEY);
        core.setModuleName("Núcleo empresarial");
        core.setInstallerAvailable(true);
        core.setInstallerVersion("built-in");
        core.setStatus(STATUS_READY);
        core.setDetail("El núcleo común forma parte obligatoria de toda nueva instancia.");
        provisioningModuleRepository.save(core);

        Map<String, PlatformModuleCatalog> modules = activeModulesByKey();
        for (String key : selectedModuleKeys) {
            PlatformModuleCatalog catalog = modules.get(key);
            PlatformModuleInstaller installer = installers.get(key);
            Matrix26ProvisioningModule item = new Matrix26ProvisioningModule();
            item.setJob(job);
            item.setModuleKey(key);
            item.setModuleName(catalog == null ? key : catalog.getName());
            item.setInstallerAvailable(installer != null);
            item.setInstallerVersion(installer == null ? null : clean(installer.currentVersion()));
            boolean blocked = hasIssue(issues, "MODULE:" + key) || catalog == null || installer == null;
            item.setStatus(blocked ? STATUS_BLOCKED : STATUS_READY);
            item.setDetail(blocked
                    ? firstIssue(issues, "MODULE:" + key, "Instalador no disponible.")
                    : "Instalador detectado y apto para una futura ejecución confirmada.");
            provisioningModuleRepository.save(item);
        }
    }

    private void saveStepPlan(
            Matrix26ProvisioningJob job,
            Set<String> selectedModuleKeys,
            List<ValidationIssue> issues
    ) {
        int order = 10;
        saveStep(job, "validate-identity", order, "Validar identidad técnica",
                blockedByAny(issues, Set.of("IDENTITY", "DATABASE", "RUNTIME", "PORT", "URL")),
                issueDetails(issues, Set.of("IDENTITY", "DATABASE", "RUNTIME", "PORT", "URL"),
                        "Código, base, runtime, puerto y URL no presentan conflictos registrados."),
                "CONTROL_DB_ONLY");
        order += 10;

        saveStep(job, "create-database", order, "Crear base de datos independiente",
                hasIssue(issues, "DATABASE"),
                firstIssue(issues, "DATABASE", "Se crearía la base " + job.getDatabaseName() + " sin copiar datos de otras instancias."),
                "FUTURE_TARGET_DATABASE");
        order += 10;

        saveStep(job, "install-core", order, "Instalar núcleo común",
                false,
                "Seguridad, configuración base, roles y registro técnico mínimo.",
                "FUTURE_TARGET_DATABASE");
        order += 10;

        saveStep(job, "create-admin", order, "Crear administrador inicial",
                false,
                "Se crearía el usuario " + job.getAdminUsername() + ". La contraseña no se almacena en este Dry Run.",
                "FUTURE_TARGET_DATABASE");
        order += 10;

        Map<String, PlatformModuleCatalog> modules = activeModulesByKey();
        for (String key : selectedModuleKeys) {
            PlatformModuleCatalog module = modules.get(key);
            String label = "Instalar módulo " + (module == null ? key : module.getName());
            saveStep(job, "install-module-" + key, order, label,
                    hasIssue(issues, "MODULE:" + key),
                    firstIssue(issues, "MODULE:" + key,
                            "Se ejecutaría el instalador versionado del módulo " + key + "."),
                    "FUTURE_TARGET_DATABASE");
            order += 10;
        }

        boolean runtimeBlocked = blockedByAny(issues, Set.of("RUNTIME", "PORT", "URL"));
        saveStep(job, "generate-runtime", order, "Generar configuración runtime",
                runtimeBlocked,
                issueDetails(issues, Set.of("RUNTIME", "PORT", "URL"),
                        "Se generaría el perfil " + job.getRuntimeProfile() + " para el puerto " + job.getRuntimePort() + "."),
                "FUTURE_RUNTIME_FILES");
        order += 10;

        saveStep(job, "register-instance", order, "Registrar instancia en Matrix26",
                !issues.isEmpty(),
                issues.isEmpty()
                        ? "La instancia quedaría registrada solo después de completar todas las operaciones reales."
                        : "El registro final permanecería bloqueado hasta resolver todas las validaciones.",
                "CONTROL_DB_ONLY");
        order += 10;

        saveStep(job, "health-check", order, "Validar disponibilidad del portal",
                !issues.isEmpty(),
                issues.isEmpty()
                        ? "Matrix26 comprobaría la URL después de iniciar el nuevo runtime."
                        : "La comprobación se ejecutaría únicamente después de corregir el plan y completar el aprovisionamiento.",
                "FUTURE_HTTP_CHECK");
    }

    private void saveStep(
            Matrix26ProvisioningJob job,
            String code,
            int order,
            String label,
            boolean blocked,
            String detail,
            String safetyScope
    ) {
        Matrix26ProvisioningStep step = new Matrix26ProvisioningStep();
        step.setJob(job);
        step.setStepCode(code);
        step.setDisplayOrder(order);
        step.setLabel(label);
        step.setStatus(blocked ? STATUS_BLOCKED : STATUS_READY);
        step.setDetail(detail);
        step.setSafetyScope(safetyScope);
        stepRepository.save(step);
    }

    private void applyForm(Matrix26ProvisioningJob job, Matrix26ProvisioningPlanForm form) {
        job.setBusinessName(clean(form.getBusinessName()));
        job.setLegalName(cleanToNull(form.getLegalName()));
        job.setBusinessType(cleanToNull(form.getBusinessType()));
        job.setInstanceCode(clean(form.getInstanceCode()).toLowerCase(Locale.ROOT));
        job.setDatabaseName(clean(form.getDatabaseName()).toLowerCase(Locale.ROOT));
        job.setRuntimeProfile(clean(form.getRuntimeProfile()));
        job.setRuntimePort(form.getRuntimePort());
        job.setPublicUrl(normalizeUrl(form.getPublicUrl()));
        job.setCity(defaultValue(form.getCity(), "Iquitos"));
        job.setAdminUsername(clean(form.getAdminUsername()));
        job.setAdminEmail(cleanToNull(form.getAdminEmail()));
        job.setDemoDataEnabled(form.isDemoDataEnabled());
        job.setNotes(cleanToNull(form.getNotes()));
        job.setStatus("VALIDATING");
    }

    private void saveAudit(Matrix26ProvisioningJob job, String action, String actor) {
        Matrix26InstanceAuditLog log = new Matrix26InstanceAuditLog();
        log.setAction(action);
        log.setActorUsername(defaultValue(actor, "system"));
        log.setSummary("Dry Run " + job.getReferenceCode() + " guardado con estado " + job.getStatus() + ".");
        log.setAfterSnapshot("instanceCode=" + job.getInstanceCode()
                + ";database=" + job.getDatabaseName()
                + ";runtime=" + job.getRuntimeProfile()
                + ";port=" + job.getRuntimePort()
                + ";status=" + job.getStatus());
        auditRepository.save(log);
    }

    private Matrix26ProvisioningJob getJob(Long id) {
        return jobRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("El plan de aprovisionamiento no existe."));
    }

    private Map<String, PlatformModuleCatalog> activeModulesByKey() {
        Map<String, PlatformModuleCatalog> result = new LinkedHashMap<>();
        for (PlatformModuleCatalog module : moduleRepository.findAllByActiveTrueOrderByAreaAscDisplayOrderAscNameAsc()) {
            result.put(normalizeKey(module.getModuleKey()), module);
        }
        return result;
    }

    private Set<String> normalizedModuleKeys(Collection<String> moduleKeys) {
        Set<String> result = new LinkedHashSet<>();
        if (moduleKeys == null) {
            return result;
        }
        moduleKeys.stream()
                .map(this::normalizeKey)
                .filter(value -> !value.isBlank())
                .filter(value -> !CORE_MODULE_KEY.equals(value))
                .sorted(Comparator.naturalOrder())
                .forEach(result::add);
        return result;
    }

    private int nextSuggestedPort() {
        Set<Integer> used = clientRepository.findAllByOrderByBusinessNameAsc().stream()
                .map(client -> client.getRuntimePort())
                .filter(port -> port != null)
                .collect(LinkedHashSet::new, Set::add, Set::addAll);
        Integer controlPort = portOf(properties.getPortalUrl());
        if (controlPort != null) {
            used.add(controlPort);
        }
        for (int port = 8092; port <= 8999; port++) {
            if (!used.contains(port)) {
                return port;
            }
        }
        return 9000;
    }

    private boolean blockedByAny(List<ValidationIssue> issues, Set<String> codes) {
        return issues.stream().anyMatch(issue -> codes.contains(issue.code()));
    }

    private boolean hasIssue(List<ValidationIssue> issues, String code) {
        return issues.stream().anyMatch(issue -> code.equals(issue.code()));
    }

    private String firstIssue(List<ValidationIssue> issues, String code, String fallback) {
        return issues.stream()
                .filter(issue -> code.equals(issue.code()))
                .map(ValidationIssue::message)
                .findFirst()
                .orElse(fallback);
    }

    private String issueDetails(List<ValidationIssue> issues, Set<String> codes, String fallback) {
        String details = issues.stream()
                .filter(issue -> codes.contains(issue.code()))
                .map(ValidationIssue::message)
                .distinct()
                .reduce((left, right) -> left + " " + right)
                .orElse("");
        return details.isBlank() ? fallback : details;
    }

    private boolean isValidHttpUrl(String value) {
        try {
            URI uri = URI.create(clean(value));
            return ("http".equalsIgnoreCase(uri.getScheme()) || "https".equalsIgnoreCase(uri.getScheme()))
                    && uri.getHost() != null
                    && !uri.getHost().isBlank();
        } catch (IllegalArgumentException ex) {
            return false;
        }
    }

    private Integer portOf(String url) {
        try {
            URI uri = URI.create(clean(url));
            if (uri.getPort() >= 0) {
                return uri.getPort();
            }
            if ("https".equalsIgnoreCase(uri.getScheme())) {
                return 443;
            }
            if ("http".equalsIgnoreCase(uri.getScheme())) {
                return 80;
            }
        } catch (IllegalArgumentException ignored) {
            return null;
        }
        return null;
    }

    private boolean sameUrl(String left, String right) {
        return normalizeUrl(left).equalsIgnoreCase(normalizeUrl(right));
    }

    private String normalizeUrl(String value) {
        String cleaned = clean(value);
        while (cleaned.endsWith("/")) {
            cleaned = cleaned.substring(0, cleaned.length() - 1);
        }
        return cleaned;
    }

    private String generateReference() {
        return "DRY-"
                + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"))
                + "-"
                + UUID.randomUUID().toString().substring(0, 6).toUpperCase(Locale.ROOT);
    }

    private String normalizeKey(String value) {
        return clean(value).toLowerCase(Locale.ROOT);
    }

    private String clean(String value) {
        return value == null ? "" : value.trim();
    }

    private String cleanToNull(String value) {
        String cleaned = clean(value);
        return cleaned.isBlank() ? null : cleaned;
    }

    private String defaultValue(String value, String fallback) {
        String cleaned = clean(value);
        return cleaned.isBlank() ? fallback : cleaned;
    }

    private record ValidationIssue(String code, String message) {
    }
}
