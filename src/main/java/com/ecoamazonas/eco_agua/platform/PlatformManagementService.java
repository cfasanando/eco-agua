package com.ecoamazonas.eco_agua.platform;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class PlatformManagementService {

    private final PlatformBusinessClientRepository clientRepository;
    private final PlatformBusinessTemplateRepository templateRepository;
    private final PlatformModuleCatalogRepository moduleRepository;
    private final PlatformBusinessTemplateModuleRepository templateModuleRepository;
    private final PlatformClientModuleRepository clientModuleRepository;

    public PlatformManagementService(PlatformBusinessClientRepository clientRepository,
                                     PlatformBusinessTemplateRepository templateRepository,
                                     PlatformModuleCatalogRepository moduleRepository,
                                     PlatformBusinessTemplateModuleRepository templateModuleRepository,
                                     PlatformClientModuleRepository clientModuleRepository) {
        this.clientRepository = clientRepository;
        this.templateRepository = templateRepository;
        this.moduleRepository = moduleRepository;
        this.templateModuleRepository = templateModuleRepository;
        this.clientModuleRepository = clientModuleRepository;
    }

    public List<PlatformBusinessClient> listClients() {
        return clientRepository.findAllByOrderByCreatedAtDescIdDesc();
    }

    public List<PlatformBusinessTemplate> listTemplates() {
        return templateRepository.findAllByActiveTrueOrderByDisplayOrderAscNameAsc();
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

    public Map<Long, List<PlatformBusinessTemplateModule>> templateModulesByTemplate() {
        Map<Long, List<PlatformBusinessTemplateModule>> grouped = new LinkedHashMap<>();
        for (PlatformBusinessTemplateModule templateModule : templateModuleRepository.findAllWithTemplateAndModule()) {
            grouped.computeIfAbsent(templateModule.getTemplate().getId(), ignored -> new ArrayList<>()).add(templateModule);
        }
        return grouped;
    }

    public List<PlatformTemplateSummary> templateSummaries() {
        Map<Long, List<PlatformBusinessTemplateModule>> grouped = templateModulesByTemplate();
        return listTemplates().stream()
                .map(template -> {
                    List<PlatformBusinessTemplateModule> modules = grouped.getOrDefault(template.getId(), List.of());
                    long required = modules.stream().filter(PlatformBusinessTemplateModule::isRequired).count();
                    long recommended = modules.stream().filter(PlatformBusinessTemplateModule::isRecommended).count();
                    return new PlatformTemplateSummary(template, recommended, required);
                })
                .toList();
    }

    public PlatformClientSummary buildSummary() {
        List<PlatformBusinessClient> clients = listClients();
        long draft = clients.stream().filter(client -> "DRAFT".equalsIgnoreCase(safe(client.getStatus()))).count();
        long configured = clients.stream().filter(client -> "CONFIGURED".equalsIgnoreCase(safe(client.getStatus()))).count();
        long pendingDatabases = clients.stream().filter(client -> !"CREATED".equalsIgnoreCase(safe(client.getDatabaseStatus()))).count();
        long activeTemplates = listTemplates().size();
        long activeModules = listModules().size();
        return new PlatformClientSummary(clients.size(), draft, configured, pendingDatabases, activeTemplates, activeModules);
    }

    public PlatformClientForm newClientForm(Long templateId) {
        PlatformClientForm form = new PlatformClientForm();
        form.setCurrency("PEN");
        form.setCity("Iquitos");
        if (templateId != null) {
            templateRepository.findById(templateId).ifPresent(template -> {
                form.setTemplateId(template.getId());
                form.setBusinessType(template.getBusinessType());
                form.setCity(defaultValue(template.getDefaultCity(), "Iquitos"));
                form.setCurrency(defaultValue(template.getDefaultCurrency(), "PEN"));
                form.setPrimaryColor(template.getDefaultPrimaryColor());
            });
        }
        return form;
    }

    public Set<String> recommendedModuleKeys(Long templateId) {
        if (templateId == null) {
            return listModules().stream()
                    .filter(PlatformModuleCatalog::isDefaultEnabled)
                    .map(PlatformModuleCatalog::getModuleKey)
                    .collect(Collectors.toCollection(LinkedHashSet::new));
        }

        return templateModuleRepository.findTemplateModules(templateId).stream()
                .filter(PlatformBusinessTemplateModule::isRecommended)
                .map(item -> item.getModule().getModuleKey())
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    public PlatformBusinessClient getClient(Long id) {
        return clientRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Business client not found."));
    }

    public List<PlatformClientModule> getClientModules(Long clientId) {
        return clientModuleRepository.findClientModules(clientId);
    }

    @Transactional
    public PlatformBusinessClient createClient(PlatformClientForm form, List<String> selectedModules) {
        String code = normalizeCode(form.getCode(), form.getBusinessName());
        if (clientRepository.existsByCode(code)) {
            throw new IllegalArgumentException("Ya existe un negocio con el código: " + code);
        }

        PlatformBusinessTemplate template = null;
        if (form.getTemplateId() != null) {
            template = templateRepository.findById(form.getTemplateId())
                    .orElseThrow(() -> new IllegalArgumentException("Plantilla no encontrada."));
        }

        PlatformBusinessClient client = new PlatformBusinessClient();
        client.setCode(code);
        client.setBusinessName(required(form.getBusinessName(), "El nombre comercial es obligatorio."));
        client.setLegalName(clean(form.getLegalName()));
        client.setTemplate(template);
        client.setBusinessType(clean(defaultValue(form.getBusinessType(), template != null ? template.getBusinessType() : "")));
        client.setDatabaseName(normalizeDatabaseName(defaultValue(form.getDatabaseName(), code)));
        client.setDatabaseStatus("PENDING_STRUCTURE");
        client.setStatus("DRAFT");
        client.setOwnerName(clean(form.getOwnerName()));
        client.setContactPhone(clean(form.getContactPhone()));
        client.setContactEmail(clean(form.getContactEmail()));
        client.setCity(clean(defaultValue(form.getCity(), template != null ? template.getDefaultCity() : "Iquitos")));
        client.setCurrency(clean(defaultValue(form.getCurrency(), "PEN")));
        client.setWhatsapp(clean(form.getWhatsapp()));
        client.setPrimaryColor(clean(defaultValue(form.getPrimaryColor(), template != null ? template.getDefaultPrimaryColor() : "#0d6efd")));
        client.setLogoUrl(clean(form.getLogoUrl()));
        client.setPublicSlug(normalizeCode(defaultValue(form.getPublicSlug(), code), code));
        client.setDemoDataEnabled(form.isDemoDataEnabled());
        client.setNotes(clean(form.getNotes()));

        PlatformBusinessClient saved = clientRepository.save(client);
        saveClientModules(saved, selectedModules, "MANUAL");
        return saved;
    }

    @Transactional
    public void updateClientModules(Long clientId, List<String> selectedModules) {
        PlatformBusinessClient client = getClient(clientId);
        List<PlatformClientModule> currentModules = getClientModules(clientId);
        clientModuleRepository.deleteAll(currentModules);
        saveClientModules(client, selectedModules, "MANUAL_UPDATE");
        client.setStatus("CONFIGURED");
        clientRepository.save(client);
    }

    private void saveClientModules(PlatformBusinessClient client, List<String> selectedModules, String source) {
        Set<String> moduleKeys = new LinkedHashSet<>();
        if (selectedModules != null) {
            selectedModules.stream()
                    .map(this::clean)
                    .filter(value -> !value.isBlank())
                    .forEach(moduleKeys::add);
        }

        if (moduleKeys.isEmpty() && client.getTemplate() != null) {
            moduleKeys.addAll(recommendedModuleKeys(client.getTemplate().getId()));
        }

        for (String moduleKey : moduleKeys) {
            moduleRepository.findByModuleKey(moduleKey).ifPresent(module -> {
                PlatformClientModule clientModule = new PlatformClientModule();
                clientModule.setClient(client);
                clientModule.setModule(module);
                clientModule.setEnabled(true);
                clientModule.setSelectionSource(source);
                clientModuleRepository.save(clientModule);
            });
        }
    }

    private String required(String value, String message) {
        String cleaned = clean(value);
        if (cleaned.isBlank()) {
            throw new IllegalArgumentException(message);
        }
        return cleaned;
    }

    private String clean(String value) {
        return value == null ? "" : value.trim();
    }

    private String safe(String value) {
        return value == null ? "" : value.trim();
    }

    private String defaultValue(String value, String fallback) {
        String cleaned = clean(value);
        return cleaned.isBlank() ? clean(fallback) : cleaned;
    }

    private String normalizeCode(String value, String fallback) {
        String base = defaultValue(value, fallback).toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("-+", "-")
                .replaceAll("^-|-$", "");
        return base.isBlank() ? "cliente-demo" : base;
    }

    private String normalizeDatabaseName(String value) {
        String normalized = defaultValue(value, "cliente_demo").toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9_]+", "_")
                .replaceAll("_+", "_")
                .replaceAll("^_|_$", "");
        if (normalized.isBlank()) {
            return "cliente_demo";
        }
        return normalized;
    }
}
