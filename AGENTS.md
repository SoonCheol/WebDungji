# WebDungji 개발 지침

- 기본 작업 위치는 이 저장소 루트이며 Android Gradle 프로젝트는 `app-project`이다. 개발 브랜치는 `main`이다.
- 기존 기능과 UI를 유지한다. 별도 요청 없이 기능, 화면, 패키지명 `com.webdungji.app`, `versionCode`를 변경하지 않는다.
- `app-project/README.md`, `ARCHITECTURE.md`, `TEST-RESULTS*.md`, `docs/CURRENT-STATUS.md`를 먼저 확인하고 필요한 소스만 읽는다.
- SDK 경로, Gradle 캐시, 빌드 결과물, 테스트 증거, 키, 비밀번호, 토큰, `local.properties`를 커밋하지 않는다. `.gitignore`와 `git status`로 확인한다.
- 기존 `C:\Codex\WebDungji` 원본을 수정하거나 삭제하지 않는다. 이전 문서의 C 드라이브 경로는 과거 기록이며 현재 빌드 입력이 아니다.
- GitHub Actions `Build compatible APK`는 `main` push와 수동 실행에 반응한다. 성공한 실행의 `WebDungji-compatible-debug-apk` artifact에서 APK를 받는다.
- Actions 서명 비밀값은 저장소 Secrets에만 보관한다. 기존 debug APK 인증서 SHA-256 지문은 `1A:35:3D:05:B5:EE:F0:44:50:55:7B:24:A9:1A:19:6D:1B:CB:7A:5A:1F:A8:9E:E5:22:D1:99:97:D7:EA:6C:0D`이다. 빌드 후 지문이 다르면 기기에 설치하지 않는다.
- S24 Ultra의 기존 앱을 삭제하거나 데이터를 초기화하지 않는다. 업데이트 필요 시 패키지명, 설치본과 새 APK의 서명을 확인한 뒤 `adb install -r`을 사용한다.
- 문서와 빌드 설정 변경은 최소 범위로 검증한다. 실제 사용자 데이터를 건드리는 테스트는 명시적 요청이 있을 때만 실행한다.

## Continuity and change control (2026-11-23)
- Read AGENTS.md, docs/CURRENT-STATUS.md, actual main HEAD, and relevant code first.
- Never create new clones, branches, repositories or deployment environments without permission.
- Modify, commit, build, deploy only with explicit user instruction. Report each step separately.
- Preserve existing app data, package ID and signing certificate. Increment versionCode on installable updates.
- Never commit secrets, auth tokens, private keys, local.properties or user data.
- Backups exclude WebView cookies, logins and secrets. Restore requires explicit merge or replacement selection.
- Manual Google Drive document provider backup is NOT automatic synchronization. Do not conflate them.
- Keep docs/CURRENT-STATUS.md, docs/ROADMAP.md and docs/RELEASE.md current.
