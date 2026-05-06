---
name: mobile-backend-api-implementation
description: "Use when implementing Flutter mobile features from existing backend APIs, including contract mapping, DTO/repository integration, auth handling, and validation."
argument-hint: "Which role/feature or endpoint should be integrated from backend to mobile? Include FR/BR IDs when available."
user-invocable: true
---

# Mobile Backend API Implementation

## Purpose

Implement backend API capabilities in the Flutter mobile app with consistent architecture, role safety, and testable state flows.

## When To Use

- A Customer or Merchant mobile feature needs backend data/actions.
- You have FR/BR IDs from SRS and need implementation-ready mobile changes.
- You need to add or update API DTOs, repositories, cubits, and UI flows together.

## Workflow

1. Confirm scope from SRS and role constraints.
   - Map FR/BR IDs to concrete mobile behavior and state transitions.
   - Explicitly list out-of-scope requirements.
2. Discover backend API contracts.
   - Locate controller endpoint paths, request DTOs, success response shape (`data` envelope), and error response shape (`code`, `message`, `details`).
   - Verify auth requirements and role access constraints.
3. Plan mobile integration points before editing.
   - Identify domain models, data source methods, repository interfaces, and cubit/state updates.
   - Identify affected routes and guard logic.
4. Implement data layer.
   - Add/update typed request/response models.
   - Add/update remote data source methods with consistent error conversion.
   - Add/update repository methods that expose domain-friendly results.
5. Implement state and UI.
   - Add/update Cubit/Bloc actions and state variants (idle/loading/success/empty/failure).
   - Connect screens/forms to cubit actions and feedback handling.
   - Keep business rules out of widgets.
6. Validate behavior.
   - Run `flutter analyze` and `flutter test`.
   - Verify role-gated navigation and session behavior manually.
7. Report traceability.
   - Summarize FR/BR coverage, files changed, and any assumptions/open questions.

## Decision Points

- If API contract and SRS conflict: follow SRS intent, then flag backend mismatch explicitly before coding around it.
- If endpoint is auth-protected: wire bearer token and session refresh behavior through existing auth/session abstractions.
- If endpoint pagination/filtering is complex: add domain-level query models instead of passing raw maps through UI layers.
- If backend lacks required endpoint/field: stop and propose a backend change rather than inventing client-side workarounds.

## Rules

- Keep one-app role strategy (Customer/Merchant) unless explicitly told otherwise.
- Use existing mobile architecture under `mobile/lib/features/**` and `mobile/lib/core/**`.
- Use `mobile/lib/core/config/app_config.dart` for base URL and override behavior.
- Preserve and extend existing auth lifecycle (FR01/FR02/FR03/FR26) instead of replacing it.
- Keep API handling typed and repository-driven; avoid direct HTTP usage in UI/cubit layers.

## Completion Checks

- Backend endpoint contract mapped to mobile request/response models.
- Error handling produces user-meaningful states/messages.
- Route guards and role constraints still hold.
- `flutter analyze` passes.
- `flutter test` passes.
- FR/BR-to-file traceability is documented in final summary.

## References

- [SRS](../../docs/FOODYA_SRS.md)
- [Mobile Instructions](../../.github/instructions/mobile-flutter.instructions.md)
- [Backend README](../../backend/README.md)
- [Mobile README](../../mobile/README.md)
