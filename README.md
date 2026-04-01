# AQI Notification Service

Real-time air quality monitoring backend — delivers AI-generated health alerts via LINE Messaging API, streams live AQI data to a Next.js dashboard, and stores user data in Firebase Firestore.

**Live demo:** [Dashboard](https://aqi-map-short-paper.onrender.com) · [API Health](https://api-short-paper.onrender.com/actuator/health)

---

## Architecture

```
┌─────────────┐    location/text     ┌──────────────────────────────────────┐
│  LINE App   │ ──────────────────► │           Spring Boot (8082)          │
└─────────────┘                     │                                        │
                                    │  ┌─────────────┐  ┌────────────────┐  │
                                    │  │LineController│  │ AqiScheduler   │  │
                                    │  │ (webhook)   │  │ (hourly cron)  │  │
                                    │  └──────┬──────┘  └───────┬────────┘  │
                                    │         │                  │           │
                                    │  ┌──────▼──────────────────▼────────┐  │
                                    │  │           Services                │  │
                                    │  │  IntentService  (Claude AI)       │  │
                                    │  │  ClaudeService  (Claude AI)       │  │
                                    │  │  AqiAlertService                  │  │
                                    │  │  SseBroadcastService              │  │
                                    │  │  RateLimitService                 │  │
                                    │  └──────┬───────────────┬────────────┘  │
                                    └─────────┼───────────────┼───────────────┘
                                              │               │
                          ┌───────────────────┼───┐    ┌──────▼──────────────┐
                          │  External APIs    │   │    │  Firebase Firestore  │
                          │                  ▼   │    │  - users             │
                          │  Google Air Quality  │    │  - locations         │
                          │  Claude AI (Haiku)   │    │  - userProfiles      │
                          │  LINE Messaging API  │    └─────────────────────┘
                          └──────────────────────┘
                                              │
                                    ┌─────────▼──────────┐
                                    │   SSE Stream        │
                                    │  /api/v1/sse/       │
                                    │  aqi-stream         │
                                    └─────────┬───────────┘
                                              │
                                    ┌─────────▼──────────┐
                                    │  Next.js Dashboard  │
                                    │  (Leaflet map)      │
                                    └────────────────────┘
```

---

## Tech Stack

| Layer | Technology |
|---|---|
| Backend | Spring Boot 2.7.18, Java 17 |
| Database | Firebase Firestore |
| AI | Claude claude-haiku-4-5-20251001 (Anthropic) |
| Messaging | LINE Messaging API |
| Air Quality | Google Air Quality API |
| Frontend | Next.js 16, Leaflet.js, Tailwind CSS |
| CI/CD | GitHub Actions |
| Deployment | Render (Docker) |
| Monitoring | Spring Actuator, UptimeRobot |

---

## Features

- **LINE Bot** — Users send location via LINE, receive AI-generated AQI health alerts personalized to their health profile
- **Intent Detection** — Claude AI classifies user messages into 6 intents (AQI_NOW, SET_PROFILE, DELETE_PROFILE, etc.)
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
| `CLAUDE_API_KEY` | Anthropic Claude API key |
| `INTERNAL_API_KEY` | API key for protected endpoints |
| `FRONTEND_URL` | Comma-separated allowed CORS origins |
| `GOOGLE_APPLICATION_CREDENTIALS` | Path to Firebase service account JSON |

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
