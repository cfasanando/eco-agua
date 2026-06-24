# Matrix26 Phase 3B Windows runtime path hotfix

## Symptom

A generated runtime fails with:

```text
Config data resource ... /c/Users/.../application.properties does not exist
```

Git Bash exposes Windows paths as `/c/...`, while the Java process requires a
Windows-compatible file path.

## Correction

Generated launchers now:

1. Resolve the absolute configuration file.
2. Convert it using `cygpath -m` when running under Git Bash.
3. Pass the converted value to Spring Boot.

A repair helper is included for runtimes generated before this correction.

## Repair current runtime

```bash
bash scripts/repair-matrix26-runtime-launcher.sh matrix26_restaurant_lab
bash runtime-clients/matrix26_restaurant_lab/run.sh
```

The application should start on port `8093`.
