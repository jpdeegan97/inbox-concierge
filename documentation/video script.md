Inbox Concierge Capstone Video Script v0

Target Length: 10–20 minutesRecommended Delivery: 14–16 minutesProject: Inbox Concierge — LLM-powered Gmail thread classification appStack: React + TypeScript, Java Spring Boot, Postgres, Redis, optional MongoDB, Google OAuth/Gmail API, LLM classification pipeline

0. Opening — 30 to 60 seconds

Hi, my name is John Deegan, and this is my capstone project: Inbox Concierge.

Inbox Concierge is a React and Java Spring Boot application that connects to a user’s Gmail account, loads their latest 200 Gmail threads, and uses an LLM-powered classification pipeline to organize those threads into useful buckets like Important, Can Wait, Newsletter, Auto-Archive, Transactional, and Needs Review.

The key feature is that users are not limited to the default categories. They can create their own buckets — for example, Recruiters, Bills, Customer Issues, Family, or Legal — and the system will reclassify the full set of loaded emails based on the updated bucket taxonomy.

The reason I built it this way is because email overload is not just a volume problem. It is a prioritization problem. A user does not only need to know what arrived. They need to know what matters, what can wait, what is noise, and what fits into their personal workflow.

In this walkthrough, I’ll cover five things:

A product demo of what I built, why I built it, and the business impact.

The documentation process I used before implementation.

The tech stack I selected and why.

The major architectural decisions behind the system.

The technical trade-offs I made, including what I would do next to productionize and improve it.

1. Product Demo — 4 to 7 minutes

1.1 Product Problem

The problem I focused on is that inboxes are increasingly difficult to triage manually.

Most users receive a mixture of urgent work messages, personal emails, newsletters, receipts, product updates, marketing emails, automated alerts, and long-tail messages that may or may not matter. Traditional email clients already provide labels, search, and basic filters, but those systems usually require the user to manually define rules in advance.

That works well for deterministic patterns, like emails from a specific sender, but it does not work as well when the user’s intent is semantic. For example, a user may want to separate “messages that require my attention today” from “messages that are useful but not urgent.” That type of classification requires context.

So the goal of Inbox Concierge is to create a lightweight intelligent triage layer on top of Gmail.

The user connects their Gmail account, the app loads the latest 200 threads, and the system classifies those threads into clear action-oriented buckets.

1.2 Authentication and Gmail Connection

The first thing the user sees is a simple connection screen.

The call to action is to connect a Google Workspace or Gmail account. When the user clicks the connect button, the app starts a Google OAuth flow. The backend requests Gmail read-only access, so the system can retrieve threads but cannot send, delete, or modify emails.

I intentionally kept the scope narrow because this product only needs to read email metadata and snippets for classification. It does not need to mutate the user’s mailbox.

Once the OAuth flow succeeds, the user is redirected back into the app and the backend starts loading the latest 200 Gmail threads.

1.3 Inbox Loading and Classification

After authentication, the app enters a loading and classification state.

At this stage, the backend retrieves the latest 200 threads from Gmail, normalizes the data into a compact thread preview object, and sends that normalized thread set into the classification pipeline.

The UI shows progress so the user understands that the system is working. In a production version, this progress can be backed by Redis job state, so the frontend can poll for classification status instead of waiting on one long synchronous request.

Once classification completes, the frontend displays an email-homepage-style interface.

Each bucket has a count. Under each bucket, the user can see email cards with subject lines and previews. I intentionally did not implement full email click-through because the project requirements only called for subject and preview display. The goal here is not to replace Gmail. The goal is to create an intelligent triage layer.

1.4 Default Buckets

The default buckets I chose are:

Important

Can Wait

Auto-Archive

Newsletter

Transactional

Personal

Work / Professional

Needs Review

These buckets were chosen to represent common inbox actions.

Important is for messages that likely require attention or response. Can Wait is for messages that are relevant but not urgent. Newsletter captures subscription-style content. Transactional captures receipts, confirmations, account notices, and shipping updates. Auto-Archive captures low-value messages that probably do not require the user’s attention. Needs Review is a safety bucket for ambiguous emails where the model should avoid being overly confident.

That last bucket is important because in a product like this, the cost of hiding an important email is much higher than the cost of asking the user to review something uncertain.

1.5 Custom Bucket Creation

The main interaction beyond the default inbox view is custom bucket creation.

