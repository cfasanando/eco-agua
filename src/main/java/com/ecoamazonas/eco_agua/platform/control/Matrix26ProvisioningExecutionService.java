package com.ecoamazonas.eco_agua.platform.control;

import com.ecoamazonas.eco_agua.platform.PlatformBusinessClient;
import com.ecoamazonas.eco_agua.platform.PlatformBusinessClientRepository;
import com.ecoamazonas.eco_agua.platform.PlatformClientModule;
import com.ecoamazonas.eco_agua.platform.PlatformClientModuleRepository;
import com.ecoamazonas.eco_agua.platform.PlatformModuleCatalog;
import com.ecoamazonas.eco_agua.platform.PlatformModuleCatalogRepository;
import com.ecoamazonas.eco_agua.platform.module.PlatformModuleInstaller;
import com.ecoamazonas.eco_agua.platform.control.appearance.Matrix26ProvisioningAppearanceService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

@Service
@ConditionalOnProperty(name = "matrix26.control-center.enabled", havingValue = "true")
public class Matrix26ProvisioningExecutionService {

    private static final String STATUS_READY = "READY";
    private static final String STATUS_RUNNING = "RUNNING";
    private static final String STATUS_COMPLETED = "COMPLETED";
    private static final String STATUS_FAILED = "FAILED";
    private static final String STEP_COMPLETED = "COMPLETED";
    private static final String STEP_SKIPPED = "SKIPPED";
    private static final int MODULE_DETAIL_MAX_LENGTH = 4000;
    private static final int AUDIT_SUMMARY_MAX_LENGTH = 500;

    private final Matrix26ControlCenterProperties properties;
    private final Matrix26ProvisioningService provisioningService;
    private final Matrix26ProvisioningJobRepository jobRepository;
    private final Matrix26ProvisioningStepRepository stepRepository;
    private final Matrix26ProvisioningModuleRepository provisioningModuleRepository;
    private final Matrix26TargetDatabaseService targetDatabaseService;
    private final PlatformBusinessClientRepository clientRepository;
    private final PlatformModuleCatalogRepository moduleCatalogRepository;
    private final PlatformClientModuleRepository clientModuleRepository;
    private final Matrix26InstanceHealthService healthService;
    private final Matrix26InstanceAuditLogRepository auditRepository;
    private final Matrix26ProvisioningAppearanceService appearanceService;
    private final PasswordEncoder passwordEncoder;
    private final Map<String, PlatformModuleInstaller> installers = new LinkedHashMap<>();
    private final Map<Long, ReentrantLock> executionLocks = new ConcurrentHashMap<>();

    public Matrix26ProvisioningExecutionService(
            Matrix26ControlCenterProperties properties,
            Matrix26ProvisioningService provisioningService,
            Matrix26ProvisioningJobRepository jobRepository,
            Matrix26ProvisioningStepRepository stepRepository,
            Matrix26ProvisioningModuleRepository provisioningModuleRepository,
            Matrix26TargetDatabaseService targetDatabaseService,
            PlatformBusinessClientRepository clientRepository,
            PlatformModuleCatalogRepository moduleCatalogRepository,
            PlatformClientModuleRepository clientModuleRepository,
            Matrix26InstanceHealthService healthService,
            Matrix26InstanceAuditLogRepository auditRepository,
            Matrix26ProvisioningAppearanceService appearanceService,
            PasswordEncoder passwordEncoder,
            List<PlatformModuleInstaller> installerList
    ) {
        this.properties = properties;
        this.provisioningService = provisioningService;
        this.jobRepository = jobRepository;
        this.stepRepository = stepRepository;
        this.provisioningModuleRepository = provisioningModuleRepository;
        this.targetDatabaseService = targetDatabaseService;
        this.clientRepository = clientRepository;
        this.moduleCatalogRepository = moduleCatalogRepository;
        this.clientModuleRepository = clientModuleRepository;
        this.healthService = healthService;
        this.auditRepository = auditRepository;
        this.appearanceService = appearanceService;
        this.passwordEncoder = passwordEncoder;
        for (PlatformModuleInstaller installer : installerList) {
            installers.put(normalizeKey(installer.moduleKey()), installer);
        }
    }

