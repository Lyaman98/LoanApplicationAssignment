# Loan Comparison Platform

A REST API where customers submit loan applications, partner lenders respond with offers, and the
customer accepts one. Java 21, Spring Boot 3.5, PostgreSQL, Flyway, Maven.

---

## Build and run

### With Docker 

```bash
docker compose up --build
```

Brings up PostgreSQL and the application. The app waits for the database's health check, Flyway
migrates the schema on startup, and the API is available at `http://localhost:8080`.

### Locally against your own PostgreSQL

```bash
docker compose up -d postgres         
./mvnw spring-boot:run
```

Defaults are `jdbc:postgresql://localhost:5432/lendo` with user and password `lendo`. Override with
the standard Spring variables - `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME`,
`SPRING_DATASOURCE_PASSWORD` - which is exactly what `docker-compose.yml` does.

### After a code change

```bash
docker compose up -d --build app
```

Rebuilds and restarts only the application, leaving PostgreSQL and its data alone.

If a migration file changed after it was already applied, Flyway will refuse to start with a checksum
mismatch. During development the quickest way out is to throw the schema away and let it migrate again:

```bash
docker compose exec postgres psql -U lendo -d lendo -c 'drop schema public cascade; create schema public;'
docker compose up -d --build app
```

Or drop the whole database volume with `docker compose down -v`.

### Tests

```bash
./mvnw test
```

Tests need a Docker daemon: the persistence and concurrency tests run against a real PostgreSQL
container via Testcontainers. The first run pulls `postgres:16-alpine`.

---

## API

Base path `/api/v1`. Interactive docs once the app is running:

- Swagger UI - <http://localhost:8080/swagger-ui.html>
- OpenAPI document - <http://localhost:8080/v3/api-docs>

All endpoints except the Swagger paths require HTTP Basic credentials.

| Method | Path | Purpose |
| --- | --- | --- |
| `POST` | `/applications` | Create an application. `201` with a `Location` header. `CUSTOMER`. |
| `GET` | `/applications/{id}` | One application including all its offers. |
| `GET` | `/applications` | List, filterable by `status`, `createdFrom`, `createdTo`; paged. |
| `POST` | `/applications/{applicationId}/offers` | A lender submits an offer. `201` with a `Location` header. `LENDER`. |
| `POST` | `/applications/{applicationId}/offers/{offerId}/accept` | The customer accepts one offer. `CUSTOMER`. |

Demo credentials: `customer` / `password` and `lender` / `password`. In Swagger UI, put them into the
**Authorize** dialog.

```bash
# create
curl -si -X POST localhost:8080/api/v1/applications \
  -u customer:password \
  -H 'Content-Type: application/json' \
  -d '{"firstName":"Customer","lastName":"Surname","email":"customer@example.com","amount":50000.00,"loanTerms":24}'

# a lender offers
curl -s -X POST localhost:8080/api/v1/applications/$ID/offers \
  -u lender:password \
  -H 'Content-Type: application/json' \
  -d '{"lenderName":"Lender A","annualInterestRate":5.90,"monthlyPaymentAmount":1200.00,"totalRepayment":43200.00}'

# the customer accepts
curl -s -u customer:password -X POST localhost:8080/api/v1/applications/$ID/offers/$OFFER_ID/accept

# filter and page
curl -s -u customer:password \
  "localhost:8080/api/v1/applications?status=PENDING&createdFrom=2026-01-01T00:00:00Z&page=0&size=20"
```

Errors produced in one place by `GlobalExceptionHandler`:

```json
{
  "type": "about:blank",
  "title": "Conflict with current state",
  "status": 409,
  "detail": "Loan application 8f3c… is ACCEPTED and no longer open"
}
```

| Status | When                                                                       |
| --- |----------------------------------------------------------------------------|
| `400` | Malformed body, e.g. loan policy violation, reversed date range            |
| `401` | Missing or wrong credentials                                                |
| `403` | Authenticated, but the wrong role for this operation                        |
| `404` | Unknown application or an offer that is not on this application            |
| `409` | Application not `PENDING`, lender has already offered, offer already decided |

---

## Design decisions and trade-offs

