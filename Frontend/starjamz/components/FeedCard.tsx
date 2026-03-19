import React, { useState } from "react";

// ─── Types mirroring FeedService models ──────────────────────────────────────

export type EventType =
  | "TRACK_POSTED"
  | "TRACK_LIKED"
  | "TRACK_PLAYED"
  | "TRACK_REPOSTED"
  | "TRACK_REMIXED"
  | "VIDEO_POSTED"
  | "VIDEO_LIKED"
  | "VIDEO_VIEWED"
  | "ARTIST_FOLLOWED"
  | "PLAYLIST_CREATED"
  | "PLAYLIST_LIKED"
  | "PLAYLIST_TRACK_ADDED"
  | "PLAYLIST_COLLABORATIVE_JOINED"
  | "PLAYLIST_SHARED"
  | "LIVESTREAM_STARTED_AUDIO"
  | "LIVESTREAM_STARTED_VIDEO"
  | "LIVESTREAM_ENDED"
  | "LIVESTREAM_CLIPPED"
  | "DIGEST"
  | "TRENDING_USER"
  | "POPULAR_TRACK"
  | "POPULAR_VIDEO"
  | "POPULAR_PLAYLIST"
  | "POPULAR_LIVESTREAM";

export interface CollaboratorRef {
  userId: string;
  displayName: string;
  avatarUrl: string | null;
}

export interface FeedEvent {
  cardType?: string;
  eventId: string;
  eventType: EventType;
  actorId: string;
  actorDisplayName: string;
  actorAvatarUrl: string | null;
  // track
  trackId?: string;
  trackTitle?: string;
  trackDuration?: number;
  coverArtUrl?: string;
  audioStreamUrl?: string;
  genre?: string[];
  mood?: string;
  // video
  videoId?: string;
  videoTitle?: string;
  videoThumbnailUrl?: string;
  videoStreamUrl?: string;
  // counters
  playCount?: number;
  likeCount?: number;
  repostCount?: number;
  commentCount?: number;
  // timestamps
  postedAt?: string;
  // viral
  isNew?: boolean;
  isBuzzing?: boolean;
  // gift-to-unlock
  isLocked?: boolean;
  giftThreshold?: number;
  giftProgress?: number;
  // repost
  originalActorId?: string;
  originalActorDisplayName?: string;
  repostCommentText?: string;
  // collaborators
  collaborators?: CollaboratorRef[];
  // livestream
  streamId?: string;
  isLive?: boolean;
  viewerCount?: number;
  streamThumbnailUrl?: string;
  // playlist
  playlistId?: string;
  playlistTitle?: string;
  playlistCoverMosaic?: string[];
}

// ─── Helpers ─────────────────────────────────────────────────────────────────

function fmtCount(n: number | undefined): string {
  if (!n) return "0";
  if (n >= 1_000_000) return `${(n / 1_000_000).toFixed(1)}M`;
  if (n >= 1_000)     return `${(n / 1_000).toFixed(1)}K`;
  return String(n);
}

function fmtDuration(secs: number | undefined): string {
  if (!secs) return "0:00";
  const m = Math.floor(secs / 60);
  const s = secs % 60;
  return `${m}:${s.toString().padStart(2, "0")}`;
}

function timeAgo(isoString: string | undefined): string {
  if (!isoString) return "";
  const diff = Date.now() - new Date(isoString).getTime();
  const mins = Math.floor(diff / 60_000);
  if (mins < 1)  return "just now";
  if (mins < 60) return `${mins}m ago`;
  const hrs = Math.floor(mins / 60);
  if (hrs < 24)  return `${hrs}h ago`;
  return `${Math.floor(hrs / 24)}d ago`;
}

// ─── Sub-components ───────────────────────────────────────────────────────────

function Avatar({
  url,
  name,
  size = 40,
}: {
  url: string | null;
  name: string;
  size?: number;
}) {
  const initials = name
    .split(" ")
    .slice(0, 2)
    .map((w) => w[0])
    .join("")
    .toUpperCase();

  return url ? (
    // eslint-disable-next-line @next/next/no-img-element
    <img
      src={url}
      alt={name}
      width={size}
      height={size}
      className="rounded-full object-cover flex-shrink-0"
      style={{ width: size, height: size }}
    />
  ) : (
    <div
      className="rounded-full flex items-center justify-center bg-surface-subtle text-ink-muted font-semibold flex-shrink-0"
      style={{ width: size, height: size, fontSize: size * 0.38 }}
    >
      {initials}
    </div>
  );
}