    public boolean canExecute(Matrix26ProvisioningJob job) {
        if (!properties.isProvisioningExecutionEnabled() || job == null) {
            return false;
        }
        return STATUS_READY.equals(job.getStatus()) || STATUS_FAILED.equals(job.getStatus());
    }

    public boolean isExecutionEnabled() {
        return properties.isProvisioningExecutionEnabled();
    }

    public Matrix26ProvisioningJob execute(
            Long jobId,
            Matrix26ProvisioningExecutionForm form,
            String actor
    ) {
        ReentrantLock lock = executionLocks.computeIfAbsent(jobId, ignored -> new ReentrantLock());
        if (!lock.tryLock()) {
            throw new IllegalArgumentException("Este plan ya está siendo ejecutado por otro proceso.");
        }

        try {
            assertExecutionEnabled();
            Matrix26ProvisioningJob job = requiredJob(jobId);
            validateConfirmation(job, form);

            if (STATUS_COMPLETED.equals(job.getStatus())) {
                return job;
            }
            if (STATUS_RUNNING.equals(job.getStatus())) {
                throw new IllegalArgumentException("El plan ya se encuentra en ejecución.");
            }
            if (!STATUS_READY.equals(job.getStatus()) && !STATUS_FAILED.equals(job.getStatus())) {
                throw new IllegalArgumentException("Solo se pueden ejecutar planes LISTOS o reintentar planes FALLIDOS.");
            }

            if (STATUS_READY.equals(job.getStatus())) {
                job = provisioningService.revalidate(jobId, actor);
                if (!STATUS_READY.equals(job.getStatus())) {
                    throw new IllegalArgumentException("El plan cambió y ya no está listo. Revisa las validaciones.");
                }
            }

            final Matrix26ProvisioningJob executionJob = job;
            boolean retrying = STATUS_FAILED.equals(executionJob.getStatus());
            Set<String> moduleKeys = selectedModuleKeys(jobId);
            validateTargetInstallers(moduleKeys);
            if (retrying) {
                resetAdministratorStepForRetry(jobId);
            }
            startJob(executionJob, actor);

            runStep(executionJob, "validate-identity", () -> "Identidad técnica revalidada antes de la ejecución real.");
            runStep(executionJob, "create-database", () -> {
                targetDatabaseService.createDatabase(executionJob.getDatabaseName(), isStepCompleted(jobId, "create-database"));
                return "Base independiente creada: " + executionJob.getDatabaseName() + ".";
            });
            runStep(executionJob, "install-core", () -> {
                try {
                    int copied = targetDatabaseService.installCompatibleCore(executionJob, moduleKeys);
                    updateModuleExecution(jobId, "core", STEP_COMPLETED,
                            "Núcleo compatible instalado sin copiar datos operativos.");
                    return "Núcleo compatible instalado sin copiar datos operativos. Tablas estructurales: " + copied + ".";
                } catch (RuntimeException ex) {
                    updateModuleExecution(jobId, "core", STATUS_FAILED, cleanError(ex));
                    throw ex;
                }
            });
            runStep(executionJob, "create-admin", () -> {
                targetDatabaseService.createAdministrator(executionJob, passwordEncoder.encode(form.getAdminPassword()));
                return "Administrador inicial creado: " + executionJob.getAdminUsername() + ". La contraseña no fue almacenada en Matrix26.";
            });

            for (String moduleKey : moduleKeys) {
                runStep(executionJob, "install-module-" + moduleKey, () -> {
                    PlatformModuleInstaller installer = requiredTargetInstaller(moduleKey);
                    updateModuleExecution(jobId, moduleKey, STATUS_RUNNING,
                            "Ejecutando instalador " + installer.currentVersion() + ".");
                    try {
                        installer.installOnTarget(
                                targetDatabaseService.targetJdbcTemplate(executionJob.getDatabaseName()),
                                executionJob.isDemoDataEnabled()
                        );
                        updateModuleExecution(jobId, moduleKey, STEP_COMPLETED,
                                "Instalador " + installer.currentVersion() + " ejecutado correctamente.");
                        return "Módulo " + installer.displayName() + " instalado y activado.";
                    } catch (RuntimeException ex) {
                        updateModuleExecution(jobId, moduleKey, STATUS_FAILED, cleanError(ex));
                        throw ex;
                    }
                });
            }

            targetDatabaseService.applyFinalBusinessSettings(executionJob, moduleKeys);

            runStep(executionJob, "install-appearance", () ->
                    appearanceService.installOnTarget(executionJob, actor)
            );

            runStep(executionJob, "generate-runtime", () -> {
                String runtimeFolder = targetDatabaseService.generateRuntimeFiles(executionJob, moduleKeys);
                executionJob.setRuntimeFolder(runtimeFolder);
                jobRepository.saveAndFlush(executionJob);
                return "Runtime generado en " + runtimeFolder + ".";
            });

            PlatformBusinessClient instance = registerInstanceStep(executionJob, moduleKeys);
            runStep(executionJob, "register-appearance", () -> {
                appearanceService.registerCentralAppearance(instance, executionJob, actor);
                return "Apariencia inicial v1 registrada en Appearance Studio.";
            });
            processHealthCheck(executionJob, instance);

            completeJob(executionJob, actor, instance);
            return requiredJob(jobId);
        } catch (RuntimeException ex) {
            markJobFailed(jobId, actor, ex);
            throw ex;
        } finally {
            lock.unlock();
            if (!lock.hasQueuedThreads()) {
                executionLocks.remove(jobId, lock);
            }
        }
    }

