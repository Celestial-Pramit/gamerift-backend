# Phase 5 notes (for explaining to faculty)

## Request flow (sample feature: Categories)
```
Browser / Swagger
   GET /api/categories
        |
        v
CategoryController          (layer: HTTP. no logic)
        |  calls the INTERFACE
        v
ICategoryService -> CategoryServiceImpl   (layer: business logic, throws NotFoundException)
        |
        v
CategoryRepository          (layer: database access, Spring Data creates the code)
        |
        v
MongoDB Atlas  (collection "categories")
        |
        v  back up: Category (model) --CategoryMapper--> CategoryResponse (DTO) --> JSON
```
Errors: NotFoundException -> GlobalExceptionHandler -> `{status:404, message:"Category not found: x", ...}`.

## What each Phase 5 piece is for
| Piece | Purpose |
|---|---|
| `pom.xml` | Dependencies (Spring Boot 4.1.1, MongoDB, Security, Validation, Actuator, springdoc, Lombok, tests) |
| `application.yml` + profiles | Settings; secrets come from `.env`; `dev` and `prod` profiles |
| `shared/config` | `SecurityConfig` (stateless, BCrypt bean, temporarily open), `CorsConfig` (only the frontend origin), `SwaggerConfig` |
| `shared/exception` | One error format for the whole API (`GlobalExceptionHandler`) |
| `feature/*/model` | MongoDB documents (`@Document`, `@Indexed`) |
| `feature/*/repository` | Spring Data interfaces (derived queries) |
| `feature/category/*` | Full sample of the layered pattern: Controller, Service interface + Impl, DTO, Mapper |
| `shared/seed/DataSeeder` | Fills an empty database (idempotent, admin from `.env`) |
| `CategoryServiceImplTest` | JUnit 5 + Mockito test of the service layer |

## Viva cheat sheet
1. **Why package by feature?** Everything about one feature is together; easier to find, change and test than flat controller/service folders.
2. **Why a service interface + implementation?** The controller depends on an abstraction (Dependency Inversion); implementations can change or be mocked in tests; narrow interfaces (Interface Segregation).
3. **Why DTOs instead of returning models?** Control exactly what leaves the server (never password hashes), and the API stays stable if the database model changes.
4. **What does `@RestControllerAdvice` do?** Catches exceptions from all controllers in one place and returns the same JSON error shape.
5. **Why constructor injection?** Dependencies are explicit and final, the class is easy to test without Spring, and no hidden field injection.
6. **How can a repository interface work without code?** Spring Data creates the implementation at start-up and turns method names like `findBySlug` into MongoDB queries.
7. **Why BCrypt?** One-way, salted, deliberately slow hash; we can verify a password but never read it back.
8. **Why environment variables and profiles?** Secrets never go into git; `dev` and `prod` behave differently (demo users, logging).
9. **What is a `CommandLineRunner`?** A bean whose `run()` executes once after start-up; we use it to seed data safely (only when collections are empty).
10. **Why is security open now?** Phase 5 only builds the foundation; Phase 6 adds JWT and role rules.

## Troubleshooting
- `Could not resolve placeholder 'MONGODB_URI'` -> `.env` missing or not in the `backend` folder.
- `MongoTimeoutException` / authentication failed -> wrong password or IP not allowed in Atlas Network Access.
- Lombok errors in IntelliJ (`cannot find symbol getX()`) -> enable annotation processing and install/enable the Lombok plugin.
- Port 9090 busy -> stop the other app or change `server.port`.
