# 💰 Finance Tracker

Personal finance REST API built with Kotlin, Spring Boot, and PostgreSQL.  
A pet project for learning backend development.

## Features

- 🔐 **JWT authentication** — register, login, and refresh tokens
- 🏷️ **Categories** — income/expense categories
- 💸 **Transactions** — full CRUD plus partial update
- 📊 **Monthly summary** — totals, balance, and per-category breakdown
- ⚡ **Redis caching** — monthly summary cached with a TTL and evicted on writes
- 🔁 **Recurring transactions** — schedules that auto-create transactions on a cron job
- 📖 **OpenAPI / Swagger UI** — interactive API docs

## Tech Stack

- **Kotlin** + **Spring Boot 4**
- **PostgreSQL** — database
- **Redis** — cache backend (Spring Cache abstraction + Lettuce)
- **Spring Data JPA** + **Hibernate** — data access
- **Flyway** — database migrations
- **Spring Security** + **JWT (jjwt)** — authentication
- **Spring Boot Actuator** — health, metrics, and cache endpoints
- **Springdoc OpenAPI / Swagger UI** — API documentation
- **MockK** + **JUnit 5** — testing
- **Docker** — local infrastructure
- **Kubernetes** + **Kustomize** — deployment manifests (`k8s/`)
- **GitHub Actions** — CI (tests) and CD (build, push to GHCR, deploy)

## Getting Started

### Prerequisites
- JDK 21
- Docker Desktop

### Run locally

1. Clone the repository
```bash
git clone https://github.com/vivolkova/finance-tracker.git
cd finance-tracker
```

2. Run the application (Docker Compose starts PostgreSQL and Redis automatically)
```bash
./gradlew bootRun
```

API will be available at `http://localhost:8080`  
Swagger UI: `http://localhost:8080/swagger-ui/index.html`

## Authentication

The API uses stateless JWT authentication. All endpoints require a valid token
**except** `/api/auth/**` and the Swagger/OpenAPI docs, which are public.

1. Register or log in to receive an `accessToken` and a `refreshToken`.
2. Send the access token on every protected request:
```
Authorization: Bearer <accessToken>
```
3. When the access token expires, exchange the refresh token at `/api/auth/refresh`
   for a new pair.

## API Endpoints

### Auth
| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/auth/register` | Register a new user |
| POST | `/api/auth/login` | Log in and receive tokens |
| POST | `/api/auth/refresh` | Exchange a refresh token for new tokens |

### Users
| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/users` | List all users (public endpoint) |

### Categories
| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/categories` | Get all categories |
| GET | `/api/categories/{id}` | Get category by id |
| POST | `/api/categories` | Create category |
| DELETE | `/api/categories/{id}` | Delete category |

### Transactions
| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/transactions` | Get all transactions |
| GET | `/api/transactions/{id}` | Get transaction by id |
| POST | `/api/transactions` | Create transaction |
| PATCH | `/api/transactions/{id}` | Partially update a transaction |
| DELETE | `/api/transactions/{id}` | Delete transaction |
| GET | `/api/transactions/summary?year={year}&month={month}` | Monthly summary (totals, balance, by category) |

### Recurring Schedules
| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/transactions/{transactionId}/schedule` | Get the schedule for a transaction |
| POST | `/api/transactions/{transactionId}/schedule` | Create a recurring schedule |
| DELETE | `/api/transactions/{transactionId}/schedule` | Delete the schedule |

A background job (`RecurringTransactionScheduler`) runs every minute, creates due
transactions from active schedules, and deactivates schedules past their end date.
Supported frequencies: `DAILY`, `WEEKLY`, `MONTHLY`, `YEARLY`.

## Caching

The monthly summary is backed by **Redis**. `getMonthlySummary` is `@Cacheable`
(cache `monthlySummary`, keyed by `year-month`) with a **10-minute TTL**; values are
stored as JSON (`GenericJacksonJsonRedisSerializer`). Every transaction
create/update/delete evicts the whole `monthlySummary` cache, so totals never go
stale. Caching is turned on with `@EnableCaching` and configured in `CacheConfig`.

## API Documentation

Full interactive API documentation available via Swagger UI:

http://localhost:8080/swagger-ui/index.html

OpenAPI JSON (importable to Postman):

http://localhost:8080/v3/api-docs

## Deployment

### Docker

Build the JAR, then the image (`Dockerfile` uses a Temurin 21 JRE base, exposes 8080):
```bash
./gradlew bootJar
docker build -t finance-tracker:1.0 .
```

Published images are available on GitHub Container Registry:
`ghcr.io/vivolkova/finance-tracker` (tagged with the commit SHA and `latest`).

### Kubernetes

Manifests live in `k8s/` and are bundled with **Kustomize** (`kustomization.yaml`):
namespace, ConfigMap, PostgreSQL StatefulSet/Service/PVC, a Redis Deployment/Service,
the app Deployment/Service, and an Ingress (`finance-tracker.local`, nginx). The app
reads its DB and Redis connection from environment variables injected via the
`finance-config` ConfigMap and the `finance-secrets` Secret.

```bash
# DB password (the Secret is not committed)
kubectl create secret generic finance-secrets -n finance-tracker \
  --from-literal=DB_PASSWORD=<your-password>

# apply everything through Kustomize
kubectl apply -k k8s/
```

> The namespace is created by the same `kubectl apply -k` run, so create the Secret
> after the first apply (or add it to your cluster out-of-band) if the namespace does
> not yet exist.

## CI/CD

Two GitHub Actions workflows live in `.github/workflows/`:

- **`run_tests.yml` (CI)** — on pushes to `dev`/`master` and PRs to `master`: builds
  with the Gradle Wrapper on JDK 21, runs the test suite, and uploads the test reports
  as an artifact.
- **`cd.yml` (CD)** — on push to `master` (or manual `workflow_dispatch`): builds the
  Docker image and pushes it to `ghcr.io/vivolkova/finance-tracker` (tagged with the
  long commit SHA and `latest`), then a self-hosted runner pins that tag in
  `k8s/kustomization.yaml`, runs `kubectl apply -k k8s/`, and waits for the rollout.

## Configuration

Key settings live in `src/main/resources/application.properties`:

| Property | Description |
|----------|-------------|
| `spring.datasource.*` | PostgreSQL connection (env-driven, see below) |
| `spring.data.redis.*` | Redis connection for the cache (env-driven, see below) |
| `jwt.secret` | HMAC signing key (must be ≥ 256 bits for HS256) |
| `jwt.expiration` | Access token lifetime, ms |
| `jwt.refresh-expiration` | Refresh token lifetime, ms |

The datasource is configured via environment variables (with local defaults), so the
same build runs locally, in Docker, and in Kubernetes:

| Env var | Default | Description |
|---------|---------|-------------|
| `DB_HOST` | `localhost` | PostgreSQL host |
| `DB_PORT` | `5432` | PostgreSQL port |
| `DB_NAME` | `finance_tracker` | Database name |
| `DB_USERNAME` | `postgres` | Database user |
| `DB_PASSWORD` | — | Database password (set via the `finance-secrets` Secret in k8s) |
| `REDIS_HOST` | `localhost` | Redis host |
| `REDIS_PORT` | `6379` | Redis port |

## Running Tests

```bash
./gradlew test
```

The suite includes unit tests (MockK) and integration tests that spin up a real
PostgreSQL and Redis via **Testcontainers**, so Docker must be running.

## License
MIT
