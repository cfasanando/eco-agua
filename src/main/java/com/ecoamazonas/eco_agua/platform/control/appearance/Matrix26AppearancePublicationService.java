package com.ecoamazonas.eco_agua.platform.control.appearance;

import com.ecoamazonas.eco_agua.platform.PlatformBusinessClient;
import com.ecoamazonas.eco_agua.platform.PlatformBusinessClientRepository;
import com.ecoamazonas.eco_agua.platform.control.Matrix26ControlCenterProperties;
import com.ecoamazonas.eco_agua.platform.control.Matrix26InstanceAuditLog;
import com.ecoamazonas.eco_agua.platform.control.Matrix26InstanceAuditLogRepository;
import com.ecoamazonas.eco_agua.platform.control.Matrix26TargetDatabaseService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@ConditionalOnProperty(name = "matrix26.control-center.enabled", havingValue = "true")
public class Matrix26AppearancePublicationService {

    private static final Set<Integer> RESERVED_PORTS = Set.of(8081, 8082, 8084, 8091);
    private static final Set<String> RESERVED_DATABASES = Set.of(
            "eco_agua",
            "productos_selva_belen",
            "restaurante_buen_sabor",
            "matrix26_platform_control"
    );

    private final PlatformBusinessClientRepository clientRepository;
    private final Matrix26InstanceAppearanceRepository appearanceRepository;
    private final Matrix26InstanceAppearanceDraftRepository draftRepository;
    private final Matrix26InstanceAppearanceHistoryRepository historyRepository;
    private final Matrix26AppearanceEditorService editorService;
    private final Matrix26TargetDatabaseService targetDatabaseService;
    private final Matrix26ControlCenterProperties properties;
    private final Matrix26InstanceAuditLogRepository auditRepository;
    private final Matrix26BrandingService brandingService;

    public Matrix26AppearancePublicationService(
            PlatformBusinessClientRepository clientRepository,
            Matrix26InstanceAppearanceRepository appearanceRepository,
            Matrix26InstanceAppearanceDraftRepository draftRepository,
            Matrix26InstanceAppearanceHistoryRepository historyRepository,
            Matrix26AppearanceEditorService editorService,
            Matrix26TargetDatabaseService targetDatabaseService,
            Matrix26ControlCenterProperties properties,
            Matrix26InstanceAuditLogRepository auditRepository,
            Matrix26BrandingService brandingService
    ) {
        this.clientRepository = clientRepository;
        this.appearanceRepository = appearanceRepository;
        this.draftRepository = draftRepository;
        this.historyRepository = historyRepository;
        this.editorService = editorService;
        this.targetDatabaseService = targetDatabaseService;
        this.properties = properties;
        this.auditRepository = auditRepository;
        this.brandingService = brandingService;
    }

    @Transactional(readOnly = true)
    public Matrix26AppearancePublicationState state(Long instanceId) {
        PlatformBusinessClient instance = requireInstance(instanceId);
        String rejection = publicationRejection(instance);
        if (rejection != null) {
            return new Matrix26AppearancePublicationState(false, false, null, false, rejection);
        }

        try {
            Integer targetVersion = targetPublishedVersion(instance);
            int centralVersion = editorService.publishedAppearance(instanceId).getPublishedVersion();
            boolean present = targetVersion != null;
            boolean synchronizedState = present && targetVersion == centralVersion;
            String message = !present
                    ? "La instancia todavía no tiene una configuración visual local publicada."
                    : synchronizedState
                    ? "La instancia está sincronizada con Matrix26."
                    : "La configuración local y Matrix26 tienen versiones distintas.";
            return new Matrix26AppearancePublicationState(
                    true,
                    present,
                    targetVersion,
                    synchronizedState,
                    message
            );
        } catch (RuntimeException ex) {
            return new Matrix26AppearancePublicationState(
                    true,
                    false,
                    null,
                    false,
                    "No se pudo comprobar la configuración local: " + safeMessage(ex)
            );
        }
    }

