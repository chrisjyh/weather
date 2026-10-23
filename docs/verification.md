# 검증 기록 — 2026-10-23

## 통과

- `:app:assembleDebug`: debug APK 생성.
- `:domain:test`, `:data:testDebugUnitTest`, `:app:testDebugUnitTest`: 단위 테스트 26개 통과.
- `:app:lintDebug`, `:data:lintDebug`: 오류 0. 기존 도구/SDK 버전 유지에 따른 최신 버전 권고 등 경고는 남음.
- Pleos Connect 2.1.0, Android 14, emulator-5554 / 사용자 10에 APK 설치 및 실제 Activity 실행.
- 기기 테스트: Room 파일 재개방 1개, Compose 화면 테스트 3개 통과. 실제 API 테스트 1개는 인증키 미설정으로 기본 실행에서 제외.
- Compose: 560×800 / 1120×800 주·야간, 360×600 주행 간소화, 로딩·오류·오프라인·권한 상태.
- `res/drawable`에 주·야간 날씨/새로고침 아이콘 10개를 VectorDrawable XML로 변환.
  별도 SVG 렌더러 제거, Compose `Image(painterResource(...))` 사용.
- 기존 SVG 렌더링과 XML 렌더링의 주요 아이콘 영역을 픽셀 비교. 평균 채널 차이 0.39–1.91/255로
  안티앨리어싱 수준이며 겹침/크기 깨짐 없음. PNG 대체가 필요한 아이콘 없음.
- 마지막 APK를 재설치한 뒤 `MainActivity` COLD 실행 성공(Status: ok).

검증용 수치는 androidTest 전용이며 실제 앱의 데이터로 주입하지 않았습니다.
캡처는 앱 내부 `files/weather-qa`에 저장되며 저장소에 포함하지 않습니다.

## 미검증 / 후속 확인

- `KMA_SERVICE_KEY` 미설정: 실제 기상청 인증·응답 및 실제 날씨 표시 미검증.
  앱은 설정 오류를 표시하며 샘플 날씨로 대체하지 않음.
- 실제 API 성공 후 네트워크 차단·프로세스 강제 종료·격자 이동의 종단 간 검증은 키 설정 후 수행 필요.
  대응 정책은 단위 테스트 및 Room 재개방 테스트로 확인함.
- 실제 위치 권한/위치 서비스 전환과 Geocoder 동작은 기기 통합 확인 필요.
- 실제 플랫폼 주행 상태 전환 및 DO/주행 중 실행 승인은 미검증.
  제한 상태에 대한 Compose 표시 및 ViewModel 반영은 테스트로 확인함.
- Pleos 시스템 창 확장 제스처 자체는 미검증. Compose 가용 창 크기 변경은 기기 테스트로 확인함.

실제 API 선택 테스트와 수동 확인 순서는 루트 README를 참고하세요.
