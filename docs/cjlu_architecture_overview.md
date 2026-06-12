# CJLU Student App & Backend — Architecture Overview

> **Audience:** Engineers, architects · **Purpose:** Technical deep dive (persistence, APIs, data flow, gaps) · **Updated:** 2026-05-22

| Quick links |
|-------------|
| [← Docs index](README.md) · **Overview** · [Stakeholder summary](cjlu_architecture_stakeholder_summary.md) · [ADR review](cjlu_architecture_review_adr.md) · [Onboarding](cjlu_architecture_onboarding.md) |

---

## 1) System architecture at a glance

```text
+----------------------------------------+          +-----------------------------------------+
|             ANDROID CLIENT             |          |             KTOR BACKEND                |
|  Room (SQLite: cjlu_database)          |  HTTP    |  Exposed ORM + H2 File Database         |
|  - student_requests (sync queue)       | <======> |  System of record for profiles,         |
|  - academic_cache (offline snapshots)  |  REST    |  catalogs, inbox, academic datasets     |
+----------------------------------------+          +-----------------------------------------+
```

| Layer | Technology | Responsibility |
|---|---|---|
| Android | Room v2 (`cjlu_database`) | Local-first reads for selected academic views; durable queue for request workflows |
| Backend | Ktor + Exposed + H2 (`requests_sql_db`) | Authentication, profiles, service workflows, content catalogs, inbox, academic APIs |

### Ownership model

- **Backend is source of truth** for all canonical student/domain data.
- **Android stores derived/cache state** for resilience and offline UX:
  - `student_requests`: workflow + sync state.
  - `academic_cache`: JSON snapshots keyed by `studentId + cacheKey`.

---

## 2) Persistence and schema overview

### Android (Room)

Location: `app/src/main/java/com/cjlu/studentapp/data/`

| Setting | Value |
|---|---|
| Database name | `cjlu_database` |
| Schema version | `2` |
| Migration strategy | Destructive migration on version bump |

#### Room tables

| Table | Primary purpose |
|---|---|
| `student_requests` | Local queue and status tracking for service request lifecycle |
| `academic_cache` | Cached JSON payloads for attendance, transcript, dormitory, timetable |

#### Key Android data-layer types

- `AppDatabase`
- `StudentRequestDao`
- `AcademicCacheDao`
- `RequestManager`
- `AcademicRepository`

### Backend (Exposed + H2)

Location: `backend-ktor/.../Database.kt`

#### Core tables

- `app_config`
- `students`
- `student_requests`
- `catalog_services`
- `inbox_messages`
  - `recipient_student_id = null` → broadcast to all students
  - `recipient_student_id = <id>` → targeted delivery
- `student_message_reads`

#### Academic tables

Location: `AcademicRepository.kt`

| Table | Purpose |
|---|---|
| `class_courses` | Shared 7-course class catalog |
| `student_course_attendance` | Per-student, per-course attendance metrics |
| `student_weekly_attendance` | Weekly trend series for charting |
| `student_transcript_grades` | Course scores + grade points |
| `student_timetable_slots` | Weekly timetable slot assignments |
| `student_dormitory` | Building 13 placement, room/bed, leave metadata |

---

## 3) API + DTO coverage map

### Implemented and wired end-to-end

| Domain | DTO / Model | Endpoint | Room cache |
|---|---|---|---|
| Student profile | `StudentProfileDto` | `GET /students/{id}/profile` | No |
| Attendance detail | `StudentAttendanceDetailDto` | `GET .../academic/attendance` | Yes |
| Transcript / GPA | `StudentTranscriptDto` | `GET .../academic/transcript` | Yes |
| Timetable | `StudentTimetableDto` | `GET .../academic/timetable` | Yes |
| Dormitory | `StudentDormitoryDto` | `GET .../dormitory` | Yes |
| Service requests | `StudentRequest` | CRUD + sync endpoints | Yes (`student_requests`) |
| Messages / catalog | `MessageDto`, `CatalogServiceDto` | List endpoints | No |

### Current mock boundaries (non-production academics)

