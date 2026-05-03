# Email Automation App

[![Build Status](https://img.shields.io/badge/build-passing-brightgreen?style=flat-square)](https://github.com/baargavi100/EmailAutomation_App/actions)
[![Java](https://img.shields.io/badge/java-21-blue?style=flat-square)](https://www.oracle.com/java/)
[![Spring Boot](https://img.shields.io/badge/spring--boot-3.2.3-brightgreen?style=flat-square)](https://spring.io/projects/spring-boot)
[![License](https://img.shields.io/badge/license-MIT-blue?style=flat-square)](LICENSE)
[![Repo Size](https://img.shields.io/github/repo-size/baargavi100/EmailAutomation_App?style=flat-square)](https://github.com/baargavi100/EmailAutomation_App)

## Overview

Email Automation App is a full-stack initiative for enterprise outreach workflows. It provides role-based access, contact management, templated email sends, analytics dashboarding, and workflow orchestration through n8n.

##  Key Features

-  Role-based authentication (ADMIN, OPERATOR)
-  User session management (X-Auth-Token)
-  CSV import/export for contacts and logs
-  Send and Add-and-Send email workflows
-  Dashboard metrics (queued, sent, delivered, ailed)
-  Google Sheets sync (optional)
-  n8n webhook integration for email operations
-  CORS ready for local frontend usage

##  Tech Stack

### Backend
- Java 21
- Spring Boot 3.2.3
- Maven
- Spring Web + WebFlux (WebClient)

### Frontend
- Vanilla HTML/CSS/JavaScript
- Static hosting (Python HTTP server in dev)

### Integrations
- n8n workflow
- Google Sheets API (service account)

##  Repository Structure

`
EmailAutomation_app/
  backend/
    src/main/java/com/wenxt/emailautomation/{config,controller,model,service}
    src/main/resources/application.properties
    pom.xml
  frontend/{index.html,app.js,style.css}
  BACKEND_COMPLETE_BUILD_DOCUMENTATION.md
  COMPLETE_TESTING_GUIDE.md
  README.md
`

##  Configuration

Edit ackend/src/main/resources/application.properties:

`properties
server.port=8080
n8n.webhook.url=https://unfledged-dayle-forcedly.ngrok-free.dev/webhook/send-email
google.sheets.id=<your-google-sheet-id>
`

Local n8n test URLs:
- http://localhost:5678/webhook-test/send-email
- http://localhost:5678/webhook/send-email

##  Run Locally

### Prerequisites
- Java 21
- Maven 3.9+
- Python 3

### Backend
`powershell
cd backend
mvn spring-boot:run
`

Health URL: http://localhost:8080/api/health

### Frontend
`powershell
cd frontend
python -m http.server 5500
`

App URL: http://localhost:5500

##  Demo Accounts
- dmin / admin123
- operator / operator123

##  API Summary
Base path: /api

- GET /health
- POST /auth/login
- POST /auth/logout
- GET /auth/me
- GET /persons
- GET /search?query=...
- POST /persons
- PUT /persons/{email}
- DELETE /persons/{email}
- POST /send
- POST /add-and-send
- GET /logs
- GET /stats
- GET /export/csv (admin)
- POST /import/csv (admin)

##  Deployment
1. Create GitHub repo: https://github.com/baargavi100/EmailAutomation_App
2. Link local repo and push (below)

##  Security Notes
- Never commit secret files or API keys.
- Add credentials.json to .gitignore.

##  License
MIT (add LICENSE file in repo root)
