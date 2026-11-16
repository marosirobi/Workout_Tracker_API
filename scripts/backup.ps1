$ErrorActionPreference = "Stop"

$projectDir = Split-Path $PSScriptRoot -Parent
$backupDir = "H:\backups\workouttracker"
$keepDays = 14

function Invoke-Checked([scriptblock]$command) {
    & $command
    if ($LASTEXITCODE -ne 0) { throw "Command failed: $command" }
}

New-Item -ItemType Directory -Force -Path $backupDir | Out-Null
$file = Join-Path $backupDir ("backup-{0}.dump" -f (Get-Date -Format "yyyy-MM-dd"))

Set-Location $projectDir
Invoke-Checked { docker compose exec -T db sh -c 'pg_dump -U "$POSTGRES_USER" -d "$POSTGRES_DB" -Fc -f /tmp/backup.dump' }
Invoke-Checked { docker compose cp db:/tmp/backup.dump $file }
Invoke-Checked { docker compose exec -T db rm /tmp/backup.dump }

Get-ChildItem $backupDir -Filter "backup-*.dump" |
    Where-Object { $_.LastWriteTime -lt (Get-Date).AddDays(-$keepDays) } |
    Remove-Item

Write-Output "Backup saved: $file"
