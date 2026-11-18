# #003-v4 — S24 WebView viewport 수정 결과
2026-10-31 Asia/Seoul

## 원인 및 대조 검증
Galaxy S24 Ultra SM-S928N / Android 16 API 36 / com.google.android.webview 156.0.8078.47.
Compose AndroidView Modifier.fillMaxSize()는 이미 있었고, EXACTLY 높이 제약도 정상 전달됨.
그러나 Android View 자체의 LayoutParams가 기본 WRAP_CONTENT(-2,-2)였음.
임시 측정 빌드 로그: initialLoad measured=0,0 actual=0,0 lp=null,null; 이후 EXACTLY constraint=1655/1667, measuredHeight 및 실제 height=1655/1667, lp=-2,-2.
따라서 상위 Compose가 최종 높이를 0으로 전달한 문제가 아님. WebView가 WRAP_CONTENT로 동작하면서 CSS viewport 높이를 0으로 취급한 것이 직접 원인.
MATCH_PARENT(-1,-1)만 명시한 대조 빌드에서 최초 로딩은 여전히 0x0에서 시작했으나 CSS dvh가 555.667px로 정상화됨. 원인 수정 효과를 로딩 지연과 분리 확인함.
Android 공식 권고도 WebView width/height MATCH_PARENT, WRAP_CONTENT 회피:
https://developer.android.com/develop/ui/views/layout/webapps/best-practices

## 최종 변경
app/src/main/java/com/webdungji/app/web/BrowserScreen.kt
- WebView 생성 시 LayoutParams(MATCH_PARENT, MATCH_PARENT) 명시.
- fillMaxSize, Scaffold padding 및 기존 상위 레이아웃 유지. 화면/상태바 높이 하드코딩이나 inset 중복 계산 없음.
- 최초 restoreState/loadUrl만 doOnLayout 뒤 유효 크기에서 한 번 실행. 이후 resize/recomposition에서 강제 reload 또는 재생성하지 않음.
- 최종 로그: initialLoad measured=1080,1655 actual=1080,1655 lp=-1,-1.
- 진행 표시줄 제거에 따라 실제 높이가 1667px로 바뀌면 WebView의 정상 크기 변경 처리로 CSS 뷰포트도 갱신됨. 수동 JS resize, 강제 requestLayout, CSS 보정 불필요.
- 임시 측정용 WebView subclass 제거. debug 최초 크기 숫자 로그만 유지.
- Basic 인증 처리, UA, 쿠키 정책, SSL 및 mixed-content 정책 변경 없음.
- 사이트별 조건/JS/CSS 주입 없음.

## 수정 전후 수치
단위 구분: 네이티브 px / 웹 CSS px (S24 DPR 3).
최종 네이티브 measuredHeight/height: 수정 전후 모두 1667px (진행 표시 중 1655px).
window.innerHeight: 555 -> 555
html.clientHeight: 556 -> 555
visualViewport.height: 555.667 -> 555.667
CSS (height:0px): true -> false
.v-application__wrap min-height: 0px -> 555.667px (선언 100dvh)
html rect.height: 0 -> 555.667px
main rect.height: 0 -> 487.667px

## 최종 빌드/실기기 검증
assembleDebug BUILD SUCCESSFUL. 최종 adb -s R5KYA01BH5V install -r Success.
APK: app/build/outputs/apk/debug/app-debug.apk
SHA256: CB02132B88244E50119FDFEC8D953AFEC345937FB41C03EDF8B0653F29182685
- RomM: 실제 로그인 화면 렌더링 확인, 빈 아이디/비밀번호 폼 및 버튼 표시. evidence/s24-v4-romm.png 시각 검토 완료.
- CDP Page.reload: Document 200, 네트워크 loadingFailed 0, JS 미처리 예외 0, viewport 정상 유지.
- am force-stop 후 am start 및 기존 RomM 등록 재진입: innerHeight 555, main 487.667, zero-height false, 비밀번호 폼 존재.
- tag: 기존 등록으로 진입, HTTPS tag 출처를 표시하는 Basic 인증창 및 마스킹 비밀번호 필드 확인, 자격증명 입력 없이 취소 확인. 실제 계정 인증 성공은 사용자 입력 필요하여 미실행.
- naver: 기존 등록으로 진입, m.naver.com / NAVER / 본문 글자수 5503, innerHeight 555, zero-height false 확인.
- 최종 PID 12519 crash 버퍼 FATAL EXCEPTION 및 Fatal signal 없음.
- 기존 등록 3개 ID 유지: arcade 27e42cce-d42e-4e19-84a4-899825250a77, tag 465b7f6d-06f1-4abc-9872-31a8695267c6, naver a1650c30-8fe3-44cd-92e2-8b325fbbfe8a. JS/images true, textZoom100 유지. 등록 편집/삭제 없음.
- 기존 쿠키 저장소 파일 존재 확인. 쿠키 값/로그인 정보는 읽거나 기록하지 않음. 제거/데이터 초기화/쿠키 삭제 API 실행 없음. 로그인 세션의 실제 유효성은 계정 입력 없이 검증하지 않음.

## 범위와 남은 확인
S24에만 설치 및 조작. 에뮬레이터 비교는 이전 진단값만 활용.
원본 Hermit, 분석 자료, 다른 프로젝트, MCP/서버/네트워크 설정 변경 없음.
실제 로그인 이후 RomM 기능은 사용자 인증 후 확인 필요. 다른 기기/화면 회전/분할 화면 전체 회귀는 수행하지 않음.
종료 시 S24 웹둥지 RomM 로그인 화면 유지.
