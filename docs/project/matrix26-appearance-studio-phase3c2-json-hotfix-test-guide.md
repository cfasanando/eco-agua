# Matrix26 Appearance Studio 3C.2 JSON hotfix

## Symptom

Compilation failed because `Matrix26AppearanceEditorService` imported Jackson 2:

```text
package com.fasterxml.jackson.databind does not exist
```

The current Spring Boot 4 project does not expose those Jackson 2 packages.

## Correction

- Removed the Jackson 2 imports and `ObjectMapper` constructor dependency.
- Added `Matrix26JsonCodec`, implemented only with Java 17.
- Preserved JSON support for:
  - theme tokens;
  - layout configuration;
  - appearance overrides;
  - nested appearance history snapshots.
- No Maven dependency was added.

## Test

```bash
rm -rf target
mvn clean -DskipTests package
```

Expected:

```text
BUILD SUCCESS
```

Then start Matrix26 and test the editor and draft workflow normally.
