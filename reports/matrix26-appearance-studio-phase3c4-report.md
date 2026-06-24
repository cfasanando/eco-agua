# Matrix26 Appearance Studio — Phase 3C.4 report

## Observed issues

The first real publication exposed four visual quality problems:

1. Primary button text could use the same or an unreadable color as the button.
2. The split login left a large empty decorative block on wide screens.
3. The native color control did not allow direct hexadecimal entry.
4. Public layouts required additional responsive tuning.

## Corrections

- Added WCAG-style relative-luminance calculation on the server and in the editor.
- Added `--theme-on-primary`, hover, secondary and accent foreground tokens.
- Applied those tokens to Bootstrap primary/success buttons, badges and login CTA.
- Replaced the empty pseudo-panel with a semantic login brand panel.
- Added synchronized native picker + hexadecimal text fields.
- Added responsive refinements to all three public layouts.

## Safety

No business table, provisioning record or runtime identity is modified. Existing
appearance JSON remains compatible; foreground colors are derived at render time.
