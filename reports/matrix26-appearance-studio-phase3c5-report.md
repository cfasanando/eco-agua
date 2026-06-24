# Matrix26 Appearance Studio — Phase 3C.5 report

## Scope

This phase adds isolated branding and visual asset management per Matrix26
instance.

## Features

- Branding text editor.
- Primary and compact logos.
- Favicon.
- Login cover.
- Primary and secondary hero images.
- Product placeholder.
- Social sharing image.
- Safe multipart validation.
- Draft storage isolated by instance.
- Atomic publication into runtime data.
- Local runtime asset controller.
- Runtime branding configuration loaded from the instance database.
- Joint publication and rollback with theme/layout versions.
- One-click complete demo kit.
- Standalone sample files and test data.

## Security decisions

- SVG is not accepted in this phase.
- File content is validated using magic bytes.
- User-provided paths are never used as destination paths.
- Published file names are generated from controlled asset types.
- Paths are normalized and constrained inside `runtime-data`.
- Protected business databases remain unchanged.
- Files are not stored as database BLOBs.
- Generated runtime assets are not committed to Git.

## Database impact

Matrix26 creates only:

```text
matrix26_instance_branding_draft
matrix26_instance_branding_asset
```

The managed laboratory appearance table receives two nullable columns when the
first branding version is published:

```text
branding_json
asset_manifest_json
```

No manual SQL is required.

## Demo content

A complete restaurant-themed test kit is bundled with the feature, so the user can
validate the workflow without searching for images or preparing content.