    @Transactional
    public int publishDraft(
            Long instanceId,
            String confirmationCode,
            boolean acknowledged,
            String actor
    ) {
        PlatformBusinessClient instance = requireInstance(instanceId);
        assertPublicationAllowed(instance, confirmationCode, acknowledged);

        Matrix26InstanceAppearanceDraft draft = draftRepository.findByInstance_Id(instanceId).orElse(null);
        boolean brandingDraftPresent = brandingService.draftPresent(instanceId);
        if (draft == null && !brandingDraftPresent) {
            throw new IllegalArgumentException("La instancia no tiene un borrador de apariencia o branding para publicar.");
        }

        Matrix26AppearanceEditorForm form = editorService.currentForm(instanceId);
        editorService.validate(form);

        int version = editorService.nextVersion(instanceId);
        String overridesJson = editorService.serializedOverrides(form);
        String snapshot = editorService.serializedSnapshot(form, "PUBLISHED", version);
        Matrix26InstanceAppearance published = editorService.publishedAppearance(instanceId);
        String beforeSnapshot = publishedSnapshot(published);

        writeTargetConfiguration(instance, form, overridesJson, version, actor);
        Matrix26BrandingPublicationResult brandingResult = brandingService.publishToTarget(
                instance,
                version,
                actor
        );
        snapshot = mergeBrandingSnapshot(snapshot, brandingResult, version);
        applyPublishedAppearance(published, form, overridesJson, version, actor);
        appearanceRepository.save(published);

        Matrix26InstanceAppearanceHistory history = new Matrix26InstanceAppearanceHistory();
        history.setInstance(instance);
        history.setVersion(version);
        history.setStatus("PUBLISHED");
        history.setSnapshotJson(snapshot);
        history.setActorUsername(safeActor(actor));
        history.setReason(defaultReason(
                draft == null ? null : draft.getReason(),
                brandingResult.published() ? "Appearance and branding published" : "Appearance draft published"
        ));
        historyRepository.save(history);

        if (draft != null) {
            draftRepository.delete(draft);
        }
        saveAudit(
                instance,
                "APPEARANCE_PUBLISHED",
                actor,
                "Appearance v" + version + " published to " + instance.getCode(),
                beforeSnapshot,
                snapshot
        );
        return version;
    }

    @Transactional
    public int rollback(
            Long instanceId,
            Long historyId,
            String confirmationCode,
            boolean acknowledged,
            String actor
    ) {
        PlatformBusinessClient instance = requireInstance(instanceId);
        assertPublicationAllowed(instance, confirmationCode, acknowledged);

        Matrix26InstanceAppearanceHistory source = historyRepository.findById(historyId)
                .orElseThrow(() -> new IllegalArgumentException("La versión histórica no existe."));
        if (source.getInstance() == null || !instanceId.equals(source.getInstance().getId())) {
            throw new IllegalArgumentException("La versión histórica no pertenece a esta instancia.");
        }
        if (!"PUBLISHED".equals(source.getStatus())) {
            throw new IllegalArgumentException("Solo se pueden restaurar versiones que fueron publicadas.");
        }

        Matrix26AppearanceEditorForm form = editorService.formFromSnapshot(source.getSnapshotJson());
        editorService.validate(form);

        int version = editorService.nextVersion(instanceId);
        String overridesJson = editorService.serializedOverrides(form);
        String snapshot = editorService.serializedSnapshot(form, "PUBLISHED", version);
        Matrix26InstanceAppearance published = editorService.publishedAppearance(instanceId);
        String beforeSnapshot = publishedSnapshot(published);

        writeTargetConfiguration(instance, form, overridesJson, version, actor);
        brandingService.restoreFromHistory(
                instance,
                source.getVersion(),
                version,
                source.getSnapshotJson(),
                actor
        );
        snapshot = mergeBrandingSnapshotFromSource(snapshot, source.getSnapshotJson(), version);
        applyPublishedAppearance(published, form, overridesJson, version, actor);
        appearanceRepository.save(published);

        Matrix26InstanceAppearanceHistory restored = new Matrix26InstanceAppearanceHistory();
        restored.setInstance(instance);
        restored.setVersion(version);
        restored.setStatus("PUBLISHED");
        restored.setSnapshotJson(snapshot);
        restored.setActorUsername(safeActor(actor));
        restored.setReason("Rollback to published version v" + source.getVersion());
        historyRepository.save(restored);

        draftRepository.findByInstance_Id(instanceId).ifPresent(draftRepository::delete);
        saveAudit(
                instance,
                "APPEARANCE_ROLLED_BACK",
                actor,
                "Appearance restored from v" + source.getVersion() + " as v" + version,
                beforeSnapshot,
                snapshot
        );
        return version;
    }

    @Transactional(readOnly = true)
    public boolean canPublish(Long instanceId) {
        return publicationRejection(requireInstance(instanceId)) == null;
    }

