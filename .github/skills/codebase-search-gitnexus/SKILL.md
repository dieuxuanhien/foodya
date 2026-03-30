---
name: codebase-search-gitnexus
description: "Repository search workflow for locating classes, dependencies, and implementation patterns before coding."
argument-hint: "What feature, module, or symbol should be located and analyzed?"
user-invocable: true
---
# Codebase Search (GitNexus)

## Purpose
Understand repository structure using indexed search and avoid duplicate implementations.

## When To Use
- You need to find where a feature already exists.
- You need dependency impact before making changes.
- You need to identify project patterns for consistency.

## Workflow
1. Define search scope: module, symbol, endpoint, or domain term.
2. Locate candidate files: classes, interfaces, DTOs, services, repositories.
3. Map dependencies: callers, callees, imports, and data flow.
4. Compare patterns: naming, layering, error handling, validation.
5. Select reuse-first approach: extend existing component if possible.
6. Produce output: file map, reuse decision, and safe edit points.

## Decision Points
- If a stable existing implementation exists: reuse and extend.
- If similar code exists but diverges from standards: refactor toward standard pattern.
- If no reusable component exists: create new code aligned to module conventions.

## Rules
- Reuse existing code before creating new components.
- Keep package boundaries and naming consistent with surrounding code.
- Do not introduce parallel abstractions for the same responsibility.

## Completion Checks
- At least one dependency path is documented from entry point to persistence/integration.
- Reuse vs new-build decision is explicit and justified.
- Chosen files match existing architectural boundaries.
