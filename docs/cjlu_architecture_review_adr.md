# CJLU Student App & Backend — Architecture Review (ADR)

> **Audience:** Tech leads, reviewers · **Purpose:** Recorded decisions, trade-offs, and acceptance signals · **Updated:** 2026-05-22

| Quick links |
|-------------|
| [← Docs index](README.md) · [Overview](cjlu_architecture_overview.md) · [Stakeholder summary](cjlu_architecture_stakeholder_summary.md) · **ADR review** · [Onboarding](cjlu_architecture_onboarding.md) |

---

## Context

The CJLU Student platform consists of:

- Android client using Room for local persistence and offline fallback.
- Ktor backend using Exposed + H2 for system data, workflows, and academic APIs.

The team needs a clear architecture position on data ownership, cache shape, migration strategy, and path to production-grade academic integrations.

---

## Decision 1 — Backend as single source of truth

| Field | Value |
|-------|-------|
| **Status** | Accepted |
| **Decision** | All canonical student, workflow, catalog, and academic entities are owned by the backend. Android persists only operational and cache state needed for UX resilience. |
| **Rationale** | Prevents business-rule divergence; supports centralized policy and auditing; simplifies multi-client consistency. |
| **Consequences** | Requires reliable API contracts and versioning; offline UX depends on cache quality and invalidation. |

---

## Decision 2 — Client-side academic cache as JSON snapshots

| Field | Value |
|-------|-------|
| **Status** | Accepted (transitional) |
| **Decision** | Use `academic_cache` (JSON payload by `studentId + cacheKey`) for attendance/transcript/timetable/dormitory offline reads. |
| **Rationale** | Fast implementation for multi-shape payloads; low schema churn in early phases. |
| **Consequences** | Limited on-device queryability; coarse invalidation; harder partial updates and reconciliation. |
| **Follow-up** | Evaluate normalized Room entities for high-value query surfaces (e.g., transcript/course-level views). |

---

## Decision 3 — Room destructive migration in current stage

| Field | Value |
|-------|-------|
| **Status** | Accepted (temporary, risk acknowledged) |
| **Decision** | Room schema version upgrades currently use destructive migration. |
| **Rationale** | Development speed during active model iteration. |
| **Risks** | Local data loss on upgrade; reduced confidence for pilot/production cohorts. |
| **Exit criteria** | Before broader rollout: (1) tested incremental migrations, (2) migration verification in CI, (3) retained/offline-critical data policy. |

---

## Decision 4 — Seeded academic dataset for development

| Field | Value |
|-------|-------|
| **Status** | Accepted (non-production) |
| **Decision** | Academic data is generated/seeded deterministically (per student identity) from seed classes and repository seed routines. |
| **Rationale** | Predictable demos and deterministic testing; decouples dev from registrar dependencies. |
| **Consequences** | Data realism gap; policy edge cases (corrections, appeals) not represented. |
| **Follow-up** | Replace generators with registrar and attendance-system integrations. |

---

## Decision 5 — Workflow vs domain leave state separation

| Field | Value |
|-------|-------|
| **Status** | Observed design (needs harmonization) |
| **Observation** | Dormitory leave fields and `ask_leave` request workflow are not yet a unified state machine. |
| **Risk** | Users can perceive inconsistencies between displayed leave status and workflow outcomes. |
| **Recommendation** | Introduce a unified leave domain model (`submitted` → `under_review` → `approved` / `rejected` / `cancelled`); make dormitory display derive from workflow state. |

---

## Current architecture fit assessment

### Strengths

- Clear backend ownership boundaries.
- Good local resilience pattern for mobile reads.
- Deterministic seed strategy supports stable development.
- Surface-level feature coverage across attendance/transcript/timetable/dormitory.

### Weaknesses

- Temporary migration strategy unsuitable for production.
- JSON cache limits long-term client analytics/query features.
- Generated academics not equivalent to institutional truth.

### Overall

Architecture is appropriate for prototype-to-pilot maturity and can evolve to production through focused improvements in data integration, migration safety, and state unification.

---

## Recommended architecture roadmap (prioritized)

1. **Data trust path**: integrate registrar and attendance systems for academic truth.
2. **Schema safety**: replace destructive Room migrations with incremental migrations.
3. **Cache strategy**: introduce freshness metadata (timestamp/version/etag) and SWR semantics.
4. **Leave domain coherence**: unify leave workflow and dormitory leave presentation.
5. **Selective normalization**: normalize high-value academic payloads in Room where needed.

---

## Acceptance signals

- No destructive migration in release builds.
- Academic endpoints backed by non-seeded institutional integrations.
- Leave state shown consistently across workflow and dormitory views.
- Defined cache invalidation policy with measurable stale-read rates.

---

## Related documentation

| Document | Best for |
|----------|----------|
| [Architecture index](README.md) | Choose the right doc by role |
| [Architecture overview](cjlu_architecture_overview.md) | Implementation detail behind these decisions |
| [Stakeholder summary](cjlu_architecture_stakeholder_summary.md) | Executive framing of risks and milestones |
| [Engineering onboarding](cjlu_architecture_onboarding.md) | How engineers apply ADRs in day-to-day work |
