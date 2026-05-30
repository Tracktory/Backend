# Contributing to tracktory-backend

이 문서는 본 레포에 코드를 추가하는 팀원이 지켜야 할 규칙을 담고 있습니다.
**사람이 판단해야 하는 규칙** 만 정리했습니다. 포매팅·import 순서·네이밍 등 기계가 강제할 수 있는 것은 린터·타입체커 설정이 자동으로 잡습니다.

## 목차

1. [개발 루틴](#1-개발-루틴)
2. [커밋 메시지 규칙](#2-커밋-메시지-규칙)
3. [언어 기본 규약](#3-언어-기본-규약)
4. [프레임워크 / 도메인 규약](#4-프레임워크--도메인-규약)
5. [테스트 규약](#5-테스트-규약)

---

## 1. 개발 루틴

### 도구 체인

| 목적 | 도구 | 명령어 |
|---|---|---|
| 패키지 관리 | gradle | ./gradlew dependencies --refresh-dependencies |
| 포매터 | spotless | ./gradlew spotlessApply |
| 린터 | checkstyle | ./gradlew check |
| 타입 체커 | javac | ./gradlew compileJava |
| 테스트 | junit-jupiter | ./gradlew test |
| Lock 검증 | gradle | ./gradlew dependencies --write-locks |

### 커밋 전 루틴 (pre-commit)

최초 1회 설치:

```bash
pre-commit install
```

설치 후에는 `git commit` 시 lint + 타입체크가 **자동 실행** 됩니다. 수동 실행이 필요하면:

```bash
./gradlew spotlessApply
./gradlew check
./gradlew compileJava
./gradlew test
./gradlew dependencies --write-locks
```

### Java/Spring 추가 단계

#### Gradle wrapper 사용

`./gradlew` 사용 — wrapper 가 Gradle 버전을 핀. 시스템 gradle 호출 X.

```bash
./gradlew build              # 컴파일 + 검사 + 테스트
./gradlew bootRun            # 로컬 실행
./gradlew test               # 테스트만
./gradlew spotlessApply      # 포매터 적용
./gradlew spotlessCheck      # 포매터 검증 (CI)
```

#### JDK 버전 핀

`.tool-versions` (asdf) 또는 `jenv` / `sdk` (SDKMAN) 으로 JDK 버전 명시:

```
# .tool-versions
java temurin-21.0+0
```

CI 의 `actions/setup-java@v4` 와 일치해야 재현성 확보.

#### Spotless 자동 적용

본 레포의 spotless 는 `ratchetFrom 'origin/develop'` 로 설정되어 **이번 브랜치에서 변경한 파일만** 포매팅을 강제합니다 (develop 에 이미 있는 코드는 일괄 재포맷 X). 커밋 전:

```bash
./gradlew spotlessApply       # 변경 파일 자동 포매팅 적용
git add -u
```

또는 pre-commit hook 의 `spotless-check` 가 실패하면 위 명령 후 재시도.

#### dependency lock

본 레포는 Gradle dependency locking 활성화. 의존성 추가/변경 시:

```bash
./gradlew dependencies --write-locks   # lock 갱신
```

`gradle.lockfile` 이 갱신되며 git 추적 대상 (변경 시 PR 에 포함).

**머지 충돌 시** — `gradle.lockfile` 은 `.gitattributes` 에서 `merge=binary` 로 지정되어 줄 단위 자동 병합이 차단됩니다 (잘못 섞이면 어느 쪽 의존성 해석에도 대응하지 않는 깨진 lock 이 생기기 때문). 충돌 시 lock 을 직접 재생성합니다:

```bash
# 1. build.gradle 의 의존성 충돌을 먼저 사람이 해결 (어느 라이브러리를 살릴지 판단)
# 2. build.gradle 기준으로 lockfile 전체 재생성
./gradlew dependencies --write-locks
# 3. 재생성 결과를 스테이징하고 머지 계속
git add gradle.lockfile
git merge --continue   # rebase 중이면 git rebase --continue
```

lockfile 은 편집 대상이 아니라 **재생성 대상** — 충돌난 줄을 손으로 맞추지 말고 `build.gradle` 을 정답으로 두고 도구가 다시 뽑게 합니다.

<!-- === LANG_DEV_ROUTINE_HERE === (lang overlay 가 언어별 추가 단계 / 핵심 수치를 여기에 추가) -->

---

## 2. 커밋 메시지 규칙

### 포맷

```
[TYPE] 작업 내용
```

### Types

| Type | 용도 |
|---|---|
| `feat` | 새 기능 |
| `fix` | 버그 수정 |
| `refactor` | 리팩토링 |
| `docs` | 문서 |
| `chore` | 기타 (빌드, 설정, 의존성 등) |
| `test` | 테스트 추가/수정 |
| `design` | UI/디자인 시스템 |

### 예시

```
feat: 사용자 인증 모듈 추가
fix: 토큰 갱신 시 race condition 수정
refactor: 컴포넌트 단일 책임으로 분리
docs: README에 실행 방법 추가
chore: 의존성 업데이트
test: 통합 테스트 추가
```

### 원칙

- 제목은 한글 또는 영문 모두 허용, **50자 이내** 권장
- 본문이 필요하면 제목 다음 빈 줄 후에 작성
- 하나의 커밋은 하나의 논리적 변경만
- **스쿼시 머지를 전제**로 한다. 브랜치 안의 개별 커밋은 title-only 로 충분하며, `Closes #N` 은 브랜치 커밋이 아닌 **PR 본문** 에 적는다.

### 브랜치 네이밍

브랜치명은 `{type}/{issue-number}` 형식으로 통일.

- `{type}`: 커밋 타입과 동일 (`feat`, `fix`, `refactor`, `docs`, `chore`, `test`, `design`)
- `{issue-number}`: 해당 브랜치의 작업 단위가 되는 GitHub 이슈 번호

예시: `feat/42`, `fix/87`, `chore/103`

### 이슈 / PR 제목

**커밋 메시지는 소문자 prefix**, **이슈·PR 제목은 대문자 prefix** 로 작성.

| 대상 | 형식 | 예시 |
|---|---|---|
| **커밋 메시지** | `{type}: 작업 내용` (소문자) | `chore: 프로젝트 초기 세팅` |
| **이슈 제목** | `{이모지} {Type}: 작업 내용` | `🛠️ Chore: 이슈 템플릿 분리` |
| **PR 제목** | `{이모지} [{Type}] 작업 내용` | `🛠️ [Chore] 프로젝트 초기 세팅` |

이슈 제목은 `.github/ISSUE_TEMPLATE/*.yml` 에 이모지 + `Type:` 형태로 하드코딩되어 있으므로 템플릿을 그대로 쓰면 규약에 맞음.

---

## 3. 언어 기본 규약

도구가 못 잡지만 팀이 지켜야 하는 것만 나열합니다.

### 3.1 명시적 타입 / Records / Sealed

- **Java 17+ Records 활용** — DTO / 값 객체는 record 로 (불변 + boilerplate 제거)
- **Sealed class/interface 활용** — 도메인 ADT (대안 합) 표현
- `var` 는 internal 변수에서만, 공개 API 시그니처에는 명시 타입

```java
// ✅ Do
public record UserCreatedEvent(UUID userId, Instant occurredAt) {}

public sealed interface PaymentResult
    permits PaymentResult.Success, PaymentResult.Failed { ... }

// ❌ Don't (mutable POJO + Lombok @Data 남발 — 동일성 / equals 모호)
@Data public class UserCreatedEvent {
    private UUID userId;
    private Instant occurredAt;
}
```

### 3.2 Javadoc / Documentation

- **public API**: Javadoc 필수 (`@param`, `@return`, `@throws`)
- **"왜 존재하는가"** 가 핵심. 자명한 getter/setter 는 생략

### 3.3 가시성

- 기본은 `package-private` (생략). 외부 노출 의도 없으면 그대로
- `private` 적극 — 의도된 캡슐화 신호
- `public` 은 외부 모듈 약속 — 변경 시 호출자 영향 검토

### 3.4 예외 처리

- **Checked vs Unchecked**: 호출자가 합리적으로 처리할 수 있는 것만 checked. 그 외 RuntimeException
- catch-all (`catch (Exception e)`) 지양. 구체 타입 catch
- 예외를 삼키지 말 것 — 재발생 또는 로깅 후 상위로 전파
- 비즈니스 예외는 전용 클래스 (`extends RuntimeException` 또는 `extends BusinessException`)

```java
// ✅ Do
try {
    userService.save(user);
} catch (DuplicateUserException e) {
    log.warn("user already exists: {}", e.getUsername(), e);
    throw e;
}

// ❌ Don't
try {
    userService.save(user);
} catch (Exception e) {
    return null;  // 예외 삼킴
}
```

### 3.5 Spring 특화

- **Constructor injection** 만 사용 (필드 injection / setter injection X — 불변성 + 테스트 용이성)
- `@Component` / `@Service` / `@Repository` 의도에 맞게 사용
- `@Autowired` 명시 생략 (생성자 1개면 자동)
- 트랜잭션은 service 계층에서만 (`@Transactional`). controller / repository 에 직접 X

```java
// ✅ Do
@Service
public class UserService {
    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Transactional
    public User createUser(CreateUserCommand cmd) { ... }
}
```

### 3.6 핵심 수치

- Java 버전: **21** (Spring Boot 4.x 권장 LTS)
- 들여쓰기: **2 spaces** (Google Java Format 기본 — Spotless `googleJavaFormat()`)
- 라인 길이: **120** (checkstyle `LineLength`)
- 파일당 클래스 수: **1** (public 클래스 한정)
- star import 금지 (checkstyle `AvoidStarImport`)

<!-- === LANG_RULES_HERE === (lang overlay 가 언어별 규약 본문을 여기에 삽입) -->

---

## 4. 프레임워크 / 도메인 규약

- **응답 envelope 일원화** — 컨트롤러 응답은 `global/response` 의 `ApiResponse` 로 감싼다. 직접 `ResponseEntity` raw body 반환 지양.
- **예외 처리 일원화** — 비즈니스 예외는 `BusinessException` + `ErrorCode` 로 표현하고, 변환은 `GlobalExceptionHandler` 단일 지점에서. 컨트롤러/서비스에 try/catch 분산 금지.
- **요청 추적** — `X-Request-Id` 는 `RequestIdFilter` 가 전파. 로깅 시 MDC 컨텍스트 활용.
- **AI 중계 호출** — 내부 AI 서버 호출은 `WebClient` (`WebClientConfig`) 경유. 내부 인증 헤더 규약을 따른다 (인증 토큰은 환경변수, 코드 인라인 금지).

---

## 5. 테스트 규약

### 일반 원칙 (언어 무관)

- **단위 테스트**: 외부 I/O는 mock. 순수 함수 입출력 비교.
- **통합 테스트**: 실제 의존성 사용. CI 비용·flakiness 고려해 마커로 분리.
- `System.out.println` 금지, 로깅(slf4j) 사용
- 실제 네트워크/DB 의존 테스트는 별도로 구분 (예: `@Tag("integration")`)

---

## 변경 이력

| 버전 | 날짜 | 변경자 | 변경 내용 |
|---|---|---|---|
| 0.1 | 2026-05-30 | (initial) | harness-template 기반 초기 작성 |
| 0.2 | 2026-05-30 | 이재원 | dependency lock 섹션에 머지 충돌 시 lockfile 재생성 절차 추가 |