    private void applyPublishedAppearance(
            Matrix26InstanceAppearance published,
            Matrix26AppearanceEditorForm form,
            String overridesJson,
            int version,
            String actor
    ) {
        published.setPublicThemeCode(form.getPublicThemeCode());
        published.setPublicLayoutCode(form.getPublicLayoutCode());
        published.setAdminThemeCode(form.getAdminThemeCode());
        published.setAdminLayoutCode(form.getAdminLayoutCode());
        published.setLoginLayoutCode(form.getLoginLayoutCode());
        published.setOverridesJson(overridesJson);
        published.setStatus("PUBLISHED");
        published.setPublishedVersion(version);
        published.setPublishedAt(LocalDateTime.now());
        published.setPublishedBy(safeActor(actor));
    }

    private void writeTargetConfiguration(
            PlatformBusinessClient instance,
            Matrix26AppearanceEditorForm form,
            String overridesJson,
            int version,
            String actor
    ) {
        JdbcTemplate target = targetDatabaseService.targetJdbcTemplate(instance.getDatabaseName());
        target.execute("""
                CREATE TABLE IF NOT EXISTS matrix26_instance_appearance_config (
                    id SMALLINT NOT NULL,
                    instance_code VARCHAR(80) NOT NULL,
                    public_theme_code VARCHAR(80) NOT NULL,
                    public_layout_code VARCHAR(80) NOT NULL,
                    admin_theme_code VARCHAR(80) NOT NULL,
                    admin_layout_code VARCHAR(80) NOT NULL,
                    login_layout_code VARCHAR(80) NOT NULL,
                    overrides_json TEXT NULL,
                    published_version INT NOT NULL,
                    published_at DATETIME(6) NOT NULL,
                    published_by VARCHAR(120) NOT NULL,
                    updated_at DATETIME(6) NOT NULL,
                    PRIMARY KEY (id)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
                """);
        target.update(
                """
                INSERT INTO matrix26_instance_appearance_config (
                    id, instance_code, public_theme_code, public_layout_code,
                    admin_theme_code, admin_layout_code, login_layout_code,
                    overrides_json, published_version, published_at,
                    published_by, updated_at
                ) VALUES (
                    1, ?, ?, ?, ?, ?, ?, ?, ?, NOW(6), ?, NOW(6)
                )
                ON DUPLICATE KEY UPDATE
                    instance_code = VALUES(instance_code),
                    public_theme_code = VALUES(public_theme_code),
                    public_layout_code = VALUES(public_layout_code),
                    admin_theme_code = VALUES(admin_theme_code),
                    admin_layout_code = VALUES(admin_layout_code),
                    login_layout_code = VALUES(login_layout_code),
                    overrides_json = VALUES(overrides_json),
                    published_version = VALUES(published_version),
                    published_at = VALUES(published_at),
                    published_by = VALUES(published_by),
                    updated_at = NOW(6)
                """,
                instance.getCode(),
                form.getPublicThemeCode(),
                form.getPublicLayoutCode(),
                form.getAdminThemeCode(),
                form.getAdminLayoutCode(),
                form.getLoginLayoutCode(),
                overridesJson,
                version,
                safeActor(actor)
        );
    }

    private Integer targetPublishedVersion(PlatformBusinessClient instance) {
        if (!targetDatabaseService.databaseExists(instance.getDatabaseName())) {
            return null;
        }
        JdbcTemplate target = targetDatabaseService.targetJdbcTemplate(instance.getDatabaseName());
        Integer tableCount = target.queryForObject(
                """
                SELECT COUNT(*)
                FROM information_schema.TABLES
                WHERE TABLE_SCHEMA = DATABASE()
                  AND TABLE_NAME = 'matrix26_instance_appearance_config'
                """,
                Integer.class
        );
        if (tableCount == null || tableCount == 0) {
            return null;
        }
        return target.query(
                "SELECT published_version FROM matrix26_instance_appearance_config WHERE id = 1",
                resultSet -> resultSet.next() ? resultSet.getInt(1) : null
        );
    }

    private void assertPublicationAllowed(
            PlatformBusinessClient instance,
            String confirmationCode,
            boolean acknowledged
    ) {
        String rejection = publicationRejection(instance);
        if (rejection != null) {
            throw new IllegalArgumentException(rejection);
        }
        if (!acknowledged) {
            throw new IllegalArgumentException("Debes confirmar que revisaste la instancia y la versión visual.");
        }
        if (confirmationCode == null || !instance.getCode().equalsIgnoreCase(confirmationCode.trim())) {
            throw new IllegalArgumentException("Escribe exactamente el código de la instancia para confirmar la publicación.");
        }
    }

