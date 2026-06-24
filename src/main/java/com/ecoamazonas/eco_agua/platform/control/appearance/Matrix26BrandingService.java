package com.ecoamazonas.eco_agua.platform.control.appearance;

import com.ecoamazonas.eco_agua.platform.PlatformBusinessClient;
import com.ecoamazonas.eco_agua.platform.PlatformBusinessClientRepository;
import com.ecoamazonas.eco_agua.platform.control.Matrix26TargetDatabaseService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Pattern;

@Service
@ConditionalOnProperty(name = "matrix26.control-center.enabled", havingValue = "true")
public class Matrix26BrandingService {

    private static final Pattern SAFE_CODE = Pattern.compile("^[a-z0-9][a-z0-9-]{1,78}[a-z0-9]$");
    private static final DateTimeFormatter VIEW_DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final JdbcTemplate jdbcTemplate;
    private final PlatformBusinessClientRepository clientRepository;
    private final Matrix26TargetDatabaseService targetDatabaseService;
    private final ResourceLoader resourceLoader;
    private final Path dataRoot;

    public Matrix26BrandingService(
            JdbcTemplate jdbcTemplate,
            PlatformBusinessClientRepository clientRepository,
            Matrix26TargetDatabaseService targetDatabaseService,
            ResourceLoader resourceLoader,
            @Value("${matrix26.control-center.appearance-data-directory:runtime-data}") String dataDirectory
    ) {
        this.jdbcTemplate = jdbcTemplate;
        this.clientRepository = clientRepository;
        this.targetDatabaseService = targetDatabaseService;
        this.resourceLoader = resourceLoader;
        this.dataRoot = Path.of(dataDirectory).toAbsolutePath().normalize();
    }

    @Transactional(readOnly = true)
    public Matrix26BrandingView view(Long instanceId) {
        PlatformBusinessClient instance = requireInstance(instanceId);
        DraftRow draft = draft(instanceId).orElse(null);
        Matrix26BrandingForm form = draft == null ? defaults(instance) : toForm(draft);
        Map<String, AssetRow> assets = assets(instanceId);

        List<Matrix26BrandingAssetView> views = java.util.Arrays.stream(Matrix26BrandingAssetType.values())
                .map(type -> assetView(instanceId, type, assets.get(type.code())))
                .toList();

        return new Matrix26BrandingView(
                instance,
                form,
                views,
                draft != null || !assets.isEmpty(),
                draft == null ? null : draft.updatedBy(),
                draft == null || draft.updatedAt() == null ? null : draft.updatedAt().format(VIEW_DATE),
                true
        );
    }

    @Transactional
    public void saveText(Long instanceId, Matrix26BrandingForm form, String actor) {
        PlatformBusinessClient instance = requireInstance(instanceId);
        normalize(form, instance);
        String safeActor = safeActor(actor);

        jdbcTemplate.update(
                """
                INSERT INTO matrix26_instance_branding_draft (
                    instance_id, display_name, short_name, tagline, welcome_message,
                    hero_title, hero_subtitle, primary_cta_label, secondary_cta_label,
                    contact_phone, whatsapp, location, reason, updated_by,
                    created_at, updated_at
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, NOW(6), NOW(6))
                ON DUPLICATE KEY UPDATE
                    display_name = VALUES(display_name),
                    short_name = VALUES(short_name),
                    tagline = VALUES(tagline),
                    welcome_message = VALUES(welcome_message),
                    hero_title = VALUES(hero_title),
                    hero_subtitle = VALUES(hero_subtitle),
                    primary_cta_label = VALUES(primary_cta_label),
                    secondary_cta_label = VALUES(secondary_cta_label),
                    contact_phone = VALUES(contact_phone),
                    whatsapp = VALUES(whatsapp),
                    location = VALUES(location),
                    reason = VALUES(reason),
                    updated_by = VALUES(updated_by),
                    updated_at = NOW(6)
                """,
                instanceId,
                form.getDisplayName(),
                form.getShortName(),
                nullable(form.getTagline()),
                nullable(form.getWelcomeMessage()),
                nullable(form.getHeroTitle()),
                nullable(form.getHeroSubtitle()),
                nullable(form.getPrimaryCtaLabel()),
                nullable(form.getSecondaryCtaLabel()),
                nullable(form.getContactPhone()),
                nullable(form.getWhatsapp()),
                nullable(form.getLocation()),
                nullable(form.getReason()),
                safeActor
        );
    }

    @Transactional
    public void storeAsset(Long instanceId, String assetCode, MultipartFile file, String actor) {
        PlatformBusinessClient instance = requireInstance(instanceId);
        Matrix26BrandingAssetType type = requireType(assetCode);
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Selecciona un archivo para " + type.label() + ".");
        }

