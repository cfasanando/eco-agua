package com.ecoamazonas.eco_agua.platform.control.backups;

import com.ecoamazonas.eco_agua.platform.PlatformBusinessClient;
import com.ecoamazonas.eco_agua.platform.PlatformBusinessClientRepository;
import com.ecoamazonas.eco_agua.platform.control.Matrix26InstanceAuditLog;
import com.ecoamazonas.eco_agua.platform.control.Matrix26InstanceAuditLogRepository;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.CipherOutputStream;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

@Service
@ConditionalOnProperty(name = "matrix26.control-center.enabled", havingValue = "true")
public class Matrix26BackupSecurityService {

    private static final byte[] MAGIC = "M26BKP01".getBytes(StandardCharsets.US_ASCII);
    private static final int FORMAT_VERSION = 1;
    private static final int NONCE_BYTES = 12;
    private static final int GCM_TAG_BITS = 128;
    private static final String ALGORITHM = "AES-256-GCM";
    private static final Set<String> PUBLIC_FILES = Set.of(
            "package.m26backup", "public-manifest.json"
    );
    private static final Pattern CHECKSUM_LINE = Pattern.compile("^([0-9a-fA-F]{64})\\s{2}(.+)$");

    private final Matrix26BackupRepository backupRepository;
    private final Matrix26BackupSecurityRepository securityRepository;
    private final Matrix26BackupProperties properties;
    private final PlatformBusinessClientRepository clientRepository;
    private final Matrix26InstanceAuditLogRepository auditLogRepository;
    private final SecureRandom secureRandom = new SecureRandom();

    public Matrix26BackupSecurityService(
            Matrix26BackupRepository backupRepository,
            Matrix26BackupSecurityRepository securityRepository,
            Matrix26BackupProperties properties,
            PlatformBusinessClientRepository clientRepository,
            Matrix26InstanceAuditLogRepository auditLogRepository
    ) {
        this.backupRepository = backupRepository;
        this.securityRepository = securityRepository;
        this.properties = properties;
        this.clientRepository = clientRepository;
        this.auditLogRepository = auditLogRepository;
    }

    public Matrix26BackupKeyStatus keyStatus() {
        if (!properties.isEncryptionEnabled()) {
            return new Matrix26BackupKeyStatus(false, "", "Backup encryption is disabled in Matrix26 configuration.");
        }
        try {
            KeyMaterial material = keyMaterial();
            return new Matrix26BackupKeyStatus(true, material.keyId(),
                    "AES-256-GCM master key is available through " + properties.getMasterKeyEnvironment() + ".");
        } catch (Matrix26BackupException ex) {
            return new Matrix26BackupKeyStatus(false, "", ex.getMessage());
        }
    }

    public Matrix26BackupEncryption metadata(long jobId) {
        return securityRepository.findEncryption(jobId).orElse(null);
    }

