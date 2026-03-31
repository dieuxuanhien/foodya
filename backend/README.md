# Foodya Backend

Spring Boot backend scaffold for Foodya with integration-ready configuration for:

- Google AI Studio
- Supabase PostgreSQL and Storage S3 credentials
- Goong Maps
- Firebase config
- OpenWeather

## Prerequisites

- Java 21
- Maven 3.9+

## Configuration

1. Copy `.env.example` to `.env` (already created in this workspace).
2. Keep secrets in local `.env` or `config/api-keys.json`.
3. Do not commit secrets. `.env` and local secret files are git-ignored.

## Run

```bash
mvn spring-boot:run
```

## Verify

- Swagger UI: `http://localhost:8080/swagger-ui.html`
- Integration key status: `GET /api/v1/system/integrations/status`
- Firebase config probe: `GET /api/v1/system/integrations/firebase-config`
- Supabase config probe: `GET /api/v1/system/integrations/supabase-config`

## Test

```bash
mvn test
```
