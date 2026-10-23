# 화면과 리소스 기준

## 디자인 기준

[Figma GUI](https://www.figma.com/design/7R5NVjMNGbal1cTU6xw3FA/pleose-weather?node-id=3-2)의
색상·카드·큰 기온·시간별 예보를 Compose로 구현합니다. 기능과 상태 흐름은
[FigJam 기획](https://www.figma.com/board/agsqiFkUyZ1VzWgyZZF0QE/pleose-weather?node-id=0-1)을 기준으로 합니다.
체감 온도, 바람·습도·일몰은 현재 MVP 범위에서 제외합니다.

## 컴포넌트

구현은 [WeatherScreen.kt](../app/src/main/java/com/example/weather/ui/WeatherScreen.kt)에 있습니다.

| 컴포넌트 | 표시 내용 |
|---|---|
| CurrentWeatherHero | 위치와 출처, 현재 기온·날씨, 공식 최고·최저, 자료 시각 |
| WeatherMetric | 다음 1시간 예상 강수량, 향후 6시간 최대 강수 확률 |
| HourlyForecast | 선택한 시점별 기온·날씨·강수 정보 |
| StatusBanner | 오래된 자료, 위치 fallback, 오프라인·부분 실패 등 상태 |
| WeatherActionButton | 새로고침·재시도, 권한 요청·설정 진입 |

## 창 크기와 주행 제한

기기 전체 해상도가 아닌 앱 창의 가용 dp 크기로 배치를 결정합니다.

| 조건 | 배치 |
|---|---|
| 너비 840dp 이상 | 현재 날씨와 향후 6시간 예보 좌우 배치 |
| 너비 840dp 미만 | 현재 날씨 중심, 핵심 4개 예보 시점 |
| 높이 700dp 미만 | 크기와 간격을 줄이는 compact 배치 |
| 제한 활성·상태 미확인·차량 연결 해제 | 핵심 날씨와 2개 예보 시점, 스크롤 제한, 권한 설정 숨김 |
| 일반 Android의 차량 API 미지원 | 일반 화면 |

간소화 예보는 첫 시점과 날씨 변화 시점, 변화가 없으면 마지막 시점을 선택합니다.
새로고침 버튼은 유지합니다. CarUxRestrictions의 초기 상태와 변경 콜백을 사용하며 속도·기어로
별도 주행 판단을 하지 않습니다. 방향 고정과 DO 선언은 하지 않고 플랫폼 실행 제한을 따릅니다.

## 상태 표현

- 최초 로딩에는 정적 스켈레톤을 표시합니다.
- 캐시가 있는 갱신은 기존 화면과 자료 시각을 유지합니다.
- 사용 가능한 자료가 없는 실패는 오류와 재시도를 표시합니다.
- 부분 누락은 해당 값만 `—` 또는 `정보 없음`으로 표시합니다.
- 위치 출처와 오래된 실황 경고를 보여 주며 결측을 0이나 맑음으로 대체하지 않습니다.

액션의 터치 영역은 최소 56dp입니다. 시스템 글자 크기를 따르고 반복 애니메이션이나 자동 스크롤을
사용하지 않습니다. 큰 글자와 작은 창에서의 잘림은 화면 변경 시 함께 확인합니다.

## 주·야간과 폰트

[WeatherTheme.kt](../app/src/main/java/com/example/weather/ui/WeatherTheme.kt)가 시스템 주·야간을 따릅니다.

| 토큰 | 주간 | 야간 |
|---|---|---|
| 배경 | `#EEF2F5` | `#0E151F` |
| 카드 | `#FFFFFF` | `#172330` |
| 주요 글자 | `#172937` | `#F4F7FA` |
| 강조 | `#185C8D` | `#A7D8FF` |

폰트는 `app/src/main/res/font/asta_sans.ttf`의 Asta Sans를 사용합니다.
[OFL 라이선스](licenses/AstaSans-OFL.txt)를 함께 보관합니다.

## 이미지·아이콘 저장 규칙

사용자 결정에 따라 앱 이미지와 아이콘은 Android drawable 리소스로 포함합니다.
Compose에서는 `painterResource(R.drawable.…)`로 읽습니다.

1. 정상 변환되는 벡터는 `res/drawable`의 VectorDrawable XML로 저장합니다.
2. 마스크·필터 등으로 XML 변환이 깨지는 경우에만 원본 기준 2배수 PNG로 내보냅니다.
3. 1배 논리 크기의 2배 픽셀 PNG는 `res/drawable-xhdpi`에 넣어 밀도 정보를 맞춥니다.
4. 앱 화면에서 실제 크기·주야간 대비·투명 배경을 확인한 뒤 APK 포함 여부를 확인합니다.

현재 [drawable 폴더](../app/src/main/res/drawable)에는 주간/야간 각각
맑음·구름·구름 조금·비·새로고침의 **10개 XML**이 있습니다. 모두 정상 변환되어 PNG 대체는 없습니다.
런타임 SVG와 별도 SVG 렌더러는 제거했습니다. 눈 상태는 현재 텍스트 눈송이, 알 수 없는 상태는 대시를
사용합니다. 새 날씨 아이콘을 추가하면 상태 매핑과 주·야간 리소스를 함께 점검합니다.

## 화면 검증

자동 화면 테스트는 560×800dp, 1120×800dp의 주·야간과 360×600dp 간소화 화면,
로딩·오류·오프라인·권한 상태를 확인합니다. 이 검증은 실제 Pleos 창 제스처나
플랫폼의 주행 중 실행 승인을 대신하지 않습니다.

수행 결과는 [검증 기록](verification.md), 실행 명령과 debug 전환은
[개발 가이드](development.md)를 확인합니다.