        try {
            byte[] bytes = file.getBytes();
            ValidatedImage image = validate(type, file.getOriginalFilename(), bytes);
            Path directory = draftDirectory(instance);
            Files.createDirectories(directory);
            Path target = directory.resolve(type.code() + "." + image.extension()).normalize();
            assertInside(directory, target);
            Path temporary = directory.resolve(target.getFileName() + ".tmp");
            Files.write(temporary, bytes);
            moveReplacing(temporary, target);

            jdbcTemplate.update(
                    """
                    INSERT INTO matrix26_instance_branding_asset (
                        instance_id, asset_type, relative_path, original_name,
                        content_type, extension, size_bytes, width_px, height_px,
                        sha256, updated_by, created_at, updated_at
                    ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, NOW(6), NOW(6))
                    ON DUPLICATE KEY UPDATE
                        relative_path = VALUES(relative_path),
                        original_name = VALUES(original_name),
                        content_type = VALUES(content_type),
                        extension = VALUES(extension),
                        size_bytes = VALUES(size_bytes),
                        width_px = VALUES(width_px),
                        height_px = VALUES(height_px),
                        sha256 = VALUES(sha256),
                        updated_by = VALUES(updated_by),
                        updated_at = NOW(6)
                    """,
                    instanceId,
                    type.code(),
                    dataRoot.relativize(target).toString().replace('\\', '/'),
                    cleanFileName(file.getOriginalFilename(), type.code() + "." + image.extension()),
                    image.contentType(),
                    image.extension(),
                    bytes.length,
                    image.width(),
                    image.height(),
                    sha256(bytes),
                    safeActor(actor)
            );
        } catch (IOException ex) {
            throw new IllegalStateException("No se pudo guardar el recurso visual.", ex);
        }
    }

    @Transactional
    public void removeAsset(Long instanceId, String assetCode) {
        PlatformBusinessClient instance = requireInstance(instanceId);
        Matrix26BrandingAssetType type = requireType(assetCode);
        Optional<AssetRow> asset = asset(instanceId, type.code());
        asset.ifPresent(row -> {
            try {
                Path path = resolveStoredPath(row.relativePath());
                Files.deleteIfExists(path);
            } catch (IOException ex) {
                throw new IllegalStateException("No se pudo eliminar el archivo del borrador.", ex);
            }
        });
        jdbcTemplate.update(
                "DELETE FROM matrix26_instance_branding_asset WHERE instance_id = ? AND asset_type = ?",
                instanceId,
                type.code()
        );
    }

    @Transactional
    public void applyDemoKit(Long instanceId, String actor) {
        PlatformBusinessClient instance = requireInstance(instanceId);
        Matrix26BrandingForm form = new Matrix26BrandingForm();
        form.setDisplayName("Matrix26 Restaurant Laboratory");
        form.setShortName("Matrix26 Restaurant");
        form.setTagline("Sabores amazónicos con una experiencia visual propia");
        form.setWelcomeMessage("Bienvenido al centro de gestión de Matrix26 Restaurant Laboratory.");
        form.setHeroTitle("Sabor amazónico con identidad propia");
        form.setHeroSubtitle("Una experiencia cálida para descubrir platos, promociones y atención directa por WhatsApp.");
        form.setPrimaryCtaLabel("Ver nuestra carta");
        form.setSecondaryCtaLabel("Reservar una mesa");
        form.setContactPhone("(065) 000000");
        form.setWhatsapp("51928527493");
        form.setLocation("Iquitos, Loreto");
        form.setReason("Kit demostrativo de branding Matrix26 3C.5");
        saveText(instanceId, form, actor);

        Map<Matrix26BrandingAssetType, String> samples = new EnumMap<>(Matrix26BrandingAssetType.class);
        samples.put(Matrix26BrandingAssetType.LOGO_PRIMARY, "logo-primary.png");
        samples.put(Matrix26BrandingAssetType.LOGO_COMPACT, "logo-compact.png");
        samples.put(Matrix26BrandingAssetType.FAVICON, "favicon.png");
        samples.put(Matrix26BrandingAssetType.LOGIN_COVER, "login-cover.jpg");
        samples.put(Matrix26BrandingAssetType.HERO_PRIMARY, "hero-primary.jpg");
        samples.put(Matrix26BrandingAssetType.HERO_SECONDARY, "hero-secondary.jpg");
        samples.put(Matrix26BrandingAssetType.PRODUCT_PLACEHOLDER, "product-placeholder.png");
        samples.put(Matrix26BrandingAssetType.SOCIAL_SHARE, "social-share.jpg");

        samples.forEach((type, name) -> copyDemoAsset(instance, type, name, actor));
    }

    @Transactional(readOnly = true)
    public boolean draftPresent(Long instanceId) {
        Integer count = jdbcTemplate.queryForObject(
                """
                SELECT (
                    (SELECT COUNT(*) FROM matrix26_instance_branding_draft WHERE instance_id = ?)
                    +
                    (SELECT COUNT(*) FROM matrix26_instance_branding_asset WHERE instance_id = ?)
                )
                """,
                Integer.class,
                instanceId,
                instanceId
        );
        return count != null && count > 0;
    }

    @Transactional(readOnly = true)
    public String draftSnapshot(Long instanceId) {
        PlatformBusinessClient instance = requireInstance(instanceId);
        DraftRow draft = draft(instanceId).orElse(null);
        Matrix26BrandingForm form = draft == null ? defaults(instance) : toForm(draft);
        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("displayName", form.getDisplayName());
        snapshot.put("shortName", form.getShortName());
        snapshot.put("tagline", form.getTagline());
        snapshot.put("welcomeMessage", form.getWelcomeMessage());
        snapshot.put("heroTitle", form.getHeroTitle());
        snapshot.put("heroSubtitle", form.getHeroSubtitle());
        snapshot.put("primaryCtaLabel", form.getPrimaryCtaLabel());
        snapshot.put("secondaryCtaLabel", form.getSecondaryCtaLabel());
        snapshot.put("contactPhone", form.getContactPhone());
        snapshot.put("whatsapp", form.getWhatsapp());
        snapshot.put("location", form.getLocation());
        return Matrix26JsonCodec.write(snapshot);
    }

    @Transactional
    public Matrix26BrandingPublicationResult publishToTarget(
            PlatformBusinessClient instance,
            int version,
            String actor
    ) {
        DraftRow draft = draft(instance.getId()).orElse(null);
        Map<String, AssetRow> draftAssets = assets(instance.getId());
        if (draft == null && draftAssets.isEmpty()) {
            return currentPublishedState(instance);
        }

        Matrix26BrandingForm form = draft == null ? defaults(instance) : toForm(draft);
        Map<String, Object> branding = brandingMap(form);
        Map<String, String> manifest = publishFiles(instance, version, draftAssets);

        JdbcTemplate target = targetDatabaseService.targetJdbcTemplate(instance.getDatabaseName());
        ensureTargetColumns(target);
        String brandingJson = Matrix26JsonCodec.write(branding);
        String manifestJson = Matrix26JsonCodec.write(manifest);
        target.update(
                """
                UPDATE matrix26_instance_appearance_config
                SET branding_json = ?,
                    asset_manifest_json = ?,
                    updated_at = NOW(6)
                WHERE id = 1
                """,
                brandingJson,
                manifestJson
        );

        jdbcTemplate.update(
                "DELETE FROM matrix26_instance_branding_draft WHERE instance_id = ?",
                instance.getId()
        );
        jdbcTemplate.update(
                "DELETE FROM matrix26_instance_branding_asset WHERE instance_id = ?",
                instance.getId()
        );
        deleteQuietly(draftDirectory(instance));
        return new Matrix26BrandingPublicationResult(brandingJson, manifestJson, true);
    }

    private Matrix26BrandingPublicationResult currentPublishedState(
            PlatformBusinessClient instance
    ) {
        if (!targetDatabaseService.databaseExists(instance.getDatabaseName())) {
            return new Matrix26BrandingPublicationResult("{}", "{}", false);
        }
        JdbcTemplate target = targetDatabaseService.targetJdbcTemplate(instance.getDatabaseName());
        Integer columns = target.queryForObject(
                """
                SELECT COUNT(*)
                FROM information_schema.COLUMNS
                WHERE TABLE_SCHEMA = DATABASE()
                  AND TABLE_NAME = 'matrix26_instance_appearance_config'
                  AND COLUMN_NAME IN ('branding_json', 'asset_manifest_json')
                """,
                Integer.class
        );
        if (columns == null || columns < 2) {
            return new Matrix26BrandingPublicationResult("{}", "{}", false);
        }
        return target.query(
                """
                SELECT branding_json, asset_manifest_json
                FROM matrix26_instance_appearance_config
                WHERE id = 1
                """,
                resultSet -> {
                    if (!resultSet.next()) {
                        return new Matrix26BrandingPublicationResult("{}", "{}", false);
                    }
                    String branding = resultSet.getString("branding_json");
                    String assets = resultSet.getString("asset_manifest_json");
                    boolean present = (branding != null && !branding.isBlank() && !"{}".equals(branding.trim()))
                            || (assets != null && !assets.isBlank() && !"{}".equals(assets.trim()));
                    return new Matrix26BrandingPublicationResult(
                            branding == null || branding.isBlank() ? "{}" : branding,
                            assets == null || assets.isBlank() ? "{}" : assets,
                            present
                    );
                }
        );
    }

    @Transactional
    public void restoreFromHistory(
            PlatformBusinessClient instance,
            int sourceVersion,
            int newVersion,
            String snapshotJson,
            String actor
    ) {
        Map<String, Object> snapshot = Matrix26JsonCodec.readObject(snapshotJson);
        Map<String, String> branding = stringMap(snapshot.get("branding"));
        Map<String, String> assets = stringMap(snapshot.get("assets"));

        try {
            Path instanceRoot = dataRoot.resolve(safeInstanceCode(instance.getCode())).resolve("appearance");
            Path source = instanceRoot.resolve("history").resolve("v" + sourceVersion);
            Path destination = instanceRoot.resolve("history").resolve("v" + newVersion);
            Path temporary = instanceRoot.resolve(".rollback-v" + newVersion + "-tmp");
            Path current = instanceRoot.resolve("current");
            Path backup = instanceRoot.resolve(".current-backup");

            deleteQuietly(temporary);
            Files.createDirectories(temporary);
            if (Files.isDirectory(source)) {
                copyDirectory(source, temporary);
            }

            deleteQuietly(destination);
            copyDirectory(temporary, destination);
            activateDirectory(temporary, current, backup);

            JdbcTemplate target = targetDatabaseService.targetJdbcTemplate(instance.getDatabaseName());
            ensureTargetColumns(target);
            target.update(
                    """
                    UPDATE matrix26_instance_appearance_config
                    SET branding_json = ?,
                        asset_manifest_json = ?,
                        updated_at = NOW(6)
                    WHERE id = 1
                    """,
                    Matrix26JsonCodec.write(branding),
                    Matrix26JsonCodec.write(assets)
            );
        } catch (IOException ex) {
            throw new IllegalStateException("No se pudo restaurar el branding histórico.", ex);
        }
    }

    public Optional<Path> draftAssetPath(Long instanceId, String assetCode) {
        requireInstance(instanceId);
        Matrix26BrandingAssetType type = requireType(assetCode);
        return asset(instanceId, type.code())
                .map(AssetRow::relativePath)
                .map(this::resolveStoredPath)
                .filter(Files::isRegularFile);
    }

    private Map<String, String> publishFiles(
            PlatformBusinessClient instance,
            int version,
            Map<String, AssetRow> draftAssets
    ) {
        try {
            Path instanceRoot = dataRoot.resolve(safeInstanceCode(instance.getCode())).resolve("appearance");
            Path current = instanceRoot.resolve("current");
            Path history = instanceRoot.resolve("history").resolve("v" + version);
            Path temporary = instanceRoot.resolve(".current-v" + version + "-tmp");

            deleteQuietly(temporary);
            Files.createDirectories(temporary);
            if (Files.isDirectory(current)) {
                copyDirectory(current, temporary);
            }

            Map<String, String> manifest = existingManifest(temporary);
            for (Matrix26BrandingAssetType type : Matrix26BrandingAssetType.values()) {
                AssetRow row = draftAssets.get(type.code());
                if (row == null) {
                    continue;
                }
                Path source = resolveStoredPath(row.relativePath());
                String fileName = type.code() + "-v" + version + "." + row.extension();
                Files.copy(source, temporary.resolve(fileName), StandardCopyOption.REPLACE_EXISTING);
                removeOtherExtensions(temporary, type.code(), fileName);
                manifest.put(type.code(), fileName);
            }

            deleteQuietly(history);
            copyDirectory(temporary, history);
            Path backup = instanceRoot.resolve(".current-backup");
            activateDirectory(temporary, current, backup);
            return manifest;
        } catch (IOException ex) {
            throw new IllegalStateException("No se pudieron publicar los recursos visuales.", ex);
        }
    }

    private Map<String, String> existingManifest(Path directory) throws IOException {
        Map<String, String> result = new LinkedHashMap<>();
        if (!Files.isDirectory(directory)) {
            return result;
        }
        try (var stream = Files.list(directory)) {
            stream.filter(Files::isRegularFile).forEach(path -> {
                String fileName = path.getFileName().toString();
                for (Matrix26BrandingAssetType type : Matrix26BrandingAssetType.values()) {
                    if (fileName.startsWith(type.code() + ".")
                            || fileName.startsWith(type.code() + "-v")) {
                        String existing = result.get(type.code());
                        if (existing == null || assetVersion(fileName) >= assetVersion(existing)) {
                            result.put(type.code(), fileName);
                        }
                        break;
                    }
                }
            });
        }
        return result;
    }

    private Matrix26BrandingAssetView assetView(
            Long instanceId,
            Matrix26BrandingAssetType type,
            AssetRow asset
    ) {
        if (asset == null) {
            return new Matrix26BrandingAssetView(
                    type.code(),
                    type.label(),
                    type.recommendedSize(),
                    null,
                    null,
                    0L,
                    null,
                    null,
                    null,
                    false
            );
        }
        return new Matrix26BrandingAssetView(
                type.code(),
                type.label(),
                type.recommendedSize(),
                asset.originalName(),
                asset.contentType(),
                asset.sizeBytes(),
                asset.width(),
                asset.height(),
                "/control-center/branding-assets/" + instanceId + "/" + type.code(),
                true
        );
    }

    private Map<String, String> stringMap(Object value) {
        Map<String, String> result = new LinkedHashMap<>();
        if (value instanceof Map<?, ?> source) {
            source.forEach((key, item) -> {
                if (key != null && item != null) {
                    result.put(String.valueOf(key), String.valueOf(item));
                }
            });
        }
        return result;
    }

    private Map<String, Object> brandingMap(Matrix26BrandingForm form) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("displayName", form.getDisplayName());
        result.put("shortName", form.getShortName());
        result.put("tagline", nullable(form.getTagline()));
        result.put("welcomeMessage", nullable(form.getWelcomeMessage()));
        result.put("heroTitle", nullable(form.getHeroTitle()));
        result.put("heroSubtitle", nullable(form.getHeroSubtitle()));
        result.put("primaryCtaLabel", nullable(form.getPrimaryCtaLabel()));
        result.put("secondaryCtaLabel", nullable(form.getSecondaryCtaLabel()));
        result.put("contactPhone", nullable(form.getContactPhone()));
        result.put("whatsapp", nullable(form.getWhatsapp()));
        result.put("location", nullable(form.getLocation()));
        return result;
    }

    private void ensureTargetColumns(JdbcTemplate target) {
        ensureTargetColumn(target, "branding_json", "ALTER TABLE matrix26_instance_appearance_config ADD COLUMN branding_json TEXT NULL AFTER overrides_json");
        ensureTargetColumn(target, "asset_manifest_json", "ALTER TABLE matrix26_instance_appearance_config ADD COLUMN asset_manifest_json TEXT NULL AFTER branding_json");
    }

    private void ensureTargetColumn(JdbcTemplate target, String column, String alterSql) {
        Integer count = target.queryForObject(
                """
                SELECT COUNT(*)
                FROM information_schema.COLUMNS
                WHERE TABLE_SCHEMA = DATABASE()
                  AND TABLE_NAME = 'matrix26_instance_appearance_config'
                  AND COLUMN_NAME = ?
                """,
                Integer.class,
                column
        );
        if (count == null || count == 0) {
            target.execute(alterSql);
        }
    }

    private void copyDemoAsset(
            PlatformBusinessClient instance,
            Matrix26BrandingAssetType type,
            String fileName,
            String actor
    ) {
        Resource resource = resourceLoader.getResource(
                "classpath:/static/demo/branding/restaurant-lab/" + fileName
        );
        if (!resource.exists()) {
            throw new IllegalStateException("No se encontró el recurso demo: " + fileName);
        }
        try (InputStream input = resource.getInputStream()) {
            byte[] bytes = input.readAllBytes();
            ValidatedImage image = validate(type, fileName, bytes);
            Path directory = draftDirectory(instance);
            Files.createDirectories(directory);
            Path target = directory.resolve(type.code() + "." + image.extension());
            Files.write(target, bytes);

            jdbcTemplate.update(
                    """
                    INSERT INTO matrix26_instance_branding_asset (
                        instance_id, asset_type, relative_path, original_name,
                        content_type, extension, size_bytes, width_px, height_px,
                        sha256, updated_by, created_at, updated_at
                    ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, NOW(6), NOW(6))
                    ON DUPLICATE KEY UPDATE
                        relative_path = VALUES(relative_path),
                        original_name = VALUES(original_name),
                        content_type = VALUES(content_type),
                        extension = VALUES(extension),
                        size_bytes = VALUES(size_bytes),
                        width_px = VALUES(width_px),
                        height_px = VALUES(height_px),
                        sha256 = VALUES(sha256),
                        updated_by = VALUES(updated_by),
                        updated_at = NOW(6)
                    """,
                    instance.getId(),
                    type.code(),
                    dataRoot.relativize(target).toString().replace('\\', '/'),
                    fileName,
                    image.contentType(),
                    image.extension(),
                    bytes.length,
                    image.width(),
                    image.height(),
                    sha256(bytes),
                    safeActor(actor)
            );
        } catch (IOException ex) {
            throw new IllegalStateException("No se pudo cargar el kit demostrativo.", ex);
        }
    }

    private ValidatedImage validate(
            Matrix26BrandingAssetType type,
            String originalName,
            byte[] bytes
    ) {
        if (bytes.length > type.maximumBytes()) {
            throw new IllegalArgumentException(type.label() + " supera el tamaño máximo permitido.");
        }
        String extension = extension(originalName);
        if (!type.extensions().contains(extension)) {
            throw new IllegalArgumentException("Formato no permitido para " + type.label() + ".");
        }

        String contentType = detectContentType(bytes);
        if (contentType == null || !contentTypeMatches(extension, contentType)) {
            throw new IllegalArgumentException("El contenido real del archivo no coincide con su extensión.");
        }

        Integer width = null;
        Integer height = null;
        if (!"webp".equals(extension) && !"ico".equals(extension)) {
            try {
                BufferedImage image = ImageIO.read(new ByteArrayInputStream(bytes));
                if (image == null) {
                    throw new IllegalArgumentException("No se pudo leer la imagen.");
                }
                width = image.getWidth();
                height = image.getHeight();
                if (width < type.minimumWidth() || height < type.minimumHeight()) {
                    throw new IllegalArgumentException(
                            type.label() + " debe medir al menos "
                                    + type.minimumWidth() + " × " + type.minimumHeight() + " px."
                    );
                }
            } catch (IOException ex) {
                throw new IllegalArgumentException("No se pudo validar la imagen.", ex);
            }
        }
        return new ValidatedImage(extension, contentType, width, height);
    }

    private String detectContentType(byte[] bytes) {
        if (bytes.length >= 8
                && bytes[0] == (byte) 0x89
                && bytes[1] == 0x50
                && bytes[2] == 0x4E
                && bytes[3] == 0x47) {
            return "image/png";
        }
        if (bytes.length >= 3
                && bytes[0] == (byte) 0xFF
                && bytes[1] == (byte) 0xD8
                && bytes[2] == (byte) 0xFF) {
            return "image/jpeg";
        }
        if (bytes.length >= 12
                && bytes[0] == 'R'
                && bytes[1] == 'I'
                && bytes[2] == 'F'
                && bytes[3] == 'F'
                && bytes[8] == 'W'
                && bytes[9] == 'E'
                && bytes[10] == 'B'
                && bytes[11] == 'P') {
            return "image/webp";
        }
        if (bytes.length >= 4
                && bytes[0] == 0
                && bytes[1] == 0
                && bytes[2] == 1
                && bytes[3] == 0) {
            return "image/x-icon";
        }
        return null;
    }

    private boolean contentTypeMatches(String extension, String contentType) {
        return switch (extension) {
            case "png" -> "image/png".equals(contentType);
            case "jpg", "jpeg" -> "image/jpeg".equals(contentType);
            case "webp" -> "image/webp".equals(contentType);
            case "ico" -> "image/x-icon".equals(contentType);
            default -> false;
        };
    }

    private Matrix26BrandingForm defaults(PlatformBusinessClient instance) {
        Matrix26BrandingForm form = new Matrix26BrandingForm();
        form.setDisplayName(defaultValue(instance.getBusinessName(), "Nueva instancia"));
        form.setShortName(shorten(defaultValue(instance.getBusinessName(), instance.getCode()), 100));
        form.setTagline(shorten(defaultValue(instance.getNotes(), "Experiencia administrada por Matrix26"), 220));
        form.setWelcomeMessage("Bienvenido al sistema de gestión de " + form.getShortName() + ".");
        form.setHeroTitle("Descubre " + form.getShortName());
        form.setHeroSubtitle("Productos, servicios y atención directa desde una experiencia visual personalizada.");
        form.setPrimaryCtaLabel("Ver catálogo");
        form.setSecondaryCtaLabel("Contactar");
        form.setContactPhone(instance.getContactPhone());
        form.setWhatsapp(instance.getWhatsapp());
        form.setLocation(instance.getCity());
        return form;
    }

    private void normalize(Matrix26BrandingForm form, PlatformBusinessClient instance) {
        form.setDisplayName(required(form.getDisplayName(), "Nombre visible"));
        form.setShortName(required(form.getShortName(), "Nombre corto"));
        form.setTagline(clean(form.getTagline()));
        form.setWelcomeMessage(clean(form.getWelcomeMessage()));
        form.setHeroTitle(clean(form.getHeroTitle()));
        form.setHeroSubtitle(clean(form.getHeroSubtitle()));
        form.setPrimaryCtaLabel(clean(form.getPrimaryCtaLabel()));
        form.setSecondaryCtaLabel(clean(form.getSecondaryCtaLabel()));
        form.setContactPhone(clean(form.getContactPhone()));
        form.setWhatsapp(clean(form.getWhatsapp()));
        form.setLocation(clean(form.getLocation()));
        form.setReason(clean(form.getReason()));
    }

    private Matrix26BrandingForm toForm(DraftRow row) {
        Matrix26BrandingForm form = new Matrix26BrandingForm();
        form.setDisplayName(row.displayName());
        form.setShortName(row.shortName());
        form.setTagline(row.tagline());
        form.setWelcomeMessage(row.welcomeMessage());
        form.setHeroTitle(row.heroTitle());
        form.setHeroSubtitle(row.heroSubtitle());
        form.setPrimaryCtaLabel(row.primaryCtaLabel());
        form.setSecondaryCtaLabel(row.secondaryCtaLabel());
        form.setContactPhone(row.contactPhone());
        form.setWhatsapp(row.whatsapp());
        form.setLocation(row.location());
        form.setReason(row.reason());
        return form;
    }

    private Optional<DraftRow> draft(Long instanceId) {
        return jdbcTemplate.query(
                """
                SELECT display_name, short_name, tagline, welcome_message,
                       hero_title, hero_subtitle, primary_cta_label,
                       secondary_cta_label, contact_phone, whatsapp, location,
                       reason, updated_by, updated_at
                FROM matrix26_instance_branding_draft
                WHERE instance_id = ?
                """,
                resultSet -> resultSet.next()
                        ? Optional.of(new DraftRow(
                        resultSet.getString("display_name"),
                        resultSet.getString("short_name"),
                        resultSet.getString("tagline"),
                        resultSet.getString("welcome_message"),
                        resultSet.getString("hero_title"),
                        resultSet.getString("hero_subtitle"),
                        resultSet.getString("primary_cta_label"),
                        resultSet.getString("secondary_cta_label"),
                        resultSet.getString("contact_phone"),
                        resultSet.getString("whatsapp"),
                        resultSet.getString("location"),
                        resultSet.getString("reason"),
                        resultSet.getString("updated_by"),
                        resultSet.getTimestamp("updated_at") == null
                                ? null
                                : resultSet.getTimestamp("updated_at").toLocalDateTime()
                ))
                        : Optional.empty(),
                instanceId
        );
    }

    private Map<String, AssetRow> assets(Long instanceId) {
        Map<String, AssetRow> result = new LinkedHashMap<>();
        jdbcTemplate.query(
                """
                SELECT asset_type, relative_path, original_name, content_type,
                       extension, size_bytes, width_px, height_px
                FROM matrix26_instance_branding_asset
                WHERE instance_id = ?
                """,
                resultSet -> {
                    while (resultSet.next()) {
                        AssetRow row = new AssetRow(
                                resultSet.getString("asset_type"),
                                resultSet.getString("relative_path"),
                                resultSet.getString("original_name"),
                                resultSet.getString("content_type"),
                                resultSet.getString("extension"),
                                resultSet.getLong("size_bytes"),
                                (Integer) resultSet.getObject("width_px"),
                                (Integer) resultSet.getObject("height_px")
                        );
                        result.put(row.assetType(), row);
                    }
                    return null;
                },
                instanceId
        );
        return result;
    }

    private Optional<AssetRow> asset(Long instanceId, String type) {
        return Optional.ofNullable(assets(instanceId).get(type));
    }

    private PlatformBusinessClient requireInstance(Long id) {
        return clientRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("La instancia no existe."));
    }

    private Matrix26BrandingAssetType requireType(String code) {
        return Matrix26BrandingAssetType.fromCode(code)
                .orElseThrow(() -> new IllegalArgumentException("El tipo de recurso visual no es válido."));
    }

    private Path draftDirectory(PlatformBusinessClient instance) {
        Path directory = dataRoot.resolve("matrix26-control")
                .resolve("drafts")
                .resolve(safeInstanceCode(instance.getCode()))
                .resolve("branding")
                .normalize();
        assertInside(dataRoot, directory);
        return directory;
    }

    private Path resolveStoredPath(String relativePath) {
        Path path = dataRoot.resolve(relativePath).normalize();
        assertInside(dataRoot, path);
        return path;
    }

    private String safeInstanceCode(String value) {
        String normalized = value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
        if (!SAFE_CODE.matcher(normalized).matches()) {
            throw new IllegalArgumentException("El código de instancia no es seguro para almacenamiento de recursos.");
        }
        return normalized;
    }

    private void assertInside(Path root, Path path) {
        Path normalizedRoot = root.toAbsolutePath().normalize();
        Path normalizedPath = path.toAbsolutePath().normalize();
        if (!normalizedPath.startsWith(normalizedRoot)) {
            throw new IllegalArgumentException("Ruta de almacenamiento no permitida.");
        }
    }

    private String extension(String fileName) {
        String clean = cleanFileName(fileName, "");
        int separator = clean.lastIndexOf('.');
        if (separator < 0 || separator == clean.length() - 1) {
            return "";
        }
        return clean.substring(separator + 1).toLowerCase(Locale.ROOT);
    }

    private String cleanFileName(String fileName, String fallback) {
        if (fileName == null || fileName.isBlank()) {
            return fallback;
        }
        String clean = Path.of(fileName).getFileName().toString().replaceAll("[\\r\\n]", "");
        return shorten(clean, 255);
    }

    private String sha256(byte[] bytes) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return java.util.HexFormat.of().formatHex(digest.digest(bytes));
        } catch (Exception ex) {
            throw new IllegalStateException("No se pudo calcular la huella del archivo.", ex);
        }
    }

    private void activateDirectory(Path temporary, Path current, Path backup) throws IOException {
        deleteQuietly(backup);
        if (Files.exists(current)) {
            moveReplacing(current, backup);
        }
        try {
            moveReplacing(temporary, current);
            deleteQuietly(backup);
        } catch (IOException ex) {
            if (!Files.exists(current) && Files.exists(backup)) {
                moveReplacing(backup, current);
            }
            throw ex;
        }
    }

    private void moveReplacing(Path source, Path target) throws IOException {
        Files.createDirectories(Objects.requireNonNull(target.getParent()));
        try {
            Files.move(source, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } catch (AtomicMoveNotSupportedException ex) {
            Files.move(source, target, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private void copyDirectory(Path source, Path target) throws IOException {
        Files.createDirectories(target);
        try (var stream = Files.walk(source)) {
            for (Path item : stream.toList()) {
                Path relative = source.relativize(item);
                Path destination = target.resolve(relative);
                if (Files.isDirectory(item)) {
                    Files.createDirectories(destination);
                } else {
                    Files.createDirectories(Objects.requireNonNull(destination.getParent()));
                    Files.copy(item, destination, StandardCopyOption.REPLACE_EXISTING);
                }
            }
        }
    }

    private void removeOtherExtensions(Path directory, String stem, String selected) throws IOException {
        try (var stream = Files.list(directory)) {
            for (Path item : stream.toList()) {
                String fileName = item.getFileName().toString();
                if (fileName.startsWith(stem + ".") && !fileName.equals(selected)) {
                    Files.deleteIfExists(item);
                }
            }
        }
    }

    private void deleteQuietly(Path directory) {
        if (directory == null || !Files.exists(directory)) {
            return;
        }
        try (var stream = Files.walk(directory)) {
            List<Path> paths = stream.sorted(java.util.Comparator.reverseOrder()).toList();
            for (Path path : paths) {
                Files.deleteIfExists(path);
            }
        } catch (IOException ignored) {
            // Best-effort cleanup of generated files.
        }
    }

    private int assetVersion(String fileName) {
        int marker = fileName.lastIndexOf("-v");
        int dot = fileName.lastIndexOf('.');
        if (marker < 0 || dot <= marker + 2) {
            return 0;
        }
        try {
            return Integer.parseInt(fileName.substring(marker + 2, dot));
        } catch (NumberFormatException ex) {
            return 0;
        }
    }

    private String stripExtension(String fileName) {
        int separator = fileName.lastIndexOf('.');
        return separator < 0 ? fileName : fileName.substring(0, separator);
    }

    private String safeActor(String actor) {
        return shorten(defaultValue(clean(actor), "matrix26-system"), 120);
    }

    private String required(String value, String label) {
        String clean = clean(value);
        if (clean == null) {
            throw new IllegalArgumentException(label + " es obligatorio.");
        }
        return clean;
    }

    private String clean(String value) {
        if (value == null) {
            return null;
        }
        String clean = value.trim();
        return clean.isBlank() ? null : clean;
    }

    private String nullable(String value) {
        return clean(value);
    }

    private String defaultValue(String value, String fallback) {
        String clean = clean(value);
        return clean == null ? fallback : clean;
    }

    private String shorten(String value, int maximum) {
        if (value == null) {
            return null;
        }
        return value.length() > maximum ? value.substring(0, maximum) : value;
    }

    private record ValidatedImage(
            String extension,
            String contentType,
            Integer width,
            Integer height
    ) {
    }

    private record AssetRow(
            String assetType,
            String relativePath,
            String originalName,
            String contentType,
            String extension,
            long sizeBytes,
            Integer width,
            Integer height
    ) {
    }

    private record DraftRow(
            String displayName,
            String shortName,
            String tagline,
            String welcomeMessage,
            String heroTitle,
            String heroSubtitle,
            String primaryCtaLabel,
            String secondaryCtaLabel,
            String contactPhone,
            String whatsapp,
            String location,
            String reason,
            String updatedBy,
            LocalDateTime updatedAt
    ) {
    }
}
