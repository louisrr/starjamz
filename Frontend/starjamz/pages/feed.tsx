import React, { useState, useEffect, useCallback, useRef } from "react";
import type { NextPage, GetServerSideProps } from "next";
import Head from "next/head";
import FeedCard, { FeedEvent } from "@/components/FeedCard";

// ─── Constants ────────────────────────────────────────────────────────────────

const GATEWAY_URL = process.env.NEXT_PUBLIC_GATEWAY_URL ?? "http://localhost:8080";
const PAGE_SIZE = 20;

// ─── Mock data — shown when the backend is unreachable ────────────────────────

const MOCK_EVENTS: FeedEvent[] = [
  {
    eventId: "1",
    eventType: "TRACK_POSTED",
    actorId: "u1",
    actorDisplayName: "Solara Beats",
    actorAvatarUrl: null,
    trackId: "t1",
    trackTitle: "Midnight Cascade (ft. Nova)",
    trackDuration: 214,
    coverArtUrl: null,
    genre: ["Electronic", "Ambient"],
    mood: "Dreamy",
    playCount: 14820,
    likeCount: 1043,
    repostCount: 312,
    commentCount: 87,
    postedAt: new Date(Date.now() - 25 * 60_000).toISOString(),
    isNew: true,
    isBuzzing: true,
    collaborators: [
      { userId: "u5", displayName: "Nova", avatarUrl: null },
    ],
  },
  {
    eventId: "2",
    eventType: "LIVESTREAM_STARTED_VIDEO",
    actorId: "u2",
    actorDisplayName: "DJ Rëal",
    actorAvatarUrl: null,
    streamId: "s1",
    isLive: true,
    viewerCount: 3210,
    streamThumbnailUrl: null,
    postedAt: new Date(Date.now() - 8 * 60_000).toISOString(),
  },
  {
    eventId: "3",
    eventType: "TRACK_REPOSTED",
    actorId: "u3",
    actorDisplayName: "Kira Vox",
    actorAvatarUrl: null,
    trackId: "t2",
    trackTitle: "Golden Hour Loop",
    trackDuration: 178,
    coverArtUrl: null,
    genre: ["Lo-fi", "Chill"],
    playCount: 5400,
    likeCount: 320,
    repostCount: 95,
    commentCount: 22,
    postedAt: new Date(Date.now() - 2 * 3600_000).toISOString(),
    originalActorDisplayName: "Lux Taito",
    repostCommentText: "This loop is pure therapy. Had it on repeat all afternoon.",
  },
  {
    eventId: "4",
    eventType: "TRACK_POSTED",
    actorId: "u4",
    actorDisplayName: "Neon Prophecy",
    actorAvatarUrl: null,
    trackId: "t3",
    trackTitle: "Violet Protocol",
    trackDuration: 257,
    coverArtUrl: null,
    genre: ["Synthwave", "Dark"],
    mood: "Intense",
    playCount: 890,
    likeCount: 74,
    repostCount: 18,
    commentCount: 9,
    postedAt: new Date(Date.now() - 5 * 3600_000).toISOString(),
    isLocked: true,
    giftThreshold: 5,
    giftProgress: 3,
  },
  {
    eventId: "5",
    eventType: "TRENDING_USER",
    actorId: "u5",
    actorDisplayName: "Wren Solis",
    actorAvatarUrl: null,
    postedAt: new Date(Date.now() - 1 * 3600_000).toISOString(),
  },
  {
    eventId: "6",
    eventType: "VIDEO_POSTED",
    actorId: "u6",
    actorDisplayName: "Studio Session",
    actorAvatarUrl: null,
    videoId: "v1",
    videoTitle: "Making a Beat from Scratch — full session",
    videoThumbnailUrl: null,
    playCount: 7200,
    likeCount: 541,
    repostCount: 88,
    commentCount: 130,
    postedAt: new Date(Date.now() - 12 * 3600_000).toISOString(),
  },
  {
    eventId: "7",
    eventType: "DIGEST",
    actorId: "u7",
    actorDisplayName: "Bass Theory",
    actorAvatarUrl: null,
    playCount: 42000,
    likeCount: 3100,
    postedAt: new Date(Date.now() - 3 * 3600_000).toISOString(),
  },
  {
    eventId: "8",
    eventType: "TRACK_REMIXED",
    actorId: "u8",
    actorDisplayName: "Pixel Drift",
    actorAvatarUrl: null,
    trackId: "t4",
    trackTitle: "Aurora (Pixel Drift Remix)",
    trackDuration: 198,
    coverArtUrl: null,
    genre: ["House", "Electronic"],
    mood: "Uplifting",
    playCount: 2100,
    likeCount: 184,
    repostCount: 46,
    commentCount: 31,
    postedAt: new Date(Date.now() - 7 * 3600_000).toISOString(),
    isNew: true,
    originalActorDisplayName: "Solara Beats",
  },
];

