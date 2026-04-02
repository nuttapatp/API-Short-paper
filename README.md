# AQI Notification Service

Real-time air quality monitoring backend — delivers AI-generated health alerts via LINE Messaging API, streams live AQI data to a Next.js dashboard, and stores user data in Firebase Firestore.

**Live demo:** [Dashboard](https://aqi-map-short-paper.onrender.com) · [API Health](https://api-short-paper.onrender.com/actuator/health)

---

## System Architecture

This service is part of a 3-repo microservice system:

```
┌─────────────────────────────────────────────────────────────────┐
│                     AQI-Short-paper (8080)                       │
│              Data Pipeline — OpenWeatherMap → BigQuery           │
│   Hourly cron: fetches Bangkok + Chiang Mai AQI → stores to BQ  │
└──────────────────────────┬──────────────────────────────────────┘
                           │ BigQuery (currentaqi, forecastaqi)
                           ▼
┌─────────────────────────────────────────────────────────────────┐
│                     API-Short-paper (8082)  ◄── LINE webhook     │
│                                                                   │
│  ┌─────────────┐  ┌──────────────┐  ┌────────────────────────┐  │
│  │LineController│  │ForecastCtrl  │  │    AqiScheduler        │  │
│  │ (webhook)   │  │GET /forecast │  │    (hourly cron)       │  │
│  └──────┬──────┘  └──────┬───────┘  └───────────┬────────────┘  │
│         │                │                        │               │
│  ┌──────▼────────────────▼────────────────────────▼────────────┐ │
│  │                        Services                              │ │
│  │  IntentService   — Claude AI intent classification          │ │
│  │  ClaudeService   — AQI notifications + forecast analysis    │ │
│  │  BigQueryService — reads historical + forecast AQI          │ │
│  │  AqiAlertService — proactive hourly alerts                  │ │
│  │  SseBroadcastService — SSE push to dashboard                │ │
│  │  RateLimitService — 10 req/min per user                     │ │
│  └──────┬───────────────────────────────┬─────────────────────┘ │
└─────────┼───────────────────────────────┼───────────────────────┘
          │                               │
┌─────────▼──────────┐       ┌────────────▼──────────────┐
│  Firebase Firestore│       │      External APIs          │
│  - users           │       │  Google Air Quality API     │
│  - locations       │       │  Claude AI (Haiku)          │
│  - userProfiles    │       │  LINE Messaging API         │
└────────────────────┘       └─────────────────────────────┘
          │
┌─────────▼──────────┐
│   SSE Stream        │
│  /api/v1/sse/       │
│  aqi-stream         │
└─────────┬───────────┘
          │
┌─────────▼──────────────────┐
│  aqi-map-short-paper (3000) │
│  Next.js + Leaflet map      │
└─────────────────────────────┘
```

---

## Tech Stack

| Layer | Technology |
|---|---|
| Backend | Spring Boot 2.7.18, Java 17 |
| Database | Firebase Firestore, Google BigQuery |
| AI | Claude Haiku 4.5 (Anthropic) |
| Messaging | LINE Messaging API |
| Air Quality | Google Air Quality API |
| Data Pipeline | OpenWeatherMap API (via AQI-Short-paper) |
| Frontend | Next.js 16, Leaflet.js, Tailwind CSS |
| CI/CD | GitHub Actions |
| Deployment | Render (Docker) |
| Monitoring | Spring Actuator, UptimeRobot |

---

## Features

- **LINE Bot** — Users send location via LINE, receive AI-generated AQI health alerts personalized to their health profile
- **Intent Detection** — Claude AI classifies user messages into 6 intents (AQI_NOW, AQI_FORECAST, SET_PROFILE, DELETE_PROFILE, etc.)
- **AQI Forecast** — Fetches 24h history + forecast from BigQuery, Claude analyzes trend and gives Thai-language summary
- **Proactive Alerts** — Hourly cron job alerts users when AQI exceeds threshold (150 general, 100 sensitive groups)
- **Live Dashboard** — SSE-powered real-time map showing all users' AQI readings
- **Health Profiles** — Users can save health conditions (asthma, allergies) for personalized Claude responses
- **Security** — API key auth on internal endpoints, rate limiting (10 req/min per user), MDC request correlation

---

## API Endpoints

### Public
| Method | Path | Description |
|---|---|---|
| GET | `/` | Health check |
| POST | `/webhook` | LINE webhook receiver |
| GET | `/api/v1/sse/aqi-stream` | SSE stream (real-time AQI) |
| GET | `/api/v1/forecast/{city}` | AI forecast analysis for city (Bangkok, Chiang Mai, Phuket, Chonburi) |
| GET | `/actuator/health` | Spring Actuator health |

### Protected (requires `X-API-Key` header)
| Method | Path | Description |
|---|---|---|
| POST | `/api/v1/notify/{userId}` | Send manual notification |
| POST | `/api/v1/sse/broadcast` | Trigger manual SSE broadcast |

---

## Environment Variables

| Variable | Description |
|---|---|
| `LINE_ACCESS_TOKEN` | LINE Messaging API channel access token |
| `GOOGLE_AIR_QUALITY_API_KEY` | Google Air Quality API key |
| `ANTHROPIC_API_KEY` | Anthropic Claude API key |
| `INTERNAL_API_KEY` | API key for protected endpoints |
| `FRONTEND_URL` | Comma-separated allowed CORS origins |
| `GOOGLE_APPLICATION_CREDENTIALS` | Path to Firebase service account JSON |
| `BIGQUERY_CREDENTIALS` | Path to BigQuery service account JSON (e.g. `/etc/secrets/bigquery.json`) |

---

## Local Development

```bash
# 1. Copy and fill in your keys
cp .env.example .env

# 2. Run
mvn spring-boot:run

# 3. Run tests
mvn test
```

---

## Testing

26 unit tests covering:
- AQI conversion boundary values (PM2.5 → AQI)
- API key filter (protected/unprotected paths, valid/invalid keys)
- Rate limit service (per-user limits, independent counters)
- Air Quality API (JSON parsing, error handling)

```bash
mvn test
```
