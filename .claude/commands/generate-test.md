Read all source code under src/main before writing any tests.

Generate comprehensive tests targeting ≥ 80% code coverage.

## 1. Unit Tests — Service Layer
Use JUnit 5 + Mockito. Place in src/test/java mirroring the main package structure.

### AccountService tests
- Happy path: create account, get by ID, get by account number, get balance
- Business rule violations: duplicate account, account not found
- Edge cases: boundary values for minimum deposit

### TransactionService tests
- Happy path: deposit, withdraw, transfer
- Business rule violations: insufficient funds, negative amount, zero amount
- Edge cases: exact balance withdrawal, transfer to same account
- Atomicity: verify transfer rolls back fully when either side fails

## 2. Integration Tests — Controllers
Use @SpringBootTest + MockMvc. Test the full request/response cycle.

### AccountController tests
- Valid account creation returns 201 with location header
- Invalid request body returns 400 with validation error details
- Non-existent account returns 404

### TransactionController tests
- Valid deposit/withdraw/transfer returns correct response
- Validation errors return 400
- Business rule violations return correct status and error message

## 3. Coverage
After writing tests, confirm coverage with:
```
mvn test jacoco:report
```
Fix any gaps to maintain ≥ 80% line coverage.