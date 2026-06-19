# Test guide - Platform provisioning refresh hotfix

## Goal

Confirm that the provisioning page updates immediately after every automatic step.

## Steps

1. Apply the files from this ZIP.
2. Compile the app.
3. Restart the main platform on port 8081.
4. Open the provisioning screen for `Restaurante El Buen Sabor`.
5. Run the automatic provisioning steps one by one.

## Expected checks

- After creating the database, step 2 becomes available without manual refresh.
- After copying structure, step 3 becomes available without manual refresh.
- After applying bootstrap, step 4 becomes available without manual refresh.
- After loading demo data, step 5 becomes available without manual refresh.
- After activating the business, step 6 becomes available without manual refresh.
- After generating runtime files, the generated paths are shown without manual refresh.
- While a step is running, the clicked button shows loading text and cannot be clicked again.

## Suggested restaurant flow after this fix

Use the restaurant instance on port 8084. Do not run provisioning actions on protected instances such as Eco Agua 8081 or Productos de la Selva 8082.
