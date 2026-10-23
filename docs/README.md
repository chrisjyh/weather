# Pleose Weather 문서

차량용 날씨 MVP의 기획, 구현 구조, 개발 방법과 검증 결과를 정리합니다.
기준일은 **2026-10-23**입니다. 계획과 현재 구현, 실제 기기에서 확인한 범위를 구분합니다.

| 문서 | 내용 |
|---|---|
| [구현 계획](implementation-plan.md) | 목표, 포함·제외 범위, 단계별 계획과 진행 상태, 완료 조건 |
| [프로젝트 구조](architecture.md) | 모듈 의존성, 파일별 책임, DI, 데이터·상태·생명주기 흐름 |
| [화면과 리소스](ui-guidelines.md) | Compose 컴포넌트, 반응형 배치, 주행 제한, XML/PNG 사용 기준 |
| [개발·실행 가이드](development.md) | 인증키 설정, 빌드·설치, 테스트, debug 주행 전환, 수동 확인 순서 |
| [기상청 API 기준](kma-contract.md) | 공식 가이드 버전, 발표 시각, 항목 코드, 결측·오류 처리 |
| [검증 기록](verification.md) | 통과한 검증과 아직 확인하지 못한 항목 |
| [Asta Sans 라이선스](licenses/AstaSans-OFL.txt) | 포함된 폰트의 OFL 라이선스 |

## 현재 상태

`app / domain / data` 모듈 분리, 기상청 연동 코드, 위치 fallback, Room 캐시,
Compose 화면, 주행 제한 연동을 구현했습니다. debug APK 빌드·설치·실행,
단위 테스트와 기기 화면 테스트, Lint를 수행했습니다.

**실제 기상청 데이터 표시까지 완료된 상태는 아닙니다.** 인증키가 없어 실제 API 검증이 남아 있습니다.
실제 위치 서비스·플랫폼 주행 상태 전환·Pleos 창 제스처 등 종단 간 확인 범위는
[검증 기록](verification.md)을 따릅니다.

## 기획·디자인 원본

- [FigJam 기획](https://www.figma.com/board/agsqiFkUyZ1VzWgyZZF0QE/pleose-weather?node-id=0-1): 기능과 상태 흐름
- [Figma GUI](https://www.figma.com/design/7R5NVjMNGbal1cTU6xw3FA/pleose-weather?node-id=3-2): 화면과 컴포넌트
- [Pleos 주행 규제 가이드](https://document.pleos.ai/docs/pleos-only/vehicle-app-developer-guide/driving-restriction-development-guide)
- [Pleos 화면·생명주기 가이드](https://document.pleos.ai/docs/pleos-only/vehicle-app-planning-guide/app-structure-screen-model)
- [Pleos UI 디자인 가이드](https://document.pleos.ai/docs/connect/guide/docs-design/Introduction/intro)

## 문서 관리

범위나 우선순위는 구현 계획, 코드 책임은 프로젝트 구조, 화면·에셋 결정은 화면과 리소스 문서를
수정합니다. API 규칙을 바꿀 때는 공식 기준과 관련 테스트를 함께 갱신합니다.
검증 기록에는 실행 환경과 실제 결과를 남기며, 테스트 데이터 검증을 실제 API 검증으로 기록하지 않습니다.
