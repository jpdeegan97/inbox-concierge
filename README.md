# 📨 Inbox Concierge

Inbox Concierge is an AI-powered smart email classifier that securely reads recent emails from your Gmail inbox and intelligently routes them into structured buckets using OpenAI's `gpt-4o-mini` foundation model.

## 🏗️ Architecture

- **Backend:** Java Spring Boot (v3.x / Java 21)
- **Frontend:** React + Vite + TypeScript + Tailwind CSS
- **Datastore:** H2 In-Memory DB (Postgres schemas provided) & Redis Layer
- **Integrations:** Gmail OAuth2 API & OpenAI Chat Completions API

## 📋 Prerequisites

Before running the application, ensure your environment has the following active:
1. **Java 21**
2. **Node.js** (v18+)
3. **Redis** (Bound to port `6379`)
4. **OpenAI API Key** bound to your environment variables (or Spring defaults to a mocked fallback if absent).

---

## 🚀 Running the Application

### 1. Boot up Redis
The Spring Boot classification engine strictly requires a highly-available Redis datastore to intelligently cache evaluated email mappings to prevent rate-limiting and quota exhaustion on the OpenAI platform. 

Start Redis easily using Docker:
```bash
docker run --name redis-concierge -p 6379:6379 -d redis
```

### 2. Start the Spring Boot Backend (API)
Open a terminal, navigate into the service directory, and run the Maven wrapper:
```bash
cd "services/api"
sh mvnw spring-boot:run
```
*The backend server will successfully initialize and host the API over `http://localhost:8080`.*

### 3. Start the React Frontend (UI)
Open a new terminal tab, navigate into the web directory, and spin up the Vite development server:
```bash
cd "apps/web"
npm install
npm run dev
```
*The frontend will launch and serve the user interface—generally mapping to `http://localhost:5198` (check your terminal output for your exact port).*

---

## 🧪 Testing the Classification Flow

1. Click **Log In With Google** on the frontend to grant the Concierge read-only execution privileges over your secure Inbox snippets.
2. Observe the initial fetch of emails. The platform will bulk-poll the 25 most recent emails to evaluate.
3. The platform leverages parallel streams throttling (5 concurrent jobs bounded by a 150ms token restock rate) to securely classify the email headers and snippets.
4. **Create a Custom Bucket:** Locate the inputs to create a custom bucket (e.g. "Receipts/Bills").
5. Observe the LLM dynamically invalidate the Redis cache mappings specifically for your custom categories, evaluating the 25 emails entirely over again to map into your newly added custom architecture.

## 🗄️ Database Schemas (Optional PostgreSQL)
By default, the backend relies on an auto-cleaning H2 In-Memory instance. If you prefer persistent bucket mappings, the migration schema is exported for your use locally in `services/api/src/main/resources/schema-postgres.sql`. 
Simply hook up the `.yml` URL configuration into your external Postgres driver when ready.
