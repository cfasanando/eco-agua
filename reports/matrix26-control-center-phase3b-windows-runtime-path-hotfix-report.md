# Matrix26 Phase 3B Windows runtime path hotfix report

## Root cause

The generated `run.sh` passed the Git Bash path `/c/Users/...` directly to
`spring.config.additional-location`. Java/Spring interpreted it as a malformed
Windows file path.

## Fix

- Added `cygpath -m` conversion to generated runtime launchers.
- Added existence validation for `application.properties`.
- Added a reusable repair script for previously generated runtimes.

## Data impact

None. No database or provisioning record is modified.
