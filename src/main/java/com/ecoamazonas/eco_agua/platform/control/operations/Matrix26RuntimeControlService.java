package com.ecoamazonas.eco_agua.platform.control.operations;

import com.ecoamazonas.eco_agua.platform.PlatformBusinessClient;
import com.ecoamazonas.eco_agua.platform.PlatformBusinessClientRepository;
import com.ecoamazonas.eco_agua.platform.control.Matrix26InstanceAuditLog;
import com.ecoamazonas.eco_agua.platform.control.Matrix26InstanceAuditLogRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.ServerSocket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Stream;

@Service
@ConditionalOnProperty(name = "matrix26.control-center.enabled", havingValue = "true")
public class Matrix26RuntimeControlService {

    private static final Logger LOGGER = LoggerFactory.getLogger(Matrix26RuntimeControlService.class);
    private static final Set<String> JAVA_EXECUTABLE_NAMES = Set.of("java", "java.exe", "javaw", "javaw.exe");

    private final Matrix26OperationsInventoryService inventoryService;
    private final Matrix26OperationsProperties properties;
    private final PlatformBusinessClientRepository clientRepository;
    private final Matrix26RuntimeOperationRepository operationRepository;
    private final Matrix26RuntimeManagedStateRepository stateRepository;
    private final Matrix26InstanceAuditLogRepository auditRepository;
    private final Map<String, ReentrantLock> locks = new ConcurrentHashMap<>();

    public Matrix26RuntimeControlService(
            Matrix26OperationsInventoryService inventoryService,
            Matrix26OperationsProperties properties,
            PlatformBusinessClientRepository clientRepository,
            Matrix26RuntimeOperationRepository operationRepository,
            Matrix26RuntimeManagedStateRepository stateRepository,
            Matrix26InstanceAuditLogRepository auditRepository
    ) {
        this.inventoryService = inventoryService;
        this.properties = properties;
        this.clientRepository = clientRepository;
        this.operationRepository = operationRepository;
        this.stateRepository = stateRepository;
        this.auditRepository = auditRepository;
    }

    public Matrix26RuntimeControlView view(Matrix26RuntimeInventoryItem runtime) {
        Matrix26RuntimeTarget target = runtime.target();
        Matrix26RuntimeManagedState managedState = target.instanceId() == null
                ? null
                : stateRepository.findByInstance_Id(target.instanceId()).orElse(null);
        Matrix26RuntimeOperation lastOperation = target.instanceId() == null
                ? null
                : operationRepository.findTopByInstance_IdOrderByRequestedAtDesc(target.instanceId()).orElse(null);

        boolean inProgress = lastOperation != null
                && (lastOperation.getStatus() == Matrix26RuntimeOperationStatus.REQUESTED
                || lastOperation.getStatus() == Matrix26RuntimeOperationStatus.RUNNING);
        String reason = manageabilityReason(runtime);
        boolean manageable = reason.isBlank();
        boolean portOwnedByExpectedRuntime = runtime.portListening()
                && runtime.expectedProcess()
                && runtime.processId() != null;
        boolean stopped = !runtime.portListening() && runtime.processId() == null;

        return new Matrix26RuntimeControlView(
                properties.isRuntimeControlEnabled(),
                manageable,
                inProgress,
                manageable && !inProgress && stopped,
                manageable && !inProgress && portOwnedByExpectedRuntime,
                manageable && !inProgress && portOwnedByExpectedRuntime,
                reason,
                "STOP " + target.code(),
                "RESTART " + target.code(),
                managedState,
                lastOperation
        );
    }

    public Map<String, Matrix26RuntimeControlView> views(List<Matrix26RuntimeInventoryItem> runtimes) {
        Map<String, Matrix26RuntimeControlView> result = new LinkedHashMap<>();
        for (Matrix26RuntimeInventoryItem runtime : runtimes) {
            result.put(runtime.target().key(), view(runtime));
        }
        return result;
    }

    public List<Matrix26RuntimeOperation> recentOperations() {
        return operationRepository.findTop100ByOrderByRequestedAtDesc();
    }

    public List<Matrix26RuntimeOperation> operationsForInstance(Long instanceId) {
        if (instanceId == null) {
            return List.of();
        }
        return operationRepository.findTop20ByInstance_IdOrderByRequestedAtDesc(instanceId);
    }

    public Matrix26RuntimeControlResult start(String runtimeKey, String actor) {
        return executeLocked(runtimeKey, Matrix26RuntimeOperationAction.START, actor, null);
    }

