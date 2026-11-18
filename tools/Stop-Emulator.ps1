. "$PSScriptRoot\Android-Env.ps1"
$adb = "$env:ANDROID_HOME\platform-tools\adb.exe"
foreach ($line in (& $adb devices)) {
    if ($line -match '^(emulator-\d+)\s+device') {
        $serial = $Matches[1]
        if ((& $adb -s $serial emu avd name) -contains 'WebDungji_Test') {
            & $adb -s $serial emu kill
            exit $LASTEXITCODE
        }
    }
}
Write-Output 'WebDungji_Test is not running.'
