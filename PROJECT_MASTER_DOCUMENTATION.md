# CJLU Student App & Backend — Master Project Reference Guide

This documentation serves as a comprehensive reference guide for the **CJLU Student App & Backend** ecosystem. It explains the system architecture, directory layouts, data models, authentication flow, real-time pipelines, and deployment settings.

---

## 1. System Overview & Architecture

The **CJLU Student App** is a full-stack student management mobile system developed for **China Jiliang University (CJLU)**. The system enables students to manage academic schedules, monitor performance, and file campus service requests. Concurrently, it allows university administrators and staff to review, manage, and audit student data, schedules, and request submissions.

The architecture is divided into three primary modules within a unified Gradle project:

```text
                               +-----------------------------+
                               |     shared-contract DTOs    |
                               +--------------+--------------+
                                              |
                       +----------------------+----------------------+
                       v                                             v
          +-------------------------+                   +-------------------------+
          |      ANDROID APP        |                   |      KTOR BACKEND       |
          |  (Jetpack Compose UI)   |     REST APIs     |   (Kotlin Web Server)   |
          |  Room Local Cache DB    | <===============> |  Exposed ORM & H2 DB    |
          |  Glance Home Widget     |    WebSockets     |  FreeMarker Admin Portal|
          +-------------------------+                   +-------------------------+
```

### A. Shared Serialization Contract (`/shared-contract`)
A central library holding all Kotlin data transfer objects (DTOs) and serialization schemas (`kotlinx.serialization`). It is imported by both the Android app and Ktor backend to ensure end-to-end API contract safety. Any changes to data shapes are automatically reflected in both client and server during compilation.

### B. Android Client App (`/app`)
A modern, native Android application built with an offline-first and real-time syncing philosophy.
* **Declarative UI:** Jetpack Compose with Material 3 styling.
* **Networking:** Retrofit 2 & OkHttp 3 for REST endpoint communication.
* **Local Persistence:** Room (SQLite) database (`cjlu_database`) caches academic states and buffers offline requests.
* **Home Screen Widget:** Built with Jetpack Glance to display unread messages and active request counts.
* **Realtime Sync:** Persistent WebSockets connection for live updates, with a fallback to Firebase Cloud Messaging (FCM) push notifications when the app is backgrounded or closed.
* **Offline Fallback:** `SharedPreferences` serialization for the service catalog to ensure instant loading and zero visual disappearance on cold starts.

### C. Backend Web Server (`/backend-ktor`)
An asynchronous Kotlin web server that serves as the REST API provider, WebSocket push hub, and host for the administration portal.
* **Core Framework:** Ktor on the Netty engine.
* **Database & ORM:** Jetpack Exposed ORM with a file-backed H2 relational database (`requests_sql_db.mv.db`).
* **Web Admin Portal:** Templated HTML screens rendered via Apache FreeMarker, enabling staff to interactively administer academic registries, attendance records, course timetables, transcript grades, and announcements.
* **Realtime Hub:** A WebSocket manager (`WebSocketHub`) that maps live sockets to student IDs and delivers instant push frames.

---

## 2. Directory Structure

```text
CJLUStudentApp/
├── shared-contract/            # Shared DTOs & Serialization Schemas
│   └── src/main/kotlin/com/cjlu/contract/
│       └── ApiModels.kt        # All API Request/Response data contracts
│
├── app/                        # Android Client Application
│   ├── src/main/
│   │   ├── AndroidManifest.xml # Permissions, WS/FCM services, widgets config
│   │   ├── java/com/cjlu/studentapp/
│   │   │   ├── MainActivity.kt # Main entrypoint, layout routing, session control
│   │   │   ├── data/           # Repositories, Room database, cache entities
│   │   │   │   ├── AcademicCache.kt      # Room cache table & DAO
│   │   │   │   ├── AppDatabase.kt        # Room DB init & migrations
│   │   │   │   ├── InboxMessage.kt       # Local Message Entity & mapping
│   │   │   │   ├── RequestManager.kt     # Submissions sync & attachments upload
│   │   │   │   ├── ServiceCatalogRepository.kt # Offline prefs cache & static fallbacks
│   │   │   │   └── StudentRequest.kt     # Local request entity & status enum
│   │   │   ├── navigation/     # Jetpack Compose navigation routes
│   │   │   ├── network/        # WebSockets sync, push handlers, Retrofit configs
│   │   │   ├── ui/             # UI Components, Form definitions, Screens
│   │   │   │   ├── forms/      # Input validations & dynamic form structures
│   │   │   │   └── screens/    # Jetpack Compose Screens (Home, Services, Profile...)
│   │   │   └── widget/         # Jetpack Glance widgets & broadcast receivers
│   │   └── res/                # XML styles, app icons, localized strings
│
├── backend-ktor/               # Ktor HTTP REST Server & Admin Portal
│   ├── src/main/kotlin/com/cjlu/backend/
│   │   ├── Application.kt      # Netty configuration, plugins wiring (JWT, WebSockets)
│   │   ├── Database.kt         # H2 DB schema definition & seed registries
│   │   ├── AcademicRepository.kt # Academic read model, course registries, grades
│   │   ├── auth/               # JWT token parsing, password hashing, api-keys
│   │   ├── admin/              # Admin templates, models, routes & controllers
│   │   ├── websocket/          # Live WebSocket session mapping & routing
│   │   └── fcm/                # FCM push delivery integrations
│   └── src/main/resources/     # Admin Portal templates (FreeMarker FTL) & CSS/JS
│
└── cjlu-student-backend/       # Docker Git repository for Hugging Face Space
    ├── Dockerfile              # Two-stage Gradle compilation and runtime build
    └── README.md               # Hugging Face Spaces configuration metadata
```

