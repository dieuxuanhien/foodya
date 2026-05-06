---
name: "Mobile Feature Plan From SRS"
description: "Use when planning a Foodya mobile feature from SRS FR/BR IDs before coding. Produces a consistent implementation plan for Flutter Customer/Merchant work."
argument-hint: "Provide role, FR IDs, BR IDs, feature scope, and constraints (example: role=CUSTOMER; FR07,FR09,FR27; BR12,BR29; add restaurant list + cart)."
agent: "Foodya Flutter Mobile Developer"
tools: [read, search, todo]
---

Generate a plan-only implementation blueprint for a Foodya mobile feature from SRS requirements.

Do not write or edit code in this prompt. Do not run format/test/build commands. This prompt is for planning before coding.

Inputs to use:

- User-provided role and feature scope
- FR/BR IDs provided in arguments
- [SRS](../../docs/FOODYA_SRS.md)
- [Mobile conventions](../instructions/mobile-flutter.instructions.md)
- Existing mobile structure under mobile/lib

Planning constraints:

- One Flutter app with role-based flows (Customer/Merchant only unless explicitly requested)
- State management with Bloc/Cubit
- Routing with go_router and role guards
- Material 3 UI patterns
- Preserve existing auth/session foundation (FR01/FR02/FR03/FR26) unless scope says otherwise

Return exactly this output format:

## 1) Requirement Mapping

- FR IDs in scope and what each means for mobile behavior
- BR IDs in scope and how each constrains UI/state/data
- Out-of-scope requirements (explicitly listed)

## 2) Feature Slice and UX Flow

- User journey from entry to completion
- Screen list (new vs existing)
- Navigation transitions and route-guard implications

## 3) Data and API Plan

- Endpoints to call (method + path)
- Request/response model plan (DTO names to create/update)
- Error model mapping (API errors -> user-facing states)

## 4) State Management Plan

- Cubit/Bloc units to create/update
- State variants (idle/loading/success/empty/failure)
- Events/actions and side effects

## 5) File-Level Implementation Plan

- Ordered file list with action tags:
  - CREATE: new files
  - UPDATE: existing files
- For each file: concise purpose and key changes

## 6) Validation and Test Plan

- Analyze/test commands to run after coding
- Widget tests to add/update
- Bloc tests to add/update
- Manual verification checklist

## 7) Risks and Open Questions

- Top risks (contract ambiguity, auth/routing coupling, edge cases)
- Assumptions made
- Questions to resolve before coding

Style requirements:

- Keep the plan concise but actionable
- Prefer checklists and short bullets
- Include explicit dependencies between tasks
- End with a recommended implementation order (Phase 1, 2, 3)
