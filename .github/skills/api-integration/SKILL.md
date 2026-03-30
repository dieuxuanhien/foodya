---
name: api-integration
description: "Production-safe integration workflow for external APIs (OpenWeather, Uber H3, Supabase, Firebase)."
argument-hint: "Which external API and business flow are you integrating?"
user-invocable: true
---
# API Integration

## Supported APIs
- OpenWeather API
- Uber H3
- Supabase
- Firebase

## Purpose
Implement reliable external integrations with clear adapters, retries, and observability.

## Workflow
1. Define integration contract: input, output, latency target, failure behavior.
2. Create adapter boundary for provider-specific logic.
3. Validate request/response schema and map to internal DTOs.
4. Add resilience: timeout, retry policy, fallback behavior.
5. Add observability: structured logs, metrics, trace identifiers.
6. Add governance: quota/rate-limit handling and error classification.
7. Verify with integration tests and failure-path tests.

## Decision Points
- If provider response is optional for core flow: degrade gracefully with fallback data.
- If provider response is critical: fail fast with explicit error code and retry guidance.
- If quota threshold is near limit: apply backoff and cache-first strategy.

## Rules
- Use Adapter Pattern for each external provider.
- Handle errors gracefully with typed error categories.
- Use retry mechanisms only for transient failures.
- Log failures with provider, operation, latency, and traceId.

## Completion Checks
- Provider call is isolated in adapter layer.
- Retry policy is explicit and bounded.
- Failure paths return deterministic API errors.
- Integration emits logs/metrics needed for production support.