// ─── Types ────────────────────────────────────────────────────────────────────

interface FeedPage {
  items: FeedEvent[];
  nextCursor: string | null;
  hasMore: boolean;
}

// ─── Data fetching ────────────────────────────────────────────────────────────

async function fetchFeedPage(
  userId: string,
  cursor: string | null
): Promise<FeedPage> {
  const params = new URLSearchParams({ limit: String(PAGE_SIZE) });
  if (cursor) params.set("cursor", cursor);

  const res = await fetch(
    `${GATEWAY_URL}/feed/api/v1/users/${userId}/feed?${params.toString()}`
  );

  if (!res.ok) throw new Error(`Feed API ${res.status}`);
  return res.json() as Promise<FeedPage>;
}

// ─── Skeleton card ────────────────────────────────────────────────────────────

function SkeletonCard() {
  return (
    <div className="card p-4 space-y-3">
      <div className="flex items-center gap-3">
        <div className="skeleton w-10 h-10 rounded-full" />
        <div className="flex-1 space-y-2">
          <div className="skeleton h-3 w-32 rounded" />
          <div className="skeleton h-2 w-20 rounded" />
        </div>
      </div>
      <div className="flex gap-3">
        <div className="skeleton w-20 h-20 rounded-xl" />
        <div className="flex-1 space-y-2 pt-1">
          <div className="skeleton h-3 w-3/4 rounded" />
          <div className="skeleton h-2 w-1/2 rounded" />
          <div className="skeleton h-8 w-full rounded mt-2" />
        </div>
      </div>
    </div>
  );
}

// ─── Nav bar ──────────────────────────────────────────────────────────────────

function NavBar({ activeTab, onTab }: { activeTab: string; onTab: (t: string) => void }) {
  const tabs = ["For You", "Following", "Trending", "Live"];

  return (
    <nav className="sticky top-0 z-20 bg-surface-base/80 backdrop-blur-md border-b border-surface-divider">
      <div className="max-w-xl mx-auto px-4 flex items-center justify-between h-14">
        {/* Logo */}
        <div className="flex items-center gap-2">
          <div className="w-7 h-7 rounded-full bg-brand-gradient shadow-lime-sm" />
          <span className="font-bold text-ink-primary tracking-tight">starjamz</span>
        </div>

        {/* Tabs */}
        <div className="flex items-center gap-1">
          {tabs.map((tab) => (
            <button
              key={tab}
              onClick={() => onTab(tab)}
              className={`px-3 py-1.5 rounded-full text-xs font-semibold transition-all ${
                activeTab === tab
                  ? "bg-brand-300 text-surface-base shadow-lime-sm"
                  : "text-ink-muted hover:text-ink-secondary"
              }`}
            >
              {tab === "Live" ? (
                <span className="flex items-center gap-1">
                  <span className="w-1.5 h-1.5 rounded-full bg-red-500 animate-pulse" />
                  Live
                </span>
              ) : tab}
            </button>
          ))}
        </div>

        {/* Profile avatar placeholder */}
        <button aria-label="Profile" className="w-8 h-8 rounded-full bg-surface-subtle border border-surface-border flex items-center justify-center text-xs text-ink-muted">
          U
        </button>
      </div>
    </nav>
  );
}

// ─── Page ─────────────────────────────────────────────────────────────────────

interface FeedPageProps {
  userId: string;
}