**Accept is `POST /applications/{id}/offers/{offerId}/accept`, not a PATCH.**
It closes the application and rejects the other offers, so it's an action, not a
field update. A PATCH with a status in the body would let clients try changes
that aren't allowed.

**Accepting is handled by `LoanApplicationService`, not `LenderOfferService`.**
Acceptance appears to be an operation on an offer, but the application's own
status changes with it: it moves from `PENDING` to `ACCEPTED` while the chosen
offer is accepted and every remaining offer is rejected, all in one transaction.
The endpoint stays with the other offer routes, since it falls under
`/applications/{id}/offers/` and a single controller owning a single path keeps
the routing predictable.

**Customer is embedded in `loan_application`, not its own table.**
An application records what was submitted at that time. A shared customer row
would mean a later name change rewrites old applications. It also avoids having
to decide how to recognise a returning customer. Would become an entity once
there's login or an application history.

**Validation is split.**
DTOs check format and positivity (400). Amount and term limits are business
policy and live in `application.properties` via `LoanPolicyProperties`, checked
in the service. Bean Validation annotations need compile-time constants, so
configurable limits can't go there anyway.

**Concurrency: pessimistic lock on the application.**
See the section below.

**One offer per lender is enforced by a unique index.**
The service check is a read then-write that two concurrent callers can both
pass. The index enforces the rule, the check just gives a 409.

**Authentication: HTTP Basic with roles.**
Customers and lenders are separate roles, so a lender cannot accept offers on a customer's behalf.
Basic over an in-memory user store was chosen for simplicity - the rules about who may do what are the
part that matters and they stay the same if the mechanism becomes JWT later. 

**Flyway owns the schema, `ddl-auto=validate`.**
Startup fails if entities and schema disagree. `open-in-view` is off so lazy
loading mistakes surface during development.


**Tests use Testcontainers Postgres, not H2.**
Row locking and the unique index behave differently on H2, so a green test there
would prove nothing. Requires Docker to run tests.

### Not implemented

- **Ownership checks** - biggest gap now that roles exist: any authenticated
  customer can accept any application's offer.
- **JWT** - HTTP Basic with roles is used instead to keep
  the security layer to a single config class. 
- **A real user store** - credentials are demo users in configuration and Basic
  sends them on every request with no expiry. Production would need a user store and
  short-lived tokens.
- **Idempotency on create** - a retried POST creates a second application. Would
  use an `Idempotency-Key` in the header sent by the Client.
- **Async fan-out to partners** - Currently partners are assigned as inbounds for the assignment and simplicity. 
- **Domain events** - nothing is published when an application is created, an offer arrives or one is accepted.
- **Background expiry check** - A scheduled bulk `UPDATE` that would close the expired applications
- **Partner lenders and per-partner configuration** - Currently in the system a lender is a free-text name on a request, not a registered partner.
## Concurrency

Two customers accepting two different offers on the same application at the same
moment. Both read PENDING, both pass the check, both proceed - and the
application ends up with two accepted offers.

The accept path takes a row lock on the application when loading it and checks
the status inside the same transaction. The second request waits for the lock,
then reads ACCEPTED and gets a 409.

The lock is on the application, not the offer - the application's status is what
decides whether accepting is allowed. Two requests locking two different offers
would both still read a stale application.

### Why pessimistic over `@Version`

Blocking for a  moment is cheaper than doing all the work and throwing it away on a version
conflict and there is no retry logic or lock exception to translate.

### The three write paths need different things

- **create** - plain insert, no race
- **submit offer** - unique index, since we can't lock a row that doesn't exist
- **accept** - row lock, since a read decides a write
---

## Testing

| Layer | Tool | What it establishes |
| --- | --- | --- |
| Service | JUnit 5 + Mockito | Branching, guards and policy, with no I/O |
| Persistence | `@DataJpaTest` + Testcontainers | Constraints, entity graph, migrations, the locking query |
| Web | `@WebMvcTest` | Routing, binding, validation, status codes, `Location` headers |
| Full stack | `@SpringBootTest` + Testcontainers | Context starts, and the acceptance race |

Tests run against PostgreSQL.

---

