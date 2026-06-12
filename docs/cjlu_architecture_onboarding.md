# CJLU Student App — Engineering Onboarding Guide

> **Audience:** New engineers · **Purpose:** Day-1 paths, repos, pitfalls, and starter tasks · **Updated:** 2026-05-22

| Quick links |
|-------------|
| [← Docs index](README.md) · [Overview](cjlu_architecture_overview.md) · [Stakeholder summary](cjlu_architecture_stakeholder_summary.md) · [ADR review](cjlu_architecture_review_adr.md) · **Onboarding** |

---

## Purpose

This guide helps new engineers understand how the CJLU Student app and backend are structured, where data lives, and where to make safe changes first.

---

## 1) System mental model

- **Android app**: user interface + local persistence + offline fallback.
- **Ktor backend**: source of truth for student data, requests, catalog content, messages, and academic APIs.
- **Contract layer**: DTOs over REST.

Think of Android Room as a reliability layer and UX accelerator, not the canonical datastore.

```text
UI → Repository → Retrofit → Backend → Exposed/H2
         ↓ (on failure)
       Room cache / request queue
```

---

## 2) Repositories and key areas

### Android

Primary path: `app/src/main/java/com/cjlu/studentapp/`

| Area | Key types / routes |
|------|---------------------|
| Data layer | `AppDatabase`, `StudentRequestDao`, `AcademicCacheDao`, `RequestManager`, `AcademicRepository` |
| Navigation | `home`, `services`, `messages`, `profile`; `attendance_detail`; `service_hub/{id}`, `service_detail/{id}` |
| Realtime | `RealtimePushHandler` (WebSocket payloads) |

### Backend

Primary path: `backend-ktor/`

| Area | Key files |
|------|-----------|
| Routes | `Application.kt` |
| Core DB | `Database.kt` (auth, messages, requests) |
| Academics | `AcademicRepository.kt` (reads/writes, seed logic) |

---

## 3) Persistence quick reference

### Android Room (`cjlu_database`, version 2)

| Table | Role |
|-------|------|
| `student_requests` | Durable request workflow/sync state |
| `academic_cache` | JSON snapshots keyed by `studentId + cacheKey` |

Current migration mode is **destructive**; do not assume cache/request data survives schema bumps.

### Backend H2 (`requests_sql_db`)

**Core:** `students`, `student_requests`, `catalog_services`, `inbox_messages`, `student_message_reads`, `app_config`

**Academic:** `class_courses`, `student_course_attendance`, `student_weekly_attendance`, `student_transcript_grades`, `student_timetable_slots`, `student_dormitory`

---

## 4) Endpoints to know first

| Endpoint | Domain |
|----------|--------|
| `GET /students/{id}/profile` | Profile |
| `GET .../academic/attendance` | Attendance |
| `GET .../academic/transcript` | Transcript / GPA |
| `GET .../academic/timetable` | Timetable |
| `GET .../dormitory` | Dormitory |
| Request CRUD + sync | Service workflows |

**Trace pattern:** UI call site → repository method → DTO → backend route → Exposed query.

---

## 5) Day-1 / day-7 onboarding plan

### Day 1

1. Run app + backend locally.
2. Log in with seeded student account (password = student ID).
3. Navigate all bottom tabs and service detail screens.
4. Simulate offline and verify Room fallback behavior.

### Day 2–3

1. Trace one academic flow end-to-end (attendance recommended).
2. Trace one request workflow flow end-to-end.
3. Inspect how seed data is loaded on backend startup.

### Day 4–7

1. Make a small, isolated change (e.g., additional profile field or service metadata).
2. Add/adjust DTO and endpoint contract.
3. Validate app fallback and failure behavior.

---

## 6) Safe change strategy

- Prefer additive DTO changes first.
- Keep backend as source of truth; avoid business logic drift in client.
- For cached academic data, preserve key stability in `academic_cache`.
- If Room schema must change, prepare migration plan before release branches.

---

## 7) Common pitfalls

1. Treating seeded academic data as production-accurate.
2. Assuming dormitory leave data is workflow-approved leave state.
3. Breaking cache keys and invalidating offline reads unexpectedly.
4. Introducing API changes without backward-compatible DTO handling.

---

## 8) First good starter tasks

- Add freshness metadata to academic cache rows.
- Improve request sync error reporting from `RequestManager`.
- Add backend validation for one request type.
- Add integration test for one academic endpoint payload shape.

---

## 9) Current data reality

- Roster: **47 students** (22 L1 + 25 L2).
- Academic values are deterministic/generated for development realism, not live registrar sourced.

For full system detail, read [Architecture overview](cjlu_architecture_overview.md). For decisions and trade-offs, read [Architecture review (ADR)](cjlu_architecture_review_adr.md).

---

## Related documentation

| Document | Best for |
|----------|----------|
| [Architecture index](README.md) | Choose the right doc by role |
| [Architecture overview](cjlu_architecture_overview.md) | Deep dive after onboarding |
| [Stakeholder summary](cjlu_architecture_stakeholder_summary.md) | Product context and milestones |
| [Architecture review (ADR)](cjlu_architecture_review_adr.md) | Why the system is shaped this way |
