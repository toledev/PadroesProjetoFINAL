$ErrorActionPreference = 'Stop'
$OutputEncoding = [System.Text.UTF8Encoding]::new($false)
[Console]::OutputEncoding = [System.Text.UTF8Encoding]::new($false)
Set-Location -LiteralPath $PSScriptRoot
$sources = @(Get-ChildItem -Path 'src/main/java','src/test/java' -Filter '*.java' -Recurse | ForEach-Object FullName)
New-Item -ItemType Directory -Path 'build/test-classes' -Force | Out-Null
& javac -encoding UTF-8 --release 17 -d build/test-classes $sources
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
& java '-Dfile.encoding=UTF-8' -cp build/test-classes br.com.assistencia.TestesNegocio
exit $LASTEXITCODE