    public Matrix26BackupJob encryptBackup(
            long jobId,
            Matrix26BackupRetentionClass retentionClass,
            String actor
    ) {
        if (!properties.isEncryptionEnabled()) {
            throw new Matrix26BackupException("Backup encryption is disabled in Matrix26 configuration.");
        }
        Matrix26BackupJob job = backupRepository.findById(jobId)
                .orElseThrow(() -> new Matrix26BackupException("The requested backup does not exist."));
        if (!job.isCompleted()) {
            throw new Matrix26BackupException("Only completed backups can be encrypted.");
        }
        if (!"MANUAL_FULL".equalsIgnoreCase(job.backupType())) {
            throw new Matrix26BackupException("Phase 3E.3 encrypts full instance backups only.");
        }
        if (securityRepository.findEncryption(jobId).map(Matrix26BackupEncryption::encrypted).orElse(false)) {
            throw new Matrix26BackupException("This backup is already encrypted.");
        }

        PlatformBusinessClient instance = clientRepository.findById(job.instanceId())
                .orElseThrow(() -> new Matrix26BackupException("The backup instance no longer exists."));
        validateAllowedInstance(instance);
        KeyMaterial key = keyMaterial();
        Path root = backupRoot();
        Path directory = Path.of(job.backupDirectory()).toAbsolutePath().normalize();
        ensureInside(root, directory);
        if (!Files.isDirectory(directory)) {
            throw new Matrix26BackupException("The backup directory no longer exists.");
        }

        Path tempBase = root.resolve(".matrix26-temp").normalize();
        ensureInside(root, tempBase);
        Path work = null;
        Path packagePart = directory.resolve("package.m26backup.part");
        Path packageFile = directory.resolve("package.m26backup");
        boolean packageVerified = false;
        try {
            Files.createDirectories(tempBase);
            work = Files.createTempDirectory(tempBase, "encrypt-");
            Path payloadZip = work.resolve("payload.zip");
            List<Path> payload = payloadFiles(directory);
            if (payload.isEmpty()) {
                throw new Matrix26BackupException("No recovery artifacts were found to encrypt.");
            }
            createPayloadZip(directory, payload, payloadZip);

            byte[] nonce = new byte[NONCE_BYTES];
            secureRandom.nextBytes(nonce);
            encrypt(payloadZip, packagePart, key, nonce);
            moveAtomically(packagePart, packageFile);

            PackageInspection inspection = inspectEncryptedPackage(packageFile, key);
            if (!inspection.valid()) {
                throw new Matrix26BackupException("The encrypted package did not pass verification: " + inspection.message());
            }
            packageVerified = true;

            String packageSha = sha256(packageFile);
            Matrix26BackupPolicy policy = securityRepository.findPolicy(instance.getId(), instance.getCode());
            LocalDateTime expiresAt = expiry(retentionClass, policy);
            boolean protectedFlag = retentionClass == Matrix26BackupRetentionClass.FINAL;
            String protectionReason = protectedFlag ? "Final archive" : "";

            Path stagedManifest = work.resolve("public-manifest.json");
            writePublicManifest(stagedManifest, job, instance, key, retentionClass, expiresAt,
                    Files.size(packageFile), packageSha);
            String manifestSha = sha256(stagedManifest);

            Path stagedChecksums = work.resolve("checksums.sha256");
            Files.writeString(stagedChecksums,
                    packageSha + "  package.m26backup" + System.lineSeparator()
                            + manifestSha + "  public-manifest.json" + System.lineSeparator(),
                    StandardCharsets.UTF_8);

            Path stagedReport = work.resolve("backup-report.txt");
            writePublicReport(stagedReport, job, instance, key, retentionClass, packageFile, packageSha, inspection);

            for (Path file : payload) {
                Files.deleteIfExists(file);
            }

            Path publicManifest = directory.resolve("public-manifest.json");
            Path checksums = directory.resolve("checksums.sha256");
            Path report = directory.resolve("backup-report.txt");
            moveAtomically(stagedManifest, publicManifest);
            moveAtomically(stagedChecksums, checksums);
            moveAtomically(stagedReport, report);

            LocalDateTime now = LocalDateTime.now();
            securityRepository.upsertEncryption(
                    job.id(),
                    ALGORITHM,
                    FORMAT_VERSION,
                    key.keyId(),
                    relative(root, packageFile),
                    Files.size(packageFile),
                    packageSha,
                    Matrix26BackupVerificationState.VERIFIED,
                    now,
                    retentionClass,
                    expiresAt,
                    protectedFlag,
                    protectionReason,
                    now
            );
            securityRepository.updateJobPackage(
                    job.id(),
                    Files.size(packageFile) + Files.size(publicManifest) + Files.size(checksums) + Files.size(report),
                    packageSha,
                    relative(root, publicManifest),
                    relative(root, report),
                    "Encrypted package passed public checksum, AES-GCM authentication, ZIP structure, and internal checksum verification."
            );
            securityRepository.replaceArtifacts(job.id(), List.of(
                    artifact(job.id(), "ENCRYPTED_PACKAGE", packageFile, root, packageSha),
                    artifact(job.id(), "PUBLIC_MANIFEST", publicManifest, root, manifestSha),
                    artifact(job.id(), "CHECKSUMS", checksums, root, sha256(checksums)),
                    artifact(job.id(), "REPORT", report, root, sha256(report))
            ));
            backupRepository.insertVerification(job.id(), "ENCRYPTED_PACKAGE", "AES-GCM package authentication",
                    Matrix26BackupVerificationStatus.PASSED,
                    "The encrypted package was decrypted with key identifier " + key.keyId() + ".");
            backupRepository.insertVerification(job.id(), "INTERNAL_CHECKSUMS", "Internal artifact checksums",
                    Matrix26BackupVerificationStatus.PASSED, inspection.message());
            writeAudit(instance, actor, "BACKUP_ENCRYPTED",
                    "Encrypted backup " + job.publicId() + " using key " + key.keyId() + ".");
            return backupRepository.findById(job.id()).orElseThrow();
        } catch (Matrix26BackupException ex) {
            deleteQuietly(packagePart);
            if (!packageVerified) {
                deleteQuietly(packageFile);
            }
            throw ex;
        } catch (Exception ex) {
            deleteQuietly(packagePart);
            if (!packageVerified) {
                deleteQuietly(packageFile);
            }
            throw new Matrix26BackupException("The backup could not be encrypted: " + safeMessage(ex), ex);
        } finally {
            deleteTreeQuietly(work);
        }
    }