    public Matrix26RuntimeControlResult stop(String runtimeKey, String actor, String confirmation) {
        return executeLocked(runtimeKey, Matrix26RuntimeOperationAction.STOP, actor, confirmation);
    }

    public Matrix26RuntimeControlResult restart(String runtimeKey, String actor, String confirmation) {
        return executeLocked(runtimeKey, Matrix26RuntimeOperationAction.RESTART, actor, confirmation);
    }

    private Matrix26RuntimeControlResult executeLocked(
            String runtimeKey,
            Matrix26RuntimeOperationAction action,
            String actor,
            String confirmation
    ) {
        ReentrantLock lock = locks.computeIfAbsent(runtimeKey, ignored -> new ReentrantLock());
        if (!lock.tryLock()) {
            throw new Matrix26RuntimeControlException("Another runtime operation is already in progress.");
        }

        try {
            inventoryService.invalidateCache();
            Matrix26RuntimeInventoryItem runtime = inventoryService.runtime(runtimeKey, true);
            validateManageable(runtime);
            validateConfirmation(runtime, action, confirmation);

            PlatformBusinessClient instance = clientRepository.findById(runtime.target().instanceId())
                    .orElseThrow(() -> new Matrix26RuntimeControlException("The registered instance no longer exists."));
            Matrix26RuntimeOperation operation = newOperation(instance, runtime, action, actor);
            operationRepository.save(operation);

            try {
                operation.setStatus(Matrix26RuntimeOperationStatus.RUNNING);
                operation.setStartedAt(LocalDateTime.now());
                operationRepository.save(operation);

                Matrix26RuntimeControlResult result = switch (action) {
                    case START -> startInternal(instance, runtime, operation, false);
                    case STOP -> stopInternal(instance, runtime, operation, false);
                    case RESTART -> restartInternal(instance, runtime, operation);
                };
                recordAudit(instance, operation, actor, result.message());
                return result;
            } catch (Matrix26RuntimeControlException ex) {
                failOperation(operation, ex.getMessage(), ex);
                recordAudit(instance, operation, actor, "Runtime operation failed: " + safe(ex.getMessage()));
                throw ex;
            } catch (RuntimeException ex) {
                String message = "The runtime operation failed unexpectedly.";
                failOperation(operation, message, ex);
                recordAudit(instance, operation, actor, message);
                throw new Matrix26RuntimeControlException(message, ex);
            }
        } finally {
            lock.unlock();
        }
    }

