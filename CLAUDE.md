# tracktory-backend — Claude Code 컨텍스트

> 본 파일은 Claude Code 세션이 이 레포에서 작업할 때 자동 로드되는 프로젝트 컨텍스트입니다.
> Codex / 기타 AI 에이전트와의 호환을 위해 본문은 **zone marker** 로 구분되며, 향후 AGENTS.md 자동 분리에 사용됩니다.

---

<!-- ZONE: SHARED — copies to AGENTS.md verbatim. 어떤 에이전트가 읽어도 동일하게 유효한 정보 -->

## 프로젝트 개요

tracktory 메인 백엔드. Spring Boot 기반으로 사용자 인증·프로필·이수 과목·트랙/직무/과목 도메인과 추천 결과 API 를 담당하며, AI 중계 서버와 내부 통신한다. 앱과 직접 통신하는 유일한 백엔드이며, AI 처리는 내부 중계 서버로 위임한다.

- **언어**: java 21
- **프레임워크**: Spring Boot 4.0.x
- **패키지 매니저**: gradle (wrapper 9.4.x)
- **테스트 러너**: junit-jupiter
- **CI**: github_actions

## 빌드 / 테스트 / 린트 명령

```bash
# 의존성 설치
./gradlew dependencies --refresh-dependencies

# 포매터
./gradlew spotlessApply

# 린터 (spotlessCheck + checkstyle + test 포함)
./gradlew check

# 타입 체커 (컴파일)
./gradlew compileJava

# 테스트
./gradlew test
```

상세 규약은 [`CONTRIBUTING.md`](./CONTRIBUTING.md) 참조.

## 디렉토리 구조

```
src/main/java/com/hansung/tracktory/
├── TracktoryApplication.java        # 엔트리포인트
└── global/                          # 공통 인프라 (도메인 무관)
    ├── config/                      # WebConfig, WebClientConfig
    ├── entity/                      # BaseEntity (공통 감사 필드)
    ├── filter/                      # RequestIdFilter (X-Request-Id 전파)
    ├── exception/                   # ErrorCode, BusinessException, GlobalExceptionHandler
    └── response/                    # ApiResponse envelope, ErrorPayload, ApiResponseBodyAdvice
src/main/resources/
├── application.yaml
├── application-secret.yaml.example  # 시크릿 템플릿 (실제 값은 gitignored)
└── logback-spring.xml
src/test/java/com/hansung/tracktory/
config/checkstyle/checkstyle.xml     # 코드 스타일 규칙
```

<!-- /ZONE: SHARED -->

---

<!-- ZONE: CLAUDE-ONLY — Claude Code 전용 (slash commands, hooks, skills, agents). 다른 에이전트는 무시 -->

## Claude Code 통합

### 슬래시 커맨드
- `/commit` — 커밋 메시지 검증 + 자동 작성 ([.claude/commands/commit.md](./.claude/commands/commit.md))
- 기타: [`.claude/commands/`](./.claude/commands/) 참조

### 도구 경계 (settings.json)
권한 허용/거부 규칙은 [`.claude/settings.json`](./.claude/settings.json) 에 정의. `.env*`, lock 파일, 운영 시크릿은 항상 거부.

### Hooks
- **PreToolUse**: 외부 가시 액션 (`gh pr create`, standalone PR comment) 차단 — [.claude/hooks/block-pr-create.sh](./.claude/hooks/block-pr-create.sh)
- **SessionStart**: 현재 브랜치 + 오픈 이슈 컨텍스트 자동 주입 — [.claude/hooks/session-start-context.sh](./.claude/hooks/session-start-context.sh)

### 서브에이전트
- [.claude/agents/code-reviewer.md](./.claude/agents/code-reviewer.md) — 코드 리뷰 전문 에이전트
- [.claude/agents/pr-reviewer.md](./.claude/agents/pr-reviewer.md) — PR 리뷰 전용

### 컨텍스트 압축 지침 (`/compact`)
컴팩션 시 반드시 보존:
- 수정된 파일 전체 경로 목록
- 현재 구현 중인 컴포넌트 / 기능
- 실행한 테스트 명령과 결과 요약
- 열린 TODO / 미해결 결정 사항

<!-- /ZONE: CLAUDE-ONLY -->

---

<!-- ZONE: CODEX-ONLY — Codex / OpenAI 에이전트 전용. v0.1 비어있음, v0.2+ 분리 시 사용 -->

<!-- (v0.1 단계 — 비어있음. v0.2 의 zone-aware 자동 분리에서 채워짐) -->

<!-- /ZONE: CODEX-ONLY -->

---

<!-- ZONE: PROJECT-DOMAIN — 프로젝트 도메인 자유 영역. 비즈니스 규칙, 아키텍처 다이어그램 링크, 도메인 용어 등 -->

## 아키텍처 철학

1. **공통 인프라 격리** — 응답 envelope·예외·요청 추적 등 도메인 무관 인프라는 `global/` 에만 둔다. 도메인 패키지가 `global/` 을 의존하되 역방향은 금지.
2. **응답 일원화** — 모든 컨트롤러 응답은 `ApiResponse` envelope 으로 감싸고, 예외는 `GlobalExceptionHandler` 에서 단일 처리한다 (컨트롤러에서 try/catch 분산 금지).
3. **계층 책임 분리** — 트랜잭션은 service 계층(`@Transactional`)에서만. controller 는 요청/응답 DTO 변환, repository 는 영속성만.
4. **AI 위임 경계** — AI 처리(추천·설명 생성 등)는 내부 중계 서버로 위임하고, 본 백엔드는 인증·도메인·트랜잭션·API 게이트키퍼 역할에 집중한다.

## 코드 작업 시 체크리스트

새로운 코드를 제안하기 전에 다음을 확인합니다:

- [ ] 컴포넌트가 단일 책임인가? (controller/service/repository 경계 준수)
- [ ] 외부 I/O 가 순수 로직과 섞이지 않았는가?
- [ ] public API 에 타입 정보와 Javadoc 이 있는가?
- [ ] 커밋 전 루틴 (`./gradlew spotlessApply` → `./gradlew check`) 을 안내했는가?

<!-- /ZONE: PROJECT-DOMAIN -->

---

## 하네스 메타

본 프로젝트는 [`harness-template`](https://github.com/jwon0523/harness-template) 기반으로 하네스 적용됨.
재적용 (drift 감지 + idempotent 머지): `/apply-harness` 또는 [apply 프롬프트](https://github.com/jwon0523/harness-template/tree/main/apply) 사용.
