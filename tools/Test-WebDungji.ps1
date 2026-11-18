$ErrorActionPreference='Stop'
. "$PSScriptRoot\Android-Env.ps1"
$project=Join-Path (Split-Path $PSScriptRoot) 'app-project'
& "$project\gradlew.bat" -p $project assembleDebug assembleDebugAndroidTest testDebugUnitTest lintDebug --console=plain
if($LASTEXITCODE -ne 0){throw 'Build/unit test/lint failed'}
& "$PSScriptRoot\Start-WebDungji.ps1"
$adb="$env:ANDROID_HOME\platform-tools\adb.exe"
$serial=$null
foreach($line in (& $adb devices)) {
 if($line -match '^(emulator-\d+)\s+device') {
  $candidate=$Matches[1]
  if((& $adb -s $candidate emu avd name) -contains 'WebDungji_Test'){ $serial=$candidate; break }
 }
}
if(!$serial){throw 'WebDungji_Test not found'}
$apkDirectory=Join-Path $project 'app\build\outputs\apk\debug'
$metadata=Get-Content -LiteralPath (Join-Path $apkDirectory 'output-metadata.json') -Raw | ConvertFrom-Json
if(@($metadata.elements).Count -ne 1){throw 'Expected one debug APK output'}
$debugApk=Join-Path $apkDirectory $metadata.elements[0].outputFile
foreach($apk in @($debugApk, "$project\app\build\outputs\apk\androidTest\debug\app-debug-androidTest.apk")) {
 & $adb -s $serial install -r $apk
 if($LASTEXITCODE -ne 0){throw 'Install failed'}
}
$runId=[Guid]::NewGuid().ToString('N').Substring(0,8)
function Invoke-Flow([string]$Method,[string]$LogName) {
 $result=& $adb -s $serial shell am instrument -w -r -e fixtureRun $runId -e class "com.webdungji.app.AppFlowTest#$Method" com.webdungji.app.test/androidx.test.runner.AndroidJUnitRunner
 $result | Set-Content -LiteralPath "$project\evidence\$LogName" -Encoding utf8
 if(($result -join "`n") -notmatch 'OK \('){throw "Instrumentation failed: $Method (see $LogName)"}
}
Invoke-Flow 'stageOneCrudAndNavigation' 'repeat-crud.log'
& $adb -s $serial shell am force-stop com.webdungji.app
& $adb -s $serial shell am start -W -n com.webdungji.app/.MainActivity
Invoke-Flow 'stageTwoRestartPersistenceAndKorean' 'repeat-restart.log'
Invoke-Flow 'galleryImportIsBoundedAndLocal' 'repeat-gallery.log'
Invoke-Flow 'syncAckKeepsNewerLocalChanges' 'repeat-sync.log'
& $adb -s $serial shell am start -W -n com.webdungji.app/.MainActivity
Write-Output "PASS. Test run $runId left its local fixture registration for review; existing entries were retained."
