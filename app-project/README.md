# 웹둥지 / WebDungji 0.1.0

> 이 문서는 2026-10-30의 구현 기록입니다. 아래 `C:\Codex\WebDungji` 경로와 APK 해시는 당시 로컬 환경 기준입니다. 현재 GitHub 빌드 및 다운로드 방법은 저장소 루트 `README.md`와 `docs/CURRENT-STATUS.md`를 참조하세요.

작업지시서 #003 결과 — 2026-10-30 (Asia/Seoul).

독립 작성한 Kotlin + Jetpack Compose Material 3 앱입니다. 패키지명은 `com.webdungji.app`, 최소 API 29, 테스트 기준/target API 36입니다. 기존 Hermit APK·분석 자료·MCP 설정·environment-test 프로젝트는 변경하지 않았습니다.

## APK와 실행
- 개발용 APK: `C:\Codex\WebDungji\app-project\app\build\outputs\apk\debug\app-debug.apk`
- SHA-256: `2956c7b57daba785002a98fba055b86745df227ab664592c438f39efac015b1c`
- 설치된 APK와 위 파일의 SHA-256이 일치합니다.
- AVD `WebDungji_Test`에 설치했고, 최종 한국어 홈 화면을 실행해 두었습니다.

```powershell
# 기존 SDK/Gradle 사용, 빌드
& 'C:\Codex\WebDungji\tools\Build-WebDungji.ps1'
# 설치 또는 업데이트
& 'C:\Codex\WebDungji\tools\Android-Control.ps1' -Action Install -Apk 'C:\Codex\WebDungji\app-project\app\build\outputs\apk\debug\app-debug.apk'
# 로컬 테스트 서버·에뮬레이터·웹둥지 실행
& 'C:\Codex\WebDungji\tools\Start-WebDungji.ps1'
```

Android Studio에서는 이 app-project 디렉터리를 여세요. SDK/환경 설명은 상위 디렉터리의 기존 README.md를 참조하세요. Gradle Wrapper는 이미 해시 검증한 `C:\Codex\WebDungji\tools\gradle-9.4.1-bin.zip`을 재사용합니다.

## 구현 완료
- 한국어 기본 홈, 그리드 목록, 등록·이름/주소/아이콘 편집·확인 후 삭제, 자체 아이콘
- HTTP(S) 주소 검증, 스킴 생략 시 HTTPS 기본값, 위험 스킴·사용자정보·알려진 인증 토큰 매개변수 등록 차단
- 명시적 조회 버튼을 통한 사이트 제목·Manifest/favicon 조회, 실패 시 수동 이름·기본 아이콘, 갤러리 이미지 선택/내부 복사
- 기본 로그인 프로필과 동일 출처 사이트 데이터 공유 안내
- WebView 뒤로·앞으로·새로고침·시작 주소·목록 복귀, 로딩 표시·오류 화면·현재 주소·확인 후 외부 앱 열기·Android 뒤로
- Room 등록 정보와 앱별 JavaScript·이미지·글자 크기 저장
- DataStore 한국어/영어·밝게/어둡게/시스템 테마 저장
- 확인 후 기본 프로필 전체 사이트 쿠키·웹 저장소 삭제, 앱 정보
- UUID·시각·스키마 버전·revision/dirty·삭제 tombstone·로컬 변경 이력, 원격 저장소 인터페이스와 메타데이터 DTO
- SSL 오류 취소, 파일/콘텐츠 접근 차단, 네이티브 JS 브리지 없음, 개발 빌드에서만 WebView 디버깅, 미지원 사이트 권한 안전 거부

## 직접 테스트
1. 실행된 에뮬레이터에서 **로컬 둥지 · 보존**을 누르면 로컬 페이지가 열립니다.
2. `NEXT_PAGE_LINK`를 눌러 이동하고 하단의 뒤로·앞으로·새로고침·시작 주소 버튼을 사용하세요. 왼쪽 위 격자 버튼은 웹앱 목록으로 돌아옵니다.
3. **웹앱 추가**에서 주소 `http://127.0.0.1:8873/index.html` → **사이트 정보 가져오기** → 이름/아이콘 확인 → 상단 **저장**을 누르세요.
4. 카드의 ⋮ 메뉴에서 편집·삭제를 할 수 있습니다. 삭제는 등록 정보만 제거하고 사이트 로그인 상태는 유지합니다.
5. 오른쪽 위 설정에서 언어·테마를 바꾸세요. 앱별 JavaScript·이미지·글자 크기는 해당 웹앱 편집 화면에서 설정합니다.
6. 갤러리 이미지 선택은 시스템 사진 선택기를 엽니다. 이미지가 없으면 직접 이미지를 추가해 선택할 수 있습니다. 가져오기는 2MB 이하의 지원 이미지이며 내부 PNG로 보존합니다.

로컬 서버는 PC 루프백 `127.0.0.1:8873`에서만 대기하고 ADB reverse로 연결합니다. 에뮬레이터를 껐다 켜면 Start-WebDungji.ps1이 연결을 다시 만듭니다. 자동 웹사이트 테스트는 로컬 fixture에만 수행했으며, 사용자가 만든 다른 등록 항목은 보존했습니다.

## 검증 결과와 수정
빌드·설치·실행, 등록·목록 선택·탐색·이름 수정·삭제·한영 전환, 실제 프로세스 강제 종료 후 목록/언어/테마/쿠키/DOM 저장소 보존을 확인했습니다. 갤러리 이미지 로컬 가져오기와 동기화 응답이 새 로컬 변경을 지우지 않는 테스트도 통과했습니다. URL 단위 테스트 3개와 최종 Lint가 통과했습니다. 현재 에뮬레이터 crash 버퍼에서 웹둥지 치명적 충돌은 0건입니다.

초기 UI 테스트의 키보드·스크롤·다이얼로그 탐색 문제를 수정했고, 사용자도 편하게 저장하도록 저장 버튼을 상단에 고정했습니다. SDK 속성 파일의 Windows 경로 이스케이프도 이 프로젝트 안에서 수정했습니다. 이미 성공한 조작을 불필요하게 반복하지 않고 실패한 단계부터 이어 검증했습니다. 상세 범위와 증거는 TEST-RESULTS.md, evidence/verification.json을 보세요.

## 남은 범위
- 실제 Google 인증/Drive API/업로드·다운로드·충돌 병합·자동 스케줄링은 구현하지 않았습니다. 다음 단계의 P0 작업입니다.
- 다중 프로필 완전 격리, 북마크·태그·드래그 정렬, 다운로드·사이트 파일 업로드, 카메라·마이크·위치·팝업은 아직 지원하지 않습니다.
- Android 10(API 29)은 빌드/Lint 정적 호환성만 확인했습니다. 실제 실행 검증은 Android 16(API 36)에서 수행했습니다.
- 일반 외부 사이트와 실제 계정 로그인은 자동 테스트하지 않았습니다. 사이트별 OAuth/WebView 호환성은 후속 검증이 필요합니다.
- 갤러리 가져오기 처리 경로는 실제 MediaStore URI로 검증했습니다. 시스템 사진 선택기에서 사용자 사진을 직접 고르는 종단간 자동 테스트는 수행하지 않았습니다.
- Lint 오류는 0건이며 버전 업데이트 권고, HTTP 지원 설정, KTX 권장 등의 경고는 남아 있습니다. 개발용 debug 서명이며 배포 서명은 별도 작업입니다.

이번 작업은 여기서 종료하며, 추가 기능은 다음 지시서 이후 진행합니다.
