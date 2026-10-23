# Autonomous Research Workspace — Backend

[![Java](https://img.shields.io/badge/Java-21%20LTS-orange.svg)](https://www.oracle.com/java/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.3.4-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-16%2B%20pgvector-blue.svg)](https://www.postgresql.org/)
[![License](https://img.shields.io/badge/License-MIT-green.svg)](LICENSE)

The **Autonomous Research Workspace** is an enterprise-grade AI-powered research automation platform. Users submit a research topic (e.g., *"Analyze India's semiconductor manufacturing ecosystem, major investments, government policies, challenges, and future opportunities"*), and the system autonomously plans multi-angle investigations, searches live web and academic sources, extracts and cleans content, chunks documents, computes embeddings, retrieves evidence via hybrid vector/keyword search, extracts factual claims, cross-verifies claims (`SUPPORTED`, `PARTIALLY_SUPPORTED`, `CONTRADICTED`, `UNVERIFIED`), synthesizes comprehensive research reports with IEEE & APA citations, streams real-time step progress over Server-Sent Events (SSE), and exports publication-ready PDF reports and PowerPoint presentations.

---

## 1. System Architecture & Autonomous Pipeline

```
                              [ User Request ]
                                     │
                                     ▼
                      [ ResearchWorkflowCoordinator ]
                                     │
      ┌──────────────────────────────┼──────────────────────────────┐
      │                              │                              │
      ▼                              ▼                              ▼
1. ResearchPlannerService    2. WebSearchService          3. AcademicSearchService
   • Formulates objective       • DuckDuckGo HTML Live       • Semantic Scholar API
   • Subquestions & Queries     • Tavily API / Fallback      • Deduplication & Normalization
      │                              │                              │
      └──────────────────────────────┼──────────────────────────────┘
                                     ▼
                      4. WebPageExtractionService
                         • Jsoup content cleaning
                         • SSRF & Link-Local Protection (RFC1918)
                         • Unsafe protocol & domain blocking
                                     │
                                     ▼
                      5. DocumentProcessingService
                         • Document & Chunk persistence
                         • Sentence-boundary chunking (TextChunker)
                         • Embeddings generation (VectorMath)
                                     │
                                     ▼
                      6. EvidenceRetrievalService
                         • Hybrid 70% Cosine Vector + 30% Keyword
                         • Scored contextual evidence retrieval
                                     │
                                     ▼
                      7. ClaimExtractionService
                         • Extract distinct factual assertions
                                     │
                                     ▼
                      8. ClaimVerificationService
                         • Evidence cross-checking
                         • Multi-status classification:
                           SUPPORTED | PARTIALLY_SUPPORTED | CONTRADICTED | UNVERIFIED
                                     │
                                     ▼
                      9. ResearchSynthesisService & CitationService
                         • Grounded report synthesis
                         • IEEE & APA formatted references
                                     │
                                     ▼
                      10. ReportExportService
                         • Multi-page PDF generation (Apache PDFBox)
                         • Professional slide deck generation (Apache POI PPTX)
                                     │
                                     ▼
                      11. SseEmitterService
                         • Real-time SSE streaming to Frontend
```

---

## 2. Technology Stack

- **Runtime & Language**: Java 21 LTS (targeting Java 21 bytecode).
- **Core Framework**: Spring Boot 3.3.4 (Web MVC, Security, Data JPA, Validation, Actuator).
- **Security & Auth**: Spring Security with stateless JWT (Access & SHA-256 hashed Refresh Tokens), Argon2 password hashing, and granular resource-level authorization.
- **Database**: PostgreSQL 16+ with Flyway database migrations (`V1`, `V2`, `V3`).
- **Vector Search & RAG**: Cosine vector similarity & hybrid keyword retrieval (`VectorMath`, pgvector compatible schema).
- **Web & Academic Search**: Live DuckDuckGo HTML Search (`DuckDuckGoHtmlSearchProvider`), Semantic Scholar Academic API (`SemanticScholarSearchProvider`), Tavily Search (`TavilySearchProvider`), and offline high-fidelity test fallback (`MockSearchProvider`).
- **HTML & Document Parsing**: Jsoup 1.18.1 with robust SSRF safeguards (`UrlValidator`).
- **Export Engines**: Apache PDFBox 3.0.3 (PDF report generation) and Apache POI 5.3.0 (PPTX slide deck generation).
- **Documentation**: SpringDoc OpenAPI 3 (`/swagger-ui.html`).
- **Testing**: JUnit 5, Mockito, Spring Boot Test, Testcontainers.

---

## 3. Database Schema & Flyway Migrations

The database schema is managed deterministically via Flyway migrations located in `src/main/resources/db/migration/`:

1. `V1__initial_schema.sql`: Core user authentication, roles (`STUDENT`, `RECRUITER`, `ADMIN`), and refresh tokens.
2. `V2__research_workspace_schema.sql`: Autonomous research workspace tables:
   - `workspaces`, `workspace_members` (multi-tenant workspace security & roles: `OWNER`, `ADMIN`, `MEMBER`, `VIEWER`).
   - `projects` (scoped research containers).
   - `research_runs` (execution metadata, depth, token usage, progress).
   - `research_steps` (audit trail and step-by-step execution metrics).
   - `sources`, `run_sources` (web and academic references, canonical URLs).
   - `documents`, `document_chunks` (text chunks, token counts, vector embeddings).
   - `claims`, `claim_evidence` (claim text, verification statuses, contradictory passages).
   - `reports`, `citations` (structured executive summary, methodology, findings, references).
   - `artifacts` (persisted binary files: PDF reports, PPTX decks).
   - `scheduled_research` (recurring cron-based automated research jobs).
   - `notifications`, `audit_logs` (event logging and user notifications).
3. `V3__cascade_user_deletion.sql`: Foreign key cascade constraints for clean user lifecycle teardown.

---

## 4. REST API Reference

All protected endpoints require `Authorization: Bearer <token>` header or `?token=<token>` query parameter (supported for SSE and binary downloads).

### Authentication & Users
| Method | Endpoint | Description | Auth Required |
|---|---|---|---|
| `POST` | `/api/v1/auth/register` | Register a new user | No |
| `POST` | `/api/v1/auth/login` | Authenticate and obtain JWT access & refresh tokens | No |
| `POST` | `/api/v1/auth/refresh` | Exchange valid refresh token for new access token | No |
| `POST` | `/api/v1/auth/logout` | Revoke refresh token and invalidate session | Yes |
| `GET` | `/api/v1/users/me` | Fetch profile of authenticated user | Yes |
| `PATCH` | `/api/v1/users/me` | Update current user's profile | Yes |

### Workspaces
| Method | Endpoint | Description | Auth Required |
|---|---|---|---|
| `POST` | `/api/v1/workspaces` | Create a new research workspace | Yes |
| `GET` | `/api/v1/workspaces` | List all workspaces accessible to the user | Yes |
| `GET` | `/api/v1/workspaces/{id}` | Get workspace details, members, and projects | Yes |

### Projects
| Method | Endpoint | Description | Auth Required |
|---|---|---|---|
| `POST` | `/api/v1/projects` | Create a research project inside a workspace | Yes |
| `GET` | `/api/v1/projects` | List projects accessible to user (optional `workspaceId`) | Yes |
| `GET` | `/api/v1/projects/{id}` | Get project overview, active runs, and history | Yes |
| `PATCH` | `/api/v1/projects/{id}` | Update project title, description, or status | Yes |

### Autonomous Research Runs & Execution
| Method | Endpoint | Description | Auth Required |
|---|---|---|---|
| `POST` | `/api/v1/projects/{projectId}/runs` | Submit autonomous research topic (**HTTP 202 Accepted**) | Yes |
| `GET` | `/api/v1/runs/{id}` | Get run details, status, step timings, and metrics | Yes |
| `GET` | `/api/v1/runs/{id}/sources` | Retrieve all collected web and academic sources | Yes |
| `GET` | `/api/v1/runs/{id}/claims` | Retrieve extracted claims, verification statuses & evidence | Yes |
| `GET` | `/api/v1/runs/{id}/report` | Retrieve the generated synthesis report & citations | Yes |
| `POST` | `/api/v1/runs/{id}/cancel` | Safely cancel an in-progress research execution | Yes |
| `GET` | `/api/v1/runs/{id}/events` | **Server-Sent Events (SSE)** stream of live progress events | Yes |

### Report Exports & Artifacts
| Method | Endpoint | Description | Auth Required |
|---|---|---|---|
| `POST` | `/api/v1/runs/{id}/exports` | Request report export (`PDF` or `PPTX`) | Yes |
| `GET` | `/api/v1/artifacts/{id}` | Download generated PDF report or PPTX slide deck | Yes |

### Grounded Research Chat
| Method | Endpoint | Description | Auth Required |
|---|---|---|---|
| `POST` | `/api/v1/projects/{projectId}/chat` | Ask follow-up questions grounded on project evidence | Yes |

### Productivity & Scheduled Research
| Method | Endpoint | Description | Auth Required |
|---|---|---|---|
| `POST` | `/api/v1/projects/{projectId}/schedules` | Create recurring scheduled research job | Yes |
| `GET` | `/api/v1/projects/{projectId}/schedules` | List scheduled research jobs for a project | Yes |
| `PATCH` | `/api/v1/schedules/{id}` | Enable/disable schedule or update cron expression | Yes |
| `DELETE` | `/api/v1/schedules/{id}` | Delete a scheduled research configuration | Yes |

---

## 5. Real-Time Server-Sent Events (SSE) Flow

When a research run is submitted via `POST /api/v1/projects/{projectId}/runs`, the backend queues the task on a bounded async executor and returns **HTTP 202 Accepted** with the `runId`.

The frontend connects to `GET /api/v1/runs/{runId}/events?token=<jwt_token>` and receives structured events in real time:

```json
event: run.created
data: {"step":"RUN_CREATED","progress":5,"message":"Research run initialized"}

event: planning.started
data: {"step":"PLANNING","progress":15,"message":"Formulating research plan and subquestions"}

event: search.progress
data: {"step":"SEARCHING","progress":35,"message":"Discovered 8 web and academic sources"}

event: documents.processed
data: {"step":"DOCUMENT_PROCESSING","progress":55,"message":"Extracted and indexed 24 text chunks"}

event: verification.completed
data: {"step":"VERIFICATION","progress":75,"message":"Verified 6 claims across evidence passages"}

event: report.generated
data: {"step":"REPORT_GENERATION","progress":90,"message":"Synthesized research report with citations"}

event: run.completed
data: {"step":"COMPLETED","progress":100,"message":"Research pipeline completed successfully"}
```

In the event of a failure, a `run.failed` event is emitted with the sanitized error message.

---

## 6. Configuration & Environment Variables

Copy `.env.example` to `.env` or set the environment variables in your deployment environment:

```ini
# ==========================================
# Database Configuration
# ==========================================
DB_URL=jdbc:postgresql://localhost:5432/cc
DB_USERNAME=postgres
DB_PASSWORD=your_secure_db_password

# ==========================================
# JWT Security Configuration
# ==========================================
JWT_SECRET=404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970
JWT_ACCESS_EXPIRATION=900000
JWT_REFRESH_EXPIRATION=604800000

# ==========================================
# CORS Configuration
# ==========================================
CORS_ALLOWED_ORIGINS=http://localhost:3000,http://localhost:5173,http://localhost:8080

# ==========================================
# GenAI & LLM Configuration
# ==========================================
# Set to 'auto' to auto-detect LLM_API_KEY, or 'mock' for local offline testing
LLM_PROVIDER=auto
LLM_BASE_URL=https://api.openai.com/v1
LLM_API_KEY=your_openai_or_groq_api_key_here
LLM_MODEL=gpt-4o-mini
LLM_EMBEDDING_MODEL=text-embedding-3-small
LLM_TIMEOUT_SECONDS=60

# ==========================================
# Search Provider Configuration
# ==========================================
# Set TAVILY_API_KEY if available. If omitted, live DuckDuckGo HTML and Semantic Scholar operate automatically.
SEARCH_PROVIDER=auto
TAVILY_API_KEY=your_tavily_api_key_optional

# ==========================================
# Artifacts Storage Directory
# ==========================================
ARTIFACTS_STORAGE_DIR=target/artifacts
```

---

## 7. How to Run Locally

### Option A: Running Without Docker (Native Local Execution)

You can run the backend completely without Docker using your local PostgreSQL instance and the bundled Maven wrapper.

#### 1. Prerequisites
- **Java 21 LTS** or newer installed (`java -version`).
- **PostgreSQL 14+** installed and running on `localhost:5432` (or a free cloud database such as Supabase / Neon / Render).
- Ensure a database named `cc` exists. In `psql` or pgAdmin, execute:
  ```sql
  CREATE DATABASE cc;
  ```

#### 2. Configure Database Credentials
Verify or update your database username and password in `src/main/resources/application-dev.yml` (or via environment variables in `.env`):
```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/cc
    username: postgres
    password: your_postgres_password
```

#### 3. Build & Package (Optional)
Run Maven through the wrapper to verify dependencies and compile:
```bash
# Windows PowerShell / CMD
.\mvnw.cmd clean package -DskipTests

# macOS / Linux
./mvnw clean package -DskipTests
```

#### 4. Start the Application
You have two options to launch the backend:

**Method 1: Direct Run via Maven (Recommended for Development)**
```bash
# Windows PowerShell / CMD
.\mvnw.cmd spring-boot:run

# macOS / Linux
./mvnw spring-boot:run
```

**Method 2: Run Built Production JAR Directly**
```bash
java -jar target/backend-0.0.1-SNAPSHOT.jar
```

#### 5. Verify Successful Startup
Once launched, you will observe the following in your console:
- Flyway automatically applies migrations (`V1`, `V2`, `V3`) to initialize the full schema.
- Tomcat starts on port `8080`.
- The scheduled research background scheduler activates.
- Interactive OpenAPI Swagger UI is available at: **[http://localhost:8080/swagger-ui/index.html](http://localhost:8080/swagger-ui/index.html)**.
- Health check is accessible at: **[http://localhost:8080/actuator/health](http://localhost:8080/actuator/health)**.

---

### Option B: Running with Docker Compose (Containerized)

If you prefer to run PostgreSQL (with `pgvector`), Redis, and the backend application together inside isolated containers:

```bash
docker compose up --build -d
```
Check container logs:
```bash
docker compose logs -f backend
```
The API and Swagger UI will be available at `http://localhost:8080`.

To stop the containers:
```bash
docker compose down
```

---

## 8. Running Automated Tests

The test suite validates:
- Unit tests: URL SSRF safeguards, sentence-boundary text chunker, cosine vector calculations, search providers, claim verification classifications, citation formatting, and PDF/PPTX export generators.
- Integration tests: Database migrations, JPA entity mappings, user authentication, resource-level authorization boundaries, and full end-to-end autonomous research workflows against PostgreSQL.

Run the test suite using the Maven wrapper:

```bash
# Windows
.\mvnw.cmd clean test

# Linux / macOS
./mvnw clean test
```

---

## 9. API Documentation & OpenAPI Swagger

Interactive Swagger UI and OpenAPI documentation are available when the server is running:

- **Swagger UI**: `http://localhost:8080/swagger-ui.html`
- **OpenAPI v3 JSON**: `http://localhost:8080/v3/api-docs`

---

## 10. Security Safeguards

- **SSRF & Private Network Isolation (`UrlValidator`)**: Prohibits requests targeting `localhost`, `127.0.0.1`, RFC1918 private subnets (`10.0.0.0/8`, `172.16.0.0/12`, `192.168.0.0/16`), and AWS/GCP cloud metadata endpoints (`169.254.169.254`).
- **Resource-Level Authorization (`ProjectSecurityService`)**: Validates workspace and project ownership on every protected resource to prevent unauthorized cross-tenant data access.
- **Untrusted Input Treatment**: Web content and external PDFs are sanitized, truncated to configurable size limits, and parsed in memory without shell execution.
- **Credential Safety**: Zero hardcoded credentials. Refresh tokens are securely hashed with SHA-256 before storage.
