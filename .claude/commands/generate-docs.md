Read these docs before starting:
- docs/account-management-prd.md
- docs/technical-design.md

Then read the existing source code under src/ to understand the actual implementation.

Generate the following documentation:

## 1. README.md
- Project overview (what it does, tech stack)
- Prerequisites and how to run locally
- H2 console access instructions
- Swagger UI access (http://localhost:8080/swagger-ui.html)
- curl examples for every API endpoint

## 2. Swagger/OpenAPI Annotations
Verify all controllers have complete OpenAPI annotations:
- @Operation with summary and description
- @ApiResponse for each possible HTTP status code
- @Parameter on path/query variables
- Ensure /swagger-ui.html renders correctly with no missing descriptions

## 3. docs/API-GUIDE.md
- Full API reference: endpoint, method, request body, response body
- Example curl commands for happy path and error cases
- Business rule descriptions (minimum deposit, transfer limits, etc.)
- Error response format with all possible error codes