# CurrEx — Currency Converter

A full-stack currency converter with real-time rates, historical charts, price alerts, and conversion history.

---

## Tech Stack

| Layer    | Technology                                                         |
|----------|--------------------------------------------------------------------|
| Backend  | Java 21 · Spring Boot 3.2 · Spring Security (JWT) · Hibernate/JPA |
| Database | PostgreSQL 16                                                      |
| Cache    | Redis 7 (1-hour rate TTL)                                          |
| Frontend | React 18 · TypeScript · Vite · Recharts · Axios                   |
| Deploy   | Docker + Docker Compose                                            |
| Rates    | ExchangeRate-API v6 (free tier — 1500 req/month)                  |

---

## Features

| Feature | Details |
|---|---|
| Live conversion | Debounced real-time conversion across 20 currencies, works without login |
| Historical charts | 7d / 30d / 90d / 1y rate charts, stored daily via scheduler |
| Price alerts | Email notification when rate crosses your target (checked every 15 min) |
| Conversion history | Paginated per-user log of past conversions |
| Favorites | Star currency pairs for one-click access, synced to backend |
| Auth | JWT-based register/login, stateless sessions |

---

## Prerequisites

- Java 21 — `brew install openjdk@21`
- Maven — `brew install maven`
- Node.js 20+ — `brew install node`
- PostgreSQL running on 5432
- Redis running on 6379

---

## Local Development (no Docker)

### 1. Configure environment

```bash
cp .env.example .env
# Required: fill in JWT_SECRET, EXCHANGE_RATE_API_KEY
# Optional: fill in MAIL_* for price alert emails
```

### 2. Create the database

```bash
psql -U postgres -c "CREATE DATABASE currencydb;"
```

### 3. Start the backend

```bash
cd backend
export $(grep -v '^#' ../.env | xargs)
export JAVA_HOME=/opt/homebrew/opt/openjdk@21   # macOS Homebrew
mvn spring-boot:run
# → http://localhost:8080
```

### 4. Start the frontend

```bash
cd frontend
# frontend/.env already contains VITE_API_URL=http://localhost:8080
npm install
npm run dev
# → http://localhost:5173
```

---

## Docker Compose

```bash
cp .env.example .env
# Edit .env — at minimum set JWT_SECRET and EXCHANGE_RATE_API_KEY

docker compose up --build
```

| Service  | Host URL               |
|----------|------------------------|
| Frontend | http://localhost:5173  |
| Backend  | http://localhost:8080  |
| Postgres | localhost:5432         |
| Redis    | localhost:6379         |

---

## Environment Variables

See [`.env.example`](.env.example) for the full list.

| Variable                | Required | Description |
|-------------------------|----------|-------------|
| `JWT_SECRET`            | Yes      | Base64-encoded secret, min 32 bytes (`openssl rand -base64 64`) |
| `EXCHANGE_RATE_API_KEY` | Yes      | Free key from [exchangerate-api.com](https://www.exchangerate-api.com) |
| `MAIL_USERNAME`         | No       | Gmail address — needed only for price alert emails |
| `MAIL_PASSWORD`         | No       | Gmail App Password (not your account password) |
| `DB_URL`                | No       | Defaults to `jdbc:postgresql://localhost:5432/currencydb` |
| `REDIS_HOST`            | No       | Defaults to `localhost` |

> **Note:** price alerts are silently skipped if mail credentials are not set — the app still runs normally.

---

## API Reference

| Method | Path                         | Auth | Description |
|--------|------------------------------|------|-------------|
| POST   | `/api/auth/register`         | No   | Register, returns JWT |
| POST   | `/api/auth/login`            | No   | Login, returns JWT |
| GET    | `/api/rates/convert`         | No   | `?from=USD&to=EUR&amount=100` |
| GET    | `/api/rates/historical`      | No   | `?from=USD&to=EUR&period=7d` |
| GET    | `/api/rates/current`         | No   | `?base=USD` — full rate map |
| GET    | `/api/history`               | Yes  | `?page=0&size=15` — paginated history |
| POST   | `/api/alerts`                | Yes  | Create rate alert |
| GET    | `/api/alerts`                | Yes  | List user's alerts |
| DELETE | `/api/alerts/{id}`           | Yes  | Delete an alert |
| GET    | `/api/user/profile`          | Yes  | Profile + favorites |
| POST   | `/api/user/favorites`        | Yes  | Add favorite pair `{"pair":"USD/EUR"}` |
| DELETE | `/api/user/favorites/{pair}` | Yes  | Remove favorite pair |

---

## Architecture

```
Browser
  │
  ├── GET /              → React SPA (Vite dev server / Nginx in Docker)
  │
  └── /api/**            → Spring Boot :8080
                              │
                         ┌────┴────────────────┐
                         │  Controllers         │
                         │  Services            │
                         └────┬──────────┬──────┘
                              │          │
                           Redis       PostgreSQL
                        (rate cache)  (users, history,
                        TTL: 1 hour    alerts, hist. rates)
                              │
                    ExchangeRate-API v6
                    (on cache miss or
                     daily scheduler)
```

### Caching

Rates are cached in Redis under `rates:{baseCurrency}` with a 1-hour TTL. On cache miss the backend calls ExchangeRate-API and repopulates. Hardcoded fallback rates are used if the API is unavailable.

### Historical data

A midnight cron job (`0 0 0 * * *`) stores today's rates for 9 major currencies. The historical chart shows an empty state until at least one day's data exists — check back after the first midnight run, or trigger it manually by calling `exchangeRateService.fetchAndStoreHistoricalRates(...)` from a test.

### Alert checks

Every 15 minutes a scheduled task compares each active alert's target against the live (cached) rate. On trigger: the alert is marked inactive, `triggeredAt` is stamped, and an email is sent via Gmail SMTP. Failed sends are logged as warnings and do not crash the scheduler.

---

## Troubleshooting

| Symptom | Cause | Fix |
|---|---|---|
| Converter returns 401 | Old code without rates permit | Ensure `SecurityConfig` permits `/api/rates/**` |
| History response includes `passwordHash` | Entity serialized directly | `@JsonIgnore` on `ConversionHistory.user` |
| Dates show as `[2024,5,13]` | Jackson timestamps enabled | `spring.jackson.serialization.write-dates-as-timestamps: false` in `application.yml` |
| Historical chart always empty | Scheduler hasn't run yet | Wait for midnight, or seed data manually |
| Alert emails not sending | Missing `MAIL_*` env vars | Set Gmail address + App Password in `.env` |
| `Could not connect to Redis` | Redis not running | `brew services start redis` or `docker run -p 6379:6379 redis` |
