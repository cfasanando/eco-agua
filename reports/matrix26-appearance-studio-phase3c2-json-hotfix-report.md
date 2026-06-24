# Matrix26 Appearance Studio 3C.2 JSON hotfix report

## Root cause

Phase 3C.2 used Jackson 2 package names (`com.fasterxml.jackson.*`). The project
build does not include those classes, causing compilation to fail.

## Fix

A small internal Java 17 JSON codec now handles the limited Matrix26 appearance
metadata structures. This avoids coupling the feature to an additional JSON
library and keeps the existing `pom.xml` unchanged.

## Data impact

None. No database schema or stored appearance data is modified.
