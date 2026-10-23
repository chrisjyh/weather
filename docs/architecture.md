# 프로젝트 구조와 상태 흐름

## 모듈 의존성

```text
app ───────→ domain
 │             ↑
 └──→ data ────┘
```

| 모듈 | 역할 | 주요 의존성 |
|---|---|---|
| app | 화면, MVVM, 권한·생명주기, 객체 조립 | Compose, Lifecycle, domain, data |
| domain | 날씨·위치·주행 모델, 저장소 계약, 유효성·위치 정책 | Kotlin/JVM, Coroutines |
| data | API, DTO·매핑, 저장소 구현, 플랫폼 어댑터 | Retrofit, OkHttp, Serialization, Room/KSP, Android Car API |

ViewModel은 domain의 계약만 참조합니다. data 구현을 선택하고 생성하는 곳은 app의 AppContainer입니다.
Room 생성은 data의 WeatherData에 두고 app에는 DAO 생성 진입점을 제공합니다.

## 디렉터리

```text
weather/
├── app/
│   └── src/
│       ├── main/
│       │   ├── AndroidManifest.xml
│       │   ├── java/com/example/weather/
│       │   │   ├── MainActivity.kt
│       │   │   ├── AppContainer.kt
│       │   │   └── ui/
│       │   │       ├── WeatherViewModel.kt
│       │   │       ├── WeatherScreen.kt
│       │   │       └── WeatherTheme.kt
│       │   └── res/
│       │       ├── drawable/       # 주·야간 VectorDrawable XML 10개
│       │       ├── font/           # Asta Sans
│       │       └── values/
│       ├── debug/java/com/example/weather/DrivingProvider.kt
│       ├── release/java/com/example/weather/DrivingProvider.kt
│       ├── test/                  # ViewModel 단위 테스트
│       └── androidTest/           # 화면, Room 복원, 선택적 실제 API 테스트
├── domain/
│   └── src/
│       ├── main/kotlin/com/example/weather/domain/
│       │   ├── Weather.kt
│       │   └── KmaGrid.kt
│       └── test/                  # 위치·격자·만료·주행 정책 테스트
├── data/
│   ├── schemas/                   # Room 스키마
│   └── src/
│       ├── main/kotlin/com/example/weather/data/
│       │   ├── KmaApi.kt
│       │   ├── KmaTime.kt
│       │   ├── WeatherMapper.kt
│       │   ├── KmaWeatherRepository.kt
│       │   ├── WeatherDatabase.kt
│       │   ├── WeatherData.kt
│       │   ├── AndroidLocationRepository.kt
│       │   └── CarDrivingRestrictionsRepository.kt
│       └── test/                  # 매핑·발표·MockWebServer 테스트와 fixtures
├── docs/
├── gradle/libs.versions.toml
├── build.gradle.kts
└── settings.gradle.kts
```

실제 소스는 [app](../app/src/main/java/com/example/weather),
[domain](../domain/src/main/kotlin/com/example/weather/domain),
[data](../data/src/main/kotlin/com/example/weather/data)에서 확인할 수 있습니다.

## 파일별 책임

| 파일 | 책임 |
|---|---|
| MainActivity.kt | 위치 권한 요청/설정 이동, START/RESUME/STOP 전달, 생명주기 기반 UI 구독 |
| AppContainer.kt | WeatherApplication, DB/Repository/UseCase 조립, ViewModel Factory 제공 |
| WeatherViewModel.kt | WeatherUiState와 StateFlow, 위치·캐시 선택, 갱신 작업과 취소, 제한 상태 반영 |
| WeatherScreen.kt | 현재 날씨·강수·예보·상태 배너·버튼, 가용 크기별 배치, drawable 선택 |
| WeatherTheme.kt | Figma 주·야간 색상, Asta Sans 타이포그래피 |
| Weather.kt | 모델·저장소 인터페이스, WeatherSnapshot 유효성, ResolveLocation/RefreshWeather |
| KmaGrid.kt | 위경도 → 기상청 Lambert 격자 변환 |
| KmaApi.kt | Retrofit 서비스, KmaItem DTO, JSON/XML 응답과 HTTP/API 오류 처리 |
| KmaTime.kt | 자료 종류별 최신/이전 발표 시각 및 유효 시각 파싱 |
| WeatherMapper.kt | 제품·발표 간 값 결합, 공식 최고·최저, 결측·강수 범주 매핑 |
| KmaWeatherRepository.kt | 격자별 캐시 관찰, 발표 재사용, API 조회·fallback·Room 저장 |
| WeatherDatabase.kt | WeatherRecord/LocationRecord, DAO, Room DB 선언 |
| WeatherData.kt | DB 생성 진입점 |
| AndroidLocationRepository.kt | LocationManager 수신, 시간 제한, Geocoder, 마지막 위치 저장 |
| CarDrivingRestrictionsRepository.kt | Car 서비스 초기 상태·변경 콜백 → domain Flow, 구독 종료 시 해제 |
| debug/release DrivingProvider.kt | 빌드 타입별 차량 저장소 선택. debug만 제한 상태 override 제공 |

