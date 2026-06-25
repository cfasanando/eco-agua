package com.ecoamazonas.eco_agua.platform.control.backups;

import com.ecoamazonas.eco_agua.platform.PlatformBusinessClient;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

@Component
@ConditionalOnProperty(name = "matrix26.control-center.enabled", havingValue = "true")
public class Matrix26FullBackupAssembler {

    private static final Pattern SENSITIVE_KEY = Pattern.compile(
            "(?i).*(password|passwd|secret|token|api[-_.]?key|private[-_.]?key|credential).*"
    );
    private static final Set<String> DATA_ROOTS = Set.of(
            "appearance", "uploads", "documents", "attachments", "media", "public"
    );
    private static final Set<String> RUNTIME_EXTENSIONS = Set.of(
            ".sh", ".cmd", ".bat", ".ps1", ".json", ".yml", ".yaml", ".txt", ".md"
    );

    private final Matrix26BackupProperties properties;
    private final Matrix26BackupRepository backupRepository;
    private final JdbcTemplate jdbcTemplate;

    public Matrix26FullBackupAssembler(
            Matrix26BackupProperties properties,
            Matrix26BackupRepository backupRepository,
            JdbcTemplate jdbcTemplate
    ) {
        this.properties = properties;
        this.backupRepository = backupRepository;
        this.jdbcTemplate = jdbcTemplate;
    }