function WaveformVisualiser({ bars = 28 }: { bars?: number }) {
  return (
    <div className="flex items-center gap-px h-8 overflow-hidden">
      {Array.from({ length: bars }).map((_, i) => (
        <div
          key={i}
          className="waveform-bar"
          style={{
            animationDelay: `${(i * 0.05) % 1.2}s`,
            height: `${30 + Math.sin(i * 0.9) * 20 + Math.random() * 20}%`,
          }}
        />
      ))}
    </div>
  );
}

function StatRow({ event }: { event: FeedEvent }) {
  const [liked, setLiked] = useState(false);
  const [reposted, setReposted] = useState(false);

  return (
    <div className="flex items-center gap-5 mt-3 pt-3 border-t border-surface-divider">
      <button
        onClick={() => setLiked((v) => !v)}
        className={`stat-pill ${liked ? "text-brand-300" : ""}`}
        aria-label="Like"
      >
        <svg width="14" height="14" viewBox="0 0 24 24" fill={liked ? "currentColor" : "none"} stroke="currentColor" strokeWidth="2">
          <path d="M20.84 4.61a5.5 5.5 0 0 0-7.78 0L12 5.67l-1.06-1.06a5.5 5.5 0 0 0-7.78 7.78l1.06 1.06L12 21.23l7.78-7.78 1.06-1.06a5.5 5.5 0 0 0 0-7.78z" />
        </svg>
        <span>{fmtCount((event.likeCount ?? 0) + (liked ? 1 : 0))}</span>
      </button>

      <button
        onClick={() => setReposted((v) => !v)}
        className={`stat-pill ${reposted ? "text-accent-300" : ""}`}
        aria-label="Repost"
      >
        <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
          <polyline points="17 1 21 5 17 9" />
          <path d="M3 11V9a4 4 0 0 1 4-4h14" />
          <polyline points="7 23 3 19 7 15" />
          <path d="M21 13v2a4 4 0 0 1-4 4H3" />
        </svg>
        <span>{fmtCount((event.repostCount ?? 0) + (reposted ? 1 : 0))}</span>
      </button>

      <span className="stat-pill">
        <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
          <path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z" />
        </svg>
        <span>{fmtCount(event.commentCount)}</span>
      </span>

      <span className="stat-pill ml-auto">
        <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
          <polygon points="5 3 19 12 5 21 5 3" />
        </svg>
        <span>{fmtCount(event.playCount)}</span>
      </span>
    </div>
  );
}

// ─── Card variants ────────────────────────────────────────────────────────────

