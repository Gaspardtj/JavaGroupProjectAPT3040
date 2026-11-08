param([switch]$BuildOnly, [switch]$Console)
$ErrorActionPreference = 'Stop'
Push-Location $PSScriptRoot
try {
    $driver = 'lib/postgresql-42.7.14.jar'
    New-Item -ItemType Directory -Force lib,build/classes | Out-Null
    if (!(Test-Path $driver)) {
        Invoke-WebRequest 'https://jdbc.postgresql.org/download/postgresql-42.7.14.jar' -OutFile $driver
    }
    # Use .NET directly so checksum verification also works without Get-FileHash.
    $sha256 = [System.Security.Cryptography.SHA256]::Create()
    $driverStream = $null
    try {
        $driverStream = [System.IO.File]::OpenRead((Join-Path $PSScriptRoot $driver))
        $driverHash = [System.BitConverter]::ToString($sha256.ComputeHash($driverStream)).Replace('-', '')
    } finally {
        if ($null -ne $driverStream) { $driverStream.Dispose() }
        $sha256.Dispose()
    }
    if ($driverHash -ne '73914527305A40CCE504B0D3D90B23CAF565136912D607AE8AE7C5895512332C') {
        throw 'PostgreSQL driver checksum mismatch.'
    }
    $sources = @(Get-ChildItem GroupProject/src -Recurse -Filter *.java | ForEach-Object { $_.FullName })
    & javac -encoding UTF-8 --release 11 -d build/classes @sources
    if ($LASTEXITCODE -ne 0) { throw 'Java compilation failed.' }
    & jar --create --file build/library-management.jar --main-class library.ui.LibraryApp -C build/classes .
    if ($LASTEXITCODE -ne 0) { throw 'JAR packaging failed.' }
    if (!$BuildOnly) {
        $entryPoint = if ($Console) { 'library.main.Main' } else { 'library.ui.LibraryApp' }
        & java '-Dfile.encoding=UTF-8' -cp "build/library-management.jar;$driver" $entryPoint
        if ($LASTEXITCODE -ne 0) { throw 'Library application failed.' }
    }
} finally { Pop-Location }
