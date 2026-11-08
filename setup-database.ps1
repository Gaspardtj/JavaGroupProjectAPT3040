param([string]$PostgresBin = 'C:/Program Files/PostgreSQL/18/bin')
$ErrorActionPreference = 'Stop'
Push-Location $PSScriptRoot
$oldPassword = $env:PGPASSWORD
try {
    $line = Get-Content postgres-admin.local.properties | Where-Object { $_ -match '^password=' } | Select-Object -First 1
    if (!$line -or $line -eq 'password=') { throw 'Enter your postgres password in postgres-admin.local.properties first.' }
    $env:PGPASSWORD = $line.Substring(9)
    $psql = Join-Path $PostgresBin psql.exe
    function Invoke-Sql([string]$Sql, [string]$Db = 'postgres', [string]$User = 'postgres') {
        $result = $Sql | & $psql -X -h localhost -p 5432 -U $User -d $Db -w -v ON_ERROR_STOP=1 -t -A
        if ($LASTEXITCODE -ne 0) { throw 'PostgreSQL setup failed.' }
        return $result
    }
    if (Test-Path database.local.properties) {
        $appLine = Get-Content database.local.properties | Where-Object { $_ -match '^db.password=' } | Select-Object -First 1
        if (!$appLine) { throw 'Missing db.password in database.local.properties.' }
        $appPassword = $appLine.Substring(12)
    } else {
        $appPassword = [guid]::NewGuid().ToString('N') + [guid]::NewGuid().ToString('N')
        @("db.url=jdbc:postgresql://localhost:5432/library_management_system", 'db.user=library_app', "db.password=$appPassword") | Set-Content database.local.properties
    }
    $roleExists = Invoke-Sql "SELECT 1 FROM pg_roles WHERE rolname='library_app'"
    if ($roleExists -ne '1') {
        $escaped = $appPassword.Replace("'", "''")
        Invoke-Sql "CREATE ROLE library_app LOGIN PASSWORD '$escaped'" | Out-Null
    }
    $dbExists = Invoke-Sql "SELECT 1 FROM pg_database WHERE datname='library_management_system'"
    if ($dbExists -ne '1') { Invoke-Sql 'CREATE DATABASE library_management_system OWNER library_app' | Out-Null }
    $env:PGPASSWORD = $appPassword
    Invoke-Sql (Get-Content schema.sql -Raw) 'library_management_system' 'library_app' | Out-Null
    Write-Host 'Library database and schema are ready.'
    Set-Content postgres-admin.local.properties '# Setup complete. Administrator password removed.'
} finally {
    $env:PGPASSWORD = $oldPassword
    Pop-Location
}