    private Matrix26RuntimeControlResult startInternal(
            PlatformBusinessClient instance,
            Matrix26RuntimeInventoryItem runtime,
            Matrix26RuntimeOperation operation,
            boolean restarting
    ) {
        inventoryService.invalidateCache();
        Matrix26RuntimeInventoryItem current = inventoryService.runtime(runtime.target().key(), true);
        if (current.portListening() || current.processId() != null) {
            throw new Matrix26RuntimeControlException("The runtime is already running or its port is occupied.");
        }
        if (!isPortAvailable(current.target().expectedPort())) {
            throw new Matrix26RuntimeControlException("The expected port is not available.");
        }

        RuntimePaths paths = resolvePaths(current);
        Path applicationJar = findApplicationJar(paths.projectRoot());
        Path javaExecutable = resolveJavaExecutable();
        ensureOperationFiles(paths);

        Matrix26RuntimeManagedState managedState = state(instance, current);
        updateManagedState(
                managedState,
                restarting ? Matrix26ManagedRuntimeState.RESTARTING : Matrix26ManagedRuntimeState.STARTING,
                null,
                "Starting runtime.",
                operation,
                paths
        );
        instance.setRuntimeStatus(restarting ? "RESTARTING" : "STARTING");
        clientRepository.save(instance);

        appendOperationMarker(paths.standardLog(), "Starting runtime through Matrix26.");
        List<String> command = List.of(
                javaExecutable.toString(),
                "-jar",
                applicationJar.toString(),
                "--spring.config.additional-location=" + paths.configuration().toUri()
        );

        Process process;
        try {
            ProcessBuilder builder = new ProcessBuilder(command)
                    .directory(paths.projectRoot().toFile())
                    .redirectOutput(ProcessBuilder.Redirect.appendTo(paths.standardLog().toFile()))
                    .redirectError(ProcessBuilder.Redirect.appendTo(paths.errorLog().toFile()));
            process = builder.start();
        } catch (IOException ex) {
            throw new Matrix26RuntimeControlException("The Java process could not be started.", ex);
        }

        long pid = process.pid();
        writePidFile(paths.pidFile(), pid, instance.getCode(), current.target().expectedPort());
        operation.setResultingPid(pid);
        operation.setStandardLogPath(relative(paths.projectRoot(), paths.standardLog()));
        operation.setErrorLogPath(relative(paths.projectRoot(), paths.errorLog()));
        operationRepository.save(operation);

        managedState.setLastKnownPid(pid);
        managedState.setProcessStartedAt(LocalDateTime.now());
        managedState.setMessage("Process started. Waiting for the health check.");
        stateRepository.save(managedState);

        int timeoutSeconds = Math.max(10, properties.getStartTimeoutSeconds());
        int pollMs = Math.max(250, Math.min(properties.getPollIntervalMs(), 3_000));
        Instant deadline = Instant.now().plusSeconds(timeoutSeconds);
        Matrix26RuntimeInventoryItem detected = current;

        while (Instant.now().isBefore(deadline)) {
            if (!process.isAlive()) {
                int exitCode = process.exitValue();
                throw new Matrix26RuntimeControlException(
                        "The Java process exited before the portal became available. Exit code: " + exitCode + "."
                );
            }
            sleep(pollMs);
            inventoryService.invalidateCache();
            detected = inventoryService.runtime(current.target().key(), true);
            if (detected.http().online() && detected.expectedProcess()) {
                return completeStart(instance, operation, managedState, paths, detected, pid);
            }
        }

        managedState.setCurrentState(Matrix26ManagedRuntimeState.DEGRADED);
        managedState.setLastKnownPid(pid);
        managedState.setMessage("The process is running, but the HTTP health check did not become available in time.");
        managedState.setLastOperationId(operation.getId());
        stateRepository.save(managedState);
        instance.setRuntimeStatus("DEGRADED");
        clientRepository.save(instance);
        operation.setStatus(Matrix26RuntimeOperationStatus.FAILED);
        operation.setFinalState("DEGRADED");
        operation.setCompletedAt(LocalDateTime.now());
        operation.setDurationMs(durationMs(operation));
        operation.setMessage("Process started, but the health check timed out.");
        operationRepository.save(operation);
        throw new Matrix26RuntimeControlException(
                "The process is running with PID " + pid + ", but the portal did not answer before the timeout. Review the runtime logs."
        );
    }

    private Matrix26RuntimeControlResult completeStart(
            PlatformBusinessClient instance,
            Matrix26RuntimeOperation operation,
            Matrix26RuntimeManagedState managedState,
            RuntimePaths paths,
            Matrix26RuntimeInventoryItem detected,
            long pid
    ) {
        LocalDateTime now = LocalDateTime.now();
        managedState.setCurrentState(Matrix26ManagedRuntimeState.ONLINE);
        managedState.setLastKnownPid(pid);
        managedState.setLastOnlineAt(now);
        managedState.setLastOperationId(operation.getId());
        managedState.setMessage("Runtime online and verified by HTTP.");
        managedState.setStandardLogPath(relative(paths.projectRoot(), paths.standardLog()));
        managedState.setErrorLogPath(relative(paths.projectRoot(), paths.errorLog()));
        managedState.setPidFilePath(relative(paths.projectRoot(), paths.pidFile()));
        stateRepository.save(managedState);

        instance.setRuntimeStatus("ONLINE");
        instance.setLastHealthStatus("ONLINE");
        instance.setLastHealthCheckedAt(now);
        instance.setLastHealthMessage("Runtime started by Matrix26 and verified by HTTP.");
        clientRepository.save(instance);

        operation.setStatus(Matrix26RuntimeOperationStatus.COMPLETED);
        operation.setFinalState(detected.state().name());
        operation.setCompletedAt(now);
        operation.setDurationMs(durationMs(operation));
        operation.setMessage("Runtime started and verified successfully.");
        operationRepository.save(operation);
        appendOperationMarker(paths.standardLog(), "Runtime online and verified by Matrix26.");
        return new Matrix26RuntimeControlResult(
                operation,
                "Runtime started successfully with PID " + pid + "."
        );
    }

