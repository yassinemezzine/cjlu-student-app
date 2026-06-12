# CJLU Student Platform — Stakeholder Architecture Summary

> **Audience:** Product, leadership, partners · **Purpose:** What works, constraints, risks, and production milestones · **Updated:** 2026-05-22

| Quick links |
|-------------|
| [← Docs index](README.md) · [Overview](cjlu_architecture_overview.md) · **Stakeholder summary** · [ADR review](cjlu_architecture_review_adr.md) · [Onboarding](cjlu_architecture_onboarding.md) |

---

## Executive summary

The CJLU Student platform has a working end-to-end architecture that supports core student experiences (profile, messages, services, attendance, transcript, class schedule, dormitory context) with reliable mobile behavior under poor connectivity.

- **Backend** is the authoritative source of student and academic records.
- **Android app** uses local storage to keep key screens responsive and available when network quality is low.
- The current academic dataset is intentionally mock/generated and is suitable for development, demos, and pilot iteration, but not yet institutional-grade production data.

---

## What is working today

### Student experience coverage

- Profile retrieval
- Service request workflows
- Message/inbox feeds
- Attendance details + trend charts
- Transcript + GPA views
- Class schedule views
- Dormitory information views

### Reliability and usability

- App can fall back to local cache for major academic reads.
- Request workflows retain local state for eventual sync behavior.
- Charting and key academic views are available in the current product surface.

---

## Why this architecture is valuable

1. **Fast iteration**: backend-driven data model allows policy and logic changes without app rewrites.
2. **Mobile resilience**: offline/read fallback improves user trust in variable campus network conditions.
3. **Scalable governance**: centralized backend ownership supports future web/mini-program clients.

---

## Current constraints (important)

1. **Academic data source**
   - Current attendance/grades/timetable are generated from deterministic seed logic.
   - Not yet connected to live registrar/attendance systems.

2. **Mobile cache shape**
   - Some academic data is stored as JSON snapshots, which is efficient now but less flexible for advanced on-device analytics.

3. **Migration readiness**
   - Local database migration currently uses destructive behavior during schema changes.
   - This is acceptable for active development but must be upgraded before broad production rollout.

---

## Risk snapshot

| Area | Level | Notes |
|------|-------|-------|
| Demos / internal pilots | Low–moderate | Suitable for current stage |
| Production scale | Moderate–high | Needs integrations and migration safety |
| Leave consistency | Moderate | Dormitory display vs request workflow not fully unified |

---

## Recommended next milestones

### Milestone 1 — Data authenticity (highest priority)

Integrate registrar and attendance systems to replace generated academics.

### Milestone 2 — Release safety

Move from destructive local migrations to tested incremental migrations.

### Milestone 3 — Consistent leave lifecycle

Unify leave status across request workflow and dormitory display.

### Milestone 4 — Smarter caching

Add freshness/version policy and normalize selected high-value cached data structures.

---

## Success criteria for production readiness

- Academic records are sourced from institutional systems.
- App upgrades do not delete local user-critical state.
- Leave status is consistent across all related screens.
- Cache staleness policy is defined, measurable, and monitored.

---

## Bottom line

The platform foundation is strong for current stage objectives and demonstrates good architecture direction. With focused investment in data integration, migration safety, and state consistency, it can transition from pilot-quality to production-grade student infrastructure.

---

## Related documentation

| Document | Best for |
|----------|----------|
| [Architecture index](README.md) | Choose the right doc by role |
| [Architecture overview](cjlu_architecture_overview.md) | Technical depth (schema, APIs, data flow) |
| [Architecture review (ADR)](cjlu_architecture_review_adr.md) | Recorded decisions and acceptance signals |
| [Engineering onboarding](cjlu_architecture_onboarding.md) | How the team builds and ships changes |
