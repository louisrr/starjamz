# CLAUDE.md — Starjamz Codebase Guide

This document provides AI assistants with a comprehensive understanding of the Starjamz
repository: its architecture, conventions, workflows, and key implementation details.

---

## Project Overview

**Starjamz / Fetio** is a cloud-based music streaming and peer-to-peer social gifting platform.
It targets musicians, podcasters, educators, event organizers, fitness instructors, charities,
and gaming communities. The backend is composed of independent Spring Boot services behind a
single API gateway; the frontend is a Next.js 14 application.

The platform includes a **social music feed layer** powered by AerospikeDB: a follow graph,
fan-out-on-write activity feeds, viral mechanics (reposts, trending, First-48, gift-to-unlock),
per-user feed ranking, digest cards, livestream feed integration, and a push notification
infrastructure via NotificationService.

---

## Repository Layout

```
starjamz/
├── GatewayService/       # API gateway (port 8080)
├── AdminService/         # Admin management (port 8083)
├── FeedService/          # Social feed, follow graph, fan-out, ranking (port 8085)
├── LikeService/          # Like/reaction tracking (port 8086)
├── MediaService/         # Video/media processing
├── MusicService/         # Music streaming core (port 8082)
├── NotificationService/  # Push/in-app notifications, dedup, preferences (port 8088)
├── PaymentService/       # Stripe payment integration (port 8081)
├── PlaylistService/      # Playlist management (placeholder, not implemented)
├── UploadService/        # File uploads to S3 / ScyllaDB (port 8087)
├── UserService/          # User auth, profiles, Aerospike + ClickHouse (port 8084)
├── Frontend/
│   └── starjamz/         # Next.js 14 frontend (port 3000)
├── scylla/
│   ├── schema.cql        # ScyllaDB schema for UploadService
│   └── init.sh           # One-shot schema initialization script
├── docker-compose.yml    # Full-stack local orchestration
├── .github/
│   └── workflows/
│       └── gradle.yml    # Java CI (runs Gradle build on push/PR)
└── README.md
```

Each backend service follows the same self-contained layout:
```
<ServiceName>/
├── src/
│   ├── main/
│   │   ├── java/com/play/stream/Starjams/<ServiceName>/
│   │   │   ├── <ServiceName>Application.java   # @SpringBootApplication entry
│   │   │   ├── config/                         # Spring config classes
│   │   │   ├── controller/                     # REST controllers
│   │   │   ├── services/ (or Services/)        # Business logic
│   │   │   ├── models/                         # Data models / entities
│   │   │   ├── dto/                            # Data transfer objects
│   │   │   └── util/                           # Utility classes
│   │   └── resources/
│   │       └── application.yml
│   └── test/java/...
├── build.gradle
└── Dockerfile
```

> **Note:** Package naming is `com.play.stream.Starjams.<ServiceName>` (capital S on Starjams).
> Some services use `Services/` instead of `services/` — follow whichever convention exists in
> the service you are editing.

---

## Tech Stack

### Backend
| Technology | Version | Purpose |
|---|---|---|
| Java | 21 | Runtime (all services target JDK 21) |
| Spring Boot | 3.5.11 | Web framework |
| Spring Cloud | 2025.0.1 | Service discovery, gateway |
| Gradle | 8.x | Build system |
| Eureka | (Netflix OSS) | Service registry |
| Spring Cloud Gateway | — | API routing + load balancing |
| Apache Kafka | 3.6.1 | Async messaging |
| Redis | — | Caching |
| Spring Security + OAuth2 | — | Auth / JWT |
| Stripe Java SDK | 24.17.0-beta.1 | Payments |
| SendGrid Java SDK | 4.10.2 | Transactional email |
| AWS SDK S3 | 1.12.666 | File storage |
| GStreamer (gst1-java-core) | 1.4.0 | Audio/video processing |

### UserService-specific databases
| Database | Purpose |
|---|---|
| Aerospike | Primary user profile store |
| ClickHouse | Analytical / event data |

### UploadService database
| Database | Purpose |
|---|---|
| ScyllaDB 5.4 | Upload record storage (Cassandra-compatible) |