---

## 3. Detailed Data Models & Schema Registry

### A. Exposed Relational Database Schema (Backend H2)
The backend utilizes Exposed ORM to manage tables in H2:

1. **`students`**: Core account information.
   * `student_id` (VARCHAR(20), Primary Key)
   * `display_name` (VARCHAR(200))
   * `class_section` (VARCHAR(80))
   * `password_hash` (TEXT - BCrypt encoded)
   * `major` (VARCHAR(200))
   * `school` (VARCHAR(200))
   * `overall_attendance_percent` (INTEGER - Defaults to 96)
   * `class_update_notice` (TEXT)
   * `class_update_at_millis` (LONG)

2. **`student_requests`**: Log of student service requests.
   * `id` (VARCHAR(50), Primary Key)
   * `service_id` (VARCHAR(50))
   * `student_id` (VARCHAR(20))
   * `contact_info` (TEXT)
   * `notes` (TEXT)
   * `status` (VARCHAR(20) - `submitted`, `in_review`, `action_needed`, `completed`)
   * `created_at_millis` (LONG)
   * `attachment_url` (VARCHAR(255), Nullable)

3. **`inbox_messages`**: School notifications.
   * `id` (VARCHAR(64), Primary Key)
   * `recipient_student_id` (VARCHAR(32), Nullable - Null indicates a broad announcement)
   * `category` (VARCHAR(32))
   * `sender_en` / `sender_zh` (TEXT)
   * `title_en` / `title_zh` (TEXT)
   * `body_en` / `body_zh` (TEXT)
   * `time_label_en` / `time_label_zh` (VARCHAR(80))
   * `related_service_id` (VARCHAR(50), Nullable)
   * `requires_action` / `starts_unread` (BOOLEAN)
   * `sent_at_millis` (LONG)

4. **`student_message_reads`**: Tracks which student read which broadcast announcement.
   * `student_id` (VARCHAR(20), Primary Key)
   * `message_id` (VARCHAR(64), Primary Key)
   * `read_at_millis` (LONG)

5. **`student_fcm_tokens`**: Registered tokens for Firebase Cloud Messaging.
   * `student_id` (VARCHAR(20))
   * `token` (VARCHAR(512), Primary Key)
   * `updated_at_millis` (LONG)

6. **`class_courses`**: Predefined academic courses catalog.
   * `course_code` (VARCHAR(16), Primary Key)
   * `name_en` / `name_zh` (VARCHAR(200))
   * `credits` (INTEGER)
   * `sort_order` (INTEGER)

7. **`student_course_attendance`**: Course-level attendance stats.
   * `student_id` (VARCHAR(20), Primary Key)
   * `course_code` (VARCHAR(16), Primary Key)
   * `attendance_percent` (INTEGER)
   * `sessions_attended` (INTEGER)
   * `sessions_total` (INTEGER)

8. **`student_weekly_attendance`**: Historical attendance percentage for trends.
   * `student_id` (VARCHAR(20), Primary Key)
   * `week_index` (INTEGER, Primary Key)
   * `week_label` (VARCHAR(8))
   * `percent` (INTEGER)

9. **`student_transcript_grades`**: Grade listings per student per course.
   * `student_id` (VARCHAR(20), Primary Key)
   * `course_code` (VARCHAR(16), Primary Key)
   * `score_percent` (INTEGER)
   * `grade_point` (DOUBLE - Recalculated automatically based on standard GPA scale)

10. **`student_timetable_slots`**: Timetable slots mapped to students.
    * `student_id` (VARCHAR(20), Primary Key)
    * `slot_index` (INTEGER, Primary Key)
    * `day_of_week` (INTEGER)
    * `day_label` (VARCHAR(8))
    * `start_time` / `end_time` (VARCHAR(8))
    * `course_code` (VARCHAR(16))
    * `room_name` (VARCHAR(64))