    public Matrix26BackupEncryption verifyEncryptedBackup(long jobId, String actor) {
        Matrix26BackupJob job = backupRepository.findById(jobId)
                .orElseThrow(() -> new Matrix26BackupException("The requested backup does not exist."));
        Matrix26BackupEncryption metadata = securityRepository.findEncryption(jobId)
                .orElseThrow(() -> new Matrix26BackupException("This backup does not have encrypted package metadata."));
        PlatformBusinessClient instance = clientRepository.findById(job.instanceId())
                .orElseThrow(() -> new Matrix26BackupException("The backup instance no longer exists."));
        validateAllowedInstance(instance);
        securityRepository.updateVerification(jobId, Matrix26BackupVerificationState.VERIFYING, null);
        try {
            KeyMaterial key = keyMaterial();
            if (!key.keyId().equals(metadata.keyId())) {
                securityRepository.updateVerification(jobId, Matrix26BackupVerificationState.KEY_UNAVAILABLE, null);
                throw new Matrix26BackupException("The available master key does not match backup key identifier " + metadata.keyId() + ".");
            }
            Path root = backupRoot();
            Path packageFile = root.resolve(metadata.packagePath()).normalize();
            ensureInside(root, packageFile);
            if (!Files.isRegularFile(packageFile)) {
                securityRepository.updateVerification(jobId, Matrix26BackupVerificationState.CORRUPTED, null);
                throw new Matrix26BackupException("The encrypted package file is missing.");
            }
            String currentSha = sha256(packageFile);
            if (!currentSha.equalsIgnoreCase(metadata.packageSha256())) {
                securityRepository.updateVerification(jobId, Matrix26BackupVerificationState.CORRUPTED, null);
                throw new Matrix26BackupException("The encrypted package SHA-256 no longer matches its registered value.");
            }
            PackageInspection inspection = inspectEncryptedPackage(packageFile, key);
            if (!inspection.valid()) {
                securityRepository.updateVerification(jobId, Matrix26BackupVerificationState.VERIFICATION_FAILED, null);
                throw new Matrix26BackupException(inspection.message());
            }
            LocalDateTime verifiedAt = LocalDateTime.now();
            securityRepository.updateVerification(jobId, Matrix26BackupVerificationState.VERIFIED, verifiedAt);
            backupRepository.insertVerification(jobId, "REVERIFY_ENCRYPTED_PACKAGE", "Encrypted package reverification",
                    Matrix26BackupVerificationStatus.PASSED, inspection.message());
            writeAudit(instance, actor, "BACKUP_REVERIFIED", "Encrypted backup reverified: " + job.publicId());
            return securityRepository.findEncryption(jobId).orElseThrow();
        } catch (Matrix26BackupException ex) {
            throw ex;
        } catch (Exception ex) {
            securityRepository.updateVerification(jobId, Matrix26BackupVerificationState.VERIFICATION_FAILED, null);
            throw new Matrix26BackupException("Encrypted backup verification failed: " + safeMessage(ex), ex);
        }
    }

    public Matrix26BackupPolicy policy(long instanceId) {
        PlatformBusinessClient instance = clientRepository.findById(instanceId)
                .orElseThrow(() -> new Matrix26BackupException("The requested instance does not exist."));
        validateAllowedInstance(instance);
        return securityRepository.findPolicy(instanceId, instance.getCode());
    }

