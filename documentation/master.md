Gmail Bucket Classifier Capstone — Standardized Documentation Pack v0

Project Code: NS-INBOX-CAPSTONEWorking Name: Inbox ConciergeCandidate Project Type: Interview CapstoneInterface: React Web AppPrimary Integration: Google OAuth + Gmail APICore Capability: LLM-powered classification of the user’s latest Gmail threads into default and user-defined buckets

000 — CHARTER

Mission

Build a React-based Gmail intelligence interface that allows a user to authenticate a Google Workspace / G-Suite Gmail account, retrieve the user’s latest 200 Gmail threads, classify them into useful email-management buckets using an LLM-powered pipeline, and allow the user to define custom buckets that trigger reclassification across the same thread set.

Capstone Goal

Demonstrate practical product engineering judgment across frontend architecture, OAuth integration, Gmail data access, LLM workflow design, classification explainability, state management, and user-centered interaction design.

Primary User Value

The product helps users reduce inbox overload by automatically organizing recent Gmail conversations into clear action-oriented categories such as Important, Can Wait, Auto-Archive, Newsletter, and custom user-defined categories.

Success Criteria

A successful implementation should:

Authenticate the user with Google OAuth.

Request Gmail read access using appropriate scopes.

Load the latest 200 Gmail threads.

Display each thread using subject line and preview text.

Classify threads into default buckets using an LLM-powered pipeline.

Allow users to create custom buckets.

Reclassify all loaded threads when buckets change.

Preserve a simple, responsive, email-client-like interface.

Clearly separate integration, classification, and UI state concerns.

Demonstrate thoughtful tradeoffs around privacy, latency, cost, and reliability.

Non-Goals

The project does not need to:

Send emails.

Modify Gmail labels.

Archive emails in the user’s actual Gmail account.

Support clicking into full email details.

Support production multi-tenant deployment.

Store long-term email content unless explicitly needed for the demo.

Build a mobile app, since this implementation will use React web.

001 — OVERVIEW

Product Summary

Inbox Concierge is a React web application that connects to a user’s Gmail account and organizes their most recent email threads into intelligent buckets. The initial experience loads default buckets, classifies the latest 200 Gmail threads, and renders a simplified inbox grouped by category.

The distinguishing feature is bucket customization. A user can create a new bucket such as “Recruiters,” “Bills,” “Customer Issues,” “Family,” or “Needs Reply Today.” Once the new bucket is added, the application reruns classification across the already-loaded thread set and redistributes emails into the updated category taxonomy.

Core Flow

User opens the app.

User signs in with Google.

App requests Gmail read permission.

Backend retrieves the latest 200 Gmail threads.

Backend normalizes each thread into a compact classification object.

LLM classifier assigns each thread to one bucket.

Frontend displays grouped email cards.

User creates a new custom bucket.

Backend reruns classification with the new bucket list.

Frontend updates grouped results.

Recommended Architecture Direction

Use a React frontend with a Java Spring Boot backend service. The frontend should not directly own Gmail tokens or call the LLM provider from the browser. The Spring Boot backend should handle OAuth callback exchange, Gmail API calls, thread normalization, LLM classification orchestration, Redis-backed job/progress state, Postgres persistence, and response shaping.

Suggested Stack

Layer

Recommended Choice

Rationale

Frontend

React + TypeScript + Vite

Fast implementation, strong typing, simple local dev

Styling

Tailwind CSS

Rapid clean UI implementation

Backend

Java Spring Boot

Best fit for candidate background, production-grade API design, OAuth handling, typed workflow orchestration, and interview differentiation

Auth

Spring Security OAuth2 Client + Google OAuth 2.0

Secure Google authentication and Gmail authorization

Email API

Gmail API Java Client

Retrieves threads/messages from the authenticated account

LLM

OpenAI API or equivalent

Classification pipeline

Primary Database

Postgres

Source of truth for users, buckets, classification runs, and results

Cache / Queue / Progress

Redis

Job state, progress tracking, short-lived thread cache, and reclassification locks

Document Store

MongoDB optional

Useful for raw Gmail payloads, normalized document snapshots, prompt packets, and LLM trace artifacts

Frontend Server State

TanStack Query

Clean loading, mutation, retry, and cache handling on the React side

002 — TAXONOMY

Primary Domain Objects

Object

Definition

User

Authenticated Google account holder

