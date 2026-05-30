# tracktory-backend — AI 에이전트 컨텍스트 (AGENTS.md 표준)

> 본 파일은 [agents.md](https://agents.md) 표준에 따라 모든 AI 에이전트(Codex, Cursor, GitHub Copilot, Claude Code 등) 가 공통으로 읽는 컨텍스트.
> **본 파일은 `CLAUDE.md` 의 ZONE: SHARED + ZONE: PROJECT-DOMAIN 으로부터 자동 생성됨** — 직접 수정 X. 변경은 `CLAUDE.md` 에 가하고 `apply/strategies/zone-extract.sh` 재실행.

---

<!-- AUTO-GENERATED FROM CLAUDE.md — DO NOT EDIT BY HAND -->
<!-- Source zones: ZONE: SHARED, ZONE: PROJECT-DOMAIN -->
<!-- Generator: apply/strategies/zone-extract.sh -->


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

---


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

---

## 하네스 메타

본 프로젝트는 [\`harness-template\`](https://github.com/jwon0523/harness-template) 기반.