    public Matrix26BackupPolicy savePolicy(
            long instanceId,
            int dailyKeep,
            int weeklyKeep,
            int monthlyKeep,
            boolean finalKeepIndefinitely,
            boolean enabled,
            String actor
    ) {
        PlatformBusinessClient instance = clientRepository.findById(instanceId)
                .orElseThrow(() -> new Matrix26BackupException("The requested instance does not exist."));
        validateAllowedInstance(instance);
        Matrix26BackupPolicy policy = new Matrix26BackupPolicy(
                null,
                instanceId,
                instance.getCode(),
                bounded(dailyKeep, 1, 365),
                bounded(weeklyKeep, 1, 104),
                bounded(monthlyKeep, 1, 120),
                finalKeepIndefinitely,
                enabled,
                safeActor(actor),
                LocalDateTime.now()
        );
        securityRepository.upsertPolicy(policy);
        writeAudit(instance, actor, "BACKUP_POLICY_UPDATED",
                "Backup retention policy updated for " + instance.getCode() + ".");
        return securityRepository.findPolicy(instanceId, instance.getCode());
    }

    public Matrix26RetentionPreview retentionPreview(long instanceId) {
        if (!properties.isRetentionEnabled()) {
            throw new Matrix26BackupException("Backup retention is disabled in Matrix26 configuration.");
        }
        PlatformBusinessClient instance = clientRepository.findById(instanceId)
                .orElseThrow(() -> new Matrix26BackupException("The requested instance does not exist."));
        validateAllowedInstance(instance);
        Matrix26BackupPolicy policy = securityRepository.findPolicy(instanceId, instance.getCode());
        List<Matrix26BackupSecurityRepository.RetentionRow> rows = securityRepository.findRetentionRows(instanceId);
        Long newestVerified = rows.stream()
                .filter(row -> row.verificationStatus() == Matrix26BackupVerificationState.VERIFIED)
                .map(Matrix26BackupSecurityRepository.RetentionRow::jobId)
                .findFirst()
                .orElse(null);
        Map<Matrix26BackupRetentionClass, Integer> positions = new EnumMap<>(Matrix26BackupRetentionClass.class);
        List<Matrix26RetentionItem> items = new ArrayList<>();
        long reclaimable = 0L;
        for (Matrix26BackupSecurityRepository.RetentionRow row : rows) {
            int position = positions.merge(row.retentionClass(), 1, Integer::sum);
            int keep = keepCount(policy, row.retentionClass());
            boolean beyondPolicy = row.retentionClass() != Matrix26BackupRetentionClass.FINAL && position > keep;
            boolean latest = newestVerified != null && newestVerified.equals(row.jobId());
            boolean verified = row.verificationStatus() == Matrix26BackupVerificationState.VERIFIED;
            boolean deletable = policy.enabled()
                    && beyondPolicy
                    && verified
                    && !row.protectedFlag()
                    && !latest;
            String reason;
            if (!policy.enabled()) {
                reason = "Retention cleanup is disabled.";
            } else if (row.retentionClass() == Matrix26BackupRetentionClass.FINAL) {
                reason = "Final archives are protected.";
            } else if (row.protectedFlag()) {
                reason = firstNonBlank(row.protectionReason(), "Backup is protected.");
            } else if (latest) {
                reason = "Newest verified backup is always protected.";
            } else if (!verified) {
                reason = "Backup requires investigation before deletion.";
            } else if (!beyondPolicy) {
                reason = "Kept by the current " + row.retentionClass().getLabel().toLowerCase(Locale.ROOT) + " policy.";
            } else {
                reason = "Eligible for manual retention cleanup.";
            }
            if (deletable) {
                reclaimable += row.storedBytes();
            }
            items.add(new Matrix26RetentionItem(
                    row.jobId(), row.publicId(), row.requestedAt(), row.retentionClass(),
                    row.verificationStatus(), row.protectedFlag(), row.storedBytes(),
                    row.backupDirectory(), deletable, reason
            ));
        }
        return new Matrix26RetentionPreview(instanceId, instance.getCode(), instance.getBusinessName(), policy,
                List.copyOf(items), reclaimable);
    }

    public Matrix26RetentionPreview executeRetention(long instanceId, String confirmation, String actor) {
        Matrix26RetentionPreview preview = retentionPreview(instanceId);
        String expected = "CLEAN BACKUPS " + preview.instanceCode();
        if (!expected.equals(confirmation == null ? "" : confirmation.trim())) {
            throw new Matrix26BackupException("Type exactly: " + expected);
        }
        Path root = backupRoot();
        long deletedBytes = 0L;
        for (Matrix26RetentionItem item : preview.items()) {
            if (!item.deletable()) {
                continue;
            }
            Path directory = Path.of(item.backupDirectory()).toAbsolutePath().normalize();
            ensureInside(root, directory);
            securityRepository.insertRetentionEvent(
                    instanceId, preview.instanceCode(), item.jobId(), item.publicId(),
                    "DELETE_REQUESTED", safeActor(actor), item.reason(), item.sizeBytes()
            );
            deleteTree(directory);
            securityRepository.deleteJob(item.jobId());
            securityRepository.insertRetentionEvent(
                    instanceId, preview.instanceCode(), item.jobId(), item.publicId(),
                    "DELETED", safeActor(actor), item.reason(), item.sizeBytes()
            );
            deletedBytes += item.sizeBytes();
        }
        PlatformBusinessClient instance = clientRepository.findById(instanceId).orElseThrow();
        writeAudit(instance, actor, "BACKUP_RETENTION_EXECUTED",
                "Manual retention cleanup reclaimed " + Matrix26BackupService.formatBytes(deletedBytes) + ".");
        return retentionPreview(instanceId);
    }

