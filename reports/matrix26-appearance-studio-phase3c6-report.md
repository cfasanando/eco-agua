# Matrix26 Appearance Studio — Phase 3C.6 Report

## Delivered

- Added appearance as a first-class provisioning step.
- Added three provisioning presets:
  - Matrix26 Classic Business;
  - Matrix26 Nature Amazon;
  - Matrix26 Warm Restaurant.
- Added visual selection of public/admin themes and layouts.
- Added safe color and composition overrides.
- Added initial branding text fields.
- Added optional one-click demo visual assets.
- Stored appearance selections in the provisioning job.
- Added automatic schema migration for existing Matrix26 control databases.
- Added Dry Run validation for themes, layouts, compatibility and demo resources.
- Added target-local appearance configuration creation.
- Added isolated runtime asset publication.
- Added initial central appearance registration and history `v1`.
- Enabled later publication for any Matrix26-managed non-reserved instance.
- Updated generated runtime metadata and base settings with the selected branding.

## Provisioning sequence

```text
Validate identity
Create database
Install core
Create administrator
Install selected modules
Install initial appearance
Generate runtime
Register instance
Register appearance in Appearance Studio
Check portal availability
```

## Isolation

The feature does not reuse or modify protected business databases.

Generated visual assets are isolated under:

```text
runtime-data/<instance-code>/appearance/
```

## Data model

New provisioning metadata is stored in existing `matrix26_provisioning_job` through automatically managed columns:

- `appearance_preset_code`;
- `public_theme_code`;
- `public_layout_code`;
- `admin_theme_code`;
- `admin_layout_code`;
- `login_layout_code`;
- `appearance_overrides_json`;
- `branding_json`;
- `branding_demo_assets_enabled`.

No manual SQL is included or required.