    private void resetAdministratorStepForRetry(Long jobId) {
        Matrix26ProvisioningStep step = requiredStep(jobId, "create-admin");
        if (STEP_COMPLETED.equals(step.getStatus()) || STEP_SKIPPED.equals(step.getStatus())) {
            step.setStatus(STATUS_READY);
            step.setDetail("La credencial inicial se aplicará nuevamente durante el reintento.");
            step.setStartedAt(null);
            step.setCompletedAt(null);
            step.setLastError(null);
            stepRepository.saveAndFlush(step);
        }
    }

    private void runStep(Matrix26ProvisioningJob job, String stepCode, StepAction action) {
        Matrix26ProvisioningStep step = requiredStep(job.getId(), stepCode);
        if (STEP_COMPLETED.equals(step.getStatus()) || STEP_SKIPPED.equals(step.getStatus())) {
            return;
        }

        step.setStatus(STATUS_RUNNING);
        step.setStartedAt(LocalDateTime.now());
        step.setCompletedAt(null);
        step.setLastError(null);
        step.setAttemptCount(step.getAttemptCount() + 1);
        stepRepository.saveAndFlush(step);

        try {
            String detail = action.run();
            step.setStatus(STEP_COMPLETED);
            step.setDetail(detail);
            step.setCompletedAt(LocalDateTime.now());
            step.setLastError(null);
            stepRepository.saveAndFlush(step);
        } catch (RuntimeException ex) {
            step.setStatus(STATUS_FAILED);
            step.setLastError(cleanError(ex));
            step.setDetail("Falló el paso. Puedes corregir la causa y reintentar el mismo plan.");
            step.setCompletedAt(LocalDateTime.now());
            stepRepository.saveAndFlush(step);
            throw ex;
        } catch (Exception ex) {
            step.setStatus(STATUS_FAILED);
            step.setLastError(cleanError(ex));
            step.setDetail("Falló el paso. Puedes corregir la causa y reintentar el mismo plan.");
            step.setCompletedAt(LocalDateTime.now());
            stepRepository.saveAndFlush(step);
            throw new IllegalArgumentException(cleanError(ex), ex);
        }
    }


    private PlatformBusinessClient registerInstanceStep(Matrix26ProvisioningJob job, Set<String> moduleKeys) {
        Matrix26ProvisioningStep step = requiredStep(job.getId(), "register-instance");
        if (STEP_COMPLETED.equals(step.getStatus()) || STEP_SKIPPED.equals(step.getStatus())) {
            if (job.getRegisteredInstanceId() != null) {
                return clientRepository.findById(job.getRegisteredInstanceId())
                        .orElseThrow(() -> new IllegalStateException("La instancia registrada ya no existe."));
            }
            return clientRepository.findByCodeIgnoreCase(job.getInstanceCode())
                    .orElseThrow(() -> new IllegalStateException("No se encontró la instancia registrada por el plan."));
        }

        step.setStatus(STATUS_RUNNING);
        step.setStartedAt(LocalDateTime.now());
        step.setCompletedAt(null);
        step.setLastError(null);
        step.setAttemptCount(step.getAttemptCount() + 1);
        stepRepository.saveAndFlush(step);

        try {
            PlatformBusinessClient instance = registerInstance(job, moduleKeys);
            step.setStatus(STEP_COMPLETED);
            step.setDetail("Instancia registrada en Matrix26 con ID " + instance.getId() + ".");
            step.setCompletedAt(LocalDateTime.now());
            stepRepository.saveAndFlush(step);
            return instance;
        } catch (RuntimeException ex) {
            step.setStatus(STATUS_FAILED);
            step.setLastError(cleanError(ex));
            step.setDetail("Falló el registro central. Puedes corregir la causa y reintentar el plan.");
            step.setCompletedAt(LocalDateTime.now());
            stepRepository.saveAndFlush(step);
            throw ex;
        }
    }

