# CurrEx — Currency Converter

A full-stack currency converter with real-time rates, historical charts, price alerts, and conversion history. Built as a portfolio project using Spring Boot, React, PostgreSQL, and Redis.

---

## Tech Stack

| Layer    | Technology                                                 |
|----------|------------------------------------------------------------|
| Backend  | Java 21, Spring Boot 3.2, Spring Security (JWT), Hibernate |
| Database | PostgreSQL 16                                              |
| Cache    | Redis 7                                                    |
| Frontend | React 18, TypeScript, Vite, Recharts, Axios                |
| Deploy   | Docker + Docker Compose                                    |
| Rates    | ExchangeRate-API v6 (free tier)                            |

---

## Features

- **Live conversion** — debounced, real-time currency conversion across 20+ currencies
- **Historical charts** — 7d / 30d / 90d / 1y rate charts stored daily via scheduler
- **Price alerts** — get email notifications when a rate crosses your target (checked every 15 min)
- **Conversion history** — paginated log of past conversions per user
- **Favorites** — star currency pairs for one-click access
- **JWT authentication** — stateless auth, all sensitive routes protected

---

## Local Development (without Docker)

### Prerequisites

- Java 21 (`brew install openjdk@21`)
- Maven (`brew install maven`)
- Node.js 20+ (`brew install node`)
- PostgreSQL running on port 5432
- Redis running on port 6379

### 1. Clone and configure

```bash
cp .env.example .env
# Fill in JWT_SECRET, EXCHANGE_RATE_API_KEY, and mail credentials
```

### 2. Create the database

```bash
psql -U postgres -c "CREATE DATABASE currencydb;"
```

### 3. Start the backend

```bash
cd backend
export $(grep -v '^#' ../.env | xargs)
mvn spring-boot:run
# Runs on http://localhost:8080
```

### 4. Start the frontend

```bash
cd frontend
# .env already contains VITE_API_URL=http://localhost:8080
npm install
npm run dev
# Opens http://localhost:5173
```

---

## Docker Compose

```bash
cp .env.example .env
# Edit .env with your secrets

docker compose up --build
```

| Service  | URL                    |
|----------|------------------------|
| Frontend | http://localhost:5173  |
| Backend  | http://localhost:8080  |
| Postgres | localhost:5432         |
| Redis    | localhost:6379         |

---

## Environment Variables

See [`.env.example`](.env.example) for the full list. Key variables:

| Variable               | Description                                    |
|------------------------|------------------------------------------------|
| `JWT_SECRET`           | Base64-encoded secret (min 32 bytes)           |
| `EXCHANGE_RATE_API_KEY`| Free key from exchangerate-api.com             |
| `MAIL_USERNAME`        | Gmail address for alert emails                 |
| `MAIL_PASSWORD`        | Gmail App Password (not your account password) |

---

## API Endpoints

| Method | Path                        | Auth | Description                      |
|--------|-----------------------------|------|----------------------------------|
| POST   | `/api/auth/register`        | No   | Register a new user              |
| POST   | `/api/auth/login`           | No   | Login, returns JWT               |
| GET    | `/api/rates/convert`        | No   | Convert `from/to/amount`         |
| GET    | `/api/rates/historical`     | No   | Historical rates `from/to/period`|
| GET    | `/api/history`              | Yes  | Paginated conversion history     |
| POST   | `/api/alerts`               | Yes  | Create a rate alert              |
| GET    | `/api/alerts`               | Yes  | List user alerts                 |
| DELETE | `/api/alerts/{id}`          | Yes  | Delete an alert                  |
| GET    | `/api/user/profile`         | Yes  | User profile + favorites         |
| POST   | `/api/user/favorites`       | Yes  | Add a favorite pair              |
| DELETE | `/api/user/favorites/{pair}`| Yes  | Remove a favorite pair           |

---

## Architecture

```
┌─────────────┐     HTTPS      ┌──────────────────────────────────┐
│  React SPA  │ ─────────────► │  Spring Boot (port 8080)         │
│  (Vite)     │                │                                  │
└─────────────┘                │  Controllers → Services          │
                               │       │            │             │
                               │    Redis        Postgres         │
                               │  (rate cache)  (persistence)     │
                               │                    │             │
                               │  Scheduler → ExchangeRate-API   │
                               │            → JavaMailSender      │
                               └──────────────────────────────────┘
```

### Caching strategy

Exchange rates are cached in Redis with a **1-hour TTL** under the key `rates:{baseCurrency}`. On cache miss the app calls ExchangeRate-API and repopulates the cache.

### Historical data

A `@Scheduled(cron="0 0 0 * * *")` job stores today's rates for 15 major currencies at midnight. The historical chart falls back to an empty state until at least one day's data is stored.

### Alert checks

Every 15 minutes a scheduled task compares each active alert's target rate against the live rate. When triggered, it marks the alert as triggered and sends an email via Gmail SMTP.