    private PackageInspection inspectEncryptedPackage(Path packageFile, KeyMaterial key) {
        Path root = backupRoot();
        Path tempBase = root.resolve(".matrix26-temp").normalize();
        Path work = null;
        try {
            Files.createDirectories(tempBase);
            work = Files.createTempDirectory(tempBase, "verify-");
            Path decryptedZip = work.resolve("payload.zip");
            decrypt(packageFile, decryptedZip, key);
            Path extracted = work.resolve("extracted");
            Files.createDirectories(extracted);
            int entries = extractZipSafely(decryptedZip, extracted);
            Path internalChecksums = extracted.resolve("checksums.sha256");
            if (!Files.isRegularFile(internalChecksums)) {
                return new PackageInspection(false, entries, "Internal checksums.sha256 is missing.");
            }
            List<String> failures = verifyInternalChecksums(extracted, internalChecksums);
            if (!failures.isEmpty()) {
                return new PackageInspection(false, entries, "Internal checksum failures: " + String.join(", ", failures));
            }
            if (!Files.isRegularFile(extracted.resolve("database.sql.gz"))
                    || !Files.isRegularFile(extracted.resolve("manifest.json"))
                    || !Files.isRegularFile(extracted.resolve("instance-files.zip"))) {
                return new PackageInspection(false, entries,
                        "Required database, manifest, or instance archive artifact is missing.");
            }
            return new PackageInspection(true, entries,
                    entries + " encrypted entries and all internal SHA-256 checks passed.");
        } catch (GeneralSecurityException ex) {
            return new PackageInspection(false, 0, "AES-GCM authentication failed or the key is incorrect.");
        } catch (Exception ex) {
            return new PackageInspection(false, 0, safeMessage(ex));
        } finally {
            deleteTreeQuietly(work);
        }
    }

    private void createPayloadZip(Path directory, List<Path> payload, Path target) throws IOException {
        try (ZipOutputStream zip = new ZipOutputStream(new BufferedOutputStream(Files.newOutputStream(target)))) {
            for (Path file : payload) {
                String entryName = directory.relativize(file).toString().replace('\\', '/');
                if (!safeEntry(entryName)) {
                    throw new Matrix26BackupException("Unsafe backup payload path: " + entryName);
                }
                ZipEntry entry = new ZipEntry(entryName);
                entry.setTime(Files.getLastModifiedTime(file).toMillis());
                zip.putNextEntry(entry);
                try (InputStream input = new BufferedInputStream(Files.newInputStream(file))) {
                    input.transferTo(zip);
                }
                zip.closeEntry();
            }
        }
    }

