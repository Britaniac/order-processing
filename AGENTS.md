
# Project Instructions

This is an educational Java/Spring microservices project.

## Architecture
- Each microservice owns its database.
- Services communicate through explicit APIs/events.
- Business logic belongs in the service layer.
- Avoid distributed database transactions.

## Goals
- Learn distributed systems, concurrency and security.
- Prefer understandable designs over unnecessary complexity.
- Use production-relevant patterns where justified.

## Working Rules
- Discuss significant architectural changes before implementation.
- Explain tradeoffs and alternatives.
- Implement one feature at a time.
- Write meaningful integration tests.
- Never silently weaken tests to make a build pass.
- Do not introduce new infrastructure without justification.
- Preserve database ownership boundaries.
- Explicitly identify race conditions and failure scenarios.
- Do not create READMEs without justification.
- Do not use var.
- Use Lombok annotations

## Completion
- Run relevant tests.
- Summarize changes.
- Identify remaining risks.
- Suggest the next learning exercise.
