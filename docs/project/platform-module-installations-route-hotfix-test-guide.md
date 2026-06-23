# Platform module installations route hotfix

## Root cause

`SystemModuleAccessFilter` mapped `/admin/system-modules/**` to the
`platform_settings` feature flag.

When `platform_settings` was disabled, the filter returned HTTP 404 before
Spring MVC could invoke `SystemModuleAdminController`.

The controller route and template were present and valid.

## Correction

The `/admin/system-modules/**` route was removed from module feature filtering.

Access remains protected by Spring Security:

```java
.requestMatchers("/admin/system-modules/**", "/admin/dashboard-widgets/**")
.hasAnyAuthority(PLATFORM_ADMIN)
```

Therefore only authorized platform administrators can access module management.

## Test

1. Restart the application.
2. Open `/admin/system-modules`.
3. Open `/admin/system-modules/installations`.
4. Confirm that both pages load.
5. Confirm that a user without platform administration permissions receives 403 or is redirected according to security configuration.
6. Confirm that `/admin/platform/**` remains controlled by the `platform_settings` flag.

No SQL changes are required.
