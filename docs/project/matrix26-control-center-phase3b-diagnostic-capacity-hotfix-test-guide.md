# Matrix26 Phase 3B diagnostic capacity hotfix

## Symptom

A provisioning execution failed while saving the module diagnostic:

```text
Data too long for column 'detail'
```

The target database creation step had already completed. The diagnostic persistence
error then hid the original cause of the core installation failure.

## Correction

- `matrix26_provisioning_module.detail` now uses `TEXT`.
- Matrix26 automatically migrates an existing `VARCHAR(500)` column during startup.
- Module diagnostic messages are limited defensively.
- Audit summaries are limited to their declared capacity.
- Root-cause messages are preserved when available.

## Recovery

Do not delete the target database and do not create another Dry Run.

1. Stop Matrix26.
2. Apply the hotfix.
3. Build and start Matrix26.
4. Open the same failed plan.
5. Enter the same reference and the administrator password again.
6. Confirm the acknowledgement.
7. Click **Reintentar aprovisionamiento**.

Completed steps remain completed. The workflow resumes from the failed step.
