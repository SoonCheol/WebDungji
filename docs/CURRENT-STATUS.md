# 현재 상태

2026-11-19 (Asia/Seoul) 기준.

## 앱

- Android 앱 ID `com.webdungji.app`, 버전 0.1.0, `versionCode` 1, min/target API 29/36.
- Kotlin, Compose Material 3, Room, DataStore, WebView 구현. 기존 기능 범위와 미구현 항목은 `app-project/README.md` 및 `ARCHITECTURE.md`를 따른다.
- 이번 GitHub 전환에서 앱 기능/UI 소스는 수정하지 않았다.

## 저장소와 빌드

- GitHub: `https://github.com/SoonCheol/WebDungji`, 브랜치 `main`.
- Gradle Wrapper 9.4.1은 공식 배포 URL과 SHA-256 고정값을 사용한다. 로컬 SDK 경로는 커밋하지 않는다.
- `.github/workflows/apk.yml`은 `main` push와 수동 실행으로 debug APK를 빌드하고 artifact로 제공한다.
- 이 문서의 GitHub 실행 상태, 커밋 SHA, artifact 링크는 최초 실제 빌드 확인 후 갱신한다.

## 서명과 업데이트

- 기존 0.1.0 debug APK와 로컬 `C:\Users\homes\.android\debug.keystore` 인증서 SHA-256 지문이 일치한다: `1A:35:3D:05:B5:EE:F0:44:50:55:7B:24:A9:1A:19:6D:1B:CB:7A:5A:1F:A8:9E:E5:22:D1:99:97:D7:EA:6C:0D`.
- Actions는 저장소 Secrets `WEB_DUNGJI_KEYSTORE_BASE64`, `WEB_DUNGJI_KEYSTORE_PASSWORD`, `WEB_DUNGJI_KEY_ALIAS`, `WEB_DUNGJI_KEY_PASSWORD`를 사용한다. 개인키와 비밀번호는 Git에 없다.
- S24 Ultra 설치본 서명은 기기 연결을 통해 확인해야 한다. 설치된 0.1.0 debug APK가 위 지문이라면 같은 앱 ID와 더 높은 `versionCode`의 APK로 데이터를 보존하며 업데이트할 수 있다. 현재 `versionCode` 1 재설치는 동일 버전 교체에 해당한다.

## 후속 작업 원칙

- `main`의 새 커밋은 Actions 실행 결과와 APK 서명을 확인한다.
- 사용자 기기 업데이트 전에 기존 설치본의 패키지명·인증서와 새 APK 지문을 비교한다.
- 기존 C 드라이브 원본은 보존한다. 이전 `TEST-RESULTS`의 에뮬레이터 검증을 반복할 필요가 없으면 CI 빌드 결과를 우선 사용한다.
