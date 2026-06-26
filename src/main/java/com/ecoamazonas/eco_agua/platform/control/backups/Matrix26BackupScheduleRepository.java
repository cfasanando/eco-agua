package com.ecoamazonas.eco_agua.platform.control.backups;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
@ConditionalOnProperty(name = "matrix26.control-center.enabled", havingValue = "true")
public class Matrix26BackupScheduleRepository {

    private final JdbcTemplate jdbcTemplate;

    public Matrix26BackupScheduleRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public long insertSchedule(Matrix26BackupSchedule schedule) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            var statement = connection.prepareStatement(
                    """
                    INSERT INTO matrix26_backup_schedule (
                        instance_id, instance_code, instance_name, name, frequency,
                        day_of_week, day_of_month, hour_of_day, minute_of_hour, timezone,
                        encryption_required, retention_class, max_attempts, retry_delay_minutes,
                        missed_policy, enabled, next_run_at, last_run_at, last_status,
                        created_by, created_at, updated_by, updated_at
                    ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                    """,
                    Statement.RETURN_GENERATED_KEYS
            );
            statement.setLong(1, schedule.instanceId());
            statement.setString(2, schedule.instanceCode());
            statement.setString(3, schedule.instanceName());
            statement.setString(4, schedule.name());
            statement.setString(5, schedule.frequency().name());
            statement.setObject(6, schedule.dayOfWeek());
            statement.setObject(7, schedule.dayOfMonth());
            statement.setInt(8, schedule.hourOfDay());
            statement.setInt(9, schedule.minuteOfHour());
            statement.setString(10, schedule.timezone());
            statement.setBoolean(11, schedule.encryptionRequired());
            statement.setString(12, schedule.retentionClass().name());
            statement.setInt(13, schedule.maxAttempts());
            statement.setInt(14, schedule.retryDelayMinutes());
            statement.setString(15, schedule.missedPolicy().name());
            statement.setBoolean(16, schedule.enabled());
            statement.setObject(17, schedule.nextRunAt());
            statement.setObject(18, schedule.lastRunAt());
            statement.setString(19, schedule.lastStatus());
            statement.setString(20, schedule.createdBy());
            statement.setObject(21, schedule.createdAt());
            statement.setString(22, schedule.updatedBy());
            statement.setObject(23, schedule.updatedAt());
            return statement;
        }, keyHolder);
        Number id = keyHolder.getKey();
        if (id == null) {
            throw new IllegalStateException("The backup schedule could not be created.");
        }
        return id.longValue();
    }

    public void updateSchedule(Matrix26BackupSchedule schedule) {
        jdbcTemplate.update(
                """
                UPDATE matrix26_backup_schedule
                SET name = ?, frequency = ?, day_of_week = ?, day_of_month = ?,
                    hour_of_day = ?, minute_of_hour = ?, timezone = ?,
                    encryption_required = ?, retention_class = ?, max_attempts = ?,
                    retry_delay_minutes = ?, missed_policy = ?, enabled = ?,
                    next_run_at = ?, updated_by = ?, updated_at = ?
                WHERE id = ?
                """,
                schedule.name(),
                schedule.frequency().name(),
                schedule.dayOfWeek(),
                schedule.dayOfMonth(),
                schedule.hourOfDay(),
                schedule.minuteOfHour(),
                schedule.timezone(),
                schedule.encryptionRequired(),
                schedule.retentionClass().name(),
                schedule.maxAttempts(),
                schedule.retryDelayMinutes(),
                schedule.missedPolicy().name(),
                schedule.enabled(),
                schedule.nextRunAt(),
                schedule.updatedBy(),
                schedule.updatedAt(),
                schedule.id()
        );
    }

    public Optional<Matrix26BackupSchedule> findSchedule(long id) {
        return jdbcTemplate.query(
                "SELECT * FROM matrix26_backup_schedule WHERE id = ?",
                scheduleMapper(),
                id
        ).stream().findFirst();
    }

    public List<Matrix26BackupSchedule> findSchedules() {
        return jdbcTemplate.query(
                "SELECT * FROM matrix26_backup_schedule ORDER BY instance_name, name",
                scheduleMapper()
        );
    }

    public List<Matrix26BackupSchedule> findEnabledSchedules() {
        return jdbcTemplate.query(
                "SELECT * FROM matrix26_backup_schedule WHERE enabled = 1 ORDER BY next_run_at, id",
                scheduleMapper()
        );
    }

    public List<Matrix26BackupSchedule> findSchedulesByInstance(long instanceId) {
        return jdbcTemplate.query(
                "SELECT * FROM matrix26_backup_schedule WHERE instance_id = ? ORDER BY name",
                scheduleMapper(),
                instanceId
        );
    }

    public void updateScheduleAfterEvaluation(
            long scheduleId,
            LocalDateTime nextRunAt,
            LocalDateTime lastRunAt,
            String lastStatus
    ) {
        jdbcTemplate.update(
                """
                UPDATE matrix26_backup_schedule
                SET next_run_at = ?, last_run_at = ?, last_status = ?, updated_at = ?
                WHERE id = ?
                """,
                nextRunAt,
                lastRunAt,
                lastStatus,
                LocalDateTime.now(),
                scheduleId
        );
    }

    public void setScheduleEnabled(long scheduleId, boolean enabled, LocalDateTime nextRunAt, String actor) {
        jdbcTemplate.update(
                """
                UPDATE matrix26_backup_schedule
                SET enabled = ?, next_run_at = ?, updated_by = ?, updated_at = ?
                WHERE id = ?
                """,
                enabled,
                nextRunAt,
                actor,
                LocalDateTime.now(),
                scheduleId
        );
    }

    public Optional<Long> insertExecution(Matrix26BackupScheduleExecution execution) {
        try {
            KeyHolder keyHolder = new GeneratedKeyHolder();
            jdbcTemplate.update(connection -> {
                var statement = connection.prepareStatement(
                        """
                        INSERT INTO matrix26_backup_schedule_execution (
                            schedule_id, instance_id, instance_code, instance_name, schedule_name,
                            planned_at, status, attempt_count, max_attempts, queued_at, started_at,
                            completed_at, next_retry_at, backup_job_id, backup_public_id,
                            trigger_type, error_message, created_at, updated_at
                        ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                        """,
                        Statement.RETURN_GENERATED_KEYS
                );
                statement.setLong(1, execution.scheduleId());
                statement.setLong(2, execution.instanceId());
                statement.setString(3, execution.instanceCode());
                statement.setString(4, execution.instanceName());
                statement.setString(5, execution.scheduleName());
                statement.setObject(6, execution.plannedAt());
                statement.setString(7, execution.status().name());
                statement.setInt(8, execution.attemptCount());
                statement.setInt(9, execution.maxAttempts());
                statement.setObject(10, execution.queuedAt());
                statement.setObject(11, execution.startedAt());
                statement.setObject(12, execution.completedAt());
                statement.setObject(13, execution.nextRetryAt());
                statement.setObject(14, execution.backupJobId());
                statement.setString(15, execution.backupPublicId());
                statement.setString(16, execution.triggerType());
                statement.setString(17, limit(execution.errorMessage(), 4000));
                statement.setObject(18, execution.createdAt());
                statement.setObject(19, execution.updatedAt());
                return statement;
            }, keyHolder);
            Number id = keyHolder.getKey();
            return id == null ? Optional.empty() : Optional.of(id.longValue());
        } catch (DuplicateKeyException ex) {
            return Optional.empty();
        }
    }

    public Optional<Matrix26BackupScheduleExecution> findExecution(long id) {
        return jdbcTemplate.query(
                """
                SELECT e.*, s.name AS current_schedule_name
                FROM matrix26_backup_schedule_execution e
                LEFT JOIN matrix26_backup_schedule s ON s.id = e.schedule_id
                WHERE e.id = ?
                """,
                executionMapper(),
                id
        ).stream().findFirst();
    }

    public List<Matrix26BackupScheduleExecution> findRecentExecutions(int limit) {
        int safeLimit = Math.max(1, Math.min(limit, 500));
        return jdbcTemplate.query(
                """
                SELECT e.*, s.name AS current_schedule_name
                FROM matrix26_backup_schedule_execution e
                LEFT JOIN matrix26_backup_schedule s ON s.id = e.schedule_id
                ORDER BY e.planned_at DESC, e.id DESC
                LIMIT
                """ + " " + safeLimit,
                executionMapper()
        );
    }

    public List<Matrix26BackupScheduleExecution> findExecutionsBySchedule(long scheduleId) {
        return jdbcTemplate.query(
                """
                SELECT e.*, s.name AS current_schedule_name
                FROM matrix26_backup_schedule_execution e
                LEFT JOIN matrix26_backup_schedule s ON s.id = e.schedule_id
                WHERE e.schedule_id = ?
                ORDER BY e.planned_at DESC, e.id DESC
                LIMIT 200
                """,
                executionMapper(),
                scheduleId
        );
    }

    public List<Matrix26BackupScheduleExecution> findRetryable(LocalDateTime now) {
        return jdbcTemplate.query(
                """
                SELECT e.*, s.name AS current_schedule_name
                FROM matrix26_backup_schedule_execution e
                LEFT JOIN matrix26_backup_schedule s ON s.id = e.schedule_id
                WHERE e.status = 'RETRY_WAITING'
                  AND e.next_retry_at <= ?
                ORDER BY e.next_retry_at, e.id
                """,
                executionMapper(),
                now
        );
    }

    public List<Matrix26BackupScheduleExecution> findCalendarExecutions(LocalDateTime from, LocalDateTime to) {
        return jdbcTemplate.query(
                """
                SELECT e.*, s.name AS current_schedule_name
                FROM matrix26_backup_schedule_execution e
                LEFT JOIN matrix26_backup_schedule s ON s.id = e.schedule_id
                WHERE e.planned_at >= ? AND e.planned_at < ?
                ORDER BY e.planned_at, e.id
                """,
                executionMapper(),
                from,
                to
        );
    }

    public boolean hasActiveExecution(long instanceId, Long excludedExecutionId) {
        Integer count;
        if (excludedExecutionId == null) {
            count = jdbcTemplate.queryForObject(
                    """
                    SELECT COUNT(*) FROM matrix26_backup_schedule_execution
                    WHERE instance_id = ?
                      AND status IN ('QUEUED','RUNNING','RETRY_WAITING')
                    """,
                    Integer.class,
                    instanceId
            );
        } else {
            count = jdbcTemplate.queryForObject(
                    """
                    SELECT COUNT(*) FROM matrix26_backup_schedule_execution
                    WHERE instance_id = ?
                      AND id <> ?
                      AND status IN ('QUEUED','RUNNING','RETRY_WAITING')
                    """,
                    Integer.class,
                    instanceId,
                    excludedExecutionId
            );
        }
        return count != null && count > 0;
    }

    public void markExecutionQueued(long id) {
        jdbcTemplate.update(
                """
                UPDATE matrix26_backup_schedule_execution
                SET status = 'QUEUED', queued_at = ?, updated_at = ?
                WHERE id = ?
                """,
                LocalDateTime.now(),
                LocalDateTime.now(),
                id
        );
    }

    public int markExecutionRunning(long id) {
        return jdbcTemplate.update(
                """
                UPDATE matrix26_backup_schedule_execution
                SET status = 'RUNNING', attempt_count = attempt_count + 1,
                    started_at = ?, next_retry_at = NULL, error_message = NULL, updated_at = ?
                WHERE id = ?
                  AND status IN ('SCHEDULED','QUEUED','RETRY_WAITING')
                """,
                LocalDateTime.now(),
                LocalDateTime.now(),
                id
        );
    }

    public void completeExecution(long id, long backupJobId, String backupPublicId) {
        jdbcTemplate.update(
                """
                UPDATE matrix26_backup_schedule_execution
                SET status = 'COMPLETED', completed_at = ?, backup_job_id = ?,
                    backup_public_id = ?, error_message = NULL, next_retry_at = NULL, updated_at = ?
                WHERE id = ?
                """,
                LocalDateTime.now(),
                backupJobId,
                backupPublicId,
                LocalDateTime.now(),
                id
        );
    }

    public void updateExecutionFailure(
            long id,
            Matrix26BackupScheduleExecutionStatus status,
            LocalDateTime nextRetryAt,
            String error
    ) {
        jdbcTemplate.update(
                """
                UPDATE matrix26_backup_schedule_execution
                SET status = ?, completed_at = ?, next_retry_at = ?, error_message = ?, updated_at = ?
                WHERE id = ?
                """,
                status.name(),
                status == Matrix26BackupScheduleExecutionStatus.RETRY_WAITING ? null : LocalDateTime.now(),
                nextRetryAt,
                limit(error, 4000),
                LocalDateTime.now(),
                id
        );
    }

    public void markExecutionFinal(long id, Matrix26BackupScheduleExecutionStatus status, String message) {
        jdbcTemplate.update(
                """
                UPDATE matrix26_backup_schedule_execution
                SET status = ?, completed_at = ?, error_message = ?, updated_at = ?
                WHERE id = ?
                """,
                status.name(),
                LocalDateTime.now(),
                limit(message, 4000),
                LocalDateTime.now(),
                id
        );
    }

    public long insertAlert(Matrix26BackupAlert alert) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            var statement = connection.prepareStatement(
                    """
                    INSERT INTO matrix26_backup_alert (
                        instance_id, instance_code, schedule_id, execution_id, alert_code,
                        severity, status, title, message, created_at, resolved_at, resolved_by
                    ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                    """,
                    Statement.RETURN_GENERATED_KEYS
            );
            statement.setLong(1, alert.instanceId());
            statement.setString(2, alert.instanceCode());
            statement.setObject(3, alert.scheduleId());
            statement.setObject(4, alert.executionId());
            statement.setString(5, alert.alertCode());
            statement.setString(6, alert.severity().name());
            statement.setString(7, alert.status().name());
            statement.setString(8, alert.title());
            statement.setString(9, limit(alert.message(), 4000));
            statement.setObject(10, alert.createdAt());
            statement.setObject(11, alert.resolvedAt());
            statement.setString(12, alert.resolvedBy());
            return statement;
        }, keyHolder);
        Number id = keyHolder.getKey();
        return id == null ? 0L : id.longValue();
    }

    public boolean hasOpenAlert(long instanceId, Long scheduleId, String alertCode) {
        Integer count = jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*) FROM matrix26_backup_alert
                WHERE instance_id = ?
                  AND ((schedule_id IS NULL AND ? IS NULL) OR schedule_id = ?)
                  AND alert_code = ?
                  AND status = 'OPEN'
                """,
                Integer.class,
                instanceId,
                scheduleId,
                scheduleId,
                alertCode
        );
        return count != null && count > 0;
    }

    public void resolveAlerts(long instanceId, Long scheduleId, List<String> codes, String actor) {
        if (codes == null || codes.isEmpty()) {
            return;
        }
        String placeholders = String.join(",", java.util.Collections.nCopies(codes.size(), "?"));
        java.util.ArrayList<Object> parameters = new java.util.ArrayList<>();
        parameters.add(LocalDateTime.now());
        parameters.add(actor);
        parameters.add(instanceId);
        parameters.add(scheduleId);
        parameters.add(scheduleId);
        parameters.addAll(codes);
        jdbcTemplate.update(
                """
                UPDATE matrix26_backup_alert
                SET status = 'RESOLVED', resolved_at = ?, resolved_by = ?
                WHERE instance_id = ?
                  AND ((schedule_id IS NULL AND ? IS NULL) OR schedule_id = ?)
                  AND alert_code IN (
                """ + placeholders + ") AND status = 'OPEN'",
                parameters.toArray()
        );
    }

    public void resolveAlert(long alertId, String actor) {
        jdbcTemplate.update(
                """
                UPDATE matrix26_backup_alert
                SET status = 'RESOLVED', resolved_at = ?, resolved_by = ?
                WHERE id = ? AND status = 'OPEN'
                """,
                LocalDateTime.now(),
                actor,
                alertId
        );
    }

    public List<Matrix26BackupAlert> findAlerts(boolean openOnly) {
        String sql = """
                SELECT * FROM matrix26_backup_alert
                """ + (openOnly ? " WHERE status = 'OPEN' " : " ") + """
                ORDER BY created_at DESC, id DESC
                LIMIT 300
                """;
        return jdbcTemplate.query(sql, alertMapper());
    }

    public Matrix26BackupScheduleSummary summary() {
        Long activeSchedules = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM matrix26_backup_schedule WHERE enabled = 1",
                Long.class
        );
        Long openAlerts = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM matrix26_backup_alert WHERE status = 'OPEN'",
                Long.class
        );
        Long completed = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM matrix26_backup_schedule_execution WHERE status = 'COMPLETED'",
                Long.class
        );
        Long failed = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM matrix26_backup_schedule_execution WHERE status = 'FAILED'",
                Long.class
        );
        Long missed = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM matrix26_backup_schedule_execution WHERE status = 'MISSED'",
                Long.class
        );
        return new Matrix26BackupScheduleSummary(
                value(activeSchedules),
                value(openAlerts),
                value(completed),
                value(failed),
                value(missed)
        );
    }

    private org.springframework.jdbc.core.RowMapper<Matrix26BackupSchedule> scheduleMapper() {
        return (rs, rowNum) -> new Matrix26BackupSchedule(
                rs.getLong("id"),
                rs.getLong("instance_id"),
                rs.getString("instance_code"),
                rs.getString("instance_name"),
                rs.getString("name"),
                Matrix26BackupScheduleFrequency.from(rs.getString("frequency")),
                rs.getObject("day_of_week", Integer.class),
                rs.getObject("day_of_month", Integer.class),
                rs.getObject("hour_of_day", Integer.class),
                rs.getObject("minute_of_hour", Integer.class),
                rs.getString("timezone"),
                rs.getBoolean("encryption_required"),
                Matrix26BackupRetentionClass.from(rs.getString("retention_class")),
                rs.getObject("max_attempts", Integer.class),
                rs.getObject("retry_delay_minutes", Integer.class),
                Matrix26BackupMissedPolicy.from(rs.getString("missed_policy")),
                rs.getBoolean("enabled"),
                rs.getObject("next_run_at", LocalDateTime.class),
                rs.getObject("last_run_at", LocalDateTime.class),
                rs.getString("last_status"),
                rs.getString("created_by"),
                rs.getObject("created_at", LocalDateTime.class),
                rs.getString("updated_by"),
                rs.getObject("updated_at", LocalDateTime.class)
        );
    }

    private org.springframework.jdbc.core.RowMapper<Matrix26BackupScheduleExecution> executionMapper() {
        return (rs, rowNum) -> new Matrix26BackupScheduleExecution(
                rs.getLong("id"),
                rs.getLong("schedule_id"),
                rs.getLong("instance_id"),
                rs.getString("instance_code"),
                rs.getString("instance_name"),
                firstNonBlank(rs.getString("schedule_name"), rs.getString("current_schedule_name")),
                rs.getObject("planned_at", LocalDateTime.class),
                Matrix26BackupScheduleExecutionStatus.valueOf(rs.getString("status")),
                rs.getObject("attempt_count", Integer.class),
                rs.getObject("max_attempts", Integer.class),
                rs.getObject("queued_at", LocalDateTime.class),
                rs.getObject("started_at", LocalDateTime.class),
                rs.getObject("completed_at", LocalDateTime.class),
                rs.getObject("next_retry_at", LocalDateTime.class),
                rs.getObject("backup_job_id", Long.class),
                rs.getString("backup_public_id"),
                rs.getString("trigger_type"),
                rs.getString("error_message"),
                rs.getObject("created_at", LocalDateTime.class),
                rs.getObject("updated_at", LocalDateTime.class)
        );
    }

    private org.springframework.jdbc.core.RowMapper<Matrix26BackupAlert> alertMapper() {
        return (rs, rowNum) -> new Matrix26BackupAlert(
                rs.getLong("id"),
                rs.getLong("instance_id"),
                rs.getString("instance_code"),
                rs.getObject("schedule_id", Long.class),
                rs.getObject("execution_id", Long.class),
                rs.getString("alert_code"),
                Matrix26BackupAlertSeverity.valueOf(rs.getString("severity")),
                Matrix26BackupAlertStatus.valueOf(rs.getString("status")),
                rs.getString("title"),
                rs.getString("message"),
                rs.getObject("created_at", LocalDateTime.class),
                rs.getObject("resolved_at", LocalDateTime.class),
                rs.getString("resolved_by")
        );
    }

    private long value(Long value) {
        return value == null ? 0L : value;
    }

    private String firstNonBlank(String first, String second) {
        if (first != null && !first.isBlank()) {
            return first;
        }
        return second == null ? "" : second;
    }

    private String limit(String value, int maximum) {
        if (value == null || value.length() <= maximum) {
            return value;
        }
        return value.substring(0, maximum);
    }
}
