# RELEASE

- Branch main. Compatible signed debug APK must retain certificate SHA-256:
  1A:35:3D:05:B5:EE:F0:44:50:55:7B:24:A9:1A:19:6D:1B:CB:7A:5A:1F:A8:9E:E5:22:D1:99:97:D7:EA:6C:0D
- Gradle version is app-project/app/build.gradle. Increase versionCode on each APK update and versionName for a new GitHub Release.
- Github Actions apk.yml builds, verifies the signature, uploads the artifact and publishes a versioned release asset.
- Signed release requires the existing four WEB_DUNGJI_KEYSTORE_* Actions secrets, never check them into git.
- Settings checks /repos/SoonCheol/WebDungji/releases/latest and opens the matching APK asset in the browser.
- Installation is controlled by Android, not silently done by app. Never uninstall the current app or wipe data as part of an update.
- Check run conclusion, release asset, signature and device behavior separately.
