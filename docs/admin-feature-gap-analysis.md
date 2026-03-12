# Starjamz Admin Feature Gap Analysis

> **Produced:** 2026-03-12
> **Methodology:** Every Java source file across all nine implemented microservices was read. Features are mapped to actual classes and methods in the codebase. Gap assessments are based solely on what was found in the code — no guesses.

---

## Codebase State Summary

| Service | Admin Endpoints | Notable Gaps |
|---|---|---|
| AdminService | **0** — no controller exists | `AdminSettingsServiceImpl` only implements `createSettings()` — 7 of 8 interface methods are stubs |
| UserService | **0** | Only `POST /signup` exists; no login, no admin user management |
| FeedService | **0** | Rich read/follow/trending API but no operator controls |
| NotificationService | **0** | User inbox only; no admin broadcast or override |
| PaymentService | **0** | Full Stripe service layer (`StripeService`) but **no REST controller at all** |
| UploadService | **1** (`GET /api/upload/{id}`) | No moderation, takedown, or bulk action endpoints |
| MusicService | **0** admin | Audio streaming only (`GET /stream/{filename}`) |
| LikeService | **0** | All `LikeServiceImpl` methods are empty stubs |
| GatewayService | — | **`SecurityConfig` permits ALL requests — zero authentication** |
| MediaService | `DELETE /api/streams/{id}` | Sessions in-memory only; no persistence or monitoring |
| PlaylistService | — | Empty stub — no Java source files exist |

---

## Domain 1: User Management

### 1.1 Account Suspension / Ban / Reinstatement

| | |
|---|---|
| **Touches** | `UserServiceImpl` (Aerospike `users` set, ClickHouse `user_events`), `FeedFanoutService`, `FollowGraphService` |
| **Gap** | **Build from scratch.** `UserModel` has no `status`, `suspendedAt`, or `banReason` field. No admin endpoint exists anywhere. |
| **Priority** | **Critical** |
| **Implementation** | Add `status` enum bin (ACTIVE / SUSPENDED / BANNED) to Aerospike `users` set. New `AdminUserController` in AdminService: `POST /admin/users/{id}/suspend`, `POST /admin/users/{id}/ban`, `POST /admin/users/{id}/reinstate`. Gateway Spring Security filter must reject requests from SUSPENDED/BANNED accounts. Log each action to ClickHouse `admin_audit_log` table. |

---

### 1.2 Account Deletion / GDPR Erasure

| | |
|---|---|
| **Touches** | `UserServiceImpl.deleteUser()`, Aerospike sets (`users`, `email_idx`, `phone_idx`, `confirmations`, `affinity:{userId}`, `user_prefs:{userId}`, `feed:{userId}`, `follows:{userId}`, `followers:{userId}`), ClickHouse (`users`, `user_events`), `FollowGraphService`, `UserNotificationRepository` (PostgreSQL `user_notifications`), `UploadRecordRepository` (JPA + ScyllaDB), S3 (`S3Config`) |
| **Gap** | **Partially exists, mostly scratch.** `UserService.deleteUser(UUID)` removes the Aerospike `users` record and logs DELETED to ClickHouse. It does **not** cascade to: follow graph bins, feed bins, affinity bins, notification prefs, PostgreSQL notifications, ScyllaDB upload records, or S3 assets. |
| **Priority** | **Critical** |
| **Implementation** | Create `GdprErasureService` orchestrating deletion across all Aerospike sets (namespace `starjamz` + `fetio`), PostgreSQL tables, ScyllaDB `upload_records_by_user`, S3 objects (via `TransferManager`), and Kafka tombstone events. Expose `DELETE /admin/users/{id}/gdpr-erasure` with confirmation token. Log full erasure receipt. |

---

### 1.3 Role & Permission Management

| | |
|---|---|
| **Touches** | `GatewayService` (`SecurityConfig`), `AdminService`, `UserModel` (Aerospike `users` set) |
| **Gap** | **Build from scratch.** `UserModel` has no roles or permissions field. `GatewayService.SecurityConfig` uses `.anyExchange().permitAll()` — the entire platform is unauthenticated. |
| **Priority** | **Critical** |
| **Implementation** | Add `roles` list bin to Aerospike `users` set. Replace `SecurityConfig` with JWT/OAuth2 `SecurityWebFilterChain`. Create roles: `ROLE_ADMIN`, `ROLE_MODERATOR`, `ROLE_SUPPORT`. Protect all `/admin/**` routes. The `AdminService` needs its own `SecurityConfig`. |

---

### 1.4 Identity Verification & Fraud Flagging

| | |
|---|---|
| **Touches** | `UserServiceImpl` (Aerospike `users`), `StripeService.handleWebhookEvent()` |
| **Gap** | **Build from scratch.** No `verificationStatus` or `fraudFlags` field in `UserModel`. Signup captures IP and User-Agent but no fraud scoring exists. |
| **Priority** | **High** |
| **Implementation** | Add `verificationStatus` (UNVERIFIED / PENDING / VERIFIED) and `fraudFlags` list bins to Aerospike `users`. Admin endpoint `POST /admin/users/{id}/flag-fraud`. Wire `StripeService.handleWebhookEvent()` to set fraud flag on `charge.dispute.created`. |

