#!/usr/bin/env bash
# PreToolUse hook — 브랜치명 컨벤션 강제 (CONTRIBUTING.md §브랜치 네이밍)
#
# 트리거: matcher = "Bash"
# 동작: hook 입력 JSON 의 tool_input.command 를 검사. 브랜치를 새로 만들거나 rename 하는
#       명령(아래)에서 "새 브랜치명"을 추출해 컨벤션 `{type}/{issue-number}` 와 대조:
#   - git checkout -b|-B <name>
#   - git switch -c|-C|--create <name>
#   - git branch -m|-M|--move [<old>] <new>
#
# 컨벤션: ^(feat|fix|refactor|docs|chore|test|design)/[0-9]+$   (예: feat/46, fix/87)
#   - slug 부착(feat/46-foo) / 다른 형식은 차단 (mistakes-log 사례 3 재발 방지)
#   - main / master / develop 은 예외 허용 (장기 브랜치)
#
# 위반 시 stderr 에 사유 출력 + exit 2 → Claude Code 가 사용자에게 사유 표시 후 호출 abort.
# 통과 시 exit 0.
#
# 우회 (정당한 예외 — 사용자 명시 동의 시):
#   HARNESS_ALLOW_BRANCH_NAME=1 <git 명령>

set -euo pipefail

INPUT=$(cat)
COMMAND=$(echo "$INPUT" | jq -r '.tool_input.command // ""')

# 전역 우회 스위치
if [[ "${HARNESS_ALLOW_BRANCH_NAME:-0}" == "1" ]]; then
  exit 0
fi

CONV='^(feat|fix|refactor|docs|chore|test|design)/[0-9]+$'
ALLOW='^(main|master|develop)$'

# 명령을 && || ; 기준으로 세그먼트 분할 (bash 3.2 호환 — mapfile 미사용)
SEGMENTS=()
while IFS= read -r seg; do
  SEGMENTS+=("$seg")
done < <(echo "$COMMAND" | sed -E 's/[[:space:]]*(&&|\|\||;)[[:space:]]*/\n/g')

for seg in "${SEGMENTS[@]:-}"; do
  # trim
  seg="${seg#"${seg%%[![:space:]]*}"}"
  seg="${seg%"${seg##*[![:space:]]}"}"
  [[ -z "$seg" ]] && continue

  name=""
  if [[ "$seg" =~ ^git[[:space:]]+branch[[:space:]]+(-m|-M|--move)([[:space:]]|$) ]]; then
    # rename: 새 이름은 세그먼트의 마지막 위치 인자
    name="$(awk '{print $NF}' <<< "$seg")"
  elif [[ "$seg" =~ ^git[[:space:]]+(checkout|switch)[[:space:]] ]]; then
    # create: -b/-B/-c/-C/--create 바로 뒤 토큰
    name="$(awk '{for(i=1;i<=NF;i++){if($i=="-b"||$i=="-B"||$i=="-c"||$i=="-C"||$i=="--create"){print $(i+1); exit}}}' <<< "$seg")"
  else
    continue
  fi

  [[ -z "$name" ]] && continue

  # 양끝 따옴표 제거
  name="${name%[\"\']}"
  name="${name#[\"\']}"

  # 장기 브랜치 예외
  [[ "$name" =~ $ALLOW ]] && continue

  if [[ ! "$name" =~ $CONV ]]; then
    cat >&2 << EOF
🛑 차단됨: 브랜치명 컨벤션 위반 — "$name"

CONTRIBUTING.md §브랜치 네이밍: 브랜치명은 {type}/{issue-number} 형식.
  type = feat | fix | refactor | docs | chore | test | design
  예: feat/46, fix/87, chore/103

slug 부착(feat/46-foo)이나 다른 형식은 허용되지 않습니다 (mistakes-log 사례 3).

우회 (정당한 예외 — 사용자 직접 동의 시):
  HARNESS_ALLOW_BRANCH_NAME=1 <git 명령>
EOF
    exit 2
  fi
done

exit 0
