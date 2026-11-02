# Wi-Fi Data Track

Android Java recorder for a Redmi Note 8 Pro running Android 11, with a password-protected web dashboard served by the phone. The laptop can be switched off: the phone records household router counters, and a laptop browser on the same Wi-Fi can later view reports.

## What is implemented

- User-controlled foreground service, visible notification, CPU wake lock while recording, and 120-second delay between completed collection attempts.
- Fixed router endpoint: `https://192.168.1.1/statistics_status_web_app.cgi`.
- Reads WAN1 `1_TR069_INTERNET_R_VID_0` only: `EthernetBytesReceived` and `EthernetBytesSent`. Excludes WAN2, LAN, and WLAN to avoid counting traffic twice.
- Login in the app's own WebView, using the router's original login page. No fabricated login API or stored router password. Collector uses this WebView's cookies.
- Explicit import and SHA-256 verification of the router's exact certificate, scoped to `192.168.1.1`, for self-signed router certificates. No trust-all SSL configuration.
- SQLite sample/interval history, India-time daily reports, gap/reset flags, CSV export and report JSON download.
- Phone-hosted dashboard on port 8080 with a separate password, PBKDF2 password hashing, in-memory one-hour bearer sessions, request-size limits and login throttling.
- No browser tab is required on either phone or laptop after in-app login. Router session expiry still requires login again.

## Explicit limits

This is an initial implementation requiring an Android build and real-device QA. It has not been tested against a live router. Additional router authentication (for example, a CSRF token rather than session cookies) may require adjustment after testing.

Connected-device API and per-device byte counters have NOT been identified. The current router data provides household totals, not consumption per device. The dashboard says this explicitly. This version does not scrape device identities or infer consumption from Wi-Fi link rates.

The native dashboard is lightweight HTML/CSS/JavaScript, not Angular. The collector is an Android foreground service, not Spring Boot: Android owns its lifecycle and SQLite storage. There is no separate server process to keep running on a laptop. A future Angular frontend can consume the same report API.

Only the phone stays on. Reboots, force-stop, Wi-Fi loss, Android Doze, Xiaomi task killing, or expired router sessions can interrupt collection. This version intentionally does not auto-start at boot or bypass Android restrictions. Start manually after a reboot. A CPU wake lock is used while recording, which increases battery consumption and does not itself guarantee network access during Doze.

Daily totals cover recorded same-day intervals only. First reading establishes a baseline. Counter decreases exclude the interval as a reset/rollover. Resets that return above the prior value between readings cannot be detected. Gaps over five minutes are flagged. An interval crossing midnight/multiple days is shown separately as an unallocated total; it is NOT divided into invented daily figures. Router counters may differ from Airtel billing.

## Build

Requirements: JDK 17, Android SDK/platform 35, Gradle 8.9, internet for Gradle/Maven dependencies. Open this folder in Android Studio as a Gradle project, or use an installed Gradle:

```sh
gradle :app:testDebugUnitTest :app:assembleDebug
```

The Gradle wrapper is not bundled. CI installs the specified Gradle version and builds/tests; its APK is a workflow artifact. Debug APK output: `app/build/outputs/apk/debug/app-debug.apk`.

`minSdk` and `targetSdk` are 30 for this Android 11 sideloaded prototype. This is not a Google Play submission or a promise of compatibility with later Android foreground-service policies.

## Phone setup

1. Install the debug APK on the Redmi and connect to Airtel Wi-Fi.
2. Open the app. Set a unique dashboard password of 12–128 characters. Do NOT reuse your router, email, or bank password.
3. If the router uses an untrusted certificate, export its certificate from desktop Chrome's certificate viewer. Compare its SHA-256 fingerprint and transfer the certificate file privately to the phone. In the app choose **Import router certificate**. Compare the displayed fingerprint against Chrome before confirming trust. The exact imported leaf certificate is trusted only for the fixed router IP; this explicitly accepts that pinned certificate even if self-signed, expired, or without a matching hostname. A changed certificate fails closed and needs fresh manual verification/import. Never import an unknown certificate from a message or website.
4. Choose **Router login (inside app)** and log in using the router page. Chrome's cookie session is separate and cannot substitute for this step. Tap **Done — return to recorder**.
5. Choose **Start recorder + dashboard**. Confirm the recorder notification appears and a successful reading is reported. Wait at least two minutes for the next reading.
6. On Redmi, allow this app to run in the background using the battery settings available on that MIUI version. Check screen-off operation for at least 30 minutes. Keep the phone on home Wi-Fi. The app does not automatically change system settings.
7. On the laptop, open the `http://PHONE_IP:8080` address shown on the app home screen. Use the dashboard password. The two devices must be on a network that allows client-to-client access. A DHCP reservation for the phone avoids changing its address.
8. Stop recording from the Android app when no longer wanted. Restart after reboot. A dashboard login expires after one hour or service restart.

## Local dashboard security

The first version serves **HTTP**, not HTTPS. The dashboard password, session token, and reports can be observed by someone able to intercept that LAN traffic. Use only on trusted home Wi-Fi and use a unique password. Do not port-forward 8080, expose this server to the internet, or use it on public/shared networks. Router credentials and session cookies are never returned by the dashboard API. Router requests themselves use HTTPS with platform trust or the explicitly verified certificate pin.

Credentials are not committed to source. Android stores the dashboard hash and certificate pin in app-private preferences; router session cookies remain in WebView's app-private cookie store. Uninstalling the app deletes its data. JSON download is a report export, not a full database backup; no restore UI is implemented.

Changing the dashboard password stops the service and invalidates sessions; start it again. Phone display lists local addresses; select the Wi-Fi address rather than any VPN address. The server rejects non-local Host headers as basic DNS-rebinding protection and does not enable CORS.

## APIs

- `POST /api/login` JSON `{ "password": "your separate dashboard password" }` → `{ "token": "..." }`
- `GET /api/report` with `Authorization: Bearer TOKEN` → daily totals, cross-day interval totals, collector status, timestamps; `perDeviceSupported:false`
- `POST /api/logout` with the bearer token → invalidates that session

The dashboard refreshes displayed reports every 30 seconds. This does not trigger router collection: the recorder collects independently every 120 seconds after a completed attempt, so the actual start-to-start interval also includes request time.

## Verification status

Pure Java usage calculations can run without Android. Unit tests cover normal differences, resets, cross-midnight gaps, clock reversal, malformed counters, and WAN1 selection. The development environment ran JavaScript syntax and project structure/XML checks. A standalone Java arithmetic test harness was prepared but could not run: only a Java runtime was available, with no javac, Android SDK, or Gradle. Java compilation, JUnit execution, APK generation, certificate pin behavior, router login and screen-off collection remain unverified. GitHub Actions is configured to build and run unit tests on push; check its actual run result before treating the APK as verified.

## First real-device acceptance checks

- Import the verified router certificate; a different certificate must be rejected.
- Login inside app, Start, see baseline, then a second successful sample.
- Login from laptop with separate dashboard password; wrong password and expired token must fail.
- Turn laptop off for 30 minutes while phone screen is off; reopen dashboard and inspect sample timestamps/gaps.
- Disconnect Wi-Fi: error status without false usage. Reconnect: resume with gap flag where applicable.
- Confirm router reboot produces reset flag when counters decrease; no negative daily totals.
- Cross midnight: preserve interval as unallocated, not a fabricated daily split.
- Stop recorder: server becomes unavailable. Restart: SQLite history remains.
