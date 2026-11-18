# 검증 결과 — 작업지시서 #003

2026-10-30 Asia/Seoul / AVD WebDungji_Test / Android 16 API 36 / com.webdungji.app 0.1.0.

## 실제 확인
| 항목 | 결과 / 증거 |
|---|---|
| Kotlin·Compose APK 빌드 | PASS — evidence/build-tested.log |
| 설치 APK와 최종 산출물 해시 | PASS — evidence/verification.json |
| HTTP(S)·위험 스킴·인증 매개변수 검증 | 단위 테스트 3개, 실패 0 — app/build/test-results/testDebugUnitTest/ |
| API 29 하한 정적 점검 | Lint PASS — evidence/final-test-build-lint.log; API 29 런타임 미실행 |
| 한국어 홈·등록·제목/Manifest 아이콘·WebView 선택 실행 | 실제 UI 조작/Room 및 아이콘 파일 확인 완료 |
| 뒤로·앞으로·새로고침·시작 주소·현재 주소 | 로컬 두 페이지를 사용한 실제 UI 조작 완료 |
| 이름 수정·앱별 JS/이미지 설정 저장 | Room revision 증가 및 설정값 확인 완료 |
| 삭제·동기화용 삭제 기록 | PASS — evidence/ui-deletion-sync-language.log; 삭제 확인 후 tombstone/dirty/change log/SyncEnvelope 확인 |
| 한영·테마 전환 | PASS — 위 로그 및 evidence/ui-prepare-restart.log |
| 강제 종료·프로세스 교체 | PASS — evidence/process-restart.log의 이전/이후 PID가 다르고 중간 PID 없음 |
| 재시작 후 목록·언어·테마·쿠키·DOM 저장소 보존, 한국어 복귀 | PASS — evidence/ui-restart-final.log (OK 1 test) |
| 갤러리 MediaStore URI 가져오기 | PASS — evidence/ui-persistence-gallery.log 해당 메서드 STATUS_CODE 0 |
| 오래된 동기화 ACK가 새 변경 보존 | PASS — 같은 로그 syncAckKeepsNewerLocalChanges STATUS_CODE 0 |
| 웹둥지 치명적 충돌 | 현재 Emulator crash 버퍼 0건 — evidence/app-crashes.log |
| 최종 화면/실행 상태 | 한국어 홈 및 기존 등록 목록 유지 — evidence/final-home.png, final-home.xml |

## 테스트 문제와 재검증 범위
첫 UI 시나리오는 테스트 하네스의 키보드 표시·화면 밖 요소·별도 Compose 다이얼로그 태그 탐색에서 실패했습니다. 초기 실패 로그도 보존했습니다. 저장 버튼은 제품 사용성 개선을 겸해 상단에 고정하고, 테스트는 입력기 닫기·안전한 스크롤·다이얼로그 텍스트 탐색으로 수정했습니다.

evidence/ui-stage1.log는 삭제 확인창 탐색 실패로 끝나지만, 그 이전 등록·제목/아이콘·탐색·편집·앱별 설정 검증은 실제 통과했습니다. 실패했던 삭제와 이후 단계만 별도 실행해 ui-deletion-sync-language.log에서 통과했습니다. 따라서 초기 로그 한 개가 전체 완료를 의미하는 방식은 아닙니다.

중간에 현재 언어가 한국어인 상태에서 영어를 전제로 한 보존 검사가 실패한 기록이 있습니다. 영어 저장을 명시적으로 확인한 직후 실제 강제 종료를 시행해 ui-prepare-restart.log 및 ui-restart-final.log에서 다시 통과했습니다. 같은 묶음의 갤러리·동기화 테스트는 이미 통과해 반복하지 않았습니다.

로컬 HTML/CSS/Manifest/아이콘만 테스트 요청 대상으로 사용했습니다. 계정 로그인·Drive 호출·클라우드 변경은 하지 않았습니다. 사용자 등록 데이터는 초기화하거나 삭제하지 않았습니다. 테스트가 만든 삭제 전용 항목만 soft-delete하고, 테스트가 만든 MediaStore 이미지 한 개만 제거했습니다.

## 후속 재실행
`C:\Codex\WebDungji\tools\Test-WebDungji.ps1`에 빌드→설치→UI CRUD→강제 종료→보존→이미지→동기화 테스트 순서를 저장했습니다. 매 실행마다 고유 fixture 이름을 사용하며, 기존 항목은 유지합니다. 이 정리된 재실행 스크립트는 파서/테스트 APK 컴파일만 확인했고, 이미 성공한 전체 시나리오를 또 실행하지 않았습니다.

런타임 미검증: API 29 기기, 일반 외부 사이트, 실제 인증 제공자, 갤러리 선택기의 실제 사용자 사진 선택 전체 과정. 지원 기능·보안 경계는 ARCHITECTURE.md를 참조하세요.
