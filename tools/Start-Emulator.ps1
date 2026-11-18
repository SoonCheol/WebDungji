. "$PSScriptRoot\Android-Env.ps1"
$adb = "$env:ANDROID_HOME\platform-tools\adb.exe"
$devices = & $adb devices
foreach ($line in $devices) {
    if ($line -match '^(emulator-\d+)\s+device') {
        $serial = $Matches[1]
        if ((& $adb -s $serial emu avd name) -contains 'WebDungji_Test') {
            Write-Output "WebDungji_Test already running: $serial"
            exit 0
        }
    }
}
Start-Process -FilePath "$env:ANDROID_HOME\emulator\emulator.exe" -ArgumentList '-avd WebDungji_Test -gpu auto' -WindowStyle Normal

