# Matrix26 Appearance Studio Phase 3C.7 Report

## Objective

Stabilize the visual system after completing theme, layout, branding, asset and provisioning integration.

## Delivered changes

### Independent theme resolution

Public and admin CSS variables are now generated independently.

Previously, admin theme defaults could overwrite public theme defaults when both areas used different themes. The runtime now exposes:

- `appearancePublicCssVariables`
- `appearanceAdminCssVariables`

The legacy `appearanceCssVariables` attribute remains available for compatibility and points to the admin values.


### Explicit instance palette mode

The editor now distinguishes two intentional modes:

- Theme palette mode: public and admin themes keep their own catalog colors.
- Custom instance palette mode: one validated palette is shared by public, admin and login areas.

Saving a draft no longer serializes admin theme defaults as public overrides when the custom palette is disabled. New snapshots store an explicit `customPalette` flag. Legacy full palettes remain compatible, while the single primary color seeded in Phase 3C.1 is treated as a legacy accent rather than a complete shared palette when edited.

### Theme-native radius system

Border composition now supports an explicit `THEME` mode. In that mode, Classic, Nature and Warm keep separate small, medium and large radius scales instead of being flattened to one global value. Small, medium and large instance overrides remain available when a client needs a uniform composition.

### Semantic component system

The runtime bridge now preserves the meaning of Bootstrap component variants:

- Primary uses the configured primary color.
- Success uses the semantic success color.
- Warning uses the semantic warning color.
- Danger uses the semantic danger color.
- Information uses the semantic information color.

Primary, hover and outline states use readable foreground colors.

### Accessibility safeguards

Added:

- Visible `focus-visible` treatment.
- Reduced motion support.
- Readable disabled states.
- Improved form focus.
- Improved table and pagination states.
- Better muted text and border consistency.

### Branding asset preservation

Public hero overlays now preserve the uploaded hero image through `--appearance-hero-image`.

The theme gradient no longer replaces the branding asset.

### Restaurant public menu integration

The restaurant menu now uses:

- Published display name.
- Published tagline.
- Published logo.
- Published location.
- Published WhatsApp number.
- Published hero image.
- Published product placeholder.

Semantic classes were added for the restaurant hero, featured products, groups, menu items, images and prices.

### Layout stabilization

#### Admin Sidebar Classic

- Stable sidebar width.
- Responsive content padding.
- Stable topbar height.

#### Admin Compact Workspace

- Fixed rail footprint.
- Expanded sidebar overlays the workspace instead of shifting it.
- Labels remain hidden in rail mode.
- Nested menus appear only when the rail is expanded.
- Mobile behavior returns to a full sidebar.

#### Public Classic Grid

- Stable product grid.
- Predictable content width.
- Consistent hover and border states.

#### Public Nature Editorial

- Editorial typography.
- Scoped vertical rhythm.
- Decorative hero composition.
- Asymmetry disabled on mobile.

#### Public Restaurant Visual

- Strong restaurant hero.
- Featured product emphasis.
- Two-column large-screen menu.
- Mobile item composition.
- Prominent price treatment.

#### Login Split

- Better wide-screen proportions.
- Real information cards in the brand panel.
- Stable mobile fallback.
- Improved card and logo spacing.

### Appearance Quality Lab

Added:

```text
/control-center/appearance/quality-lab
```

The lab compares:

- All themes.
- All public layouts.
- All admin layouts.
- Login layout.
- Component states.
- Desktop, tablet and mobile widths.

The lab is read-only and does not publish changes.

## Database impact

None.

Catalog seed metadata is updated to version `1.1.0` through the existing idempotent initializer.

## Operational safety

This phase does not:

- modify protected operational databases;
- create or delete runtimes;
- publish automatically;
- alter sales, restaurant, inventory or accounting data.

## Validation performed

- Java structural checks.
- Independent palette serialization checks.
- JavaScript syntax check with Node.js.
- CSS brace validation.
- HTML tokenization.
- ZIP integrity validation.

A full Maven build must be confirmed in the project environment. The Maven wrapper could not fetch Apache Maven 3.9.11 from `repo.maven.apache.org` in the isolated environment.

## Recommended commit

```text
Polish Matrix26 themes and layouts
```
