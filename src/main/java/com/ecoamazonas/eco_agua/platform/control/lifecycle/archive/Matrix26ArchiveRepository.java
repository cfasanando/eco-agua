package com.ecoamazonas.eco_agua.platform.control.lifecycle.archive;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
@ConditionalOnProperty(name = "matrix26.control-center.enabled", havingValue = "true")
public class Matrix26ArchiveRepository {
    private final JdbcTemplate jdbcTemplate;

    public Matrix26ArchiveRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public Optional<Matrix26ArchiveRecord> findByDecommissionJob(long decommissionJobId) {
        return jdbcTemplate.query(
                "SELECT * FROM matrix26_archive_record WHERE decommission_job_id = ?",
                recordMapper(), decommissionJobId
        ).stream().findFirst();
    }

    public Optional<Matrix26ArchiveRecord> findById(long id) {
        return jdbcTemplate.query(
                "SELECT * FROM matrix26_archive_record WHERE id = ?",
                recordMapper(), id
        ).stream().findFirst();
    }

    public List<Matrix26ArchiveRecord> findAll() {
        return jdbcTemplate.query(
                "SELECT * FROM matrix26_archive_record ORDER BY updated_at DESC, id DESC LIMIT 200",
                recordMapper()
        );
    }

    public Matrix26ArchiveSummary summary() {
        return jdbcTemplate.queryForObject("""
                SELECT
                    (SELECT COUNT(*) FROM matrix26_archive_record) AS total_archives,
                    (SELECT COUNT(*) FROM matrix26_archive_record WHERE archive_status = 'READY') AS ready_archives,
                    (SELECT COUNT(*) FROM matrix26_archive_record WHERE last_verified_at IS NOT NULL) AS verified_archives,
                    (SELECT COUNT(*) FROM matrix26_archive_restore_link) AS clone_restores
                """, (rs, rowNum) -> new Matrix26ArchiveSummary(
                rs.getLong("total_archives"),
                rs.getLong("ready_archives"),
                rs.getLong("verified_archives"),
                rs.getLong("clone_restores")
        ));
    }

