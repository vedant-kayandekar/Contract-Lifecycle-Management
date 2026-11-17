# Contract Story – Backend API & AI Intelligence Engine

> **Assignment Submission**  
> **Role:** Full-Stack Intern (3 Months)  
> **Company:** Seventh Ray Consulting and Technology Private Limited  
> **Product Context:** Contract Story SaaS ([seventhraylegaltech.com](https://www.seventhraylegaltech.com))  
> **Tech Stack:** Java 21, Spring Boot 3.4.2, Spring Security (JWT), PostgreSQL, Apache PDFBox 3.0, Apache POI 5.3, OpenAI-compatible LLM Gateway  

---

## 📌 Project Overview

This repository contains the backend implementation for **Contract Story**, an AI-powered Contract Lifecycle Management (CLM) platform. 

It handles the entire lifecycle of corporate agreements:
1. **User Authentication & Tenant Isolation**: Secure registration and login using stateless JWT with BCrypt password hashing.
2. **Document Ingestion & File Storage**: Uploading PDF, Word (`.docx`), and text contracts, partitioned by user directory on disk.
3. **Document Parsing**: Extracting raw text from PDFs using **Apache PDFBox 3.0** and `.docx` files using **Apache POI 5.3**.
4. **Lifecycle State Machine**: Strict enforcement of contract status transitions (`DRAFT` ➔ `UNDER_REVIEW` ➔ `APPROVED` / `REJECTED`) with business validation that rejects illegal jumps (e.g., trying to approve a draft directly without review).
5. **Intelligent Q&A with Clause Citations**: An AI-ready engine that searches contract clauses, scores relevance, extracts citations, and synthesizes answers using an LLM gateway with a fallback heuristic.
6. **10-Message Conversational Memory**: Thread-aware chat tracking per contract and across the global portfolio.
7. **Cross-Portfolio Global Search**: Querying terms across all contracts owned by a user to surface enterprise-wide insights.

---

## 🏗️ Architecture & Key Engineering Decisions

### 1. Multi-Tenancy & Data Security
Every contract and chat message in the database is strictly tied to a `user_id`. When querying by ID (e.g. `GET /api/contracts/{id}`), the repository uses `findByIdAndUserId(id, user.getId())`. If User A tries to view or alter User B's contract, the system returns `404 Not Found`, preventing unauthorized access.

### 2. State Machine Enforcement
Contract status isn't just an arbitrary database string. It is modeled in `ContractStatus` using modern Java switch pattern matching:
```
[DRAFT] ──────► [UNDER_REVIEW] ──────► [APPROVED]
  ▲                    │                     │
  │                    ▼                     ▼
  └────────────── [REJECTED] ◄───────────────┘
```
* **Allowed:** `DRAFT` ➔ `UNDER_REVIEW` ➔ `APPROVED` (or `REJECTED`).
* **Forbidden:** `DRAFT` ➔ `APPROVED` (skipping review) or `APPROVED` ➔ `DRAFT` (downgrading executed contract directly). These trigger an immediate `400 Bad Request` with an explanatory message.

### 3. Document Parsing Pipeline
* **PDFs (`.pdf`)**: Handled via Apache PDFBox `PDFTextStripper`. Strips line noise and extracts clean text paragraphs.
* **Word Documents (`.docx`)**: Handled via Apache POI `XWPFDocument`. Extracts paragraphs and table text.
* The extracted text is saved directly in PostgreSQL (`parsed_text`), allowing fast indexing and immediate search without reparsing the physical file every time.

### 4. RAG (Retrieval-Augmented Generation) & Fallback Engine
When a user asks a question about a contract:
1. **Stop-word removal** isolates meaningful legal keywords (e.g. `payment`, `notice`, `indemnity`).
2. **Paragraph chunking & scoring** finds the top 3 most relevant clauses in the document.
3. **Prompt assembly** packages the legal persona, contract metadata, cited clauses, and the last 10 messages from conversational history.
4. **LLM Execution**: Sent via Spring 6 `RestClient` to an OpenAI-compatible gateway (`/v1/chat/completions`).
5. **Graceful Fallback**: If the external LLM is offline or times out, the engine catches the exception in `0ms` and returns the cited clauses directly with `aiGenerated: false`. The user **never** receives a `500 Internal Server Error`.

---

## 🗄️ Database Schema

The database runs on **PostgreSQL** (`contract_story_dev`). JPA automatically manages the schema:

| Table | Primary Columns | Purpose |
| :--- | :--- | :--- |
| `users` | `id`, `name`, `email`, `password`, `company_name`, `role` | User accounts and tenant ownership |
| `contracts` | `id`, `user_id`, `title`, `client_name`, `status`, `contract_type`, `file_name`, `file_path`, `parsed_text`, `value`, `start_date`, `end_date` | Main contract metadata and extracted text |
| `contract_chats` | `id`, `contract_id` (nullable), `user_id`, `role`, `message`, `created_at` | Conversational history (single contract & global portfolio) |

---

## 🚀 API Endpoints Reference

All `/api/contracts/**` endpoints require an `Authorization: Bearer <token>` header obtained from `/api/auth/login`.

### 1. Authentication
| Method | Endpoint | Description |
| :--- | :--- | :--- |
| `GET` | `/api/health` | Health check probe |
| `POST` | `/api/auth/register` | Register new user & company |
| `POST` | `/api/auth/login` | Authenticate & receive JWT token |

### 2. Contracts & Document Management
| Method | Endpoint | Description |
| :--- | :--- | :--- |
| `POST` | `/api/contracts/upload` | Multipart file upload (`PDF`/`DOCX`/`TXT`) + metadata |
| `GET` | `/api/contracts` | List user's contracts (supports `?search=` and `?status=`) |
| `GET` | `/api/contracts/{id}` | Get full contract details & extracted `parsedText` |
| `PATCH` | `/api/contracts/{id}/status` | Update contract lifecycle status (enforces state machine) |
| `GET` | `/api/contracts/{id}/download` | Stream original physical document from server disk |
| `DELETE`| `/api/contracts/{id}` | Delete contract record and physical file |

### 3. AI Q&A, Search & Conversational Memory
| Method | Endpoint | Description |
| :--- | :--- | :--- |
| `POST` | `/api/contracts/{id}/ask` | Ask question on a contract (returns answer + clause citations) |
| `POST` | `/api/contracts/{id}/search?keyword=term` | Fast keyword chunk search in contract text |
| `GET` | `/api/contracts/{id}/chat-history` | Fetch 10-message rolling chat history for contract |
| `POST` | `/api/contracts/global/ask` | Ask question across **all** portfolio contracts |
| `GET` | `/api/contracts/global/chat-history` | Fetch global portfolio chat history |

---

## 💻 How to Run Locally

### Prerequisites
* **Java 21 LTS** installed (`java -version`)
* **PostgreSQL** running locally on port `5432`
* **Maven Wrapper** (included as `./mvnw` / `mvnw.cmd`)

### Step 1: Create Database
Open PostgreSQL (pgAdmin or `psql`) and run:
```sql
CREATE DATABASE contract_story_dev;
```

### Step 2: Configure Properties
Check `src/main/resources/application.properties` to ensure your database credentials match:
```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/contract_story_dev
spring.datasource.username=postgres
spring.datasource.password=root@123
```

### Step 3: Run the Application
Open a terminal in the project root:
```powershell
# Windows
.\mvnw.cmd spring-boot:run

# Linux / macOS
./mvnw spring-boot:run
```
The server will start at **`http://localhost:8080`**.

---

## 🧪 Testing the APIs

Two options are provided to test all endpoints out-of-the-box:

### Option A: VS Code / IntelliJ REST Client (`request.http`)
Open [request.http](request.http) in your IDE. It contains **12 pre-built, numbered requests**:
1. Health check
2. Register User
3. Login User (captures auth token)
4. Upload sample contract
5. List contracts
6. Get contract details
7. State Machine: Valid transition (`DRAFT` ➔ `UNDER_REVIEW`)
8. State Machine: Valid transition (`UNDER_REVIEW` ➔ `APPROVED`)
9. State Machine: Forbidden transition (`APPROVED` ➔ `DRAFT` triggering `400 Bad Request`)
10. Download file
11. AI Question & Answer with citations
12. Global Portfolio Question & Answer

### Option B: Postman Collection
A complete Postman collection is included in the project root:  
📁 **`Contract_Story_Postman_Collection.json`**

1. Open Postman ➔ Click **Import**.
2. Select `Contract_Story_Postman_Collection.json`.
3. Run the **Login** request—it automatically stores `authToken` so all subsequent requests run without manual copy-pasting.

---

## 📦 Project Structure

```
contract-backend/
├── src/main/java/com/seventhray/contract_backend/
│   ├── controller/
│   │   ├── AuthController.java           # Login & Register endpoints
│   │   ├── ContractController.java       # Contract CRUD & lifecycle
│   │   └── QAController.java             # Q&A, citations & chat history
│   ├── dto/
│   │   ├── auth/                         # LoginRequest, RegisterRequest, AuthResponse
│   │   ├── contract/                     # ContractResponse, StatusUpdateRequest, SearchResponse
│   │   ├── qa/                           # QuestionRequest, QuestionResponse, ChatMessageResponse
│   │   └── llm/                          # LlmChatRequest, LlmChatResponse
│   ├── exception/
│   │   ├── GlobalExceptionHandler.java   # Centralized error handler returning clean JSON
│   │   ├── BadRequestException.java      # Triggers 400 (validation & state machine)
│   │   ├── ResourceNotFoundException.java# Triggers 404 (missing records)
│   │   └── FileStorageException.java     # Triggers 500 (disk I/O errors)
│   ├── model/
│   │   ├── User.java                     # User JPA entity
│   │   ├── Role.java                     # Role enum (USER, ADMIN)
│   │   ├── Contract.java                 # Contract JPA entity
│   │   ├── ContractStatus.java           # State machine logic (DRAFT, UNDER_REVIEW, APPROVED, REJECTED)
│   │   ├── ContractType.java             # Contract categorization enum
│   │   └── ContractChat.java             # Conversational memory JPA entity
│   ├── repository/
│   │   ├── UserRepository.java
│   │   ├── ContractRepository.java       # Custom multi-tenant & portfolio queries
│   │   └── ContractChatRepository.java   # Rolling context queries
│   ├── security/
│   │   ├── JwtUtil.java                  # Token generation & validation
│   │   ├── JwtAuthenticationFilter.java  # Per-request bearer token inspection
│   │   ├── CustomUserDetailsService.java # User loader
│   │   └── SecurityConfig.java           # Stateless security filter chain & CORS
│   └── service/
│       ├── AuthService.java & AuthServiceImpl.java
│       ├── ContractService.java & ContractServiceImpl.java
│       ├── QAService.java & QAServiceImpl.java
│       ├── parser/DocumentParserService.java  # PDFBox & POI document parser
│       └── llm/LlmService.java & LlmServiceImpl.java # OpenAI-compatible RAG client
├── uploads/                              # Uploaded contracts partitioned by user
├── request.http                          # 12 ready-to-run REST Client tests
├── Contract_Story_Postman_Collection.json# Importable Postman collection
├── pom.xml                               # Dependencies (Java 21, Spring Boot 3.4.2)
└── README.md                             # Documentation
```

---

## 🎯 Candidate Note

This backend was built strictly adhering to the requirements of the **Contract Story** internship assignment:
* **Clean Code & Modularity**: Controller ➔ Service ➔ Repository separation with strict DTO usage.
* **Production-Grade Error Handling**: Zero raw stack traces exposed to clients; all errors return standardized JSON responses.
* **Real Document Parsing**: Functional Apache PDFBox and POI implementations handling both `.pdf` and `.docx` formats.
* **Defensive Lifecycle Design**: Finite state machine preventing illegal status transitions.
* **Intelligent Q&A Architecture**: RAG pipeline designed to provide immediate clause citations and smart contextual memory.
