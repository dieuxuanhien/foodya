# Clean Architecture Refactor Blueprint (Foodya Backend)

Date: 2026-03-30
Scope: `backend/`

## Research Baselines

Reference repositories reviewed:
- `mattia-battiston/clean-architecture-example`
- `maarten-vandeperre/clean-architecture-software-sample-project`
- `arhohuttunen/hexagonal-architecture-spring-boot`

Common proven practices from these repositories:
- Use cases are independent from web and database frameworks.
- Controllers are entrypoints that map HTTP requests/responses to application models.
- Data providers implement ports/interfaces defined by the use case layer.
- Architecture is enforced by module/package dependency rules and architecture tests.
- Testing pyramid includes fast use-case tests plus selected integration tests.

## Current Foodya Status After Phase 1

Completed in this refactor:
- `application` no longer depends on `interfaces/rest` DTO or exception packages.
- Shared business exceptions moved to `application.exception`.
- Service command/result contracts moved to `application.dto`.
- REST controllers updated to consume `application.dto` contracts.
- Global exception handler mapped to `application.exception`.

Still remaining gaps to full Uncle Bob compliance:
- Application services still depend directly on `infrastructure.repository` and `infrastructure.config`.
- Domain entities still use JPA annotations.
- Security filters are still under `interfaces/rest/support` package.
- No architecture-boundary tests currently enforce dependency rules.

## Phase 2 Plan (Ports + Adapters)

### 1) Introduce output ports in application
Create interfaces under `application.port.out`, for example:
- `UserAccountPort`
- `RefreshTokenPort`
- `PasswordResetChallengePort`
- `RestaurantPort`
- `MenuItemPort`
- `MenuCategoryPort`
- `SystemParameterPort`
- `AuditLogPort`

Use case classes depend only on these ports.

### 2) Implement adapters in infrastructure
Create adapter classes under `infrastructure.adapter.persistence` that delegate to Spring Data repositories and implement application ports.

### 3) Isolate runtime config behind ports
Wrap `SecurityProperties`, `ApiSecretsProvider`, and similar dependencies behind application-facing config ports.

### 4) Introduce domain-core models
For hot-path use cases, split domain model into:
- pure domain objects (`domain/core`) without JPA annotations
- persistence entities (`infrastructure/persistence/entity`) with mappers

Start with auth and catalog first, then expand.

### 5) Move security components out of interfaces
Move authentication/authorization filters and handlers to `infrastructure/security`, keeping `interfaces/rest` focused on HTTP entrypoints only.

### 6) Add architecture tests
Add package dependency tests (ArchUnit or equivalent) to enforce:
- `application` must not import `interfaces` or `infrastructure`
- `domain` must not import Spring or JPA
- `interfaces` depends inward only

## Suggested Package Direction

```text
com.foodya.backend
  domain
    core
    service
  application
    usecase
    dto
    exception
    port
      in
      out
  infrastructure
    adapter
      persistence
      integration
    security
    config
  interfaces
    rest
      controller
      dto
      mapper
```

## Verification Gates

- Build/test green for each migrated vertical slice.
- Architecture tests fail on boundary violations.
- API contracts unchanged unless SRS requires change.
- OpenAPI updated in same PR for endpoint contract changes.
