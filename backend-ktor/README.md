# CJLU Student Backend (Ktor)

HTTP API and admin portal for the CJLU Student Android app.

## Quick start

From the project root:

```bash
./scripts/start-backend.sh
# or
./gradlew :backend-ktor:run
```

Server listens on **http://0.0.0.0:8080**.

Dev mode (`CJLU_ALLOW_INSECURE_DEV_DEFAULTS=true`, set automatically for `gradlew run`) uses:

| Setting | Dev value |
|---------|-----------|
| Student API key | `cjlu-insecure-local-student-api-key-do-not-use-in-production` |
| Admin login | `admin` / `cjlu2026` |
| Default student password | Same as student ID (e.g. `20230901`) |

## Pairing with the Android app

In `local.properties` at the project root:

```properties
cjlu.api.host=10.0.2.2
cjlu.api.port=8080
cjlu.student.api.key=cjlu-insecure-local-student-api-key-do-not-use-in-production
```

| Device | `cjlu.api.host` |
|--------|-----------------|
| Emulator | `10.0.2.2` |
| Physical device (same Wi‑Fi) | Your machine's LAN IP (e.g. `192.168.1.10`) |

Debug builds only allow cleartext HTTP to `localhost`, `127.0.0.1`, and `10.0.2.2`. For a real device IP, extend `network_security_config` or use HTTPS.

## Student API (mobile app)

All student endpoints require `X-API-Key`. Authenticated routes also need `Authorization: Bearer <JWT>` from `POST /auth/login`.

| Method | Path |
|--------|------|
| POST | `/auth/login` |
| POST | `/auth/change-password` |
| GET | `/auth/me` |
| GET | `/services` |
| GET/PATCH | `/students/{id}/profile` |
| GET | `/students/{id}/messages` |
| PATCH | `/students/{id}/messages/{messageId}/read` |
| GET | `/students/{id}/academic/attendance` |
| GET | `/students/{id}/academic/transcript` |
| GET | `/students/{id}/academic/timetable` |
| GET | `/students/{id}/dormitory` |
| GET | `/students/{id}/requests` |
| POST | `/requests` |
| POST | `/requests/{id}/upload` |
| POST/DELETE | `/students/{id}/fcm-token` |
| WS | `/updates/{studentId}` |

Errors on student routes are **plain text** (not JSON), matching the Android client.

`Accept-Language: zh*` localizes catalog and messages.

## Admin portal

| URL | Purpose |
|-----|---------|
| http://localhost:8080/admin/login | Staff login |
| http://localhost:8080/admin | Dashboard (requests, register walk-in, **attendance rate**, student learning, **student inbox**) |
| http://localhost:8080/admin/api/requests | All requests (JSON, admin session) |
| POST `/admin/attendance/generate-courses` | Register student for catalog courses (5–7 or all) with initial attendance |
| POST `/admin/attendance/randomize` | Fill random attendance % and sessions for each registered course |
| POST `/admin/student-attendance` | Save manual per-course attendance (recomputes overall %, pushes to app) |
| POST `/admin/student-timetable` | Weekly class schedule slots (pushes to app) |
| POST `/admin/student-learning` | Overall attendance override + home schedule notice banner |
| POST `/admin/student-inbox-message` | Compose inbox message to one student or broadcast; pushes `messages` over WebSocket |

**Attendance rate** (`/admin?student=20230901#attendance`): pick a student, **generate registered courses** from the 7-course catalog (Data Structures, OS, etc.), **fill random attendance** per course, edit values, and save. Overall attendance and weekly trend update automatically; the student app is notified in real time.

**Student learning** (`/admin?student=20230901#learning`): weekly timetable and home banner notice (per-course attendance is under Attendance rate).

**Student inbox** (`/admin?panel=messages#messages`): send English (required) and optional Chinese title/body into the app Messages screen; choose **all students** or one recipient from the roster. Seeded demo messages remain broadcast (`recipient_student_id` null).

## App integration notes

The mobile app listens on `ws://<host>:<port>/updates/{studentId}` with the same `X-API-Key` used for REST calls.

Published realtime events include:

- `messages` — refreshes the message list and, when the app is backgrounded, can trigger a notification
- `academic_updated` — refreshes cached academic data and syncs learning alerts
- `learning_alerts` — refreshes profile-derived learning alert state
- `REFRESH` — refreshes requests without payload parsing

The same payloads are sent over **FCM** (data key `payload`) to all tokens registered for the student. Configure FCM with `CJLU_FCM_SERVICE_ACCOUNT` or `backend-ktor/fcm-service-account.json` (see `fcm-service-account.json.example`).

Legacy `/`, `/login`, and `/logout` redirect to the `/admin` URLs.

## Project layout

```
shared-contract/             # Shared kotlinx-serialization DTOs (app + backend)
backend-ktor/src/main/kotlin/com/cjlu/backend/
├── Application.kt          # Entry + route wiring
├── Models.kt               # Typealiases to shared-contract
├── Database.kt             # Auth, messages, requests
├── AcademicRepository.kt   # Academic read model
├── plugins/                # Serialization, security, WebSockets
├── routes/                 # HTTP route modules
├── auth/                   # API key / JWT helpers
├── admin/                  # Session, paths, template models, services, routes
├── websocket/              # Realtime push hub
├── fcm/                  # FCM HTTP v1 client + push service
├── resources/admin/templates/  # Admin HTML (FreeMarker)
├── resources/admin/static/     # Admin CSS & JS
└── *Seed.kt                # Roster & content seeds (top-level)
```

Uploads at `/uploads/{file}` require a **student JWT** (owner) or **admin session**. They are not public static files.

## Tests

Contract tests assert responses match the Android DTO shapes:

```bash
./gradlew :shared-contract:build :backend-ktor:test
```

CI runs the same via GitHub Actions (`.github/workflows/ci.yml`).

## Production environment

| Variable | Required |
|----------|----------|
| `STUDENT_API_KEY` | Yes (must match app `cjlu.student.api.key`) |
| `JWT_SECRET` | Yes (32+ chars) |
| `ADMIN_USERNAME` / `ADMIN_PASSWORD` | Yes |
| `CJLU_ALLOW_INSECURE_DEV_DEFAULTS` | Must be **unset** or `false` |

See also [Architecture documentation index](../docs/README.md).