---

### 1.5 Forced Password Reset & Session Invalidation

| | |
|---|---|
| **Touches** | `UserServiceImpl`, `Prep.hashPassword()` / `verifyPassword()`, `ConfirmationCode` |
| **Gap** | **Build from scratch.** `Prep.hashPassword()` and `verifyPassword()` are implemented (Blake3 + salt) but there is no login endpoint, session storage, or password reset flow. |
| **Priority** | **High** |
| **Implementation** | Add `passwordVersion` (incrementing int) bin to Aerospike `users`. JWT tokens embed `passwordVersion`; mismatch invalidates session. Add `sessionRevocationAt` timestamp bin. Admin endpoint `POST /admin/users/{id}/force-password-reset` — increments `passwordVersion` and triggers confirmation code dispatch via existing `ConfirmationCode.dispatch()`. |

---

### 1.6 Admin Impersonation (Support Investigation)

| | |
|---|---|
| **Touches** | `GatewayService`, `AdminService` |
| **Gap** | **Build from scratch.** No impersonation mechanism exists. |
| **Priority** | **Medium** |
| **Implementation** | Issue short-lived (15 min) impersonation JWTs via `POST /admin/users/{id}/impersonate`. Embed `impersonatedBy` claim. All downstream requests under this token are logged to `admin_audit_log`. Requires `ROLE_SUPER_ADMIN`. |

---

## Domain 2: Content Moderation

### 2.1 Upload Review Queue (Flag, Approve, Reject, Take Down)

| | |
|---|---|
| **Touches** | `UploadController`, `UploadServiceImpl`, `UploadRecord` (JPA entity), `UploadRecordRepository`, `S3Config` (AWS `TransferManager`) |
| **Gap** | **Build from scratch.** `UploadRecord` has no `moderationStatus` field. `UploadController` has no moderation endpoints. Content goes directly to S3 on upload with no review gate. |
| **Priority** | **Critical** |
| **Implementation** | Add `moderationStatus` enum column (PENDING / APPROVED / REJECTED / TAKEN_DOWN) plus `moderatorId`, `moderationAt`, `moderationNotes` to `UploadRecord` JPA entity. New `AdminModerationController`: `GET /admin/uploads/queue`, `POST /admin/uploads/{id}/approve`, `POST /admin/uploads/{id}/reject`, `POST /admin/uploads/{id}/takedown` (sets status + deletes S3 object). Update ScyllaDB schema accordingly. |

---

### 2.2 DMCA & Copyright Takedown Workflow

| | |
|---|---|
| **Touches** | `UploadServiceImpl`, `UploadRecord`, S3 assets, `NotificationDeliveryService` (publish to `notification.event`) |
| **Gap** | **Build from scratch.** No DMCA model, workflow state machine, or notification mechanism exists. |
| **Priority** | **High** |
| **Implementation** | New `DmcaTakedownRequest` JPA entity (claimant, trackId/uploadId, submittedAt, status: RECEIVED / UNDER_REVIEW / ACTIONED / COUNTER_CLAIMED). Admin endpoints: `POST /admin/dmca`, `GET /admin/dmca/{id}`, `POST /admin/dmca/{id}/action`. On ACTIONED: set `UploadRecord.moderationStatus = TAKEN_DOWN`, quarantine S3 object, publish `notification.event` Kafka message to notify uploader. |

---

### 2.3 Artist Verification & Badge Management

| | |
|---|---|
| **Touches** | `UserModel` (Aerospike `users` set), `FeedFanoutService` (propagates actor info into `FeedEvent`) |
| **Gap** | **Build from scratch.** No `verifiedArtist` flag or `badgeType` in `UserModel`. |
| **Priority** | **High** |
| **Implementation** | Add `verifiedArtist` boolean and `badgeType` string bins to Aerospike `users` set. Admin endpoints: `POST /admin/users/{id}/verify-artist`, `DELETE /admin/users/{id}/verify-artist`. `FeedFanoutService` should propagate badge into `FeedEvent.actorDisplayName` or a new `actorBadge` field at fan-out time. |

---

### 2.4 Explicit Content Flagging & Age-Gate

| | |
|---|---|
| **Touches** | `UploadRecord` (JPA), `MusicStreamModel` (Cassandra), `FeedEvent`, `FeedRankingService`, `FeedReadService` |
| **Gap** | **Build from scratch.** Neither `UploadRecord` nor `MusicStreamModel` have explicit content or age-rating fields. |
| **Priority** | **High** |
| **Implementation** | Add `isExplicit` boolean and `ageRating` enum to `UploadRecord` (JPA) and `MusicStreamModel` (Cassandra). Admin endpoint `POST /admin/uploads/{id}/set-explicit`. `FeedReadService.scanFeedSet()` should filter explicit events for users without age verification. `FeedRankingService.rank()` should suppress explicit content below age-gate threshold. |

---

### 2.5 Bulk Content Actions

