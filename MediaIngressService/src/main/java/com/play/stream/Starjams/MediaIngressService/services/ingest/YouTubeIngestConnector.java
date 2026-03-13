package com.play.stream.Starjams.MediaIngressService.services.ingest;

import com.play.stream.Starjams.MediaIngressService.model.StreamPlatform;
import com.play.stream.Starjams.MediaIngressService.services.PipelineHealthService;
import com.play.stream.Starjams.MediaIngressService.services.StreamRouter;
import org.springframework.context.annotation.Primary;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * Ingests a YouTube Live stream by pulling from the creator's RTMP re-stream URL.
 *
 * <p>The creator must enable re-streaming in YouTube Studio and provide the
 * re-stream URL (format: {@code rtmp://a.rtmp.youtube.com/live2/{streamKey}}).
 * MediaIngressService pulls from this URL — no YouTube API auth is required
 * as long as the re-stream URL is provided by the creator.
 *
 * <p>If an auth token is supplied, it is appended to the source URL as a query param.
 */
@Service
public class YouTubeIngestConnector extends GenericRtmpConnector {

    public YouTubeIngestConnector(StreamRouter streamRouter, PipelineHealthService healthService) {
        super(streamRouter, healthService);
    }

    @Override
    public StreamPlatform platform() {
        return StreamPlatform.YOUTUBE;
    }

    @Override
    @Async("gstreamerExecutor")
    public void connect(String streamKey, String sourceUrl, String authToken) {
        // YouTube RTMP re-stream URLs already contain the stream key in the path.
        // authToken is not required at the GStreamer level for RTMP pull.
        super.connect(streamKey, sourceUrl, authToken);
    }
}
