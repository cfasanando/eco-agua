# Matrix26 Appearance Studio — Phase 3C.5 test guide

## Goal

Validate branding text, isolated image assets, one-click demo content, publication,
local runtime loading and rollback without searching for external resources.

## Delivered demo kit

Matrix26 includes a complete test kit under:

```text
sample-data/matrix26-branding-demo/
```

The same resources are embedded in the application for the **Load complete demo kit**
button.

| Asset | File | Dimensions |
|---|---|---:|
| Primary logo | `logo-primary.png` | 600 × 200 |
| Compact logo | `logo-compact.png` | 256 × 256 |
| Favicon | `favicon.png` | 64 × 64 |
| Login cover | `login-cover.jpg` | 1400 × 1000 |
| Primary hero | `hero-primary.jpg` | 1600 × 900 |
| Secondary hero | `hero-secondary.jpg` | 1200 × 800 |
| Product placeholder | `product-placeholder.png` | 800 × 800 |
| Social share image | `social-share.jpg` | 1200 × 630 |

## Apply

Stop Matrix26 and the laboratory runtime, copy the package over the project root,
build and restart both applications.

```bash
rm -rf target
mvn clean -DskipTests package
bash scripts/run-matrix26-control.sh
```

In a second terminal:

```bash
bash runtime-clients/matrix26_restaurant_lab/run.sh
```

## One-click test

1. Open Matrix26:
   `http://localhost:8091/control-center/appearance/instances`
2. Open **Matrix26 Restaurant Laboratory**.
3. Click **Branding and resources**.
4. Click **Load complete demo kit**.
5. Confirm that eight preview cards contain images.
6. Return to the appearance detail.
7. Confirm that **Branding pending** is visible.
8. Publish using the instance code:
   `matrix26-restaurant-lab`.

## Expected runtime result

Reload:

- `http://localhost:8093/`
- `http://localhost:8093/login`
- `http://localhost:8093/admin/restaurant/dashboard`

Expected:

- the public header uses the new primary logo;
- the public hero uses the included banner and texts;
- the login uses the included cover and compact logo;
- the admin sidebar uses the compact logo and short name;
- the favicon is served from the local runtime;
- the portal continues working when Matrix26 is stopped.

## Manual upload test

Use files from `sample-data/matrix26-branding-demo/` to replace one or more assets.

Matrix26 validates:

- real file signature;
- allowed extension;
- maximum file size;
- minimum dimensions for PNG and JPEG;
- safe generated destination paths;
- isolation by instance code.

## Storage

Draft assets:

```text
runtime-data/matrix26-control/drafts/<instance-code>/branding/
```

Published assets:

```text
runtime-data/<instance-code>/appearance/current/
runtime-data/<instance-code>/appearance/history/v<version>/
```

The folder `runtime-data/` remains ignored by Git.

## Rollback test

Restore a previous published version from Matrix26. Theme, layout, branding text and
assets must be restored together as a new version.