| | |
|---|---|
| **Touches** | `UploadRecordRepository` (JPA), ScyllaDB `upload_records_by_user`, `S3Config`, `FeedFanoutService` |
| **Gap** | **Build from scratch.** No bulk operations exist. |
| **Priority** | **Medium** |
| **Implementation** | Admin endpoints: `POST /admin/users/{id}/takedown-all-uploads` (queries ScyllaDB `upload_records_by_user`, sets TAKEN_DOWN, batch-deletes S3 objects), `POST /admin/uploads/bulk-takedown?genre=X`. Use `@Async("feedFanoutExecutor")` with a job-status tracker. Emit Kafka events to trigger feed quarantine via `QuarantineService` (see 4.4). |

---

## Domain 3: Gift & Payment Economy

### 3.1 Gift Transaction Ledger Visibility & Search

| | |
|---|---|
| **Touches** | `PaymentService.getPaymentsByStatus()`, `PaymentRecord` (Cassandra `@Table("payment_records")`), `PaymentRecordRepository` (CassandraRepository), `StripeService` |
| **Gap** | **Partially exists / REST surface missing.** `PaymentService.getPaymentsByStatus()` queries Cassandra by status, but there is **no REST controller** in PaymentService — zero endpoints. `PaymentRecord` only has `transactionId` + `status` — no amount, userId, or timestamp. |
| **Priority** | **Critical** |
| **Implementation** | Add `PaymentAdminController` to PaymentService: `GET /admin/payments?userId=&status=&from=&to=&limit=`, `GET /admin/payments/{transactionId}`. Extend `PaymentRecord` with `userId`, `amount`, `currency`, `createdAt` fields. Add ClickHouse or PostgreSQL immutable transaction ledger for analytics queries. |

---

### 3.2 Refund & Dispute Resolution Tooling

| | |
|---|---|
| **Touches** | `StripeService` (has `updatePaymentStatus()` but no `Refund.create()` call), `PaymentRecord`, `PaymentEventListener` |
| **Gap** | **Build from scratch.** `StripeService` wraps Stripe subscriptions and webhooks but has no refund method. No dispute entity exists. |
| **Priority** | **Critical** |
| **Implementation** | Add `StripeService.createRefund(String paymentIntentId, long amount)` wrapping Stripe SDK `Refund.create()`. New `DisputeRecord` entity (disputeId, transactionId, reason, status, createdAt). Admin endpoints: `POST /admin/payments/{id}/refund`, `GET /admin/disputes`, `POST /admin/disputes/{id}/resolve`. Wire `handleWebhookEvent()` to create `DisputeRecord` on Stripe `charge.dispute.created` event. |

---

### 3.3 Fraud Detection Triggers (Velocity Checks, Anomalous Gifting)

| | |
|---|---|
| **Touches** | `ViralMechanicsService.recordGift()` (Aerospike `track_stats` bins: `giftProgress`, `giftThreshold`), `StripeService.handleWebhookEvent()` |
| **Gap** | **Build from scratch.** `recordGift()` tracks unlock progress atomically via Aerospike `operate()` but has no velocity check or fraud trigger. No fraud scoring exists anywhere. |
| **Priority** | **High** |
| **Implementation** | Add hourly gift-count velocity check in `recordGift()`: TTL-windowed counter in Aerospike. Threshold breach → set `fraudFlag` bin on `UserModel` + publish `notification.event` with custom `FRAUD_ALERT` type. Add Stripe Radar integration in `StripeService`. Admin endpoint `GET /admin/fraud/alerts`. |

---

### 3.4 Royalty Ledger Reconciliation & Manual Adjustment

| | |
|---|---|
| **Touches** | `PaymentService`, `UploadRecord` (track ownership via `uploadedBy` field), `StatsUpdateConsumer` (Aerospike `track_stats` counters) |
| **Gap** | **Build from scratch.** No royalty model or ledger exists anywhere in the codebase. |
| **Priority** | **High** |
| **Implementation** | New `RoyaltyLedger` entity in PaymentService (artistId, trackId, period, playCount, giftRevenue, royaltyAmount, status: PENDING/RECONCILED/PAID). `@Scheduled` job to flush from Aerospike `track_stats` counters + Cassandra gift records into the ledger. Admin endpoints: `GET /admin/royalties?artistId=&period=`, `POST /admin/royalties/{id}/adjust`, `POST /admin/royalties/reconcile`. |

---

### 3.5 Payout Scheduling & Hold Management

| | |
|---|---|
| **Touches** | `StripeService` (has subscription + payment method management), PaymentService |
| **Gap** | **Build from scratch.** No payout scheduling or hold mechanism exists. |
| **Priority** | **High** |
| **Implementation** | New `PayoutSchedule` entity (artistId, frequency: WEEKLY/MONTHLY, minThreshold, stripeConnectAccountId). Admin endpoints: `GET /admin/payouts`, `POST /admin/payouts/{artistId}/hold`, `POST /admin/payouts/{artistId}/release`. Wire to Stripe Connect transfers. Integrates with `RoyaltyLedger` from 3.4. |

---

## Domain 4: Feed & Virality Controls

### 4.1 Pin / Suppress / Remove Feed Items

