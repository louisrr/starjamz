import React, { useCallback, useEffect, useRef, useState } from 'react';

const STEM_TYPES = ['vocals', 'drums', 'bass', 'instruments'] as const;
type StemType = typeof STEM_TYPES[number];

interface StemVolumes {
  [key: string]: number;
}

interface StemMixerProps {
  trackId: string;
  userId: string;
  accessibleStems: string[];
  sessionId?: string;
  onRemixSave?: (remixId: string) => void;
}

interface TierInfo {
  tier: number;
  accessibleStems: string[];
  nextTierCost: number;
}

const STEM_LABELS: Record<StemType, string> = {
  vocals:      'Vocals',
  drums:       'Drums',
  bass:        'Bass',
  instruments: 'Instruments',
};

const STEM_COLORS: Record<StemType, string> = {
  vocals:      'bg-purple-500',
  drums:       'bg-red-500',
  bass:        'bg-yellow-500',
  instruments: 'bg-blue-500',
};

export default function StemMixer({
  trackId,
  userId,
  accessibleStems,
  sessionId,
  onRemixSave,
}: StemMixerProps) {
  const [volumes, setVolumes] = useState<StemVolumes>(() =>
    Object.fromEntries(STEM_TYPES.map((s) => [s, 1.0]))
  );
  const [tierInfo, setTierInfo] = useState<TierInfo | null>(null);
  const [isPlaying, setIsPlaying] = useState(false);
  const [remixTitle, setRemixTitle] = useState('');
  const [isSavingRemix, setIsSavingRemix] = useState(false);
  const [remixSaved, setRemixSaved] = useState(false);
  const [patchPending, setPatchPending] = useState(false);

  const audioRef = useRef<HTMLAudioElement | null>(null);
  const patchTimerRef = useRef<ReturnType<typeof setTimeout> | null>(null);

  const GATEWAY = process.env.NEXT_PUBLIC_GATEWAY_URL ?? 'http://localhost:8080';

  // Fetch tier info on mount
  useEffect(() => {
    fetch(`${GATEWAY}/music/stream/${trackId}/tier?userId=${userId}`)
      .then((r) => r.ok ? r.json() : null)
      .then((data: TierInfo | null) => {
        if (data) setTierInfo(data);
      })
      .catch(() => {});
  }, [GATEWAY, trackId, userId]);

  const streamUrl = `${GATEWAY}/music/stream/${trackId}/mix?userId=${userId}`;

  const handlePlay = useCallback(() => {
    if (!audioRef.current) {
      audioRef.current = new Audio(streamUrl);
    }
    audioRef.current.play();
    setIsPlaying(true);
  }, [streamUrl]);

  const handlePause = useCallback(() => {
    audioRef.current?.pause();
    setIsPlaying(false);
  }, []);

  // Debounced PATCH to backend after volume change
  const schedulePatch = useCallback(
    (newVolumes: StemVolumes) => {
      if (patchTimerRef.current) clearTimeout(patchTimerRef.current);
      patchTimerRef.current = setTimeout(async () => {
        setPatchPending(true);
        try {
          const endpoint = sessionId
            ? `${GATEWAY}/music/stem-session/${sessionId}/mix`
            : `${GATEWAY}/music/stream/${trackId}/mix?userId=${userId}`;

          const method = 'PATCH';
          const body = sessionId
            ? JSON.stringify({ participantUserId: userId, stemVolumes: newVolumes })
            : JSON.stringify({ stemVolumes: newVolumes });

          const res = await fetch(endpoint, {
            method,
            headers: { 'Content-Type': 'application/json' },
            body,
          });

          if (res.ok && !sessionId && audioRef.current) {
            // Reload stream with updated mix
            const wasPlaying = !audioRef.current.paused;
            audioRef.current.pause();
            audioRef.current = new Audio(
              `${GATEWAY}/music/stream/${trackId}/mix?userId=${userId}`
            );
            if (wasPlaying) audioRef.current.play();
          }
        } finally {
          setPatchPending(false);
        }
      }, 400);
    },
    [GATEWAY, trackId, userId, sessionId]
  );

  const handleVolumeChange = useCallback(
    (stemType: string, value: number) => {
      const clamped = Math.max(0, Math.min(1, value));
      setVolumes((prev) => {
        const next = { ...prev, [stemType]: clamped };
        schedulePatch(next);
        return next;
      });
    },
    [schedulePatch]
  );

  const handleMuteToggle = useCallback(
    (stemType: string) => {
      setVolumes((prev) => {
        const next = { ...prev, [stemType]: prev[stemType] > 0 ? 0 : 1.0 };
        schedulePatch(next);
        return next;
      });
    },
    [schedulePatch]
  );

  const handleSaveRemix = useCallback(async () => {
    if (!remixTitle.trim()) return;
    setIsSavingRemix(true);
    try {
      const res = await fetch(`${GATEWAY}/music/stream/${trackId}/remix`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          remixerUserId: userId,
          remixTitle: remixTitle.trim(),
          stemVolumes: volumes,
        }),
      });
      if (res.ok) {
        const data = await res.json();
        setRemixSaved(true);
        onRemixSave?.(data.remixId);
      }
    } finally {
      setIsSavingRemix(false);
    }
  }, [GATEWAY, trackId, userId, remixTitle, volumes, onRemixSave]);

  // Cleanup on unmount
  useEffect(() => {
    return () => {
      audioRef.current?.pause();
      if (patchTimerRef.current) clearTimeout(patchTimerRef.current);
    };
  }, []);

  const isStemAccessible = (stem: string) => accessibleStems.includes(stem);

  return (
    <div className="bg-gray-900 text-white rounded-2xl p-6 w-full max-w-lg shadow-xl">
      {/* Header */}
      <div className="flex items-center justify-between mb-6">
        <h2 className="text-lg font-bold tracking-tight">Stem Mixer</h2>
        <div className="flex items-center gap-2">
          {patchPending && (
            <span className="text-xs text-gray-400 animate-pulse">syncing…</span>
          )}
          {tierInfo && (
            <span className="text-xs bg-gray-700 px-2 py-0.5 rounded-full">
              Tier {tierInfo.tier}
            </span>
          )}
        </div>
      </div>

      {/* Stem sliders */}
      <div className="space-y-5 mb-6">
        {STEM_TYPES.map((stem) => {
          const accessible = isStemAccessible(stem);
          const muted = volumes[stem] === 0;
          return (
            <div key={stem} className="flex items-center gap-3">
              {/* Mute button */}
              <button
                onClick={() => accessible && handleMuteToggle(stem)}
                disabled={!accessible}
                className={`w-8 h-8 rounded-full flex items-center justify-center text-xs font-bold transition-opacity
                  ${accessible ? 'cursor-pointer' : 'cursor-not-allowed opacity-30'}
                  ${muted ? 'bg-gray-600' : STEM_COLORS[stem]}`}
                title={accessible ? (muted ? 'Unmute' : 'Mute') : 'Locked — send gifts to unlock'}
              >
                {muted ? 'M' : STEM_LABELS[stem][0]}
              </button>

              {/* Label */}
              <span className={`w-20 text-sm font-medium ${!accessible ? 'text-gray-500' : ''}`}>
                {STEM_LABELS[stem]}
              </span>

              {/* Volume slider */}
              <div className="flex-1 relative">
                <input
                  type="range"
                  min={0}
                  max={1}
                  step={0.01}
                  value={volumes[stem]}
                  disabled={!accessible}
                  onChange={(e) => handleVolumeChange(stem, parseFloat(e.target.value))}
                  className={`w-full h-2 rounded-lg appearance-none outline-none
                    ${accessible ? 'cursor-pointer' : 'cursor-not-allowed opacity-30'}
                    accent-current`}
                  style={{ accentColor: accessible && !muted ? undefined : '#4B5563' }}
                />
                {!accessible && (
                  <div className="absolute inset-0 flex items-center justify-end pr-1 pointer-events-none">
                    <span className="text-xs text-yellow-500">
                      {tierInfo
                        ? `${tierInfo.nextTierCost} gifts`
                        : 'locked'}
                    </span>
                  </div>
                )}
              </div>

              {/* Volume readout */}
              <span className={`w-8 text-right text-xs tabular-nums
                ${accessible ? 'text-gray-300' : 'text-gray-600'}`}>
                {accessible ? Math.round(volumes[stem] * 100) : '—'}
              </span>
            </div>
          );
        })}
      </div>

      {/* Transport */}
      <div className="flex items-center gap-3 mb-6">
        <button
          onClick={isPlaying ? handlePause : handlePlay}
          className="flex-1 py-2 rounded-xl font-semibold bg-indigo-600 hover:bg-indigo-500
                     active:bg-indigo-700 transition-colors"
        >
          {isPlaying ? 'Pause' : 'Play Mix'}
        </button>
      </div>

      {/* Save as Remix */}
      {!sessionId && (
        <div className="border-t border-gray-700 pt-5">
          <p className="text-xs text-gray-400 mb-2">Save this mix as a remix card in your feed</p>
          {remixSaved ? (
            <p className="text-sm text-green-400 font-medium">Remix saved and published to feed</p>
          ) : (
            <div className="flex gap-2">
              <input
                type="text"
                value={remixTitle}
                onChange={(e) => setRemixTitle(e.target.value)}
                placeholder="Remix title…"
                maxLength={80}
                className="flex-1 bg-gray-800 rounded-lg px-3 py-2 text-sm outline-none
                           focus:ring-1 focus:ring-indigo-500 placeholder-gray-500"
              />
              <button
                onClick={handleSaveRemix}
                disabled={isSavingRemix || !remixTitle.trim()}
                className="px-4 py-2 bg-pink-600 hover:bg-pink-500 disabled:opacity-40
                           rounded-lg text-sm font-semibold transition-colors"
              >
                {isSavingRemix ? 'Saving…' : 'Save Remix'}
              </button>
            </div>
          )}
        </div>
      )}
    </div>
  );
}
