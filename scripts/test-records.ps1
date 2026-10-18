[CmdletBinding()]
param([string]$JdkRoot)
$ErrorActionPreference = 'Stop'
$taskBuild = & (Join-Path $PSScriptRoot 'build.ps1') -JdkRoot $JdkRoot
$taskClasspath = $taskBuild.ClassDirectory + [IO.Path]::PathSeparator + $taskBuild.ResourceDirectory
$taskTest = @(
    (Join-Path $taskBuild.RepositoryPath 'tests\records\RecordsSmokeTest.java'),
    (Join-Path $taskBuild.RepositoryPath 'tests\records\GameplayIntegrationTest.java'),
    (Join-Path $taskBuild.RepositoryPath 'tests\screen\GameScreenWavesTest.java')
)
& $taskBuild.CompilerExecutable -encoding UTF-8 -cp $taskClasspath -d $taskBuild.ClassDirectory @taskTest
if ($LASTEXITCODE -ne 0) { throw 'Records checks compilation failed.' }
Push-Location -LiteralPath $taskBuild.RepositoryPath
try {
    & $taskBuild.JavaExecutable '-Djava.awt.headless=true' -ea -cp $taskClasspath records.RecordsSmokeTest
    if ($LASTEXITCODE -ne 0) { throw 'Records checks failed.' }
    foreach ($taskSuite in @('records.GameplayIntegrationTest', 'screen.GameScreenWavesTest')) {
        & $taskBuild.JavaExecutable '-Djava.awt.headless=true' -ea -cp $taskClasspath $taskSuite
        if ($LASTEXITCODE -ne 0) { throw "$taskSuite failed." }
    }
} finally { Pop-Location }
