# Backend Complete Build Documentation

This document describes everything currently implemented in your backend project.

## 1. Backend Identity and Runtime

- Project name: EmailAutomation
- Group and artifact: com.wenxt:emailautomation
- Framework: Spring Boot 3.2.3
- Java version: 21
- Default server port: 8080
- Main app entrypoint: `EmailAutomationApplication`

## 2. Backend Architecture

Your backend is a REST API application with these layers:

- Controller layer: HTTP endpoints under `/api`
- Service layer:
  - Authentication/session service (`AuthService`)
  - Email delivery orchestration (`EmailService`)
  - Person directory and business rules (`PersonDirectoryService`)
  - Google Sheets integration (`GoogleSheetsService`)
- Model layer: Request/response and domain models
- Config layer: CORS settings (`CorsConfig`)

## 3. Dependencies You Added

Core dependencies configured in Maven:

- `spring-boot-starter-web` for REST APIs
- `spring-boot-starter-webflux` for `WebClient` outbound calls
- Google libraries:
  - `google-api-client`
  - `google-oauth-client-jetty`
  - `google-api-services-sheets`
  - `google-auth-library-oauth2-http`
- `lombok` for model boilerplate generation
- `spring-boot-starter-test` for test support

## 4. Configuration in application.properties

You configured these important properties:

- `server.port=8080`
- `n8n.webhook.url=<your webhook URL>`
- `google.sheets.id=<your spreadsheet id>`
- Logging:
  - `logging.level.com.wenxt=INFO`
  - `logging.level.root=WARN`
- JSON response pretty print:
  - `spring.jackson.serialization.indent-output=true`

## 5. Security and Access Model Implemented

### 5.1 Login System

You built a lightweight in-memory authentication system:

- Hardcoded users:
  - `admin / admin123` with role `ADMIN`
  - `operator / operator123` with role `OPERATOR`
- On successful login:
  - Server creates a UUID token
  - Session is stored in memory (`ConcurrentHashMap`)
- Session token is sent in request header:
  - `X-Auth-Token`

### 5.2 Role-based Authorization

- `ADMIN` and `OPERATOR` can use most operational APIs
- `ADMIN` only APIs:
  - Delete person
  - Export CSV
  - Import CSV

### 5.3 CORS

CORS is configured for:

- `http://localhost:5500`
- `http://127.0.0.1:5500`

Allowed methods: GET, POST, PUT, DELETE, OPTIONS

Allowed headers include:

- `Content-Type`
- `X-Auth-Token`

## 6. API Endpoints You Built

All endpoints are under base path `/api`.

### 6.1 Health

- `GET /health`
- Purpose: quick backend health check

### 6.2 Authentication

- `POST /auth/login`
  - Body: `AuthRequest { username, password }`
  - Returns token + role on success
- `POST /auth/logout`
  - Requires `X-Auth-Token`
  - Invalidates current session token
- `GET /auth/me`
  - Requires `X-Auth-Token`
  - Returns active session details

### 6.3 Person Directory

- `GET /persons`
  - Role: ADMIN or OPERATOR
  - Returns all persons from in-memory directory
- `GET /search?query=...`
  - Role: ADMIN or OPERATOR
  - Searches by name or email (contains match)
- `POST /persons`
  - Role: ADMIN or OPERATOR
  - Creates person (email required)
- `PUT /persons/{email}`
  - Role: ADMIN or OPERATOR
  - Updates person by original email
- `DELETE /persons/{email}`
  - Role: ADMIN only
  - Deletes person by email

### 6.4 Email Sending Flows

- `POST /send`
  - Role: ADMIN or OPERATOR
  - Validates email and message
  - Calls n8n webhook via `EmailService.sendEmail`
  - Records send result/status in directory and Google Sheets
- `POST /add-and-send`
  - Role: ADMIN or OPERATOR
  - First adds person with `Queue` status
  - Then sends email via same webhook flow
  - Records send result/status

### 6.5 Logs and Stats

- `GET /logs`
  - Role: ADMIN or OPERATOR
  - Returns current person records
- `GET /stats`
  - Role: ADMIN or OPERATOR
  - Returns counts by status and total

### 6.6 CSV Data Portability

- `GET /export/csv`
  - Role: ADMIN only
  - Returns CSV text for all current records
