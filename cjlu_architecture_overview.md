# CJLU Student App & Backend — Current System Architecture & Implementation Report

This report details the technical architecture, database schemas, model mappings, caching systems, and real-time sync pipelines implemented in the **CJLU Student App & Backend** ecosystem for **China Jiliang University (CJLU)**. 

---

## 1. System Overview & Architecture

The system is designed as a multi-module, offline-first application with a real-time synchronization layer. It connects an Android client app to a Kotlin Ktor backend server that hosts both the REST/WebSocket APIs and a FreeMarker-rendered Administration Web Portal.

```text
+----------------------------------------+          +-----------------------------------------+
|             ANDROID CLIENT             |          |             KTOR BACKEND                |
|  Room DB (SQLite: cjlu_database)       |  HTTP    |  Exposed ORM + H2 File Database         |
|  Caches requests, academic data,       | <======> |  Central source of truth for profiles,  |
|  and message inbox locally             |  REST    |  catalogs, messages, and state          |
|  app/src/main/java/com/cjlu/.../data/  |  WS/FCM  |  FreeMarker Administration Web Portal   |
+----------------------------------------+          +-----------------------------------------+
```

* **Android (Local Layer):** Uses Room (SQLite) database (`cjlu_database`) to implement local caching and offline-first functionality. Local data persistence includes:
  * Caching student service requests (`student_requests`).
  * Caching inbox notifications (`inbox_messages`).
  * Caching complex academic states (`academic_cache` table) via JSON-serialized payloads for offline reading.
  * Caching the service catalog catalog via `SharedPreferences` with a static fallback list to prevent slow load times or visual disappearances.
* **Backend Layer:** Powered by Kotlin Ktor on the Netty engine, utilizing Jetpack Exposed ORM speaking to an embedded H2 file-backed relational database (`requests_sql_db.mv.db`).
* **Domain Implementation:** Unlike early mock phases, the current system implements **fully structured database entities** for academic classes, timetables, course-level attendance, weekly trends, dormitory rooms, off-campus housing status, and academic transcript grades.

---

## 2. Database Schema Setup

### Android Client — Room Schema (`cjlu_database`)
The local database is current on **Version 5** and contains three primary tables:

1. **`student_requests`**: Stores student service requests.
   * `id` (TEXT, Primary Key)
   * `serviceId` (TEXT)
   * `studentId` (TEXT)
   * `contactInfo` (TEXT)
   * `notes` (TEXT)
   * `status` (TEXT)
   * `createdAtMillis` (INTEGER)
   * `attachmentUrl` (TEXT, Nullable)
2. **`inbox_messages`**: Caches system-wide and targeted inbox notifications.
   * `id` (TEXT, Primary Key)
   * `studentId` (TEXT)
   * `category` (TEXT)
   * `sender` (TEXT)
   * `title` (TEXT)
   * `body` (TEXT)
   * `timeLabel` (TEXT)
   * `relatedServiceId` (TEXT, Nullable)
   * `requiresAction` (INTEGER)
   * `isRead` (INTEGER)
3. **`academic_cache`**: Offline JSON cache for academic views.
   * `studentId` (TEXT, Primary Key)
   * `cacheKey` (TEXT, Primary Key - e.g. `"attendance_detail"`, `"transcript"`, `"timetable"`, `"dormitory"`, `"calendar"`)
   * `payloadJson` (TEXT)
   * `fetchedAtMillis` (INTEGER)
   * `sourceVersion` (TEXT - e.g., `"api-v1"`, `"seeded-v1"`)

---

### Backend Server — Exposed ORM + H2 Schema
The server initializes an H2 relational database containing 11 tables:

1. **`students`**: User credentials and profile states.
   * `student_id` (VARCHAR(20), PK)
   * `display_name` (VARCHAR(200))
   * `class_section` (VARCHAR(80))
   * `password_hash` (TEXT - BCrypt encoded)
   * `major` (VARCHAR(200))
   * `school` (VARCHAR(200))
   * `overall_attendance_percent` (INTEGER)
   * `class_update_notice` (TEXT)
   * `class_update_at_millis` (LONG)
2. **`student_requests`**: Central repository for submitted requests.
   * `id` (VARCHAR(50), PK)
   * `service_id` (VARCHAR(50))
   * `student_id` (VARCHAR(20))
   * `contact_info` (TEXT)
   * `notes` (TEXT)
   * `status` (VARCHAR(20) - `submitted`, `in_review`, `action_needed`, `completed`)
   * `created_at_millis` (LONG)
   * `attachment_url` (VARCHAR(255), Nullable)
