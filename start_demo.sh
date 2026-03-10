#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "$0")" && pwd)"
BACKEND_DIR="$ROOT_DIR/backend/api"
LOCAL_PROPERTIES="$ROOT_DIR/local.properties"
BACKEND_LOG="/tmp/hopon-backend.log"
TUNNEL_LOG="/tmp/hopon-cloudflared.log"

DEVICE_SERIAL="${1:-}"

require_cmd() {
  if ! command -v "$1" >/dev/null 2>&1; then
    echo "❌ Required command not found: $1"
    exit 1
  fi
}

require_cmd node
require_cmd cloudflared
require_cmd python3

if [[ ! -f "$LOCAL_PROPERTIES" ]]; then
  echo "❌ local.properties not found at: $LOCAL_PROPERTIES"
  exit 1
fi

if [[ ! -f "$ROOT_DIR/gradlew" ]]; then
  echo "❌ gradlew not found at: $ROOT_DIR/gradlew"
  exit 1
fi

echo "🔄 Clearing port 3001..."
for pid in $(lsof -ti tcp:3001 2>/dev/null || true); do
  kill -9 "$pid" || true
done

echo "🚀 Starting backend..."
cd "$BACKEND_DIR"
nohup node app.js >"$BACKEND_LOG" 2>&1 &
BACKEND_PID=$!

for _ in {1..20}; do
  if lsof -ti tcp:3001 >/dev/null 2>&1; then
    break
  fi
  sleep 0.5
done

if ! lsof -ti tcp:3001 >/dev/null 2>&1; then
  echo "❌ Backend failed to start. See: $BACKEND_LOG"
  tail -n 60 "$BACKEND_LOG" || true
  exit 1
fi

echo "🌐 Starting Cloudflare tunnel..."
nohup cloudflared tunnel --url http://localhost:3001 --logfile "$TUNNEL_LOG" >/tmp/hopon-cloudflared.out 2>&1 &
TUNNEL_PID=$!

TUNNEL_URL=""
for _ in {1..60}; do
  TUNNEL_URL="$(grep -Eo 'https://[-a-zA-Z0-9]+\.trycloudflare\.com' "$TUNNEL_LOG" 2>/dev/null | head -n 1 || true)"
  if [[ -n "$TUNNEL_URL" ]]; then
    break
  fi
  sleep 1
done

if [[ -z "$TUNNEL_URL" ]]; then
  echo "❌ Could not detect tunnel URL. See: $TUNNEL_LOG"
  tail -n 80 "$TUNNEL_LOG" || true
  exit 1
fi

echo "✅ Tunnel URL: $TUNNEL_URL"

python3 - "$LOCAL_PROPERTIES" "$TUNNEL_URL" <<'PY'
import sys
from pathlib import Path

path = Path(sys.argv[1])
url = sys.argv[2]
full = f"{url}/api/v1/"

lines = path.read_text(encoding="utf-8").splitlines()
updates = {
    "apiBaseUrlDevice": full,
    "apiBaseUrl": full,
}

seen = {k: False for k in updates}
out = []
for line in lines:
    replaced = False
    for k, v in updates.items():
        if line.startswith(f"{k}="):
            out.append(f"{k}={v}")
            seen[k] = True
            replaced = True
            break
    if not replaced:
        out.append(line)

for k, ok in seen.items():
    if not ok:
        out.append(f"{k}={updates[k]}")

path.write_text("\n".join(out) + "\n", encoding="utf-8")
PY

echo "🛠️ Updated local.properties with tunnel URL."

echo "📦 Installing debug app..."
cd "$ROOT_DIR"
if [[ -n "$DEVICE_SERIAL" ]]; then
  ./gradlew :app:installDebug -Pandroid.injected.device.serial="$DEVICE_SERIAL"
else
  ./gradlew :app:installDebug
fi

echo
echo "🎉 Demo environment ready"
echo "- Backend PID: $BACKEND_PID"
echo "- Tunnel PID:  $TUNNEL_PID"
echo "- Tunnel URL:  $TUNNEL_URL"
echo "- Backend log: $BACKEND_LOG"
echo "- Tunnel log:  $TUNNEL_LOG"
echo
echo "To stop later:"
echo "  kill $BACKEND_PID $TUNNEL_PID"
