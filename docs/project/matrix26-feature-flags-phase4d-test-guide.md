# Matrix26 Feature Flags Phase 4D - Final Acceptance Checklist

## Goal

Close the Matrix26 Feature Flags block with a read-only acceptance page that reviews:

- Phase 4A module activation declarations.
- Phase 4B runtime/sidebar module visibility.
- Phase 4C direct URL route protection.
- Customer profile coverage for Restaurante, Agua Eco, Belén and Matrix26 Control.
- Remaining warnings before moving to a new module development.

## Scope

New route:

```text
/control-center/modules/acceptance
```

This phase is read-only. It does not:

- Save module activation changes.
- Install or uninstall modules.
- Execute SQL in tenant databases.
- Restart runtimes.
- Modify backups, restores, lifecycle, purge or archive-destruction flows.

## Apply

```bash
cd "$HOME/Projects/eco-agua-workspace/eco-agua"

unzip "$HOME/Downloads/matrix26-feature-flags-phase4d-final.zip" \
  -d "$HOME/Downloads"

SRC="$HOME/Downloads/matrix26-feature-flags-phase4d-final"

cp -rf "$SRC/src/." src/
cp -rf "$SRC/scripts/." scripts/
cp -rf "$SRC/docs/." docs/
cp -rf "$SRC/reports/." reports/

chmod +x scripts/check-matrix26-feature-flags-phase4d.sh
chmod +x scripts/configure-matrix26-feature-flags-phase4d.sh
```

## Configure and check

```bash
bash scripts/configure-matrix26-feature-flags-phase4d.sh
bash scripts/check-matrix26-feature-flags-phase4d.sh
```

Expected output:

```text
Matrix26 Feature Flags Phase 4D static checks passed.
```

## Compile

```bash
rm -rf target
mvn clean -DskipTests package
```

## Runtime test

Start Matrix26 Control Center:

```bash
bash scripts/run-matrix26-control.sh
```

Open:

```text
http://localhost:8091/control-center/modules/acceptance
```

Verify:

- Overall status renders without errors.
- Metrics show instances, enabled declarations, runtime flags, route rules and recent events.
- 4A Activation Center group is visible.
- 4B Runtime Navigation group is visible.
- 4C Route Protection group is visible.
- Instance Coverage group is visible.
- Governance and Handoff group is visible.
- No POST actions are present on the acceptance page.

## Suggested final evidence

For final sign-off, capture screenshots from:

```text
http://localhost:8091/control-center/modules/activation
http://localhost:8091/control-center/modules/acceptance
http://localhost:8084/admin/system-modules/visibility
```

If possible, repeat the runtime visibility screenshot for Agua Eco and Productos de la Selva Belén.

## Acceptance result

Feature Flags can be closed when:

- 4A Activation Center works.
- 4B Sidebar/navigation visibility works.
- 4C Direct URL blocking returns 403 for inactive modules.
- 4D Acceptance Checklist has no failed items.
