package com.play.Starjams.MediaService.models;

/**
 * Request body for POST /api/streams.
 *
 * Examples:
 *
 * <pre>
 * // Stream a music file
 * { "type": "AUDIO_FILE", "sourcePath": "/media/music/song.flac" }
 *
 * // Stream a video file
 * { "type": "VIDEO_FILE", "sourcePath": "/media/video/clip.mp4" }
 *
 * // Live audio from default PulseAudio device
 * { "type": "LIVE_AUDIO" }
 *
 * // Live video from first camera + default audio device
 * { "type": "LIVE_VIDEO", "videoDevice": "/dev/video0", "audioDevice": "default" }
 * </pre>
 */
public class StreamRequest {

    private StreamType type;

    /**
     * Absolute filesystem path to the media file.
     * Required for AUDIO_FILE and VIDEO_FILE.
     */
    private String sourcePath;

    /**
     * PulseAudio sink/source name (e.g. "default", "alsa_input.pci-0000_00_1f.3.analog-stereo").
     * Used for LIVE_AUDIO; optional for LIVE_VIDEO (falls back to stream.audio.device property).
     */
    private String audioDevice;

    /**
     * V4L2 device path (e.g. "/dev/video0").
     * Required for LIVE_VIDEO; ignored otherwise.
     */
    private String videoDevice;

    public StreamType getType()                     { return type; }
    public void setType(StreamType type)            { this.type = type; }

    public String getSourcePath()                   { return sourcePath; }
    public void setSourcePath(String sourcePath)    { this.sourcePath = sourcePath; }

    public String getAudioDevice()                  { return audioDevice; }
    public void setAudioDevice(String audioDevice)  { this.audioDevice = audioDevice; }

    public String getVideoDevice()                  { return videoDevice; }
    public void setVideoDevice(String videoDevice)  { this.videoDevice = videoDevice; }
}
