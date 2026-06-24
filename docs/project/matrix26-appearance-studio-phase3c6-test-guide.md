# Matrix26 Appearance Studio — Phase 3C.6 Test Guide

## Objective

Validate that a new Matrix26 instance is provisioned with its complete visual identity already published:

- theme;
- public layout;
- administrative layout;
- login layout;
- color overrides;
- branding texts;
- optional demo visual assets.

No second publication from Appearance Studio should be required after provisioning.

## Recommended test instance

```text
Business name: Matrix26 Appearance Laboratory
Business type: restaurant
Instance code: matrix26-appearance-lab
Database: matrix26_appearance_lab
Runtime: matrix26_appearance_lab
Port: 8094
URL: http://localhost:8094
Administrator: admin_demo
Module: Restaurant
Demo business data: enabled
```

## Recommended appearance preset

```text
Preset: Matrix26 Warm Restaurant
Public theme: Matrix26 Warm
Public layout: Public Restaurant Visual
Administrative theme: Matrix26 Classic
Administrative layout: Admin Compact Workspace
Login: Login Split
Demo visual assets: enabled
```

## Recommended branding

```text
Display name: Matrix26 Appearance Laboratory
Short name: Appearance Lab
Tagline: Sabores amazónicos con una identidad creada desde Matrix26
Welcome: Bienvenido al laboratorio visual de Matrix26.
Hero title: Una experiencia gastronómica con identidad propia
Hero subtitle: Theme, layout, branding y recursos publicados desde el primer arranque.
Primary CTA: Ver nuestra carta
Secondary CTA: Contactar
Phone: (065) 000000
WhatsApp: 51928527493
Location: Iquitos, Loreto
```

## Execution

1. Start Matrix26 on port `8091`.
2. Open `/control-center/provisioning/new`.
3. Enter the technical and business data.
4. Select **Matrix26 Warm Restaurant**.
5. Review the appearance fields.
6. Keep **Install demo visual kit** enabled.
7. Save the Dry Run.
8. Confirm that the plan contains:
   - Install initial appearance;
   - Generate runtime configuration;
   - Register appearance in Appearance Studio.
9. Execute the plan with explicit confirmation and an administrator password.
10. Start the generated runtime:

```bash
bash runtime-clients/matrix26_appearance_lab/run.sh
```

## Expected result

Open:

```text
http://localhost:8094/
http://localhost:8094/login
http://localhost:8094/admin/restaurant/dashboard
```

The first start must already show:

- custom display name;
- custom short name;
- Matrix26 Warm public theme;
- Public Restaurant Visual layout;
- Admin Compact Workspace;
- Login Split;
- selected colors;
- demo logo;
- demo favicon;
- demo login cover;
- demo hero;
- demo product placeholder.

## Matrix26 verification

Open the new instance in:

```text
/control-center/appearance/instances
```

Expected:

```text
PUBLISHED v1
Local synchronization: synchronized
No pending draft
```

The appearance history must contain the initial provisioning publication.

## Safety checks

Verify that these instances remain unchanged:

```text
8081 / eco_agua
8082 / productos_selva_belen
8084 / restaurante_buen_sabor
8093 / matrix26_restaurant_lab
```

No manual SQL is required.