11. **`student_dormitory`**: Dormitory and off-campus status tracking.
    * `student_id` (VARCHAR(20), Primary Key)
    * `building_name` (VARCHAR(120))
    * `room_number` (VARCHAR(16))
    * `floor` (INTEGER)
    * `bed_label` (VARCHAR(4))
    * `has_active_leave` (BOOLEAN)
    * `leave_reason` / `leave_from_date` / `leave_to_date` (VARCHAR/TEXT, Nullable)
    * `is_off_campus` (BOOLEAN - Defaults to false)
    * `off_campus_address` (TEXT, Nullable)

### B. Room Cache Tables (Android SQLite)
To support offline operation, the client maintains these Room entities:

1. **`student_requests`**: Mirror of the student's requests synced from the server, or created locally.
2. **`inbox_messages`**: Caches synced messages.
3. **`academic_cache`**: Caches raw JSON strings of academic views (Attendance, Transcript, Timetable, Dormitory, Calendar) keyed by `(studentId, cacheKey)`. The `cacheKey` constants are defined in `AcademicCacheKeys.kt` (e.g., `"attendance_detail"`, `"transcript"`, `"timetable"`, `"dormitory"`, `"calendar"`).

---

## 4. REST Endpoints & API Contract

All mobile endpoints require `X-API-Key` in the request headers (verified against the server-wide secret `STUDENT_API_KEY`). Protected routes require a JWT token passed in the `Authorization: Bearer <token>` header.

### A. Authentication
* **`POST /auth/login`**: Authenticates student credentials. Returns a JWT token and profile payload.
* **`POST /auth/change-password`**: Updates the logged-in student's password.
* **`GET /auth/me`**: Fetches the authenticated student's profile information.

### B. Campus Services
* **`GET /services`**: Retrieves the list of available services (localized by `Accept-Language` header).
* **`GET /students/{id}/requests`**: Fetches a student's filed requests.
* **`POST /requests`**: Files a new service request.
* **`POST /requests/{id}/upload`**: Uploads a file attachment (Multipart form) associated with a request ID.

### C. Messages & Alerts
* **`GET /students/{id}/messages`**: Retrieves system messages and announcements.
* **`PATCH /students/{id}/messages/{messageId}/read`**: Marks a message as read or unread.

### D. Academic Data
* **`GET /students/{id}/academic/attendance`**: Returns course attendance details and weekly historical trends.
* **`GET /students/{id}/academic/transcript`**: Returns course scores, grade points, and the computed cumulative GPA.
* **`GET /students/{id}/academic/timetable`**: Returns the student's weekly schedule slots.
* **`GET /students/{id}/dormitory`**: Returns dormitory placements, room details, off-campus status, and active leave records.
* **`GET /students/{id}/academic/calendar`**: Retrieves the institutional academic calendar events.

---

## 5. Caching, Offline-First, and Real-Time Sync Pipelines

### A. Service Catalog Cache and Offline Fallback
To solve network delays and page loading failures:
1. **Initial Load:** The app loads immediately using `ServiceCatalogRepository.getCachedOrFallback(context)`. If a previously saved list is found in `SharedPreferences`, it deserializes and loads instantly. If `SharedPreferences` is empty (cold start), it uses `getFallbackServices()` which returns the 23 default CJLU services hardcoded in code.
2. **Online Sync:** In the background, `loadServices(context)` calls the network API. If successful, it overrides the `SharedPreferences` cache and refreshes the UI. If the request fails, it logs the failure and gracefully falls back to the cache without interrupting the user.

### B. WebSocket Pipeline
A persistent WebSockets connection is established at `ws://<host>:<port>/updates/{studentId}`:
* **Connection Lifecycle:** The client opens the socket upon login. It remains active as long as the app is running in the foreground.
* **Realtime Frame Broadcasts:** The backend publishes targeted DTO frames to notify the app of updates. Examples:
  * `"messages"` (reloads messages inbox)
  * `"academic_updated"` (scope: `"attendance"`, `"timetable"`, `"transcript"`, or `"dormitory"`)
  * `"learning_alerts"` (reloads profile alerts)
  * `"REFRESH"` (triggers request sync)
* **Local Notifications:** If the client receives a frame, it automatically invokes local repository syncs, updates the database cache, refreshes the UI via flow emissions, and pushes a notification to the system tray if relevant.

### C. FCM Push Notifications (Background Fallback)
When the app is killed or running in the background, WebSocket connections are dropped.
* **FCM Registration:** The client registers its FCM Token using `POST /students/{id}/fcm-token`.
* **Server Relay:** Whenever the backend broadcasts a WebSocket update frame, it checks for active FCM tokens mapped to the target student. It builds and posts a data payload mirroring the WebSocket DTO structure via the **Firebase Cloud Messaging HTTP v1 API**.
* **Client Handlers:** A background service (`CjluFirebaseMessagingService`) receives the FCM data message, extracts the payload, updates Room, and displays a system tray notification to alert the user.

