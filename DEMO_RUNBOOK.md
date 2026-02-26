# HopOn Demo Runbook (After Restart / Shutdown)

This file contains everything needed to get the HopOn demo running again.

## 1) Prerequisites

- macOS with Android SDK + `adb`
- Node.js installed
- Homebrew installed
- `cloudflared` installed (`brew install cloudflared`)
- Android device (physical and/or emulator)

## 2) Open project root

```bash
cd /Users/nattasasi/Documents/SeniorProject_1/HopOn
```

## 3) Start backend API (port 3001)

Kill anything on port 3001 first:

```bash
for pid in $(lsof -ti tcp:3001); do kill -9 "$pid"; done
```

Start backend:

```bash
cd /Users/nattasasi/Documents/SeniorProject_1/HopOn/backend/api
node app.js
```

Expected: server starts and MongoDB connects.

Keep this terminal open.

## 4) Start public tunnel (for physical device without USB/LAN dependency)

In a new terminal:

```bash
cloudflared tunnel --url http://localhost:3001
```

Copy the generated URL, e.g.:

`https://xxxx-xxxx-xxxx.trycloudflare.com`

Keep this terminal open.

## 5) Set app API URLs in `local.properties`

Edit `/Users/nattasasi/Documents/SeniorProject_1/HopOn/local.properties` and set:

```properties
apiBaseUrlEmulator=http://10.0.2.2:3001/api/v1/
apiBaseUrlDevice=https://YOUR_CURRENT_TRYCLOUDFLARE_URL/api/v1/
apiBaseUrl=https://YOUR_CURRENT_TRYCLOUDFLARE_URL/api/v1/
```

Important:
- `apiBaseUrlDevice` and `apiBaseUrl` must be updated every time tunnel URL changes.
- The app auto-selects emulator vs device URL at runtime.

## 6) Build + install app

From project root:

```bash
cd /Users/nattasasi/Documents/SeniorProject_1/HopOn
./gradlew installDebug
```

If multiple devices are connected and you want physical only:

```bash
adb devices
./gradlew :app:installDebug -Pandroid.injected.device.serial=YOUR_DEVICE_SERIAL
```

## 7) Quick health checks

Backend local:

```bash
curl -s -o /dev/null -w '%{http_code}\n' http://127.0.0.1:3001/api/v1/
```

Tunnel:

```bash
curl -s -o /dev/null -w '%{http_code}\n' https://YOUR_CURRENT_TRYCLOUDFLARE_URL/api/v1/
```

`404` is acceptable for base route; it still proves server reachability.

## 8) If login shows "Unable to reach server"

1. Confirm backend terminal is still running.
2. Confirm cloudflared terminal is still running.
3. Confirm `apiBaseUrlDevice` matches the current tunnel URL.
4. Reinstall app:

```bash
cd /Users/nattasasi/Documents/SeniorProject_1/HopOn
./gradlew installDebug
```

5. Capture app network logs:

```bash
adb logcat -d | grep -iE "okhttp|retrofit|SocketTimeout|UnknownHost|ConnectException|hopon"
```

## 9) Optional: USB-only fallback (no tunnel)

If USB is connected, you can use reverse port mapping and local URL:

```bash
adb -s YOUR_DEVICE_SERIAL reverse tcp:3001 tcp:3001
```

Then set device URL to localhost:

```properties
apiBaseUrlDevice=http://127.0.0.1:3001/api/v1/
apiBaseUrl=http://127.0.0.1:3001/api/v1/
```

Reinstall app after changes.

## 10) Demo startup checklist (fast)

1. Start backend (`node app.js`)
2. Start tunnel (`cloudflared tunnel --url http://localhost:3001`)
3. Update `local.properties` with current tunnel URL
4. `./gradlew installDebug`
5. Open app and login

## 11) One-command automation (recommended)

You can automate most steps using:

```bash
cd /Users/nattasasi/Documents/SeniorProject_1/HopOn
chmod +x start_demo.sh
./start_demo.sh
```

Install to a specific physical device serial:

```bash
./start_demo.sh 3B1F6ME5MS11C52Q
```

This script will:
- start backend on port `3001`
- start Cloudflare quick tunnel
- update `local.properties` with the current tunnel URL
- install the debug app
