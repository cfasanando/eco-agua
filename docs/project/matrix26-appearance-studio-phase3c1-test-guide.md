# Matrix26 Appearance Studio — Phase 3C.1 Test Guide

## Scope

This phase establishes the central theme and layout foundation. It does not publish
appearance changes to operational instances yet.

## Expected catalog

Themes:

- Matrix26 Classic
- Matrix26 Nature
- Matrix26 Warm

Layouts:

- Public Classic Grid
- Public Nature Editorial
- Public Restaurant Visual
- Admin Sidebar Classic
- Admin Compact Workspace
- Login Split

## Startup

```bash
mvn clean -DskipTests package
bash scripts/run-matrix26-control.sh
```

Open:

```text
http://localhost:8091/control-center/appearance
```

## Routes

```text
/control-center/appearance
/control-center/themes
/control-center/themes/matrix26-classic
/control-center/themes/matrix26-nature
/control-center/themes/matrix26-warm
/control-center/layouts
/control-center/appearance/instances
/control-center/instances/{id}/appearance
```

## Database verification

The following tables must be created automatically in
`matrix26_platform_control`:

```text
matrix26_theme_catalog
matrix26_layout_catalog
matrix26_instance_appearance
matrix26_instance_appearance_history
```

No manual SQL is required.

## Functional checks

1. Appearance Studio is visible in the Matrix26 sidebar.
2. The theme gallery displays three distinct CSS previews.
3. The layout gallery displays public, administrative and login groups.
4. Every registered instance has an initial published appearance.
5. Restaurant instances use Matrix26 Warm and Restaurant Visual by default.
6. Jungle-product instances use Matrix26 Nature and Nature Editorial by default.
7. Water-delivery instances use Matrix26 Nature and Classic Grid by default.
8. Administrative appearance starts with Matrix26 Classic and Admin Sidebar Classic.
9. The instance detail page contains the **Apariencia** action.
10. Existing Matrix26 screens preserve their current Classic appearance.

## Safety checks

- Do not modify `eco_agua`.
- Do not modify `productos_selva_belen`.
- Do not modify `restaurante_buen_sabor`.
- Do not publish appearance changes to operational portals during this phase.
- All new catalog and appearance records stay in `matrix26_platform_control`.