    private Matrix26RuntimeControlResult stopInternal(
            PlatformBusinessClient instance,
            Matrix26RuntimeInventoryItem runtime,
            Matrix26RuntimeOperation operation,
            boolean restarting
    ) {
        inventoryService.invalidateCache();
        Matrix26RuntimeInventoryItem current = inventoryService.runtime(runtime.target().key(), true);
        if (!current.portListening() && current.processId() == null) {
            RuntimePaths paths = resolvePaths(current);
            deletePidFile(paths.pidFile());
            Matrix26RuntimeManagedState managedState = state(instance, current);
            managedState.setCurrentState(Matrix26ManagedRuntimeState.STOPPED);
            managedState.setLastKnownPid(null);
            managedState.setLastStoppedAt(LocalDateTime.now());
            managedState.setLastOperationId(operation.getId());
            managedState.setMessage("Runtime was already stopped.");
            stateRepository.save(managedState);
            instance.setRuntimeStatus("STOPPED");
            clientRepository.save(instance);
            return completeStop(operation, current, "Runtime was already stopped.");
        }

        Long pid = resolveOwnedPid(current);
        if (pid == null) {
            throw new Matrix26RuntimeControlException(
                    "Matrix26 refused to stop the process because ownership could not be verified."
            );
        }

        ProcessHandle handle = ProcessHandle.of(pid)
                .filter(ProcessHandle::isAlive)
                .orElseThrow(() -> new Matrix26RuntimeControlException("The detected PID is no longer active."));
        if (!ownsProcess(handle, current)) {
            throw new Matrix26RuntimeControlException(
                    "Matrix26 refused to stop the process because its command line does not match the runtime profile."
            );
        }

        RuntimePaths paths = resolvePaths(current);
        Matrix26RuntimeManagedState managedState = state(instance, current);
        updateManagedState(
                managedState,
                restarting ? Matrix26ManagedRuntimeState.RESTARTING : Matrix26ManagedRuntimeState.STOPPING,
                pid,
                restarting ? "Stopping runtime before restart." : "Stopping runtime gracefully.",
                operation,
                paths
        );
        instance.setRuntimeStatus(restarting ? "RESTARTING" : "STOPPING");
        clientRepository.save(instance);
        operation.setPreviousPid(pid);
        operationRepository.save(operation);
        appendOperationMarker(paths.standardLog(), restarting
                ? "Stopping runtime before restart."
                : "Stopping runtime through Matrix26.");

        boolean destroyRequested = handle.destroy();
        if (!destroyRequested && handle.isAlive()) {
            throw new Matrix26RuntimeControlException("The operating system rejected the graceful stop request.");
        }

        int timeoutSeconds = Math.max(5, properties.getStopTimeoutSeconds());
        Instant deadline = Instant.now().plusSeconds(timeoutSeconds);
        int pollMs = Math.max(250, Math.min(properties.getPollIntervalMs(), 3_000));
        while (handle.isAlive() && Instant.now().isBefore(deadline)) {
            sleep(pollMs);
        }

        if (handle.isAlive()) {
            managedState.setCurrentState(Matrix26ManagedRuntimeState.FAILED);
            managedState.setMessage("Graceful stop timed out. Force stop was not executed.");
            managedState.setLastOperationId(operation.getId());
            stateRepository.save(managedState);
            instance.setRuntimeStatus("STOP_TIMEOUT");
            clientRepository.save(instance);
            operation.setStatus(Matrix26RuntimeOperationStatus.STOP_TIMEOUT);
            operation.setFinalState("STOP_TIMEOUT");
            operation.setCompletedAt(LocalDateTime.now());
            operation.setDurationMs(durationMs(operation));
            operation.setMessage("Graceful stop timed out. Force stop was not executed.");
            operationRepository.save(operation);
            throw new Matrix26RuntimeControlException(
                    "The runtime did not stop before the timeout. Matrix26 did not force-kill the process."
            );
        }

        waitForPortRelease(current.target().expectedPort(), Math.min(timeoutSeconds, 10));
        deletePidFile(paths.pidFile());
        managedState.setCurrentState(restarting
                ? Matrix26ManagedRuntimeState.RESTARTING
                : Matrix26ManagedRuntimeState.STOPPED);
        managedState.setLastKnownPid(null);
        managedState.setLastStoppedAt(LocalDateTime.now());
        managedState.setLastOperationId(operation.getId());
        managedState.setMessage(restarting ? "Runtime stopped. Starting replacement process." : "Runtime stopped gracefully.");
        stateRepository.save(managedState);
        instance.setRuntimeStatus(restarting ? "RESTARTING" : "STOPPED");
        clientRepository.save(instance);

        if (restarting) {
            return new Matrix26RuntimeControlResult(operation, "Runtime stopped before restart.");
        }
        return completeStop(operation, current, "Runtime stopped gracefully.");
    }