    private String publicationRejection(PlatformBusinessClient instance) {
        if (!properties.isAppearancePublishingEnabled()) {
            return "La publicación real de apariencia está deshabilitada en este runtime de Matrix26.";
        }
        if (!allowedInstanceCodes().contains(normalize(instance.getCode()))) {
            return "Esta fase solo permite publicar en instancias de laboratorio autorizadas.";
        }
        if (instance.getRuntimePort() == null || RESERVED_PORTS.contains(instance.getRuntimePort())) {
            return "El puerto de esta instancia está reservado o no es válido para pruebas de apariencia.";
        }
        String database = normalize(instance.getDatabaseName());
        if (database.isBlank() || RESERVED_DATABASES.contains(database)) {
            return "La base de esta instancia está protegida para publicación de apariencia.";
        }
        if (!targetDatabaseService.databaseExists(instance.getDatabaseName())) {
            return "La base de la instancia no existe.";
        }
        return null;
    }

    private Set<String> allowedInstanceCodes() {
        String configured = properties.getAppearancePublishAllowedInstanceCodes();
        if (configured == null || configured.isBlank()) {
            return Set.of();
        }
        return Arrays.stream(configured.split(","))
                .map(this::normalize)
                .filter(value -> !value.isBlank())
                .collect(Collectors.toUnmodifiableSet());
    }

    private PlatformBusinessClient requireInstance(Long instanceId) {
        return clientRepository.findById(instanceId)
                .orElseThrow(() -> new IllegalArgumentException("La instancia no existe."));
    }

    private String publishedSnapshot(Matrix26InstanceAppearance appearance) {
        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("status", appearance.getStatus());
        snapshot.put("version", appearance.getPublishedVersion());
        snapshot.put("publicTheme", appearance.getPublicThemeCode());
        snapshot.put("publicLayout", appearance.getPublicLayoutCode());
        snapshot.put("adminTheme", appearance.getAdminThemeCode());
        snapshot.put("adminLayout", appearance.getAdminLayoutCode());
        snapshot.put("loginLayout", appearance.getLoginLayoutCode());
        snapshot.put("overrides", Matrix26JsonCodec.readFlatObject(appearance.getOverridesJson()));
        return Matrix26JsonCodec.write(snapshot);
    }

    private String mergeBrandingSnapshotFromSource(
            String appearanceSnapshot,
            String sourceSnapshot,
            int version
    ) {
        Map<String, Object> result = Matrix26JsonCodec.readObject(appearanceSnapshot);
        Map<String, Object> source = Matrix26JsonCodec.readObject(sourceSnapshot);
        if (source.containsKey("branding")) {
            result.put("branding", source.get("branding"));
        }
        if (source.containsKey("assets")) {
            result.put("assets", source.get("assets"));
        }
        if (source.containsKey("branding") || source.containsKey("assets")) {
            result.put("assetVersion", version);
        }
        return Matrix26JsonCodec.write(result);
    }

    private String mergeBrandingSnapshot(
            String appearanceSnapshot,
            Matrix26BrandingPublicationResult branding,
            int version
    ) {
        Map<String, Object> snapshot = Matrix26JsonCodec.readObject(appearanceSnapshot);
        if (branding != null && branding.published()) {
            snapshot.put("branding", Matrix26JsonCodec.readObject(branding.brandingJson()));
            snapshot.put("assets", Matrix26JsonCodec.readObject(branding.assetManifestJson()));
            snapshot.put("assetVersion", version);
        }
        return Matrix26JsonCodec.write(snapshot);
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
        log.setActorUsername(safeActor(actor));
        log.setSummary(summary.length() > 500 ? summary.substring(0, 500) : summary);
        log.setBeforeSnapshot(before);
        log.setAfterSnapshot(after);
        auditRepository.save(log);
    }

    private String safeActor(String actor) {
        String clean = actor == null ? "" : actor.trim();
        if (clean.isBlank()) {
            return "matrix26-system";
        }
        return clean.length() > 120 ? clean.substring(0, 120) : clean;
    }

    private String defaultReason(String value, String fallback) {
        String clean = value == null ? "" : value.trim();
        return clean.isBlank() ? fallback : clean;
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    private String safeMessage(Exception ex) {
        String message = ex.getMessage();
        if (message == null || message.isBlank()) {
            return ex.getClass().getSimpleName();
        }
        return message.length() > 300 ? message.substring(0, 300) : message;
    }
}
