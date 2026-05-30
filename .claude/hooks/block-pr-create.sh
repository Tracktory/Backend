#!/usr/bin/env bash
# PreToolUse hook — 외부 가시 액션 (PR 생성 / standalone PR comment) 차단
#
# 트리거: matcher = "Bash"
# 동작: hook 입력 JSON 의 tool_input.command 를 검사. 다음 패턴이면 차단:
#   - gh pr create ...
#   - gh api .../pulls/{N}/comments  (standalone POST — Submit review 우회)
#   - gh pr comment ...
#
# 차단 시 stderr 에 사유 출력 + exit 2 → Claude Code 가 자동으로 사용자에게 사유 표시 후 호출 abort.
# 통과 시 exit 0.
#
# 우회 시 (사용자 명시 동의):
#   환경변수 HARNESS_ALLOW_PR_CREATE=1 또는 HARNESS_ALLOW_PR_COMMENT=1 설정 후 hook 재호출

set -euo pipefail

# stdin 으로 JSON 입력 받음
INPUT=$(cat)
COMMAND=$(echo "$INPUT" | jq -r '.tool_input.command // ""')

# Pattern 1: gh pr create
if echo "$COMMAND" | grep -qE '^[[:space:]]*gh[[:space:]]+pr[[:space:]]+create'; then
  if [[ "${HARNESS_ALLOW_PR_CREATE:-0}" != "1" ]]; then
    cat >&2 << 'EOF'
🛑 차단됨: gh pr create — PR 발행 가드

PR 발행은 슬래시 트리거(/pr) + 직전 명시 승인 + affirmative 응답 3중 조건 필요.
본 hook 은 자발 발행을 원천 차단합니다 (mistakes-log 사례 3 — PR #42 standalone 발행 재발 방지).

우회 (사용자 직접 동의 시):
  HARNESS_ALLOW_PR_CREATE=1 gh pr create ...
EOF
    exit 2
  fi
fi

# Pattern 2: gh api .../pulls/{N}/comments standalone POST (Submit review 우회)
if echo "$COMMAND" | grep -qE 'gh[[:space:]]+api[[:space:]]+.*pulls/[0-9]+/comments'; then
  # POST method 인지 확인 (-X POST 또는 method 미지정 + body 포함)
  if echo "$COMMAND" | grep -qE '\-X[[:space:]]+POST|\--method[[:space:]]+POST|\-f[[:space:]]+|\--field'; then
    if [[ "${HARNESS_ALLOW_PR_COMMENT:-0}" != "1" ]]; then
      cat >&2 << 'EOF'
🛑 차단됨: standalone PR comment 발행

PR 리뷰 코멘트는 Pending Review 로 묶어 사용자가 Submit 해야 함.
standalone POST /pulls/{N}/comments 는 알림 노이즈 + 검수 우회.

대안: POST /repos/.../pulls/{N}/reviews (event 미지정) 로 Pending Review 생성 후
사용자가 직접 Submit (event 지정한 PATCH).

우회 (사용자 직접 동의 시):
  HARNESS_ALLOW_PR_COMMENT=1 gh api ...
EOF
      exit 2
    fi
  fi
fi

# Pattern 3: gh pr comment (인라인 코멘트 의도 — 일반 conversation 코멘트로 들어가므로 의미 X + 외부 가시)
if echo "$COMMAND" | grep -qE '^[[:space:]]*gh[[:space:]]+pr[[:space:]]+comment'; then
  if [[ "${HARNESS_ALLOW_PR_COMMENT:-0}" != "1" ]]; then
    cat >&2 << 'EOF'
🛑 차단됨: gh pr comment

리뷰 인라인 의도라면 Pending Review 패턴 사용 (Pattern 2 메시지 참조).
일반 conversation 코멘트라면 사용자 명시 동의 필요.

우회: HARNESS_ALLOW_PR_COMMENT=1 gh pr comment ...
EOF
    exit 2
  fi
fi

# 통과
exit 0