Gmail Thread

A Gmail conversation thread returned by the Gmail API

Email Preview

Normalized display object containing subject and preview/snippet

Bucket

A category used to group threads

Default Bucket

A bucket predefined by the application

Custom Bucket

A user-created bucket added during the session

Classification Run

One execution of the classification pipeline across loaded threads

Classification Result

Mapping of a thread to a bucket with optional confidence/reason

Default Buckets

Bucket

Purpose

Important

Messages that likely require attention, response, or action soon

Can Wait

Messages that may be relevant but are not urgent

Auto-Archive

Low-value messages that likely do not need user attention

Newsletter

Marketing, subscription, digest, or publication-style emails

Transactional

Receipts, confirmations, account alerts, shipping, billing notices

Personal

Friends, family, social, non-work personal communication

Work / Professional

Professional correspondence, job-related messages, colleagues, vendors

Needs Review

Ambiguous messages that should not be confidently hidden

Bucket Design Rules

Each bucket should include:

id — stable internal identifier.

name — user-visible name.

description — classification guidance.

priority — optional ordering weight.

isDefault — boolean marker.

createdByUser — boolean marker.

Example Custom Buckets

Custom Bucket

Example Use

Recruiters

Job opportunities, interview scheduling, hiring messages

Bills

Utility bills, credit cards, loan notices, payment reminders

Family

Family-related personal communication

Customer Issues

Escalations, complaints, urgent support needs

Legal / Compliance

Contracts, compliance requests, policy-related communication

003 — ARCHITECTURE

System Shape

The application should be structured as a three-part system:

React Client — owns the user interface, bucket editing experience, grouped inbox rendering, and interaction state.

Java Spring Boot Backend — owns OAuth exchange, Gmail access, normalization, classification orchestration, persistence, Redis-backed job state, and API responses.

Data Layer — Postgres as source of truth, Redis for transient workflow state, and optional MongoDB for document/evidence storage.

External Services — Google OAuth, Gmail API, and the selected LLM provider.

Logical Architecture

flowchart LR
    U[User] --> FE[React Web App]
    FE --> AUTH[Google OAuth Flow]
    AUTH --> BE[Spring Boot Backend API]
    BE --> PG[(Postgres)]
    BE --> REDIS[(Redis)]
    BE --> MONGO[(MongoDB Optional)]
    BE --> GMAIL[Gmail API]
    BE --> NORM[Thread Normalizer]
    NORM --> CLS[LLM Classification Pipeline]
    CLS --> PG
    CLS --> API[Grouped Inbox Response]
    API --> FE
    FE --> CB[Create Custom Bucket]
    CB --> BE
    BE --> CLS
    CLS --> API

Backend Responsibilities

Responsibility

Description

OAuth Handling

Initiate auth, receive callback, exchange code, manage access token

Gmail Retrieval

Fetch latest 200 threads and minimal message metadata

Normalization

Convert Gmail API payloads into compact classification records

Classification

Send structured batch prompts or smaller chunks to LLM

Reclassification

Re-run classification when bucket definitions change

Response Shaping

Return grouped thread previews to frontend

Frontend Responsibilities

Responsibility

Description

Auth CTA

Show Google sign-in/connect state

Loading State

Indicate thread retrieval/classification progress

Inbox View

Display bucket columns/sections with subject and preview

Bucket Creation

Allow user to add new bucket name and description

Reclassification Trigger

Submit updated bucket taxonomy to backend

Error Handling

Display auth, Gmail, and classifier errors clearly

Recommended Repository Layout

inbox-concierge/
  apps/
    web/
      src/
        components/
        pages/
        hooks/
        api/
        state/
        types/
  services/
    api/
      src/main/java/com/northfield/inboxconcierge/
        auth/
        gmail/
        classifier/
        buckets/
        threads/
        jobs/
        users/
        persistence/
        config/
      src/main/resources/
        application.yml
  infra/
    docker-compose.yml
    postgres/
    redis/
    mongo/
  docs/
  README.md

004 — LIFECYCLE

Application Lifecycle

