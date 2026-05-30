# tracktory-backend

tracktory 메인 백엔드 (Spring Boot 4.0.x · Java 21).
사용자 인증·프로필·이수 과목·트랙/직무/과목 도메인을 담당하며, AI 처리는 내부 중계 서버 (FastAPI) 로 위임한다.

상세 컨벤션은 [`CONTRIBUTING.md`](./CONTRIBUTING.md), AI 에이전트 컨텍스트는 [`CLAUDE.md`](./CLAUDE.md) 참조.

---

## Quick Start

### 1. 사전 요구사항

| 도구 | 버전 | 설치 |
|---|---|---|
| JDK | Temurin 21 | `asdf install java temurin-21.0+0` 또는 SDKMAN |
| Gradle | wrapper 가 핀 (시스템 gradle 불필요) | — |
| pre-commit CLI | 4.x | 아래 §2 참조 |

### 2. pre-commit 설치 (최초 1회)

본 레포는 [pre-commit framework](https://pre-commit.com) 으로 커밋 hook 을 관리한다.
**도구 자체는 Python 으로 작성됐지만 검사 대상은 100% Java** — Spotless / Checkstyle / gitleaks 가 `./gradlew` 명령을 wrapping 한다.

#### 2.1 CLI 설치 (둘 중 택1)

**macOS (Homebrew, 권장)**:
```bash
brew install pre-commit
```

**크로스 플랫폼 (pipx)**:
```bash
pipx install pre-commit
# pipx 가 없으면: brew install pipx 또는 python3 -m pip install --user pipx
```

설치 확인:
```bash
pre-commit --version   # 4.x 출력되면 OK
```

#### 2.2 레포에 hook 등록

```bash
pre-commit install
# → .git/hooks/pre-commit 생성. 이후 git commit 시 자동 작동
```

#### 2.3 작동 확인 (선택)

```bash
pre-commit run --all-files   # 전체 파일 1회 검사 (Spotless 는 ratchetFrom 기준만 검사)
```

등록되는 hook:
- **gitleaks** — 시크릿 하드코딩 차단 (`.env` 값을 코드에 인라인하는 사고 방지)
- **spotless-check** — `*.java|kt|gradle` 변경 시 Google Java Format 검증
- **checkstyle** — `*.java` 변경 시 코드 스타일 검사

### 3. 빌드 / 실행

```bash
./gradlew build              # 컴파일 + 검사 + 테스트
./gradlew bootRun            # 로컬 실행 (http://localhost:8080)
./gradlew test               # 테스트만
./gradlew spotlessApply      # 포매터 자동 수정
./gradlew check              # 전체 게이트 (spotless + checkstyle + test)
```

---

## 트러블슈팅

### `pre-commit: command not found`
CLI 자체 설치 누락. §2.1 참조.

### `pre-commit install --install-hooks` 가 처음 실행에서 느리다
gitleaks 바이너리를 처음 받아오기 때문. 1회만 발생.

### Spotless 가 기존 코드에서도 실패한다고 외친다
`ratchetFrom 'origin/develop'` 기준이라 develop 에 있는 코드는 grandfather 되어야 정상.
`git fetch origin develop` 으로 base 히스토리를 받았는지 확인.

### CI 에서 Spotless 단계만 빨개진다
로컬에서 `./gradlew spotlessApply` 후 재커밋.