    private Matrix26RuntimeControlResult completeStop(
            Matrix26RuntimeOperation operation,
            Matrix26RuntimeInventoryItem runtime,
            String message
    ) {
        operation.setStatus(Matrix26RuntimeOperationStatus.COMPLETED);
        operation.setResultingPid(null);
        operation.setFinalState("STOPPED");
        operation.setCompletedAt(LocalDateTime.now());
        operation.setDurationMs(durationMs(operation));
        operation.setMessage(message);
        operationRepository.save(operation);
        inventoryService.invalidateCache();
        return new Matrix26RuntimeControlResult(operation, message + " Port " + runtime.target().expectedPort() + " is free.");
    }

    private Matrix26RuntimeControlResult restartInternal(
            PlatformBusinessClient instance,
            Matrix26RuntimeInventoryItem runtime,
            Matrix26RuntimeOperation operation
    ) {
        if (runtime.portListening() || runtime.processId() != null) {
            stopInternal(instance, runtime, operation, true);
        }
        inventoryService.invalidateCache();
        Matrix26RuntimeInventoryItem stopped = inventoryService.runtime(runtime.target().key(), true);
        if (stopped.portListening() || stopped.processId() != null) {
            throw new Matrix26RuntimeControlException("Restart was cancelled because the previous process is still active.");
        }
        Matrix26RuntimeControlResult started = startInternal(instance, stopped, operation, true);
        operation.setMessage("Runtime restarted and verified successfully.");
        operationRepository.save(operation);
        return new Matrix26RuntimeControlResult(
                operation,
                "Runtime restarted successfully with PID " + operation.getResultingPid() + "."
        );
    }

    private Matrix26RuntimeOperation newOperation(
            PlatformBusinessClient instance,
            Matrix26RuntimeInventoryItem runtime,
            Matrix26RuntimeOperationAction action,
            String actor
    ) {
        Matrix26RuntimeOperation operation = new Matrix26RuntimeOperation();
        operation.setInstance(instance);
        operation.setRuntimeKey(runtime.target().key());
        operation.setInstanceCode(runtime.target().code());
        operation.setAction(action);
        operation.setStatus(Matrix26RuntimeOperationStatus.REQUESTED);
        operation.setRequestedBy(actor == null || actor.isBlank() ? "matrix26-system" : actor.trim());
        operation.setRequestedAt(LocalDateTime.now());
        operation.setPreviousPid(runtime.processId());
        operation.setRuntimePort(runtime.target().expectedPort());
        operation.setInitialState(runtime.state().name());
        operation.setMessage("Runtime operation requested.");
        return operation;
    }

    private void failOperation(
            Matrix26RuntimeOperation operation,
            String message,
            Throwable error
    ) {
        if (operation.getStatus() != Matrix26RuntimeOperationStatus.STOP_TIMEOUT) {
            operation.setStatus(Matrix26RuntimeOperationStatus.FAILED);
        }
        operation.setCompletedAt(LocalDateTime.now());
        operation.setDurationMs(durationMs(operation));
        operation.setFinalState(operation.getFinalState() == null ? "FAILED" : operation.getFinalState());
        operation.setMessage(safe(message));
        operation.setErrorDetail(Matrix26OperationsSanitizer.limit(
                Matrix26OperationsSanitizer.sanitize(error == null ? message : error.toString()),
                4_000
        ));
        operationRepository.save(operation);
    }

    private Matrix26RuntimeManagedState state(
            PlatformBusinessClient instance,
            Matrix26RuntimeInventoryItem runtime
    ) {
        return stateRepository.findByInstance_Id(instance.getId()).orElseGet(() -> {
            Matrix26RuntimeManagedState state = new Matrix26RuntimeManagedState();
            state.setInstance(instance);
            state.setRuntimeKey(runtime.target().key());
            state.setInstanceCode(instance.getCode());
            state.setCurrentState(Matrix26ManagedRuntimeState.UNKNOWN);
            state.setMessage("Runtime control state initialized.");
            return stateRepository.save(state);
        });
    }

    private void updateManagedState(
            Matrix26RuntimeManagedState state,
            Matrix26ManagedRuntimeState currentState,
            Long pid,
            String message,
            Matrix26RuntimeOperation operation,
            RuntimePaths paths
    ) {
        state.setCurrentState(currentState);
        state.setLastKnownPid(pid);
        state.setLastOperationId(operation.getId());
        state.setMessage(message);
        state.setStandardLogPath(relative(paths.projectRoot(), paths.standardLog()));
        state.setErrorLogPath(relative(paths.projectRoot(), paths.errorLog()));
        state.setPidFilePath(relative(paths.projectRoot(), paths.pidFile()));
        stateRepository.save(state);
    }

