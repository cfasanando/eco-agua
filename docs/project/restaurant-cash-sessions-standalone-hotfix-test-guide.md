# Restaurant cash sessions standalone hotfix test guide

1. Stop the restaurant runtime.
2. Copy only `cash_sessions.html`.
3. Rebuild and start port 8084.
4. Open `/admin/restaurant/cash-sessions`.
5. Confirm the page renders completely without `ERR_INCOMPLETE_CHUNKED_ENCODING`.
6. Open a cash session and verify the redirect to the detail page.

This hotfix intentionally removes shared Thymeleaf layout fragments from the list page to isolate it from the fragment rendering chain. It does not change Java code, SQL, tables, or business data.
