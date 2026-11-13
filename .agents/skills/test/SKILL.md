---
name: test
description: Test implementation details
---

# Test Implementation

When implementing tests:

1. Implement unit tests for services and controllers using Mockito
2. Use when...Then... naming convention
3. Do not implement repository tests
4. For controllers fo not test for invalid body/content type
5. Do not test for possible mocked service exceptions unless there handling for them in tested code
6. Before implementing integration test think if they are actually needed on top of unit tests, and if they are present your reasoning