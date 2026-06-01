# tracktory-backend

[![CI](https://github.com/Tracktory/Backend/actions/workflows/ci.yml/badge.svg)](https://github.com/Tracktory/Backend/actions/workflows/ci.yml)

tracktory 의 메인 백엔드. 모바일 앱이 직접 통신하는 **유일한** API 서버로, 인증·도메인·트랜잭션을 담당하고 AI 처리(추천·설명 생성 등)는 내부 AI 중계 서버에 위임한다.

**Stack**: Java 21 · Spring Boot 4.x · Gradle (wrapper) · MySQL 8.4

## 시스템 내 위치

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

전체 개발 루틴·도구 체인·코드 규약은 [`CONTRIBUTING.md`](./CONTRIBUTING.md) 참조.

## 트러블슈팅

| 증상 | 해결 |
|---|---|
| `bootRun` 이 DB 연결 실패 | MySQL 이 떴는지(`docker compose ps`) + `application-secret.yaml` 의 `DATABASE_URL` 확인 |
| `pre-commit: command not found` | CLI 미설치 — 위 3단계 참조 |
| 첫 `pre-commit install` 이 느림 | gitleaks 바이너리 최초 다운로드, 1회성 |
| Spotless 가 기존 코드에서도 실패 | `ratchetFrom 'origin/develop'` 기준이라 develop 히스토리 필요 — `git fetch origin develop` |
| CI 에서 Spotless 단계만 실패 | 로컬 `./gradlew spotlessApply` 후 재커밋 |

## 더 보기

| 무엇 | 어디 |
|---|---|
| 기여 규약 · 커밋/브랜치 · 테스트 규약 | [`CONTRIBUTING.md`](./CONTRIBUTING.md) |
| 아키텍처 철학 · 디렉토리 구조 · AI 에이전트 컨텍스트 | [`CLAUDE.md`](./CLAUDE.md) |
| 아키텍처 결정 기록 (ADR) | [`docs/adr/`](./docs/adr/README.md) |
