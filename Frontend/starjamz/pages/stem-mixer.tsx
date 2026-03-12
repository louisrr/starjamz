import React, { useEffect, useState } from 'react';
import StemMixer from '../components/StemMixer';

interface SessionInfo {
  sessionId: string;
  hostUserId: string;
  trackId: string;
  participants: string[];
  stemVolumes: Record<string, number>;
}

export default function StemMixerPage() {
  // In production these come from auth context + router query.
  // For now, read from URL search params so the page is functional without full auth.
  const [trackId, setTrackId] = useState('');
  const [userId, setUserId] = useState('');
  const [accessibleStems, setAccessibleStems] = useState<string[]>(['full_mix']);
  const [tierInfo, setTierInfo] = useState<{ tier: number; nextTierCost: number } | null>(null);

  const [sessionId, setSessionId] = useState<string | undefined>(undefined);
  const [sessionInfo, setSessionInfo] = useState<SessionInfo | null>(null);
  const [joinCode, setJoinCode] = useState('');
  const [sessionError, setSessionError] = useState('');
  const [remixId, setRemixId] = useState<string | null>(null);

  const GATEWAY = process.env.NEXT_PUBLIC_GATEWAY_URL ?? 'http://localhost:8080';

  // Read trackId + userId from URL on mount (pages router)
  useEffect(() => {
    if (typeof window === 'undefined') return;
    const params = new URLSearchParams(window.location.search);
    setTrackId(params.get('trackId') ?? '');
    setUserId(params.get('userId') ?? '');
    setSessionId(params.get('sessionId') ?? undefined);
  }, []);

  // Fetch tier info whenever trackId + userId are set
  useEffect(() => {
    if (!trackId || !userId) return;
    fetch(`${GATEWAY}/music/stream/${trackId}/tier?userId=${userId}`)
      .then((r) => r.ok ? r.json() : null)
      .then((data) => {
        if (!data) return;
        setAccessibleStems(data.accessibleStems ?? ['full_mix']);
        setTierInfo({ tier: data.tier, nextTierCost: data.nextTierCost });
      })
      .catch(() => {});
  }, [GATEWAY, trackId, userId]);

  // Fetch session info if sessionId is set
  useEffect(() => {
    if (!sessionId) return;
    fetch(`${GATEWAY}/music/stem-session/${sessionId}`)
      .then((r) => r.ok ? r.json() : null)
      .then((data: SessionInfo | null) => {
        if (data) setSessionInfo(data);
      })
      .catch(() => {});
  }, [GATEWAY, sessionId]);

  // -------------------------------------------------------------------------
  // Session actions
  // -------------------------------------------------------------------------

  const handleCreateSession = async () => {
    if (!trackId || !userId) return;
    setSessionError('');
    const res = await fetch(`${GATEWAY}/music/stem-session`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ trackId, hostUserId: userId }),
    });
    if (res.ok) {
      const data = await res.json();
      setSessionId(data.sessionId);
    } else {
      const text = await res.text();
      setSessionError(text);
    }
  };

  const handleJoinSession = async () => {
    if (!joinCode.trim() || !userId) return;
    setSessionError('');
    const res = await fetch(
      `${GATEWAY}/music/stem-session/${joinCode.trim()}/join?userId=${userId}`,
      { method: 'POST' }
    );
    if (res.ok) {
      setSessionId(joinCode.trim());
    } else {
      setSessionError('Could not join session. It may be full or the code is invalid.');
    }
  };

  // -------------------------------------------------------------------------
  // Render
  // -------------------------------------------------------------------------

  if (!trackId || !userId) {
    return (
      <div className="min-h-screen bg-gray-950 flex items-center justify-center text-white">
        <p className="text-gray-400 text-sm">
          Missing <code className="text-pink-400">trackId</code> and{' '}
          <code className="text-pink-400">userId</code> query parameters.
        </p>
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-gray-950 text-white px-4 py-10">
      <div className="max-w-lg mx-auto space-y-8">

        {/* Page header */}
        <div>
          <h1 className="text-2xl font-bold tracking-tight">Stem Mixer</h1>
          <p className="text-gray-400 text-sm mt-1">
            Track <span className="font-mono text-xs text-gray-500">{trackId}</span>
          </p>
          {tierInfo && (
            <p className="text-sm mt-1">
              <span className="text-gray-400">Access tier: </span>
              <span className="font-semibold text-indigo-400">Tier {tierInfo.tier}</span>
              {tierInfo.tier < 3 && (
                <span className="text-gray-500 ml-2 text-xs">
                  ({tierInfo.nextTierCost} gifts → Tier {tierInfo.tier + 1})
                </span>
              )}
            </p>
          )}
        </div>

        {/* Stem mixer component */}
        <StemMixer
          trackId={trackId}
          userId={userId}
          accessibleStems={accessibleStems}
          sessionId={sessionId}
          onRemixSave={(id) => setRemixId(id)}
        />

        {remixId && (
          <div className="bg-green-900/40 border border-green-700 rounded-xl px-4 py-3">
            <p className="text-sm text-green-300">
              Remix published to feed.{' '}
              <span className="font-mono text-xs text-green-500">{remixId}</span>
            </p>
          </div>
        )}

        {/* Collaborative session panel */}
        <div className="bg-gray-800 rounded-2xl p-5 space-y-4">
          <h2 className="font-semibold text-base">Collaborative Session</h2>

          {sessionId ? (
            <div className="space-y-2">
              <p className="text-sm text-gray-300">
                Session active:{' '}
                <span className="font-mono text-xs text-indigo-400">{sessionId}</span>
              </p>
              {sessionInfo && (
                <p className="text-xs text-gray-500">
                  {sessionInfo.participants.length} / 8 participants
                </p>
              )}
              <button
                onClick={() => navigator.clipboard.writeText(sessionId)}
                className="text-xs text-indigo-400 hover:text-indigo-300 underline"
              >
                Copy invite code
              </button>
            </div>
          ) : (
            <div className="space-y-3">
              <p className="text-sm text-gray-400">
                Requires Tier 2 stem access to host. Listeners can join with a session code.
              </p>

              {/* Create session */}
              <button
                onClick={handleCreateSession}
                className="w-full py-2 rounded-xl bg-indigo-600 hover:bg-indigo-500
                           text-sm font-semibold transition-colors"
              >
                Start Collaborative Session
              </button>

              {/* Join session */}
              <div className="flex gap-2">
                <input
                  type="text"
                  value={joinCode}
                  onChange={(e) => setJoinCode(e.target.value)}
                  placeholder="Paste session code…"
                  className="flex-1 bg-gray-700 rounded-lg px-3 py-2 text-sm outline-none
                             focus:ring-1 focus:ring-indigo-500 placeholder-gray-500"
                />
                <button
                  onClick={handleJoinSession}
                  disabled={!joinCode.trim()}
                  className="px-4 py-2 bg-gray-600 hover:bg-gray-500 disabled:opacity-40
                             rounded-lg text-sm font-semibold transition-colors"
                >
                  Join
                </button>
              </div>

              {sessionError && (
                <p className="text-xs text-red-400">{sessionError}</p>
              )}
            </div>
          )}
        </div>

        {/* Tier unlock info */}
        <div className="bg-gray-800 rounded-2xl p-5">
          <h2 className="font-semibold text-base mb-3">Stem Access Tiers</h2>
          <div className="space-y-2 text-sm">
            {[
              { tier: 0, label: 'Free',    gifts: 0,  stems: 'Full mix only' },
              { tier: 1, label: 'Tier 1',  gifts: 1,  stems: 'Instrumental (no vocals)' },
              { tier: 2, label: 'Tier 2',  gifts: 5,  stems: 'All individual stems' },
              { tier: 3, label: 'Tier 3',  gifts: 20, stems: 'Raw stems + BPM/key metadata' },
            ].map(({ tier, label, gifts, stems }) => (
              <div
                key={tier}
                className={`flex justify-between items-center px-3 py-2 rounded-lg
                  ${tierInfo && tier <= tierInfo.tier
                    ? 'bg-indigo-900/40 border border-indigo-700'
                    : 'bg-gray-700/40'}`}
              >
                <span className="font-medium">
                  {label}
                  {tierInfo && tier === tierInfo.tier && (
                    <span className="ml-2 text-xs text-indigo-400">current</span>
                  )}
                </span>
                <span className="text-gray-400 text-xs">
                  {gifts === 0 ? 'Free' : `${gifts} gift${gifts > 1 ? 's' : ''}`}
                  {' · '}
                  {stems}
                </span>
              </div>
            ))}
          </div>
        </div>
      </div>
    </div>
  );
}
