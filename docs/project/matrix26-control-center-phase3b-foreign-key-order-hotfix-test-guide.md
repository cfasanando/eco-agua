# Matrix26 Phase 3B foreign-key order hotfix

## Symptom

The provisioning workflow failed during **Install common core** with an error similar to:

```text
Failed to open the referenced table 'academy_course'
```

The source DDL for `academy_assessment` contains a foreign key to `academy_course`,
but the structural-copy routine processed tables alphabetically. Therefore,
`academy_assessment` was attempted before `academy_course`.

## Correction

The structural copy now runs in two phases:

1. Create every compatible table without foreign-key constraints.
2. Add the foreign-key constraints after all referenced tables exist.

The workflow also checks whether each constraint already exists before adding it,
which keeps retries idempotent when the target database already contains tables
created by a previous attempt.

## Recovery procedure

Do not delete `matrix26_restaurant_lab` and do not create a new Dry Run.

1. Stop Matrix26.
2. Apply this hotfix.
3. Rebuild and restart Matrix26.
4. Open the same failed provisioning plan.
5. Enter the same plan reference and administrator password.
6. Confirm the acknowledgement.
7. Click **Retry provisioning**.

Steps 1 and 2 remain completed. The workflow resumes from **Install common core**.

## Expected result

The core step should complete without dependency-order errors. Matrix26 will then
continue with administrator creation, Restaurant installation, runtime generation
and central registration.
