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

Phase 2 capabilities:

- register and edit instance metadata;
- validate duplicate code, port, database, and runtime profile;
- enable or pause monitoring;
- execute manual health checks;
- maintain module declarations;
- audit administrative changes.

Matrix26 writes only to matrix26_platform_control. It does not modify eco_agua,
productos_selva_belen, restaurante_buen_sabor, or any other operational database.