3. **`catalog_services`**: Settings and metadata for forms.
   * `id` (VARCHAR(50), PK)
   * `category` (VARCHAR(32))
   * `title_en` / `title_zh` (TEXT)
   * `desc_en` / `desc_zh` (TEXT)
   * `turnaround_en` / `turnaround_zh` (VARCHAR(120))
   * `checklist_en` / `checklist_zh` (TEXT)
   * `is_popular` (BOOLEAN)
4. **`inbox_messages`**: Central repository of notifications.
   * `id` (VARCHAR(64), PK)
   * `recipient_student_id` (VARCHAR(32), Nullable - Null represents a broadcast announcement)
   * `category` (VARCHAR(32))
   * `sender_en` / `sender_zh` (TEXT)
   * `title_en` / `title_zh` (TEXT)
   * `body_en` / `body_zh` (TEXT)
   * `time_label_en` / `time_label_zh` (VARCHAR(80))
   * `related_service_id` (VARCHAR(50), Nullable)
   * `requires_action` / `starts_unread` (BOOLEAN)
   * `sent_at_millis` (LONG)
5. **`student_message_reads`**: Links student reads to broadcast notifications.
   * `student_id` (VARCHAR(20), PK)
   * `message_id` (VARCHAR(64), PK)
   * `read_at_millis` (LONG)
6. **`student_fcm_tokens`**: Firebase cloud messaging credentials.
   * `student_id` (VARCHAR(20))
   * `token` (VARCHAR(512), PK)
   * `updated_at_millis` (LONG)
7. **`class_courses`**: Predefined academic courses registry.
   * `course_code` (VARCHAR(16), PK)
   * `name_en` / `name_zh` (VARCHAR(200))
   * `credits` (INTEGER)
   * `sort_order` (INTEGER)
8. **`student_course_attendance`**: Granular attendance records.
   * `student_id` (VARCHAR(20), PK)
   * `course_code` (VARCHAR(16), PK)
   * `attendance_percent` (INTEGER)
   * `sessions_attended` (INTEGER)
   * `sessions_total` (INTEGER)
9. **`student_weekly_attendance`**: Historical weekly attendance curves.
   * `student_id` (VARCHAR(20), PK)
   * `week_index` (INTEGER, PK)
   * `week_label` (VARCHAR(8))
   * `percent` (INTEGER)
10. **`student_transcript_grades`**: Grade listings per student per course.
    * `student_id` (VARCHAR(20), PK)
    * `course_code` (VARCHAR(16), PK)
    * `score_percent` (INTEGER)
    * `grade_point` (DOUBLE)
11. **`student_timetable_slots`**: Custom schedule slots.
    * `student_id` (VARCHAR(20), PK)
    * `slot_index` (INTEGER, PK)
    * `day_of_week` (INTEGER)
    * `day_label` (VARCHAR(8))
    * `start_time` / `end_time` (VARCHAR(8))
    * `course_code` (VARCHAR(16))
    * `room_name` (VARCHAR(64))
12. **`student_dormitory`**: Dormitory and housing placement tracking.
    * `student_id` (VARCHAR(20), PK)
    * `building_name` (VARCHAR(120))
    * `room_number` (VARCHAR(16))
    * `floor` (INTEGER)
    * `bed_label` (VARCHAR(4))
    * `has_active_leave` (BOOLEAN)
    * `leave_reason` / `leave_from_date` / `leave_to_date` (VARCHAR/TEXT, Nullable)
    * `is_off_campus` (BOOLEAN)
    * `off_campus_address` (TEXT, Nullable)

---

## 3. Entity & Model Inventory

| Domain | Model Class / Representation | Database Entity? | Technical Description / Fields |
| :--- | :--- | :--- | :--- |
| **Student Profile** | `StudentProfileDto` | **Yes** (Backend `students`) | Core properties: `studentId`, `displayName`, `classSection`, `major`, `school`, `overallAttendance`, `classUpdateNotice`. |
| **Service Requests**| `StudentRequest` | **Yes** (Both Layers) | Syncs client-side submissions to server: `serviceId`, `contactInfo`, `notes`, `status`, `attachmentUrl`. |
| **Messages** | `MessageDto` | **Yes** (Both Layers) | Syllabic announcements and updates: `id`, `category`, `sender`, `title`, `body`, `timeLabel`, `isRead`. |
| **Service Catalog** | `CatalogServiceDto` | **Yes** (Backend + Client Prefs) | Holds forms metadata: `id`, `category`, `title`, `description`, `turnaround`, `checklist`, `isPopular`. |
| **Attendance** | `StudentAttendanceDetailDto` | **Yes** (Backend tables) | Holds course-by-course metrics: `courses` (list of `CourseAttendanceDto`) and `weeklyTrend` (list of `WeeklyAttendanceDto`). |
| **Transcripts & GPA**| `StudentTranscriptDto` | **Yes** (Backend table) | Aggregates grade points: `courses` (list of `TranscriptCourseDto` containing `scorePercent` and `gradePoint`) and `cumulativeGpa`. |
| **Timetables** | `StudentTimetableDto` | **Yes** (Backend table) | Schedules course slots: `slots` (list of `TimetableSlotDto` with day of week, timeslots, and room numbers). |
| **Dormitory** | `StudentDormitoryDto` | **Yes** (Backend table) | Tracks housing assets: `buildingName`, `roomNumber`, `floor`, `bedLabel`, `hasActiveLeave`, `isOffCampus`, `offCampusAddress`. |

