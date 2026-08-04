# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build System

This is a Maven project using Java 11.

**Build the project:**
```bash
mvn clean compile
```

**Run tests:**
```bash
mvn test
```

**Run a specific test class:**
```bash
mvn test -Dtest=UserServiceTest
```

**Run a specific test method:**
```bash
mvn test -Dtest=UserServiceTest#findById_用户存在_返回对应用户
```

**Package:**
```bash
mvn package
```

## Architecture

This is a demo project showcasing a three-layer architecture:

**Model Layer** (`com.example.model`)
- `User`: User entity with role-based access (ADMIN/USER/GUEST), logical deletion via `active` flag
- `Order`: Order entity linked to users via `userId`, with status workflow (PENDING → PAID → SHIPPED → COMPLETED/CANCELLED)

**Service Layer** (`com.example.service`)
- `UserService`: In-memory user management with ConcurrentHashMap as database simulation
- `OrderService`: Order management that depends on UserService, uses ConcurrentHashMap for storage
- Both services use `AtomicLong`/stream-based ID generation for concurrency

**Controller Layer** (`com.example.controller`)
- `UserController`: REST-style endpoint handlers that coordinate UserService and OrderService
- Returns `Map<String, Object>` responses with `code`, `data`, `message` fields

**Utilities** (`com.example.util`)
- `StringUtils`: Common string operations, validation (email/phone), case conversion

## Known Issues (Intentional for Demo)

The codebase intentionally contains bugs for demonstration/review purposes. Comments in the code mark these with "BUG:":

- `UserService.update()`: Missing null check, throws NPE when user doesn't exist
- `UserService.search()`: Missing null check on keyword parameter
- `UserService.login()`: Uses plain string comparison instead of BCrypt, returns null for nonexistent users
- `UserController.createUser()`: No input validation
- `UserController.updateUser()`: No exception handling for NPE
- `UserController.searchUsers()`: Passes null keyword directly to service
- `UserController.createOrder()`: Doesn't validate user existence before creating order
- `UserController.batchUpdateRole()`: Missing authorization check
- `OrderService.createOrder()`: Doesn't validate user existence
- `OrderService.cancelOrder()`: Allows canceling already-paid orders
- `OrderService.findByUserId()`: Performance issue - full collection scan instead of indexed lookup
- `OrderService.getOrderDetail()`: N+1 query problem
- `StringUtils.buildLikeQuery()`: SQL injection vulnerability via direct string concatenation

## Testing Conventions

Tests use JUnit 5 with the following naming pattern:
`methodName_scenario_expectedResult` in Chinese

Example: `findById_用户存在_返回对应用户`

Each test includes a comment explaining the intent (`意图：...`).

Test structure:
- `@BeforeEach` creates fresh service instances for isolation
- Helper methods like `createAndSaveUser()` reduce boilerplate
- Tests document known bugs with comments like "已知 BUG"
