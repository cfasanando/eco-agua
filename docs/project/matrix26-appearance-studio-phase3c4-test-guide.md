# Matrix26 Appearance Studio — Phase 3C.4 test guide

## Scope

This phase improves visual quality, accessibility and responsive behavior after
publishing an appearance configuration.

Delivered:

- automatic readable foreground colors for primary buttons and badges;
- independent hover foreground calculation;
- editable hexadecimal inputs synchronized with native color pickers;
- live contrast hints in Appearance Studio;
- real content inside the split login brand panel;
- bounded and balanced widescreen login composition;
- mobile fallback to a centered login card;
- responsive polish for Classic Grid, Nature Editorial and Restaurant Visual;
- full-preview contrast variables aligned with runtime behavior.

## Apply

Stop Matrix26 and the laboratory portal, copy this package over the repository,
then rebuild:

```bash
rm -rf target
mvn clean -DskipTests package
```

Start Matrix26 and the laboratory again.

## Validation

### Button contrast

Publish a light primary color such as `#E8D86A` and a dark primary color such as
`#6244A7`. In both cases button text must remain readable without relying on hover.

### Hexadecimal editor

In Appearance Studio:

1. Type `#6244A7` directly.
2. Confirm that the native picker synchronizes.
3. Change the picker.
4. Confirm that the hexadecimal field updates in uppercase.

### Split login

Open the managed portal login at desktop widths. The left panel must contain:

- Matrix26 branding;
- business name;
- descriptive text;
- three feature rows;
- the configured background image under a readable overlay.

At widths below 900 px the brand panel must disappear and the login card must be
centered.

### Layout responsiveness

Check the public portal at 1920, 1366, 768 and 390 pixels. Cards, hero content and
CTA buttons must stay inside the viewport.
