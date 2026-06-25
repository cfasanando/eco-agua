$ErrorActionPreference = "SilentlyContinue"

$candidates = @()
$candidates += (Get-Command mysqldump.exe).Source
$candidates += (Get-Command mariadb-dump.exe).Source
$candidates += @(
    "$env:ProgramFiles\MySQL\MySQL Server 8.4\bin\mysqldump.exe",
    "$env:ProgramFiles\MySQL\MySQL Server 8.0\bin\mysqldump.exe",
    "$env:ProgramFiles\MariaDB 11.4\bin\mariadb-dump.exe",
    "$env:ProgramFiles\MariaDB 10.11\bin\mariadb-dump.exe",
    "$env:ProgramFiles\MariaDB 10.6\bin\mysqldump.exe",
    "C:\xampp\mysql\bin\mysqldump.exe"
)

$found = $candidates |
    Where-Object { $_ -and (Test-Path $_ -PathType Leaf) } |
    Select-Object -Unique

if (-not $found) {
    Write-Host "No mysqldump or mariadb-dump executable was found." -ForegroundColor Yellow
    Write-Host "Install MySQL/MariaDB client tools or set MATRIX26_MYSQLDUMP_PATH manually."
    exit 1
}

foreach ($path in $found) {
    Write-Host "Found: $path" -ForegroundColor Green
    & $path --version
    Write-Host "Git Bash environment command:"
    Write-Host "export MATRIX26_MYSQLDUMP_PATH='$($path -replace '\\','/')'"
    Write-Host
}
