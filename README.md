# GymCheck Backend

GymCheck backend is a Kotlin/Spring Boot API for a workout check-in app. It owns social login, JWT authentication, workout logs, exercise type management, statistics, streaks, and Firebase push notification settings.

## Quick Start

Prerequisites:

- JDK 21
- Docker, for local PostgreSQL
- Firebase credentials only when push notifications are enabled

Run locally:

```bash
docker-compose up -d
./gradlew bootRun --args='--spring.profiles.active=local'
```

Run verification:

```bash
./gradlew test
./gradlew build
```

The application starts on `http://localhost:8080`. Swagger UI is available at `http://localhost:8080/swagger-ui/index.html`.

## Environment

`src/main/resources/application.yml` imports a root `.env` file through `spring.config.import: optional:file:.env[.properties]`. Local database defaults match `docker-compose.yml`, so a `.env` file is optional for basic DB startup.

Common variables:

| Variable | Purpose | Local default |
| --- | --- | --- |
| `DB_USERNAME` | PostgreSQL username | `gymcheck` |
| `DB_PASSWORD` | PostgreSQL password | `gymcheck` |
| `JWT_SECRET` | HMAC signing key for access and refresh JWTs | development fallback |
| `JWT_ACCESS_TOKEN_EXPIRATION` | Access token TTL in milliseconds | `1800000` |
| `JWT_REFRESH_TOKEN_EXPIRATION` | Refresh token TTL in milliseconds | `2592000000` |
| `GOOGLE_CLIENT_ID` | Google ID token audience | empty |
| `KAKAO_CLIENT_ID` | Kakao REST API key | empty |
| `KAKAO_CLIENT_SECRET` | Kakao client secret | empty |
| `KAKAO_REDIRECT_URI` | Kakao OAuth redirect URI | empty |
| `FIREBASE_CREDENTIALS_PATH` | Firebase Admin SDK JSON path | empty, uses application default credentials |
| `FIREBASE_PROJECT_ID` | Firebase project id | empty |
| `firebase.enabled` | Enables Firebase beans and scheduler | true unless explicitly false |

For tests, `src/test/resources/application.yml` switches to H2 in PostgreSQL compatibility mode and disables Firebase.

## Package Map

```text
com.gymcheck
├── config/            Spring, security, OpenAPI, Firebase, and time beans
├── controller/        REST API entry points
├── domain/            JPA entities grouped by user, workout, auth, notification
├── dto/               Request and response contracts
├── exception/         ErrorCode, CustomException, global error response handling
├── repository/        Spring Data JPA repositories
├── security/
│   ├── jwt/           Token creation, parsing, authentication filter
│   └── oauth/         OAuthClient abstraction and provider implementations
└── service/           Business rules and transaction boundaries
```

The usual request path is:

```text
Controller -> Service -> Repository -> Domain entity -> DTO response
```

Controllers should stay thin. Business rules such as ownership checks, duplicate names, token rotation, goal calculations, and notification defaults belong in services.

## Main Flows

### Authentication

1. Client calls `POST /auth/oauth/google` with a Google ID token or `POST /auth/oauth/kakao` with a Kakao authorization code.
2. `AuthService` selects the matching `OAuthClient` implementation from the injected client list.
3. The provider client verifies or exchanges the credential and returns normalized `OAuthUserInfo`.
4. `AuthService` finds or creates a `User` using `(socialProvider, socialId)`.
5. The API returns an access token and refresh token. Access tokens are stateless JWTs; refresh tokens are JWTs persisted in `refresh_tokens`.
6. `JwtAuthenticationFilter` reads `Authorization: Bearer <accessToken>` and puts `UserPrincipal(id)` into the Spring Security context.

Refresh token behavior is intentionally one-active-token-per-user: `RefreshTokenService.saveRefreshToken` deletes existing tokens before saving the newly issued token.

### Workout Logs and Exercise Types

- `ExerciseType` can be global default data (`isDefault = true`) or user-owned custom data.
- Users can create, update, and delete only their custom exercise types.
- Workout logs can reference default exercise types or the current user's custom exercise types.
- `ExerciseTypeService.getExerciseTypes` sorts by the last 30 days of usage, then default status, then name.

### Statistics and Streaks

- Monthly calendar and summary endpoints use `WorkoutLog.logDate`, not `createdAt`.
- Daily goals count distinct workout dates.
- Weekly goals count total workout logs in Monday-to-Sunday periods.
- Future months report zero weekly progress.
- Past-month weekly progress is calculated from the week containing that month end.

`TimeConfig` exposes a `Clock` bean so tests can control date-sensitive behavior.

### Notifications

- `NotificationSetting` stores whether reminders are enabled, the preferred local `notifyTime`, and an IANA timezone such as `Asia/Seoul`.
- `FcmToken` stores per-device Firebase tokens and is unique per `(user_id, token)`.
- `NotificationScheduler` runs every minute when `firebase.enabled=true`.
- The scheduler loads enabled settings, converts the current instant to each user's timezone, then sends through `FcmService` when local time matches `notifyTime`.

## Database

Flyway migrations live in `src/main/resources/db/migration`.

Rules:

- Do not rely on Hibernate schema generation for application environments. `spring.jpa.hibernate.ddl-auto` is `validate`.
- Add a new `V{N}__description.sql` migration for every schema or seed-data change.
- Keep entity mapping, DTO behavior, and migration files in sync.

Current migration sequence:

- `V1__init_schema.sql`: users, goals, exercise types, workout logs, refresh tokens, FCM tokens
- `V2__seed_default_exercises.sql`: initial default exercise type data
- `V3__notification_settings.sql`: notification settings

## API Surface

Public endpoints:

- `GET /health`
- `POST /auth/oauth/google`
- `POST /auth/oauth/kakao`
- `POST /auth/refresh`
- Swagger/OpenAPI paths under `/swagger-ui/**` and `/v3/api-docs/**`

Authenticated endpoint groups:

- `/auth/logout`
- `/users/**`
- `/exercise-types/**`
- `/workout-logs/**`
- `/stats/**`
- `/notifications/**`

Use `Authorization: Bearer <accessToken>` for authenticated calls.

## Common Development Tasks

Add a new OAuth provider:

1. Add a `SocialProvider` enum value.
2. Implement `OAuthClient`.
3. Register provider properties/configuration if needed.
4. Add controller route and integration tests.
5. Update Swagger descriptions and this README.

Add or change an API field:

1. Update the request or response DTO.
2. Update service mapping logic.
3. Update controller tests and any affected service tests.
4. If persisted, add a Flyway migration and update the entity.

Change date-sensitive rules:

1. Use the injected `Clock`; do not call system time directly in business services.
2. Add or update tests for current, past, and future date cases.
3. Check both streak and summary behavior if the change touches goals.

## Testing Notes

Useful commands:

```bash
./gradlew test
./gradlew test --tests "com.gymcheck.service.StatisticsServiceTest"
./gradlew test --tests "com.gymcheck.controller.auth.AuthControllerIntegrationTest"
```

Test coverage is organized around:

- Controller integration tests for HTTP status, auth, validation, and JSON contracts
- Service tests for goal, statistics, streak, and token rules
- OAuth client tests using mocked provider behavior
- Flyway/JPA tests for migration and repository compatibility

Prefer adding focused tests near the changed behavior instead of broad snapshot-style assertions.
