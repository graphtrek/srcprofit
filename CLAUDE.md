# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

SrcProfit is a Spring Boot 3.5.6 application for tracking and analyzing trading positions, particularly focused on options trading. It integrates with multiple financial data providers (Alpaca, Interactive Brokers/IBKR, Alpha Vintage) to fetch market data, track net asset values, and calculate trading metrics like daily premium and annualized ROI.

## Technology Stack

- **Java 24** with Spring Boot 3.5.6
- **Database**: PostgreSQL 15 (via JPA/Hibernate)
- **Templating**: JTE (Java Template Engine) for server-side rendering
- **Build Tool**: Maven (use `./mvnw` wrapper)
- **Containerization**: Docker with multi-stage builds
- **Virtual Threads**: Enabled for improved concurrency

## Development Commands

### Local Development

```bash
# Build the project
./mvnw clean install

# Run the application (requires environment variables)
./mvnw spring-boot:run

# Run with development profile
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev

# Skip tests during build
./mvnw clean install -DskipTests
```

### Docker & Database

```bash
# Start PostgreSQL and pgAdmin only (uses docker-compose.env)
docker-compose --env-file docker-compose.env up db pgadmin

# View logs
docker-compose logs -f db

# Stop all services
docker-compose down

# Remove volumes and restart (if you need fresh databases)
docker-compose down -v
docker-compose --env-file docker-compose.env up db pgadmin
```

**Database Setup**:
- The `init/init-db.sh` script automatically creates 5 databases on first startup:
  - `srcprofit`, `srcprofit1`, `srcprofit2`, `moneypenny`, `stableips`
- All databases are owned by the `srcprofit` user (password: `srcprofit`)
- PostgreSQL runs with default `postgres` superuser, but applications connect as `srcprofit` user
- Database initialization only runs on first container startup (when volume is empty)

### CI/CD

GitHub Actions workflow (`.github/workflows/ci.yml`) automatically:
- Builds with Maven on push to master/develop
- Creates Docker image and pushes to GitHub Container Registry (ghcr.io)
- Uploads JAR artifact

## Architecture

### Package Structure

```
co.grtk.srcprofit/
├── config/         - RestClient and ObjectMapper configuration
├── controller/     - REST and MVC controllers for different data sources
├── dto/            - Data Transfer Objects for API responses
├── entity/         - JPA entities with Hibernate mappings
├── mapper/         - Conversion logic between entities and DTOs
├── repository/     - Spring Data JPA repositories
└── service/        - Business logic for data retrieval and processing
```

### Key Components

**Entity Hierarchy**:
- `BaseAsset` is a `@MappedSuperclass` containing common fields (tradeDate, quantity, positionValue, etc.)
- `OptionEntity` extends `BaseAsset` and adds option-specific fields (expirationDate, strike, status, type)
- Uses Single Table Inheritance pattern with discriminator

**REST Clients**: Each external service has dedicated configuration:
- `AlpacaService`: Fetches stock/option quotes and market data snapshots
- `IbkrService`: Integrates with Interactive Brokers API and Flex Web Service for trades and reports
- `AlphaVintageService`: Additional market data provider

**Data Flow**:
1. Controllers expose REST endpoints and web pages
2. Services call external APIs via configured RestClients
3. Data is mapped to DTOs or persisted as entities
4. JTE templates render data for web UI

### Template Engine (JTE)

- Templates located in `src/main/jte/`
- Pre-compiled templates enabled in production (`gg.jte.use-precompiled-templates=true`)
- JTE Maven plugin generates templates during build phase

### Database Configuration

Hibernate settings optimized for batch operations:
- Batch size: 200
- Fetch size: 50
- Order inserts/updates enabled
- DDL auto-update enabled (use with caution in production)
- Connection pool: HikariCP with 20 max connections, 5 minimum idle

## Environment Variables

Required environment variables (reference `application.yaml`):

```bash
# Alpaca
ALPACA_DATA_URL=
ALPACA_API_KEY=
ALPACA_API_SECRET_KEY=

# Interactive Brokers
IBKR_DATA_URL=
IBKR_FLEX_URL=
IBKR_FLEX_API_TOKEN=
IBKR_FLEX_TRADES_ID=
IBKR_FLEX_NET_ASSET_VALUE_ID=
IBKR_ACCOUNT_ID=

# Alpha Vintage
ALPHA_VINTAGE_API_KEY=

# Database - Multiple databases for different instances
SRCPROFIT_DB_URL=jdbc:postgresql://db:5432/srcprofit
SRCPROFIT_DB_URL1=jdbc:postgresql://db:5432/srcprofit1
SRCPROFIT_DB_URL2=jdbc:postgresql://db:5432/srcprofit2
SRCPROFIT_DB_USER=srcprofit
SRCPROFIT_DB_PWD=srcprofit

# StableIPS Database
STABLEIPS_DB_URL=jdbc:postgresql://db:5432/stableips
STABLEIPS_DB_USER=srcprofit
STABLEIPS_DB_PWD=srcprofit

# MoneyPenny Database
MONEYPENNY_DB_URL=jdbc:postgresql://db:5432/moneypenny
MONEYPENNY_DB_USER=srcprofit
MONEYPENNY_DB_PWD=srcprofit

# pgAdmin (for docker-compose)
PGADMIN_DEFAULT_EMAIL=
PGADMIN_DEFAULT_PASSWORD=
```

Store these in `docker-compose.env` for local development.

## Spring Boot Actuator

Management endpoints exposed at `/actuator`:
- `/health` - Liveness and readiness probes
- `/info` - Application info
- `/prometheus` - Metrics in Prometheus format
- `/shutdown` - Graceful shutdown (requires POST)

Health checks configured with separate liveness (ping only) and readiness (all checks) groups.

## Important Notes

- The application uses virtual threads (Java 24 feature) - ensure compatibility when working with thread-based operations
- JPA `open-in-view` is disabled - ensure all lazy relationships are fetched within transaction boundaries
- Database schema updates automatically via `hibernate.ddl-auto: update` - be cautious with schema changes in production
- The Dockerfile uses multi-stage builds: JDK 24 for build, JRE 24 Alpine for runtime
- Application runs as non-root user (`appuser`) in container for security
