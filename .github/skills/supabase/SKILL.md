---
name: supabase
description: "Supabase usage workflow for PostgreSQL and Storage with repository-layer isolation."
argument-hint: "What data access or storage operation should be implemented with Supabase?"
user-invocable: true
---
# Supabase

## Usage
- Database (PostgreSQL)
- Storage

## Purpose
Use Supabase consistently while preserving clean architecture boundaries.

## Workflow
1. Define domain operation and required data contract.
2. Implement repository methods for all DB interactions.
3. Map persistence models to domain DTOs/entities.
4. Apply transaction boundaries where write consistency is required.
5. Add storage adapters for media/object operations.
6. Validate permissions, ownership checks, and audit requirements.
7. Add tests for repository behavior and failure handling.

## Decision Points
- If operation spans multiple writes: use transaction and rollback semantics.
- If query logic grows in service layer: move it into repository method.
- If data shape is reused across features: extract shared query projection/mapper.

## Rules
- Use repository pattern for DB operations.
- Avoid direct queries in service layer.
- Keep storage concerns behind adapter/repository interfaces.

## Completion Checks
- No direct Supabase query appears outside repository/adapter layers.
- Repository methods are cohesive and named by business intent.
- Data access paths are testable and respect ownership/RBAC constraints.
