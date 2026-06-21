# CJLU Student App

Android student mobile app for China Jiliang University (CJLU), with a Ktor backend and admin portal. Students use the app to view academic information, submit campus service requests, and receive notifications. Staff use the web admin console to handle requests and learning alerts.

## Features

| Area | Description |
| :--- | :--- |
| **Login & profile** | JWT login, password change, student profile |
| **Home** | Attendance rate, class schedule, quick actions |
| **Campus services** | 25+ service catalog (leave, room change, transcripts, etc.) with file uploads and offline sync |
| **Messages** | School notifications and read status |
| **Academic details** | Attendance with weekly trends, transcript/GPA, timetable, dormitory info |
| **Widget** | Glance home-screen widget showing unread messages and active requests |
| **Realtime** | WebSocket learning alerts, academic updates, and message updates |
| **Push (FCM)** | Firebase Cloud Messaging when the app is backgrounded or killed |
| **Localization** | Chinese / English UI |

## Widget and realtime flow

The widget is kept in sync by the main app process. When the app refreshes messages or service requests, it writes the latest counts to shared preferences and triggers a widget update.

Realtime updates arrive over `ws://<host>:<port>/updates/{studentId}` and are used to refresh:

- message counts and notifications
- request status changes
- academic / learning alert state

If you want the widget to stay current, open the app at least once after login so it can seed the initial counts. After that, widget updates are driven by app refreshes and WebSocket events.

## Firebase Cloud Messaging (FCM)

The app registers an FCM device token after login (`POST /students/{id}/fcm-token`). The backend mirrors every WebSocket push to registered tokens as a **data message** (`payload` = same JSON as the socket).

### One-time setup

1. **Firebase Console** — Create a project, add an Android app with package `com.cjlu.studentapp`, download `google-services.json` into `app/` (replace the dev stub).
2. **Backend service account** — Firebase Console → Project settings → Service accounts → Generate new private key. Save as `backend-ktor/fcm-service-account.json` (gitignored) or set `CJLU_FCM_SERVICE_ACCOUNT` to its path.
3. **Profile** — Enable **Notify me about updates** in the app and allow notifications (Android 13+).

Without the service account file, the backend still runs; only FCM delivery is skipped (WebSocket + local notifications still work when the app is open).

## Tech stack

| Component | Technology |
| :--- | :--- |
| Android client | Kotlin, Jetpack Compose, Navigation, Room, Retrofit, OkHttp, Glance, Firebase Messaging |
| Backend | Ktor, Exposed ORM, H2, FreeMarker (admin UI), WebSocket |
| Shared contract | `shared-contract` module (kotlinx-serialization DTOs shared by app and backend) |
| Build | Gradle (Kotlin DSL), JDK 17, minSdk 30, targetSdk 36 |

## Project layout

```text
CJLUStudentApp/
├── app/                    # Android client
├── backend-ktor/           # Ktor HTTP API + admin portal (see backend-ktor/README.md)
├── shared-contract/        # Shared serialization DTOs
├── docs/                   # Architecture and data-layer notes
├── scripts/                # Helper scripts (e.g. start backend)
└── .github/workflows/      # CI (backend tests + Android compile)
```

## Quick start

### 1. Configure local properties

Copy the examples from `local.properties.example` into `local.properties` at the project root (this file is gitignored):

```properties
cjlu.api.host=10.0.2.2
cjlu.api.port=8080
cjlu.student.api.key=cjlu-insecure-local-student-api-key-do-not-use-in-production
```

| Environment | `cjlu.api.host` |
| :--- | :--- |
| Emulator | `10.0.2.2` |
| Physical device (same Wi‑Fi) | Your machine's LAN IP (e.g. `192.168.1.10`) |

### 2. Start the backend

From the project root:

```bash
./scripts/start-backend.sh
# or
./gradlew :backend-ktor:run
```

The server listens on **<http://0.0.0.0:8080>** by default.

Development mode (`gradlew run` sets `CJLU_ALLOW_INSECURE_DEV_DEFAULTS=true` automatically):

| Setting | Dev default |
| :--- | :--- |
| Student API key | `cjlu-insecure-local-student-api-key-do-not-use-in-production` |
| Admin login | `admin` / `cjlu2026` |
| Default student password | Same as student ID (e.g. `20230901`) |

Admin portal: <http://localhost:8080/admin/login>

### 3. Run the Android app

Open the project in Android Studio, select the `app` module, and run on an emulator or device.

Debug builds allow cleartext HTTP to `localhost`, `127.0.0.1`, and `10.0.2.2`. For a physical device on a LAN IP, update `network_security_config` or use HTTPS.

## Tests

```bash
# Backend contract tests (aligned with Android DTOs)
./gradlew :shared-contract:build :backend-ktor:test

# Android unit tests
./gradlew :app:testDebugUnitTest
```

GitHub Actions runs these checks on push/PR to `main` or `master`.

## Production deployment

Set these environment variables on the backend (**do not** enable `CJLU_ALLOW_INSECURE_DEV_DEFAULTS`):

| Variable | Description |
| :--- | :--- |
| `STUDENT_API_KEY` | Must match `cjlu.student.api.key` in the app |
| `JWT_SECRET` | At least 32 characters |
| `ADMIN_USERNAME` / `ADMIN_PASSWORD` | Admin portal credentials |
| `CJLU_FCM_SERVICE_ACCOUNT` | Path to Firebase service account JSON for push (optional) |

Release builds require `cjlu.student.api.key` in `local.properties`; otherwise student API calls will fail.

## Further reading

- [Architecture documentation index](docs/README.md) (overview, stakeholder summary, ADR, onboarding)
- [Backend API & admin portal](backend-ktor/README.md)

## License

No open-source license is declared in this repository; contact the maintainers before use.