    private String manageabilityReason(Matrix26RuntimeInventoryItem runtime) {
        Matrix26RuntimeTarget target = runtime.target();
        if (!properties.isRuntimeControlEnabled()) {
            return "Runtime control is disabled by configuration.";
        }
        if (target.controlCenter()) {
            return "Matrix26 cannot control its own process.";
        }
        if (target.instanceId() == null) {
            return "The runtime is not linked to a registered instance.";
        }
        if (!allowedCode(target.code())) {
            return target.protectedInstance()
                    ? "The instance is administratively protected and is not in the runtime control allowlist."
                    : "The instance is not included in the runtime control allowlist.";
        }
        if (!runtime.runtimeDirectoryPresent()) {
            return "The runtime directory does not exist.";
        }
        if (!runtime.configurationPresent() || !runtime.configurationConsistent()) {
            return "The runtime configuration is missing or inconsistent.";
        }
        if (!runtime.launcherPresent()) {
            return "The runtime launcher is missing.";
        }
        if (runtime.target().expectedPort() == null) {
            return "The runtime does not have a registered port.";
        }
        if (runtime.portListening() && !runtime.expectedProcess()) {
            return "The registered port is owned by another process.";
        }
        try {
            findApplicationJar(Path.of("").toAbsolutePath().normalize());
        } catch (Matrix26RuntimeControlException ex) {
            return ex.getMessage();
        }
        return "";
    }

    private void validateManageable(Matrix26RuntimeInventoryItem runtime) {
        String reason = manageabilityReason(runtime);
        if (!reason.isBlank()) {
            throw new Matrix26RuntimeControlException(reason);
        }
    }

    private void validateConfirmation(
            Matrix26RuntimeInventoryItem runtime,
            Matrix26RuntimeOperationAction action,
            String confirmation
    ) {
        if (action == Matrix26RuntimeOperationAction.START) {
            return;
        }
        String expected = action.name() + " " + runtime.target().code();
        if (confirmation == null || !expected.equals(confirmation.trim())) {
            throw new Matrix26RuntimeControlException("The confirmation text does not match: " + expected);
        }
    }

    private boolean allowedCode(String code) {
        if (code == null) {
            return false;
        }
        return properties.getAllowedInstanceCodes().stream()
                .filter(value -> value != null)
                .map(value -> value.trim().toLowerCase(Locale.ROOT))
                .anyMatch(code.trim().toLowerCase(Locale.ROOT)::equals);
    }

    private Long resolveOwnedPid(Matrix26RuntimeInventoryItem runtime) {
        List<Long> candidates = new ArrayList<>();
        if (runtime.processId() != null) {
            candidates.add(runtime.processId());
        }
        if (runtime.portOwnerPid() != null && !candidates.contains(runtime.portOwnerPid())) {
            candidates.add(runtime.portOwnerPid());
        }
        if (runtime.target().instanceId() != null) {
            stateRepository.findByInstance_Id(runtime.target().instanceId())
                    .map(Matrix26RuntimeManagedState::getLastKnownPid)
                    .filter(pid -> pid != null && !candidates.contains(pid))
                    .ifPresent(candidates::add);
        }
        RuntimePaths paths = resolvePaths(runtime);
        readPidFile(paths.pidFile())
                .filter(pid -> !candidates.contains(pid))
                .ifPresent(candidates::add);

        for (Long pid : candidates) {
            Optional<ProcessHandle> handle = ProcessHandle.of(pid).filter(ProcessHandle::isAlive);
            if (handle.isPresent() && ownsProcess(handle.get(), runtime)) {
                return pid;
            }
        }
        return null;
    }

    private boolean ownsProcess(ProcessHandle handle, Matrix26RuntimeInventoryItem runtime) {
        ProcessHandle.Info info = handle.info();
        String command = info.command().orElse("");
        String commandLine = info.commandLine().orElseGet(() -> buildCommandLine(info));
        String executableName = Path.of(command.isBlank() ? "java" : command).getFileName().toString().toLowerCase(Locale.ROOT);
        if (!JAVA_EXECUTABLE_NAMES.contains(executableName)) {
            return false;
        }
        String normalized = commandLine.replace('\\', '/').toLowerCase(Locale.ROOT);
        String profile = runtime.target().runtimeProfile() == null
                ? ""
                : runtime.target().runtimeProfile().toLowerCase(Locale.ROOT);
        return !profile.isBlank()
                && normalized.contains(profile)
                && normalized.contains("application.properties");
    }

