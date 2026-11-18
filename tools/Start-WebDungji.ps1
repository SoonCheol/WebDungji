$ErrorActionPreference='Stop'
. "$PSScriptRoot\Android-Env.ps1"
$project=Join-Path (Split-Path $PSScriptRoot) 'app-project'
try {
 $response=Invoke-WebRequest 'http://127.0.0.1:8873/manifest.json' -TimeoutSec 2
 if($response.Content -notmatch 'Local Nest'){throw 'Port 8873 is occupied by another server'}
} catch {
 $listener=Get-NetTCPConnection -LocalPort 8873 -State Listen -ErrorAction SilentlyContinue
 if($listener){throw 'Port 8873 is occupied; existing server was not changed.'}
 Start-Process -FilePath (Get-Command python).Source -ArgumentList "$project\fixtures\serve.py" -WindowStyle Hidden
}
& "$PSScriptRoot\Start-Emulator.ps1"
& "$PSScriptRoot\Android-Control.ps1" -Action WaitBoot
$adb="$env:ANDROID_HOME\platform-tools\adb.exe"
$serial=$null
foreach($line in (& $adb devices)) {
 if($line -match '^(emulator-\d+)\s+device') {
  $candidate=$Matches[1]
  if((& $adb -s $candidate emu avd name) -contains 'WebDungji_Test'){ $serial=$candidate; break }
 }
}
if(!$serial){throw 'WebDungji_Test not connected'}
& $adb -s $serial reverse tcp:8873 tcp:8873
if($LASTEXITCODE -ne 0){throw 'Local test page forwarding failed'}
& "$PSScriptRoot\Android-Control.ps1" -Action Launch -Package com.webdungji.app
