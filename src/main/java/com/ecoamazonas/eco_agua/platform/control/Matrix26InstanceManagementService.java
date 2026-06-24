package com.ecoamazonas.eco_agua.platform.control;

import com.ecoamazonas.eco_agua.platform.PlatformBusinessClient;
import com.ecoamazonas.eco_agua.platform.PlatformBusinessClientRepository;
import com.ecoamazonas.eco_agua.platform.PlatformClientModule;
import com.ecoamazonas.eco_agua.platform.PlatformClientModuleRepository;
import com.ecoamazonas.eco_agua.platform.PlatformModuleCatalog;
import com.ecoamazonas.eco_agua.platform.PlatformModuleCatalogRepository;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@ConditionalOnProperty(name = "matrix26.control-center.enabled", havingValue = "true")
public class Matrix26InstanceManagementService {

    private final PlatformBusinessClientRepository clientRepository;
    private final PlatformModuleCatalogRepository moduleRepository;
    private final PlatformClientModuleRepository clientModuleRepository;
    private final Matrix26InstanceAuditLogRepository auditRepository;

    public Matrix26InstanceManagementService(
            PlatformBusinessClientRepository clientRepository,
            PlatformModuleCatalogRepository moduleRepository,
            PlatformClientModuleRepository clientModuleRepository,
            Matrix26InstanceAuditLogRepository auditRepository
    ) {
        this.clientRepository = clientRepository;
        this.moduleRepository = moduleRepository;
        this.clientModuleRepository = clientModuleRepository;
        this.auditRepository = auditRepository;
    }

    public List<PlatformBusinessClient> listInstances() {
        return clientRepository.findAllByOrderByBusinessNameAsc();
    }

