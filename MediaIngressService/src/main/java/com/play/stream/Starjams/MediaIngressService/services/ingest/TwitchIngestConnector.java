package com.play.stream.Starjams.MediaIngressService.services.ingest;

import com.play.stream.Starjams.MediaIngressService.model.StreamPlatform;
import com.play.stream.Starjams.MediaIngressService.services.PipelineHealthService;
import com.play.stream.Starjams.MediaIngressService.services.StreamRouter;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * Ingests a Twitch Live stream by pulling from the creator's RTMP re-stream URL.
 *
 * <p>The creator must enable re-streaming in their Twitch Dashboard and provide
 * the re-stream URL (format: {@code rtmp://live.twitch.tv/app/{streamKey}}).
 * Pull-based ingestion requires the re-stream URL to be shared by the creator;
 * direct Twitch API integration would use OAuth but is not required here since
 * the creator provides the RTMP endpoint explicitly.
 *
 * <p>Note: Twitch's Terms of Service require compliance with their re-stream policies.
 * Verify that re-streaming a Twitch broadcast to Starjamz is permitted for the
 * specific use case (educator/creator simulcasting their own content).
 */
@Service
public class TwitchIngestConnector extends GenericRtmpConnector {

    public TwitchIngestConnector(StreamRouter streamRouter, PipelineHealthService healthService) {
        super(streamRouter, healthService);
    }

    @Override
    public StreamPlatform platform() {
        return StreamPlatform.TWITCH;
    }

    @Override
    @Async("gstreamerExecutor")
    public void connect(String streamKey, String sourceUrl, String authToken) {
        super.connect(streamKey, sourceUrl, authToken);
    }
}
