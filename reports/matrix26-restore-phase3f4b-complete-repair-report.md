# Matrix26 Restore Manager Phase 3F.4b

This repair restores the complete `Matrix26InPlaceRestore*` Java source set from Phase 3F.4 and keeps the Phase 3F.4a numeric route constraints.

Cause addressed:

- `Matrix26InPlaceRestoreController` was present.
- `Matrix26InPlaceRestoreService` and `Matrix26InPlaceRestoreJob` were absent from the local source tree.
- Maven therefore failed before evaluating the route hotfix.

The repair does not modify databases, runtime data, backups, credentials, templates, or application properties.