    private void processHealthCheck(Matrix26ProvisioningJob job, PlatformBusinessClient instance) {
        Matrix26ProvisioningStep step = requiredStep(job.getId(), "health-check");
        if (STEP_COMPLETED.equals(step.getStatus()) || STEP_SKIPPED.equals(step.getStatus())) {
            return;
        }

        step.setStatus(STATUS_RUNNING);
        step.setStartedAt(LocalDateTime.now());
        step.setAttemptCount(step.getAttemptCount() + 1);
        stepRepository.saveAndFlush(step);

        Matrix26InstanceStatus status = healthService.refreshInstance(instance.getId());
        step.setCompletedAt(LocalDateTime.now());
        if (status.online()) {
            step.setStatus(STEP_COMPLETED);
            step.setDetail("La nueva instancia respondió en " + status.responseTimeMs() + " ms.");
            instance.setRuntimeStatus("ONLINE");
            instance.setStatus("ACTIVE");
        } else {
            step.setStatus(STEP_SKIPPED);
            step.setDetail("Runtime generado, pero todavía no está iniciado. Ejecuta el run.sh y usa Comprobar ahora desde Matrix26.");
            instance.setRuntimeStatus("GENERATED");
            instance.setStatus("PROVISIONED");
        }
        step.setLastError(null);
        stepRepository.saveAndFlush(step);
        clientRepository.saveAndFlush(instance);
    }

    private PlatformBusinessClient registerInstance(Matrix26ProvisioningJob job, Set<String> moduleKeys) {
        PlatformBusinessClient instance;
        if (job.getRegisteredInstanceId() != null) {
            instance = clientRepository.findById(job.getRegisteredInstanceId())
                    .orElseThrow(() -> new IllegalStateException("El registro de instancia asociado ya no existe."));
        } else {
            instance = clientRepository.findByCodeIgnoreCase(job.getInstanceCode()).orElseGet(PlatformBusinessClient::new);
        }

        instance.setCode(job.getInstanceCode());
        instance.setBusinessName(job.getBusinessName());
        instance.setLegalName(job.getLegalName());
        instance.setBusinessType(job.getBusinessType());
        instance.setDatabaseName(job.getDatabaseName());
        instance.setDatabaseStatus("READY");
        instance.setStatus("PROVISIONED");
        instance.setContactEmail(job.getAdminEmail());
        instance.setCity(job.getCity());
        instance.setCurrency("PEN");
        instance.setPublicSlug(job.getInstanceCode());
        instance.setDemoDataEnabled(job.isDemoDataEnabled());
        instance.setRuntimeProfile(job.getRuntimeProfile());
        instance.setRuntimePort(job.getRuntimePort());
        instance.setPublicUrl(job.getPublicUrl());
        instance.setRuntimeStatus("GENERATED");
        instance.setManagementMode("MATRIX26_MANAGED");
        instance.setMonitorVisible(true);
        instance.setProtectedInstance(true);
        instance.setRuntimeCommand("bash runtime-clients/" + job.getRuntimeProfile() + "/run.sh");
        instance.setLastRuntimeGeneratedAt(LocalDateTime.now());
        instance.setNotes("Provisioned by Matrix26. The instance is protected by default after creation.");
        PlatformBusinessClient saved = clientRepository.saveAndFlush(instance);

        Set<String> desiredKeys = new LinkedHashSet<>();
        desiredKeys.add("core");
        desiredKeys.addAll(moduleKeys);
        Map<String, PlatformClientModule> existing = new LinkedHashMap<>();
        for (PlatformClientModule assignment : clientModuleRepository.findClientModules(saved.getId())) {
            existing.put(normalizeKey(assignment.getModule().getModuleKey()), assignment);
        }
        for (String moduleKey : desiredKeys) {
            PlatformModuleCatalog module = moduleCatalogRepository.findByModuleKey(moduleKey)
                    .orElseThrow(() -> new IllegalStateException("No existe el módulo central " + moduleKey + "."));
            PlatformClientModule assignment = existing.get(moduleKey);
            if (assignment == null) {
                assignment = new PlatformClientModule();
                assignment.setClient(saved);
                assignment.setModule(module);
            }
            assignment.setEnabled(true);
            assignment.setSelectionSource("MATRIX26_PROVISIONING");
            assignment.setNotes("Assigned by provisioning job " + job.getReferenceCode() + ".");
            clientModuleRepository.save(assignment);
        }

        job.setRegisteredInstanceId(saved.getId());
        jobRepository.saveAndFlush(job);
        return saved;
    }

