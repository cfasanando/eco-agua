package com.ecoamazonas.eco_agua.platform.control.backups;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@Component
@ConditionalOnProperty(name = "matrix26.control-center.enabled", havingValue = "true")
public class Matrix26BackupToolLocator {

    private final Matrix26BackupProperties properties;

    public Matrix26BackupToolLocator(Matrix26BackupProperties properties) {
        this.properties = properties;
    }

    public Matrix26BackupToolStatus locate() {
        Set<Path> candidates = new LinkedHashSet<>();
        addCandidate(candidates, properties.getDumpExecutable());
        addCandidate(candidates, System.getenv("MATRIX26_MYSQLDUMP_PATH"));
        findOnPath(candidates, "mysqldump");
        findOnPath(candidates, "mariadb-dump");
        findOnWindowsPath(candidates, "mysqldump.exe");
        findOnWindowsPath(candidates, "mariadb-dump.exe");
        commonWindowsCandidates().forEach(candidates::add);

        for (Path candidate : candidates) {
            if (candidate != null && Files.isRegularFile(candidate)) {
                String version = version(candidate);
                return new Matrix26BackupToolStatus(
                        true,
                        candidate.toAbsolutePath().normalize().toString(),
                        version,
                        "Database dump tool detected."
                );
            }
        }

        return new Matrix26BackupToolStatus(
                false,
                "",
                "",
                "mysqldump or mariadb-dump was not found. Configure MATRIX26_MYSQLDUMP_PATH with the full executable path."
        );
    }

    private void addCandidate(Set<Path> candidates, String value) {
        if (value == null || value.isBlank()) {
            return;
        }
        try {
            candidates.add(Path.of(value.trim()).toAbsolutePath().normalize());
        } catch (RuntimeException ignored) {
            // Invalid user configuration is reported through the final unavailable state.
        }
    }

    private void findOnPath(Set<Path> candidates, String command) {
        ProcessBuilder builder = new ProcessBuilder(isWindows() ? "where.exe" : "which", command);
        try {
            Process process = builder.start();
            List<String> lines = readLines(process);
            if (process.waitFor(4, TimeUnit.SECONDS) && process.exitValue() == 0) {
                for (String line : lines) {
                    addCandidate(candidates, line);
                }
            } else {
                process.destroyForcibly();
            }
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        } catch (IOException ignored) {
            // The next candidate may still be available.
        }
    }

    private void findOnWindowsPath(Set<Path> candidates, String command) {
        if (!isWindows()) {
            return;
        }
        findOnPath(candidates, command);
    }

    private List<Path> commonWindowsCandidates() {
        if (!isWindows()) {
            return List.of(
                    Path.of("/usr/bin/mysqldump"),
                    Path.of("/usr/bin/mariadb-dump"),
                    Path.of("/usr/local/bin/mysqldump")
            );
        }
        List<Path> values = new ArrayList<>();
        String[] roots = {
                System.getenv("ProgramFiles"),
                System.getenv("ProgramFiles(x86)"),
                "C:/xampp",
                "C:/laragon"
        };
        String[] suffixes = {
                "MySQL/MySQL Server 8.4/bin/mysqldump.exe",
                "MySQL/MySQL Server 8.0/bin/mysqldump.exe",
                "MariaDB 11.4/bin/mariadb-dump.exe",
                "MariaDB 10.11/bin/mariadb-dump.exe",
                "MariaDB 10.6/bin/mysqldump.exe",
                "mysql/bin/mysqldump.exe"
        };
        for (String root : roots) {
            if (root == null || root.isBlank()) {
                continue;
            }
            for (String suffix : suffixes) {
                values.add(Path.of(root, suffix));
            }
        }
        return values;
    }

    private String version(Path executable) {
        try {
            Process process = new ProcessBuilder(executable.toString(), "--version")
                    .redirectErrorStream(true)
                    .start();
            List<String> lines = readLines(process);
            if (!process.waitFor(5, TimeUnit.SECONDS)) {
                process.destroyForcibly();
                return "Version check timed out";
            }
            return lines.isEmpty() ? "Version unavailable" : lines.get(0).trim();
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            return "Version unavailable";
        } catch (IOException ex) {
            return "Version unavailable";
        }
    }

    private List<String> readLines(Process process) throws IOException {
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8)
        )) {
            return reader.lines().filter(line -> !line.isBlank()).toList();
        }
    }

    private boolean isWindows() {
        return System.getProperty("os.name", "").toLowerCase(Locale.ROOT).contains("win");
    }
}