- Timetables come from seeded templates (not registrar-originated).
- Attendance and grades are deterministic from `studentId`-based generation.
- No barcode- or geolocation-backed attendance capture.
- Dormitory leave payload is mock and independent of `ask_leave` workflow approval state.

---

## 4) Roster, seeds, and deterministic dataset generation

### Roster

- `ClassRoster.kt` / `RosterSeed.kt` define **47 students** total:
  - Level 1: 22
  - Level 2: 25
- `classSection` on profile determines which academic variants apply.

### Seed sources

| Source | Dataset |
|---|---|
| `RosterSeed.kt` | 47 student accounts (default password = student ID) |
| `ContentSeed.kt` | 25 catalog services, 5 inbox messages |
| `AcademicDataSeed.kt` | 7 courses, Building 13 dorm model, timetable templates |
| `AcademicRepository.seedAllRosterStudents()` | Per-student attendance, weekly trends, transcript rows, timetable, dorm data |

---

## 5) Runtime data flow

### Android request/read flow

```text
UI Screens
  → AcademicRepository / RequestManager / MessagesRepository
    → Retrofit API call
      → success: persist snapshot/state to Room
      → failure: fallback to Room (academic_cache + student_requests)
```

### Backend dispatch flow

```text
Application.kt routes
  → Database.kt (auth, messages, requests)
  → AcademicRepository (academic read/write paths)
```

---

## 6) Navigation and surface mapping (Android)

| Route | Purpose |
|---|---|
| `home`, `services`, `messages`, `profile` | Bottom tabs via `navigateToMainTab` |
| `attendance_detail` | Attendance drill-down from Home |
| `service_hub/{id}` → `service_detail/{id}` | Catalog browsing + submit flow |
| transcript/schedule/dorm views | Embedded in `ServiceDetailScreen` for selected catalog IDs |

Additional notes:

- WebSocket payloads are parsed by `RealtimePushHandler` (used by `MainActivity`).
- Admin HTML under `/admin/*` is operationally separate from student request endpoint `POST /requests`.

---

## 7) UI feature coverage

| Feature | Entry points |
|---|---|
| Attendance detail + weekly chart | Home metric card; service `attendance_rate` |
| Transcript + GPA | Service `transcripts` |
| Class schedule | Hero card; quick action; service `class_schedule` |
| Dormitory + leave UI | Services `changing_room`, `ask_leave` |

Rendering note: charts are implemented with Compose `Canvas` (no external chart library dependency).

---

## 8) Operational summary

```text
1) Service workflow state:  Android Room (queue/cache) ↔ Backend H2
2) Identity and alerts:     Backend-owned (profile + overallAttendancePercent)
3) Academic read model:     Backend H2 tables → REST DTOs → Android academic_cache
4) Catalog/content feeds:   Backend seeds + REST (currently uncached on device)
```

---

## 9) Gaps, risks, and productionization roadmap

### Known gaps

1. **Academic data realism**: generated values simplify testing but do not model real registrar latency, corrections, or policy exceptions.
2. **Cache normalization**: JSON blob storage in `academic_cache` is flexible but limits selective invalidation and queryability.
3. **Migration strategy**: destructive Room migration risks local data loss during schema evolution.
4. **Consistency model**: leave in dormitory payload and leave request workflow can diverge semantically.

### Recommended next steps

1. Replace seed generators with registrar and attendance-system integrations.
2. Add explicit write APIs for leave creation/approval and unify leave state across modules.
3. Introduce cache freshness metadata (timestamp/ETag/version) and stale-while-revalidate behavior.
4. Migrate high-value academic payloads from JSON blobs to normalized Room entities (course/grade/attendance tables) where query needs justify it.
5. Move from destructive migrations to tested incremental Room migrations before broader rollout.

---

## Related documentation

| Document | Best for |
|----------|----------|
| [Architecture index](README.md) | Choose the right doc by role |
| [Stakeholder summary](cjlu_architecture_stakeholder_summary.md) | Product and leadership narrative |
| [Architecture review (ADR)](cjlu_architecture_review_adr.md) | Decisions, trade-offs, acceptance criteria |
| [Engineering onboarding](cjlu_architecture_onboarding.md) | Day-1 setup and safe change patterns |