    public long insertRecord(Matrix26ArchiveRecord record) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            var statement = connection.prepareStatement("""
                    INSERT INTO matrix26_archive_record (
                        public_id, decommission_job_id, decommission_public_id,
                        instance_id, instance_code, instance_name, instance_status,
                        final_backup_job_id, final_backup_public_id, final_backup_sha256,
                        final_backup_key_id, final_backup_verified_at, retention_until,
                        archive_status, retention_status, runtime_file_count, data_file_count,
                        inventory_summary, created_at, updated_at, last_verified_at, last_error
                    ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                    """, Statement.RETURN_GENERATED_KEYS);
            statement.setString(1, record.publicId());
            statement.setLong(2, record.decommissionJobId());
            statement.setString(3, record.decommissionPublicId());
            statement.setLong(4, record.instanceId());
            statement.setString(5, record.instanceCode());
            statement.setString(6, record.instanceName());
            statement.setString(7, record.instanceStatus());
            statement.setLong(8, record.finalBackupJobId());
            statement.setString(9, record.finalBackupPublicId());
            statement.setString(10, record.finalBackupSha256());
            statement.setString(11, record.finalBackupKeyId());
            statement.setObject(12, record.finalBackupVerifiedAt());
            statement.setObject(13, record.retentionUntil());
            statement.setString(14, record.archiveStatus());
            statement.setString(15, record.retentionStatus());
            statement.setInt(16, value(record.runtimeFileCount()));
            statement.setInt(17, value(record.dataFileCount()));
            statement.setString(18, limit(record.inventorySummary(), 8000));
            statement.setObject(19, record.createdAt());
            statement.setObject(20, record.updatedAt());
            statement.setObject(21, record.lastVerifiedAt());
            statement.setString(22, limit(record.lastError(), 8000));
            return statement;
        }, keyHolder);
        Number key = keyHolder.getKey();
        if (key == null) {
            throw new Matrix26ArchiveException("Matrix26 could not persist the archive record.");
        }
        return key.longValue();
    }

    public void updateRecord(Matrix26ArchiveRecord record) {
        jdbcTemplate.update("""
                UPDATE matrix26_archive_record
                SET instance_status = ?, final_backup_sha256 = ?, final_backup_key_id = ?,
                    final_backup_verified_at = ?, retention_until = ?, archive_status = ?,
                    retention_status = ?, runtime_file_count = ?, data_file_count = ?,
                    inventory_summary = ?, updated_at = ?, last_verified_at = ?, last_error = ?
                WHERE id = ?
                """,
                record.instanceStatus(), record.finalBackupSha256(), record.finalBackupKeyId(),
                record.finalBackupVerifiedAt(), record.retentionUntil(), record.archiveStatus(),
                record.retentionStatus(), value(record.runtimeFileCount()), value(record.dataFileCount()),
                limit(record.inventorySummary(), 8000), LocalDateTime.now(), record.lastVerifiedAt(),
                limit(record.lastError(), 8000), record.id());
    }

    public void addEvent(long archiveRecordId, String eventType, String status, String actor, String detail) {
        jdbcTemplate.update("""
                INSERT INTO matrix26_archive_event (
                    archive_record_id, event_type, status, actor, detail, created_at
                ) VALUES (?, ?, ?, ?, ?, ?)
                """, archiveRecordId, eventType, status, limit(actor, 120), limit(detail, 8000), LocalDateTime.now());
    }

    public List<Matrix26ArchiveEvent> events(long archiveRecordId) {
        return jdbcTemplate.query("""
                SELECT * FROM matrix26_archive_event
                WHERE archive_record_id = ? ORDER BY created_at, id
                """, (rs, rowNum) -> new Matrix26ArchiveEvent(
                rs.getLong("id"),
                rs.getLong("archive_record_id"),
                rs.getString("event_type"),
                rs.getString("status"),
                rs.getString("actor"),
                rs.getString("detail"),
                rs.getObject("created_at", LocalDateTime.class)
        ), archiveRecordId);
    }

    public void insertRestoreLink(
            long archiveRecordId,
            long restoreJobId,
            String restorePublicId,
            String targetInstanceCode,
            String targetDatabaseName,
            String targetRuntimeProfile,
            int targetRuntimePort,
            String status,
            String actor
    ) {
        jdbcTemplate.update("""
                INSERT INTO matrix26_archive_restore_link (
                    archive_record_id, restore_job_id, restore_public_id, target_instance_code,
                    target_database_name, target_runtime_profile, target_runtime_port,
                    status, requested_by, requested_at, completed_at
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """, archiveRecordId, restoreJobId, restorePublicId, targetInstanceCode,
                targetDatabaseName, targetRuntimeProfile, targetRuntimePort, status, limit(actor, 120),
                LocalDateTime.now(), "COMPLETED".equals(status) ? LocalDateTime.now() : null);
    }

    public List<Matrix26ArchiveRestoreLink> restoreLinks(long archiveRecordId) {
        return jdbcTemplate.query("""
                SELECT * FROM matrix26_archive_restore_link
                WHERE archive_record_id = ? ORDER BY requested_at DESC, id DESC
                """, restoreLinkMapper(), archiveRecordId);
    }

    public List<Matrix26ArchiveRestoreLink> recentRestoreLinks() {
        return jdbcTemplate.query("""
                SELECT * FROM matrix26_archive_restore_link
                ORDER BY requested_at DESC, id DESC LIMIT 100
                """, restoreLinkMapper());
    }

    private RowMapper<Matrix26ArchiveRecord> recordMapper() {
        return (rs, rowNum) -> new Matrix26ArchiveRecord(
                rs.getLong("id"),
                rs.getString("public_id"),
                rs.getLong("decommission_job_id"),
                rs.getString("decommission_public_id"),
                rs.getLong("instance_id"),
                rs.getString("instance_code"),
                rs.getString("instance_name"),
                rs.getString("instance_status"),
                rs.getLong("final_backup_job_id"),
                rs.getString("final_backup_public_id"),
                rs.getString("final_backup_sha256"),
                rs.getString("final_backup_key_id"),
                rs.getObject("final_backup_verified_at", LocalDateTime.class),
                rs.getObject("retention_until", LocalDateTime.class),
                rs.getString("archive_status"),
                rs.getString("retention_status"),
                rs.getObject("runtime_file_count", Integer.class),
                rs.getObject("data_file_count", Integer.class),
                rs.getString("inventory_summary"),
                rs.getObject("created_at", LocalDateTime.class),
                rs.getObject("updated_at", LocalDateTime.class),
                rs.getObject("last_verified_at", LocalDateTime.class),
                rs.getString("last_error")
        );
    }

    private RowMapper<Matrix26ArchiveRestoreLink> restoreLinkMapper() {
        return (rs, rowNum) -> new Matrix26ArchiveRestoreLink(
                rs.getLong("id"),
                rs.getLong("archive_record_id"),
                rs.getLong("restore_job_id"),
                rs.getString("restore_public_id"),
                rs.getString("target_instance_code"),
                rs.getString("target_database_name"),
                rs.getString("target_runtime_profile"),
                rs.getInt("target_runtime_port"),
                rs.getString("status"),
                rs.getString("requested_by"),
                rs.getObject("requested_at", LocalDateTime.class),
                rs.getObject("completed_at", LocalDateTime.class)
        );
    }

    private int value(Integer value) { return value == null ? 0 : value; }
    private String limit(String value, int max) { return value == null || value.length() <= max ? value : value.substring(0, max); }
}