    private void startJob(Matrix26ProvisioningJob job, String actor) {
        job.setStatus(STATUS_RUNNING);
        job.setExecutedBy(defaultValue(actor, "system"));
        if (job.getExecutionStartedAt() == null) {
            job.setExecutionStartedAt(LocalDateTime.now());
        }
        job.setExecutionCompletedAt(null);
        job.setLastError(null);
        jobRepository.saveAndFlush(job);
        saveAudit(job, null, "PROVISIONING_EXECUTION_STARTED", actor,
                "Ejecución real iniciada para " + job.getReferenceCode() + ".");
    }

    private void completeJob(Matrix26ProvisioningJob job, String actor, PlatformBusinessClient instance) {
        job.setStatus(STATUS_COMPLETED);
        job.setExecutionCompletedAt(LocalDateTime.now());
        job.setLastError(null);
        job.setValidationSummary("Aprovisionamiento completado. La base, el runtime y la apariencia inicial fueron generados. La instancia quedó protegida por defecto.");
        jobRepository.saveAndFlush(job);
        saveAudit(job, instance, "PROVISIONING_EXECUTION_COMPLETED", actor,
                "Aprovisionamiento completado para " + instance.getBusinessName() + ".");
    }

    private void markJobFailed(Long jobId, String actor, RuntimeException ex) {
        Matrix26ProvisioningJob job = jobRepository.findById(jobId).orElse(null);
        if (job == null || STATUS_COMPLETED.equals(job.getStatus()) || !STATUS_RUNNING.equals(job.getStatus())) {
            return;
        }
        job.setStatus(STATUS_FAILED);
        job.setLastError(cleanError(ex));
        job.setExecutionCompletedAt(LocalDateTime.now());
        jobRepository.saveAndFlush(job);
        PlatformBusinessClient instance = job.getRegisteredInstanceId() == null
                ? null
                : clientRepository.findById(job.getRegisteredInstanceId()).orElse(null);
        saveAudit(job, instance, "PROVISIONING_EXECUTION_FAILED", actor,
                "Falló el aprovisionamiento " + job.getReferenceCode() + ": " + cleanError(ex));
    }

    private void validateConfirmation(Matrix26ProvisioningJob job, Matrix26ProvisioningExecutionForm form) {
        if (form == null) {
            throw new IllegalArgumentException("Falta la confirmación de ejecución.");
        }
        if (!job.getReferenceCode().equalsIgnoreCase(defaultValue(form.getConfirmationReference(), ""))) {
            throw new IllegalArgumentException("El código de referencia no coincide con el plan.");
        }
        if (!form.isAcknowledged()) {
            throw new IllegalArgumentException("Debes confirmar que revisaste el plan.");
        }
        if (!defaultValue(form.getAdminPassword(), "").equals(defaultValue(form.getAdminPasswordConfirmation(), ""))) {
            throw new IllegalArgumentException("Las contraseñas del administrador no coinciden.");
        }
        if (defaultValue(form.getAdminPassword(), "").length() < 10) {
            throw new IllegalArgumentException("La contraseña inicial debe contener al menos 10 caracteres.");
        }
    }

    private void validateTargetInstallers(Set<String> moduleKeys) {
        for (String moduleKey : moduleKeys) {
            requiredTargetInstaller(moduleKey);
        }
    }

