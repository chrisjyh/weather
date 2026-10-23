# 개발·실행 가이드

## 개발 환경과 인증키


- Android SDK 35, JDK 17 이상, 기존 Gradle Wrapper 사용
- Pleos Connect 2.1.0 / Android 14 에뮬레이터 또는 일반 Android API 28 이상
- [기상청 단기예보 조회서비스](https://www.data.go.kr/data/15084084/openapi.do) 이용 신청 후 승인된 인증키

Git에서 제외되는 루트 `local.properties`에 **일반 인증키(Decoding)** 를 추가합니다.
기존 `sdk.dir`는 유지합니다. 환경변수 `KMA_SERVICE_KEY`가 있으면 그 값을 우선합니다.

```properties
KMA_SERVICE_KEY=발급받은_Decoding_키
```

```bash
./gradlew :app:assembleDebug
./gradlew :app:installDebug
adb shell am start -n com.example.weather/.MainActivity
```

키 변경 후 다시 빌드해야 합니다. 키가 없으면 설정 오류가 표시됩니다. 잘못된 키·승인 대기·쿼터 초과는
별도 오류로 표시하며 자동으로 다른 발표를 반복 요청하지 않습니다. 요청 URL/키 로깅은 없습니다.
APK에 포함된 키는 추출 가능하므로 공개 배포용 인증 설계는 별도 검토가 필요합니다.

화면에서 **위치 허용**을 누릅니다. 권한이 없거나 위치 서비스가 꺼졌거나 10초 내 위치를 얻지 못하면
마지막 저장 위치, 없으면 서울 강남구(37.5172, 127.0473)를 사용합니다. 재요청이 차단된 경우
앱 설정으로 이동합니다. Android LocationManager를 사용하므로 Google Play Services가 필요 없습니다.

## 빌드 산출물

- debug APK: `app/build/outputs/apk/debug/app-debug.apk`
- 단위 테스트 결과: 각 모듈의 `build/reports/tests/`
- Android Lint: `app/build/reports/lint-results-debug.html`, `data/build/reports/lint-results-debug.html`
- 기기 테스트 결과: `app/build/reports/androidTests/connected/`

현재 applicationId는 `com.example.weather`입니다. Kotlin 2.0.0, AGP 8.8.2,
compile/target SDK 35, min SDK 28을 사용합니다. 의존성 버전은
[libs.versions.toml](../gradle/libs.versions.toml)을 기준으로 합니다.

## Debug 주행 제한 전환

Debug 빌드에서만 shell의 DUMP 권한으로 제한 상태를 시험할 수 있습니다. 앱이 보이는 동안 실행합니다.

```bash
adb shell am broadcast --user current -a com.example.weather.DEBUG_DRIVING -p com.example.weather --es state restricted
# state: restricted / unrestricted / unknown / disconnected / platform
```

Release에는 이 수신기와 override가 포함되지 않습니다.

여러 기기가 연결되어 있으면 `adb -s <serial>`로 대상을 선택합니다.
Pleos의 활성 Android 사용자에 앱이 설치되어 있어야 합니다. 검증 환경에서는 사용자 10을 사용했으며,
명령의 `--user current`는 현재 활성 사용자를 가리킵니다.

## 테스트와 수동 통합 확인


```bash
./gradlew :domain:test :data:testDebugUnitTest :app:testDebugUnitTest
./gradlew :app:lintDebug :data:lintDebug :app:assembleDebug
./gradlew :app:connectedDebugAndroidTest
```

단위 테스트는 좌표·발표 경계·자정·결측·강수 범주·공식 최고/최저·캐시 재사용/격자 분리·
XML/JSON 오류·인증/쿼터/타임아웃·부분 실패·위치 fallback·ViewModel 취소/중복/복귀를 확인합니다.
기기 테스트는 실제 Room 파일 재개방과 주·야간/창 크기/로딩/오류/오프라인/권한/주행 화면을 확인합니다.
테스트용 날씨는 앱의 런타임 데이터 경로에 주입하지 않습니다.

실제 API 검증은 키 설정 후 명시적으로 실행합니다(기본 테스트에서는 건너뜁니다).

```bash
./gradlew :app:connectedDebugAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.class=com.example.weather.LiveWeatherTest \
  -Pandroid.testInstrumentationRunnerArguments.liveKma=true
```

이 테스트는 실제 응답을 앱 캐시에 저장합니다. Gradle 기기 테스트는 종료 후 앱을 제거할 수 있으므로
실행 확인을 계속하려면 `:app:installDebug`로 다시 설치합니다.

수동 통합 확인:

1. 권한 허용 후 `adb emu geo fix 127.0473 37.5172`로 위치 제공 → 현재 위치 출처 확인.
2. 강남/부산 위치 변경 → 서로 다른 격자의 자료가 섞이지 않는지 확인.
3. 실제 조회 성공 후 네트워크 차단 → 캐시·실황 시각 유지와 오프라인 배너 확인.
4. 화면을 숨겨 10분 이상 대기 → 위치·갱신 중지, 복귀 후 갱신 확인.
5. 앱 강제 종료 후 재실행 → 마지막 위치와 Room 캐시 복원 확인.
6. Pleos 창 확장/축소, 시스템 주·야간, 권한 회수, debug 제한 전환 확인.
7. 실제 플랫폼의 CarUxRestrictions 변경 및 실행 차단은 Pleos 정책에 따라 별도 확인.

실행 결과와 남은 검증은 [검증 기록](verification.md)에 정리했습니다.
