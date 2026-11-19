$ErrorActionPreference = 'Stop'
$projectPath = Split-Path -Parent $PSScriptRoot
$bundledJava = Join-Path $env:ProgramFiles 'Android\Android Studio\jbr'
if (-not $env:JAVA_HOME -and (Test-Path -LiteralPath $bundledJava)) {
    $env:JAVA_HOME = $bundledJava
}
Push-Location -LiteralPath $projectPath
try {
    & .\gradlew.bat :app:assembleDebug :app:testDebugUnitTest :app:lintDebug --console=plain
    if ($LASTEXITCODE -ne 0) { throw 'A compilação ou a verificação encontrou um erro.' }
    Write-Host "APK pronto: $projectPath\app\build\outputs\apk\debug\app-debug.apk"
} finally {
    Pop-Location
}
