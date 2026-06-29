package com.ecoamazonas.eco_agua.platform.control.purge;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Stream;

@Component
@ConditionalOnProperty(name = "matrix26.control-center.enabled", havingValue = "true")
public class Matrix26HistoricalArchiveExecutor {
    private static final Set<String> ALLOWED_ARCHIVE_FILE_NAMES = Set.of(
            "package.m26backup",
            "public-manifest.json",
            "manifest.json",
            "checksums.sha256",
            "backup-report.txt",
            "database.sql.gz",
            "instance-files.zip",
            "runtime-config.properties",
            "instance.json",
            "modules.json",
            "appearance.json",
            "files-inventory.json",
            "runtime-log-tail.txt"
    );

    private final Matrix26PurgeProperties properties;

    public Matrix26HistoricalArchiveExecutor(Matrix26PurgeProperties properties) {
        this.properties = properties;
    }

    public ExecutionResult destroy(Matrix26ArchiveDestructionPlan plan, Matrix26ArchiveDestructionItem item) {
        if (item.resourcePath() == null || item.resourcePath().isBlank()) {
            throw new Matrix26PurgeException("Archive destruction item has no resource path.");
        }
        return switch (item.resourceType()) {
            case "FINAL_ARCHIVE_PACKAGE" -> destroyFile(plan, item.resourcePath());
            case "FINAL_BACKUP_DIRECTORY" -> destroyDirectory(plan, item.resourcePath());
            default -> throw new Matrix26PurgeException("Unsupported archive destruction resource type: " + item.resourceType());
        };
    }

    private ExecutionResult destroyFile(Matrix26ArchiveDestructionPlan plan, String rawPath) {
        Path path = safeArchivePath(plan, rawPath);
        if (!Files.exists(path)) {
            return new ExecutionResult("NOT_FOUND", "The archive file was already absent: " + path);
        }
        if (!Files.isRegularFile(path)) {
            throw new Matrix26PurgeException("Archive package path is not a regular file: " + path);
        }
        validateAllowedFile(path);
        try {
            Files.delete(path);
            return new ExecutionResult("DESTROYED", "Destroyed archive file: " + path);
        } catch (IOException ex) {
            throw new Matrix26PurgeException("Could not destroy archive file " + path + ": " + ex.getMessage());
        }
    }

    private ExecutionResult destroyDirectory(Matrix26ArchiveDestructionPlan plan, String rawPath) {
        Path directory = safeArchivePath(plan, rawPath);
        if (!Files.exists(directory)) {
            return new ExecutionResult("NOT_FOUND", "The archive directory was already absent: " + directory);
        }
        if (!Files.isDirectory(directory)) {
            throw new Matrix26PurgeException("Archive directory path is not a directory: " + directory);
        }
        List<Path> entries = walk(directory);
        for (Path entry : entries) {
            if (Files.isRegularFile(entry)) {
                validateAllowedFile(entry);
            }
        }
        entries.sort(Comparator.reverseOrder());
        int removed = 0;
        for (Path entry : entries) {
            try {
                if (Files.exists(entry)) {
                    Files.delete(entry);
                    removed++;
                }
            } catch (IOException ex) {
                throw new Matrix26PurgeException("Could not destroy archive directory entry " + entry + ": " + ex.getMessage());
            }
        }
        return new ExecutionResult("DESTROYED", "Destroyed archive directory: " + directory + ". Removed entries: " + removed + ".");
    }

    private Path safeArchivePath(Matrix26ArchiveDestructionPlan plan, String rawPath) {
        Path root = Path.of(properties.getBackupRootDirectory()).toAbsolutePath().normalize();
        Path input = Path.of(rawPath);
        Path target = input.isAbsolute() ? input.toAbsolutePath().normalize() : root.resolve(input).normalize();
        if (target.equals(root)) {
            throw new Matrix26PurgeException("Refusing to destroy the backup root directory.");
        }
        if (!target.startsWith(root)) {
            throw new Matrix26PurgeException("Refusing to destroy a path outside the configured backup root: " + target);
        }
        String targetText = target.toString().toLowerCase(Locale.ROOT);
        String instanceCode = plan.instanceCode() == null ? "" : plan.instanceCode().toLowerCase(Locale.ROOT);
        if (instanceCode.isBlank() || !targetText.contains(instanceCode)) {
            throw new Matrix26PurgeException("Refusing to destroy archive path because it does not include the instance code: " + target);
        }
        return target;
    }

    private List<Path> walk(Path directory) {
        try (Stream<Path> stream = Files.walk(directory)) {
            return new ArrayList<>(stream.toList());
        } catch (IOException ex) {
            throw new Matrix26PurgeException("Could not inspect archive directory before destruction: " + ex.getMessage());
        }
    }

    private void validateAllowedFile(Path path) {
        String fileName = path.getFileName() == null ? "" : path.getFileName().toString();
        if (!ALLOWED_ARCHIVE_FILE_NAMES.contains(fileName)) {
            throw new Matrix26PurgeException("Refusing to destroy unexpected archive file: " + path);
        }
    }

    public record ExecutionResult(String status, String detail) {}
}