    private PlatformModuleInstaller requiredTargetInstaller(String moduleKey) {
        PlatformModuleInstaller installer = installers.get(normalizeKey(moduleKey));
        if (installer == null || !installer.supportsTargetInstallation()) {
            throw new IllegalArgumentException("El módulo " + moduleKey + " no admite instalación sobre una nueva instancia.");
        }
        return installer;
    }

    private Set<String> selectedModuleKeys(Long jobId) {
        Set<String> keys = new LinkedHashSet<>();
        for (Matrix26ProvisioningModule module : provisioningModuleRepository.findByJob_IdOrderByModuleNameAscIdAsc(jobId)) {
            String key = normalizeKey(module.getModuleKey());
            if (!"core".equals(key)) {
                keys.add(key);
            }
        }
        return keys;
    }

    private void updateModuleExecution(Long jobId, String moduleKey, String status, String detail) {
        for (Matrix26ProvisioningModule module : provisioningModuleRepository.findByJob_IdOrderByModuleNameAscIdAsc(jobId)) {
            if (normalizeKey(module.getModuleKey()).equals(normalizeKey(moduleKey))) {
                module.setStatus(status);
                module.setDetail(limitText(detail, MODULE_DETAIL_MAX_LENGTH));
                provisioningModuleRepository.saveAndFlush(module);
                return;
            }
        }
    }

    private boolean isStepCompleted(Long jobId, String stepCode) {
        return stepRepository.findByJob_IdAndStepCode(jobId, stepCode)
                .map(step -> STEP_COMPLETED.equals(step.getStatus()))
                .orElse(false);
    }

    private Matrix26ProvisioningStep requiredStep(Long jobId, String stepCode) {
        return stepRepository.findByJob_IdAndStepCode(jobId, stepCode)
                .orElseThrow(() -> new IllegalStateException("No existe el paso " + stepCode + " en el plan."));
    }

    private Matrix26ProvisioningJob requiredJob(Long id) {
        return jobRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("El plan de aprovisionamiento no existe."));
    }

    private void assertExecutionEnabled() {
        if (!properties.isProvisioningExecutionEnabled()) {
            throw new IllegalStateException("La ejecución real de aprovisionamiento está deshabilitada en este runtime.");
        }
    }

    private void saveAudit(
            Matrix26ProvisioningJob job,
            PlatformBusinessClient instance,
            String action,
            String actor,
            String summary
    ) {
        Matrix26InstanceAuditLog log = new Matrix26InstanceAuditLog();
        log.setInstance(instance);
        log.setAction(action);
        log.setActorUsername(defaultValue(actor, "system"));
        log.setSummary(limitText(summary, AUDIT_SUMMARY_MAX_LENGTH));
        log.setAfterSnapshot("reference=" + job.getReferenceCode()
                + ";instanceCode=" + job.getInstanceCode()
                + ";database=" + job.getDatabaseName()
                + ";runtime=" + job.getRuntimeProfile()
                + ";port=" + job.getRuntimePort()
                + ";appearancePreset=" + job.getAppearancePresetCode()
                + ";publicTheme=" + job.getPublicThemeCode()
                + ";publicLayout=" + job.getPublicLayoutCode()
                + ";status=" + job.getStatus());
        auditRepository.save(log);
    }

    private String normalizeKey(String value) {
        return defaultValue(value, "").toLowerCase(Locale.ROOT);
    }

    private String defaultValue(String value, String fallback) {
        String clean = value == null ? "" : value.trim();
        return clean.isBlank() ? fallback : clean;
    }

    private String cleanError(Exception ex) {
        String message = ex.getMessage();
        if (message == null || message.isBlank()) {
            message = ex.getClass().getSimpleName();
        }

        Throwable root = ex;
        while (root.getCause() != null && root.getCause() != root) {
            root = root.getCause();
        }
        String rootMessage = root.getMessage();
        if (root != ex && rootMessage != null && !rootMessage.isBlank() && !message.contains(rootMessage)) {
            message = message + " | Root cause: " + rootMessage;
        }

        return limitText(message, 1900);
    }

    private String limitText(String value, int maximumLength) {
        if (value == null) {
            return null;
        }
        String clean = value.trim();
        return clean.length() > maximumLength ? clean.substring(0, maximumLength) : clean;
    }

    @FunctionalInterface
    private interface StepAction {
        String run() throws Exception;
    }
}