sequenceDiagram
    participant User
    participant Web as React App
    participant API as Backend API
    participant Google as Google OAuth/Gmail
    participant LLM as LLM Provider

    User->>Web: Open app
    Web->>User: Show connect Gmail button
    User->>Web: Click connect
    Web->>Google: Redirect to Google OAuth
    Google->>API: OAuth callback with code
    API->>Google: Exchange code for token
    API->>Google: Fetch latest 200 Gmail threads
    Google->>API: Return thread/message data
    API->>API: Normalize thread previews
    API->>LLM: Classify normalized threads into buckets
    LLM->>API: Return structured classifications
    API->>Web: Return grouped inbox
    Web->>User: Display bucketed email previews
    User->>Web: Create custom bucket
    Web->>API: Submit new bucket taxonomy
    API->>LLM: Reclassify same loaded threads
    LLM->>API: Return updated classifications
    API->>Web: Return updated grouped inbox

Initial Load Lifecycle

Validate user authentication state.

If unauthenticated, show connect screen.

If authenticated, request latest 200 threads.

Fetch thread IDs from Gmail.

Retrieve necessary thread/message metadata.

Normalize subject, sender, date, snippet, labels, and participants.

Classify normalized threads against default buckets.

Group classification results by bucket.

Render grouped inbox.

Custom Bucket Lifecycle

User enters bucket name.

User optionally enters bucket description.

Frontend validates duplicate or empty bucket names.

Frontend sends full bucket list to backend.

Backend validates bucket taxonomy.

Backend reruns classification over the existing normalized thread set.

Backend returns updated grouped result.

Frontend updates the inbox view.

Reclassification Rule

Classification should always run against the complete current bucket taxonomy. It should not simply search for emails matching the new bucket. This ensures that a new bucket can pull messages away from existing categories when it is a better fit.

005 — DECISION

Decision 001 — Use React Web Instead of Expo

Decision: Build a React web interface.Reasoning: The instructions explicitly allow React for web, and a web app is faster for OAuth callback handling, Gmail display, and interview demo flow.

Decision 002 — Use a Backend for Gmail and LLM Access

Decision: Do not call Gmail or LLM providers directly from the frontend.Reasoning: OAuth tokens, refresh tokens, LLM API keys, and classification orchestration should remain server-side.

Decision 003 — Classify Threads, Not Individual Messages

Decision: Use Gmail threads as the unit of classification.Reasoning: The prompt asks for the last 200 threads. Thread-level classification better matches how Gmail organizes conversations.

Decision 004 — Store Minimal Email Data

Decision: Store normalized thread previews only for the active session unless persistence is explicitly required.Reasoning: This reduces privacy risk and keeps the capstone implementation focused.

Decision 005 — Reclassify on Bucket Change

Decision: Adding a custom bucket triggers full reclassification of loaded threads.Reasoning: The prompt requires all emails to be recategorized based on the new buckets.

Decision 006 — Return Structured LLM Output

Decision: Require the LLM to return strict JSON with thread ID, bucket ID, confidence, and optional reason.Reasoning: Structured output improves reliability, debuggability, and UI integration.

006 — VERSION

Version Plan

Version

Scope

v0

Documentation, architecture, data model, classification strategy

v1

Local React UI with mocked Gmail data and mocked classifications

v2

Google OAuth and Gmail retrieval integration

v3

Real LLM classification pipeline

v4

Custom bucket creation and full reclassification

v5

Polish, error states, performance improvements, demo script

MVP Definition

The MVP should include:

Google sign-in.

Gmail thread retrieval.

Display of latest thread previews.

Default bucket classification.

Custom bucket creation.

Reclassification after bucket update.

Basic loading and error states.

Demo-Ready Definition

The demo-ready version should include:

Clean landing/auth screen.

Obvious grouped inbox layout.

Visible bucket counts.

Add bucket modal or side panel.

Reclassification progress indicator.

Sensible fallback behavior for uncertain classifications.

A short explanation panel describing the classification pipeline.

007 — DATAMODEL

User Session

type UserSession = {
  userId: string;
  email: string;
  displayName?: string;
  accessTokenRef: string;
  connectedAt: string;
};

Gmail Thread Preview

type GmailThreadPreview = {
  threadId: string;
  subject: string;
  preview: string;
  senderName?: string;
  senderEmail?: string;
  lastMessageAt?: string;
  gmailLabels?: string[];
  messageCount?: number;
};

Bucket

type Bucket = {
  id: string;
  name: string;
  description: string;
  priority?: number;
  isDefault: boolean;
  createdByUser: boolean;
};

Classification Result

