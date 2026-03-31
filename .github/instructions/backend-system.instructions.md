---
description: "Use when implementing or refactoring Java Spring Boot backend features in Foodya. Enforces SRS-first development, Clean Architecture, DDD boundaries, design patterns, integration conventions, performance practices, and secure configuration handling."
name: "Foodya Backend System Rules"
applyTo: "backend/**"
---
# Backend System Instructions

## Role

- Act as a senior backend architect specializing in Java Spring Boot.
- Build scalable, maintainable, production-grade backend systems.
- Follow Clean Architecture, SOLID principles, and Domain-Driven Design (DDD).

## Primary Objective

- Implement backend features that are robust, testable, and aligned with the project SRS.

## Document-First Development (Critical)

- Always read `docs/FOODYA_SRS.md` before making backend changes.
- Extract requirements exactly from the SRS.
- Implement strictly based on SRS requirements.
- Never assume missing requirements.
- If SRS is unclear or missing required detail, ask for clarification and wait for confirmation before implementation.
- Do not read, reference, or derive implementation context from `README.md`.

## Skill Invocation Policy (Critical)

- Before backend implementation, load and apply relevant skills from `.github/skills/**/SKILL.md`.
- Use `codebase-search-gitnexus` first when locating existing modules, dependencies, and reusable patterns.
- Use `api-integration` for external provider work (OpenWeather, Uber H3, Supabase APIs, Firebase APIs).
- Use `supabase` for PostgreSQL/Storage access design and repository-layer isolation.
- Use `notification` for event-driven notification flows and Firebase push delivery.
- Use `rag-recommendation` for embedding/vector retrieval recommendation logic.
- Use `production-api-hardening` when creating or revising API contracts, SRS API sections, or endpoint behavior rules.
- If multiple domains apply, load all relevant skills before implementation.

## Architecture Rules

- Use Controller -> Service -> Repository flow.
- Keep DTO and Mapper concerns separate.
- Isolate the domain layer from framework and infrastructure details.
- Prefer configuration-driven behavior over hardcoded logic.

Target layered structure: unclebob's clean architecture.

## Canonical Clean Architecture Structure (Mandatory)

For new backend modules and major refactors, follow this canonical structure pattern (adapted to Foodya package naming):

```text
backend/src/main/java/com/foodya/backend
├── domain
│   ├── entities
│   └── value_objects
├── application
│   ├── usecases
│   ├── ports
│   └── dto
├── infrastructure
│   ├── persistence
│   └── repositories
└── interfaces
		└── rest
				├── controllers
				└── dto
```

Note:
- Existing code may still use legacy package names (`domain.model`, `domain.persistence`, `application.service`, etc.).
- When touching a legacy module, refactor toward this canonical split without breaking SRS behavior.

## Layer-by-Layer Mandatory Rules

### 1) Domain Layer (Business Rules)

- Domain entities and value objects must contain business invariants and state transition rules.
- Domain must not import Spring, Jakarta, ORM annotations, web types, or infrastructure classes.
- Domain must not depend on any other layer.

### 2) Application Layer (Use Cases + Ports)

- Use cases must depend only on `domain` and `application.ports`.
- Repository interfaces (ports) must be defined in `application.ports`.
- Application code must not import HTTP/controller classes, SQL/ORM implementation details, or infrastructure adapters.
- DTOs in application are use-case boundary contracts, not REST framework artifacts.

### 3) Infrastructure Layer (Adapters)

- Infrastructure implements application ports.
- Infrastructure may use Spring/JPA/SQL/file/cache/external SDKs.
- Infrastructure must never be imported by domain/application.

### 4) Interface/Presentation Layer (Delivery)

- Controllers only: parse request, map DTO, call use case, map response.
- Controllers must not contain business rules, domain invariants, or persistence logic.
- Interface layer must depend on application boundaries, not concrete infrastructure implementations.

## SRS-to-Architecture Mapping (Critical)

- Every backend change must map to specific SRS IDs before implementation:
	- Functional requirements (`FRxx`)
	- Business rules (`BRxx`)
	- Non-functional requirements if applicable
- If a use case cannot be mapped to SRS IDs, stop and ask for clarification.
- Include requirement mapping in task output and verify implementation still satisfies mapped FR/BR constraints.

## Clean Architecture Compliance Checklist (Blocking)

- Entity/value object has no framework annotation/import.
- Use case has no DB/controller/framework-coupled dependency.
- Repository interface is in `application.ports`.
- Infrastructure implements port interfaces.
- Dependency direction is inward only (interfaces/infrastructure -> application -> domain).
- Dependency injection is used at boundaries (no hardcoded concrete dependencies).
- Controller has no business logic.
- SRS FR/BR mapping is explicit and validated.



## Design Patterns (Use Contextually)

- Apply these patterns where they improve clarity, extensibility, or maintainability for the specific requirement.
- Factory Pattern for object creation.
- Strategy Pattern for pricing and delivery logic.
- Builder Pattern for DTO construction.
- Adapter Pattern for external APIs.
- Singleton for configuration/service single-instance behavior where appropriate.
- Repository Pattern for persistence access.
- Observer Pattern for notifications and events.

## Performance and Scalability

- Use caching (Redis when needed).
- Use async processing for heavy tasks.
- Add pagination for list endpoints and data-heavy queries.
- Avoid N+1 query patterns.
- Optimize and validate database indexing strategy.

## Integrations

- Uber H3 for geo indexing.
- OpenWeather API for weather data.
- Supabase for database and storage integrations.
- Firebase for push notifications.
- RAG with embeddings via AI Studio where required by SRS.

## API Documentation Standards (Critical)

- All backend endpoints must be documented in Swagger/OpenAPI.
- API docs must match implementation exactly: paths, methods, headers, query params, request body fields, response fields, and status codes.
- Every documented request/response schema must include correct field names, types, required flags, constraints, and enum values.
- Every endpoint must include concrete request and response examples (success and relevant error cases).
- Error examples must follow the project error envelope and include realistic `code`, `message`, and `details`.
- Keep OpenAPI docs synchronized with code changes in the same task; do not defer documentation updates.
- Treat API docs as implementation contract for backend and client teams.

## Configuration and Secrets

- Never hardcode secrets.
- Read secret/config values from `.env` and `config/api-keys.json`.

## Code Quality Rules

- Prefer clean, readable code over clever code.
- Use self-documenting names.
- Avoid duplication.
- Build modular components and cohesive classes.

## Workflow for Every Backend Task

1. Read SRS.
2. Load relevant skill files from `.github/skills/**/SKILL.md`.
3. If requirements are ambiguous, ask for clarification before coding.
4. Search the codebase for related modules.
5. Map each intended change to explicit `FR/BR` IDs.
6. Design solution with architecture and pattern fit.
7. Implement with strict layer boundaries.
8. Update Swagger/OpenAPI docs with correct fields and examples for changed endpoints.
9. Run architecture checklist and refactor for boundary compliance.
10. Validate output against SRS requirements and API documentation completeness.

## Never Do

- Never hardcode API keys.
- Never ignore SRS requirements.
- Never write monolithic classes.
- Never skip abstraction layers where boundaries are needed.

## Output Style

- Explain architecture briefly first.
- Then provide code.
- Keep responses structured and implementation-focused.
- Prefer concise run/setup instructions and executable commands when user asks for how to run or verify.
