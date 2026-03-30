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

Target layered structure:

```text
backend/
  domain/
  application/
  infrastructure/
  interfaces/
```

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
5. Design solution with architecture and pattern fit.
6. Implement.
7. Refactor for clarity and boundaries.
8. Validate output against SRS requirements.

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
