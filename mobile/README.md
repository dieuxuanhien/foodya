# Foodya Mobile

Initial Flutter scaffold for Foodya mobile with one app and role-based login switch for Customer and Merchant.

## Stack

- Flutter + Material 3
- flutter_bloc for state management
- go_router for route configuration and role guards

## Implemented Auth Scope

- FR01 Register account
- FR02 Login
- FR03 Refresh session token
- FR26 Logout all sessions

Auth requests target backend endpoints under /api/v1/auth and persist tokens in secure storage.

## API Base URL

The app reads `FOODYA_API_BASE_URL` first. If it is not provided, it uses
platform-specific development defaults from `lib/core/config/app_config.dart`.

- Default on Android emulator: `http://10.0.2.2:8000`
- Default on web: `http://localhost:8000`
- Default on iOS/desktop: `http://localhost:8080`

Override at build/run time when your backend uses a different host or port:

```bash
flutter run --dart-define=FOODYA_API_BASE_URL=http://<host>:8080
```

For Android emulator, use `10.0.2.2` instead of `localhost` to reach a backend
running on the host machine.

## Run

1. Install dependencies:

   flutter pub get

2. Start app:

   flutter run

## Quality

- flutter analyze
- flutter test

## Structure

- lib/core
  - auth: role and session state
  - router: app router and route refresh listener
  - theme: central app theme
- lib/features
  - auth: login repository, cubit, and role login page
  - customer: customer home scaffold
  - merchant: merchant home scaffold

## Requirements Source

- Use docs/FOODYA_SRS.md as source of truth for feature expansion.
