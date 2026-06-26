package com.ecoamazonas.eco_agua.platform.control.restores;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;

final class Matrix26RestoreDumpSnapshot {

    private static final Pattern CREATE_TABLE = Pattern.compile(
            "^CREATE\\s+TABLE\\s+(?:IF\\s+NOT\\s+EXISTS\\s+)?`([^`]+)`",
            Pattern.CASE_INSENSITIVE
    );
    private static final Pattern INSERT_TABLE = Pattern.compile(
            "^INSERT\\s+INTO\\s+`([^`]+)`\\s+VALUES\\s*",
            Pattern.CASE_INSENSITIVE
    );

    private Matrix26RestoreDumpSnapshot() {
    }

    static Snapshot read(Path gzipDump) throws IOException {
        Map<String, String> createStatements = new LinkedHashMap<>();
        Map<String, Long> rowCounts = new LinkedHashMap<>();
        String currentCreateTable = null;
        StringBuilder createBuffer = new StringBuilder();
        InsertCounter insertCounter = null;

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                new GZIPInputStream(Files.newInputStream(gzipDump)), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (currentCreateTable != null) {
                    createBuffer.append('\n').append(line);
                    if (statementEnded(line)) {
                        createStatements.put(currentCreateTable, normalizeCreateStatement(createBuffer.toString()));
                        rowCounts.putIfAbsent(currentCreateTable, 0L);
                        currentCreateTable = null;
                        createBuffer.setLength(0);
                    }
                    continue;
                }

                if (insertCounter != null) {
                    insertCounter.accept(line);
                    if (insertCounter.finished()) {
                        rowCounts.merge(insertCounter.tableName(), insertCounter.rows(), Long::sum);
                        insertCounter = null;
                    }
                    continue;
                }

                Matcher createMatcher = CREATE_TABLE.matcher(line.trim());
                if (createMatcher.find()) {
                    currentCreateTable = createMatcher.group(1);
                    createBuffer.append(line);
                    if (statementEnded(line)) {
                        createStatements.put(currentCreateTable, normalizeCreateStatement(createBuffer.toString()));
                        rowCounts.putIfAbsent(currentCreateTable, 0L);
                        currentCreateTable = null;
                        createBuffer.setLength(0);
                    }
                    continue;
                }

                Matcher insertMatcher = INSERT_TABLE.matcher(line.trim());
                if (insertMatcher.find()) {
                    String table = insertMatcher.group(1);
                    int valuesOffset = line.toUpperCase(Locale.ROOT).indexOf("VALUES");
                    String payload = valuesOffset < 0 ? "" : line.substring(valuesOffset + "VALUES".length());
                    insertCounter = new InsertCounter(table);
                    insertCounter.accept(payload);
                    if (insertCounter.finished()) {
                        rowCounts.merge(table, insertCounter.rows(), Long::sum);
                        insertCounter = null;
                    }
                }
            }
        }

        if (currentCreateTable != null && createBuffer.length() > 0) {
            createStatements.put(currentCreateTable, normalizeCreateStatement(createBuffer.toString()));
            rowCounts.putIfAbsent(currentCreateTable, 0L);
        }
        if (insertCounter != null) {
            rowCounts.merge(insertCounter.tableName(), insertCounter.rows(), Long::sum);
        }
        return new Snapshot(Map.copyOf(createStatements), Map.copyOf(rowCounts));
    }

    static String normalizeCreateStatement(String sql) {
        if (sql == null) {
            return "";
        }
        return sql
                .replaceAll("(?is)/\\*!\\d+.*?\\*/", " ")
                .replaceAll("(?i)CREATE\\s+TABLE\\s+IF\\s+NOT\\s+EXISTS", "CREATE TABLE")
                .replaceAll("(?i)AUTO_INCREMENT\\s*=\\s*\\d+", "AUTO_INCREMENT")
                .replaceAll("(?i)DEFINER\\s*=\\s*`[^`]+`@`[^`]+`", "DEFINER")
                .replaceAll("\\s+", " ")
                .trim();
    }

    private static boolean statementEnded(String line) {
        return line != null && line.trim().endsWith(";");
    }

    record Snapshot(Map<String, String> createStatements, Map<String, Long> rowCounts) {
    }

    private static final class InsertCounter {
        private final String tableName;
        private long rows;
        private int depth;
        private boolean inString;
        private boolean escaped;
        private boolean finished;

        private InsertCounter(String tableName) {
            this.tableName = tableName;
        }

        void accept(String text) {
            if (finished || text == null) {
                return;
            }
            for (int i = 0; i < text.length(); i++) {
                char value = text.charAt(i);
                if (escaped) {
                    escaped = false;
                    continue;
                }
                if (inString && value == '\\') {
                    escaped = true;
                    continue;
                }
                if (value == '\'') {
                    inString = !inString;
                    continue;
                }
                if (inString) {
                    continue;
                }
                if (value == '(') {
                    if (depth == 0) {
                        rows++;
                    }
                    depth++;
                } else if (value == ')' && depth > 0) {
                    depth--;
                } else if (value == ';' && depth == 0) {
                    finished = true;
                    break;
                }
            }
        }

        String tableName() { return tableName; }
        long rows() { return rows; }
        boolean finished() { return finished; }
    }
}