const FeedPage: NextPage<FeedPageProps> = ({ userId }) => {
  const [activeTab, setActiveTab] = useState("For You");
  const [events, setEvents] = useState<FeedEvent[]>([]);
  const [cursor, setCursor] = useState<string | null>(null);
  const [hasMore, setHasMore] = useState(true);
  const [loading, setLoading] = useState(true);
  const [loadingMore, setLoadingMore] = useState(false);
  const [useMock, setUseMock] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const initialLoad = useRef(false);

  const loadFirst = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const page = await fetchFeedPage(userId, null);
      setEvents(page.items);
      setCursor(page.nextCursor);
      setHasMore(page.hasMore);
    } catch {
      // Backend unavailable — fall back to mock data
      setEvents(MOCK_EVENTS);
      setHasMore(false);
      setCursor(null);
      setUseMock(true);
    } finally {
      setLoading(false);
    }
  }, [userId]);

  const loadMore = useCallback(async () => {
    if (loadingMore || !hasMore || !cursor) return;
    setLoadingMore(true);
    try {
      const page = await fetchFeedPage(userId, cursor);
      setEvents((prev) => [...prev, ...page.items]);
      setCursor(page.nextCursor);
      setHasMore(page.hasMore);
    } catch (err) {
      setError("Failed to load more posts.");
    } finally {
      setLoadingMore(false);
    }
  }, [userId, cursor, hasMore, loadingMore]);

  useEffect(() => {
    if (initialLoad.current) return;
    initialLoad.current = true;
    loadFirst();
  }, [loadFirst]);

  // Reload when tab changes (for demo purposes the mock data stays the same)
  useEffect(() => {
    initialLoad.current = false;
    setEvents([]);
    setCursor(null);
    setHasMore(true);
    setUseMock(false);
    initialLoad.current = false;
    loadFirst();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [activeTab]);

  return (
    <>
      <Head>
        <title>Feed — Starjamz</title>
        <meta name="description" content="Your personalised music feed" />
        <meta name="viewport" content="width=device-width, initial-scale=1" />
      </Head>

      <div className="min-h-screen bg-surface-base bg-lime-glow">
        <NavBar activeTab={activeTab} onTab={setActiveTab} />

        <main className="max-w-xl mx-auto px-4 py-6 space-y-4">
          {/* Mock data notice */}
          {useMock && (
            <div className="rounded-xl border border-brand-700 bg-surface-subtle px-4 py-3 flex items-center gap-3 text-xs text-ink-muted animate-fade-in">
              <div className="w-1.5 h-1.5 rounded-full bg-brand-300 flex-shrink-0" />
              <span>
                GatewayService unavailable — showing preview data.{" "}
                <span className="text-brand-300">Connect the backend to see live feed.</span>
              </span>
            </div>
          )}

          {/* Feed header */}
          <div className="flex items-center justify-between">
            <h1 className="text-lg font-bold text-ink-primary">
              {activeTab === "For You" ? (
                <>Your <span className="text-brand-gradient">Feed</span></>
              ) : (
                activeTab
              )}
            </h1>
            <button
              onClick={loadFirst}
              disabled={loading}
              className="text-xs text-ink-muted hover:text-brand-300 transition-colors disabled:opacity-50 flex items-center gap-1"
              aria-label="Refresh feed"
            >
              <svg
                width="12"
                height="12"
                viewBox="0 0 24 24"
                fill="none"
                stroke="currentColor"
                strokeWidth="2"
                className={loading ? "animate-spin" : ""}
              >
                <polyline points="23 4 23 10 17 10" />
                <polyline points="1 20 1 14 7 14" />
                <path d="M3.51 9a9 9 0 0 1 14.85-3.36L23 10M1 14l4.64 4.36A9 9 0 0 0 20.49 15" />
              </svg>
              Refresh
            </button>
          </div>

          {/* Skeleton loading state */}
          {loading && (
            <div className="space-y-4">
              {Array.from({ length: 4 }).map((_, i) => (
                <SkeletonCard key={i} />
              ))}
            </div>
          )}

          {/* Feed cards */}
          {!loading && events.length > 0 && (
            <div className="space-y-4">
              {events.map((event) => (
                <FeedCard key={event.eventId} event={event} />
              ))}
            </div>
          )}

          {/* Empty state */}
          {!loading && events.length === 0 && (
            <div className="flex flex-col items-center justify-center py-20 text-center">
              <div className="w-16 h-16 rounded-full bg-surface-subtle flex items-center justify-center mb-4">
                <svg width="28" height="28" viewBox="0 0 24 24" fill="none" stroke="#3A4D1C" strokeWidth="1.5">
                  <circle cx="12" cy="12" r="10" />
                  <circle cx="12" cy="12" r="3" />
                </svg>
              </div>
              <p className="text-ink-secondary font-medium">Nothing here yet</p>
              <p className="text-ink-muted text-sm mt-1">Follow some artists to fill your feed.</p>
            </div>
          )}

          {/* Load more */}
          {!loading && hasMore && events.length > 0 && (
            <div className="flex justify-center pt-2">
              <button
                onClick={loadMore}
                disabled={loadingMore}
                className="btn-ghost text-sm"
              >
                {loadingMore ? (
                  <span className="flex items-center gap-2">
                    <svg className="animate-spin" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                      <path d="M21 12a9 9 0 1 1-6.219-8.56" />
                    </svg>
                    Loading…
                  </span>
                ) : (
                  "Load more"
                )}
              </button>
            </div>
          )}

          {/* End of feed */}
          {!loading && !hasMore && events.length > 0 && (
            <div className="text-center py-8">
              <div className="inline-flex items-center gap-2 text-xs text-ink-disabled">
                <div className="h-px w-12 bg-surface-border" />
                You&apos;re all caught up
                <div className="h-px w-12 bg-surface-border" />
              </div>
            </div>
          )}

          {/* Error */}
          {error && (
            <p className="text-center text-xs text-red-400">{error}</p>
          )}
        </main>
      </div>
    </>
  );
};

export const getServerSideProps: GetServerSideProps<FeedPageProps> = async (ctx) => {
  // In production this would come from the session / JWT.
  // For now, accept userId as a query param or fall back to a placeholder.
  const userId =
    typeof ctx.query.userId === "string"
      ? ctx.query.userId
      : "00000000-0000-0000-0000-000000000001";

  return { props: { userId } };
};

export default FeedPage;
