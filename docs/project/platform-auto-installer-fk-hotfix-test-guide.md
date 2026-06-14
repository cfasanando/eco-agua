# Platform auto installer FK/log hotfix - Test guide

1. Apply the hotfix file.
2. Compile the application.
3. Restart the main admin instance.
4. Open a client provisioning page.
5. Reset provisioning only if the client is already marked ready but target database is empty.
6. Run the automatic steps again: create database, copy structure, apply bootstrap, activate, generate runtime.
7. Confirm that the bootstrap step no longer fails with a foreign-key error on `platform_provisioning_log`.
