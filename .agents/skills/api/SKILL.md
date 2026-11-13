---
name: api
description: Implement Spring Boot REST api with validation, persistence, error handling, and tests.
---

# Spring Endpoint Implementation

When implementing an endpoint:

1. Inspect existing endpoint conventions.
2. Identify API contracts and acceptance criteria.
3. Identify relevant business invariants.
4. Plan the controller, service, and repository changes.
5. Implement validation and error handling.
6. Add unit and integration tests as appropriate.
7. Run relevant Gradle verification tasks.
8. Report changes, test results, and remaining risks.
9. Do not implement repetitive validations - if field was validated in controller no need to recheck it in service