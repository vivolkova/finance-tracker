# 💰 Finance Tracker

Personal finance REST API built with Kotlin, Spring Boot, and PostgreSQL.  
A pet project for learning backend development.

## Tech Stack

- **Kotlin** + **Spring Boot 4**
- **PostgreSQL** — database
- **Spring Data JPA** — data access
- **Flyway** — database migrations
- **MockK** — testing
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

2. Start the database
```bash
   docker compose up -d
```

3. Run the application
```bash
   ./gradlew bootRun
```

API will be available at `http://localhost:8080`

## Project Structure