A user can add a new bucket by entering a name and optional description. For example, they might create a bucket called “Recruiters” with the description “Messages from recruiters, hiring teams, interview coordinators, or job platforms.”

When the user saves that bucket, the system does not just apply it going forward. It reclassifies the entire loaded set of 200 threads against the updated bucket taxonomy.

That design is important.

If the user adds “Recruiters,” some messages that were previously classified as Important or Work / Professional may now move into Recruiters because the taxonomy has become more specific. The system treats bucket creation as a change in the classification model’s decision space, not just as a new empty folder.

This gives the user a more personalized inbox view immediately.

1.6 Business Impact

The business impact of this product is workflow compression.

Instead of forcing users to manually scan hundreds of threads, the system gives them an immediate map of what is urgent, what is informational, what is automated, and what is likely noise.

For individuals, this saves time and reduces cognitive load.

For teams, this pattern could extend into shared inboxes, customer support queues, recruiting pipelines, legal intake, finance operations, or executive assistant workflows.

The more general business value is that Inbox Concierge turns unstructured communication into structured workflow state.

Once emails are bucketed, downstream actions become possible. A system could summarize Important threads, draft replies, surface aging follow-ups, route customer issues, or create tasks. But the first step is classification, because classification creates the operating layer that future automation can build on.

2. Documentation Process — 1 to 2 minutes

Before implementation, I put together a standardized documentation pack for the project.

I like to do this before writing code because it forces the system to become explicit. Instead of jumping directly into components, endpoints, and database tables, I first define the product charter, the system boundaries, the core taxonomy, the architecture, the lifecycle, the data model, the API map, and the operational runbook.

For this project, I used a standardized 14-section documentation structure that I commonly use for system planning.

The sections are:

CHARTER

OVERVIEW

TAXONOMY

ARCHITECTURE

LIFECYCLE

DECISION

VERSION

DATAMODEL

EEE, which stands for Entity, Event, and Effect

IMPL

FE

APIMAP

STATE

RUNBOOK

DATADEF

The reason I call this a standardized documentation pack is that each section answers a different implementation question.

The CHARTER explains the mission and non-goals. The ARCHITECTURE section explains the system shape. The LIFECYCLE section explains how the user and backend workflows move from authentication to classification to reclassification. The DECISION section records why certain choices were made. The DATAMODEL and APIMAP sections define the contracts. The STATE section captures the state machine. The RUNBOOK explains how the system is configured and operated.

This matters because the project involves several moving parts: Google OAuth, Gmail access, LLM classification, custom bucket management, reclassification, persistence, and frontend rendering. Without documentation, it would be easy for the implementation to become a collection of disconnected features.

The documentation made the implementation path much clearer. It let me define what the MVP should include, what should be deferred, how to explain the system in an interview, and how to avoid overbuilding.

I also used Antigravity as part of my development tooling workflow. The way I think about Antigravity in this project is as an AI-assisted IDE environment for moving from architecture and documentation into implementation. The documentation pack acts as the source of truth, and Antigravity helps accelerate the coding workflow by keeping the implementation aligned with the planned components, APIs, and service boundaries.

So the development flow was not just: open an editor and start building. It was: define the system, document the decisions, map the implementation, and then use tooling like Antigravity to move faster while staying aligned with the architecture.

3. Tech Stack — 3 to 5 minutes

For the tech stack, I chose a React frontend with a Java Spring Boot backend, Postgres as the primary database, Redis for cache and job state, and MongoDB as an optional evidence/document store.

2.1 Frontend: React, TypeScript, Vite, Tailwind

On the frontend, I chose React with TypeScript.

React was the natural choice because the instructions specifically asked for React if building a web interface. It is also a strong fit for this app because the UI is state-driven: authentication state, loading state, bucket state, classification results, and reclassification progress all need to be reflected clearly.

TypeScript helps keep the frontend aligned with the backend contract. Objects like Bucket, GmailThreadPreview, ClassificationResult, and GroupedInboxResponse can be represented with explicit types.

I would use Vite for fast local development and Tailwind CSS for quickly building a clean, responsive interface.

For server state, I would use TanStack Query because the frontend is mostly interacting with backend resources: session status, inbox load, job progress, bucket mutations, and grouped results. TanStack Query gives me loading states, retry behavior, caching, and mutation handling without overcomplicating the client.

2.2 Backend: Java Spring Boot

