package com.ecoamazonas.eco_agua.platform.control.operations;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@ConditionalOnProperty(name = "matrix26.control-center.enabled", havingValue = "true")
public class Matrix26SystemProbe {

    private static final Pattern WINDOWS_NETSTAT = Pattern.compile(
            "^\\s*TCP\\s+\\S+:(\\d+)\\s+\\S+\\s+LISTENING\\s+(\\d+)\\s*$",
            Pattern.CASE_INSENSITIVE
    );
    private static final Pattern UNIX_SS_PORT = Pattern.compile("(?:\\]|:)(\\d+)\\s+");
    private static final Pattern UNIX_SS_PID = Pattern.compile("pid=(\\d+)");

    public Matrix26SystemSnapshot capture(List<Integer> expectedPorts) {
        Map<Long, Matrix26ProcessInfo> processes = captureProcesses();
        List<String> warnings = new ArrayList<>();
        Map<Integer, Matrix26PortBinding> ports = captureListeningPorts(processes, warnings);

        for (Integer expectedPort : expectedPorts) {
            if (expectedPort == null || expectedPort < 1 || expectedPort > 65535 || ports.containsKey(expectedPort)) {
                continue;
            }
            if (!isPortAvailable(expectedPort)) {
                ports.put(expectedPort, new Matrix26PortBinding(
                        expectedPort,
                        "unknown",
                        null,
                        "Unknown process",
                        ""
                ));
            }
        }

        Map<Integer, Matrix26PortBinding> orderedPorts = new LinkedHashMap<>();
        ports.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .forEach(entry -> orderedPorts.put(entry.getKey(), entry.getValue()));

        return new Matrix26SystemSnapshot(
                LocalDateTime.now(),
                Map.copyOf(processes),
                Map.copyOf(orderedPorts),
                List.copyOf(warnings)
        );
    }

    private Map<Long, Matrix26ProcessInfo> captureProcesses() {
        Map<Long, Matrix26ProcessInfo> result = new LinkedHashMap<>();
        ProcessHandle.allProcesses()
                .sorted(Comparator.comparingLong(ProcessHandle::pid))
                .forEach(handle -> {
                    ProcessHandle.Info info = handle.info();
                    Long parentPid = handle.parent().map(ProcessHandle::pid).orElse(null);
                    String executable = info.command().orElse("");
                    String commandLine = info.commandLine().orElseGet(() -> buildCommandLine(info));
                    result.put(handle.pid(), new Matrix26ProcessInfo(
                            handle.pid(),
                            parentPid,
                            Matrix26OperationsSanitizer.sanitize(executable),
                            Matrix26OperationsSanitizer.sanitize(commandLine),
                            Matrix26OperationsSanitizer.sanitize(info.user().orElse("")),
                            info.startInstant().orElse(null),
                            handle.isAlive()
                    ));
                });
        return result;
    }

    private String buildCommandLine(ProcessHandle.Info info) {
        String command = info.command().orElse("");
        String[] arguments = info.arguments().orElse(new String[0]);
        if (arguments.length == 0) {
            return command;
        }
        return command + " " + String.join(" ", arguments);
    }

    private Map<Integer, Matrix26PortBinding> captureListeningPorts(
            Map<Long, Matrix26ProcessInfo> processes,
            List<String> warnings
    ) {
        if (isWindows()) {
            Map<Integer, Matrix26PortBinding> powershell = captureWindowsPortsWithPowerShell(processes);
            if (!powershell.isEmpty()) {
                return powershell;
            }
            Map<Integer, Matrix26PortBinding> netstat = captureWindowsPortsWithNetstat(processes);
            if (!netstat.isEmpty()) {
                warnings.add("PowerShell port inventory was unavailable; Windows netstat fallback was used.");
                return netstat;
            }
            warnings.add("Windows could not expose PID ownership for listening ports. Port occupancy fallback is active.");
            return new LinkedHashMap<>();
        }

        Map<Integer, Matrix26PortBinding> ss = captureUnixPorts(processes);
        if (ss.isEmpty()) {
            warnings.add("The operating system did not expose listening port ownership. Port occupancy fallback is active.");
        }
        return ss;
    }

    private Map<Integer, Matrix26PortBinding> captureWindowsPortsWithPowerShell(
            Map<Long, Matrix26ProcessInfo> processes
    ) {
        String script = "[Console]::OutputEncoding=[System.Text.UTF8Encoding]::new(); "
                + "$ErrorActionPreference='SilentlyContinue'; "
                + "Get-NetTCPConnection -State Listen | ForEach-Object { "
                + "Write-Output ($_.LocalAddress + \"`t\" + $_.LocalPort + \"`t\" + $_.OwningProcess) }";
        List<String> lines = execute(List.of(
                "powershell.exe",
                "-NoProfile",
                "-NonInteractive",
                "-Command",
                script
        ), 4);

        Map<Integer, Matrix26PortBinding> result = new LinkedHashMap<>();
        for (String line : lines) {
            String[] parts = line.split("\\t", -1);
            if (parts.length < 3) {
                continue;
            }
            Integer port = parseInteger(parts[1]);
            Long pid = parseLong(parts[2]);
            if (port == null) {
                continue;
            }
            result.putIfAbsent(port, binding(port, parts[0], pid, processes));
        }
        return result;
    }

