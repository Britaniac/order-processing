---
name: test
description: Test implementation details
---

# Test Implementation

When implementing tests:
1. Implement unit tests for services and controllers using Mockito
2. Name tests when[Condition]_then[ExpectedResult], e.g. whenUserExists_thenReturnsUser.
3. Cover each branch of the tested method; one behavior per test
4. Before implementing integration test think if they are actually needed on top of unit tests, and if they are present your reasoning
5. Structure each test in this order, separated by blank lines (omit blocks that don't apply):
    - object creation
    - when().thenReturn() stubbing
    - call to the tested method
    - verify() (including ArgumentCaptor capture if needed, no verifyNoMoreInteractions)
    - asserts (including assertions on captured arguments)
      (see example below)

Do not test:
1. Repositories
2. framework behavior, only logic in our own code. For example:
   - Controllers: invalid request body or content type, unsupported HTTP methods
   - Plain DTOs (no annotations, no special config): serialization/deserialization; just verify service calls and HTTP status
   - Spring wiring, dependency injection, or Mockito itself
3. Exceptions thrown by mocked dependencies, unless the tested code handles them.

Example:
<code>
@Test
void whenValidRequest_thenSavesUserWithNormalizedEmail() {
    CreateUserRequest request = new CreateUserRequest("John@Example.com");
    User saved = new User(1L, "john@example.com");

    when(userRepository.save(any(User.class))).thenReturn(saved);

    UserResponse response = userService.createUser(request);

    ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
    verify(userRepository).save(captor.capture());

    assertThat(captor.getValue().getEmail()).isEqualTo("john@example.com");
    assertThat(response.getId()).isEqualTo(1L);
}
</code>