# Seed Accounts For API/OpenAPI Usage

This document lists deterministic seeded accounts available from Flyway seeds and how to get JWT tokens quickly for Swagger/OpenAPI testing.

## Purpose

- Convenient role-based login for manual API testing.
- Fast token generation for Swagger "Authorize" flow.
- Traceability to seed migrations.

## Seed Source Migrations

- `backend/src/main/resources/db/migration/V10__seed_test_data_hcmc.sql`
- `backend/src/main/resources/db/migration/V12__seed_api_route_smoke_data.sql`

## Credentials

All accounts below use the same password:

- `Strong@123`

## V12 API Smoke Accounts (Recommended For OpenAPI)

| Role | Username | Email | User ID |
| --- | --- | --- | --- |
| ADMIN | `api_admin` | `api_admin@foodya.local` | `12121212-1212-1212-1212-121212121212` |
| MERCHANT | `api_merchant` | `api_merchant@foodya.local` | `13131313-1313-1313-1313-131313131313` |
| DELIVERY | `api_delivery` | `api_delivery@foodya.local` | `14141414-1414-1414-1414-141414141414` |
| CUSTOMER | `api_customer` | `api_customer@foodya.local` | `15151515-1515-1515-1515-151515151515` |

## V10 HCMC Demo Accounts

| Role | Username | Email | User ID |
| --- | --- | --- | --- |
| ADMIN | `admin_foodya` | `admin@foodya.local` | `11111111-1111-1111-1111-111111111111` |
| MERCHANT | `merchant_pho` | `merchant_pho@foodya.local` | `22222222-2222-2222-2222-222222222222` |
| MERCHANT | `merchant_pizza` | `merchant_pizza@foodya.local` | `33333333-3333-3333-3333-333333333333` |
| MERCHANT | `merchant_sushi` | `merchant_sushi@foodya.local` | `44444444-4444-4444-4444-444444444444` |
| DELIVERY | `delivery_user` | `delivery@foodya.local` | `55555555-5555-5555-5555-555555555555` |
| CUSTOMER | `customer_alice` | `alice@foodya.local` | `66666666-6666-6666-6666-666666666666` |
| CUSTOMER | `customer_bob` | `bob@foodya.local` | `77777777-7777-7777-7777-777777777777` |
| CUSTOMER | `customer_carol` | `carol@foodya.local` | `88888888-8888-8888-8888-888888888888` |
| CUSTOMER | `customer_david` | `david@foodya.local` | `99999999-9999-9999-9999-999999999999` |

## Quick Token Retrieval

Example login request:

```bash
curl -s -X POST http://localhost:8080/api/v1/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"usernameOrEmail":"api_customer","password":"Strong@123"}'
```

Response path to use in Swagger Authorize:

- `$.data.accessToken`

## Swagger/OpenAPI Workflow

1. Open `http://localhost:8080/swagger-ui.html`.
2. Call `POST /api/v1/auth/login` with a seeded account.
3. Copy `data.accessToken` from response.
4. Click `Authorize` and paste the token value.
5. Execute secured APIs by role:
   - Customer routes: `api_customer`
   - Merchant routes: `api_merchant`
   - Delivery routes: `api_delivery`
   - Admin routes: `api_admin`

## Notes

- Tokens are JWT and expire based on `foodya.security.access-token-minutes`.
- Refresh flow is available at `POST /api/v1/auth/refresh` using `data.refreshToken`.
- For stable manual API route coverage, prefer the V12 `api_*` accounts.
