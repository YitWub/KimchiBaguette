[CmdletBinding()]
param([string]$JdkRoot)
$ErrorActionPreference = 'Stop'
$taskBuild = & (Join-Path $PSScriptRoot 'build.ps1') -JdkRoot $JdkRoot
$taskClasspath = $taskBuild.ClassDirectory + [IO.Path]::PathSeparator + $taskBuild.ResourceDirectory
Push-Location -LiteralPath $taskBuild.RepositoryPath
try {
    & $taskBuild.JavaExecutable -cp $taskClasspath engine.Core
    if ($LASTEXITCODE -ne 0) { throw 'Game launch failed.' }
} finally { Pop-Location }
