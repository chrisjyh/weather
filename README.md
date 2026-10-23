# Pleose Weather

Pleos Connect용 Kotlin · Jetpack Compose 날씨 MVP입니다. 현재 위치 또는 마지막/기본 위치의
기상청 실황과 향후 6시간 예보를 표시하는 `app / domain / data` 구조입니다.

## 빠른 실행

Android SDK 35와 JDK 17 이상이 필요합니다. Git에서 제외되는 `local.properties`에
기상청 **일반 인증키(Decoding)** 를 추가합니다. 기존 `sdk.dir`는 유지합니다.
환경변수 `KMA_SERVICE_KEY`가 있으면 우선 사용하며, 키 변경 후 다시 빌드합니다.

```properties
KMA_SERVICE_KEY=발급받은_Decoding_키
```

```bash
./gradlew :app:assembleDebug
./gradlew :app:installDebug
adb shell am start -n com.example.weather/.MainActivity
```

키가 없으면 설정 오류를 표시하며 샘플 날씨로 대체하지 않습니다.
현재 debug APK 빌드·설치·실행과 자동 테스트를 확인했으며, 인증키를 사용한 실제 API 검증은 남아 있습니다.

## 프로젝트 문서

[문서 목차](docs/README.md)에서 계획, 구현과 검증 상태를 확인할 수 있습니다.

| 문서 | 내용 |
|---|---|
| [구현 계획](docs/implementation-plan.md) | 목표·범위, 단계별 상태, 남은 완료 조건 |
| [프로젝트 구조](docs/architecture.md) | 모듈과 파일별 책임, 데이터·상태 흐름 |
| [화면과 리소스](docs/ui-guidelines.md) | 반응형 Compose, 주행 제한, drawable XML/PNG 기준 |
| [개발·실행](docs/development.md) | 환경, 키 설정, 빌드·테스트, debug 주행 전환 |
| [기상청 API 기준](docs/kma-contract.md) | 발표 시각, 항목·결측·오류 정책 |
| [검증 기록](docs/verification.md) | 실제 수행 결과와 미검증 항목 |
