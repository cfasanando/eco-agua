# Matrix26 Feature Flags Phase 4B - Runtime navigation projection

## Scope

Phase 4B connects Matrix26 module activation declarations to the client runtime visibility model.

It is intentionally limited to navigation/sidebar projection and diagnostics. It does not install modules, uninstall modules, restart runtimes, purge data, or add new route hardening rules.

## Main route

Client runtime diagnostic route:

```text
/admin/system-modules/visibility
```

Matrix26 activation source remains:

```text
/control-center/modules/activation
```

## What changed

- Adds `SystemModuleVisibilityMapper` to translate Matrix26 module declarations such as `sales`, `inventory`, `finance`, `restaurant_cash`, `restaurant_qr`, `restaurant_recipes` and `restaurant_reservations` into runtime module flags.
- Projects activation into generated runtime properties through `PlatformRuntimeService`.
- Projects activation into target database `platform_setting` values through `Matrix26TargetDatabaseService` during provisioning/final business settings.
- Adds granular restaurant visibility attributes in `GlobalModelAttributes`.
- Applies granular restaurant flags in `fragments/sidebar.html`.
- Adds a runtime diagnostic page under System Modules.

## Apply

```bash
cd "$HOME/Projects/eco-agua-workspace/eco-agua"

bash scripts/configure-matrix26-feature-flags-phase4b.sh
bash scripts/check-matrix26-feature-flags-phase4b.sh
mvn clean -DskipTests package
```

## Test checklist

### 1. Matrix26 activation remains available

Open:

```text
http://localhost:8091/control-center/modules/activation
```

Confirm that module activation still loads and saves declarations.

### 2. Runtime diagnostic page loads

Open a client runtime, for example restaurant:

```text
http://localhost:8084/admin/system-modules/visibility
```

Expected:

- The page shows current database.
- It shows effective visible/hidden module flags.
- Restaurant granular flags appear when available.

### 3. Restaurant navigation projection

With restaurant enabled and granular restaurant flags active:

- `restaurant_cash` shows Caja diaria, Cierre de caja and Reportes.
- `restaurant_qr` shows Pedidos QR, Pedidos externos and Carta pública.
- `restaurant_recipes` shows Ingredientes y recetas.
- `restaurant_reservations` shows Reservas and Solicitudes.

### 4. Non-restaurant client navigation

Open Agua Eco or Productos Belén runtime.

Expected:

- Restaurant group is hidden when restaurant flag is disabled.
- Business modules are shown according to active runtime flags.
- Public catalog / marketing visibility follows module settings.

### 5. No destructive behavior

Confirm:

- No runtime is restarted by this phase.
- No module is installed or removed by this phase.
- No backup, restore, purge or archive-destruction flow is triggered.

## Static check

```bash
bash scripts/check-matrix26-feature-flags-phase4b.sh
```

Expected:

```text
Matrix26 Feature Flags Phase 4B static checks passed.
```

## Notes

Existing route protection through `SystemModuleAccessFilter` remains unchanged. Phase 4B does not expand route hardening; a later Phase 4C can review route-level enforcement in a focused way.
