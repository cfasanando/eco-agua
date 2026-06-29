# Matrix26 Operations Phase 3I.3 — Roles and Permissions

## Scope

This phase adds a first Matrix26-specific authorization layer for Control Center operations.

It introduces:

- Matrix26 platform roles.
- Matrix26 platform permissions.
- Automatic seeding at startup.
- A read-only security overview page.
- HTTP route guards for sensitive POST actions.

## New route

- `/control-center/security`

## Seeded roles

- `MATRIX26_VIEWER`
- `MATRIX26_OPERATOR`
- `MATRIX26_BACKUP_MANAGER`
- `MATRIX26_RESTORE_MANAGER`
- `MATRIX26_LIFECYCLE_MANAGER`
- `MATRIX26_PURGE_MANAGER`
- `MATRIX26_ADMIN`

## Seeded permissions

- `matrix26.view`
- `matrix26.alerts.manage`
- `matrix26.runtimes.control`
- `matrix26.backups.manage`
- `matrix26.restores.manage`
- `matrix26.lifecycle.manage`
- `matrix26.purge.manage`
- `matrix26.appearance.manage`
- `matrix26.provisioning.manage`
- `matrix26.security.admin`
- `matrix26.settings.admin`

## Safety guarantees

This phase does not:

- start runtimes;
- stop runtimes;
- create backups;
- restore instances;
- purge files;
- destroy archives;
- delete databases;
- change business instance data.

## Expected behavior

The bootstrap administrator keeps full access through:

- `ROLE_SUPER_ADMIN`
- `MATRIX26_ADMIN`

The following action groups are protected:

| Area | Required role or permission |
| --- | --- |
| Alert actions | `MATRIX26_OPERATOR` or `matrix26.alerts.manage` |
| Runtime actions | `MATRIX26_OPERATOR` or `matrix26.runtimes.control` |
| Backup actions | `MATRIX26_BACKUP_MANAGER` or `matrix26.backups.manage` |
| Restore actions | `MATRIX26_RESTORE_MANAGER` or `matrix26.restores.manage` |
| Lifecycle actions | `MATRIX26_LIFECYCLE_MANAGER` or `matrix26.lifecycle.manage` |
| Purge and archive destruction actions | `MATRIX26_PURGE_MANAGER` or `matrix26.purge.manage` |
| Security page | `MATRIX26_ADMIN` or `matrix26.security.admin` |
| Settings and modules POST actions | `MATRIX26_ADMIN` or `matrix26.settings.admin` |

Read-only Control Center pages require Matrix26 view access or an administrative legacy/super-admin authority.

## Apply

```bash
cd "$HOME/Projects/eco-agua-workspace/eco-agua"

unzip "$HOME/Downloads/matrix26-operations-phase3i3-final.zip" \
  -d "$HOME/Downloads"

SRC="$HOME/Downloads/matrix26-operations-phase3i3-final"

cp -rf "$SRC/src/." src/
cp -rf "$SRC/scripts/." scripts/
cp -rf "$SRC/docs/." docs/
cp -rf "$SRC/reports/." reports/

chmod +x scripts/check-matrix26-operations-phase3i3.sh
chmod +x scripts/configure-matrix26-operations-phase3i3.sh
```

## Configure

```bash
bash scripts/configure-matrix26-operations-phase3i3.sh
```

## Static verification

```bash
bash scripts/check-matrix26-operations-phase3i3.sh
```

Expected output:

```text
Matrix26 Operations Phase 3I.3 static checks passed.
```

## Compile

```bash
rm -rf target
mvn clean -DskipTests package
```

Expected output:

```text
BUILD SUCCESS
```

## Functional test

Start Matrix26:

```bash
bash scripts/run-matrix26-control.sh
```

Open:

```text
http://localhost:8091/control-center/security
```

Validate:

- Matrix26 roles are visible.
- Matrix26 permissions are visible.
- Bootstrap admin user has `ROLE_SUPER_ADMIN` and `MATRIX26_ADMIN`.
- Sensitive action mapping is visible.
- Existing dashboard, alert center, backup, restore, lifecycle and purge pages still load.

## Negative permission test

Create or use a user with only `MATRIX26_VIEWER`.

Expected:

- GET pages load.
- POST actions such as runtime start, backup creation, restore execution, purge execution and archive destruction are blocked.

## Commit

```bash
git add \
  src/main/java/com/ecoamazonas/eco_agua/platform/control/security \
  src/main/java/com/ecoamazonas/eco_agua/platform/control/Matrix26ControlCenterModelAdvice.java \
  src/main/java/com/ecoamazonas/eco_agua/security/SecurityConfig.java \
  src/main/java/com/ecoamazonas/eco_agua/user/PermissionRepository.java \
  src/main/resources/templates/control_center/security \
  src/main/resources/templates/control_center/fragments/sidebar.html \
  src/main/resources/static/css/matrix26-control.css \
  scripts/check-matrix26-operations-phase3i3.sh \
  scripts/configure-matrix26-operations-phase3i3.sh \
  docs/project/matrix26-operations-phase3i3-test-guide.md \
  reports/matrix26-operations-phase3i3*

git commit -m "Add Matrix26 operations roles and permissions"
```