    public Matrix26FullBackupResult assemble(
            Matrix26BackupJob job,
            PlatformBusinessClient instance,
            Path backupRoot,
            Path backupDirectory
    ) throws IOException {
        Path projectRoot = Path.of("").toAbsolutePath().normalize();
        Path runtimeRoot = projectRoot.resolve(properties.getRuntimeDirectory()).normalize();
        Path runtimeDirectory = runtimeRoot.resolve(instance.getRuntimeProfile()).normalize();
        ensureInside(runtimeRoot, runtimeDirectory);
        if (!Files.isDirectory(runtimeDirectory)) {
            throw new Matrix26BackupException("The instance runtime directory was not found: " + runtimeDirectory);
        }

        Path runtimeConfig = runtimeDirectory.resolve("application.properties").normalize();
        ensureInside(runtimeDirectory, runtimeConfig);
        if (!Files.isRegularFile(runtimeConfig)) {
            throw new Matrix26BackupException("Runtime application.properties was not found for the full backup.");
        }

        Path runtimeDataRoot = projectRoot.resolve(properties.getRuntimeDataDirectory()).normalize();
        Path instanceData = runtimeDataRoot.resolve(instance.getCode()).normalize();
        ensureInside(runtimeDataRoot, instanceData);

        FileSnapshot before = snapshot(instanceData, runtimeDirectory);

        Path sanitizedConfig = backupDirectory.resolve("runtime-config.properties");
        Path instanceJson = backupDirectory.resolve("instance.json");
        Path modulesJson = backupDirectory.resolve("modules.json");
        Path appearanceJson = backupDirectory.resolve("appearance.json");
        Path inventoryJson = backupDirectory.resolve("files-inventory.json");
        Path diagnosticTail = backupDirectory.resolve("runtime-log-tail.txt");
        Path filesArchive = backupDirectory.resolve("instance-files.zip");

        sanitizeProperties(runtimeConfig, sanitizedConfig);
        writeInstanceMetadata(instanceJson, instance);
        writeModules(modulesJson, instance.getId());
        writeAppearance(appearanceJson, instance.getId());
        writeDiagnosticTail(instanceData, diagnosticTail);

        ArchiveResult archive = createArchive(filesArchive, instance, runtimeDirectory, instanceData);
        FileSnapshot after = snapshot(instanceData, runtimeDirectory);
        writeInventory(inventoryJson, before, after, archive);

        ZipInspection zipInspection = inspectZip(filesArchive);
        verify(job.id(), "FULL_ARCHIVE_READABLE", "Readable instance file archive", zipInspection.readable(), zipInspection.message());
        if (!zipInspection.readable() || zipInspection.entries() <= 0) {
            throw new Matrix26BackupException("The full backup file archive did not pass verification.");
        }

        verify(job.id(), "RUNTIME_CONFIG_REDACTED", "Sanitized runtime configuration", configIsRedacted(sanitizedConfig),
                "Sensitive runtime values are replaced with ***REDACTED***.");
        if (!configIsRedacted(sanitizedConfig)) {
            throw new Matrix26BackupException("The sanitized runtime configuration still contains a sensitive value.");
        }

        boolean stable = before.equals(after);
        backupRepository.insertVerification(
                job.id(),
                "FILESET_STABILITY",
                "File inventory stability",
                stable ? Matrix26BackupVerificationStatus.PASSED : Matrix26BackupVerificationStatus.WARNING,
                stable
                        ? "The instance file inventory did not change during the backup."
                        : "Files changed while the online backup was running. The archive is valid, but restore testing is recommended."
        );

        verify(job.id(), "INSTANCE_OWNERSHIP", "Instance file ownership", archive.invalidPaths() == 0,
                archive.invalidPaths() == 0
                        ? "All archived files belong to the selected instance runtime or runtime-data directory."
                        : archive.invalidPaths() + " unsafe paths were rejected.");
        if (archive.invalidPaths() > 0) {
            throw new Matrix26BackupException("Unsafe file paths were detected while building the instance archive.");
        }

        Map<Path, String> hashes = new LinkedHashMap<>();
        for (Path artifact : List.of(
                filesArchive,
                sanitizedConfig,
                instanceJson,
                modulesJson,
                appearanceJson,
                inventoryJson,
                diagnosticTail
        )) {
            if (Files.isRegularFile(artifact)) {
                hashes.put(artifact, sha256(artifact));
            }
        }

        for (Map.Entry<Path, String> entry : hashes.entrySet()) {
            Path artifact = entry.getKey();
            backupRepository.insertArtifact(
                    job.id(),
                    artifactType(artifact),
                    artifact.getFileName().toString(),
                    relative(backupRoot, artifact),
                    Files.size(artifact),
                    entry.getValue(),
                    "VERIFIED"
            );
        }

        long storedBytes = hashes.keySet().stream().mapToLong(this::sizeQuietly).sum();
        return new Matrix26FullBackupResult(
                filesArchive,
                sanitizedConfig,
                instanceJson,
                modulesJson,
                appearanceJson,
                inventoryJson,
                diagnosticTail,
                hashes,
                archive.entries(),
                archive.sourceBytes(),
                Files.size(filesArchive),
                storedBytes,
                stable,
                archive.skippedSymlinks()
        );
    }

