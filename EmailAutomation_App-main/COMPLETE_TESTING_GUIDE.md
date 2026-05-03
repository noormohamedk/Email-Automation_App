# Complete Testing Guide - Email Automation App

This guide is written for beginners. Follow it step by step.

## 1. What You Will Test

You will validate both frontend and backend for:

- App startup
- Login and role behavior (admin, operator)
- Contact create, update, delete, search
- Send and add-and-send flows
- Dashboard stats and logs
- CSV export and CSV import
- Error handling and authorization rules

## 2. Project Structure

- Backend: `backend/`
- Frontend: `frontend/`

Important backend files:

- `backend/src/main/java/com/wenxt/emailautomation/controller/EmailController.java`
- `backend/src/main/java/com/wenxt/emailautomation/service/PersonDirectoryService.java`
- `backend/src/main/resources/application.properties`

Important frontend file:

- `frontend/app.js`

## 3. Prerequisites

Install/verify these:

- Java 21
- Maven 3.9+
- Python 3 (for quick static frontend hosting)
- PowerShell (Windows)

Optional but recommended:

- Postman (for API testing)

## 4. Start the App

Open 2 terminals.

### Terminal A - Backend

Run from repository root:

```powershell
$env:JAVA_HOME = "C:\Users\murug\.jdk\jdk-21.0.8"
$env:PATH = "C:\Users\murug\.jdk\jdk-21.0.8\bin;C:\Users\murug\.maven\maven-3.9.14\bin;$env:PATH"
cd d:\EmailAutomation_app\backend
mvn spring-boot:run
```

If `spring-boot:run` fails, still verify compile first:

```powershell
$env:JAVA_HOME = "C:\Users\murug\.jdk\jdk-21.0.8"
$env:PATH = "C:\Users\murug\.jdk\jdk-21.0.8\bin;C:\Users\murug\.maven\maven-3.9.14\bin;$env:PATH"
cd d:\EmailAutomation_app\backend
mvn clean test-compile -q
```

### Terminal B - Frontend

```powershell
cd d:\EmailAutomation_app\frontend
python -m http.server 5500
```

### Browser URLs

- Frontend: `http://localhost:5500`
- Backend health: `http://localhost:8080/api/health`

## 5. Demo Credentials

- Admin user: `admin` / `admin123`
- Operator user: `operator` / `operator123`

## 6. Quick UI Smoke Test (10-15 min)

### 6.1 Login

1. Open frontend URL.
2. Login as admin.
3. Expected: Workspace is visible and role appears.

### 6.2 Compose and Send

1. Open **Compose** tab.
2. Fill name, email, provider, schedule, and message.
3. Click **Send**.
4. Expected: success toast and updated stats/logs.

### 6.3 Add and Send

1. In Compose tab, keep valid data.
2. Click **Add and Send**.
3. Expected: contact is created (if new) and send flow processed.

### 6.4 Contacts CRUD

1. Open **Contacts** tab.
2. Create contact with **Save Contact**.
3. Click row in table to load it.
4. Update fields and save again.
5. Click **Delete** (admin only).
6. Expected: create/update/delete reflected in table and stats.

### 6.5 CSV Tools

1. Open **CSV Tools** tab.
2. Click **Download CSV** (admin only).
3. Choose CSV file and click **Upload CSV**.
4. Expected: import summary toast with imported/skipped counts.

### 6.6 Role Restrictions

1. Logout.
2. Login as operator.
3. Expected: delete/export/import actions are blocked.

## 7. Backend API Testing (PowerShell)

Run commands from any terminal.

### 7.1 Health

```powershell
Invoke-RestMethod -Method Get -Uri http://localhost:8080/api/health | ConvertTo-Json -Depth 5
```

Expected:

- `success: true`
- data should be `OK`

### 7.2 Login and Token

```powershell
$loginBody = @{ username = "admin"; password = "admin123" } | ConvertTo-Json
$login = Invoke-RestMethod -Method Post -Uri http://localhost:8080/api/auth/login -ContentType "application/json" -Body $loginBody
$token = $login.data.token
$headers = @{ "X-Auth-Token" = $token }
$login | ConvertTo-Json -Depth 5
```

Expected:

- token exists
- role is `ADMIN`

### 7.3 Session Check

```powershell
Invoke-RestMethod -Method Get -Uri http://localhost:8080/api/auth/me -Headers $headers | ConvertTo-Json -Depth 5
```

### 7.4 Create Person

```powershell
$person = @{ name="Test One"; email="test.one@example.com"; provider="Gmail"; message="Hi"; aiGenerated="false"; schedule="0"; status="Queue"; sentTime="" } | ConvertTo-Json
Invoke-RestMethod -Method Post -Uri http://localhost:8080/api/persons -Headers $headers -ContentType "application/json" -Body $person | ConvertTo-Json -Depth 5
```

### 7.5 List Persons

```powershell
Invoke-RestMethod -Method Get -Uri http://localhost:8080/api/persons -Headers $headers | ConvertTo-Json -Depth 6
```

### 7.6 Search Person

```powershell
Invoke-RestMethod -Method Get -Uri "http://localhost:8080/api/search?query=test.one@example.com" -Headers $headers | ConvertTo-Json -Depth 6
```

