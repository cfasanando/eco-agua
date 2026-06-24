Matrix26 Control Center

Port: 8091
Database: matrix26_platform_control
Runtime profile: matrix26_control

Start from the project root:

  bash scripts/run-matrix26-control.sh

Default local credentials:

  matrix_admin / Matrix26Demo123!

Override secrets without editing the file:

  export MATRIX26_DB_USERNAME=root
  export MATRIX26_DB_PASSWORD=root
  export MATRIX26_ADMIN_USERNAME=matrix_admin
  export MATRIX26_ADMIN_PASSWORD='YourSecurePassword'
  bash scripts/run-matrix26-control.sh

Capabilities through Phase 3B:

- register and edit instance metadata;
- validate duplicate code, port, database, runtime profile, and runtime folder;
- enable or pause monitoring;
- execute manual health checks;
- maintain module declarations;
- audit administrative changes;
- create and revalidate provisioning plans;
- execute READY plans after explicit confirmation;
- create a new isolated database through Spring JDBC;
- copy structural core tables without operational data;
- create the initial administrator without storing the plaintext password in Matrix26;
- execute target-compatible module installers;
- generate a dedicated runtime folder and register the new protected instance;
- resume a FAILED plan from its pending or failed step.

Provisioning configuration:

  matrix26.control-center.provisioning-execution-enabled=true
  matrix26.control-center.provisioning-template-database=restaurante_buen_sabor
  matrix26.control-center.provisioning-runtime-directory=runtime-clients

The source template is read only. Matrix26 never copies its business data. A target
database that already contains tables is rejected unless it belongs to the same
resumable provisioning plan.

The generated runtime is not started automatically. Start it from the repository root:

  bash runtime-clients/<runtime-profile>/run.sh

Matrix26 does not modify eco_agua, productos_selva_belen, or restaurante_buen_sabor.
The template database is queried only to read table definitions.
