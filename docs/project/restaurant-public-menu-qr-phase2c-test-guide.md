# Restaurant Phase 2C - Public menu and table QR test guide

## Goal

Validate that each restaurant table has a public menu link and QR card. The public menu should identify the table when `tableId` is present.

## Steps

1. Build the project with `mvn clean -DskipTests package`.
2. Start the restaurant runtime on port 8084.
3. Log in as `admin_demo`.
4. Open `/admin/restaurant/tables`.
5. Confirm each table shows a QR image, a public link, and a copy button.
6. Open one table menu link in a new tab.
7. Confirm `/restaurant/menu?tableId=<id>` shows the selected table context.
8. Open `/admin/restaurant/tables/qr-cards`.
9. Confirm the print view shows one QR card per table.
10. Test the WhatsApp attention button from the public menu.

## Expected result

- Table QR links point to the current runtime host, for example `http://localhost:8084/restaurant/menu?tableId=1`.
- Public menu remains accessible without login.
- Admin table status workflow continues working.
- Existing comanda flow remains unchanged.
