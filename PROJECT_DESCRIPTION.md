# CJLU Student App & Backend — Detailed Project Description

The **CJLU Student App** is a full-stack student management mobile system developed for **China Jiliang University (CJLU)**. It consists of a Kotlin-based Jetpack Compose Android client app, a Ktor HTTP & WebSockets backend, and an administration dashboard portal.

---

## 1. System Architecture

The project is structured as a multi-module Gradle project with three primary sub-projects:

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

### A. Android Client App (`/app`)

A modern, native Android application built using:

* **UI Framework**: Jetpack Compose (Kotlin declarative UI) with Material 3 styling.
* **Network Client**: Retrofit 2 & OkHttp 3 for REST endpoint communication.
* **Local Persistence (Room)**: Uses a local SQLite database (`cjlu_database`) to support local-first reads and an offline-first sync queue.
* **Home Screen Widget**: Built with Jetpack Glance to display active requests and unread message counts.
* **Realtime Handler**: Implemented via HTML5 WebSockets to receive push alerts, message updates, and request changes.
* **Push Notifications**: Integrated with Firebase Cloud Messaging (FCM) to trigger background updates.

### B. Backend Web Server (`/backend-ktor`)

An asynchronous Kotlin web server built on:

* **Server Framework**: Ktor, using Netty.
* **Persistence & ORM**: Exposed ORM with an H2 file-backed relational database (`requests_sql_db.mv.db`).
* **Templating Engine**: Apache FreeMarker to render the server-side Web Admin Portal interface.
* **Admin Portal**: An administrative tool letting staff review, filter, and approve student requests, correct attendance rates, and broadcast campus announcements.
* **WebSockets**: A persistent WebSocket hub (`WebSocketHub`) that triggers updates directly to connected student devices.

### C. Shared Serialization Module (`/shared-contract`)

A shared library containing all Kotlin data transfer objects (DTOs) and serialization schemas (`kotlinx.serialization`). It is imported by both the Android app and Ktor backend to guarantee end-to-end API contract safety.

---

## 2. Key Features

| Feature Area | Description |
| :--- | :--- |
| **Identity & Profiles** | JWT-authenticated registration/login, device profile edits, and secure password updates. |
| **Academic Performance** | Visual representation of course-level and overall attendance metrics, weekly historical trends, current GPA, and official transcript grades. |
| **Class Schedules** | Comprehensive weekly timetables displaying course names, rooms, times, and instructors. |
| **Dormitory Management** | Room/bed placements, leave updates, and request logs. |
| **Campus Services (25+ workflows)** | Workflows for sick leaves (`ask_leave`), scholarship applications (`scholarship`), room changes (`changing_room`), credit confirmation, and visa extensions. Features file attachment uploads and background queue sync. |
| **Inbox & Messaging** | A notification feed displaying broadcasted system announcements or targeted admin messages. |
| **Realtime WebSockets** | Instant updates for learning alerts, dormitory leave state changes, and attendance corrections. |

---

## 3. Data & Persistence Model

### A. Android App (Room DB)

* **`academic_cache`**: Caches Snapshots of academic data (Attendance, Transcripts, Timetables, and Dormitory information) in JSON format for offline rendering.
* **`student_requests`**: Stores student service requests locally with synchronization state trackers.

### B. Ktor Server (H2 DB via Exposed)

* **Student Accounts (`students`)**: Holds enrollment information, hashed credentials, overall attendance, and major details.
* **Service Requests (`student_requests`)**: System of record for student requests, status tracking (`Submitted`, `InReview`, `Completed`, `Rejected`), and uploaded file paths.
* **Inbox Messages (`inbox_messages`)**: Stores notifications. Broad reception is supported by setting `recipient_student_id` to `null`.
* **Academic Databases**: Modules tracking course registries, attendance detail patches, weekly trends, grades, and room placements.

---

## 4. Deterministic Seed Roster

To facilitate local development and verification, the backend automatically initializes an in-memory or file database with:

* **Roster**: **47 predefined student profiles** categorized into different course blocks (Level 1 and Level 2).
* **Initial State**: Each student profile gets seeded with personalized academic data (attendance details, transcripts, timetables, and dorm placements).
* **Credentials**: The default password for all seed accounts is set as their respective **Student ID** (e.g., student `20230901` uses password `20230901`).
