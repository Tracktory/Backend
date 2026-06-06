# tracktory-backend

[![CI](https://github.com/Tracktory/Backend/actions/workflows/ci.yml/badge.svg)](https://github.com/Tracktory/Backend/actions/workflows/ci.yml)

tracktory 의 메인 백엔드. 모바일 앱이 직접 통신하는 **유일한** API 서버로, 인증·도메인·트랜잭션을 담당하고 AI 처리(추천·설명 생성 등)는 내부 AI 중계 서버에 위임한다.

**Stack**: Java 21 · Spring Boot 4.x · Gradle (wrapper) · MySQL 8.4

## Architecture

tracktory-backend 는 앱과 직접 통신하는 **유일한 앱-facing 백엔드(BFF)** 입니다. React Native 앱은 Spring Boot API 만 호출하고, 추천·챗봇·직무 브리핑처럼 AI 연산이 필요한 요청은 Spring 이 내부 FastAPI 서버로 중계합니다.

Spring 의 책임은 인증, 도메인 트랜잭션, 사용자 프로필과 이수 과목 관리, 추천 결과 저장과 리포트 조립입니다. AI 서버는 외부에 직접 노출하지 않고, Spring 이 검증한 사용자 컨텍스트만 내부 헤더로 전달합니다.

![Spring 책임 흐름도 placeholder](docs/assets/readme/backend-flow.svg)

## Responsibilities

| 영역 | 책임 |
|---|---|
| 인증 · 인가 | 회원가입/로그인, JWT 발급·검증, 인증 사용자 컨텍스트 제공 |
| 프로필 · 온보딩 | 학년, 소속, 트랙, 관심사, 개발 분야, 근무 가치관, 기술스택 저장·조회 |
| 이수 과목 | 사용자 이수 과목 추가·삭제, 추천 재계산을 위한 변경 이벤트 발행 |
| 추천 생성 · 저장 | 온보딩 스냅샷을 AI 서버로 전달하고, 직무·트랙·로드맵·역량 분석 결과를 DB 에 저장 |
| 추천 리포트 | 활성 추천 기준 상세 분석 리포트와 기준 직무별 역량 충족도를 조립 |
| AI 중계 | 추천, 챗봇, 직무 브리핑 요청을 내부 FastAPI API 로 위임 |
| 카탈로그 | 단과대, 학부, 트랙, 과목, 직무, 기술스택 등 정적 학사/직무 데이터를 로드·조회 |

## Internal AI Boundary

Spring Boot 는 FastAPI AI 중계 서버를 내부 서비스로만 호출합니다.

| 헤더 | 의미 |
|---|---|
| `X-Internal-Token` | Spring ↔ FastAPI 사이의 내부 호출 인증 토큰 |
| `X-User-Id` | Spring 이 JWT 로 검증한 사용자 ID 를 AI 서버에 전달하는 envelope 헤더 |

앱은 FastAPI 서버를 직접 호출하지 않습니다. 사용자 인증과 도메인 데이터 접근은 Spring 에서 끝내고, AI 서버는 추천·챗봇 그래프 실행에 집중합니다.

## Recommendation Cache & Invalidation

추천 생성 API 는 `forceRefresh=false` 일 때 사용자의 최신 `ACTIVE` 추천이 있으면 AI 서버를 다시 호출하지 않고 기존 추천을 조립해 반환합니다. `forceRefresh=true` 이거나 활성 추천이 없으면 새 추천을 생성하고, 기존 활성 추천은 `SUPERSEDED` 처리합니다.

이수 과목이 추가·삭제되면 `CompletedSubjectsChangedEvent` 가 발행되고, `RecommendationInvalidationListener` 가 해당 사용자의 활성 추천을 무효화합니다. 이후 사용자가 다시 추천을 요청하면 변경된 이수 과목 기준으로 새 추천이 생성됩니다.

## Domain Packages

| Package | 역할 |
|---|---|
| `auth` | 회원가입, 로그인, JWT 기반 인증 진입점 |
| `profile` | 온보딩, 사용자 프로필, 이수 과목 관리 |
| `recommendation` | 추천 생성 오케스트레이션, AI 응답 영속화, 리포트 조립, 추천 무효화 |
| `chatbot` | 사용자 프로필 컨텍스트를 조립해 AI 챗봇 서버로 중계 |
| `briefing` | AI 직무 브리핑 서버 호출과 응답 변환 |
| `catalog` | 학사 조직, 트랙, 과목, 직무, 기술스택 카탈로그 엔티티와 seed loader |
| `user` | 사용자 엔티티, 인증 principal, 사용자 조회 |
| `global` | 응답 envelope, 예외 처리, 보안, WebClient, request-id 등 공통 인프라 |

## Main APIs

| Method | Endpoint | 역할 |
|---|---|---|
| `POST` | `/api/v1/recommendations` | 인증 사용자의 온보딩 데이터로 추천을 생성하거나 활성 추천을 반환 |
| `GET` | `/api/v1/recommendations/report` | 활성 추천 기준 분석 리포트 조회. `anchorJobCode` 로 기준 직무 선택 가능 |
| `POST` | `/api/v1/chatbot/message` | 사용자 프로필 컨텍스트를 포함해 챗봇 AI 서버로 메시지 중계 |
| `GET` | `/api/v1/me/profile` | 인증 사용자의 프로필, 트랙, 관심사, 기술스택, 이수 과목 조회 |

## Team Roles

| Member | Backend-facing Role |
|---|---|
| [**이재원**](https://github.com/jwon0523) | Backend architecture, Spring Boot-FastAPI internal boundary, recommendation/report API design, project coordination |
| [**정종진**](https://github.com/ThreeeJ) | Chatbot domain contract, AI response integration perspective, job-competency data contract |
| [**박성훈**](https://github.com/parkseonghun598) | Frontend integration perspective, API response validation from mobile client flow |
| [**전종현**](https://github.com/J2H3233) | Research/RAG requirements alignment, backend data requirements from AI retrieval pipeline |

## Quick Start

```
React Native 앱  ──►  tracktory-backend (이 레포)  ──►  FastAPI AI 중계 서버
                      인증 · 프로필 · 도메인 · 트랜잭션         추천 · 자연어 설명 등 AI
```

- 앱과 직접 통신하는 유일한 백엔드 — 인증/인가, 프로필·도메인 CRUD, 트랜잭션, API 게이트키핑.
- AI 처리는 직접 수행하지 않고 `WebClient` 로 내부 AI 중계 서버에 위임한다.
- 도메인별 기능 범위와 패키지 구조는 코드(`src/main/java/com/hansung/tracktory/domain/`)와 [`CLAUDE.md`](./CLAUDE.md) 의 디렉토리 구조 참조.

## Getting Started

### 사전 요구사항

| 도구 | 버전 | 비고 |
|---|---|---|
| JDK | Temurin 21 | `asdf install java temurin-21.0+0` 또는 SDKMAN. Gradle 버전은 wrapper 가 핀 |
| Docker | — | 로컬 MySQL 을 compose 로 띄운다 |
| pre-commit | 4.x | 커밋 hook (아래 4단계) |

### 1. Secret 설정

```bash
cp src/main/resources/application-secret.yaml.example \
   src/main/resources/application-secret.yaml
```

복사한 `application-secret.yaml` 을 채운다. 로컬 DB 값은 아래 compose 설정과 맞춘다:

| 키 | 로컬 값 |
|---|---|
| `DATABASE_URL` | `jdbc:mysql://localhost:3306/tracktory` |
| `DATABASE_USERNAME` | `root` |
| `DATABASE_PASSWORD` | `1234` (compose 기본값) |
| `jwt.secret` | Base64 인코딩 키 (직접 생성) |
| `ai-relay.base-url` | AI 중계 서버 주소 (기본 `http://localhost:8000`) |
| `ai-relay.internal-token` | AI 서버와 공유하는 내부 토큰 |

> `application-secret.yaml` 은 gitignore 대상 — **커밋 금지**. 키 목록의 정본은 `*.example` 파일.

### 2. 로컬 인프라 기동 (MySQL)

```bash
docker compose up -d mysql        # localhost:3306, DB=tracktory
```

> brew 의 `mysql@8.4` 와 3306 이 충돌하면: `brew services stop mysql@8.4`

### 3. pre-commit hook 등록 (최초 1회)

```bash
brew install pre-commit           # 또는: pipx install pre-commit
pre-commit install
```

등록되는 hook: **gitleaks**(시크릿 하드코딩 차단) · **spotless-check** · **checkstyle**.

### 4. 실행

```bash
./gradlew bootRun                 # http://localhost:8080
```

## Build / Test / Lint

```bash
./gradlew build                   # 컴파일 + 검사 + 테스트
./gradlew test                    # 테스트만
./gradlew check                   # 전체 게이트 (spotless + checkstyle + test)
./gradlew spotlessApply           # 포매터 자동 수정
```