type ClassificationResult = {
  threadId: string;
  bucketId: string;
  confidence: number;
  reason?: string;
};

Grouped Inbox Response

type GroupedInboxResponse = {
  buckets: Bucket[];
  groups: Array<{
    bucketId: string;
    bucketName: string;
    threads: Array<GmailThreadPreview & {
      classification: ClassificationResult;
    }>;
  }>;
  run: {
    runId: string;
    classifiedAt: string;
    model: string;
    totalThreads: number;
  };
};

008 — EEE

EEE stands for Entity, Event, and Effect. For this capstone, EEE clarifies how important system actions move through the product.

Entities

Entity

Description

User

Authenticated Gmail account owner

Thread

Gmail thread loaded from the user’s mailbox

Bucket

Classification destination

ClassificationRun

Batch classification execution

ClassificationResult

Per-thread category assignment

Events

Event

Trigger

Result

UserAuthenticated

OAuth callback succeeds

Session becomes active

ThreadsLoaded

Gmail returns latest 200 threads

Normalized thread set is available

ClassificationStarted

Thread set and buckets are ready

LLM pipeline begins

ClassificationCompleted

LLM returns valid structured output

UI receives grouped inbox

BucketCreated

User adds custom bucket

Bucket taxonomy changes

ReclassificationStarted

Bucket taxonomy changes

Full loaded thread set is reprocessed

ReclassificationCompleted

Updated classifications return

UI regrouping occurs

ClassificationFailed

LLM/API error occurs

UI displays recoverable error

Effects

Effect

Description

RenderGroupedInbox

Display thread previews under bucket headers

ShowBucketCounts

Display count of threads per bucket

PreserveLoadedThreads

Keep normalized thread set stable during reclassification

ShowProgress

Indicate classification or reclassification in progress

ShowFallbackBucket

Assign uncertain results to Needs Review

009 — IMPL

Implementation Strategy

Build in thin vertical slices:

Mock UI Slice — render fake buckets and fake email previews.

Bucket Management Slice — allow adding custom buckets locally.

Mock Classification Slice — simulate reclassification when bucket list changes.

OAuth Slice — connect Google account.

Gmail Retrieval Slice — load real latest 200 threads.

LLM Classification Slice — classify normalized real threads.

Demo Polish Slice — improve loading, error states, copy, and visual layout.

Backend API Endpoints

Method

Route

Purpose

GET

/auth/google/start

Start Google OAuth flow

GET

/auth/google/callback

Handle OAuth callback

GET

/api/session

Return current auth/session state

POST

/api/inbox/load

Start loading latest Gmail threads and classify them; may return a job ID

POST

/api/inbox/reclassify

Reclassify loaded threads with updated buckets; may return a job ID

GET

/api/jobs/{jobId}

Return Redis-backed job status and classification progress

GET

/api/inbox/current

Return the latest grouped inbox result for the authenticated user

GET

/api/buckets/defaults

Return default bucket taxonomy

LLM Classification Input

{
  "buckets": [
    {
      "id": "important",
      "name": "Important",
      "description": "Messages that require attention, response, or action soon."
    }
  ],
  "threads": [
    {
      "threadId": "abc123",
      "subject": "Interview follow-up",
      "preview": "Thanks for speaking with us today. We would like to schedule...",
      "senderEmail": "recruiter@example.com",
      "gmailLabels": ["INBOX", "IMPORTANT"]
    }
  ]
}

LLM Classification Output

{
  "results": [
    {
      "threadId": "abc123",
      "bucketId": "important",
      "confidence": 0.91,
      "reason": "The message appears to require timely action related to interview scheduling."
    }
  ]
}

Prompting Rules

The classifier should be instructed to:

Assign exactly one bucket per thread.

Use only the provided bucket IDs.

Prefer user-created buckets when they are a strong semantic fit.

Use Needs Review for ambiguous or risky cases.

Avoid inventing new categories.

Return strict JSON.

Use subject, preview, sender, labels, and recency as signals.

Batching Strategy

For 200 threads, use either:

One structured batch if token limits allow.

Multiple batches of 25–50 threads with a merge step.

For the interview capstone, batching by 50 threads is a strong default because it demonstrates scalability while keeping implementation simple.

010 — FE

UI Principles

The interface should feel like a simplified email homepage:

Left or top bucket navigation.

Main grouped inbox area.

