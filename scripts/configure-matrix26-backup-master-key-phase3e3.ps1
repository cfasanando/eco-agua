param(
    [switch]$ReplaceExisting
)

$ErrorActionPreference = "Stop"
$variableName = "MATRIX26_BACKUP_MASTER_KEY"
$existing = [Environment]::GetEnvironmentVariable($variableName, "User")

if ($existing -and -not $ReplaceExisting) {
    Write-Host "$variableName is already configured for the current Windows user."
    Write-Host "Use -ReplaceExisting only when you intentionally rotate the key."
    exit 0
}

$bytes = New-Object byte[] 32
$rng = [System.Security.Cryptography.RandomNumberGenerator]::Create()
try {
    $rng.GetBytes($bytes)
}
finally {
    $rng.Dispose()
}

$key = [Convert]::ToBase64String($bytes)
[Environment]::SetEnvironmentVariable($variableName, $key, "User")

Write-Host "A 256-bit Matrix26 backup master key was generated."
Write-Host "It was stored as a user environment variable, not in Git or application.properties."
Write-Host "Open a new Git Bash terminal before starting Matrix26."
Write-Host "Store the following recovery value in a password manager. It is shown only now:"
Write-Host $key