| | |
|---|---|
| **Touches** | `FeedReadService` (Aerospike `feed:{userId}` scan, `stream_feed_pin:{userId}` read), `FeedFanoutService` |
| **Gap** | **Partially exists / admin surface missing.** `FeedReadService.pinLivestreams()` already reads `stream_feed_pin:{userId}` from Aerospike and pins live events at the top of the feed — the infrastructure is proven. No admin endpoint exists to write suppression entries or manual non-livestream pins. |
| **Priority** | **High** |
| **Implementation** | Add Aerospike set `feed_suppress` keyed by `eventId` (TTL-based). Admin endpoints: `POST /admin/feed/pin?eventId=&userId=`, `POST /admin/feed/suppress?eventId=`, `DELETE /admin/feed/events/{eventId}` (removes from all fan-out bins via Aerospike scan + write to `feed_suppress`). Update `FeedReadService.scanFeedSet()` to check `feed_suppress` and skip suppressed events. |

---

### 4.2 Override Viral Ranking Signals

| | |
|---|---|
| **Touches** | `FeedRankingService.rank()`, `ViralMechanicsService`, Aerospike `track_stats:{trackId}` (bins: `isBuzzing`, `isNew`, `giftThreshold`, `isLocked`) |
| **Gap** | **Build from scratch.** Ranking is fully algorithmic. Bins `isBuzzing`, `isNew`, etc. are only set by business logic — no operator override mechanism. |
| **Priority** | **Medium** |
| **Implementation** | Add `rankBoostOverride` and `rankSuppressOverride` float bins to Aerospike `track_stats`. Admin endpoints: `POST /admin/tracks/{trackId}/rank-boost?factor=`, `POST /admin/tracks/{trackId}/rank-suppress`. `FeedRankingService.rank()` should read and apply these multipliers. |

---

### 4.3 Manage REPOSTED / REMIXED / GIFTED Event Visibility

| | |
|---|---|
| **Touches** | `ViralMechanicsService.handleRepost()` (builds `repostLineage`), `FeedFanoutService.fanOutEngagement()`, `FeedEvent` (`EventType.TRACK_REPOSTED`, `repostLineage` list) |
| **Gap** | **Build from scratch.** No admin can suppress or remove repost chain events. `repostLineage` UUIDs are stored in Aerospike feed bins indefinitely (up to 72h TTL) with no cleanup hook. |
| **Priority** | **Medium** |
| **Implementation** | Admin endpoint `POST /admin/feed/events/{eventId}/suppress-repost-chain` — traverse `repostLineage` UUID list, write each as suppressed to `feed_suppress` Aerospike set. `POST /admin/tracks/{trackId}/disable-gifting` — sets `giftThreshold=0` in `track_stats` via `aerospike.put()`. |

---

### 4.4 Quarantine Content Across All Fan-out Queues

| | |
|---|---|
| **Touches** | `FeedFanoutService`, Aerospike `feed:{userId}` (all follower bins), `TrendingService` (Aerospike `trending_tracks` sorted map), `NotificationEventConsumer` |
| **Gap** | **Build from scratch.** No quarantine mechanism exists. Content written via fan-out to potentially thousands of follower feed bins cannot be recalled en masse. |
| **Priority** | **High** |
| **Implementation** | New `QuarantineService` in FeedService: (1) writes `eventId` to `feed_suppress` Aerospike set (TTL-based — `FeedReadService` filters it out on every read), (2) removes `trackId` from `trending_tracks` Aerospike sorted map via `MapOperation.removeByKey()`, (3) publishes `content.quarantined` Kafka event so `NotificationEventConsumer` can suppress related notifications. This approach avoids the cost of scanning all follower feed bins. |

---

## Domain 5: Streaming & Infrastructure

### 5.1 Active Stream Monitoring Dashboard

| | |
|---|---|
| **Touches** | `MediaStreamController` (`GET /api/streams`), `StreamSessionManager.all()`, `StreamSession` (fields: `id`, `type`, `startedAt`, `status`, `sourcePath`) |
| **Gap** | **Partially exists.** `GET /api/streams` returns all active `StreamSession` objects from `StreamSessionManager.all()`. However sessions are **in-memory only** — lost on service restart. No persistence, viewer count, peak viewers, or node attribution. |
| **Priority** | **High** |
| **Implementation** | Persist `StreamSession` to PostgreSQL or ScyllaDB on `StreamSessionManager.register()`. Add `hostNode`, `viewerCount`, `peakViewerCount` to `StreamSession`. Admin `GET /admin/streams` aggregates from DB (survives restarts). `StreamSessionManager.evictStaleSessions()` already runs every 60s (`@Scheduled fixedDelay=60000`) — extend it to sync status to DB. |

---

### 5.2 Force-Terminate Active GStreamer Session

| | |
|---|---|
| **Touches** | `MediaStreamController.DELETE /api/streams/{id}`, `StreamSessionManager.stop()` (calls `Pipeline.setState(State.NULL)`) |
| **Gap** | **Mostly exists.** `DELETE /api/streams/{id}` → `StreamSessionManager.stop()` → GStreamer `Pipeline.setState(State.NULL)` works. However the endpoint is **unprotected** (no auth) and has no audit trail. |
| **Priority** | **High** |
| **Implementation** | Add `ROLE_ADMIN` or `ROLE_MODERATOR` security to `DELETE /api/streams/{id}`. Log to `admin_audit_log`. Optionally publish `livestream.event` Kafka message with `LIVESTREAM_ENDED` type so `FeedFanoutConsumer` can update feed events that reference this stream. |