Bucket counts.

Subject line and preview text only.

Clear add-bucket action.

Minimal friction after authentication.

Core Components

Component

Responsibility

AuthGate

Shows sign-in state and connect Gmail CTA

InboxPage

Main page after authentication

BucketSidebar

Lists buckets and counts

BucketSection

Displays grouped emails for one bucket

EmailPreviewCard

Displays subject and preview

CreateBucketModal

Allows custom bucket creation

ClassificationStatusBar

Shows loading/reclassification status

ErrorBanner

Displays recoverable errors

Suggested Layout

flowchart TB
    A[Top Bar: Inbox Concierge + Connected Account]
    B[Status Bar: Loaded 200 threads / Classified timestamp]
    C[Add Custom Bucket Button]
    D[Bucket Sidebar with Counts]
    E[Grouped Inbox Sections]

    A --> B
    B --> C
    C --> D
    C --> E

Example Screen Structure

 ------------------------------------------------------
| Inbox Concierge                         user@domain  |
 ------------------------------------------------------
| Classified 200 threads · Last run 10:42 AM            |
 ------------------------------------------------------
| Buckets                 | Important                  |
| - Important (18)        | - Subject / Preview        |
| - Can Wait (43)         | - Subject / Preview        |
| - Newsletter (52)       |                            |
| - Auto-Archive (61)     | Can Wait                   |
| + Add Bucket            | - Subject / Preview        |
 ------------------------------------------------------

Frontend State

State

Owner

Auth/session state

Server + React Query

Bucket list

React local state or server response

Loaded inbox result

React Query

Create bucket modal state

React component state

Classification loading status

React Query mutation state

Error banners

Local UI state

011 — APIMAP

GET /api/session

Returns current user session state.

{
  "authenticated": true,
  "user": {
    "email": "user@example.com",
    "displayName": "Jane User"
  }
}

POST /api/inbox/load

Loads latest Gmail threads and classifies them against default buckets.

Request:

{
  "limit": 200
}

Response:

{
  "buckets": [],
  "groups": [],
  "run": {
    "runId": "run_123",
    "classifiedAt": "2026-05-11T14:00:00Z",
    "model": "gpt-classifier",
    "totalThreads": 200
  }
}

POST /api/inbox/reclassify

Reclassifies the loaded thread set using the updated bucket list.

Request:

{
  "buckets": [
    {
      "id": "important",
      "name": "Important",
      "description": "Messages that require attention, response, or action soon.",
      "isDefault": true,
      "createdByUser": false
    },
    {
      "id": "recruiters",
      "name": "Recruiters",
      "description": "Messages from recruiters, hiring teams, or interview coordinators.",
      "isDefault": false,
      "createdByUser": true
    }
  ]
}

Response:

{
  "buckets": [],
  "groups": [],
  "run": {
    "runId": "run_456",
    "classifiedAt": "2026-05-11T14:03:00Z",
    "model": "gpt-classifier",
    "totalThreads": 200
  }
}

Error Response Shape

{
  "error": {
    "code": "CLASSIFICATION_FAILED",
    "message": "Unable to classify the current inbox. Please retry.",
    "recoverable": true
  }
}

012 — STATE

Inbox State Machine

stateDiagram-v2
    [*] --> Unauthenticated
    Unauthenticated --> Authenticating: Connect Gmail
    Authenticating --> Authenticated: OAuth success
    Authenticating --> AuthError: OAuth failure
    Authenticated --> LoadingThreads: Load inbox
    LoadingThreads --> Classifying: Threads loaded
    LoadingThreads --> LoadError: Gmail error
    Classifying --> Ready: Classification success
    Classifying --> ClassificationError: LLM error
    Ready --> Reclassifying: User adds bucket
    Reclassifying --> Ready: Reclassification success
    Reclassifying --> ClassificationError: LLM error
    AuthError --> Unauthenticated: Retry
    LoadError --> Authenticated: Retry
    ClassificationError --> Ready: Retry or fallback

Important State Transitions

From

To

Trigger

Unauthenticated

Authenticating

User clicks connect Gmail

Authenticating

Authenticated

OAuth callback succeeds

Authenticated

LoadingThreads

App requests latest threads

LoadingThreads

Classifying

Threads normalize successfully

Classifying

Ready

LLM returns valid classifications

Ready

Reclassifying

User creates custom bucket

