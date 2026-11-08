$ErrorActionPreference = 'Stop'
Push-Location $PSScriptRoot
try {
    ./run.ps1 -BuildOnly
    & javac -encoding UTF-8 --release 11 -cp build/classes -d build/test-classes tests/IntegrationTest.java tests/CirculationTest.java
    if ($LASTEXITCODE -ne 0) { throw 'Test compilation failed.' }
    & java '-Dfile.encoding=UTF-8' -cp 'build/classes;build/test-classes;lib/postgresql-42.7.14.jar' IntegrationTest
    if ($LASTEXITCODE -ne 0) { throw 'Integration tests failed.' }
    & java '-Dfile.encoding=UTF-8' -cp 'build/classes;build/test-classes;lib/postgresql-42.7.14.jar' CirculationTest
    if ($LASTEXITCODE -ne 0) { throw 'Circulation tests failed.' }
} finally { Pop-Location }
