param([Parameter(Mandatory=$true)][ValidateSet('server','client')][string]$Side,[switch]$Listener,[switch]$Pair)
$ErrorActionPreference = 'Stop'
$neoRoot = Join-Path (Split-Path -Parent $PSScriptRoot) 'neoforge'
$version = ((Get-Content -LiteralPath (Join-Path $neoRoot 'gradle.properties') | Where-Object { $_ -match '^mod_version=' }) -split '=',2)[1]
$jar = Join-Path $neoRoot "build/libs/monsoonstory_billiards-neoforge-1.21.1-$version.jar"
if (!(Test-Path -LiteralPath $jar)) { throw 'Build the NeoForge release JAR first.' }
$prefix = if ($Side -eq 'server') { 'serverSmoke' } else { 'clientSmoke' }
$argRoot = Join-Path $neoRoot 'build/moddev'
$programArgs = Join-Path $argRoot ($prefix + 'RunProgramArgs.txt')
if ($Listener) {
    $programArgs = Join-Path $argRoot 'listenerSmokeRunProgramArgs.txt'
    $template = [IO.File]::ReadAllText((Join-Path $argRoot 'clientSmokeRunProgramArgs.txt'),[Text.Encoding]::Default)
    $template = $template.Replace('NeoBilliardsQA','NeoBilliardsView').Replace('00000000000000000000000000000078','00000000000000000000000000000079')
    [IO.File]::WriteAllText($programArgs,$template,[Text.Encoding]::Default)
}
$folders = '-Dfml.modFolders=monsoonstory_billiards%%' + $jar + ';billiards_verification%%' + (Join-Path $neoRoot 'build/classes/java/verification') + ';billiards_verification%%' + (Join-Path $neoRoot 'build/resources/verification')
$hashProperty = '-Dbilliards.testJarSha=' + (Get-FileHash -LiteralPath $jar -Algorithm SHA256).Hash.ToLowerInvariant()
$launchArgs = @(
    ('@' + (Join-Path $argRoot ($prefix + 'RunClasspath.txt'))),
    ('@' + (Join-Path $argRoot ($prefix + 'RunVmArgs.txt'))),
    $folders,
    $hashProperty
)
if ($Listener) { $launchArgs += '-Dbilliards.smokeListener=true' }
if ($Pair -or $Listener) { $launchArgs += '-Dbilliards.smokePair=true' }
$launchArgs += @('net.neoforged.devlaunch.Main',('@' + $programArgs))
if ($Side -eq 'client') { $launchArgs += @('--width','1280','--height','720') }
$directory = if ($Listener) { 'run/smoke-listener' } else { 'run/smoke-' + $Side }
Set-Location -LiteralPath (Join-Path $neoRoot $directory)
& java $launchArgs
exit $LASTEXITCODE