Reclassifying

Ready

Updated classifications return

Fallback State Behavior

If classification partially fails, the backend can preserve successfully classified results and place failed/ambiguous threads in Needs Review. For a capstone, this demonstrates thoughtful reliability design without overcomplicating the implementation.

013 — RUNBOOK

Local Setup

Create Google Cloud project.

Enable Gmail API.

Configure OAuth consent screen.

Create OAuth client credentials for a web app.

Add local redirect URI, such as http://localhost:3001/auth/google/callback.

Add required environment variables.

Start backend.

Start React frontend.

Environment Variables

GOOGLE_CLIENT_ID=
GOOGLE_CLIENT_SECRET=
GOOGLE_REDIRECT_URI=http://localhost:8080/login/oauth2/code/google
LLM_API_KEY=
SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/inbox_concierge
SPRING_DATASOURCE_USERNAME=inbox
SPRING_DATASOURCE_PASSWORD=inbox
SPRING_DATA_REDIS_HOST=localhost
SPRING_DATA_REDIS_PORT=6379
SPRING_DATA_MONGODB_URI=mongodb://localhost:27017/inbox_concierge
FRONTEND_URL=http://localhost:5198
SERVER_PORT=8080

Gmail Scope Recommendation

Use the narrowest practical read-only scope for the capstone:

https://www.googleapis.com/auth/gmail.readonly

Demo Script

Open the app.

Click “Connect Gmail.”

Complete Google OAuth.

Show loading state while latest 200 threads are retrieved.

Show default bucket classification results.

Point out bucket counts and email previews.

Add a custom bucket, such as “Recruiters.”

Trigger reclassification.

Show that emails moved into the new bucket.

Explain the classification pipeline and privacy posture.

Risk Controls

Risk

Control

OAuth friction

Use clear redirect URI setup and test user configuration

Gmail payload size

Fetch only needed metadata and snippets

LLM latency

Batch threads and show progress UI

LLM invalid JSON

Use schema validation and retry once

Sensitive content exposure

Avoid full-body storage and use read-only scope

Overclassification

Use Needs Review for ambiguous threads

014 — DATADEF

Normalized Thread Definition

A normalized thread is the minimized representation of a Gmail thread used for display and classification.

{
  "threadId": "thread_001",
  "subject": "Following up on our interview",
  "preview": "Thanks again for taking the time to speak with us. We'd like to move forward...",
  "senderName": "Alex Recruiter",
  "senderEmail": "alex@example.com",
  "lastMessageAt": "2026-05-11T13:45:00Z",
  "gmailLabels": ["INBOX", "IMPORTANT"],
  "messageCount": 3
}

Bucket Definition

{
  "id": "important",
  "name": "Important",
  "description": "Messages that likely require attention, response, or action soon.",
  "priority": 1,
  "isDefault": true,
  "createdByUser": false
}

Classification Definition

{
  "threadId": "thread_001",
  "bucketId": "important",
  "confidence": 0.92,
  "reason": "The message appears to be related to interview progression and likely requires timely attention."
}

Group Definition

{
  "bucketId": "important",
  "bucketName": "Important",
  "threads": [
    {
      "threadId": "thread_001",
      "subject": "Following up on our interview",
      "preview": "Thanks again for taking the time to speak with us...",
      "classification": {
        "threadId": "thread_001",
        "bucketId": "important",
        "confidence": 0.92
      }
    }
  ]
}

Appendix A — Interview Positioning Notes

What This Project Demonstrates

This project demonstrates:

Product judgment around inbox overload.

Secure integration with Google OAuth and Gmail.

Thoughtful LLM pipeline design.

Structured output and schema validation.

Strong frontend state management.

Human-centered custom categorization.

Practical tradeoffs around privacy, cost, and latency.

Ability to turn ambiguous instructions into a clean product architecture.

Suggested Interview Framing

“I treated the project less like a toy classifier and more like a small workflow product. The key design choice was to separate Gmail retrieval, thread normalization, bucket taxonomy, and LLM classification into explicit stages. That makes the system easier to test, easier to debug, and easier to extend when a user adds their own buckets.”

Strong Differentiator

The strongest differentiator is the reclassification flow. Many implementations would append a new category and only classify future emails. This design intentionally reruns the full loaded thread set against the updated taxonomy, which better matches the user’s mental model and the project requirement.

