param(
    [Parameter(ValueFromRemainingArguments = $true)]
    [string[]] $MavenArgs
)

$jdkHome = Join-Path $env:USERPROFILE ".jdks\ms-21.0.8"

if (-not (Test-Path $jdkHome)) {
    Write-Error "JDK 21 not found at $jdkHome"
    exit 1
}

if (-not $MavenArgs -or $MavenArgs.Count -eq 0) {
    $MavenArgs = @("test")
}

$env:JAVA_HOME = $jdkHome
$env:Path = "$env:JAVA_HOME\bin;$env:Path"

& mvn @MavenArgs
exit $LASTEXITCODE
