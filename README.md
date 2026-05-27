# 💰 Finance Tracker

Personal finance REST API built with Kotlin, Spring Boot, and PostgreSQL.  
A pet project for learning backend development.

## Tech Stack

- **Kotlin** + **Spring Boot 4**
- **PostgreSQL** — database
- **Spring Data JPA** + **Hibernate** — data access
- **Flyway** — database migrations
- **Springdoc OpenAPI / Swagger UI** — API documentation
- **MockK** + **JUnit 6** — testing
- **Docker** — local infrastructure
- **Kubernetes** — deployment (coming soon)

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

2. Run the application (Docker starts automatically)
```bash
   ./gradlew bootRun
```

API will be available at `http://localhost:8080`  
Swagger UI: `http://localhost:8080/swagger-ui/index.html`


# Project Structure
## API Endpoints

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
| DELETE | `/api/transactions/{id}` | Delete transaction |

## API Documentation

Full interactive API documentation available via Swagger UI:
http://localhost:8080/swagger-ui/index.html

OpenAPI JSON (importable to Postman):
http://localhost:8080/v3/api-docs

## Running Tests

```bash
./gradlew test
```

## License
MIT