For the backend, I chose Java Spring Boot.

I picked Spring Boot because the backend has real responsibilities beyond simply forwarding requests. It owns OAuth integration, Gmail API access, normalized thread modeling, LLM classification orchestration, persistence, job status, and error handling.

Spring Boot is a strong fit for that because it gives me mature support for dependency injection, structured service boundaries, validation, security, configuration management, persistence, and testability.

It also aligns well with how I think about production systems. I can separate the backend into clear packages such as auth, Gmail, classifier, buckets, threads, jobs, persistence, and config.

That separation makes the system easier to reason about and easier to extend.

2.3 Postgres: Source of Truth

Postgres is the primary database and source of truth.

I would use Postgres for users, Google account connections, bucket definitions, classification runs, Gmail thread metadata, and classification results.

The important modeling point is classification history.

A thread may be classified differently across different runs. For example, before the user creates a Recruiters bucket, a message may be classified as Work / Professional. After the Recruiters bucket exists, the same message may be classified as Recruiters.

That means classification should not simply overwrite a field on the thread. Instead, the system should model classification runs and classification results.

That gives us traceability over time.

2.4 Redis: Workflow State, Performance, and Backpressure

Redis is used for short-lived workflow state.

The most important Redis use case is classification progress.

Classifying 200 threads with an LLM can take a few seconds, especially if the work is batched. I do not want the frontend to depend entirely on one long blocking request.

Instead, the backend can create a classification job, store progress in Redis, and allow the frontend to poll a job-status endpoint.

I am also adding semaphore and token-bucket choking controls around the expensive parts of the system. The goal is to prevent one user action, like repeatedly triggering reclassification, from overwhelming the Gmail API, the LLM provider, or the backend worker pool.

The semaphore limits concurrency. For example, the system may only allow a certain number of classification batches to run at once per user or globally. The token bucket controls rate over time. For example, a user may receive a fixed number of classification or reclassification tokens per time window, and each job consumes tokens based on size.

Redis is a good fit for this because these counters, locks, and progress records are short-lived distributed coordination state. Redis can support temporary thread caches, rate limiting, reclassification locks, job progress, and queue-like behavior if I move classification into background workers.

2.5 MongoDB: Optional Document/Evidence Store

MongoDB is optional in my design.

I would not make it mandatory for the MVP because Postgres can store flexible JSONB data when needed. However, MongoDB becomes useful if the system evolves into a richer evidence-tracking product.

For example, MongoDB could store raw Gmail payload snapshots, normalized LLM input packets, prompt/response traces, and classification evidence artifacts.

So my practical view is: Postgres and Redis are core. MongoDB is a v2 addition if the product needs document-style auditability or deeper debugging of classification behavior.

2.6 External APIs

The two major external APIs are Gmail and the LLM provider.

Gmail provides the thread data. The LLM provider performs semantic classification.

The backend owns both integrations so that access tokens and API keys are never exposed to the browser.

4. Architectural Decisions — 3 to 5 minutes

3.1 High-Level Design

At a high level, the system has four layers:

React frontend.

Spring Boot backend.

Data layer with Postgres, Redis, and optional MongoDB.

External services: Google OAuth, Gmail API, and LLM provider.

The frontend is responsible for interaction and display.

The backend is responsible for orchestration and security.

The data layer is responsible for persistence, job state, and optional evidence storage.

The external services provide identity, email data, and classification intelligence.

3.2 Why the Backend Owns OAuth and LLM Calls

One of the most important decisions was to keep sensitive integrations on the backend.

The frontend should not directly handle LLM API keys. It also should not own Gmail refresh tokens or long-lived Google credentials.

By putting OAuth handling and LLM access in Spring Boot, I can centralize security, token management, error handling, retries, and logging.

This also gives me a clean API boundary for the frontend. The frontend does not need to understand Gmail’s API shape or the LLM prompt structure. It just asks to load the inbox or reclassify the inbox.

3.3 Thread Normalization Layer

Another key decision was to normalize Gmail threads before classification.

The Gmail API returns data in a shape that is useful for Gmail, but not necessarily ideal for LLM classification or frontend rendering.

So I would convert each thread into a compact object with fields like thread ID, subject, preview, sender, timestamp, labels, and message count.

That normalized object becomes the canonical internal representation for classification.

This helps reduce token usage, limits unnecessary exposure of email content, and gives the LLM a cleaner input.

