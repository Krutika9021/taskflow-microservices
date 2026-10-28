#!/usr/bin/env bash
# End-to-end smoke test against the running docker compose stack (gateway on :8080).
set -euo pipefail

BASE="${BASE_URL:-http://localhost:8080}"
SUFFIX="$(date +%s)"
PASS="password1"

fail() { echo "FAIL: $1"; exit 1; }

# status_of METHOD URL [extra curl args...] -> prints "<http_code>" and stores body in /tmp/body
call() {
  local method="$1" url="$2"; shift 2
  curl -s -o /tmp/body -w '%{http_code}' -X "$method" "$url" "$@"
}

expect() { # expect <label> <expected> <actual>
  if [ "$2" != "$3" ]; then
    echo "Response body: $(cat /tmp/body)"
    fail "$1: expected HTTP $2, got $3"
  fi
  echo "OK   $1 -> $3"
}

JSON=(-H "Content-Type: application/json")

# Tenant A
code=$(call POST "$BASE/auth/register" "${JSON[@]}" \
  -d "{\"email\":\"a-$SUFFIX@acme.test\",\"password\":\"$PASS\",\"tenantName\":\"Acme $SUFFIX\"}")
expect "register tenant A" 201 "$code"

code=$(call POST "$BASE/auth/login" "${JSON[@]}" \
  -d "{\"email\":\"a-$SUFFIX@acme.test\",\"password\":\"$PASS\"}")
expect "login tenant A" 200 "$code"
TOKEN_A=$(jq -r '.token' /tmp/body)
[ -n "$TOKEN_A" ] && [ "$TOKEN_A" != "null" ] || fail "no token in login response"

code=$(call POST "$BASE/tasks" "${JSON[@]}" -H "Authorization: Bearer $TOKEN_A" \
  -d '{"title":"First task"}')
expect "create task as tenant A" 201 "$code"
TASK_ID=$(jq -r '.id' /tmp/body)
[ -n "$TASK_ID" ] && [ "$TASK_ID" != "null" ] || fail "no task id in create response"

code=$(call GET "$BASE/tasks" -H "Authorization: Bearer $TOKEN_A")
expect "list tasks as tenant A" 200 "$code"
count=$(jq -r '.content | length' /tmp/body)
[ "$count" -ge 1 ] || fail "tenant A list is empty"

# No token
code=$(call GET "$BASE/tasks")
expect "list tasks without token" 401 "$code"

# Wrong password
code=$(call POST "$BASE/auth/login" "${JSON[@]}" \
  -d "{\"email\":\"a-$SUFFIX@acme.test\",\"password\":\"wrong-password\"}")
expect "login with wrong password" 401 "$code"

# Tenant B must not see tenant A's data
code=$(call POST "$BASE/auth/register" "${JSON[@]}" \
  -d "{\"email\":\"b-$SUFFIX@beta.test\",\"password\":\"$PASS\",\"tenantName\":\"Beta $SUFFIX\"}")
expect "register tenant B" 201 "$code"

code=$(call POST "$BASE/auth/login" "${JSON[@]}" \
  -d "{\"email\":\"b-$SUFFIX@beta.test\",\"password\":\"$PASS\"}")
expect "login tenant B" 200 "$code"
TOKEN_B=$(jq -r '.token' /tmp/body)

code=$(call GET "$BASE/tasks/$TASK_ID" -H "Authorization: Bearer $TOKEN_B")
expect "tenant B reads tenant A task (isolation)" 404 "$code"

code=$(call GET "$BASE/tasks" -H "Authorization: Bearer $TOKEN_B")
expect "list tasks as tenant B" 200 "$code"
count=$(jq -r '.content | length' /tmp/body)
[ "$count" -eq 0 ] || fail "tenant B can see $count task(s) of another tenant"

# Spoofed header must not override the identity from the token
code=$(call GET "$BASE/tasks/$TASK_ID" -H "Authorization: Bearer $TOKEN_B" -H "X-Tenant-Id: 1")
expect "spoofed X-Tenant-Id is ignored" 404 "$code"

echo "All smoke tests passed."