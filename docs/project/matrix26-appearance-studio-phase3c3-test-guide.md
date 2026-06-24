# Matrix26 Appearance Studio — Phase 3C.3 test guide

## Objective

Publish an approved appearance draft from Matrix26 Control Center to the local
configuration of `matrix26_restaurant_lab`, verify the real frontend, backoffice
and login changes, and restore a previous published version.

## Safety boundary

Real publication is enabled only for the instance code:

```text
matrix26-restaurant-lab
```

The following ports and databases remain blocked:

```text
8081 / eco_agua
8082 / productos_selva_belen
8084 / restaurante_buen_sabor
8091 / matrix26_platform_control
```

The publication creates or updates only this local table in the authorized target
database:

```text
matrix26_instance_appearance_config
```

No orders, products, customers, stock, accounting records or restaurant operations
are modified.

## Apply and build

Stop Matrix26 and the laboratory runtime before replacing files.

```bash
rm -rf target
mvn clean -DskipTests package
```

Start Matrix26:

```bash
bash scripts/run-matrix26-control.sh
```

Start the laboratory in another terminal:

```bash
bash runtime-clients/matrix26_restaurant_lab/run.sh
```

## Publish a draft

1. Open `http://localhost:8091/control-center/appearance/instances`.
2. Open **Matrix26 Restaurant Laboratory**.
3. Create or continue an appearance draft.
4. Review the full preview.
5. Return to the appearance detail.
6. Type the exact instance code:

```text
matrix26-restaurant-lab
```

7. Confirm the acknowledgement.
8. Click **Publish to portal**.

Expected result:

- the central published version increases;
- the draft is removed;
- the local version is detected;
- the synchronization badge changes to **Synchronized**;
- the audit log contains `APPEARANCE_PUBLISHED`.

## Verify the managed portal

Wait up to two seconds and reload:

```text
http://localhost:8093/login
http://localhost:8093/admin/restaurant/dashboard
http://localhost:8093/restaurant/menu
```

Expected:

- login uses the published theme and split layout;
- sidebar, topbar, cards, forms and tables use the administrative theme;
- Compact Workspace collapses the sidebar and expands it on hover;
- public pages use the published public theme and layout;
- the portal remains functional when Matrix26 is stopped.

## Rollback

1. Return to the appearance detail in Matrix26.
2. In **Appearance history**, choose an older `PUBLISHED` version.
3. Click **Restore** and confirm.
4. Reload the portal on port `8093`.

Expected:

- Matrix26 creates a new published version;
- it does not overwrite or delete historical rows;
- the target local version matches Matrix26;
- audit contains `APPEARANCE_ROLLED_BACK`.

## Protected-instance test

Open the appearance detail for Eco Agua, Selva Belén or El Buen Sabor.

Expected:

- publication status is blocked;
- no publication form is available;
- no target database table is created by this phase.

## Persistence test

1. Stop Matrix26.
2. Keep or restart `8093`.
3. Reload login, backoffice and public pages.

The published appearance must remain active because it is stored locally in
`matrix26_restaurant_lab`.