3.4 Classification Runs Instead of Static Labels

I chose to model classification as a run-based workflow.

That means each time the system classifies or reclassifies the loaded inbox, it creates a classification run. The results of that run map thread IDs to bucket IDs.

This is better than storing one current category directly on the thread because the classification depends on the bucket taxonomy at that time.

If the user adds a new bucket, the same email may legitimately move categories.

Run-based modeling lets the system preserve that history and makes the behavior easier to debug.

3.5 Full Reclassification After Bucket Changes

The most important product architecture decision is full reclassification when buckets change.

I did not want custom bucket creation to be a shallow filter.

When the user adds a new bucket, that bucket changes the meaning of the entire classification task. So the backend reruns the loaded thread set against the complete updated bucket list.

This gives better results and better matches the user’s expectation.

4.6 Semaphore and Token-Bucket Choking

Another architectural decision is adding explicit backpressure controls with semaphores and token buckets.

This matters because the system has several expensive or rate-limited operations: Gmail API retrieval, LLM classification, and full reclassification when the user changes the bucket taxonomy.

A naive implementation could let the user click reclassify repeatedly, or could launch too many LLM batches at once. That would create unnecessary cost, latency spikes, provider rate-limit errors, and a poor user experience.

So I would add two controls.

First, a semaphore controls concurrency. For example, only one active reclassification job should be allowed per user at a time. At the global level, the backend may also limit how many LLM batch calls are running concurrently across all users.

Second, a token bucket controls rate over time. For example, each user can have a certain number of classification tokens per minute or per hour. Loading 200 threads or triggering a reclassification consumes tokens. Tokens refill over time. If the bucket is empty, the backend can reject the request with a friendly retry message or queue it for later.

The reason I like this design is that it protects the system while preserving a good product experience. It is not just rate limiting at the HTTP edge. It is workflow-aware throttling around the operations that actually create load and cost.

Redis is the natural place to coordinate this because these locks, counters, and job states are short-lived and need to be shared across backend instances.

4.7 Needs Review as a Safety Valve

I included Needs Review as a default bucket because LLM classification should have a safe fallback.

In an email workflow, false negatives can be expensive. Accidentally burying a time-sensitive message is a bad user experience.

Needs Review gives the model a place to put ambiguous messages instead of forcing overconfident classification.

This is a simple design choice, but it improves trust.

5. Technical Trade-Offs and Production Improvements — 4 to 6 minutes

4.1 Trade-Off: Subject and Preview Only

One trade-off is that the MVP uses subject lines, snippets, sender information, labels, and lightweight metadata rather than full email bodies.

This is intentional.

The project requirements only require subject and preview display. For classification, snippets and metadata are often enough to produce useful triage. This also reduces token cost, improves latency, and limits the amount of sensitive email content sent to the LLM.

The downside is that some emails may require full-body context to classify correctly.

In a production version, I would introduce an adaptive retrieval strategy. The system would classify using lightweight previews first. If confidence is low, it could selectively retrieve more content for that thread and retry classification.

That would balance privacy, cost, and accuracy.

4.2 Trade-Off: Latest 200 Threads Only

Another trade-off is limiting the system to the latest 200 threads.

That matches the capstone requirement and keeps latency manageable.

In production, I would support pagination, background sync, incremental updates, and possibly Gmail push notifications. That would allow the system to maintain a continuously updated classification state rather than reloading a fixed batch each time.

4.3 Trade-Off: Classification Instead of Gmail Label Mutation

I chose not to modify the user’s actual Gmail labels.

The system creates an intelligent view over the inbox, but it does not archive, delete, label, or move emails in Gmail.

This is safer for an MVP because a classification mistake does not alter the user’s mailbox.

In production, I would add optional user-approved actions, such as “Apply Gmail labels,” “Archive selected Auto-Archive emails,” or “Create task from Important thread.” But those should require explicit consent and probably a separate Gmail scope.

4.4 Trade-Off: LLM Classification vs Rules Engine

Another trade-off is using an LLM for semantic classification instead of only using deterministic rules.

A rules engine is cheaper, faster, and more predictable. For example, newsletter detection can often be rule-based by looking at headers, senders, or unsubscribe signals.

But rules are less flexible when the category is semantic, such as “messages I should reply to today” or “recruiter outreach.”

So the best production version would be hybrid.

I would use deterministic signals where they are reliable, then use the LLM for ambiguous or semantic classification.