function TrackCard({ event }: { event: FeedEvent }) {
  const [playing, setPlaying] = useState(false);
  const isRepost = event.eventType === "TRACK_REPOSTED";

  return (
    <article className="card p-4 animate-slide-up">
      {/* Repost context line */}
      {isRepost && event.originalActorDisplayName && (
        <div className="flex items-center gap-1.5 text-xs text-ink-muted mb-3">
          <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
            <polyline points="17 1 21 5 17 9" />
            <path d="M3 11V9a4 4 0 0 1 4-4h14" />
            <polyline points="7 23 3 19 7 15" />
            <path d="M21 13v2a4 4 0 0 1-4 4H3" />
          </svg>
          <span>
            <span className="text-ink-secondary font-medium">{event.actorDisplayName}</span>
            {" reposted from "}
            <span className="text-ink-secondary font-medium">{event.originalActorDisplayName}</span>
          </span>
        </div>
      )}

      {/* Actor row */}
      <div className="flex items-center gap-3 mb-3">
        <Avatar url={event.actorAvatarUrl} name={event.actorDisplayName} />
        <div className="flex-1 min-w-0">
          <p className="text-sm font-semibold text-ink-primary truncate">{event.actorDisplayName}</p>
          <p className="text-xs text-ink-muted">{timeAgo(event.postedAt)}</p>
        </div>
        <div className="flex items-center gap-1.5 flex-shrink-0">
          {event.isBuzzing && <span className="badge-buzzing">⚡ Buzzing</span>}
          {event.isNew    && <span className="badge-new">New</span>}
        </div>
      </div>

      {/* Cover art + track info */}
      <div className="flex gap-3">
        <div className="relative flex-shrink-0">
          {event.coverArtUrl ? (
            // eslint-disable-next-line @next/next/no-img-element
            <img
              src={event.coverArtUrl}
              alt={event.trackTitle}
              width={80}
              height={80}
              className="rounded-xl object-cover w-20 h-20"
            />
          ) : (
            <div className="w-20 h-20 rounded-xl bg-surface-subtle flex items-center justify-center">
              <svg width="28" height="28" viewBox="0 0 24 24" fill="none" stroke="#8A9966" strokeWidth="1.5">
                <circle cx="12" cy="12" r="10" />
                <circle cx="12" cy="12" r="3" />
              </svg>
            </div>
          )}
          {/* Play button overlay */}
          <button
            onClick={() => setPlaying((v) => !v)}
            aria-label={playing ? "Pause" : "Play"}
            className="absolute inset-0 rounded-xl flex items-center justify-center
                       bg-surface-base/60 opacity-0 hover:opacity-100 transition-opacity"
          >
            <div className="w-8 h-8 rounded-full bg-brand-300 flex items-center justify-center shadow-lime-sm">
              {playing ? (
                <svg width="14" height="14" viewBox="0 0 24 24" fill="#080C04">
                  <rect x="6" y="4" width="4" height="16" />
                  <rect x="14" y="4" width="4" height="16" />
                </svg>
              ) : (
                <svg width="14" height="14" viewBox="0 0 24 24" fill="#080C04">
                  <polygon points="5 3 19 12 5 21 5 3" />
                </svg>
              )}
            </div>
          </button>
        </div>

        <div className="flex-1 min-w-0">
          <h3 className="text-sm font-semibold text-ink-primary leading-snug mb-1 truncate">
            {event.trackTitle ?? "Untitled Track"}
          </h3>
          {event.genre && event.genre.length > 0 && (
            <div className="flex flex-wrap gap-1 mb-2">
              {event.genre.slice(0, 3).map((g) => (
                <span key={g} className="text-[10px] px-1.5 py-0.5 rounded-full border border-surface-border text-ink-muted">
                  {g}
                </span>
              ))}
              {event.mood && (
                <span className="text-[10px] px-1.5 py-0.5 rounded-full border border-accent-700 text-accent-300">
                  {event.mood}
                </span>
              )}
            </div>
          )}

          {/* Waveform / progress */}
          <div className="relative">
            {playing ? (
              <WaveformVisualiser bars={32} />
            ) : (
              <div className="h-8 flex items-center">
                <div className="flex-1 h-px bg-surface-border relative overflow-hidden">
                  <div className="absolute inset-0 bg-waveform-gradient opacity-40 rounded-full" />
                </div>
              </div>
            )}
            <span className="absolute right-0 top-1/2 -translate-y-1/2 text-[10px] text-ink-disabled">
              {fmtDuration(event.trackDuration)}
            </span>
          </div>
        </div>
      </div>

      {/* Gift-to-unlock bar */}
      {event.isLocked && event.giftThreshold !== undefined && (
        <div className="mt-3 p-3 rounded-xl bg-surface-subtle border border-surface-border">
          <div className="flex items-center justify-between text-xs mb-2">
            <span className="text-ink-secondary font-medium">🎁 Gift to unlock full track</span>
            <span className="text-brand-300 font-bold">
              {event.giftProgress ?? 0} / {event.giftThreshold} gifts
            </span>
          </div>
          <div className="gift-progress-bar">
            <div
              className="gift-progress-fill"
              style={{ width: `${Math.min(100, ((event.giftProgress ?? 0) / event.giftThreshold!) * 100)}%` }}
            />
          </div>
          <button className="btn-primary w-full mt-2 text-xs justify-center">
            Send a Gift
          </button>
        </div>
      )}

      {/* Repost comment */}
      {event.repostCommentText && (
        <p className="mt-2 text-xs text-ink-secondary italic border-l-2 border-brand-700 pl-2">
          &ldquo;{event.repostCommentText}&rdquo;
        </p>
      )}

      {/* Collaborators */}
      {event.collaborators && event.collaborators.length > 0 && (
        <div className="flex items-center gap-1.5 mt-2 text-xs text-ink-muted">
          <span>ft.</span>
          {event.collaborators.map((c) => (
            <span key={c.userId} className="flex items-center gap-1">
              <Avatar url={c.avatarUrl} name={c.displayName} size={16} />
              <span className="text-ink-secondary">{c.displayName}</span>
            </span>
          ))}
        </div>
      )}

      <StatRow event={event} />
    </article>
  );
}