---

### 5.3 Stem Unlock Tier Overrides (Testing / Support)

| | |
|---|---|
| **Touches** | `ViralMechanicsService` — Aerospike `track_stats:{trackId}` bins: `isLocked` (0/1), `giftThreshold`, `giftProgress` |
| **Gap** | **Build from scratch.** The gift-unlock data already exists in Aerospike (written by `recordGift()` and `unlockTrack()`), but no admin endpoint exists to override these values. |
| **Priority** | **Medium** |
| **Implementation** | Admin endpoints: `POST /admin/tracks/{trackId}/unlock-override` (writes `isLocked=0` to Aerospike), `POST /admin/tracks/{trackId}/set-gift-threshold?threshold=N` (for testing/support overrides). Log to `admin_audit_log`. |

---

### 5.4 GStreamer Pipeline Health Dashboard

| | |
|---|---|
| **Touches** | `GStreamerPipelineFactory` (creates AUDIO_FILE / VIDEO_FILE / LIVE_AUDIO / LIVE_VIDEO pipelines), `StreamSessionManager`, `StreamSession` (fields: `status`, `errorMessage`) |
| **Gap** | **Build from scratch.** `StreamSession.status` (PENDING/PLAYING/PAUSED/STOPPED/ERROR) and `errorMessage` are populated but never surfaced to operations. No health aggregation exists. |
| **Priority** | **Medium** |
| **Implementation** | Admin endpoint `GET /admin/streams/health` — counts sessions by `StreamStatus`, lists ERROR sessions with `errorMessage`, reports GStreamer library version via `Gst.getVersionString()`. Add `@Scheduled` metrics emission to Spring Boot Actuator (`/actuator/metrics`) via `MeterRegistry`. |

---

### 5.5 S3 Storage Quota Visibility per Artist

| | |
|---|---|
| **Touches** | `UploadServiceImpl`, `UploadRecord` (fields: `fileSize`, `uploadedBy`, `s3Bucket`, `s3Key`), ScyllaDB `upload_records_by_user` table |
| **Gap** | **Build from scratch.** `UploadRecord.fileSize` (long, bytes) exists but there is no aggregation or admin endpoint to view storage consumption per artist. |
| **Priority** | **Medium** |
| **Implementation** | Admin endpoints: `GET /admin/storage/by-artist?artistId=` (sums `fileSize` from ScyllaDB `upload_records_by_user` table), `GET /admin/storage/top-artists?limit=20` (ranked by total bytes). Consider adding a ClickHouse materialized view for real-time aggregation at scale. |

---

## Domain 6: Stem Economy

> **Note:** No stem economy code exists anywhere in the codebase. All features below are build-from-scratch. They should be designed in parallel with the stem economy feature itself.

### 6.1 Stem Session Monitoring
| **Priority** | **Medium** — blocked on stem economy implementation |
|---|---|
| **Implementation** | Use `StreamSessionManager`-style in-memory + DB-backed session tracking from day one. Admin endpoint: `GET /admin/stem-sessions`. |

### 6.2 Force-Close Stem Session
| **Priority** | **Medium** |
|---|---|
| **Implementation** | Mirror `MediaStreamController.DELETE /api/streams/{id}` pattern with `ROLE_ADMIN` + audit log from day one. |

### 6.3 Override Stem Tier Unlock State
| **Priority** | **Medium** |
|---|---|
| **Implementation** | Build on `ViralMechanicsService` Aerospike bin manipulation pattern. Admin endpoint: `POST /admin/stem/{trackId}/{userId}/unlock-override`. |

### 6.4 Demucs Sidecar Job Queue Visibility
| **Priority** | **Low** |
|---|---|
| **Implementation** | If Demucs jobs flow through Kafka, DLQ inspection (see 8.4) covers this. Add `GET /admin/jobs/stem-processing` if a dedicated job table is used. |

### 6.5 Remix Card Moderation
| **Priority** | **Low** |
|---|---|
| **Implementation** | Extend `AdminModerationController` (from 2.1). Link remix `FeedEvent` to its source track via existing `FeedEvent.originalActorId` and `repostLineage` fields. |

---

## Domain 7: Analytics & Reporting

### 7.1 Platform-Wide Play Counts, Gift Volume, and Remix Activity

| | |
|---|---|
| **Touches** | `StatsUpdateConsumer` (increments Aerospike `track_stats` bins atomically), `ViralMechanicsService.recordGift()`, ClickHouse `user_events` table (exists, written by `UserServiceImpl.logEvent()`) |
| **Gap** | **Build from scratch (admin surface).** Per-track counters exist in Aerospike. ClickHouse `user_events` has user lifecycle events only — no play/gift/remix event schema. |
| **Priority** | **High** |
| **Implementation** | Add ClickHouse tables: `play_events` (userId, trackId, timestamp), `gift_events` (senderId, recipientId, trackId, amount, timestamp), `remix_events` (reposterActorId, originalActorId, trackId, timestamp). New `AnalyticsController` in AdminService: `GET /admin/analytics/platform?from=&to=` — queries ClickHouse with time-bucketed aggregations. |

