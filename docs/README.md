# CJLU Student Platform — Architecture Documentation

Mini index for the CJLU Student Android app and Ktor backend. Pick one doc by role or task.

| Document | Audience | Read this when you need… |
|----------|----------|---------------------------|
| [Architecture overview](cjlu_architecture_overview.md) | Engineers, architects | Full technical picture: persistence, APIs, data flow, gaps |
| [Stakeholder summary](cjlu_architecture_stakeholder_summary.md) | Product, leadership, partners | What works today, risks, milestones, production readiness |
| [Architecture review (ADR)](cjlu_architecture_review_adr.md) | Tech leads, reviewers | Recorded decisions, trade-offs, acceptance signals |
| [Engineering onboarding](cjlu_architecture_onboarding.md) | New engineers | Day-1 paths, repos, pitfalls, starter tasks |

## Suggested reading order

```text
New engineer     → Onboarding → Overview → ADR (as needed)
Product / exec   → Stakeholder summary → Overview (optional depth)
Architecture review → ADR → Overview
```

## System at a glance

```text
Android (Room)  ←—— REST ——→  Ktor backend (Exposed + H2)
  cache + queue                 system of record
```

**Principle:** Backend owns canonical data; Android holds workflow queue and academic cache for offline UX.

## Repo pointers

| Area | Path |
|------|------|
| Android data layer | `app/src/main/java/com/cjlu/studentapp/data/` |
| Backend routes & DB | `backend-ktor/` (`Application.kt`, `Database.kt`, `AcademicRepository.kt`) |

---

*Template: all architecture docs share the same header (audience · purpose · quick links) and a Related documentation footer.*
