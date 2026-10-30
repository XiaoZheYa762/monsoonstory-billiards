$ErrorActionPreference = 'Stop'
$repoRoot = Split-Path -Parent $PSScriptRoot
$classes = Join-Path $repoRoot 'build/music-test-classes'
New-Item -ItemType Directory -Force -Path $classes | Out-Null
$sources = @(Get-ChildItem -LiteralPath (Join-Path $repoRoot 'common/src/main/java/dev/monsoonstory/billiards/music') -Filter '*.java' | ForEach-Object FullName)
$testSource = Join-Path $repoRoot 'common/src/test/java/dev/monsoonstory/billiards/music/MusicIntegrationVerification.java'
& javac -encoding UTF-8 --release 17 -d $classes $sources $testSource
if ($LASTEXITCODE -ne 0) { throw 'Music test compilation failed.' }
& java -cp $classes dev.monsoonstory.billiards.music.MusicIntegrationVerification $repoRoot
if ($LASTEXITCODE -ne 0) { throw 'Music integration verification failed.' }