### Frontend
| Technology | Version | Purpose |
|---|---|---|
| Next.js | 14.1.0 | React framework + SSR |
| React | 18 | UI library |
| TypeScript | 5 | Type-safe JavaScript |
| Tailwind CSS | 3.3.0 | Utility-first styling |
| ESLint | — | Linting (`next/core-web-vitals`) |

---

## Service Port Map

| Service | Host Port | Internal Port |
|---|---|---|
| GatewayService | 8080 | 8080 |
| PaymentService | 8081 | 8080 |
| MusicService | 8082 | 8080 |
| AdminService | 8083 | 8080 |
| UserService | 8084 | 8080 |
| FeedService | 8085 | 8080 |
| LikeService | 8086 | 8080 |
| UploadService | 8087 | 8080 |
| NotificationService | 8088 | 8080 |
| Frontend | 3000 | 3000 |
| ScyllaDB | 9042 | 9042 |
| Aerospike | 3000 | 3000 |
| PostgreSQL | 5432 | 5432 |

All backend services internally bind to port `8080` and are differentiated only by the host
port mapping in `docker-compose.yml`.

---

## API Gateway Routes

The GatewayService (`:8080`) strips the first path segment and load-balances via Eureka:

| Incoming path | Target service |
|---|---|
| `/payment/**` | `payment-service` |
| `/music/**` | `music-service` |
| `/admin/**` | `admin-service` |
| `/user/**` | `user-service` |
| `/feed/**` | `feed-service` |
| `/like/**` | `like-service` |
| `/playlist/**` | `playlist-service` |
| `/upload/**` | `upload-service` |
| `/notification/**` | `notification-service` |

---

## Development Workflows

### Running Everything with Docker

```bash
# Build and start all services (including ScyllaDB init)
docker-compose up --build

# Tear down
docker-compose down
```

> **Important:** `docker-compose.yml` does **not** include Eureka, Kafka, or Redis.
> The services expect these to be available externally:
> - Eureka: `http://localhost:8761/eureka/`
> - Kafka: `127.0.0.1:9092`
> - Redis: `localhost:6379`
>
> Without Eureka, service discovery will fail and inter-service routing through the
> gateway will not work.

### Running a Single Backend Service

```bash
cd <ServiceName>
./gradlew bootRun          # Start the service
./gradlew build            # Build JAR + run all tests
./gradlew test             # Run tests only
```

### Running the Frontend

```bash
cd Frontend/starjamz
npm install
npm run dev                # Dev server at http://localhost:3000
npm run build              # Production build
npm run start              # Run production build
npm run lint               # ESLint check
```

---

## Testing

### Backend

Tests live in `src/test/java/` within each service. They use **JUnit 5**.

Services with meaningful test coverage:
- **MusicService**: `MusicStreamingControllerTest`, `MusicStreamingServiceTest`, `AudioStreamerServiceTest`, model tests
- **GatewayService**: `PlaylistGatewayControllerTest`
- **UserService**: `CassandraConfigTest`, `UserServiceImplTest`, config converter tests

Run tests for a service:
```bash
cd <ServiceName>
./gradlew test
```

### Frontend

No test files currently exist. Follow Next.js/Jest conventions when adding tests.

---

## Code Conventions

### Java (Backend)

- **Java version:** Always target Java 21. Do not use preview features without a comment explaining why.
- **Spring annotations:** Use `@Service`, `@RestController` (not `@Controller` for REST), `@Configuration`, `@EnableDiscoveryClient`.
- **Package root:** `com.play.stream.Starjams.<ServiceName>`
- **Layer separation:** Keep HTTP concerns in `controller/`, business logic in `services/`, data models in `models/`, Spring config in `config/`.
- **Service naming:** Each service registers with Eureka as `<name>-service` (all lowercase, hyphenated).
- **YAML config:** Put all application configuration in `src/main/resources/application.yml`. Do not hard-code connection strings.
- **Build:** Use Gradle. Do not add Maven files.

### TypeScript / React (Frontend)

- **Strict mode:** TypeScript strict mode is enabled. Fix type errors — do not use `any` unless unavoidable.
- **Path aliases:** Use `@/` for absolute imports (`tsconfig.json` maps `@/*` → `./src/*` or project root).
- **Routing:** Use the Next.js `pages/` directory (not the `app/` directory).
- **Styling:** Tailwind CSS utility classes. Avoid inline styles.
- **Linting:** Code must pass `npm run lint` before committing.
- **Components:** Write functional components with TypeScript types for props.

