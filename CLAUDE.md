# Project: Banking System API

## Tech Stack
- Java 25, Spring Boot 4.0.2
- H2 Database (embedded, relational)
- Lombok, Maven

## Project Docs
- PRD: docs/account-management-prd.md
- Technical Design: docs/technical-design.md
- Always read these before implementing

## Standards
- Use BigDecimal for ALL money fields (never double/float)
- All entities need audit fields (createdAt, updatedAt)
- Use custom exceptions, never generic ones
- Minimum test coverage: 80%
- Follow REST best practices (correct HTTP verbs and status codes)

---

## SDLC Workflow — AI-Assisted Development

```
Raw Requirement → PRD → Technical Design → Code → Tests → Docs
     (YOU)        (AI)       (AI)          (AI)   (AI)   (AI)
```

**Rule:** You own decisions and quality gates. AI owns generation and detail.

### Phase roles
| Phase | AI Role | Your Job |
|-------|---------|----------|
| Requirement → PRD | Business Analyst | Write raw requirement, APPROVE output |
| PRD → Technical Design | Solution Architect | Challenge trade-offs, APPROVE design |
| Design → Code | Senior Developer | Review correctness, APPROVE code |
| Code → Unit Tests | Developer / QA | Verify coverage is meaningful |
| Code → Integration Tests | QA Automation | Define scenarios, verify real behavior |
| Code → Docs | Technical Writer | Review accuracy, APPROVE output |

---

## Phase 4 — Implementation: Layer Decision Menu

When implementing, decide which elements to include per layer. Not every element is needed every time — pick based on requirements.

### Layer 1 — Entity
| Element | Use when |
|---------|----------|
| `@Entity` + `@Table` | Always |
| `@Id` + `@GeneratedValue` | Always |
| `@Column(nullable, unique, length)` | Field has DB-level constraints |
| `BigDecimal` for money | Always for financial values |
| `@Enumerated(EnumType.STRING)` | Field has fixed set of values (status, type) |
| `@OneToMany` / `@ManyToOne` | Entities are related |
| `createdAt` + `updatedAt` + `@PrePersist`/`@PreUpdate` | Always — every entity |
| `@Version` | Multiple users may update same record (optimistic locking) |
| `@Builder` (Lombok) | Entity has many fields |
| No `@Setter` (immutable) | Entity must not change after creation (e.g. Transaction) |

### Layer 2 — Repository
| Element | Use when |
|---------|----------|
| `extends JpaRepository<Entity, ID>` | Always — start here |
| `findBy[Field](value)` | Simple lookup by one field |
| `findAllBy[Field]OrderBy[Field]` | Need ordered list results |
| `@Query("SELECT ...")` | Derived query name is too complex |
| `@Lock(LockModeType.PESSIMISTIC_WRITE)` | Concurrent writes on same row (balance updates) |
| `Pageable` parameter | Result set can be large |
| `existsBy[Field](value)` | Duplicate check without loading entity |

### Layer 3 — DTOs (Request + Response)
| Element | Use when |
|---------|----------|
| Separate Request DTO | Always for POST/PUT/PATCH — never reuse entity |
| Separate Response DTO | Always — controls what fields are exposed |
| `@NotNull` / `@NotBlank` | Field must always be present |
| `@Size(min, max)` | String has length limits |
| `@DecimalMin` / `@DecimalMax` | Number has range limits (money amounts) |
| `@Digits(integer, fraction)` | Decimal precision required (money: max 2 decimal places) |
| `@Email` | Email fields |
| `@Pattern(regexp)` | Phone numbers, codes with specific format |
| `@Positive` / `@PositiveOrZero` | Field can't be negative |

### Layer 4 — Exceptions
| Element | Use when |
|---------|----------|
| Custom exception `extends RuntimeException` | Every business rule violation — never throw generic Exception |
| NotFoundException → 404 | findById / findByX returns empty |
| ConflictException → 409 | Duplicate (email, account number) |
| BusinessRuleException → 400 | Violates domain rule (insufficient funds, min deposit) |
| `@RestControllerAdvice` + `@ExceptionHandler` | Always — one central place for error responses |
| Standard `ErrorResponse` DTO | Always — consistent error shape: error, message, timestamp |

### Layer 5 — Service
| Element | Use when |
|---------|----------|
| `@Transactional` | Any method that writes to DB |
| `@Transactional(readOnly = true)` | Any method that only reads |
| `isolation = Isolation.READ_COMMITTED` | Default for most write operations involving money |
| `isolation = Isolation.SERIALIZABLE` | Critical ops where phantom reads are dangerous |
| Pessimistic lock via repository | Concurrent balance updates |
| Ordered lock acquisition (by ID ascending) | Transfer ops that lock 2+ rows — prevents deadlock |
| Validate → Fetch → Apply → Save pattern | All write operations |
| Throw custom exception on rule violation | Every business rule check |
| Entity → Response DTO mapping | Always — never return raw entity from service |
| `@Slf4j` + `log.info()` | All create/update/delete ops and exceptions |

### Layer 6 — Controller
| Element | Use when |
|---------|----------|
| `@RestController` + `@RequestMapping` | Always |
| `@PostMapping` → 201 Created | Creating new resource |
| `@GetMapping` → 200 OK | Fetching / listing |
| `@PutMapping` → 200 OK | Full resource replace |
| `@PatchMapping` → 200 OK | Partial update |
| `@DeleteMapping` → 204 No Content | Delete resource |
| `@PathVariable` | Identifying specific resource in URL path |
| `@RequestParam` | Filters, pagination, optional params |
| `@RequestBody` + `@Valid` | Always for POST/PUT/PATCH with body |
| `ResponseEntity.created(location).body(dto)` | POST that creates new resource |
| `@Tag` + `@Operation` + `@ApiResponses` | Always — document every endpoint |

---

## Spring Boot 4.x Gotchas
- Jackson 3.x is used — `spring.jackson.serialization.*` properties cause binding failures, remove them
- JaCoCo 0.8.12 does not support Java 25 — use 0.8.13+
- `@Lock` with `@Query` required for pessimistic locking on custom queries
- Ordered lock acquisition by ID prevents deadlocks in multi-row transactions
