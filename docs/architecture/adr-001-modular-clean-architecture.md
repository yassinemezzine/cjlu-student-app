# ADR-001: Adopt a feature-oriented modular monolith

## Status

Accepted

## Context

The Android application previously stored UI, persistence, networking, repositories,
navigation and synchronization in one application module. Data code imported presentation
models, generated files were tracked, and changes to one feature required compiling the
entire Android source set.

## Decision

Use one Gradle module per user-facing feature and focused core modules for shared
infrastructure. Keep the Ktor backend in the same build and retain `:shared-contract` as the
single transport contract.

Feature modules contain presentation code. Domain models, Room persistence, networking and
repository implementations are separated into core modules with explicit dependency
direction. `:app` remains the composition root for lifecycle services and top-level
navigation.

## Trade-offs

- More Gradle configuration and explicit dependencies are required.
- Existing package names are retained during the first migration to reduce behavioral risk.
- Full ViewModel extraction is incremental because realtime and notification orchestration
  currently spans several features.

## Consequences

- Gradle now detects accidental cross-layer dependencies.
- Feature UI can evolve without owning network or database configuration.
- Room entities no longer annotate the shared domain request model.
- The application can migrate coordinators to feature ViewModels without another physical
  source move.
