---
name: "Foodya Mobile Flutter Conventions"
description: "Use when creating or modifying Foodya Flutter mobile code for Customer and Merchant features, including Bloc state management, go_router navigation, and Material 3 UI patterns."
applyTo: "mobile/**/*.dart"
---

# Foodya Mobile Flutter Conventions

## Product Scope

- Build one Flutter app with a role-based login switch.
- Supported roles by default: CUSTOMER and MERCHANT.
- Treat docs/FOODYA_SRS.md as source of truth and map features to FR/BR IDs.
- Do not implement DELIVERY or ADMIN mobile flows unless explicitly requested.

## Architecture

- Organize code by feature first under mobile/lib/features/<feature_name>/.
- Keep each feature separated into presentation, domain, and data layers.
- Keep shared cross-cutting code in mobile/lib/core/ (routing, theme, networking, errors, auth/session).

## State Management

- Use flutter_bloc with Bloc/Cubit for feature and screen states.
- Keep state immutable and explicit with loading, success, empty, and failure variants.
- Do not place business rules inside widgets; widgets dispatch events and render state.

## Navigation

- Use go_router for all app navigation.
- Centralize route definitions and role guards in a core routing module.
- Redirect unauthorized role access to a safe fallback route.

## Auth Lifecycle

- Keep FR01/FR02/FR03/FR26 behavior intact unless explicitly changing auth requirements:
  - Register: `POST /api/v1/auth/register`
  - Login: `POST /api/v1/auth/login`
  - Refresh: `POST /api/v1/auth/refresh`
  - Logout all: `POST /api/v1/auth/logout-all`
- Persist token pairs with secure storage and keep token/session logic in core auth modules.
- Prefer deriving role from JWT role claim for post-auth route gating.

## UI and Theming

- Use Material 3 components and centralized ThemeData.
- Prefer reusable design tokens/components for spacing, colors, and typography.
- Build responsive phone-first layouts and avoid brittle fixed-size assumptions.

## API Integration

- Use typed request/response models and repository abstractions.
- Consume role-scoped backend APIs:
  - /api/v1/customer/\*\*
  - /api/v1/merchant/\*\*
- Keep auth token/session handling centralized in core auth services.
- Use `mobile/lib/core/config/app_config.dart` as the single source for API base URL selection and `FOODYA_API_BASE_URL` override.

## Quality Gates

- Run flutter analyze and flutter test for meaningful changes.
- Add or update widget tests for new screen states.
- Add or update bloc tests for non-trivial state logic.

## References

- docs/FOODYA_SRS.md
- backend/README.md