    private String buildCommandLine(ProcessHandle.Info info) {
        String command = info.command().orElse("");
        String[] arguments = info.arguments().orElse(new String[0]);
        return arguments.length == 0 ? command : command + " " + String.join(" ", arguments);
    }

    private RuntimePaths resolvePaths(Matrix26RuntimeInventoryItem runtime) {
        Path projectRoot = Path.of("").toAbsolutePath().normalize();
        Path runtimeDirectory = resolveRelative(projectRoot, runtime.runtimeDirectory(), "runtime directory");
        Path configuration = resolveRelative(projectRoot, runtime.configurationPath(), "runtime configuration");
        Path dataRoot = projectRoot.resolve(properties.getDataDirectory()).normalize();
        if (!dataRoot.startsWith(projectRoot)) {
            throw new Matrix26RuntimeControlException("The configured runtime data directory is outside the project root.");
        }
        String code = Matrix26OperationsSanitizer.slug(runtime.target().code());
        String operationDirectory = Matrix26OperationsSanitizer.slug(properties.getOperationDirectory());
        if (operationDirectory.isBlank()) {
            operationDirectory = "operations";
        }
        Path operations = dataRoot.resolve(code).resolve(operationDirectory).normalize();
        if (!operations.startsWith(dataRoot)) {
            throw new Matrix26RuntimeControlException("The runtime operation path was blocked by the security policy.");
        }
        return new RuntimePaths(
                projectRoot,
                runtimeDirectory,
                configuration,
                operations,
                operations.resolve("application.log"),
                operations.resolve("application-error.log"),
                operations.resolve("runtime.pid")
        );
    }

    private Path resolveRelative(Path projectRoot, String relative, String label) {
        if (relative == null || relative.isBlank()) {
            throw new Matrix26RuntimeControlException("The " + label + " is not available.");
        }
        Path resolved = projectRoot.resolve(relative).normalize();
        if (!resolved.startsWith(projectRoot)) {
            throw new Matrix26RuntimeControlException("The " + label + " is outside the project root.");
        }
        return resolved;
    }

