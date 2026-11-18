# 웹둥지 기본 구현 구조

이번 구현은 독립 작성한 Kotlin 코드와 자체 벡터 아이콘만 사용합니다. Hermit APK·분석 증거·자산은 빌드 입력에 포함하지 않습니다. 분석 문서의 앱 이름/패키지 초안 및 동기화 후순위 제안은 작업지시서 #003으로 대체했습니다.

- `ui`: Compose Material 3 홈 그리드, 편집, 앱별 설정, 언어/테마. ViewModel에서 Room Flow와 DataStore 값을 구독합니다.
- `data`: Room 웹앱 및 로컬 변경 이력, DataStore UI 설정, 갤러리/사이트 아이콘을 해시 기반 내부 PNG로 저장합니다. 파괴적 DB 초기화 fallback은 사용하지 않습니다.
- `domain`: HTTP(S) URL·크기·사용자정보 검증. 스킴 없는 주소는 HTTPS 기본값입니다. 등록 주소의 알려진 인증 토큰/비밀번호 매개변수는 거부하고 브라우저 내 정상 HTTP(S) 로그인 탐색과는 구분합니다.
- `network`: 명시적 ‘사이트 정보 가져오기’ 요청 시 해당 페이지, 페이지가 선언한 Manifest/favicon과 리디렉션을 제한된 크기/시간으로 조회합니다. 검색·아이콘 대행 서비스는 없습니다. 자동 조회 실패 시 수동 이름·자체 기본 아이콘으로 저장할 수 있습니다.
- `web`: 기본 프로필 WebView. 쿠키와 DOM 저장소를 WebView가 관리하며 자체 DB로 추출하지 않습니다. HTTP 등록은 허용하지만 암호화되지 않는다는 설명을 표시합니다. HTTPS→HTTP 혼합 콘텐츠는 차단합니다.
- `sync`: 버전 있는 메타데이터 DTO와 향후 원격 저장소 인터페이스. 실제 Drive 구현은 없습니다.

## 동기화 준비 (P0)
Room 스키마 버전 1과 앱별 schemaVersion 1을 둡니다. 웹앱 ID는 UUID이고 생성·수정 시각, displayOrder, revision, dirty와 deletedAt을 저장합니다. 생성/편집/삭제는 웹앱과 local_changes를 같은 트랜잭션에서 갱신합니다. 삭제는 soft-delete로 tombstone을 보존하며 사이트 세션은 삭제하지 않습니다.

SyncEnvelope는 schemaVersion과 웹앱 메타데이터를 포함합니다. 동기화 가능한 필드는 이름, 시작 주소, 자체 아이콘 스타일 또는 PNG Base64, 정렬 순서와 JavaScript/이미지/글자 크기 설정입니다. 향후 북마크·태그를 위한 DTO 필드를 두었으나 기능은 아직 구현하지 않았습니다. 기본 profileId는 ‘default’뿐입니다. 테마/언어는 기기별 로컬 설정입니다.

LocalSyncSource.pending()은 삭제 기록을 포함한 변경 데이터를 읽습니다. acknowledge()는 보낸 revision과 현재 revision이 같은 경우에만 dirty를 해제하므로 그 사이에 발생한 로컬 변경이 사라지지 않습니다. 실제 충돌 병합, 원격 변경 수신/검증/트랜잭션 적용, 아이콘 업로드, 체크포인트·재시도·WorkManager 예약은 후속 단계입니다.

SyncRemoteStore.download/upload 인터페이스만 정의했습니다. Google 인증/API, 네트워크 동기화 호출, 클라우드 업로드는 구현·실행하지 않았습니다. 쿠키·비밀번호·WebView 파일·인증 토큰을 DTO에 넣는 필드는 없습니다. 시작 주소는 개인정보가 포함될 수 있는 사용자 메타데이터이므로 실제 동기화 연결 시 대상 미리보기·동의를 설계해야 합니다.

## 보안 및 지원 범위
SSL 오류는 cancel하며 진행 허용 코드는 없습니다. file/content 접근 및 파일 URL 권한은 꺼져 있습니다. 네이티브 JavaScript 브리지는 없고 릴리스 WebView 디버깅은 꺼집니다. 위험한 최상위 스킴은 차단합니다. mailto/tel/sms/geo는 사용자 제스처와 확인 후 ACTION_VIEW로만 열며 intent URI의 component·extras를 실행하지 않습니다.

카메라·마이크·위치, 사이트 파일 업로드, 팝업은 현재 지원하지 않아 안전하게 거부합니다. 외부 브라우저 열기도 확인을 거칩니다. 백업과 기기 이전에서 앱 데이터는 제외합니다. 전체 URL·로그인 정보·쿠키를 앱 로그로 남기지 않습니다.

같은 기본 프로필의 동일 출처 사이트 데이터는 공유됩니다. 다중 프로필 격리 및 외부 OAuth 공급자의 WebView 로그인 지원은 보장하지 않습니다. 다운로드·태그·북마크·드래그 정렬·홈 바로가기는 이번 단계 범위 밖입니다.

## 호환성과 라이선스
compile/target API 36, min API 29. Java 17 바이트코드 / Microsoft JDK 21 빌드. AGP 9.2.1의 내장 Kotlin 2.2.10, Compose compiler 2.2.10, Gradle 9.4.1을 사용합니다. 고정한 안정 라이브러리를 사용하며 Gradle 업데이트 알림은 자동 업그레이드 사유로 취급하지 않습니다.

Kotlin/AndroidX는 Apache 2.0, Jsoup는 MIT 라이선스입니다. 배포용 릴리스 제작 시 전이 의존성까지 고지 파일을 정리하고 개인 배포 서명을 별도로 준비해야 합니다. 현재 APK는 개발용 debug 서명입니다.
