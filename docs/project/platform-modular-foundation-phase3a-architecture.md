# Platform modular foundation Phase 3A architecture

## Scope

Phase 3A introduces the installation framework only. It does not split every existing business table into separate modules yet.

Restaurant is the pilot module.

## Components

### `PlatformModuleInstaller`

Common contract implemented by installable modules:

- module key;
- display name;
- current schema version;
- schema validation;
- runtime enabled state;
- ordered installation steps;
- activation/deactivation.

### `PlatformModuleManager`

Central orchestrator responsible for:

- explicit installation only;
- MySQL advisory lock per database and module;
- registry creation;
- ordered step execution;
- version and status tracking;
- activation only after validation succeeds;
- failure recording and safe deactivation;
- synchronization of legacy installations;
- data-preserving disable.

### `platform_module_installation`

Per-database registry with:

- module key;
- installed and target version;
- status;
- enabled state;
- current installation step;
- last error;
- start, completion and update timestamps.

### Runtime protection

`ecoagua.modules.installation-allowed` controls schema-changing actions.

The default is `false`. This protects existing runtimes unless they are explicitly managed.

`PlatformRuntimeService` writes:

- `false` for protected instances;
- `true` for non-protected generated runtimes.

### Restaurant adapter

`RestaurantModuleInstaller` now implements the common contract. Its installation is divided into ordered steps:

1. schema;
2. module catalog;
3. settings;
4. optional demo data.

The old direct controller-to-installer coupling was removed.

## Activation semantics

```text
Install requested
→ acquire database/module lock
→ create registry if needed
→ mark INSTALLING
→ execute ordered steps
→ validate schema
→ enable module
→ mark ACTIVE
```

On failure:

```text
Step fails
→ disable module
→ preserve any already-created data
→ record FAILED and last error
→ do not report the module as active
```

## Deactivation semantics

```text
Disable requested
→ set runtime flag to false
→ mark registry DISABLED
→ keep tables and data
```

## Legacy synchronization

An already-installed module without a registry row is shown as `Installed without registry`.

Synchronization records its current version without rebuilding or deleting data.

## Deferred work

Later phases will:

- define the true minimum common schema;
- move each module to its own installer;
- replace full-schema provisioning with base-plus-modules provisioning;
- introduce versioned migration steps per module;
- test clean installations for every business template.
