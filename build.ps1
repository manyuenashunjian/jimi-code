[CmdletBinding()]
param(
    [Parameter(ValueFromRemainingArguments = $true)]
    [string[]] $MavenArgs
)

# NOTE: intentionally NOT "$ErrorActionPreference = 'Stop'". Under Windows
# PowerShell 5.1, EAP=Stop turns native-command stderr output (e.g. `java -version`
# printing to stderr) into a terminating NativeCommandError, aborting the build.
# Failures are still detected via $LASTEXITCODE and `throw` below.
$ErrorActionPreference = "Continue"

function Get-JavaMajorVersion([string] $JavaHome) {
    if ([string]::IsNullOrWhiteSpace($JavaHome)) {
        return 0
    }

    $java = Join-Path $JavaHome "bin\java.exe"
    if (-not (Test-Path -LiteralPath $java -PathType Leaf)) {
        return 0
    }

    $versionOutput = (& $java -version 2>&1 | Out-String)
    if ($LASTEXITCODE -ne 0 -or $versionOutput -notmatch 'version "(?<major>\d+)(?:\.(?<minor>\d+))?') {
        return 0
    }

    $major = [int] $Matches.major
    if ($major -eq 1 -and $Matches.minor) {
        return [int] $Matches.minor
    }
    return $major
}

$candidates = [System.Collections.Generic.List[string]]::new()
foreach ($candidate in @(
    $env:MEWCODE_JAVA_HOME,
    $env:JAVA_HOME,
    "E:\codesoft\Java\jdk-21",
    "C:\Program Files\Java\jdk-21",
    "C:\Program Files\Microsoft\jdk-21"
)) {
    if (-not [string]::IsNullOrWhiteSpace($candidate)) {
        $candidates.Add($candidate)
    }
}

foreach ($root in @(
    "E:\codesoft\Java",
    "C:\Program Files\Java",
    "C:\Program Files\Eclipse Adoptium",
    "C:\Program Files\Microsoft",
    (Join-Path $env:USERPROFILE ".jdks")
)) {
    if (Test-Path -LiteralPath $root -PathType Container) {
        Get-ChildItem -LiteralPath $root -Directory -ErrorAction SilentlyContinue |
            Where-Object { $_.Name -match 'jdk[-_]?2[1-9]|jdk[-_]?[3-9]\d' } |
            ForEach-Object { $candidates.Add($_.FullName) }
    }
}

$javaHome = $null
foreach ($candidate in $candidates | Select-Object -Unique) {
    if ((Get-JavaMajorVersion $candidate) -ge 21) {
        $javaHome = (Resolve-Path -LiteralPath $candidate).Path
        break
    }
}

if (-not $javaHome) {
    throw "Java 21 or newer was not found. Set MEWCODE_JAVA_HOME or JAVA_HOME to a JDK 21 installation."
}

$env:JAVA_HOME = $javaHome
$env:Path = "$(Join-Path $javaHome 'bin');$env:Path"

if (-not $MavenArgs -or $MavenArgs.Count -eq 0) {
    $MavenArgs = @("clean", "verify")
}

Write-Host "Using JAVA_HOME=$javaHome"
& mvn @MavenArgs
exit $LASTEXITCODE
