# Matrix26 Appearance Studio — Phase 3C.2 report

## Delivered

- Per-instance appearance editor.
- Visual theme selectors.
- Public, administrative and login layout selectors.
- Controlled color overrides.
- Controlled sidebar, radius, density, width and heading options.
- Live public and administrative previews.
- Full-screen public, backoffice and login preview.
- Draft persistence isolated in Matrix26.
- Draft counter in Appearance Studio.
- Draft history and discard workflow.
- Theme/layout compatibility validation.
- WCAG-oriented contrast validation.
- No free CSS or JavaScript fields.

## New table

```text
matrix26_instance_appearance_draft
```

The table is created idempotently by `Matrix26ControlCenterInitializer`.

## New routes

```text
GET  /control-center/instances/{id}/appearance/edit
POST /control-center/instances/{id}/appearance/preview
GET  /control-center/instances/{id}/appearance/preview
POST /control-center/instances/{id}/appearance/draft
POST /control-center/instances/{id}/appearance/draft/discard
```

## Data isolation

The phase does not publish to managed instances. All writes remain in:

```text
matrix26_platform_control
```

The following operational databases are not modified:

```text
eco_agua
productos_selva_belen
restaurante_buen_sabor
matrix26_restaurant_lab
```

## Verification performed

- New Java sources compiled successfully against generated dependency stubs with Java 17.
- Changed Java files passed brace and type-signature checks.
- HTML templates were parsed successfully.
- ZIP integrity was verified.

A complete Maven build must still be run in the project environment because the
artifact environment cannot download the Maven distribution.

## Next phase

Phase 3C.3 will publish an approved draft to the laboratory instance, persist the
active configuration locally, expose runtime theme/layout resolvers and implement
rollback without touching business data.
