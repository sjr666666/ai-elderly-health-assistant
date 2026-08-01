param(
    [Parameter(Mandatory = $true)][string]$BackupFile,
    [string]$Container = "innovative-ideas-challenge-mysql-1"
)

$ErrorActionPreference = "Stop"
if (-not (Test-Path -LiteralPath $BackupFile)) {
    throw "Backup file does not exist: $BackupFile"
}
if (-not $env:MYSQL_ROOT_PASSWORD) {
    throw "MYSQL_ROOT_PASSWORD must be set before restoring a backup"
}

$restoreFile = $BackupFile
$tempFile = $null
if ($BackupFile.EndsWith('.7z')) {
    if (-not $env:BACKUP_ENCRYPTION_PASSWORD) { throw "BACKUP_ENCRYPTION_PASSWORD must be set for encrypted restore" }
    $sevenZip = Get-Command 7z -ErrorAction SilentlyContinue
    if (-not $sevenZip) { $sevenZip = Get-Command 7zz -ErrorAction SilentlyContinue }
    if (-not $sevenZip) { throw "7z/7zz is required for encrypted restore" }
    $tempFile = Join-Path ([System.IO.Path]::GetTempPath()) ("restore-" + [guid]::NewGuid() + ".sql")
    & $sevenZip.Source e "-p$env:BACKUP_ENCRYPTION_PASSWORD" "-o$([System.IO.Path]::GetDirectoryName($tempFile))" $BackupFile | Out-Null
    if ($LASTEXITCODE -ne 0) { throw "Encrypted backup extraction failed" }
    $restoreFile = Get-ChildItem (Split-Path $tempFile) -Filter "*.sql" | Sort-Object LastWriteTime -Descending | Select-Object -First 1 -ExpandProperty FullName
}

Get-Content -LiteralPath $restoreFile -Raw | docker exec -i $Container mysql `
    -uroot "-p$env:MYSQL_ROOT_PASSWORD" elderly_medication
if ($tempFile -and (Test-Path $restoreFile)) { Remove-Item -LiteralPath $restoreFile -Force }
Write-Output "Backup restored: $BackupFile"
