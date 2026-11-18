$ErrorActionPreference='Stop'
. "$PSScriptRoot\Android-Env.ps1"
$project=Join-Path (Split-Path $PSScriptRoot) 'app-project'
& "$project\gradlew.bat" -p $project assembleDebug --console=plain
if($LASTEXITCODE -ne 0){throw 'WebDungji APK build failed'}
$apkDirectory=Join-Path $project 'app\build\outputs\apk\debug'
$metadata=Get-Content -LiteralPath (Join-Path $apkDirectory 'output-metadata.json') -Raw | ConvertFrom-Json
if(@($metadata.elements).Count -ne 1){throw 'Expected one debug APK output'}
$apkPath=Join-Path $apkDirectory $metadata.elements[0].outputFile
if(!(Test-Path -LiteralPath $apkPath)){throw 'Debug APK output not found'}
Write-Output $apkPath
