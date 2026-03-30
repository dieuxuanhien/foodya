---
name: notification
description: "Event-driven notification workflow using Firebase push delivery and observer-style event handling."
argument-hint: "What event should trigger notifications and who should receive them?"
user-invocable: true
---
# Notification

## System
- Firebase Push Notifications

## Pattern
- Observer Pattern

## Purpose
Deliver reliable, traceable notifications from business events.

## Flow
1. Domain event is triggered.
2. Observer/subscriber resolves recipients and templates.
3. Notification record is persisted.
4. Push payload is sent via Firebase.
5. Delivery status is updated and retry policy applied if needed.

## Decision Points
- If recipient has no active token: persist notification and skip push send.
- If send fails transiently: retry with bounded exponential backoff.
- If retries exhausted: mark failed and route to dead-letter handling.

## Rules
- Event emission and delivery logic must be decoupled.
- Notification content must be template-driven and localized where supported.
- Delivery failures must be logged with traceId and provider response metadata.

## Completion Checks
- Trigger event and subscriber path are documented.
- Notification persistence occurs before push attempt.
- Delivery outcome is observable (`PENDING`, `SENT`, `FAILED`).
- Retry and terminal-failure behavior are deterministic.