---

### 7.2 Per-Artist Dashboards

| | |
|---|---|
| **Touches** | `UserModel` (`followCount`, `likes`, `shares` fields), ClickHouse `user_events`, Aerospike `track_stats:{trackId}`, ScyllaDB `upload_records_by_user`, `FollowGraphService.getFollowerCount()` |
| **Gap** | **Build from scratch.** No per-artist aggregate view exists. |
| **Priority** | **High** |
| **Implementation** | Admin endpoint `GET /admin/analytics/artists/{artistId}?period=30d` — joins: stream count from Aerospike `track_stats`, upload count from ScyllaDB `upload_records_by_user`, gift revenue from ClickHouse `gift_events`, follower count via `FollowGraphService.getFollowerCount()`, royalty summary from `RoyaltyLedger` (3.4). |

---

### 7.3 Content Performance Reports (CSV Export)

| | |
|---|---|
| **Touches** | ClickHouse, `UploadRecord`, Aerospike `track_stats` |
| **Gap** | **Build from scratch.** |
| **Priority** | **Medium** |
| **Implementation** | Admin endpoint `GET /admin/reports/content-performance?from=&to=&format=csv` — streams ClickHouse query results as CSV via Spring `StreamingResponseBody`. Columns: trackId, title, artistId, playCount, likeCount, repostCount, giftRevenue, uploadDate. |

---

### 7.4 Anomaly Alerting

| | |
|---|---|
| **Touches** | `StatsUpdateConsumer`, `ViralMechanicsService`, `StripeService.handleWebhookEvent()`, `NotificationType.TRENDING_ARTIST_ALERT` (already defined) |
| **Gap** | **Build from scratch.** |
| **Priority** | **Medium** |
| **Implementation** | Add threshold-based checks in `StatsUpdateConsumer` and `handleWebhookEvent()`. Publish `notification.event` Kafka message using existing `NotificationType.TRENDING_ARTIST_ALERT` or a new `ANOMALY_ALERT` type. Admin endpoint `GET /admin/alerts/active`. |

---

## Domain 8: System Health & Operations

### 8.1 Service Health Dashboard

| | |
|---|---|
| **Touches** | All services (Spring Boot Actuator `GET /actuator/health`), `GatewayService.GatewayService` (has `WebClient.Builder` with `@LoadBalanced`) |
| **Gap** | **Partially exists / not aggregated.** Spring Boot Actuator `health` endpoint is available on every service automatically but there is no aggregated view. |
| **Priority** | **High** |
| **Implementation** | Add `management.endpoints.web.exposure.include=health,info,metrics` to all `application.yml` files. Add admin aggregation endpoint `GET /admin/health` in GatewayService — fans out to each service's `/actuator/health` via `GatewayService.proxyRequest()` or dedicated `WebClient` calls, returns composite status. |

---

### 8.2 Aerospike Namespace/Set Monitoring

| | |
|---|---|
| **Touches** | `AerospikeConfig` beans (FeedService, UserService, NotificationService — all create `IAerospikeClient`) |
| **Gap** | **Build from scratch.** Aerospike client is available but no monitoring is surfaced. |
| **Priority** | **Medium** |
| **Implementation** | Use `IAerospikeClient.infoAny(policy, "namespace/fetio")` and `"namespace/starjamz"` to query namespace stats. Admin endpoint `GET /admin/aerospike/stats` — returns set sizes, record counts, TTL distributions, memory/disk usage. Register as Spring Boot Actuator custom metrics via `MeterRegistry`. |

---

### 8.3 PostgreSQL Query Performance & Slow Query Log

| | |
|---|---|
| **Touches** | `FeedEventLogRepository`, `FollowRepository` (FeedService), `UserNotificationRepository` (NotificationService) |
| **Gap** | **Build from scratch.** No slow query visibility from the application layer. |
| **Priority** | **Medium** |
| **Implementation** | Enable `pg_stat_statements` extension in PostgreSQL. Admin endpoint `GET /admin/db/slow-queries` reads from `pg_stat_statements` view via dedicated admin DataSource (read-only). Expose top-N queries by mean execution time, total calls, rows returned. |

---

### 8.4 Dead Letter Queue Inspection & Replay

| | |
|---|---|
| **Touches** | `FeedFanoutConsumer` (topics: `track.posted`, `track.engaged`, `livestream.event`), `StatsUpdateConsumer` (`track.engaged`), `NotificationEventConsumer` (`notification.event`), `UploadEventPublisher` (publishes `audio-uploads`, `video-uploads`) |
| **Gap** | **Build from scratch.** No `@KafkaListener` error handlers or DLQ configuration exist in any consumer. Failed messages are silently dropped. |
| **Priority** | **High** |
| **Implementation** | Add `DeadLetterPublishingRecoverer` + `SeekToCurrentErrorHandler` to all three consumers. DLQ topic convention: `<topic>.DLT` (e.g., `track.posted.DLT`). Admin endpoints: `GET /admin/kafka/dlq/{topic}` (list failed messages with offset, key, headers), `POST /admin/kafka/dlq/{topic}/replay` (re-publish to original topic), `DELETE /admin/kafka/dlq/{topic}/{offset}` (discard). |