    private ArchiveResult createArchive(
            Path archive,
            PlatformBusinessClient instance,
            Path runtimeDirectory,
            Path instanceData
    ) throws IOException {
        Path temporary = archive.resolveSibling(archive.getFileName() + ".part");
        Files.deleteIfExists(temporary);
        ArchiveCounter counter = new ArchiveCounter();

        try (ZipOutputStream zip = new ZipOutputStream(new BufferedOutputStream(Files.newOutputStream(temporary)))) {
            if (Files.isDirectory(instanceData)) {
                for (String rootName : DATA_ROOTS) {
                    Path root = instanceData.resolve(rootName).normalize();
                    ensureInside(instanceData, root);
                    if (Files.isDirectory(root)) {
                        addTree(zip, root, "runtime-data/" + instance.getCode() + "/" + rootName, this::allowInstanceDataFile, counter);
                    }
                }
            }
            addTree(zip, runtimeDirectory, "runtime-clients/" + instance.getRuntimeProfile(), this::allowRuntimeFile, counter);
        } catch (Exception ex) {
            Files.deleteIfExists(temporary);
            throw ex;
        }

        if (counter.sourceBytes > properties.getMaximumArchiveSourceBytes()) {
            Files.deleteIfExists(temporary);
            throw new Matrix26BackupException(
                    "The instance files exceed the configured full-backup source limit of "
                            + Matrix26BackupService.formatBytes(properties.getMaximumArchiveSourceBytes()) + "."
            );
        }
        Files.move(temporary, archive, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        return new ArchiveResult(counter.entries, counter.sourceBytes, counter.invalidPaths, counter.skippedSymlinks);
    }

    private void addTree(
            ZipOutputStream zip,
            Path root,
            String archivePrefix,
            Predicate<Path> include,
            ArchiveCounter counter
    ) throws IOException {
        Path normalizedRoot = root.toAbsolutePath().normalize();
        Files.walkFileTree(normalizedRoot, new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
                if (Files.isSymbolicLink(dir)) {
                    counter.skippedSymlinks++;
                    return FileVisitResult.SKIP_SUBTREE;
                }
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                if (Files.isSymbolicLink(file)) {
                    counter.skippedSymlinks++;
                    return FileVisitResult.CONTINUE;
                }
                if (!attrs.isRegularFile() || !include.test(file)) {
                    return FileVisitResult.CONTINUE;
                }
                long size = attrs.size();
                if (size > properties.getMaximumSingleFileBytes()) {
                    throw new Matrix26BackupException(
                            "A file exceeds the configured full-backup size limit: " + file.getFileName()
                    );
                }
                String relative = normalizedRoot.relativize(file.toAbsolutePath().normalize()).toString().replace('\\', '/');
                String entryName = archivePrefix + (relative.isBlank() ? "" : "/" + relative);
                if (!safeEntry(entryName)) {
                    counter.invalidPaths++;
                    return FileVisitResult.CONTINUE;
                }
                ZipEntry entry = new ZipEntry(entryName);
                entry.setTime(attrs.lastModifiedTime().toMillis());
                zip.putNextEntry(entry);
                try (InputStream input = new BufferedInputStream(Files.newInputStream(file))) {
                    input.transferTo(zip);
                }
                zip.closeEntry();
                counter.entries++;
                counter.sourceBytes += size;
                if (counter.sourceBytes > properties.getMaximumArchiveSourceBytes()) {
                    throw new Matrix26BackupException("The full-backup archive source limit was exceeded.");
                }
                return FileVisitResult.CONTINUE;
            }
        });
    }

    private boolean allowInstanceDataFile(Path file) {
        String name = file.getFileName().toString().toLowerCase(Locale.ROOT);
        return !name.endsWith(".part")
                && !name.endsWith(".tmp")
                && !name.endsWith(".lock")
                && !name.equals("thumbs.db")
                && !name.equals("desktop.ini");
    }

    private boolean allowRuntimeFile(Path file) {
        String name = file.getFileName().toString().toLowerCase(Locale.ROOT);
        if (name.equals("application.properties") || name.endsWith(".jar") || name.endsWith(".log")) {
            return false;
        }
        if (name.startsWith("readme")) {
            return true;
        }
        return RUNTIME_EXTENSIONS.stream().anyMatch(name::endsWith);
    }

    private void sanitizeProperties(Path source, Path target) throws IOException {
        List<String> output = new ArrayList<>();
        for (String line : Files.readAllLines(source, StandardCharsets.ISO_8859_1)) {
            String trimmed = line.trim();
            if (trimmed.isBlank() || trimmed.startsWith("#") || trimmed.startsWith("!")) {
                output.add(line);
                continue;
            }
            int equals = line.indexOf('=');
            int colon = line.indexOf(':');
            int separator = equals < 0 ? colon : colon < 0 ? equals : Math.min(equals, colon);
            if (separator < 0) {
                output.add(line);
                continue;
            }
            String key = line.substring(0, separator).trim();
            if (SENSITIVE_KEY.matcher(key).matches()) {
                output.add(key + "=***REDACTED***");
            } else {
                output.add(line);
            }
        }
        Files.write(target, output, StandardCharsets.UTF_8);
    }

    private boolean configIsRedacted(Path config) throws IOException {
        for (String line : Files.readAllLines(config, StandardCharsets.UTF_8)) {
            String trimmed = line.trim();
            if (trimmed.isBlank() || trimmed.startsWith("#") || trimmed.startsWith("!")) {
                continue;
            }
            int separator = trimmed.indexOf('=');
            if (separator <= 0) {
                continue;
            }
            String key = trimmed.substring(0, separator).trim();
            String value = trimmed.substring(separator + 1).trim();
            if (SENSITIVE_KEY.matcher(key).matches() && !"***REDACTED***".equals(value)) {
                return false;
            }
        }
        return true;
    }

    private void writeInstanceMetadata(Path target, PlatformBusinessClient instance) throws IOException {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("formatVersion", 1);
        values.put("instanceId", instance.getId());
        values.put("instanceCode", instance.getCode());
        values.put("businessName", instance.getBusinessName());
        values.put("legalName", instance.getLegalName());
        values.put("businessType", instance.getBusinessType());
        values.put("databaseName", instance.getDatabaseName());
        values.put("runtimeProfile", instance.getRuntimeProfile());
        values.put("runtimePort", instance.getRuntimePort());
        values.put("publicUrl", instance.getPublicUrl());
        values.put("currency", instance.getCurrency());
        values.put("city", instance.getCity());
        values.put("status", instance.getStatus());
        values.put("databaseStatus", instance.getDatabaseStatus());
        values.put("runtimeStatus", instance.getRuntimeStatus());
        values.put("protectedInstance", instance.isProtectedInstance());
        values.put("createdAt", Instant.now().toString());
        Files.writeString(target, jsonObject(values), StandardCharsets.UTF_8);
    }

    private void writeModules(Path target, Long instanceId) throws IOException {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                """
                SELECT m.module_key, m.name, m.area, cm.enabled, cm.selection_source
                FROM platform_client_module cm
                JOIN platform_module_catalog m ON m.id = cm.module_id
                WHERE cm.client_id = ?
                ORDER BY m.display_order, m.module_key
                """,
                instanceId
        );
        Files.writeString(target, jsonArray(rows), StandardCharsets.UTF_8);
    }

    private void writeAppearance(Path target, Long instanceId) throws IOException {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                """
                SELECT public_theme_code, public_layout_code, admin_theme_code,
                       admin_layout_code, login_layout_code, overrides_json,
                       status, published_version, published_at, published_by
                FROM matrix26_instance_appearance
                WHERE instance_id = ?
                """,
                instanceId
        );
        Map<String, Object> output = new LinkedHashMap<>();
        output.put("formatVersion", 1);
        output.put("instanceId", instanceId);
        output.put("appearance", rows.isEmpty() ? null : rows.get(0));
        output.put("capturedAt", Instant.now().toString());
        Files.writeString(target, jsonObject(output), StandardCharsets.UTF_8);
    }

    private void writeDiagnosticTail(Path instanceData, Path target) throws IOException {
        Path operations = instanceData.resolve("operations").normalize();
        if (!operations.startsWith(instanceData) || !Files.isDirectory(operations)) {
            Files.writeString(target, "No runtime operation logs were available.\n", StandardCharsets.UTF_8);
            return;
        }
        List<Path> candidates = List.of(
                operations.resolve("application.log"),
                operations.resolve("application-error.log")
        );
        StringBuilder output = new StringBuilder();
        for (Path candidate : candidates) {
            if (!Files.isRegularFile(candidate)) {
                continue;
            }
            output.append("=== ").append(candidate.getFileName()).append(" ===\n");
            List<String> lines = tail(candidate, properties.getDiagnosticLogTailLines());
            for (String line : lines) {
                output.append(sanitizeLog(line)).append('\n');
            }
            output.append('\n');
        }
        if (output.isEmpty()) {
            output.append("No runtime operation logs were available.\n");
        }
        Files.writeString(target, output, StandardCharsets.UTF_8);
    }

    private List<String> tail(Path file, int maximumLines) throws IOException {
        ArrayList<String> ring = new ArrayList<>(Math.max(1, maximumLines));
        try (BufferedReader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (ring.size() == maximumLines) {
                    ring.remove(0);
                }
                ring.add(line);
            }
        }
        return ring;
    }

    private String sanitizeLog(String line) {
        if (line == null) {
            return "";
        }
        return line.replaceAll(
                "(?i)(password|passwd|secret|token|api[-_.]?key|private[-_.]?key)(\\s*[:=]\\s*)([^\\s,;]+)",
                "$1$2***REDACTED***"
        );
    }

    private FileSnapshot snapshot(Path instanceData, Path runtimeDirectory) throws IOException {
        long files = 0;
        long bytes = 0;
        long latest = 0;
        List<Path> roots = new ArrayList<>();
        if (Files.isDirectory(instanceData)) {
            for (String rootName : DATA_ROOTS) {
                Path root = instanceData.resolve(rootName);
                if (Files.isDirectory(root)) {
                    roots.add(root);
                }
            }
        }
        roots.add(runtimeDirectory);
        for (Path root : roots) {
            try (var stream = Files.walk(root)) {
                for (Path file : stream.filter(path -> Files.isRegularFile(path, LinkOption.NOFOLLOW_LINKS)).toList()) {
                    boolean runtimeFileIncluded = allowRuntimeFile(file)
                            || file.getFileName().toString().equalsIgnoreCase("application.properties");
                    if ((root.equals(runtimeDirectory) && !runtimeFileIncluded)
                            || (!root.equals(runtimeDirectory) && !allowInstanceDataFile(file))) {
                        continue;
                    }
                    files++;
                    bytes += Files.size(file);
                    latest = Math.max(latest, Files.getLastModifiedTime(file).toMillis());
                }
            }
        }
        return new FileSnapshot(files, bytes, latest);
    }

    private void writeInventory(Path target, FileSnapshot before, FileSnapshot after, ArchiveResult archive) throws IOException {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("formatVersion", 1);
        values.put("capturedAt", Instant.now().toString());
        values.put("beforeFileCount", before.fileCount());
        values.put("beforeBytes", before.bytes());
        values.put("beforeLatestModified", before.latestModified());
        values.put("afterFileCount", after.fileCount());
        values.put("afterBytes", after.bytes());
        values.put("afterLatestModified", after.latestModified());
        values.put("stable", before.equals(after));
        values.put("archiveEntries", archive.entries());
        values.put("archiveSourceBytes", archive.sourceBytes());
        values.put("skippedSymlinks", archive.skippedSymlinks());
        Files.writeString(target, jsonObject(values), StandardCharsets.UTF_8);
    }

    private ZipInspection inspectZip(Path archive) {
        try (ZipFile zip = new ZipFile(archive.toFile(), StandardCharsets.UTF_8)) {
            int entries = 0;
            var enumeration = zip.entries();
            while (enumeration.hasMoreElements()) {
                ZipEntry entry = enumeration.nextElement();
                if (!safeEntry(entry.getName())) {
                    return new ZipInspection(false, entries, "The archive contains an unsafe path: " + entry.getName());
                }
                if (!entry.isDirectory()) {
                    entries++;
                    try (InputStream input = zip.getInputStream(entry)) {
                        input.transferTo(OutputStream.nullOutputStream());
                    }
                }
            }
            return new ZipInspection(true, entries, "The ZIP archive is readable and contains " + entries + " files.");
        } catch (IOException ex) {
            return new ZipInspection(false, 0, "The ZIP archive could not be read: " + ex.getMessage());
        }
    }

    private boolean safeEntry(String name) {
        if (name == null || name.isBlank() || name.startsWith("/") || name.startsWith("\\")) {
            return false;
        }
        String normalized = name.replace('\\', '/');
        return !normalized.contains("../") && !normalized.equals("..") && !normalized.contains(":");
    }

    private void verify(long jobId, String code, String label, boolean passed, String detail) {
        backupRepository.insertVerification(
                jobId,
                code,
                label,
                passed ? Matrix26BackupVerificationStatus.PASSED : Matrix26BackupVerificationStatus.FAILED,
                detail
        );
    }

    private String artifactType(Path artifact) {
        return switch (artifact.getFileName().toString()) {
            case "instance-files.zip" -> "INSTANCE_FILES";
            case "runtime-config.properties" -> "RUNTIME_CONFIG";
            case "instance.json" -> "INSTANCE_METADATA";
            case "modules.json" -> "MODULES";
            case "appearance.json" -> "APPEARANCE";
            case "files-inventory.json" -> "FILE_INVENTORY";
            case "runtime-log-tail.txt" -> "DIAGNOSTIC_LOG";
            default -> "FULL_BACKUP_FILE";
        };
    }

    private String jsonObject(Map<String, ?> values) {
        StringBuilder output = new StringBuilder("{\n");
        int index = 0;
        for (Map.Entry<String, ?> entry : values.entrySet()) {
            if (index++ > 0) {
                output.append(",\n");
            }
            output.append("  \"").append(json(entry.getKey())).append("\": ")
                    .append(jsonValue(entry.getValue(), 1));
        }
        return output.append("\n}\n").toString();
    }

    private String jsonArray(List<? extends Map<String, ?>> rows) {
        StringBuilder output = new StringBuilder("[\n");
        for (int i = 0; i < rows.size(); i++) {
            if (i > 0) {
                output.append(",\n");
            }
            output.append(indent(jsonObject(rows.get(i)).trim(), 2));
        }
        return output.append("\n]\n").toString();
    }

    private String jsonValue(Object value, int depth) {
        if (value == null) {
            return "null";
        }
        if (value instanceof Number || value instanceof Boolean) {
            return value.toString();
        }
        if (value instanceof Map<?, ?> map) {
            Map<String, Object> converted = new LinkedHashMap<>();
            map.forEach((key, item) -> converted.put(String.valueOf(key), item));
            return jsonObject(converted).trim();
        }
        return "\"" + json(String.valueOf(value)) + "\"";
    }

    private String indent(String value, int spaces) {
        String prefix = " ".repeat(Math.max(0, spaces));
        return value.lines().map(line -> prefix + line).reduce((a, b) -> a + "\n" + b).orElse("");
    }

    private String json(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    private String sha256(Path file) throws IOException {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            try (InputStream input = new BufferedInputStream(Files.newInputStream(file))) {
                byte[] buffer = new byte[64 * 1024];
                int read;
                while ((read = input.read(buffer)) >= 0) {
                    if (read > 0) {
                        digest.update(buffer, 0, read);
                    }
                }
            }
            return HexFormat.of().formatHex(digest.digest());
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 is unavailable.", ex);
        }
    }

    private long sizeQuietly(Path file) {
        try {
            return Files.size(file);
        } catch (IOException ex) {
            return 0L;
        }
    }

    private String relative(Path root, Path file) {
        return root.toAbsolutePath().normalize().relativize(file.toAbsolutePath().normalize())
                .toString().replace('\\', '/');
    }

    private void ensureInside(Path parent, Path child) {
        if (!child.toAbsolutePath().normalize().startsWith(parent.toAbsolutePath().normalize())) {
            throw new Matrix26BackupException("A full-backup path escaped the configured storage boundary.");
        }
    }

    private record FileSnapshot(long fileCount, long bytes, long latestModified) {
    }

    private record ZipInspection(boolean readable, int entries, String message) {
    }

    private record ArchiveResult(long entries, long sourceBytes, int invalidPaths, int skippedSymlinks) {
    }

    private static final class ArchiveCounter {
        private long entries;
        private long sourceBytes;
        private int invalidPaths;
        private int skippedSymlinks;
    }
}
