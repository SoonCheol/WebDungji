param(
  [Parameter(Mandatory)][ValidateSet('Status','WaitBoot','Install','Launch','Stop','Restart','Tap','Swipe','Input','Screenshot','UI','Logcat','CrashLog','Uninstall','Rotate')][string]$Action,
  [string]$Serial,
  [string]$Package='kr.webdungji.environmenttest',
  [string]$Activity='.MainActivity',
  [string]$Apk,
  [string]$Text,
  [string]$Output,
  [int]$X=0,[int]$Y=0,[int]$X2=0,[int]$Y2=0,[int]$Duration=300,
  [ValidateRange(0,3)][int]$Rotation=0
)
$ErrorActionPreference='Stop'
. "$PSScriptRoot\Android-Env.ps1"
$adb="$env:ANDROID_HOME\platform-tools\adb.exe"
if (!$Serial -and $Action -eq 'WaitBoot') {
  $connectionDeadline=(Get-Date).AddMinutes(3)
  do {
    foreach ($line in (& $adb devices)) {
      if ($line -match '^(emulator-\d+)\s+device') {
        $candidate=$Matches[1]
        if ((& $adb -s $candidate emu avd name) -contains 'WebDungji_Test') { $Serial=$candidate; break }
      }
    }
    if (!$Serial) { Start-Sleep 2 }
  } while (!$Serial -and (Get-Date) -lt $connectionDeadline)
}
if (!$Serial) {
  foreach($line in (& $adb devices)) {
    if ($line -match '^(emulator-\d+)\s+device') {
      $candidate=$Matches[1]
      if ((& $adb -s $candidate emu avd name) -contains 'WebDungji_Test') { $Serial=$candidate; break }
    }
  }
}
if (!$Serial) { throw 'WebDungji_Test is not connected. Run Start-Emulator.ps1 first.' }
function Adb { & $adb -s $Serial @args; if($LASTEXITCODE -ne 0){throw "ADB failed ($LASTEXITCODE)"} }
switch($Action) {
  Status { Adb get-state; Adb shell getprop sys.boot_completed; Adb shell getprop ro.build.version.release }
  WaitBoot { $deadline=(Get-Date).AddMinutes(3); do { if((Adb shell getprop sys.boot_completed) -eq '1'){Write-Output 'boot_completed=1'; return}; Start-Sleep 2 } while((Get-Date) -lt $deadline); throw 'Boot timeout' }
  Install { if(!$Apk){throw '-Apk required'}; Adb install -r $Apk }
  Launch { Adb shell am start -W -n "$Package/$Activity" }
  Stop { Adb shell am force-stop $Package }
  Restart { Adb shell am force-stop $Package; Adb shell am start -W -n "$Package/$Activity" }
  Tap { Adb shell input tap $X $Y }
  Swipe { Adb shell input swipe $X $Y $X2 $Y2 $Duration }
  Input { if($Text -notmatch '^[a-zA-Z0-9 ._-]+$'){throw 'Use safe ASCII text here; for Korean use Gboard or official UI Automator setText.'}; Adb shell input text ($Text.Replace(' ','%s')) }
  Screenshot { if(!$Output){throw '-Output required'}; Adb shell screencap -p /sdcard/webdungji-capture.png; Adb pull /sdcard/webdungji-capture.png $Output }
  UI { if(!$Output){throw '-Output required'}; Adb shell uiautomator dump /sdcard/webdungji-ui.xml; Adb pull /sdcard/webdungji-ui.xml $Output }
  Logcat { if(!$Output){throw '-Output required'}; Adb logcat -d -v threadtime | Set-Content -LiteralPath $Output -Encoding utf8 }
  CrashLog { if(!$Output){throw '-Output required'}; Adb logcat -b crash -d -v threadtime | Set-Content -LiteralPath $Output -Encoding utf8 }
  Uninstall { Adb uninstall $Package }
  Rotate { Adb shell wm user-rotation lock $Rotation }
}

