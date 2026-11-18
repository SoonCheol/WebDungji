# 작업지시서 #003-v2 결과 — 2026-10-31 Asia/Seoul

## 결과
- ADB: emulator-5554만 연결. Galaxy S24 Ultra 미연결.
- 환경: Android 16/API 36, WebView 133.0.6943.137, 웹둥지 0.1.0 debug.
- arcade: 수정 전/후 문서 200 및 로그인 UI 표시. password input 존재. 하위 XHR 401 + WWW-Authenticate Bearer 관찰. 로그인 미실행. 흰 화면 미재현, S24 원인 미확정.
- tag: 수정 전 최상위 Document 401 + WWW-Authenticate Basic 확인. 수정 후 https://tag.homedisk.net을 표시하는 HTTP 인증창, 사용자명/마스킹 비밀번호 입력 및 취소 확인. 취소 후 다시 열었을 때 인증창 재표시. 임의 자격증명 입력/전송 없음. 실제 인증 성공 및 로그인 후 화면은 사용자 확인 필요.
- naver: 수정 전/후 https://m.naver.com으로 이동, Document 200, NAVER UI 표시. 하위 Fetch 401 존재해도 최상위 페이지는 유지. 수정 전 일부 혼합 이미지 차단 및 보조 요청 연결 거부가 있었으나 우회하지 않음.
- NAS 관찰 구간 JS 미처리 예외 0, 네트워크 loadingFailed 0, WebSocket frame error 0. 인증 이후 WebSocket 연결은 검증하지 못함.

## 원인과 수정
BrowserScreen.kt 한 파일 수정.
- tag Basic 인증을 처리할 onReceivedHttpAuthRequest 누락이 확인됨. HTTPS 현재 탐색 호스트와 요청 호스트가 일치할 때만 입력창 제공. 그 외 취소. 백그라운드 전환/화면 종료/다른 호스트 탐색 시 대기 인증 취소.
- 비밀번호 마스킹, 인증창 FLAG_SECURE, 입력 상태는 remember로만 관리하고 제출/취소 시 비움. DB/설정/로그에 자격증명 저장하지 않음. WebView 프로세스 내부 인증 캐시는 별개이며 영구 저장 API는 호출하지 않음.
- 최상위 HTTP 오류에 상태 코드 표시, 401은 인증 필요 메시지. 하위 오류가 페이지 전체를 가리지 않음.
- debug 로그: HTTP 상태/최상위 여부, 네트워크 오류 코드/최상위 여부, 콘솔 심각도/줄 번호만 기록. 콘솔 본문, URL, 요청/응답 헤더 및 본문은 기록하지 않음.
- JS 앱별 설정, DOM Storage 켜짐, 일반 쿠키 허용, 제3자 쿠키 차단, 혼합 콘텐츠 차단, SSL 오류 취소, 팝업 제한 유지. UA 변경 없음.

## User-Agent / 세션 / 한계
관찰 WebView UA:
Mozilla/5.0 (Linux; Android 16; sdk_gphone64_x86_64 Build/BE2A.250530.026.D1; wv) AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 Chrome/133.0.6943.137 Mobile Safari/537.36
실제 외부 브라우저 UA 비교는 미실행. 앱의 외부 열기는 ACTION_VIEW + CATEGORY_BROWSABLE이며 쿠키 전달 코드는 없음. 외부 브라우저 정상이라는 사용자 관찰만으로 동일 세션임을 추정하지 않음.
기존 쿠키를 삭제하거나 강제 로그아웃하지 않음. 기존 로그인 세션 유무/만료 여부와 로그인 전후 차이는 미검증. arcade의 미인증 UI와 Bearer 401은 세션 부재/만료와 부합하지만 쿠키만을 원인으로 확정하지 않음.
CDP 관찰은 해당 앱 WebView를 사용했고, tag 신규 등록 및 실제 앱 UI 진입으로 인증 콜백을 별도 검증함.

## 빌드·설치·보존
assembleDebug 최종 성공. adb install -r Success. 제거/데이터 초기화 없음.
app/build/outputs/apk/debug/app-debug.apk
기존 두 등록 ID 유지: 로컬 938a6984-8ce0-4b23-8ba6-60982ac7308e, arcade 631c6d9c-06a8-46e5-ac94-c8436d600784.
최종 두 항목 모두 JS/images true, textZoom 100, revision 2. 작업 중 기존 항목 편집 없음. 신규 NAS-Tag 등록 40a367db-ee73-470e-a102-7675afd0acb2, revision 1.
기존 설정 파일/쿠키 삭제 API 호출 없음. 에뮬레이터 키보드 입력 언어를 일시 영어로 전환 후 한국어로 복귀.
현재 PID 6871 로그 FATAL EXCEPTION 없음. crash 버퍼 com.webdungji.app 언급 0.
빌드 과정의 Compose 문자열 호출 위치 오류를 수정한 뒤 최종 빌드 성공. 전체 회귀 테스트는 실행하지 않음.

## S24 추가 확인
1. 같은 업데이트 APK를 데이터 유지 설치하고 WebView 공급자/버전 및 arcade의 앱별 JS 설정 확인.
2. tag 실제 자격증명은 사용자가 직접 입력하여 인증 성공/취소/잘못된 비밀번호 재요청 확인.
3. arcade 로그인 UI 표시 및 사용자 로그인 후 정상 화면/실시간 연결 확인. 흰 화면 지속 시 실패 시점의 콘솔/네트워크를 민감정보 제외 방식으로 관찰.
4. 외부 브라우저와 WebView의 실제 UA 및 각각의 로그인 상태 비교. 기존 쿠키 삭제는 하지 않음.

NAS/NPM/네트워크 설정, 원본 Hermit APK 및 다른 프로젝트 변경 없음.
종료 시 웹둥지 NAS-Tag HTTP 인증창을 실행 상태로 유지. 로그인 이후 NAS 정상 접속은 아직 검증 완료가 아님.