For example:

Gmail labels and headers can help identify newsletters.

Sender and domain patterns can help identify transactional messages.

The LLM can decide whether something is urgent, personal, professional, or custom-bucket relevant.

This hybrid approach would reduce cost and improve consistency.

4.5 Trade-Off: MongoDB Optional

I included MongoDB in the broader architecture but would not require it for the MVP.

The reason is that Postgres can already store structured relational data and flexible JSONB payloads.

Adding MongoDB too early could create operational complexity without enough benefit.

However, if I wanted full evidence tracing, prompt auditing, raw Gmail payload storage, or replayable classification experiments, MongoDB would become more attractive.

So the trade-off is simplicity now versus richer document storage later.

4.6 Production Improvements

If I were productionizing this system, I would focus on several areas.

First, I would harden OAuth and token storage. That means encrypted token storage, refresh-token lifecycle management, least-privilege scopes, and clear disconnect/revoke behavior.

Second, I would improve the classification pipeline. I would add schema validation, retries for invalid LLM output, confidence thresholds, deterministic pre-classifiers, semaphore-based concurrency controls, token-bucket rate controls, and evaluation datasets to measure classification quality.

Third, I would add background jobs. Classification and reclassification should run asynchronously, with Redis-backed progress tracking and eventually a real queue or worker model.

Fourth, I would improve privacy controls. Users should be able to understand what data is sent to the LLM, whether full bodies are used, and how long data is retained.

Fifth, I would add observability. I would track classification latency, error rates, token usage, model failures, bucket distribution, and user corrections.

Sixth, I would add feedback loops. If a user manually moves a thread from one bucket to another, that correction should become a signal for future classification.

Finally, I would consider Gmail write-back features, but only as opt-in functionality. For example, after the system classifies messages, the user could choose to apply labels or archive certain groups.

4.7 What I Would Improve Next

The next major improvement I would build is a user feedback and learning loop.

Right now, the system classifies based on the current bucket descriptions and message metadata. But over time, the user’s corrections are extremely valuable.

If the user repeatedly moves certain messages into Important, or repeatedly moves certain senders into Auto-Archive, the system should learn from that.

This could start with simple preference rules and eventually become a personalized ranking model.

The second improvement would be an explanation layer. Instead of only showing the assigned bucket, the system could show a short reason, such as “classified as Recruiters because the sender appears to be a hiring coordinator and the message mentions scheduling an interview.”

That would make the system more trustworthy.

6. Closing — 30 to 60 seconds

To summarize, Inbox Concierge is an intelligent Gmail triage application built with React and Java Spring Boot.

The core product idea is simple: help users turn email overload into structured workflow categories.

The most important technical choices were keeping sensitive integrations on the backend, normalizing Gmail threads before classification, modeling classification as a run-based workflow, and fully reclassifying the loaded inbox whenever the user adds custom buckets.

For the MVP, the system focuses on read-only Gmail access, subject and preview display, default bucket classification, and custom-bucket reclassification.

For production, I would harden OAuth, add background workers, improve evaluation and observability, introduce user feedback loops, and eventually support opt-in Gmail label or archive actions.

The broader value is that this project is not just sorting emails. It is converting unstructured communication into structured, user-specific workflow state — and that creates a foundation for future automation, prioritization, and productivity tools.

Thank you for watching.

Optional Short Version — 10 Minute Cut

If I needed to keep the video closer to 10 minutes, I would compress the structure like this:

Opening — 45 seconds

Product demo — 3.5 to 4 minutes

Documentation process — 1 minute

Tech stack — 2 minutes

Architecture — 1.5 to 2 minutes

Trade-offs / production improvements — 1.5 minutes

The highest-priority points to preserve are:

Product solves inbox triage and prioritization.

A standardized documentation pack was created before implementation.

Antigravity was used as part of the AI-assisted implementation workflow.

Gmail OAuth is read-only.

Latest 200 threads are normalized and classified.

Users can add custom buckets.

Custom bucket creation triggers full reclassification.

React handles the UI.

Spring Boot owns OAuth, Gmail, LLM orchestration, and persistence.

Postgres is source of truth.

Redis supports job state, progress, semaphores, token-bucket choking, and reclassification locks.

MongoDB is optional for evidence/document storage.

Production version would add background jobs, privacy controls, evaluation, feedback loops, and opt-in Gmail write-back.

