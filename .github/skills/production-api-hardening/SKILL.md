---
name: production-api-hardening
description: "Use when auditing or upgrading SRS/API specs to production-grade readiness: endpoint coverage, normalized versioned paths, deterministic contracts, error model, and compatibility gates."
argument-hint: "Which SRS or API section should be hardened to production grade?"
user-invocable: true
---
# Production API Hardening

## Purpose
Turn an API section in SRS/PRD/LLD into an implementation-ready, production-grade contract with no critical ambiguity.

## When To Use
- API table exists but may be incomplete or inconsistent with requirements.
- Requirements mention capabilities (CRUD/governance/notifications) not fully mapped to endpoints.
- Contracts need deterministic behavior (status transitions, idempotency, pagination, rounding, retention).
- Team is preparing for implementation handoff.

## Workflow
1. Build coverage baseline.
2. Reconcile requirements to APIs.
3. Normalize endpoint contracts.
4. Harden behavior semantics.
5. Harden reliability and error model.
6. Add governance and compatibility controls.
7. Run completion gates and publish unresolved risks.

## Detailed Procedure

### 1) Build Coverage Baseline
- Extract FR, BR, UC, List/View entries that imply APIs.
- Extract all declared endpoints from API table and behavior notes.
- Build FR/UC/List -> API mapping and mark gaps: missing, partial, ambiguous.

### 2) Reconcile Requirements To APIs
- Add missing endpoints for all required capabilities.
- Ensure role/ownership rules align with endpoint scope.
- Ensure list/view endpoints are present in API contracts (not only UI tables).

### 3) Normalize Endpoint Contracts
- Enforce versioning under `/api/v1`.
- Enforce naming consistency (resource-oriented paths, actor domains).
- Ensure method semantics are consistent (`GET` read, `POST` create/action, `PATCH` partial update, `DELETE` idempotent delete).

### 4) Harden Behavior Semantics
- Define deterministic rules for:
  - status transitions and terminal states
  - idempotency headers/keys and duplicate behavior
  - pagination defaults/max bounds and invalid input behavior
  - sorting/filtering allowed values
  - monetary rounding/currency policy
  - geospatial distance algorithm and units

### 5) Harden Reliability And Error Model
- Define standard success and error envelopes.
- Define endpoint-level validation behavior and HTTP statuses.
- Define retry/backoff scope, DLQ behavior, and rate limiting policy.
- Require traceability fields (`traceId`, timestamp standard).

### 6) Add Governance And Compatibility Controls
- Add OpenAPI source-of-truth requirement.
- Add backward-compatibility rule for current version.
- Add destructive-action policies and conflict semantics.

### 7) Completion Gates
- Every FR/UC/List that implies API has at least one endpoint.
- No unversioned endpoints remain.
- No contradictory behavior between BR/NFR and API contracts.
- All critical operations have deterministic failure behavior.
- Remaining gaps are explicitly listed as non-blocking or blocking.

## Decision Points
- If a capability is present in FR/UC/List but absent in API: add endpoint row and behavior notes.
- If behavior is implied but not explicit (for example, "policy" wording): convert to deterministic rule.
- If delete is governance-sensitive: define soft-delete/hard-delete preconditions and conflict codes.
- If compatibility impact is breaking: define new version path instead of modifying behavior silently.

## Rules
- API contracts must be implementable without reverse-engineering intent.
- Prefer explicit constraints and enums over narrative phrasing.
- Keep endpoint table and behavior sections synchronized.
- Do not leave blocking TBDs in production-ready SRS.

## Output Contract
Produce three sections in order:
1. Findings (ordered by severity, with file/line references).
2. Applied contract changes (endpoint and behavior deltas).
3. Residual risks and go/no-go readiness statement.
