# Matrix26 Appearance Studio Phase 3C.7 Test Guide

## Scope

This phase performs the first complete visual stabilization pass over Matrix26 themes and layouts.

It validates:

- independent public and admin theme resolution;
- semantic button colors and automatic primary contrast;
- public hero and branding asset rendering;
- product placeholder rendering in the restaurant menu;
- stable classic and compact admin layouts;
- public Classic Grid, Nature Editorial and Restaurant Visual layouts;
- Login Split behavior;
- desktop, tablet and mobile previews;
- publication, persistence and rollback.

No operational database is modified by this phase.

## Build

From the project root:

```bash
rm -rf target
mvn clean -DskipTests package
```

Expected result:

```text
BUILD SUCCESS
```

## Start the portals

Terminal 1:

```bash
bash scripts/run-matrix26-control.sh
```

Terminal 2:

```bash
bash runtime-clients/matrix26_appearance_lab/run.sh
```

Optional comparison laboratory:

```bash
bash runtime-clients/matrix26_restaurant_lab/run.sh
```

Expected ports:

- Matrix26 Control Center: `8091`
- Matrix26 Restaurant Laboratory: `8093`
- Matrix26 Appearance Laboratory: `8094`

## Quality Lab

Open:

```text
http://localhost:8091/control-center/appearance/quality-lab
```

Validate the following controls:

- Independent public theme selector.
- Independent admin and login theme selector.
- Public layout selector.
- Admin layout selector.
- Login layout selector.
- Desktop viewport.
- Tablet viewport.
- Mobile viewport.

The page must not publish or modify any instance.

## Theme matrix

Review all themes:

1. Matrix26 Classic.
2. Matrix26 Nature.
3. Matrix26 Warm.

For every theme verify:

- Primary button text is readable before hover.
- Primary button text remains readable on hover.
- Success, warning, danger and information colors remain semantic.
- Outline buttons preserve readable text and border contrast.
- Focus indicators are visible with keyboard navigation.
- Disabled controls are visibly disabled.
- Cards, form controls and muted text remain readable.
- `Borders: According to theme` preserves the distinct Classic, Nature and Warm radius systems.

## Public layout matrix

Review every public layout with every theme:

| Layout | Classic | Nature | Warm |
|---|---:|---:|---:|
| Public Classic Grid | Yes | Yes | Yes |
| Public Nature Editorial | Yes | Yes | Yes |
| Public Restaurant Visual | Yes | Yes | Yes |

Validate:

- Header and navigation remain inside the viewport.
- Hero titles do not overflow.
- Hero branding images remain visible under the theme overlay.
- Product cards remain aligned.
- Mobile buttons do not overflow.
- Restaurant menu prices remain visually prominent.
- Product placeholder is used when a menu item has no image.
- Nature Editorial does not offset cards on mobile.
- Restaurant Visual uses two product columns on large screens and one readable composition on mobile.

## Admin layout matrix

Review:

| Layout | Classic | Nature | Warm |
|---|---:|---:|---:|
| Admin Sidebar Classic | Yes | Yes | Yes |
| Admin Compact Workspace | Yes | Yes | Yes |

Validate:

- The active page is visible.
- First-level and nested menus are distinguishable.
- The compact sidebar expands over the workspace without moving the content.
- Menu labels remain hidden in rail mode.
- Browser tooltips identify compact menu items.
- Nested menus open after expanding or focusing the sidebar.
- Tables remain usable at 1920, 1366, 1024, 768 and 390 pixels.
- Topbar content does not overlap.

## Login validation

Review Login Split at:

- 1920 × 1080.
- 1366 × 768.
- 768 × 1024.
- 390 × 844.

Validate:

- The brand panel contains actual content.
- The login form remains balanced on wide displays.
- The brand panel disappears below 900 pixels.
- The form remains centered on tablet and mobile.
- The logo does not overlap the title.
- The primary button text is readable.
- Error and success messages fit inside the card.

## Real publication test

Use `matrix26-appearance-lab` only.

### Test A: independent theme palettes

```text
Public theme: Matrix26 Nature
Public layout: Public Nature Editorial
Admin theme: Matrix26 Classic
Admin layout: Admin Compact Workspace
Login layout: Login Split
Custom instance palette: Disabled
Borders: According to theme
```

Expected result:

- Public pages keep Matrix26 Nature colors.
- Admin and login pages keep Matrix26 Classic colors.
- Saving the draft does not copy Classic colors into the public portal.

### Test B: intentional shared custom palette

Enable `Custom instance palette` and use:

```text
Primary: #6244A7
Secondary: #3F2B63
Accent: #D69A36
Background: #F7F4FC
Surface: #FFFFFF
Text: #2E2340
```

Expected result:

- Public, admin and login areas use the same instance palette.
- Button text remains readable before and during hover.
- Disabling the switch restores each theme's original palette after publication.

Publish and verify:

```text
http://localhost:8094/
http://localhost:8094/login
http://localhost:8094/admin/restaurant/dashboard
http://localhost:8094/restaurant/menu
```

Important regression check:

- With the custom palette disabled, the public portal must use Nature defaults.
- With the custom palette disabled, the admin portal must use Classic defaults.
- Selecting or saving a different admin theme must not overwrite public theme defaults.
- With the custom palette enabled, the shared palette must be intentional and versioned.
- With border mode set to theme, public and admin areas keep the selected theme radius scale.

## Contrast tests

Dark primary:

```text
#6244A7
```

Expected button text:

```text
White
```

Light primary:

```text
#E8D86A
```

Expected button text:

```text
Dark
```

Test both normal and hover states.

## Branding assets

Use the existing demo kit:

```text
sample-data/matrix26-branding-demo/
```

Validate:

- Primary logo.
- Compact logo.
- Favicon.
- Login cover.
- Primary hero.
- Secondary hero.
- Product placeholder.
- Social sharing image.

The public hero must preserve the selected image. The theme must add an overlay instead of replacing the image.

## Persistence test

1. Publish the visual draft.
2. Stop Matrix26 on port `8091`.
3. Keep the instance on `8094` running.
4. Reload public, login and admin pages.

Expected result:

- Appearance remains active.
- Branding assets remain available.
- No request to Matrix26 is required.

## Rollback test

1. Start Matrix26 again.
2. Restore the previous published version.
3. Reload the instance after two seconds.

Expected result:

- Theme, layouts, tokens, branding texts and assets return together.
- A new history version is created.
- Previous history is preserved.

## Protected instance safety

Do not publish during this phase to:

- `eco_agua` on `8081`.
- `productos_selva_belen` on `8082`.
- `restaurante_buen_sabor` on `8084`.

Use only the Matrix26 laboratories.

## Static verification

Run:

```bash
bash scripts/check-matrix26-appearance-phase3c7.sh
```

## Acceptance result

The phase is accepted when:

- Maven build succeeds.
- Quality Lab loads.
- All theme and layout selectors work.
- Public and admin themes resolve independently.
- The custom palette switch cleanly alternates between independent theme defaults and intentional shared colors.
- Primary button contrast works with dark and light colors.
- Hero assets remain visible.
- Restaurant placeholder works.
- Compact sidebar does not shift the workspace.
- Login is balanced on wide and mobile displays.
- Publication, persistence and rollback pass.
