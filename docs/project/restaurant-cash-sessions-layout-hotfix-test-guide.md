# Restaurant cash sessions layout hotfix test guide

1. Stop the restaurant runtime.
2. Replace only `cash_sessions.html` with the file included in this package.
3. Run `mvn clean -DskipTests package`.
4. Start the restaurant runtime on port 8084.
5. Open `/admin/restaurant/cash-sessions`.
6. Confirm the standard sidebar and topbar are visible.
7. Confirm the page still renders the open-cash form and session history.
8. Open a cash session and verify navigation remains inside the normal admin layout.
