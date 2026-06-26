# Matrix26 Restore Manager Phase 3F.4c

## Problem

The in-place restore controller resolved correctly, but Thymeleaf failed because the templates were absent from the application source tree.

## Fix

Restored the three templates referenced by `Matrix26InPlaceRestoreController`:

- `in_place_index.html`
- `in_place_new.html`
- `in_place_detail.html`

No Java code, database schema, runtime configuration, backup, or instance data is modified by this hotfix.
