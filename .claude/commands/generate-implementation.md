Read these docs before starting:
- docs/account-management-prd.md
- docs/technical-design.md

Implement in this order:

## 1. Custom Exceptions
Create all domain exceptions (e.g. AccountNotFoundException, InsufficientFundsException, DuplicateAccountException) — never throw generic exceptions.

## 2. Entities & Repositories
- Account entity with audit fields (createdAt, updatedAt)
- Transaction entity with audit fields
- JPA repositories for each

## 3. Service Layer

### AccountService
- createAccount, getAccountById, getAccountByNumber, getBalance

### TransactionService
- deposit, withdraw, transfer (must be atomic — both debit and credit succeed or both fail)

Rules:
- BigDecimal for all money values (never double/float)
- @Transactional with appropriate isolation levels
- Enforce all PRD business rules (minimum deposit, no negative balance, etc.)
- Throw custom exceptions for all business rule violations
- Log all important operations

## 4. REST Controllers

### AccountController
### TransactionController

REST standards:
- Correct HTTP status codes (201 for creation, 400 for validation errors, 404 for not found)
- Use request/response DTOs — never expose entities directly
- @Valid on all request bodies
- Swagger/OpenAPI annotations on all endpoints

## 5. Tests
Write unit tests for all service methods targeting ≥ 80% coverage.