    private void encrypt(Path source, Path target, KeyMaterial key, byte[] nonce)
            throws IOException, GeneralSecurityException {
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(key.key(), "AES"), new GCMParameterSpec(GCM_TAG_BITS, nonce));
        byte[] aad = aad(key.keyId());
        cipher.updateAAD(aad);
        try (DataOutputStream output = new DataOutputStream(new BufferedOutputStream(Files.newOutputStream(target)))) {
            output.write(MAGIC);
            output.writeInt(FORMAT_VERSION);
            byte[] keyId = key.keyId().getBytes(StandardCharsets.US_ASCII);
            output.writeInt(keyId.length);
            output.write(keyId);
            output.writeInt(nonce.length);
            output.write(nonce);
            try (CipherOutputStream encrypted = new CipherOutputStream(output, cipher);
                 InputStream input = new BufferedInputStream(Files.newInputStream(source))) {
                input.transferTo(encrypted);
            }
        }
    }

    private void decrypt(Path source, Path target, KeyMaterial key)
            throws IOException, GeneralSecurityException {
        try (DataInputStream input = new DataInputStream(new BufferedInputStream(Files.newInputStream(source)))) {
            byte[] magic = input.readNBytes(MAGIC.length);
            if (!MessageDigest.isEqual(MAGIC, magic)) {
                throw new Matrix26BackupException("The file is not a Matrix26 encrypted backup package.");
            }
            int version = input.readInt();
            if (version != FORMAT_VERSION) {
                throw new Matrix26BackupException("Unsupported encrypted backup format version: " + version);
            }
            int keyIdLength = input.readInt();
            if (keyIdLength < 4 || keyIdLength > 128) {
                throw new Matrix26BackupException("Invalid encrypted package key identifier.");
            }
            String packageKeyId = new String(input.readNBytes(keyIdLength), StandardCharsets.US_ASCII);
            if (!packageKeyId.equals(key.keyId())) {
                throw new Matrix26BackupException("The available key does not match package key identifier " + packageKeyId + ".");
            }
            int nonceLength = input.readInt();
            if (nonceLength != NONCE_BYTES) {
                throw new Matrix26BackupException("Invalid AES-GCM nonce length.");
            }
            byte[] nonce = input.readNBytes(nonceLength);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(key.key(), "AES"), new GCMParameterSpec(GCM_TAG_BITS, nonce));
            cipher.updateAAD(aad(packageKeyId));
            try (CipherInputStream decrypted = new CipherInputStream(input, cipher);
                 OutputStream output = new BufferedOutputStream(Files.newOutputStream(target))) {
                decrypted.transferTo(output);
            }
        }
    }

    private byte[] aad(String keyId) {
        return (new String(MAGIC, StandardCharsets.US_ASCII) + ":" + FORMAT_VERSION + ":" + keyId)
                .getBytes(StandardCharsets.US_ASCII);
    }

    private int extractZipSafely(Path archive, Path destination) throws IOException {
        int entries = 0;
        try (ZipFile zip = new ZipFile(archive.toFile())) {
            var iterator = zip.entries();
            while (iterator.hasMoreElements()) {
                ZipEntry entry = iterator.nextElement();
                if (entry.isDirectory()) {
                    continue;
                }
                if (!safeEntry(entry.getName())) {
                    throw new Matrix26BackupException("Unsafe ZIP entry detected: " + entry.getName());
                }
                Path target = destination.resolve(entry.getName()).normalize();
                ensureInside(destination, target);
                Files.createDirectories(target.getParent());
                try (InputStream input = zip.getInputStream(entry)) {
                    Files.copy(input, target, StandardCopyOption.REPLACE_EXISTING);
                }
                entries++;
            }
        }
        return entries;
    }

    private List<String> verifyInternalChecksums(Path extracted, Path checksums) throws IOException {
        List<String> failures = new ArrayList<>();
        for (String line : Files.readAllLines(checksums, StandardCharsets.UTF_8)) {
            var matcher = CHECKSUM_LINE.matcher(line.trim());
            if (!matcher.matches()) {
                continue;
            }
            Path artifact = extracted.resolve(matcher.group(2)).normalize();
            ensureInside(extracted, artifact);
            if (!Files.isRegularFile(artifact) || !matcher.group(1).equalsIgnoreCase(sha256(artifact))) {
                failures.add(matcher.group(2));
            }
        }
        return failures;
    }

    private List<Path> payloadFiles(Path directory) throws IOException {
        try (var stream = Files.list(directory)) {
            return stream
                    .filter(Files::isRegularFile)
                    .filter(path -> !PUBLIC_FILES.contains(path.getFileName().toString()))
                    .filter(path -> !path.getFileName().toString().endsWith(".part"))
                    .sorted()
                    .toList();
        }
    }

    private void writePublicManifest(
            Path target,
            Matrix26BackupJob job,
            PlatformBusinessClient instance,
            KeyMaterial key,
            Matrix26BackupRetentionClass retentionClass,
            LocalDateTime expiresAt,
            long packageBytes,
            String packageSha
    ) throws IOException {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("formatVersion", FORMAT_VERSION);
        values.put("backupId", job.publicId());
        values.put("instanceCode", instance.getCode());
        values.put("backupType", "MANUAL_FULL_ENCRYPTED");
        values.put("createdAt", LocalDateTime.now().toString());
        values.put("algorithm", ALGORITHM);
        values.put("keyId", key.keyId());
        values.put("package", "package.m26backup");
        values.put("packageSizeBytes", packageBytes);
        values.put("packageSha256", packageSha);
        values.put("retentionClass", retentionClass.name());
        values.put("expiresAt", expiresAt == null ? null : expiresAt.toString());
        Files.writeString(target, toJson(values), StandardCharsets.UTF_8);
    }

    private void writePublicReport(
            Path target,
            Matrix26BackupJob job,
            PlatformBusinessClient instance,
            KeyMaterial key,
            Matrix26BackupRetentionClass retentionClass,
            Path packageFile,
            String packageSha,
            PackageInspection inspection
    ) throws IOException {
        String content = "Matrix26 encrypted backup report" + System.lineSeparator()
                + "Backup ID: " + job.publicId() + System.lineSeparator()
                + "Instance: " + instance.getBusinessName() + " (" + instance.getCode() + ")" + System.lineSeparator()
                + "Encrypted at: " + LocalDateTime.now() + System.lineSeparator()
                + "Algorithm: " + ALGORITHM + System.lineSeparator()
                + "Key identifier: " + key.keyId() + System.lineSeparator()
                + "Retention class: " + retentionClass.name() + System.lineSeparator()
                + "Package size: " + Matrix26BackupService.formatBytes(Files.size(packageFile)) + System.lineSeparator()
                + "Package SHA-256: " + packageSha + System.lineSeparator()
                + "Verification: " + inspection.message() + System.lineSeparator()
                + "Master key stored in backup: NO" + System.lineSeparator();
        Files.writeString(target, content, StandardCharsets.UTF_8);
    }

    private Matrix26BackupArtifact artifact(long jobId, String type, Path file, Path root, String sha) throws IOException {
        return new Matrix26BackupArtifact(null, jobId, type, file.getFileName().toString(),
                relative(root, file), Files.size(file), sha, "VERIFIED", LocalDateTime.now());
    }

    private KeyMaterial keyMaterial() {
        String variable = firstNonBlank(properties.getMasterKeyEnvironment(), "MATRIX26_BACKUP_MASTER_KEY");
        String raw = System.getenv(variable);
        if (raw == null || raw.isBlank()) {
            raw = System.getProperty(variable);
        }
        if (raw == null || raw.isBlank()) {
            throw new Matrix26BackupException("Backup encryption key is unavailable. Set environment variable " + variable + ".");
        }
        byte[] material;
        try {
            material = Base64.getDecoder().decode(raw.trim());
        } catch (IllegalArgumentException ex) {
            material = raw.getBytes(StandardCharsets.UTF_8);
        }
        if (material.length < 32) {
            throw new Matrix26BackupException("Backup master key must provide at least 32 bytes of key material.");
        }
        byte[] key = digest(material);
        String keyId = HexFormat.of().formatHex(digest(key)).substring(0, 16).toUpperCase(Locale.ROOT);
        return new KeyMaterial(key, keyId);
    }

    private LocalDateTime expiry(Matrix26BackupRetentionClass retentionClass, Matrix26BackupPolicy policy) {
        LocalDateTime now = LocalDateTime.now();
        return switch (retentionClass) {
            case DAILY -> now.plusDays(Math.max(1, policy.dailyKeep()));
            case WEEKLY -> now.plusWeeks(Math.max(1, policy.weeklyKeep()));
            case MONTHLY -> now.plusMonths(Math.max(1, policy.monthlyKeep()));
            case FINAL -> null;
        };
    }

    private int keepCount(Matrix26BackupPolicy policy, Matrix26BackupRetentionClass retentionClass) {
        return switch (retentionClass) {
            case DAILY -> policy.dailyKeep();
            case WEEKLY -> policy.weeklyKeep();
            case MONTHLY -> policy.monthlyKeep();
            case FINAL -> Integer.MAX_VALUE;
        };
    }

    private void validateAllowedInstance(PlatformBusinessClient instance) {
        boolean allowed = properties.getAllowedInstanceCodes().stream()
                .anyMatch(code -> code.equalsIgnoreCase(instance.getCode()));
        if (!allowed) {
            throw new Matrix26BackupException("Encryption and retention are not enabled for this instance.");
        }
    }

    private Path backupRoot() {
        String configured = firstNonBlank(System.getenv("MATRIX26_BACKUP_ROOT"), properties.getRootDirectory());
        Path root = configured.isBlank()
                ? Path.of(System.getProperty("user.home"), "Matrix26", "backups")
                : Path.of(configured);
        return root.toAbsolutePath().normalize();
    }

    private void deleteTree(Path root) {
        if (root == null || !Files.exists(root)) {
            return;
        }
        try (var stream = Files.walk(root)) {
            for (Path path : stream.sorted(Comparator.reverseOrder()).toList()) {
                Files.deleteIfExists(path);
            }
        } catch (IOException ex) {
            throw new Matrix26BackupException("Backup directory could not be removed: " + safeMessage(ex), ex);
        }
    }

    private void deleteTreeQuietly(Path root) {
        try {
            deleteTree(root);
        } catch (RuntimeException ignored) {
            // Temporary cleanup must not replace the primary result.
        }
    }

    private void deleteQuietly(Path file) {
        if (file == null) {
            return;
        }
        try {
            Files.deleteIfExists(file);
        } catch (IOException ignored) {
            // Partial artifact cleanup is best effort.
        }
    }

    private void moveAtomically(Path source, Path target) throws IOException {
        try {
            Files.move(source, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException ex) {
            Files.move(source, target, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private boolean safeEntry(String name) {
        if (name == null || name.isBlank() || name.startsWith("/") || name.startsWith("\\")) {
            return false;
        }
        String normalized = name.replace('\\', '/');
        return !normalized.contains("../") && !normalized.matches("^[A-Za-z]:.*");
    }

    private String sha256(Path file) throws IOException {
        MessageDigest digest = messageDigest();
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
    }

    private byte[] digest(byte[] value) {
        return messageDigest().digest(value);
    }

    private MessageDigest messageDigest() {
        try {
            return MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 is not available.", ex);
        }
    }

    private void ensureInside(Path parent, Path child) {
        if (!child.toAbsolutePath().normalize().startsWith(parent.toAbsolutePath().normalize())) {
            throw new Matrix26BackupException("A backup path escaped the configured storage boundary.");
        }
    }

    private String relative(Path root, Path file) {
        return root.toAbsolutePath().normalize().relativize(file.toAbsolutePath().normalize())
                .toString().replace('\\', '/');
    }

    private String firstNonBlank(String first, String second) {
        if (first != null && !first.isBlank()) {
            return first.trim();
        }
        return second == null ? "" : second.trim();
    }

    private int bounded(int value, int minimum, int maximum) {
        if (value < minimum || value > maximum) {
            throw new Matrix26BackupException("Retention values must be between " + minimum + " and " + maximum + ".");
        }
        return value;
    }

    private String safeActor(String actor) {
        if (actor == null || actor.isBlank()) {
            return "matrix26-system";
        }
        return actor.length() <= 120 ? actor.trim() : actor.trim().substring(0, 120);
    }

    private String safeMessage(Exception ex) {
        String value = ex.getMessage() == null ? ex.getClass().getSimpleName() : ex.getMessage();
        return value.replaceAll("(?i)(password|secret|token|key)(\\s*[:=]\\s*)([^\\s,;]+)", "$1$2***REDACTED***")
                .replaceAll("[\\r\\n]+", " ")
                .trim();
    }

    private String toJson(Map<String, Object> values) {
        StringBuilder json = new StringBuilder("{\n");
        int index = 0;
        for (Map.Entry<String, Object> entry : values.entrySet()) {
            if (index++ > 0) {
                json.append(",\n");
            }
            json.append("  \"").append(json(entry.getKey())).append("\": ");
            Object value = entry.getValue();
            if (value == null) {
                json.append("null");
            } else if (value instanceof Number || value instanceof Boolean) {
                json.append(value);
            } else {
                json.append("\"").append(json(value.toString())).append("\"");
            }
        }
        return json.append("\n}\n").toString();
    }

    private String json(String value) {
        return value == null ? "" : value.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    @Transactional
    protected void writeAudit(PlatformBusinessClient instance, String actor, String action, String summary) {
        Matrix26InstanceAuditLog log = new Matrix26InstanceAuditLog();
        log.setInstance(instance);
        log.setActorUsername(safeActor(actor));
        log.setAction(action);
        log.setSummary(summary.length() <= 500 ? summary : summary.substring(0, 499) + "…");
        log.setAfterSnapshot("{\"instanceCode\":\"" + json(instance.getCode()) + "\"}");
        auditLogRepository.save(log);
    }

    private record KeyMaterial(byte[] key, String keyId) {
    }

    private record PackageInspection(boolean valid, int entries, String message) {
    }
}