### Database

- **ScyllaDB (UploadService):** Schema changes go in `scylla/schema.cql`. Design tables for query patterns (denormalized), not relational normalization. Follow the existing three-table pattern: by ID, by user, by user+type.
- **Aerospike / ClickHouse (UserService):** Config classes are in `config/AerospikeConfig.java` and `config/ClickHouseConfig.java`. Keep connection details in `application.yml`.
- **Keyspace naming:** Use `starjamz_<domain>` (e.g., `starjamz_uploads`).

---

## CI/CD

GitHub Actions workflow: `.github/workflows/gradle.yml`

- Triggers on push to `main` and on pull requests.
- Runs `./gradlew build` (which includes tests).
- **Known discrepancy:** The workflow currently sets up Java 17 but the project targets Java 21.
  When modifying the workflow, update it to use Java 21.

---

## External Dependencies & Secrets

The following credentials must be present in environment variables or `application.yml`
(never commit real secrets):

| Dependency | Where it's used |
|---|---|
| Stripe API key | PaymentService |
| SendGrid API key | UserService (email confirmation) |
| AWS S3 credentials (key, secret, bucket, region) | UploadService |
| Kafka broker address | All async services |
| Eureka server URL | All services |
| Redis connection | Caching-enabled services |
| Aerospike host/port | UserService |
| ClickHouse JDBC URL | UserService |
| Twilio credentials | UserService (SMS confirmation) |
| AWS SES credentials | UserService (transactional email) |

Use placeholder values in committed config files. Real values are injected via environment
variables in deployment.

---

## ScyllaDB Schema Overview

Keyspace: `starjamz_uploads` (replication factor 1, SimpleStrategy — local dev only)

```
upload_records              — primary lookup by upload_id (UUID)
upload_records_by_user      — all uploads for a user, newest first (CLUSTERING ORDER DESC)
upload_records_by_user_type — uploads for a user filtered by media_type (AUDIO | VIDEO)
```

Schema file: `scylla/schema.cql`
Init script: `scylla/init.sh` (run once by the `scylla-init` Docker service)

---

## Key Files Quick Reference

| File | Purpose |
|---|---|
| `docker-compose.yml` | Full local stack orchestration |
| `GatewayService/application.yml` | Route definitions and Eureka config |
| `UserService/src/main/java/.../config/AerospikeConfig.java` | Aerospike connection config |
| `UserService/src/main/java/.../config/ClickHouseConfig.java` | ClickHouse connection config |
| `scylla/schema.cql` | ScyllaDB schema for uploads |
| `Frontend/starjamz/package.json` | Frontend dependencies and scripts |
| `Frontend/starjamz/tsconfig.json` | TypeScript config (path aliases, strict mode) |
| `Frontend/starjamz/tailwind.config.ts` | Tailwind CSS customization |
| `Frontend/starjamz/next.config.mjs` | Next.js config (standalone output, strict mode) |
| `.github/workflows/gradle.yml` | CI pipeline |

---

## Feed & Social Layer Architecture (FeedService)

FeedService is the core social layer. Key packages:

```
FeedService/src/main/java/com/play/stream/Starjams/FeedService/
├── config/          AerospikeConfig, AsyncConfig, KafkaTopics
├── controller/      FollowController, FeedController, TrendingController
├── consumer/        FeedFanoutConsumer (track.posted, track.engaged, livestream.event)
│                    StatsUpdateConsumer (track.engaged — increments counters, scoring)
├── services/        FollowGraphService  — Aerospike Map CDT follow graph
│                    FeedFanoutService   — async fan-out writes to follower feed bins
│                    FeedReadService     — scan + rank + paginate feed
│                    FeedRankingService  — weighted score algorithm (recency/engagement/affinity/viral/diversity)
│                    TrendingService     — decayed trending score, Aerospike sorted maps
│                    ViralMechanicsService — repost chain, buzzing, gift-to-unlock, play streak
│                    AffinityService     — per-user artist affinity EMA
│                    PrivacyService      — activity-sharing privacy controls
├── model/           FeedEvent, EventType, DigestCard, LivestreamCard, TrendingUserCard
├── dto/             TrackPostedEvent, TrackEngagedEvent, FeedPage, PrivacySettingsRequest
├── entity/          Follow (JPA), FeedEventLog (JPA)
└── repository/      FollowRepository, FeedEventLogRepository
```

