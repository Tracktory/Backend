---
name: commit
description: CONTRIBUTING.md 규약에 맞춰 staged 변경을 커밋. 타입 추론 + 제목 초안 + 사용자 승인 후 git commit. 키워드 keywords - commit, 커밋, 스테이징, staged, conventional commit.
---

# Commit

## Purpose

`CONTRIBUTING.md` §2 의 커밋 메시지 규약을 자동으로 강제하는 슬래시 커맨드. staged 변경을 분석해 타입을 추론하고 50자 이내 제목 초안을 제안한 뒤, 사용자 승인을 거쳐 커밋한다.

## Usage

```
/commit                              # diff 분석 → 타입 추론 + 제목 초안 → 승인 → 커밋
/commit feat                         # 타입 명시, 제목만 추론
/commit feat "트랙 임베딩 노드 추가"   # 타입 + 제목 모두 명시 (검증만)
/commit feat --body                  # 제목 승인 후 본문 입력 단계 추가
```

**허용 타입** (CONTRIBUTING.md §2):

| Type | 용도 |
|---|---|
| `feat` | 새 기능 |
| `fix` | 버그 수정 |
| `refactor` | 리팩토링 |
| `docs` | 문서 |
| `chore` | 빌드/설정/의존성 등 기타 |
| `test` | 테스트 추가/수정 |
| `design` | UI/디자인 시스템 |

---

## Workflow

### Step 1: 입력 파싱
- 인자 0개 → 타입·제목 모두 추론
- 첫 인자가 7타입 중 하나 → 타입 고정
- 두 번째 인자에 따옴표 문자열 → 제목 고정 (검증만)
- `--body` 플래그 → 제목 승인 후 본문 입력 단계 추가

### Step 2: Staged 변경 확인

```bash
git diff --cached --name-only
```

비어있으면 안내 후 종료. 본 커맨드는 `git add -A` / `git add .` 를 절대 실행하지 않는다 — 사용자 staging 신뢰.

### Step 3: 브랜치 정합성 (soft warn)

```bash
BRANCH=$(git branch --show-current)
```

브랜치 네이밍 규칙 `^(feat|fix|refactor|docs|chore|test|design)/\d+` 미매치 시 경고만 (차단 X):

| 현재 브랜치 | 경고 |
|---|---|
| `develop` / `main` / `master` | `⚠️ 메인 브랜치 위에서 커밋 중. issue → {type}/{N} branch → commit → PR 워크플로 확인` |
| 패턴 X | `⚠️ 브랜치명이 {type}/{issue-number} 패턴과 다름. 의도된 것이라면 무시` |
| 패턴 O | 경고 없음 |

### Step 4: 타입 결정

우선순위:
1. 인자로 명시된 타입 — `/commit feat` 의 `feat`
2. 브랜치명 prefix — `feat/42` → `feat`
3. diff 휴리스틱 fallback:

| 변경 패턴 | 추론 타입 |
|---|---|
| `*.md` 만 변경 | `docs` |
| `tests/` 또는 `test/` 하위만 변경 | `test` |
| 빌드 설정 / lock / `.github/` / pre-commit 설정 | `chore` |
| 신규 파일 또는 신규 함수/클래스 추가 | `feat` |
| 기존 코드 수정 + diff 에 "fix" / "버그" / "오류" / "수정" 키워드 | `fix` |
| 기존 코드 구조 변경 (분리/리네임/이동) without 동작 변화 | `refactor` |
| 그 외 | 사용자 선택 요청 |

<!-- === LANG_COMMIT_HEURISTICS_HERE === (lang overlay 가 언어별 빌드 설정 파일명을 추가) -->

### Step 5: 제목 초안 생성

```bash
git diff --cached
git diff --cached --stat
```

- **형식**: `{type}: {요약}` (소문자 prefix + 콜론 + 공백)
- **언어**: 기본 한글 (CONTRIBUTING.md 예시 일관)
- **길이**: 50자 이내 권장 (초과 시 경고만)
- **포커스**: "왜" 보다는 "무엇" — 본문(--body)에서 "왜" 보충

### Step 6: 사용자 승인

```
제안 메시지: feat: 사용자 인증 모듈 추가

이 메시지로 커밋할까요? (y / 수정 / 취소)
```

수정 입력 시 50자 초과 알림하되 강제 X.

### Step 7: (옵션) 본문 입력

`--body` 플래그 시만. 가이드:
- "왜" 위주
- `Closes #N` / `Resolves #N` 은 **여기에 넣지 말 것** — 스쿼시 머지 전제이므로 PR 본문에만

### Step 8: 커밋 실행

```bash
git commit -m "{title}"               # 본문 없을 때
git commit -m "{title}" -m "{body}"   # --body 사용 시
```

**금지 사항**:
- `--no-verify` 절대 X — pre-commit 훅이 lint/typecheck 자동 실행
- `--amend` X — 새 커밋 생성이 원칙 (훅 실패 시 amend 는 이전 커밋 덮어씀)
- `Co-Authored-By` 트레일러 X — 스쿼시 머지 후 잡음
- `git add -A` / `git add .` X — staging 은 사용자 책임

**훅 실패 시**:
1. 실패 출력 그대로 사용자에게 표시
2. 안내: 훅 출력 확인 → 코드 수정 → 다시 stage → `/commit` 재실행

### Step 9: 성공 출력

```bash
git log -1 --pretty=format:"%h %s"
```

```
✓ 커밋 완료: <hash> <title>

다음 단계:
  - 추가 커밋: 새 변경 stage 후 /commit
  - 푸시: git push
  - PR 발행: 사용자가 직접 슬래시 명령으로 (예: /pr) — Claude 는 PR 을 직접 생성하지 않음
```

---

## 원칙

- **Co-Authored-By / Generated with Claude Code 트레일러 X** — 스쿼시 머지 후 잡음
- **사용자 staging 신뢰** — `git add -A` 자동 실행 X
- **pre-commit 훅 신뢰** — 본 커맨드가 lint 직접 실행 X (중복)
- **`--no-verify` X** — 훅 실패 시 코드 수정이 정답
- **단일 논리 변경** — 1회 호출 = 1 커밋
- **PR 직접 생성 X** — 커밋 + push 까지만, PR 은 사용자 슬래시