---

## 6. Administration Portal & Staff Controls

The backend hosts a web portal (`http://localhost:8080/admin/login`) for staff and administrators.

```text
+--------------------------------------------------------------------------+
|                        ADMIN PORTAL ROUTING TABLE                        |
+--------------------------+-------------+---------------------------------+
| Route                    | Method      | Operation                       |
+--------------------------+-------------+---------------------------------+
| /admin/login             | GET/POST    | Staff Authentication            |
| /admin                   | GET         | Master Dashboard Console        |
| /admin/student-attendance| POST        | Edit Attendance & Push Update   |
| /admin/student-timetable | POST        | Edit Timetable & Push Update    |
| /admin/student-learning  | POST        | Manage Learning Alerts & Banner |
| /admin/student-transcript| POST        | Edit Grades (GP auto-calculates)|
| /admin/student-inbox-msg | POST        | Compose targeted / broad message|
+--------------------------+-------------+---------------------------------+
```

### A. Attendance Management
Staff can generate 7 default courses for any selected student (e.g. Data Structures, OS, Calculus) with initial attendance, or randomize attendance rates to simulate diverse student profiles. Staff can adjust course sessions manually; saving auto-recalculates the overall percentage and pushes the new data to the student's app.

### B. Timetable Slots Editor
Allows staff to define weekly timetable slots for students, assigning days of the week, times, course codes, and classrooms.

### C. Student Learning & Notice Banner
Staff can configure a warning banner that appears at the top of the student's home feed (e.g., warning them of low attendance) and toggle overrides for learning alerts.

### D. Transcript Editor
Administrators can view and edit a student's scores.
* **Auto-calculation of Grade Points:** When the admin posts a new score percentage for a course (e.g., `85`), the backend database interceptor recalculates the corresponding grade point (`3.5`) using the standard China Jiliang University grade-scale formula. The admin panel marks the grade point field as read-only, preventing manual inconsistencies.
* **Cumulative GPA Updates:** Modifying a grade triggers a recalculation of the student's overall GPA based on course credits. The updated transcript is persisted and pushed to the client.

### E. Messaging Hub
Allows administrators to send targeted messages to a specific student or broadcast notifications to all registered students.

---

## 7. Build, Configuration, & Deployment

### A. Environment Configuration & Secrets
Environment variables used to configure the backend:
* `STUDENT_API_KEY`: API Key used to validate mobile client connections.
* `JWT_SECRET`: Secret key used to sign and verify student JWT authentication tokens.
* `ADMIN_USERNAME` / `ADMIN_PASSWORD`: Credentials for the web admin portal.
* `DB_URL` / `CJLU_DB_URL`: JDBC Database Connection URL. If unset, defaults to a local file-based H2 database (`./requests_sql_db.mv.db`).
* `DB_DRIVER` / `DB_USER` / `DB_PASSWORD`: Driver details and database credentials.
* `CJLU_ALLOW_INSECURE_DEV_DEFAULTS`: Allows the server to start with default credentials for local development. Must be disabled in production.
* `CJLU_FCM_SERVICE_ACCOUNT`: File path pointing to the Firebase service account JSON configuration.

### B. Docker Build Specification
The backend includes a two-stage Docker containerization pipeline (`Dockerfile`):
1. **Compilation Stage (`gradle:8.5-jdk21`):** Copies Gradle wrapper files, dependency configurations, the `shared-contract` library, and `backend-ktor` source code into `/src`. Compiles a distribution zip using the `./gradlew :backend-ktor:installDist` task.
2. **Runtime Stage (`eclipse-temurin:21-jre-jammy`):** Copies the compiled distribution into a lightweight image. Creates a non-root user `user` (with UID `1000`) and executes `./bin/backend-ktor` exposed on port `7860`.

### C. Hugging Face Space Integration
The backend is structured to deploy directly as a Docker Space on Hugging Face:
* **Configuration:** Defined in `cjlu-student-backend/README.md` with parameters pointing to the `docker` SDK, app port `7860`, and basic branding emojis.
* **Permissions:** Uses a non-root runtime environment to align with Hugging Face Space security protocols.

### D. Local Compilation Notes (JDK 25 Compiler)
Due to Android Gradle Plugin (AGP) and Java toolchain configurations, client compilation must run with Zulu JDK 25:
```bash
JAVA_HOME=/Library/Java/JavaVirtualMachines/zulu-25.jdk/Contents/Home ./gradlew :app:assembleDebug
```
This resolves Java versioning and compiler differences.