function LivestreamCard({ event }: { event: FeedEvent }) {
  return (
    <article className="card border-red-800/60 animate-slide-up overflow-hidden">
      {/* Thumbnail */}
      <div className="relative">
        {event.streamThumbnailUrl ? (
          // eslint-disable-next-line @next/next/no-img-element
          <img
            src={event.streamThumbnailUrl}
            alt="Livestream"
            className="w-full h-40 object-cover"
          />
        ) : (
          <div className="w-full h-40 bg-gradient-to-br from-surface-subtle to-surface-overlay flex items-center justify-center">
            <svg width="40" height="40" viewBox="0 0 24 24" fill="none" stroke="#AAFF00" strokeWidth="1.5">
              <circle cx="12" cy="12" r="10" />
              <circle cx="12" cy="12" r="3" />
              <line x1="12" y1="2" x2="12" y2="5" />
              <line x1="12" y1="19" x2="12" y2="22" />
              <line x1="2" y1="12" x2="5" y2="12" />
              <line x1="19" y1="12" x2="22" y2="12" />
            </svg>
          </div>
        )}
        <div className="absolute top-2 left-2">
          <span className="badge-live">
            <span className="w-1.5 h-1.5 rounded-full bg-white animate-pulse" />
            LIVE
          </span>
        </div>
        {event.viewerCount !== undefined && event.viewerCount > 0 && (
          <div className="absolute top-2 right-2 flex items-center gap-1 bg-surface-base/80 rounded-full px-2 py-0.5 text-xs text-ink-secondary backdrop-blur-sm">
            <svg width="10" height="10" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
              <path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2" />
              <circle cx="9" cy="7" r="4" />
              <path d="M23 21v-2a4 4 0 0 0-3-3.87" />
              <path d="M16 3.13a4 4 0 0 1 0 7.75" />
            </svg>
            {fmtCount(event.viewerCount)}
          </div>
        )}
      </div>

      <div className="p-4">
        <div className="flex items-center gap-3">
          <Avatar url={event.actorAvatarUrl} name={event.actorDisplayName} />
          <div className="flex-1 min-w-0">
            <p className="text-sm font-semibold text-ink-primary truncate">{event.actorDisplayName}</p>
            <p className="text-xs text-ink-muted">is live now</p>
          </div>
        </div>
        <button className="btn-primary w-full mt-3 justify-center">
          Join Stream
        </button>
      </div>
    </article>
  );
}

function DigestCard({ event }: { event: FeedEvent }) {
  return (
    <article className="card-featured p-4 animate-fade-in">
      <div className="flex items-center gap-2 mb-3">
        <div className="w-5 h-5 rounded-full bg-brand-gradient flex items-center justify-center">
          <svg width="10" height="10" viewBox="0 0 24 24" fill="#080C04">
            <polygon points="13 2 3 14 12 14 11 22 21 10 12 10 13 2" />
          </svg>
        </div>
        <span className="text-xs font-bold text-brand-300 uppercase tracking-wider">Activity Digest</span>
      </div>
      <div className="flex items-center gap-3">
        <Avatar url={event.actorAvatarUrl} name={event.actorDisplayName} />
        <div className="min-w-0">
          <p className="text-sm font-semibold text-ink-primary">
            <span className="text-brand-300">{event.actorDisplayName}</span>
            {" had a busy session"}
          </p>
          <div className="flex items-center gap-3 mt-1 text-xs text-ink-muted">
            {event.playCount !== undefined && event.playCount > 0 && (
              <span>{fmtCount(event.playCount)} plays</span>
            )}
            {event.likeCount !== undefined && event.likeCount > 0 && (
              <span>{fmtCount(event.likeCount)} likes</span>
            )}
          </div>
        </div>
      </div>
    </article>
  );
}

function TrendingUserCard({ event }: { event: FeedEvent }) {
  return (
    <article className="card p-4 animate-fade-in">
      <div className="flex items-center gap-2 mb-3">
        <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="#AAFF00" strokeWidth="2">
          <polyline points="23 6 13.5 15.5 8.5 10.5 1 18" />
          <polyline points="17 6 23 6 23 12" />
        </svg>
        <span className="text-xs font-bold text-brand-300 uppercase tracking-wider">Trending Artist</span>
      </div>
      <div className="flex items-center gap-3">
        <Avatar url={event.actorAvatarUrl} name={event.actorDisplayName} size={48} />
        <div className="flex-1 min-w-0">
          <p className="text-base font-bold text-ink-primary truncate">{event.actorDisplayName}</p>
          <p className="text-xs text-ink-muted mt-0.5">Growing fast in your network</p>
        </div>
        <button className="btn-ghost text-xs px-4 py-1.5">Follow</button>
      </div>
    </article>
  );
}

