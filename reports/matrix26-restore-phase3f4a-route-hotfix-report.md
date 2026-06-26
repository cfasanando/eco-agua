# Matrix26 Restore Manager Phase 3F.4a

## Defect

`/control-center/restores/in-place` was accepted by the generic route
`/control-center/restores/{id}`. Spring attempted to bind `in-place` to a
`long`, causing `MethodArgumentTypeMismatchException` and HTTP 400.

## Correction

Generic restore identifiers now use numeric path constraints:

```java
@GetMapping("/{id:\\d+}")
```

The same restriction is applied to verification IDs, cleanup plan IDs, and
in-place job IDs.

## Scope

- No schema changes.
- No runtime-data changes.
- No backup changes.
- No security configuration changes.
- No destructive operation changes.

## Validation

- Controlled Java 17 compilation of both modified controllers: passed.
- Static route checker: passed.
