#!/usr/bin/env bash
# Ідемпотентне створення міток, віх, epics та issues у GitHub за scripts/github/backlog.json.
# Використання:  REPO=owner/name ./scripts/github/bootstrap.sh
# Якщо REPO не задано, береться репозиторій поточного каталогу (gh repo view).
# Вимоги: bash, gh (автентифікований), jq.
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
BACKLOG_FILE="${BACKLOG_FILE:-$SCRIPT_DIR/backlog.json}"

log() { printf '%s\n' "$*" >&2; }
die() { log "ПОМИЛКА: $*"; exit 1; }

command -v gh >/dev/null 2>&1 || die "потрібен GitHub CLI (gh)"
command -v jq >/dev/null 2>&1 || die "потрібен jq"
[ -f "$BACKLOG_FILE" ] || die "не знайдено $BACKLOG_FILE"
jq -e . "$BACKLOG_FILE" >/dev/null || die "backlog.json не є коректним JSON"

REPO="${REPO:-}"
if [ -z "$REPO" ]; then
  REPO="$(gh repo view --json nameWithOwner --jq .nameWithOwner </dev/null)" || die "не вдалося визначити репозиторій, задайте REPO=owner/name"
fi
log "Репозиторій: $REPO"

read -r -d '' DOR <<'EOF' || true
- [x] Є користувацька історія або мета
- [x] Є критерії приймання (Given/When/Then) або критерії завершення
- [x] Є оцінка в Story Points і мітки type, priority, area, sp
- [x] Залежності вказані
- [x] Історію прив'язано до epic
EOF

# ---------------------------------------------------------------- мітки
log "== Мітки"
while IFS= read -r row; do
  name="$(jq -r '.name' <<<"$row")"
  color="$(jq -r '.color' <<<"$row")"
  desc="$(jq -r '.description' <<<"$row")"
  gh label create "$name" --color "$color" --description "$desc" --force --repo "$REPO" </dev/null >/dev/null
  log "  мітка: $name"
done < <(jq -c '.labels[]' "$BACKLOG_FILE")

# ---------------------------------------------------------------- віхи
log "== Віхи"
MS_JSON="$(gh api "repos/$REPO/milestones?state=all&per_page=100" --paginate </dev/null | jq -s 'add // []')"
while IFS= read -r row; do
  title="$(jq -r '.title' <<<"$row")"
  existing="$(jq -r --arg t "$title" '[.[] | select(.title == $t) | .number][0] // empty' <<<"$MS_JSON")"
  if [ -n "$existing" ]; then
    log "  віха вже є (#$existing): $title"
  else
    desc="$(jq -r '.description' <<<"$row")"
    due="$(jq -r '.due_on' <<<"$row")"
    state="$(jq -r '.state // "open"' <<<"$row")"
    created="$(gh api --method POST "repos/$REPO/milestones" \
      -f title="$title" -f description="$desc" -f due_on="$due" -f state="$state" --jq .number </dev/null)"
    log "  створено віху #$created: $title"
  fi
done < <(jq -c '.milestones[]' "$BACKLOG_FILE")

ms_title() { jq -r --arg k "$1" '.milestones[] | select(.key == $k) | .title' "$BACKLOG_FILE"; }

# ---------------------------------------------------------------- допоміжне
ISSUES_JSON="$(gh issue list --repo "$REPO" --state all --limit 1000 --json number,title </dev/null)"
NUMS='{}'   # key -> номер issue

set_num() { NUMS="$(jq -c --arg k "$1" --argjson n "$2" '.[$k] = $n' <<<"$NUMS")"; }
get_num() { jq -r --arg k "$1" '.[$k] // empty' <<<"$NUMS"; }

render_body() {
  local entry="$1" body sp epic k n
  body="$(jq -r '.body | join("\n")' <<<"$entry")"
  sp="$(jq -r '.sp // empty' <<<"$entry")"
  epic="$(jq -r '.epic // empty' <<<"$entry")"
  if [ -n "$sp" ]; then
    if [ -n "$epic" ]; then
      body="$(printf '%s\n\n## Epic\n%s' "$body" "{{$epic}}")"
    fi
    body="$(printf '%s\n\n## Оцінка\n%s SP\n\n## Definition of Ready\n%s' "$body" "$sp" "$DOR")"
  fi
  while IFS= read -r k; do
    n="$(get_num "$k")"
    if [ -n "$n" ]; then
      body="$(jq -rn --arg b "$body" --arg k "{{$k}}" --arg r "#$n" '$b | split($k) | join($r)')"
    fi
  done < <(jq -r '.epics[].key' "$BACKLOG_FILE")
  printf '%s' "$body"
}

ensure_issue() {
  local key="$1" entry="$2" title existing body ms_key url number l
  local -a args
  title="$(jq -r '.title' <<<"$entry")"
  existing="$(jq -r --arg t "$title" '[.[] | select(.title == $t) | .number][0] // empty' <<<"$ISSUES_JSON")"
  if [ -n "$existing" ]; then
    log "  пропущено (вже є #$existing): $title"
    set_num "$key" "$existing"
    return 0
  fi
  body="$(render_body "$entry")"
  args=(--repo "$REPO" --title "$title" --body "$body")
  while IFS= read -r l; do
    args+=(--label "$l")
  done < <(jq -r '.labels[]' <<<"$entry")
  ms_key="$(jq -r '.milestone // empty' <<<"$entry")"
  if [ -n "$ms_key" ]; then
    args+=(--milestone "$(ms_title "$ms_key")")
  fi
  url="$(gh issue create "${args[@]}" </dev/null)"
  number="${url##*/}"
  set_num "$key" "$number"
  log "  створено #$number: $title"
  sleep 1
}

# ---------------------------------------------------------------- epics, історії, беклог
log "== Epics"
while IFS= read -r row; do
  ensure_issue "$(jq -r '.key' <<<"$row")" "$row"
done < <(jq -c '.epics[]' "$BACKLOG_FILE")

log "== Issues спринтів"
while IFS= read -r row; do
  ensure_issue "$(jq -r '.key' <<<"$row")" "$row"
done < <(jq -c '.issues[]' "$BACKLOG_FILE")

log "== Беклог"
while IFS= read -r row; do
  ensure_issue "$(jq -r '.key' <<<"$row")" "$row"
done < <(jq -c '.backlog[]' "$BACKLOG_FILE")

# ---------------------------------------------------------------- списки завдань в epics
log "== Списки завдань у epics"
while IFS= read -r row; do
  ekey="$(jq -r '.key' <<<"$row")"
  enum="$(get_num "$ekey")"
  tasks=""
  while IFS= read -r ik; do
    n="$(get_num "$ik")"
    if [ -n "$n" ]; then
      tasks="${tasks}- [ ] #${n}"$'\n'
    fi
  done < <(jq -r --arg k "$ekey" '[.issues[], .backlog[]] | map(select(.epic == $k)) | .[].key' "$BACKLOG_FILE")
  ebody="$(jq -r '.body | join("\n")' <<<"$row")"
  ebody="$(printf '%s\n\n## Завдання\n%s' "$ebody" "$tasks")"
  gh issue edit "$enum" --repo "$REPO" --body "$ebody" </dev/null >/dev/null
  log "  оновлено epic #$enum ($ekey)"
done < <(jq -c '.epics[]' "$BACKLOG_FILE")

# ---------------------------------------------------------------- підсумок
log "== Відповідність ключів і номерів"
jq -r --argjson nums "$NUMS" '(.epics[], .issues[], .backlog[]) | "\(.key)\t#\($nums[.key])\t\(.title)"' "$BACKLOG_FILE"
log "Готово."
