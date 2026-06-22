# Restaurant cash layout footer hotfix test guide

## Cause

The Phase 2P templates referenced:

```html
<div th:replace="fragments/footer_admin :: footer"></div>
```

The project does not contain `templates/fragments/footer_admin.html`.
Thymeleaf therefore interrupted the response after HTTP output had started, which produced
`ERR_INCOMPLETE_CHUNKED_ENCODING 200 (OK)` in the browser.

## Scope

The nonexistent footer reference was removed from:

- `cash_sessions.html`
- `cash_session_detail.html`
- `reports.html`

The existing admin head, sidebar, topbar and scripts remain unchanged.

## Test

1. Open `/admin/restaurant/cash-sessions`.
2. Confirm that sidebar and topbar are visible.
3. Open a cash session.
4. Confirm that the detail page loads.
5. Open `/admin/restaurant/reports`.
6. Confirm that the report page loads.

No SQL or Java changes are required.
