#!/usr/bin/env bash
# SessionStart hook — 세션 시작 시 현재 브랜치 + 오픈 이슈 + 최근 커밋 자동 주입
#
# 트리거: matcher = "startup"
# 동작: additionalContext JSON 출력 → Claude Code 가 자동으로 컨텍스트에 추가
#
# 출력 정보:
#   - 현재 git 브랜치 + 추적 상태
#   - 최근 5개 커밋 (oneline)
#   - working tree 상태 (clean / dirty)
#   - 오픈 이슈 (gh CLI 인증된 경우 + 최근 5개)
#   - 현재 PR (있는 경우)
#
# exit 0 + JSON 표준출력

set -uo pipefail

# git 레포가 아니면 빈 컨텍스트
if ! git rev-parse --is-inside-work-tree >/dev/null 2>&1; then
  cat << 'EOF'
{
  "hookSpecificOutput": {
    "hookEventName": "SessionStart",
    "additionalContext": "(git 레포 외부 — 컨텍스트 주입 생략)"
  }
}
EOF
  exit 0
fi

BRANCH=$(git branch --show-current 2>/dev/null || echo "(detached)")
RECENT_LOG=$(git log --oneline -5 2>/dev/null || echo "(커밋 없음)")
TREE_STATUS=$(git status --short 2>/dev/null | wc -l | tr -d ' ')
if [[ "$TREE_STATUS" == "0" ]]; then
  TREE_DESC="clean"
else
  TREE_DESC="$TREE_STATUS files modified"
fi

# gh CLI 인증된 경우만 PR / 이슈 조회
PR_INFO=""
ISSUES_INFO=""
if command -v gh >/dev/null 2>&1 && gh auth status >/dev/null 2>&1; then
  # 현재 브랜치의 PR
  PR_NUMBER=$(gh pr view --json number --jq '.number' 2>/dev/null || echo "")
  if [[ -n "$PR_NUMBER" ]]; then
    PR_INFO="현재 PR: #$PR_NUMBER"
  fi

  # 최근 오픈 이슈 5개
  ISSUES_LIST=$(gh issue list --limit 5 --state open --json number,title --jq '.[] | "  #\(.number) \(.title)"' 2>/dev/null || echo "")
  if [[ -n "$ISSUES_LIST" ]]; then
    ISSUES_INFO=$(printf "오픈 이슈 (최근 5개):\n%s" "$ISSUES_LIST")
  fi
fi

# JSON 출력 (additionalContext 에 들어갈 텍스트는 escape 필요)
CONTEXT=$(cat << EOF
=== Session Start Context ===

브랜치: $BRANCH ($TREE_DESC)

최근 커밋:
$RECENT_LOG

$PR_INFO

$ISSUES_INFO

(SessionStart hook 자동 주입 — 매 세션 시작 시 갱신)
EOF
)

# JSON escape — Python 도 jq 도 없는 환경 대비 단순 처리
ESCAPED=$(printf '%s' "$CONTEXT" | python3 -c 'import json,sys; print(json.dumps(sys.stdin.read()))' 2>/dev/null || \
          printf '%s' "$CONTEXT" | sed 's/\\/\\\\/g; s/"/\\"/g; s/$/\\n/' | tr -d '\n' | sed 's/\\n$//' | (echo -n '"' && cat && echo -n '"'))

cat << EOF
{
  "hookSpecificOutput": {
    "hookEventName": "SessionStart",
    "additionalContext": $ESCAPED
  }
}
EOF

exit 0