현재 모델과 인터페이스는 `Weather.kt`에, UI 상태는 `WeatherViewModel.kt`에 함께 있습니다.
클래스마다 별도 파일이나 전달 전용 계층을 추가하지 않았습니다. `RefreshWeather`는 현재 저장소 갱신을
위임하는 얇은 진입점이며, 발표 재사용·fallback 정책은 data 저장소에 있습니다.

## 핵심 모델과 계약

| 타입 | 의미 |
|---|---|
| Grid | 기상청 격자 x/y. 캐시 분리 기준 |
| WeatherLocation | 위경도, 지역명, CURRENT/LAST/DEFAULT 출처, 위치 문제 |
| WeatherSnapshot | 격자, 관측·조회·상태 시각, 현재 기온·상태, 날짜별 최고·최저, 시간별 예보 |
| HourlyForecast | 유효 시각, 기온, 상태, 확률, 범주형 강수량 |
| WeatherIssue | 오류 종류와 실패한 제품 출처 |
| DrivingRestrictions | 플랫폼 지원/연결 상태, 최적화 필요 여부, 활성 제한 비트 |
| WeatherRepository | `observe(grid)`로 캐시 Flow 제공, `refresh(grid, force)`로 갱신 및 오류 목록 반환 |
| LocationRepository | 현재·마지막 위치 조회, 실패 사유, 마지막 위치 저장 |
| DrivingRestrictionsRepository | 주행 제한 상태 Flow |

## 시작·갱신 흐름

1. MainActivity가 `start(permission)`을 전달합니다.
2. ViewModel은 차량 제한 Flow를 구독하고 마지막/기본 위치의 캐시를 먼저 관찰합니다.
3. ResolveLocation이 권한에 따라 현재 위치를 획득하거나 마지막/기본 위치를 선택합니다.
4. 격자가 바뀌면 이전 캐시 관찰을 취소하고 화면의 이전 격자 자료를 비웁니다.
5. RefreshWeather를 통해 KmaWeatherRepository를 호출합니다.
6. 저장소가 제품별 발표를 계산하고, 필요한 응답만 받아 Room에 저장합니다.
7. Room Flow → WeatherMapper → WeatherSnapshot → WeatherUiState 순서로 화면이 갱신됩니다.
8. 화면이 보이는 동안 10분마다 갱신하고 매분 유효성을 다시 확인합니다.

진행 중 새로고침은 추가 작업을 시작하지 않습니다. 권한 변경과 STOP은 진행 작업을 취소하며,
세대 번호와 선택 격자를 검사해 오래된 결과가 현재 화면을 덮지 않도록 합니다.
STOP 시 foreground 하위 작업과 캐시·차량 구독을 해제합니다.

## UI 상태

`WeatherUiState`는 위치, 날씨, 최초 로딩, 갱신 중, 권한 여부, 오류 목록, 주행 제한, 오래됨 여부를
하나의 불변 값으로 유지합니다. Compose는 `collectAsStateWithLifecycle`로 읽습니다.

| 조건 | 표현 |
|---|---|
| 최초 로딩 + 사용 가능한 날씨 없음 | 정적 스켈레톤 |
| 사용 가능한 날씨 + 갱신 중 | 기존 화면 유지, 버튼에 확인 중 표시 |
| 실패 + 사용 가능한 날씨 있음 | 캐시와 오류 배너 유지 |
| 실패 + 사용 가능한 날씨 없음 | 오류 설명과 다시 시도 |
| 일부 필드 누락 | 해당 값만 `—` / 정보 없음 |
| 오래된 실황 | 시각과 경고 표시 |
| 주행 제한/미확인/연결 해제 | 간소화, 2개 예보, 권한 설정 숨김 |

## 영속 저장

`WeatherRecord`의 복합 키는 `(x, y, product, release)`입니다. 조회 시각과 직렬화한 원본 항목을
보관하므로 각 항목의 baseDate/baseTime 및 fcstDate/fcstTime도 남습니다.
`LocationRecord`는 id=1인 마지막 위치 한 개만 저장합니다. 이동 이력을 저장하지 않습니다.

매퍼는 같은 유효 시각·항목의 최신 발표를 우선 사용하고 같은 날짜의 이전 공식 최고·최저를 보완합니다.
캐시가 있다고 해서 최신 자료로 표시하지 않습니다. 관측 시각과 조회 시각을 구분합니다.

## 변경할 때의 기준

- 기상청 DTO와 제품별 규칙은 data, 화면 문구와 배치는 app에 둡니다.
- domain에 Android Context, Room Entity, Compose 타입을 넣지 않습니다.
- 공개 domain 계약 변경 시 구현체·AppContainer·ViewModel·테스트 fake를 함께 점검합니다.
- Room 구조 변경 시 스키마와 migration 정책을 함께 검토합니다.
- 새 에셋은 [리소스 기준](ui-guidelines.md)에 따라 drawable로 추가합니다.
