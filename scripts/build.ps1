[CmdletBinding()]
param([string]$JdkRoot)
$ErrorActionPreference = 'Stop'
$taskRepoRoot = Split-Path -Parent $PSScriptRoot
$taskOutputRoot = Join-Path $taskRepoRoot 'out'
$taskCandidates = @()
if ($JdkRoot) { $taskCandidates += $JdkRoot }
if ($env:JAVA_HOME) { $taskCandidates += $env:JAVA_HOME }
$taskAncestor = $taskRepoRoot
while ($taskAncestor) {
    $taskPortable = Join-Path $taskAncestor '.tools\jdk-21'
    if (Test-Path -LiteralPath $taskPortable -PathType Container) {
        $taskCandidates += @(Get-ChildItem -LiteralPath $taskPortable -Directory | Sort-Object Name -Descending | Select-Object -ExpandProperty FullName)
    }
    $taskAncestor = Split-Path -Parent $taskAncestor
}
$taskCompilerOnPath = Get-Command javac.exe -CommandType Application -ErrorAction SilentlyContinue | Select-Object -First 1
if ($taskCompilerOnPath) { $taskCandidates += Split-Path -Parent (Split-Path -Parent $taskCompilerOnPath.Source) }
$taskJdkRoot = $taskCandidates | Where-Object {
    (Test-Path -LiteralPath (Join-Path $_ 'bin\javac.exe') -PathType Leaf) -and
    (Test-Path -LiteralPath (Join-Path $_ 'bin\java.exe') -PathType Leaf)
} | Select-Object -First 1
if (-not $taskJdkRoot) { throw 'JDK required: configure JAVA_HOME, PATH, or -JdkRoot.' }
$taskSources = @(Get-ChildItem -LiteralPath (Join-Path $taskRepoRoot 'src') -Recurse -File -Filter '*.java' | Sort-Object FullName | Select-Object -ExpandProperty FullName)
New-Item -ItemType Directory -Path $taskOutputRoot -Force | Out-Null
$taskCompiler = Join-Path $taskJdkRoot 'bin\javac.exe'
& $taskCompiler -encoding UTF-8 -d $taskOutputRoot @taskSources
if ($LASTEXITCODE -ne 0) { throw 'Game compilation failed.' }
Write-Host "Build succeeded: $($taskSources.Count) Java source files."
[PSCustomObject]@{ RepositoryPath=$taskRepoRoot; ClassDirectory=$taskOutputRoot; ResourceDirectory=(Join-Path $taskRepoRoot 'res'); JavaExecutable=(Join-Path $taskJdkRoot 'bin\java.exe'); CompilerExecutable=$taskCompiler }
