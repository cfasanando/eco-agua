# Matrix26 Phase 3B foreign-key order hotfix report

## Root cause

`Matrix26TargetDatabaseService.installCompatibleCore()` copied `SHOW CREATE TABLE`
definitions in alphabetical table order. MariaDB/MySQL requires a referenced table
to exist when a foreign-key constraint is created, even though foreign-key checking
is temporarily disabled in this environment.

For example:

```text
academy_assessment -> academy_course
```

Alphabetical processing attempted `academy_assessment` first.

## Fix

- Parse each `CREATE TABLE` definition.
- Separate foreign-key clauses from the initial table definition.
- Create all compatible tables first.
- Add foreign keys in a second pass.
- Skip constraints that already exist during a retry.
- Preserve indexes, unique keys, primary keys, checks and table options.

## Data impact

No existing operational database is modified by this hotfix.

The same partially created target database can be reused safely because:

- table creation uses `IF NOT EXISTS`;
- existing foreign keys are detected before `ALTER TABLE ... ADD`;
- completed provisioning steps are not repeated.
