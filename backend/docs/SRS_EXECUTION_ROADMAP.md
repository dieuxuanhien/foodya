# Backend SRS Execution Roadmap

Version: 1.0
Date: 2026-03-30
Scope: `backend/` only
Reference: `docs/FOODYA_SRS.md`

## Pre-computation

### 1. Problem Understanding
Implement all SRS backend capabilities with deterministic API contracts, typed validation, role and ownership enforcement, and production-safe persistence/integration behavior.

### 2. Stakeholder Perspective
- Customer: secure auth, accurate discovery and ordering, reliable tracking.
- Merchant: ownership-safe catalog and order operations.
- Delivery: assignment/status/location API workflow.
- Admin: governance, reporting, and runtime system configuration.
- Engineering/QA/Ops: stable contracts, observability, backups, retention, and backward compatibility.

### 3. System Boundary Definition (In-Scope vs Out-of-Scope)
- In scope: FR01-FR31, BR01-BR50, NFR01-NFR35 in `docs/FOODYA_SRS.md`.
- Out of scope: features not defined by SRS for v1.

### 4. Constraints and Assumptions
- Clean Architecture layering remains mandatory.
- No hardcoded secrets.
- OpenAPI and implementation stay synchronized per endpoint.
- Delivery uses modular monolith sequencing by phases due to current baseline maturity.

### 5. High-Level Architecture Direction
- Sequence by vertical slices while preserving horizontal standards:
  - Security and token policy baseline first.
  - Catalog and order invariants second.
  - Delivery/notification/AI and governance/reporting after transactional core.

## Progress Dashboard

- Completed:
  - Phase 0 baseline:
    - Error envelope `{ code, message, details, traceId }`.
    - Success envelope with `traceId`.
    - `TraceId` propagation filter.
    - `/health/live` and `/health/ready`.
    - Flyway migrations and initial schema for `system_parameters`, `audit_logs`.
  - FR31 initial slice:
    - `GET/PUT/PATCH /api/v1/admin/system-parameters`.
    - Typed key catalog defaults and validation.
    - Admin update audit logs and versioning behavior.
  - Phase 2 (FR07, FR08, FR09, FR13, FR14, FR15, FR30) completed:
    - Public catalog endpoints: `GET /api/v1/restaurants`, `GET /api/v1/restaurants/{id}`, `GET /api/v1/restaurants/{id}/menu-items`, `GET /api/v1/restaurants/nearby`.
    - Merchant catalog endpoints: `POST/PATCH /api/v1/merchant/restaurants*`, category CRUD, menu item CRUD-soft-delete, availability toggle.
    - Nearby discovery: Uber H3 k-ring prefilter + Haversine distance sort with kilometer rounding behavior.
    - Pagination policy enforcement from system parameters with `422` on invalid values.
    - Integration tests for grouped search, nearby sorting, menu search/filter, and merchant ownership lifecycle.

- In progress:
  - Phase 3 (FR10, FR11, FR27, FR29): cart, checkout, and orders core.

## Phase-by-Phase Plan

### Phase 1: Identity, Session, Profile
Coverage:
- FR: FR01, FR02, FR03, FR04, FR05, FR06, FR26
- BR: BR01-BR09, BR20, BR27
- NFR: NFR01-NFR05, NFR21, NFR22

Endpoints:
- `POST /api/v1/auth/register`
- `POST /api/v1/auth/login`
- `POST /api/v1/auth/refresh`
- `POST /api/v1/auth/forgot-password`
- `POST /api/v1/auth/forgot-password/verify-otp`
- `POST /api/v1/auth/reset-password`
- `POST /api/v1/auth/logout`
- `POST /api/v1/auth/logout-all`
- `GET /api/v1/me`
- `PATCH /api/v1/me`
- `PUT /api/v1/me/password`

Gate criteria:
- JWT short-lived access token + refresh rotation + reuse detection.
- Password policy and uniqueness constraints enforced.
- Profile ownership checks enforced.
- Security-critical actions written to audit logs.

### Phase 2: Catalog and Nearby Search
Coverage:
- FR: FR07, FR08, FR09, FR13, FR14, FR15, FR30
- BR: BR19, BR21-BR23, BR35, BR37, BR38, BR45, BR46

### Phase 3: Cart, Checkout, Orders Core
Coverage:
- FR: FR10, FR11, FR27, FR29
- BR: BR10-BR16, BR18, BR28, BR29, BR32-BR34, BR39, BR40, BR47, BR49

### Phase 4: Merchant Processing + Delivery Tracking
Coverage:
- FR: FR16, FR21, FR28
- BR: BR18, BR25, BR30, BR31

### Phase 5: Reviews, Notifications, AI
Coverage:
- FR: FR12, FR22, FR23
- BR: BR13, BR17, BR26, BR36

### Phase 6: Admin Governance + Reporting
Coverage:
- FR: FR17, FR18, FR19, FR20, FR24, FR25
- BR: BR20, BR24, BR41-BR44, BR48

### Phase 7: Ops Hardening
Coverage:
- NFR13-NFR20, NFR25-NFR35 and remaining BR constraints

## Immediate Execution Queue

1. Implement FR27 active cart model with single-restaurant constraint (BR28, BR29).
2. Implement FR10 checkout and order creation with idempotency (BR10-BR14, BR34).
3. Integrate Goong route distance and shipping fee formula policy (BR39, BR40, BR44).
4. Implement FR11 customer order listing/detail/cancel with BR15/BR16/BR18 enforcement.
5. Add payment baseline fields and deterministic payment transitions (FR29, BR32, BR49).
