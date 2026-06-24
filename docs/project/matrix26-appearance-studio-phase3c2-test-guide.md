# Matrix26 Appearance Studio — Phase 3C.2 test guide

## Goal

Validate the visual editor, safe overrides, drafts and preview workflow without
publishing changes to an operational portal.

## Safety boundary

This phase writes only to `matrix26_platform_control`.

It does not:

- update runtime files;
- write appearance settings into managed instances;
- restart applications;
- modify operational databases;
- publish the draft.

## Automatic schema

Matrix26 creates this table during startup:

```text
matrix26_instance_appearance_draft
```

No manual SQL is required.

## Test target

Use the generated laboratory instance:

```text
Matrix26 Restaurant Laboratory
runtime: matrix26_restaurant_lab
port: 8093
```

## Test procedure

1. Open Matrix26 on `http://localhost:8091`.
2. Go to **Appearance Studio → Por instancia**.
3. Open **Matrix26 Restaurant Laboratory**.
4. Click **Editar apariencia**.
5. Select different public and administrative themes.
6. Select a public layout and an administrative layout.
7. Change the controlled palette values.
8. Change sidebar mode, border radius, table density, content width and heading style.
9. Confirm that the live previews react immediately.
10. Click **Vista previa completa**.
11. Return to the editor.
12. Enter a change reason.
13. Click **Guardar borrador**.
14. Confirm that the instance detail shows a DRAFT badge and draft version.
15. Confirm that the published version is unchanged.
16. Review the Appearance Studio dashboard and confirm that the draft counter increased.
17. Open the saved preview.
18. Discard the draft and confirm that the published version remains unchanged.

## Validation checks

The editor must reject:

- unknown theme codes;
- layouts assigned to the wrong area;
- incompatible theme/layout combinations;
- invalid hexadecimal colors;
- inaccessible text/background contrast;
- unapproved option values;
- a draft without a reason.

## Expected result

- Preview works without touching the instance.
- Drafts are saved in Matrix26 only.
- Every draft save creates a history record.
- Discarding creates a `SUPERSEDED` history entry.
- The published appearance remains at its previous version.