    private Path findApplicationJar(Path projectRoot) {
        Path target = projectRoot.resolve("target").normalize();
        if (!Files.isDirectory(target)) {
            throw new Matrix26RuntimeControlException("No packaged application was found. Run Maven package first.");
        }
        try (Stream<Path> files = Files.list(target)) {
            return files
                    .filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().endsWith(".jar"))
                    .filter(path -> !path.getFileName().toString().contains("sources"))
                    .filter(path -> !path.getFileName().toString().contains("javadoc"))
                    .filter(path -> !path.getFileName().toString().startsWith("original-"))
                    .max(Comparator.comparingLong(this::lastModifiedMillis))
                    .map(Path::toAbsolutePath)
                    .orElseThrow(() -> new Matrix26RuntimeControlException(
                            "No packaged application JAR was found. Run Maven package first."
                    ));
        } catch (IOException ex) {
            throw new Matrix26RuntimeControlException("The packaged application directory could not be inspected.", ex);
        }
    }

    private Path resolveJavaExecutable() {
        Path javaHome = Path.of(System.getProperty("java.home", "")).toAbsolutePath().normalize();
        String executable = isWindows() ? "java.exe" : "java";
        Path candidate = javaHome.resolve("bin").resolve(executable).normalize();
        if (!Files.isRegularFile(candidate)) {
            throw new Matrix26RuntimeControlException("The Java executable could not be resolved from java.home.");
        }
        return candidate;
    }

    private void ensureOperationFiles(RuntimePaths paths) {
        try {
            Files.createDirectories(paths.operationsDirectory());
            if (!Files.exists(paths.standardLog())) {
                Files.createFile(paths.standardLog());
            }
            if (!Files.exists(paths.errorLog())) {
                Files.createFile(paths.errorLog());
            }
        } catch (IOException ex) {
            throw new Matrix26RuntimeControlException("The runtime operation directory could not be created.", ex);
        }
    }

    private void writePidFile(Path pidFile, long pid, String code, Integer port) {
        String content = "pid=" + pid + System.lineSeparator()
                + "instance=" + safe(code) + System.lineSeparator()
                + "port=" + port + System.lineSeparator()
                + "startedAt=" + LocalDateTime.now() + System.lineSeparator();
        try {
            Files.writeString(
                    pidFile,
                    content,
                    StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.TRUNCATE_EXISTING,
                    StandardOpenOption.WRITE
            );
        } catch (IOException ex) {
            throw new Matrix26RuntimeControlException("The runtime PID file could not be written.", ex);
        }
    }

    private Optional<Long> readPidFile(Path pidFile) {
        if (!Files.isRegularFile(pidFile)) {
            return Optional.empty();
        }
        try {
            return Files.readAllLines(pidFile, StandardCharsets.UTF_8).stream()
                    .filter(line -> line.startsWith("pid="))
                    .map(line -> line.substring(4).trim())
                    .map(Long::valueOf)
                    .findFirst();
        } catch (IOException | NumberFormatException ex) {
            return Optional.empty();
        }
    }

    private void deletePidFile(Path pidFile) {
        try {
            Files.deleteIfExists(pidFile);
        } catch (IOException ex) {
            LOGGER.warn("Matrix26 could not delete stale PID file {}.", pidFile);
        }
    }

    private void appendOperationMarker(Path log, String message) {
        String line = System.lineSeparator()
                + "[MATRIX26] " + LocalDateTime.now() + " " + safe(message)
                + System.lineSeparator();
        try {
            Files.writeString(
                    log,
                    line,
                    StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.APPEND
            );
        } catch (IOException ex) {
            LOGGER.warn("Matrix26 could not append the runtime operation marker to {}.", log);
        }
    }

    private void waitForPortRelease(Integer port, int timeoutSeconds) {
        if (port == null) {
            return;
        }
        Instant deadline = Instant.now().plusSeconds(Math.max(1, timeoutSeconds));
        while (!isPortAvailable(port) && Instant.now().isBefore(deadline)) {
            sleep(250);
        }
        if (!isPortAvailable(port)) {
            throw new Matrix26RuntimeControlException("The process stopped, but the registered port is still occupied.");
        }
    }

    private boolean isPortAvailable(Integer port) {
        if (port == null || port < 1 || port > 65535) {
            return false;
        }
        try (ServerSocket socket = new ServerSocket(port)) {
            socket.setReuseAddress(false);
            return true;
        } catch (IOException ex) {
            return false;
        }
    }

    private long lastModifiedMillis(Path path) {
        try {
            return Files.getLastModifiedTime(path).toMillis();
        } catch (IOException ex) {
            return 0L;
        }
    }

    private void sleep(long milliseconds) {
        try {
            TimeUnit.MILLISECONDS.sleep(milliseconds);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new Matrix26RuntimeControlException("The runtime operation was interrupted.", ex);
        }
    }

    private long durationMs(Matrix26RuntimeOperation operation) {
        LocalDateTime started = operation.getStartedAt() == null
                ? operation.getRequestedAt()
                : operation.getStartedAt();
        return Math.max(0, Duration.between(started, LocalDateTime.now()).toMillis());
    }

    private String relative(Path projectRoot, Path path) {
        if (path == null) {
            return "";
        }
        try {
            return projectRoot.relativize(path.toAbsolutePath().normalize()).toString().replace('\\', '/');
        } catch (RuntimeException ex) {
            return path.toString().replace('\\', '/');
        }
    }

    private boolean isWindows() {
        return System.getProperty("os.name", "").toLowerCase(Locale.ROOT).contains("win");
    }

    private String safe(String value) {
        return Matrix26OperationsSanitizer.limit(
                Matrix26OperationsSanitizer.sanitize(value == null ? "" : value.trim()),
                500
        );
    }

    private void recordAudit(
            PlatformBusinessClient instance,
            Matrix26RuntimeOperation operation,
            String actor,
            String summary
    ) {
        Matrix26InstanceAuditLog audit = new Matrix26InstanceAuditLog();
        audit.setInstance(instance);
        audit.setAction("RUNTIME_" + operation.getAction().name());
        audit.setActorUsername(actor == null || actor.isBlank() ? "matrix26-system" : actor.trim());
        audit.setSummary(safe(summary));
        audit.setBeforeSnapshot("state=" + safe(operation.getInitialState())
                + ";pid=" + operation.getPreviousPid()
                + ";port=" + operation.getRuntimePort());
        audit.setAfterSnapshot("status=" + operation.getStatus()
                + ";state=" + safe(operation.getFinalState())
                + ";pid=" + operation.getResultingPid());
        auditRepository.save(audit);
    }

    private record RuntimePaths(
            Path projectRoot,
            Path runtimeDirectory,
            Path configuration,
            Path operationsDirectory,
            Path standardLog,
            Path errorLog,
            Path pidFile
    ) {
    }
}