- `POST /import/csv`
  - Role: ADMIN only
  - Consumes plain text CSV body
  - Returns import summary `{ imported, skipped }`

## 7. Data Models You Built

### 7.1 `ApiResponse<T>`

Standard envelope used by almost all APIs:

- `success` (boolean)
- `message` (string)
- `data` (generic payload)

### 7.2 `AuthRequest`

- `username`
- `password`

### 7.3 `AuthResponse`

- `token`
- `username`
- `role`

### 7.4 `UserSession`

- `token`
- `username`
- `role`

### 7.5 `EmailRequest`

- `name`
- `email`
- `provider`
- `message`
- `schedule`

### 7.6 `Person`

- `name`
- `email`
- `provider`
- `message`
- `aiGenerated`
- `schedule`
- `status`
- `sentTime`

## 8. Service Logic You Implemented

### 8.1 AuthService

- In-memory user store
- In-memory token/session store
- Login validation
- Logout (session removal)
- Role validation helper (`hasAnyRole`)

### 8.2 EmailService

- Sends outbound HTTP POST to n8n webhook
- Uses Spring `WebClient`
- Sends full `EmailRequest` JSON
- Handles failures and returns readable error strings
- `addAndSend` currently delegates to `sendEmail`

### 8.3 GoogleSheetsService

- Loads Google credentials from `src/main/resources/credentials.json`
- Uses service account auth with Sheets scope
- Reads rows from `Sheet1!A:H`
- Maps sheet rows to `Person`
- Supports append row operation for new/updated send records
- Provides basic status stats (`queue`, `sent`, `viewed`, `total`)
- Graceful fallback to empty list if credentials/sheet access fails

### 8.4 PersonDirectoryService

- Maintains in-memory `personMap` keyed by normalized email
- Warm-up bootstrap from Google Sheets at startup
- Normalization rules:
  - Lowercase, trimmed email
  - Defaults for provider/schedule/status/aiGenerated
- Supports create, update, delete, search, get-all
- Send record tracking (`recordSend`) updates memory and appends to sheet
- CSV export and import logic
- Computes stats including `failed`

## 9. End-to-End Data Flow

### 9.1 Startup

1. Spring Boot starts.
2. `PersonDirectoryService` loads people from Google Sheets.
3. Data is stored in in-memory map for fast access.

### 9.2 Login Flow

1. Client sends username/password.
2. `AuthService` validates against in-memory users.
3. Token is generated and returned.
4. Client includes token in `X-Auth-Token` for protected routes.

### 9.3 Send Email Flow

1. Client posts `EmailRequest` to `/send` or `/add-and-send`.
2. Controller validates required fields.
3. `EmailService` calls n8n webhook.
4. Result status determined (`Sent` or `Failed`).
5. `PersonDirectoryService.recordSend` persists record in memory and appends to sheet.

## 10. Current Functional Strengths

- Clean endpoint organization and consistent `/api` namespace
- Simple and usable role model (`ADMIN` and `OPERATOR`)
- Practical token-based session management
- Clear API response envelope via `ApiResponse`
- Outbound integration with n8n for email workflow automation
- Google Sheets persistence integration
- CSV import/export capability for operational flexibility
- Startup warm-load and in-memory speed for common reads/search

## 11. Important Current Limitations

- Auth users/passwords are hardcoded in source
- Sessions are in-memory only (lost on restart)
- No password hashing or JWT-based stateless auth
- No persistent database (memory + append-only sheet behavior)
- Update/delete operations are not synchronized back to existing sheet rows
- CSV parser is simple split-based parser (limited quoting edge-case handling)
- `/logs` currently returns same data shape as `/persons`
- No formal validation annotations (`@Valid`, bean validation) yet
- No centralized exception handler (`@ControllerAdvice`) yet
- No automated test classes present in backend source tree

## 12. What You Have Successfully Built (Summary)

You have built a complete Java Spring Boot backend for an email automation platform that already includes:

- Authentication with role-based access
- Person directory CRUD + search
- Email send orchestration through n8n
- Tracking and stats for email operations
- Google Sheets integration for external record storage
- CSV import/export tools for admin operations
- CORS and structured API response patterns

This is a strong functional backend foundation for your product.