---

### 8.5 Feature Flag Management

| | |
|---|---|
| **Touches** | `AdminSettingsModel` (has `iosOnline`, `androidOnline`, `webOnline`, `comingSoonMode`, `carousel`, `tracksPromotedOnHomePage`, etc.), `AdminSettingsService` (8-method interface), `AdminSettingsServiceImpl` (**only `createSettings()` implemented — 7 methods are stubs**) |
| **Gap** | **Partially exists.** `AdminSettingsModel` already has platform kill-switches and toggles. But `AdminSettingsServiceImpl` implements only `createSettings()`; `updateSettings()`, `toggleIOSOnline()`, `updateLandingPageFeatures()`, etc. are all stubs. No REST controller exposes these. No per-feature flags for stem economy, gift tiers, etc. |
| **Priority** | **High** |
| **Implementation** | Implement all 7 remaining methods in `AdminSettingsServiceImpl`. Add `featureFlags` Map column to `AdminSettingsModel` for dynamic flags (e.g., `stemEconomyEnabled`, `giftTiersEnabled`, `remixFeedEnabled`). Add `AdminSettingsController` with `GET /admin/settings`, `PUT /admin/settings/{id}`, `POST /admin/settings/{id}/toggle-feature?flag=`. All services cache feature flags in Redis (60s TTL). |

---

### 8.6 Scheduled Job Monitoring

| | |
|---|---|
| **Touches** | `StreamSessionManager.evictStaleSessions()` (the only `@Scheduled` job in the codebase, `fixedDelay=60000` ms) |
| **Gap** | **Build from scratch.** Only one scheduled job exists. No job registry, last-run timestamp, or failure alerting. |
| **Priority** | **Medium** |
| **Implementation** | Enable Spring Boot Actuator `scheduledtasks` endpoint (`management.endpoints.web.exposure.include=scheduledtasks`). Add `ScheduledJobRegistry` table with `lastRunAt`, `lastRunStatus`, `lastRunDurationMs`. Admin endpoint `GET /admin/jobs`. |

---

## Domain 9: Audit & Compliance

### 9.1 Full Admin Action Audit Log

| | |
|---|---|
| **Touches** | AdminService and all services touched by admin operations; ClickHouse (existing `user_events` table provides a model) |
| **Gap** | **Build from scratch.** `UserServiceImpl.logEvent()` writes user lifecycle events (CREATED/UPDATED/DELETED) to ClickHouse `user_events` — this is the only audit trail in the system. No `admin_audit_log` table or service exists. |
| **Priority** | **Critical** |
| **Implementation** | Create ClickHouse `admin_audit_log` table: `(eventId UUID, adminUserId UUID, adminUsername String, action String, targetEntityType String, targetEntityId String, previousValue JSON, newValue JSON, requestIp String, userAgent String, occurredAt DateTime)`. New `AuditLogService.record(...)` in AdminService — called from every admin controller method. Admin endpoint `GET /admin/audit-log?adminId=&entityId=&from=&to=` with pagination. |

---

### 9.2 Data Export for Legal/Regulatory Requests

| | |
|---|---|
| **Touches** | `UserServiceImpl` (Aerospike `users`), ClickHouse `user_events`, PostgreSQL `user_notifications`, ScyllaDB `upload_records_by_user`, S3 assets, `ConfirmationCode` (has SES email dispatch already) |
| **Gap** | **Build from scratch.** No data export mechanism exists. |
| **Priority** | **High** |
| **Implementation** | Admin endpoint `POST /admin/users/{id}/data-export` — triggers `@Async` job collecting: Aerospike `users` record, ClickHouse `user_events`, PostgreSQL `user_notifications`, ScyllaDB `upload_records_by_user`, S3 asset manifest. Packages as ZIP, uploads to S3, and sends download link via existing `ConfirmationCode.sendEmail()` (SES already wired). |

---

### 9.3 Retention Policy Enforcement Dashboard

| | |
|---|---|
| **Touches** | Aerospike TTL records (`feed:{userId}` TTL=72h hardcoded in `FeedFanoutService`, `confirmations` TTL=`CONFIRM_TTL=900` in `UserServiceImpl`, `notif_dedup` TTL=`dedupWindowSeconds=3600` in `NotificationDeliveryService`), PostgreSQL `user_notifications` (no retention job exists) |
| **Gap** | **Partially exists (TTLs only).** Aerospike TTLs are in place but hard-coded in service logic. No PostgreSQL retention job. No admin visibility into configured policies. |
| **Priority** | **Medium** |
| **Implementation** | Admin endpoint `GET /admin/retention-policies` — lists all TTL settings sourced from service configs. Add `@Scheduled` job to delete `user_notifications` older than configurable period (default 90 days). Expose `PUT /admin/retention-policies/{policy}?ttlSeconds=` to update without redeploy (stored in AdminSettings). |

---

### 9.4 API Key & OAuth Client Management

