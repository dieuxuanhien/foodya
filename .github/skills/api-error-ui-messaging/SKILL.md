---
name: api-error-ui-messaging
description: "Use when handling backend API errors in Flutter and converting technical error codes/messages/details into user-friendly UI feedback, field-level validation hints, and consistent retry/recovery actions."
argument-hint: "Which endpoint or error payload should be translated? Include code, message, status, and sample response body."
user-invocable: true
---

# API Error To UI Messaging

## Purpose

Convert backend API error responses into readable, actionable, and role-safe UI feedback for Foodya mobile users.

## When To Use

- A screen currently shows raw API error text or status code.
- The UI needs consistent messages for register/login/profile/order/cart flows.
- You need field-level validation mapping from API details to form errors.
- You need retry guidance for network, auth, rate-limit, or server failures.

## Workflow

1. Capture the real backend error contract.
   - Collect HTTP status, response body, and request context.
   - Confirm the backend envelope shape: code, message, details.
   - Use real responses from logs/devtools; avoid guessing.
2. Classify error type.
   - Validation: 400/422 with field details.
   - Auth/session: 401 token expired/invalid, 403 forbidden.
   - Resource/business: 404/409 domain conflicts.
   - Capacity/system: 429 rate limit, 5xx server failures.
   - Connectivity: timeout, DNS, offline, CORS/network failures.
3. Define user-facing copy.
   - Replace technical wording with plain language.
   - Keep messages short and actionable.
   - Include next action: retry, re-login, edit field, contact support.
4. Map to UI state model.
   - Global error banner/snackbar for request-level failures.
   - Inline field errors for details map keys.
   - Empty state only when request succeeds but returns no data.
5. Implement translation in one central place.
   - Add an error mapper in core/network or feature data layer.
   - Convert raw API exception to typed UI-friendly failure object.
   - Ensure Cubit/Bloc emits stable failure states.
6. Handle auth lifecycle safely.
   - For 401, trigger refresh/logout flow via existing auth abstractions.
   - Prevent infinite retry loops.
   - Keep role-gated navigation intact after auth failures.
7. Validate end-to-end behavior.
   - Confirm each major code path renders expected copy.
   - Verify no raw server message leaks to production UI.
   - Run flutter analyze and flutter test.

## Decision Points

- If details includes form field keys: prefer inline field errors over generic snackbars.
- If code is unknown but status is known: fallback by status family (4xx vs 5xx).
- If both code and status are missing: show resilient generic network message.
- If backend message is safe and user-friendly: reuse it selectively; otherwise override.
- If repeated failures happen after retry: escalate to persistent error state with help text.

## Rules

- Do not parse errors in widget code; keep mapping in data/domain layers.
- Keep one source of truth for error-code to UI-message mapping.
- Preserve backend diagnostics for logs while showing simplified UI text.
- Never expose secrets, stack traces, SQL errors, or internal exception names.
- Keep messaging consistent across Customer and Merchant screens.

## Completion Checks

- Known backend codes are mapped to friendly copy.
- 422 validation details appear on the correct input fields.
- 401/403 paths produce correct auth and permission UX.
- 429 and 5xx show clear retry/backoff guidance.
- Unknown errors still show a safe fallback message.
- flutter analyze passes.
- Relevant flutter tests pass.

## Suggested Mapping Baseline

- VALIDATION_FAILED: "Please check the highlighted fields and try again."
- UNAUTHORIZED: "Your session has expired. Please log in again."
- FORBIDDEN: "You do not have permission to perform this action."
- RESOURCE_NOT_FOUND: "The requested item was not found."
- CONFLICT: "This action conflicts with current data. Please refresh and try again."
- RATE_LIMITED: "Too many requests. Please wait a moment and try again."
- INTERNAL_ERROR: "Something went wrong on our side. Please try again later."
- NETWORK_TIMEOUT: "The request timed out. Check your connection and try again."
- NETWORK_ERROR: "Cannot reach the server. Check your network and try again."

## Example Prompts

- Use api-error-ui-messaging for auth register 422 response with details map.
- Apply api-error-ui-messaging to map customer cart API errors into Cubit states.
- Build reusable error mapper for merchant endpoints using api-error-ui-messaging.