### 7.7 Update Person

```powershell
$updated = @{ name="Test One Updated"; email="test.one@example.com"; provider="Outlook"; message="Updated"; aiGenerated="false"; schedule="180"; status="Queue"; sentTime="" } | ConvertTo-Json
Invoke-RestMethod -Method Put -Uri "http://localhost:8080/api/persons/test.one@example.com" -Headers $headers -ContentType "application/json" -Body $updated | ConvertTo-Json -Depth 5
```

### 7.8 Send Email

```powershell
$send = @{ name="Test One Updated"; email="test.one@example.com"; provider="Outlook"; message="Hello from API test"; schedule="0" } | ConvertTo-Json
Invoke-RestMethod -Method Post -Uri http://localhost:8080/api/send -Headers $headers -ContentType "application/json" -Body $send | ConvertTo-Json -Depth 5
```

### 7.9 Add and Send

```powershell
$aas = @{ name="Another User"; email="another.user@example.com"; provider="Gmail"; message="Add and send test"; schedule="180" } | ConvertTo-Json
Invoke-RestMethod -Method Post -Uri http://localhost:8080/api/add-and-send -Headers $headers -ContentType "application/json" -Body $aas | ConvertTo-Json -Depth 5
```

### 7.10 Stats and Logs

```powershell
Invoke-RestMethod -Method Get -Uri http://localhost:8080/api/stats -Headers $headers | ConvertTo-Json -Depth 5
Invoke-RestMethod -Method Get -Uri http://localhost:8080/api/logs -Headers $headers | ConvertTo-Json -Depth 6
```

### 7.11 Export CSV (Admin)

```powershell
$csv = Invoke-WebRequest -Method Get -Uri http://localhost:8080/api/export/csv -Headers $headers
$csv.Content
```

### 7.12 Import CSV (Admin)

```powershell
$csvBody = "name,email,provider,message,aiGenerated,schedule,status,sentTime`nCsv User,csv.user@example.com,Gmail,From CSV,false,0,Queue,"
Invoke-RestMethod -Method Post -Uri http://localhost:8080/api/import/csv -Headers $headers -ContentType "text/csv" -Body $csvBody | ConvertTo-Json -Depth 5
```

### 7.13 Delete Person (Admin)

```powershell
Invoke-RestMethod -Method Delete -Uri "http://localhost:8080/api/persons/test.one@example.com" -Headers $headers | ConvertTo-Json -Depth 5
```

### 7.14 Logout

```powershell
Invoke-RestMethod -Method Post -Uri http://localhost:8080/api/auth/logout -Headers $headers | ConvertTo-Json -Depth 5
```

## 8. Authorization Tests (Must Test)

### 8.1 Without Token

Call protected endpoint without `X-Auth-Token`:

```powershell
Invoke-RestMethod -Method Get -Uri http://localhost:8080/api/persons
```

Expected: unauthorized error.

### 8.2 Operator Restriction

Login as operator and try admin endpoints:

- `DELETE /api/persons/{email}`
- `GET /api/export/csv`
- `POST /api/import/csv`

Expected: forbidden/admin-role-required behavior.

## 9. Negative Test Cases

Run these intentionally:

- Invalid login credentials
- Create contact with blank email
- Send request with blank message
- Update non-existing contact
- Delete non-existing contact
- Upload malformed CSV lines

Expected: clear validation messages, no app crash.

## 10. Integration Notes (n8n + Google Sheets)

### n8n

Configured in:

- `backend/src/main/resources/application.properties`

Property:

- `n8n.webhook.url=...`

If webhook URL is unreachable, send endpoints may return error text, which is acceptable during local tests.

### Google Sheets

- `google.sheets.id` is configured in properties.
- `credentials.json` is required in `backend/src/main/resources/` for sheet access.
- If missing, app should continue with graceful fallback/in-memory behavior.

## 11. Completion Checklist

Mark all items done:

- [ ] Backend starts or compiles successfully on Java 21
- [ ] Frontend loads at localhost:5500
- [ ] Health endpoint returns success
- [ ] Admin login/session/logout works
- [ ] Operator login works
- [ ] CRUD on contacts works
- [ ] Search works
- [ ] Send and add-and-send work (or fail gracefully if webhook unavailable)
- [ ] Stats and logs update correctly
- [ ] CSV export/import works for admin
- [ ] Unauthorized and forbidden behaviors are correct

## 12. Troubleshooting

### Backend `spring-boot:run` fails

1. Check if port 8080 is already in use.
2. Run compile check:

```powershell
mvn clean test-compile -q
```

3. Re-run startup and read full stack trace in terminal.

### Frontend cannot reach backend

- Confirm backend is running on 8080.
- Confirm frontend is opened from `http://localhost:5500`.
- Check CORS config in backend.

### Send flow fails

- Verify `n8n.webhook.url` is active and reachable.

### No historical Google Sheet data

- Add valid `credentials.json` to `backend/src/main/resources/`.

## 13. Suggested Next Testing Improvements

- Add automated unit tests under `backend/src/test/java`.
- Add API collection in Postman and share with team.
- Add CI pipeline to run `mvn clean test` automatically.
- Add test data CSV files under a dedicated `test-data/` folder.
