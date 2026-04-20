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

- Default on Android emulator: http://10.0.2.2:8080
- Default on iOS/desktop: http://localhost:8080
- Override at build/run time:

  flutter run --dart-define=FOODYA_API_BASE_URL=http://<host>:8080

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
