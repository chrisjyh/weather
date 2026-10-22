# weather

Kotlin, Jetpack Compose, MVVM으로 구성한 Pleos Connect Vehicle SDK 기본 예제입니다.
운전석 1열 왼쪽 문의 지원 범위 확인, 상태 조회, 열기/닫기 요청, 변경 알림 구독을 제공합니다.

## 요구 환경

- Pleos Connect Emulator 2.1.0 이상 (Android API 34 이미지, Cold boot, RAM 4 GB)
- Android SDK Platform 35
- JDK 17 이상

## 빌드 및 실행

```bash
./gradlew assembleDebug
./gradlew installDebug
```

Pleos Connect Emulator를 실행한 뒤 Android Studio에서 `app`을 실행해도 됩니다. 일반 Android
에뮬레이터에는 Pleos Vehicle Service가 없으므로 차량 API를 사용할 수 없습니다.

## Virtual Console

에뮬레이터가 실행 중일 때 다음 명령으로 포트를 전달합니다.

```bash
adb forward tcp:8088 tcp:8088
```

브라우저에서 <http://127.0.0.1:8088/>를 열고 `Convenience` 도메인의 운전석 문 위치를
변경하면, 앱에서 새로고침하거나 변경 알림을 구독해 상태를 확인할 수 있습니다.

## 구조

```text
MainActivity / VehicleScreen (Compose UI)
            ↓
VehicleViewModel (화면 상태와 사용자 동작)
            ↓
VehicleRepository / PleosVehicleRepository (Vehicle SDK)
```

SDK 버전과 저장소는 `gradle/libs.versions.toml`과 `settings.gradle.kts`에서 관리합니다.
