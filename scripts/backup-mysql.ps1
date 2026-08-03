param(
    [string]$OutputDirectory = "./db_backups",
    [string]$Container = "innovative-ideas-challenge-mysql-1",
    [switch]$UploadToOss
)

$ErrorActionPreference = "Stop"
New-Item -ItemType Directory -Force -Path $OutputDirectory | Out-Null
$timestamp = Get-Date -Format "yyyyMMdd-HHmmss"
$output = Join-Path $OutputDirectory "elderly_medication-$timestamp.sql"

if (-not $env:MYSQL_ROOT_PASSWORD) {
    throw "MYSQL_ROOT_PASSWORD must be set before creating a backup"
}

docker exec $Container mysqldump --single-transaction --routines --triggers `
    -uroot "-p$env:MYSQL_ROOT_PASSWORD" elderly_medication | Out-File -Encoding utf8 $output

if (-not $env:BACKUP_ENCRYPTION_PASSWORD) {
    throw "BACKUP_ENCRYPTION_PASSWORD must be set; refusing to keep an unencrypted production backup"
}

$encrypted = "$output.7z"
$sevenZip = Get-Command 7z -ErrorAction SilentlyContinue
if (-not $sevenZip) { $sevenZip = Get-Command 7zz -ErrorAction SilentlyContinue }
if (-not $sevenZip) { throw "7z/7zz is required for encrypted backups" }
& $sevenZip.Source a -t7z -mhe=on "-p$env:BACKUP_ENCRYPTION_PASSWORD" $encrypted $output | Out-Null
if ($LASTEXITCODE -ne 0) { throw "Encrypted backup creation failed" }
Remove-Item -LiteralPath $output -Force
Write-Output "Encrypted backup created: $encrypted"

if ($UploadToOss) {
    if (-not $env:ALIYUN_OSS_BUCKET -or -not $env:ALIYUN_OSS_ENDPOINT) {
        throw "ALIYUN_OSS_BUCKET and ALIYUN_OSS_ENDPOINT are required for OSS upload"
    }
    $ossutil = Get-Command ossutil -ErrorAction SilentlyContinue
    if (-not $ossutil) { throw "ossutil is required for OSS upload" }
    & $ossutil.Source cp $encrypted "oss://$env:ALIYUN_OSS_BUCKET/backups/" --endpoint $env:ALIYUN_OSS_ENDPOINT
    if ($LASTEXITCODE -ne 0) { throw "OSS upload failed" }
    Write-Output "Backup uploaded to OSS"
}
