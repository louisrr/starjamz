package com.play.Starjams.MediaService.models;

/**
 * Defines the four supported stream modes.
 */
public enum StreamType {

    /** Decode an audio file (mp3, flac, ogg, wav, …) and stream as MP3 over HTTP. */
    AUDIO_FILE,

    /** Decode a video file (mp4, mkv, avi, …) and stream as WebM/VP8 over HTTP. */
    VIDEO_FILE,

    /** Capture live audio from a system audio device and stream as MP3 over HTTP. */
    LIVE_AUDIO,

    /**
     * Capture live video from a V4L2 camera + live audio from a PulseAudio device
     * and publish as an HLS stream (playlist + segments written to disk).
     */
    LIVE_VIDEO
}