### Aerospike Namespace: `fetio`

| Set | Key | Purpose |
|---|---|---|
| `feed:{userId}` | `{userId}:{eventId}` | Personal feed bins, TTL=72h |
| `follows:{userId}` | userId | Map CDT: followees → epochMs |
| `followers:{userId}` | userId | Map CDT: followers → epochMs |
| `track_stats:{trackId}` | trackId | Engagement counters (atomic) |
| `trending_tracks` | "global" | Score map for trending |
| `affinity:{userId}` | userId | Artist affinity scores (EMA) |
| `user_prefs:{userId}` | userId | Privacy settings |
| `stream_feed_pin:{userId}` | userId | Active pinned livestream |
| `notif_dedup` | deduplicationKey | TTL-based notification dedup |
| `notif_prefs:{userId}` | userId | Per-type notification opt-out |

### Feed Ranking Score

```
score = (recencyScore   * 0.35)   // e^(-0.001 * ageMinutes)
      + (engagementScore * 0.25)  // log-normalised weighted counters
      + (affinityScore   * 0.20)  // per-user artist affinity [0,1]
      + (viralScore      * 0.15)  // repost velocity in 2h
      + (diversityPenalty * 0.05) // −penalty if actor appears >3× in top 20
```

Bonuses: `isBuzzing` → +0.30; `isNew` (First-48 small creator) → ×2.5; livestream → +10.0.

### Fan-out Policy by Event Type

| Event | Fan-out Rule |
|---|---|
| TRACK_POSTED / REPOSTED / VIDEO_POSTED | All followers, immediately |
| TRACK_LIKED / VIDEO_LIKED | Only if track >50 likes OR actor >200 followers |
| TRACK_PLAYED / VIDEO_VIEWED | Batched into DigestCard (no individual fan-out) |
| ARTIST_FOLLOWED | Discovery nudge card to actor's followers |
| LIVESTREAM_STARTED | All followers, elevated priority, pinned |

### Kafka Topics

| Topic | Producer | Consumer |
|---|---|---|
| `track.posted` | UploadService | FeedFanoutConsumer |
| `track.engaged` | Any service | FeedFanoutConsumer + StatsUpdateConsumer |
| `livestream.event` | MediaService | FeedFanoutConsumer |
| `playlist.event` | PlaylistService | FeedFanoutConsumer |
| `notification.event` | FeedService | NotificationEventConsumer |
| `engagement.counter.flush` | FeedService | (PostgreSQL sync consumer) |

---

## NotificationService

New microservice (port 8088). Consumes `notification.event` from all other services.

**Responsibilities:**
- Deduplication via Aerospike TTL records (`notif_dedup` set)
- User notification preference opt-out (`notif_prefs:{userId}`)
- Persistence to PostgreSQL `user_notifications` table
- Push delivery via Firebase Cloud Messaging (FCM)

**REST API:**
- `GET  /api/v1/users/{userId}/notifications` — paginated inbox
- `GET  /api/v1/users/{userId}/notifications/unread-count`
- `POST /api/v1/users/{userId}/notifications/mark-all-read`

---

## What Is Not Yet Implemented

- **PlaylistService** — directory exists with only a Spring Boot stub, no source code.
- **Frontend pages** — only `index.tsx` (Next.js boilerplate) and a minimal `login.tsx` exist.
- **Embedded infrastructure** — Eureka, Kafka, and Redis are not in `docker-compose.yml`
  (Aerospike and PostgreSQL were added for FeedService/NotificationService).
- **Frontend tests** — no test files exist yet.
- **Popular content blending** — FeedReadService has the blend-ratio logic wired, but
  popular FeedEvent hydration from PostgreSQL/Redis is marked TODO.
- **FCM push dispatch** — NotificationDeliveryService has the delivery hook; actual
  Firebase SDK call requires `FIREBASE_CREDENTIALS_PATH` secret.

When implementing PlaylistService, follow the same Gradle + Spring Boot structure as other services and register it with Eureka as `playlist-service`.