function VideoCard({ event }: { event: FeedEvent }) {
  return (
    <article className="card overflow-hidden animate-slide-up">
      <div className="relative">
        {event.videoThumbnailUrl ? (
          // eslint-disable-next-line @next/next/no-img-element
          <img
            src={event.videoThumbnailUrl}
            alt={event.videoTitle}
            className="w-full h-44 object-cover"
          />
        ) : (
          <div className="w-full h-44 bg-surface-subtle flex items-center justify-center">
            <svg width="36" height="36" viewBox="0 0 24 24" fill="none" stroke="#8A9966" strokeWidth="1.5">
              <rect x="2" y="2" width="20" height="20" rx="2.18" ry="2.18" />
              <line x1="7" y1="2" x2="7" y2="22" />
              <line x1="17" y1="2" x2="17" y2="22" />
              <line x1="2" y1="12" x2="22" y2="12" />
              <line x1="2" y1="7" x2="7" y2="7" />
              <line x1="2" y1="17" x2="7" y2="17" />
              <line x1="17" y1="17" x2="22" y2="17" />
              <line x1="17" y1="7" x2="22" y2="7" />
            </svg>
          </div>
        )}
        <button
          aria-label="Play video"
          className="absolute inset-0 flex items-center justify-center
                     bg-surface-base/40 hover:bg-surface-base/20 transition-colors"
        >
          <div className="w-12 h-12 rounded-full bg-brand-300/90 flex items-center justify-center shadow-lime-md">
            <svg width="20" height="20" viewBox="0 0 24 24" fill="#080C04">
              <polygon points="5 3 19 12 5 21 5 3" />
            </svg>
          </div>
        </button>
      </div>

      <div className="p-4">
        <div className="flex items-center gap-2 mb-2">
          <Avatar url={event.actorAvatarUrl} name={event.actorDisplayName} size={24} />
          <span className="text-xs text-ink-secondary font-medium">{event.actorDisplayName}</span>
          <span className="text-xs text-ink-disabled ml-auto">{timeAgo(event.postedAt)}</span>
        </div>
        <h3 className="text-sm font-semibold text-ink-primary">
          {event.videoTitle ?? "Untitled Video"}
        </h3>
        <StatRow event={event} />
      </div>
    </article>
  );
}

function FollowCard({ event }: { event: FeedEvent }) {
  return (
    <article className="card p-4 animate-fade-in">
      <div className="flex items-center gap-3">
        <Avatar url={event.actorAvatarUrl} name={event.actorDisplayName} size={40} />
        <div className="flex-1 min-w-0 text-sm text-ink-secondary">
          <span className="font-semibold text-ink-primary">{event.actorDisplayName}</span>
          {" started following someone in your network"}
        </div>
        <span className="text-xs text-ink-disabled">{timeAgo(event.postedAt)}</span>
      </div>
    </article>
  );
}

// ─── Main export ──────────────────────────────────────────────────────────────

interface FeedCardProps {
  event: FeedEvent;
}

export default function FeedCard({ event }: FeedCardProps) {
  const { eventType } = event;

  if (eventType === "LIVESTREAM_STARTED_AUDIO" || eventType === "LIVESTREAM_STARTED_VIDEO") {
    return <LivestreamCard event={event} />;
  }
  if (eventType === "DIGEST") {
    return <DigestCard event={event} />;
  }
  if (eventType === "TRENDING_USER") {
    return <TrendingUserCard event={event} />;
  }
  if (eventType === "VIDEO_POSTED" || eventType === "VIDEO_LIKED" || eventType === "POPULAR_VIDEO") {
    return <VideoCard event={event} />;
  }
  if (eventType === "ARTIST_FOLLOWED") {
    return <FollowCard event={event} />;
  }

  // Default: track-based card (TRACK_POSTED, TRACK_REPOSTED, TRACK_LIKED, etc.)
  return <TrackCard event={event} />;
}