    public PlatformBusinessClient getInstance(Long id) {
        return clientRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("La instancia no existe."));
    }

    public Matrix26InstanceForm newForm() {
        Matrix26InstanceForm form = new Matrix26InstanceForm();
        form.setCity("Iquitos");
        form.setStatus("ACTIVE");
        form.setManagementMode("REGISTERED");
        form.setMonitorVisible(true);
        form.setPrimaryColor("#2563eb");
        return form;
    }

    public Matrix26InstanceForm editForm(Long id) {
        PlatformBusinessClient client = getInstance(id);
        Matrix26InstanceForm form = new Matrix26InstanceForm();
        form.setCode(client.getCode());
        form.setBusinessName(client.getBusinessName());
        form.setLegalName(client.getLegalName());
        form.setBusinessType(client.getBusinessType());
        form.setDatabaseName(client.getDatabaseName());
        form.setRuntimeProfile(client.getRuntimeProfile());
        form.setRuntimePort(client.getRuntimePort());
        form.setPublicUrl(client.getPublicUrl());
        form.setRuntimeCommand(client.getRuntimeCommand());
        form.setStatus(defaultValue(client.getStatus(), "ACTIVE"));
        form.setManagementMode(defaultValue(client.getManagementMode(), "REGISTERED"));
        form.setCity(defaultValue(client.getCity(), "Iquitos"));
        form.setPrimaryColor(defaultValue(client.getPrimaryColor(), "#2563eb"));
        form.setMonitorVisible(client.isMonitorVisible());
        form.setProtectedInstance(client.isProtectedInstance());
        form.setNotes(client.getNotes());
        return form;
    }

    public List<PlatformModuleCatalog> listModules() {
        return moduleRepository.findAllByActiveTrueOrderByAreaAscDisplayOrderAscNameAsc();
    }

    public Map<String, List<PlatformModuleCatalog>> groupedModules() {
        Map<String, List<PlatformModuleCatalog>> grouped = new LinkedHashMap<>();
        for (PlatformModuleCatalog module : listModules()) {
            grouped.computeIfAbsent(module.getArea(), ignored -> new ArrayList<>()).add(module);
        }
        return grouped;
    }

    public Set<String> assignedModuleKeys(Long instanceId) {
        return clientModuleRepository.findClientModules(instanceId).stream()
                .filter(PlatformClientModule::isEnabled)
                .map(item -> item.getModule().getModuleKey())
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    public Map<Long, Set<String>> assignedModuleKeysByInstance(Collection<PlatformBusinessClient> instances) {
        Map<Long, Set<String>> result = new LinkedHashMap<>();
        for (PlatformBusinessClient instance : instances) {
            result.put(instance.getId(), assignedModuleKeys(instance.getId()));
        }
        return result;
    }

    public List<Matrix26InstanceAuditLog> recentAudit() {
        return auditRepository.findTop100ByOrderByCreatedAtDesc();
    }

    public long auditCount() {
        return auditRepository.count();
    }

    public List<Matrix26InstanceAuditLog> auditForInstance(Long instanceId) {
        return auditRepository.findTop50ByInstance_IdOrderByCreatedAtDesc(instanceId);
    }

    @Transactional
    public PlatformBusinessClient create(
            Matrix26InstanceForm form,
            List<String> selectedModules,
            String actor
    ) {
        String code = clean(form.getCode()).toLowerCase(Locale.ROOT);
        String databaseName = clean(form.getDatabaseName()).toLowerCase(Locale.ROOT);
        String runtimeProfile = clean(form.getRuntimeProfile());
        validateUniqueness(null, code, databaseName, runtimeProfile, form.getRuntimePort());

        PlatformBusinessClient client = new PlatformBusinessClient();
        applyForm(client, form, code, databaseName, runtimeProfile);
        client.setDatabaseStatus("EXTERNAL");
        client.setRuntimeStatus("REGISTERED");
        client.setCurrency("PEN");
        client.setPublicSlug(code);
        PlatformBusinessClient saved = clientRepository.save(client);
        replaceModules(saved, selectedModules, "MATRIX26_PHASE2_CREATE");

        saveAudit(
                saved,
                "INSTANCE_CREATED",
                actor,
                "Instancia registrada en Matrix26.",
                null,
                snapshot(saved, assignedModuleKeys(saved.getId()))
        );
        return saved;
    }

    @Transactional
    public PlatformBusinessClient update(
            Long id,
            Matrix26InstanceForm form,
            List<String> selectedModules,
            String actor
    ) {
        PlatformBusinessClient client = getInstance(id);
        String before = snapshot(client, assignedModuleKeys(id));
        String code = clean(form.getCode()).toLowerCase(Locale.ROOT);
        String databaseName = clean(form.getDatabaseName()).toLowerCase(Locale.ROOT);
        String runtimeProfile = clean(form.getRuntimeProfile());
        validateUniqueness(id, code, databaseName, runtimeProfile, form.getRuntimePort());

        applyForm(client, form, code, databaseName, runtimeProfile);
        PlatformBusinessClient saved = clientRepository.save(client);
        replaceModules(saved, selectedModules, "MATRIX26_PHASE2_UPDATE");

        saveAudit(
                saved,
                "INSTANCE_UPDATED",
                actor,
                "Metadatos de la instancia actualizados.",
                before,
                snapshot(saved, assignedModuleKeys(saved.getId()))
        );
        return saved;
    }

    @Transactional
    public PlatformBusinessClient toggleMonitoring(Long id, String actor) {
        PlatformBusinessClient client = getInstance(id);
        String before = snapshot(client, assignedModuleKeys(id));
        client.setMonitorVisible(!client.isMonitorVisible());
        PlatformBusinessClient saved = clientRepository.save(client);
        saveAudit(
                saved,
                saved.isMonitorVisible() ? "MONITORING_ENABLED" : "MONITORING_DISABLED",
                actor,
                saved.isMonitorVisible() ? "Monitoreo automático activado." : "Monitoreo automático pausado.",
                before,
                snapshot(saved, assignedModuleKeys(id))
        );
        return saved;
    }

    @Transactional
    public PlatformBusinessClient toggleProtection(Long id, String actor) {
        PlatformBusinessClient client = getInstance(id);
        String before = snapshot(client, assignedModuleKeys(id));
        client.setProtectedInstance(!client.isProtectedInstance());
        client.setManagementMode(client.isProtectedInstance() ? "PROTECTED" : "REGISTERED");
        PlatformBusinessClient saved = clientRepository.save(client);
        saveAudit(
                saved,
                saved.isProtectedInstance() ? "PROTECTION_ENABLED" : "PROTECTION_DISABLED",
                actor,
                saved.isProtectedInstance() ? "Instancia marcada como protegida." : "Protección administrativa desactivada.",
                before,
                snapshot(saved, assignedModuleKeys(id))
        );
        return saved;
    }

    @Transactional
    public void updateModules(Long id, List<String> selectedModules, String actor) {
        PlatformBusinessClient client = getInstance(id);
        Set<String> beforeKeys = assignedModuleKeys(id);
        replaceModules(client, selectedModules, "MATRIX26_PHASE2_MODULE_UPDATE");
        Set<String> afterKeys = assignedModuleKeys(id);
        saveAudit(
                client,
                "MODULE_DECLARATIONS_UPDATED",
                actor,
                "Declaraciones de módulos actualizadas. No se instaló ni desinstaló código en el portal operativo.",
                "modules=" + String.join(",", beforeKeys),
                "modules=" + String.join(",", afterKeys)
        );
    }

    @Transactional
    public void recordManualHealthCheck(Long id, String actor, Matrix26InstanceStatus status) {
        PlatformBusinessClient client = getInstance(id);
        saveAudit(
                client,
                "MANUAL_HEALTH_CHECK",
                actor,
                "Comprobación manual ejecutada: " + status.statusLabel() + ".",
                null,
                "httpStatus=" + value(status.httpStatus())
                        + ";responseTimeMs=" + value(status.responseTimeMs())
                        + ";message=" + clean(status.message())
        );
    }

    private void applyForm(
            PlatformBusinessClient client,
            Matrix26InstanceForm form,
            String code,
            String databaseName,
            String runtimeProfile
    ) {
        client.setCode(code);
        client.setBusinessName(clean(form.getBusinessName()));
        client.setLegalName(cleanToNull(form.getLegalName()));
        client.setBusinessType(cleanToNull(form.getBusinessType()));
        client.setDatabaseName(databaseName);
        client.setRuntimeProfile(runtimeProfile);
        client.setRuntimePort(form.getRuntimePort());
        client.setPublicUrl(clean(form.getPublicUrl()));
        client.setRuntimeCommand(cleanToNull(form.getRuntimeCommand()));
        client.setStatus(clean(form.getStatus()));
        String managementMode = clean(form.getManagementMode());
        if (!form.isProtectedInstance() && "PROTECTED".equals(managementMode)) {
            managementMode = "REGISTERED";
        }
        client.setManagementMode(form.isProtectedInstance() ? "PROTECTED" : managementMode);
        client.setCity(defaultValue(form.getCity(), "Iquitos"));
        client.setPrimaryColor(defaultValue(form.getPrimaryColor(), "#2563eb"));
        client.setMonitorVisible(form.isMonitorVisible());
        client.setProtectedInstance(form.isProtectedInstance());
        client.setNotes(cleanToNull(form.getNotes()));
    }

    private void validateUniqueness(
            Long currentId,
            String code,
            String databaseName,
            String runtimeProfile,
            Integer runtimePort
    ) {
        boolean codeExists = currentId == null
                ? clientRepository.existsByCodeIgnoreCase(code)
                : clientRepository.existsByCodeIgnoreCaseAndIdNot(code, currentId);
        if (codeExists) {
            throw new IllegalArgumentException("Ya existe una instancia con el código " + code + ".");
        }

        boolean portExists = currentId == null
                ? clientRepository.existsByRuntimePort(runtimePort)
                : clientRepository.existsByRuntimePortAndIdNot(runtimePort, currentId);
        if (portExists) {
            throw new IllegalArgumentException("El puerto " + runtimePort + " ya está registrado en otra instancia.");
        }

        boolean databaseExists = currentId == null
                ? clientRepository.existsByDatabaseNameIgnoreCase(databaseName)
                : clientRepository.existsByDatabaseNameIgnoreCaseAndIdNot(databaseName, currentId);
        if (databaseExists) {
            throw new IllegalArgumentException("La base " + databaseName + " ya está registrada en otra instancia.");
        }

        boolean profileExists = currentId == null
                ? clientRepository.existsByRuntimeProfileIgnoreCase(runtimeProfile)
                : clientRepository.existsByRuntimeProfileIgnoreCaseAndIdNot(runtimeProfile, currentId);
        if (profileExists) {
            throw new IllegalArgumentException("El runtime " + runtimeProfile + " ya está registrado en otra instancia.");
        }
    }

    private void replaceModules(PlatformBusinessClient client, List<String> selectedModules, String source) {
        List<PlatformClientModule> current = clientModuleRepository.findClientModules(client.getId());
        clientModuleRepository.deleteAllInBatch(current);

        Set<String> moduleKeys = selectedModules == null
                ? Set.of()
                : selectedModules.stream()
                .map(this::clean)
                .filter(value -> !value.isBlank())
                .collect(Collectors.toCollection(LinkedHashSet::new));

        for (String moduleKey : moduleKeys) {
            PlatformModuleCatalog module = moduleRepository.findByModuleKey(moduleKey)
                    .orElseThrow(() -> new IllegalArgumentException("Módulo no encontrado: " + moduleKey));
            if (!module.isActive()) {
                continue;
            }
            PlatformClientModule assignment = new PlatformClientModule();
            assignment.setClient(client);
            assignment.setModule(module);
            assignment.setEnabled(true);
            assignment.setSelectionSource(source);
            assignment.setNotes("Matrix26 module declaration only; operational installation is unchanged.");
            clientModuleRepository.save(assignment);
        }
    }

    private void saveAudit(
            PlatformBusinessClient instance,
            String action,
            String actor,
            String summary,
            String before,
            String after
    ) {
        Matrix26InstanceAuditLog log = new Matrix26InstanceAuditLog();
        log.setInstance(instance);
        log.setAction(action);
        log.setActorUsername(defaultValue(actor, "system"));
        log.setSummary(summary);
        log.setBeforeSnapshot(before);
        log.setAfterSnapshot(after);
        auditRepository.save(log);
    }

    private String snapshot(PlatformBusinessClient client, Set<String> moduleKeys) {
        return "code=" + clean(client.getCode())
                + ";name=" + clean(client.getBusinessName())
                + ";database=" + clean(client.getDatabaseName())
                + ";runtime=" + clean(client.getRuntimeProfile())
                + ";port=" + value(client.getRuntimePort())
                + ";url=" + clean(client.getPublicUrl())
                + ";status=" + clean(client.getStatus())
                + ";managementMode=" + clean(client.getManagementMode())
                + ";monitorVisible=" + client.isMonitorVisible()
                + ";protected=" + client.isProtectedInstance()
                + ";modules=" + String.join(",", moduleKeys);
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

    private String value(Object value) {
        return value == null ? "" : String.valueOf(value);
    }
}