| | |
|---|---|
| **Touches** | `GatewayService.SecurityConfig` (currently `.anyExchange().permitAll()`) |
| **Gap** | **Build from scratch.** No OAuth2 authorization server, no API key store, no third-party client management. |
| **Priority** | **High** |
| **Implementation** | Add Spring Authorization Server or integrate Keycloak. New `OAuthClient` entity: `clientId`, `clientSecret` (hashed), `scopes`, `rateLimits`, `owner`, `createdAt`, `lastUsedAt`, `revoked`. Admin endpoints: `GET /admin/oauth-clients`, `POST /admin/oauth-clients`, `DELETE /admin/oauth-clients/{clientId}/revoke`. |

---

## Summary

### Gap Count by Priority

| Priority | Count | Features |
|---|---|---|
| **Critical** | **8** | 1.1 Suspension/Ban, 1.2 GDPR Erasure, 1.3 Auth/Roles, 2.1 Upload Moderation Queue, 3.1 Payment Ledger, 3.2 Refund/Dispute, 9.1 Audit Log, + Gateway Authentication |
| **High** | **19** | 1.4 Fraud Flagging, 1.5 Password Reset, 2.2 DMCA, 2.3 Artist Verification, 2.4 Explicit Content, 3.3 Velocity Checks, 3.4 Royalty Ledger, 3.5 Payout Scheduling, 4.1 Feed Pin/Suppress, 4.4 Quarantine, 5.1 Stream Monitoring, 5.2 Force-Terminate Stream, 7.1 Platform Analytics, 7.2 Per-Artist Dashboards, 8.1 Health Dashboard, 8.4 DLQ Inspection, 8.5 Feature Flags, 9.2 Data Export, 9.4 OAuth Client Mgmt |
| **Medium** | **14** | 1.6 Impersonation, 2.5 Bulk Actions, 4.2 Rank Override, 4.3 Repost Mgmt, 5.3 Stem Unlock Override, 5.4 GStreamer Health, 5.5 S3 Quota, 6.1–6.3 Stem Admin (3 items), 7.3 CSV Export, 7.4 Anomaly Alerting, 8.2 Aerospike Monitoring, 8.3 Slow Query Log, 8.6 Job Monitoring, 9.3 Retention Policy |
| **Low** | **2** | 6.4 Demucs Job Queue, 6.5 Remix Card Moderation |
| **Total** | **43** | |

---

### Top 5 Highest-Priority Items to Build First

**1. Authentication & Authorization at the Gateway** (`GatewayService.SecurityConfig`)
`SecurityConfig` currently uses `.anyExchange().permitAll()` — the entire platform has zero authentication. Every admin endpoint built will be publicly accessible until this is fixed. Add JWT/OAuth2 `SecurityWebFilterChain`, protect `/admin/**` routes, add `roles` bin to `UserModel`.

**2. Admin Audit Log** (new ClickHouse `admin_audit_log` + `AuditLogService`)
Must be in place before any other admin feature. Every subsequent admin action needs a tamper-evident record. Model it on `UserServiceImpl.logEvent()` which already writes to ClickHouse `user_events` successfully.

**3. Account Suspension / Ban / Reinstatement** (new `AdminUserController` + `UserModel` `status` bin)
Core safety control. Without it, the platform cannot stop a bad actor. `UserServiceImpl` has the Aerospike write patterns needed — add `status` bin to `users` set and a new controller.

**4. Upload Content Moderation Queue** (`UploadRecord` + new `AdminModerationController`)
Platform cannot launch without reviewing content before it reaches all followers. `UploadRecord` needs `moderationStatus`; fan-out in `FeedFanoutConsumer` should gate on APPROVED status.

**5. Payment Admin Controller + Refund Tooling** (`PaymentAdminController` + `StripeService.createRefund()`)
`PaymentService` has a complete Stripe service layer with no REST controller at all. The operations team has no way to view transactions, process refunds, or investigate disputes. This is the highest-value leverage point — the service backend is already built.

---

### Suggested Implementation Phases

```
Phase 1 — Launch Blockers (Critical)
  1. JWT/OAuth2 at GatewayService (SecurityConfig replacement)
  2. Admin audit log table + AuditLogService
  3. Account suspension/ban/reinstate API (AdminUserController)
  4. GDPR erasure cascade (GdprErasureService)
  5. Upload moderation queue (AdminModerationController + UploadRecord.moderationStatus)
  6. Payment admin controller + refund tooling (PaymentAdminController + StripeService.createRefund)

Phase 2 — Launch Required (High)
  7.  Artist verification badges
  8.  Explicit content flags + age gate
  9.  Royalty ledger + payout scheduling
  10. Gift fraud velocity checks
  11. DLQ configuration for all Kafka consumers
  12. Feature flag management (complete AdminSettingsServiceImpl + AdminSettingsController)
  13. Service health aggregation endpoint (GET /admin/health)
  14. Data export for legal requests (async ZIP via SES)
  15. OAuth client management

Phase 3 — 90-Day Window (Medium)
  16. Feed pin/suppress/quarantine (QuarantineService + feed_suppress Aerospike set)
  17. Viral rank override for tracks
  18. GStreamer pipeline health dashboard
  19. S3 quota by artist
  20. Platform-wide analytics (ClickHouse schema + AnalyticsController)
  21. Per-artist dashboards
  22. Aerospike namespace monitoring endpoint
  23. Retention policy dashboard + enforcement job
  24. Admin impersonation with audit trail
```