    private Map<Integer, Matrix26PortBinding> captureWindowsPortsWithNetstat(
            Map<Long, Matrix26ProcessInfo> processes
    ) {
        List<String> lines = execute(List.of("netstat", "-ano", "-p", "tcp"), 4);
        Map<Integer, Matrix26PortBinding> result = new LinkedHashMap<>();
        for (String line : lines) {
            Matcher matcher = WINDOWS_NETSTAT.matcher(line);
            if (!matcher.matches()) {
                continue;
            }
            Integer port = parseInteger(matcher.group(1));
            Long pid = parseLong(matcher.group(2));
            if (port != null) {
                result.putIfAbsent(port, binding(port, "unknown", pid, processes));
            }
        }
        return result;
    }

    private Map<Integer, Matrix26PortBinding> captureUnixPorts(Map<Long, Matrix26ProcessInfo> processes) {
        List<String> lines = execute(List.of("ss", "-ltnpH"), 4);
        Map<Integer, Matrix26PortBinding> result = new LinkedHashMap<>();
        for (String line : lines) {
            Matcher portMatcher = UNIX_SS_PORT.matcher(line);
            if (!portMatcher.find()) {
                continue;
            }
            Integer port = parseInteger(portMatcher.group(1));
            Matcher pidMatcher = UNIX_SS_PID.matcher(line);
            Long pid = pidMatcher.find() ? parseLong(pidMatcher.group(1)) : null;
            if (port != null) {
                result.putIfAbsent(port, binding(port, "unknown", pid, processes));
            }
        }
        return result;
    }

    private Matrix26PortBinding binding(
            int port,
            String address,
            Long pid,
            Map<Long, Matrix26ProcessInfo> processes
    ) {
        Matrix26ProcessInfo process = pid != null ? processes.get(pid) : null;
        String processName = process == null ? "Unknown process" : executableName(process.executable());
        String commandLine = process == null ? "" : process.commandLine();
        return new Matrix26PortBinding(port, address, pid, processName, commandLine);
    }

    private List<String> execute(List<String> command, int timeoutSeconds) {
        Process process = null;
        try {
            process = new ProcessBuilder(command)
                    .redirectErrorStream(true)
                    .start();

            AtomicReference<byte[]> captured = new AtomicReference<>(new byte[0]);
            Process activeProcess = process;
            Thread reader = new Thread(() -> {
                try {
                    captured.set(activeProcess.getInputStream().readAllBytes());
                } catch (IOException ignored) {
                    captured.set(new byte[0]);
                }
            }, "matrix26-system-probe-reader");
            reader.setDaemon(true);
            reader.start();

            boolean finished = process.waitFor(timeoutSeconds, TimeUnit.SECONDS);
            if (!finished) {
                process.destroy();
                if (!process.waitFor(500, TimeUnit.MILLISECONDS)) {
                    process.destroyForcibly();
                }
            }
            reader.join(1_000);
            if (!finished) {
                return List.of();
            }

            String output = new String(captured.get(), StandardCharsets.UTF_8);
            return output.lines().limit(10_000).toList();
        } catch (IOException ex) {
            return List.of();
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            return List.of();
        }
    }

    private boolean isPortAvailable(int port) {
        try (ServerSocket socket = new ServerSocket()) {
            socket.setReuseAddress(false);
            socket.bind(new InetSocketAddress("127.0.0.1", port), 1);
            return true;
        } catch (IOException ex) {
            return false;
        }
    }

    private boolean isWindows() {
        return System.getProperty("os.name", "").toLowerCase(Locale.ROOT).contains("win");
    }

    private String executableName(String executable) {
        if (executable == null || executable.isBlank()) {
            return "Unknown process";
        }
        String normalized = executable.replace('\\', '/');
        int separator = normalized.lastIndexOf('/');
        return separator >= 0 ? normalized.substring(separator + 1) : normalized;
    }

    private Integer parseInteger(String value) {
        try {
            return Integer.valueOf(value.trim());
        } catch (RuntimeException ex) {
            return null;
        }
    }

    private Long parseLong(String value) {
        try {
            return Long.valueOf(value.trim());
        } catch (RuntimeException ex) {
            return null;
        }
    }

    static LocalDateTime toLocalDateTime(Instant value) {
        return value == null ? null : LocalDateTime.ofInstant(value, ZoneId.systemDefault());
    }
}
