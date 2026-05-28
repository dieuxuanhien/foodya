## Project Guidelines

### Workspace Scope

- Backend implementation is in `backend/`.
- Mobile implementation is now active in `mobile/` (Flutter app for Customer/Merchant).
- `web/` is still an empty placeholder.
- Use backend + mobile coordinated changes when a feature spans auth/API/mobile UX.

### Architecture

- Backend follows clean architecture package boundaries:
  - `domain` for entities/value objects and business invariants
  - `application` for use cases, DTOs, and ports
  - `infrastructure` for adapters, repositories, and framework integration
  - `interfaces/rest` for controllers and API DTOs
- Application services are explicitly wired in `backend/src/main/java/com/foodya/backend/infrastructure/config/AppConfig.java`.
- REST controllers should validate API DTOs and map to application DTOs before invoking use cases.
- Mobile uses one Flutter app with feature-first structure under `mobile/lib/features/**`, shared modules in `mobile/lib/core/**`, Bloc/Cubit state management, and go_router route guards.

### Build and Test

- Run backend commands from `backend/`.
- Main dev loop:
  - `mvn spring-boot:run`
  - `mvn test`
- Containerized run:
  - `docker-compose up`
- For migration reset/reseed, use the command documented in `backend/README.md`.
- Run mobile commands from `mobile/`:
  - `flutter pub get`
  - `flutter run`
  - `flutter analyze`
  - `flutter test`

### Conventions

- Respect architecture rules in `backend/src/test/java/com/foodya/backend/architecture/ArchitectureRulesTests.java`.
- Keep `application` layer framework-free (no direct infrastructure or REST dependencies).
- Prefer adding new integrations behind outbound ports in `application/ports/out` and implement adapters in `infrastructure/adapter`.
- Keep role-scoped API routes under explicit prefixes (`/api/v1/admin/**`, `/api/v1/merchant/**`, `/api/v1/customer/**`, `/api/v1/delivery/**`).
- Apply schema changes via Flyway migrations:
  - SQL: `backend/src/main/resources/db/migration`
  - Java migrations: `backend/src/main/java/db/migration`
- For Dart/Flutter code conventions, defer to `.github/instructions/mobile-flutter.instructions.md` (do not duplicate rules here).

### Setup Pitfalls

- Copy `backend/.env.example` to `.env` and provide required secrets before running locally.
- `FOODYA_JWT_SECRET` must be set and strong (32+ chars).
- Default runtime DB is PostgreSQL; tests use H2 in-memory (`backend/src/test/resources/application.yml`).
- If Supabase direct DB host fails on IPv4-only networks, use Supabase connection pooler host/port from dashboard (port `6543`, `sslmode=require`).
- In PowerShell, prefer `mvn --% ...` when passing multiple `-D...` arguments to avoid shell parsing issues.
- Mobile auth depends on backend `/api/v1/auth/**`; backend must be reachable before running login/register flows.
- API base URL defaults are platform-specific in `mobile/lib/core/config/app_config.dart` (`10.0.2.2` for Android emulator, `localhost` for iOS/web/desktop).
- Override mobile API base URL with `--dart-define=FOODYA_API_BASE_URL=http://<host>:8080` when testing on real devices.
- Keep secrets out of git; use env vars or local secret file configuration as documented in `backend/README.md`.

### References

- Project requirements/spec: `docs/FOODYA_SRS.md`
- Backend setup and commands: `backend/README.md`
- Mobile setup and commands: `mobile/README.md`
- Seeded test accounts: `backend/docs/SEED_ACCOUNTS.md`
- Notification implementation details: `backend/docs/NOTIFICATION_FEATURE_COMPLETION.md`

<!-- gitnexus:start -->
# GitNexus — Code Intelligence

This project is indexed by GitNexus as **foodya** (8689 symbols, 26169 relationships, 300 execution flows). Use the GitNexus MCP tools to understand code, assess impact, and navigate safely.

> If any GitNexus tool warns the index is stale, run `npx gitnexus analyze` in terminal first.

## Always Do

- **MUST run impact analysis before editing any symbol.** Before modifying a function, class, or method, run `gitnexus_impact({target: "symbolName", direction: "upstream"})` and report the blast radius (direct callers, affected processes, risk level) to the user.
- **MUST run `gitnexus_detect_changes()` before committing** to verify your changes only affect expected symbols and execution flows.
- **MUST warn the user** if impact analysis returns HIGH or CRITICAL risk before proceeding with edits.
- When exploring unfamiliar code, use `gitnexus_query({query: "concept"})` to find execution flows instead of grepping. It returns process-grouped results ranked by relevance.
- When you need full context on a specific symbol — callers, callees, which execution flows it participates in — use `gitnexus_context({name: "symbolName"})`.

## Never Do

- NEVER edit a function, class, or method without first running `gitnexus_impact` on it.
- NEVER ignore HIGH or CRITICAL risk warnings from impact analysis.
- NEVER rename symbols with find-and-replace — use `gitnexus_rename` which understands the call graph.
- NEVER commit changes without running `gitnexus_detect_changes()` to check affected scope.

## Resources

| Resource | Use for |
|----------|---------|
| `gitnexus://repo/foodya/context` | Codebase overview, check index freshness |
| `gitnexus://repo/foodya/clusters` | All functional areas |
| `gitnexus://repo/foodya/processes` | All execution flows |
| `gitnexus://repo/foodya/process/{name}` | Step-by-step execution trace |

## CLI

| Task | Read this skill file |
|------|---------------------|
| Understand architecture / "How does X work?" | `.claude/skills/gitnexus/gitnexus-exploring/SKILL.md` |
| Blast radius / "What breaks if I change X?" | `.claude/skills/gitnexus/gitnexus-impact-analysis/SKILL.md` |
| Trace bugs / "Why is X failing?" | `.claude/skills/gitnexus/gitnexus-debugging/SKILL.md` |
| Rename / extract / split / refactor | `.claude/skills/gitnexus/gitnexus-refactoring/SKILL.md` |
| Tools, resources, schema reference | `.claude/skills/gitnexus/gitnexus-guide/SKILL.md` |
| Index, status, clean, wiki CLI commands | `.claude/skills/gitnexus/gitnexus-cli/SKILL.md` |

<!-- gitnexus:end -->
