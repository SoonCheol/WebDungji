# WebDungji

Kotlin, Jetpack Compose, Room, DataStore, WebView 기반 Android 앱입니다. Android 프로젝트는 [`app-project`](app-project)에 있습니다. 현재 앱은 `com.webdungji.app` 0.1.0이며 최소 Android API 29, 대상 API 36입니다.

## 개발

Android Studio에서 `app-project`를 엽니다. JDK 21과 Android SDK Platform 36, Build Tools 36.0.0이 필요합니다. SDK 경로는 각 개발자의 `local.properties` 또는 `ANDROID_HOME`으로 지정하며 이 파일은 Git에 포함하지 않습니다.

```powershell
cd app-project
./gradlew.bat assembleDebug
```

로컬 Windows 보조 스크립트는 `tools`에 있습니다. 에뮬레이터와 SDK가 설치된 컴퓨터에서 사용합니다. 기존 `C:\Codex\WebDungji` 경로를 전제로 했던 과거 작업 기록은 `app-project/README.md`, `ARCHITECTURE.md`, `TEST-RESULTS*.md`에 남아 있으며 현재 빌드 경로로 사용하지 않습니다.

## GitHub Actions APK

`main`에 push하거나 Actions에서 **Build compatible APK**를 수동 실행하면 APK를 빌드합니다. 성공한 실행의 **Artifacts**에서 `WebDungji-compatible-debug-apk`를 다운로드합니다. 저장소 Secrets 네 개가 필요합니다: `WEB_DUNGJI_KEYSTORE_BASE64`, `WEB_DUNGJI_KEYSTORE_PASSWORD`, `WEB_DUNGJI_KEY_ALIAS`, `WEB_DUNGJI_KEY_PASSWORD`. 키 파일과 비밀번호는 Git에 넣지 않습니다. 비밀값이 없으면 빌드를 중단합니다.

APK는 기존 개발용 debug 인증서의 SHA-256 지문 `1A:35:3D:05:B5:EE:F0:44:50:55:7B:24:A9:1A:19:6D:1B:CB:7A:5A:1F:A8:9E:E5:22:D1:99:97:D7:EA:6C:0D`과 일치해야 합니다. 이 지문은 공개 인증서 정보이며 개인키가 아닙니다. 설치 전 기기의 기존 앱 패키지명과 서명을 확인하세요. 서명이 일치하는 APK는 `adb install -r`로 데이터를 유지하며 업데이트할 수 있습니다. 앱 삭제나 데이터 삭제는 필요하지 않습니다.

## 문서

- [구조](app-project/ARCHITECTURE.md)
- [기존 테스트 결과](app-project/TEST-RESULTS.md)
- [현재 상태와 후속 작업](docs/CURRENT-STATUS.md)
