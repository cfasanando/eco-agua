# Matrix26 Operations Phase 3I.3 Report

## Summary

Implemented Matrix26 roles and permissions as an incremental platform security layer.

## Added

- Matrix26 control role definitions.
- Matrix26 control permission definitions.
- Startup initializer for roles and permissions.
- Bootstrap admin assignment to `MATRIX26_ADMIN`.
- Read-only `/control-center/security` overview.
- Security model flags for templates.
- Granular Spring Security route guards for sensitive Control Center POST actions.
- Sidebar link for Security.
- Static verification script.
- Configuration script.
- Test guide.

## Safety

No destructive operation was added.

The new package does not contain:

- `DROP DATABASE`
- `DROP SCHEMA`
- `Files.delete`
- `deleteRecursively`
- `deleteIfExists`
- archive package deletion

## Main route guards

- Alert actions: `MATRIX26_OPERATOR` / `matrix26.alerts.manage`
- Runtime actions: `MATRIX26_OPERATOR` / `matrix26.runtimes.control`
- Backup actions: `MATRIX26_BACKUP_MANAGER` / `matrix26.backups.manage`
- Restore actions: `MATRIX26_RESTORE_MANAGER` / `matrix26.restores.manage`
- Lifecycle actions: `MATRIX26_LIFECYCLE_MANAGER` / `matrix26.lifecycle.manage`
- Purge and archive destruction: `MATRIX26_PURGE_MANAGER` / `matrix26.purge.manage`
- Security: `MATRIX26_ADMIN` / `matrix26.security.admin`
- Settings/modules POST: `MATRIX26_ADMIN` / `matrix26.settings.admin`

## Verification

Static verification script included:

```bash
bash scripts/check-matrix26-operations-phase3i3.sh
```

Expected:

```text
Matrix26 Operations Phase 3I.3 static checks passed.
```

Full Maven compilation must be confirmed in the target workstation.