---

## 4. Seed & Mock Data Registry

The database automatically boots with a deterministic dataset:
* **Students Roster:** Seeds **47 predefined student profiles** split across two main sections (`23计算机L1` and `23计算机L2`).
* **Credentials:** All seed profiles default to using their unique **Student ID** as their password.
* **Academic Data:** Seeding triggers automatic registration for 5 to 7 courses (e.g. Data Structures, OS, Java, Chinese Language) with initial attendance, timetable slots, transcript scores, and dormitory room placements.
* **Messages:** Seeded notifications, including warnings and general campus updates, are pre-loaded on boot.

---

## 5. Repositories & Real-Time Sync Pipelines

### Client-Side Caching Flow
* **`RequestManager`**: Handles offline queuing, REST submissions, and Multipart attachment uploads to the backend. Syncs list changes directly to the Room SQLite table.
* **`MessagesRepository`**: Synchronizes message states. Upon successful retrieval, it caches them in Room (`inbox_messages`), allowing instant offline message loads.
* **`AcademicRepository`**: Observes, caches, and retrieves academic payloads (Timetable, Transcript, Attendance, Dormitory) from Room SQLite (`academic_cache`), keeping the screens active while offline.
* **`ServiceCatalogRepository`**: Implements JSON caching via `SharedPreferences` for the catalog. Falling back to the static catalog (`getFallbackServices()`) ensures the services tab loads instantly on first open or network failures.

### Real-Time Update Pipeline
1. **Active Foreground Socket:** The client initiates a WebSocket connection to `ws://<host>:<port>/updates/{studentId}` upon login.
2. **WebSocket Hub Broadcasts:** The backend publishes simple JSON frames (`"messages"`, `"academic_updated"`, `"learning_alerts"`, `"REFRESH"`) whenever an administrator alters data.
3. **FCM Push Relay:** If the student is offline or the app is killed, the backend automatically routes updates to active registered device tokens via the **Firebase Cloud Messaging HTTP v1 API**.
4. **Glance Widget Refresh:** Received sync events trigger background updates to local Room caches, which automatically refresh Compose views and rebuild the Glance home screen widget counts.

---

## 6. Administration Web Portal

The backend hosts a FreeMarker template portal enabling administrator staff to manage states interactively:

```text
+-------------------------------------------------------------------------------+
|                            ADMIN OPERATION PORTAL                             |
+--------------------------+----------------------------------------------------+
| Feature Panel            | Administrative Controls                            |
+--------------------------+----------------------------------------------------+
| Attendance Dashboard     | Generate courses, randomize rates, edit sessions.  |
| Timetable Slot Matrix    | Re-assign slot periods, course codes, and rooms.   |
| Notice Banner Editor     | Set warnings or custom notices shown on app home.  |
| Transcript Grades Editor | Edit course scores; GPA and GP auto-calculate.    |
| Broadcasting Center      | Broadcast announcements or send targeted alerts.   |
+--------------------------+----------------------------------------------------+
```

### Transcript Auto-Calculation Engine
* **Auto-GP Formula:** When an administrator edits a student's score percentage (0–100%) in the dashboard, the backend Exposed database hook intercepts the transaction. It automatically computes the grade point (GP) using China Jiliang University's grading standard:
  $$\text{Grade Point} = \max\left(0.0, \frac{\text{Score} - 50}{10}\right)$$
  *(e.g., Score $85 \rightarrow \text{GP } 3.5$; Score $< 60 \rightarrow \text{GP } 0.0$)*
* **Cumulative GPA Calculation:** The engine recalculates the student's cumulative GPA by taking the credit-weighted average of all course grade points:
  $$\text{GPA} = \frac{\sum (\text{GP}_i \times \text{Credits}_i)}{\sum \text{Credits}_i}$$
* **Instant Update Push:** Upon transaction commit, the portal fires a WebSockets notification (`academic_updated` with scope `"transcript"`), triggering the client app to automatically fetch the revised transcript.
