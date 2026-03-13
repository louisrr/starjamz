package com.play.stream.Starjams.MediaIngressService.services.ingest;

import com.play.stream.Starjams.MediaIngressService.dto.ConnectorStatusDto;
import com.play.stream.Starjams.MediaIngressService.model.StreamPlatform;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * Spring-managed registry mapping each {@link StreamPlatform} to its connector.
 *
 * <p>MOBILE_IOS and MOBILE_ANDROID both route to {@link MobileIngestConnector}.
 */
@Service
public class ConnectorRegistry {

    private static final Logger log = LoggerFactory.getLogger(ConnectorRegistry.class);

    private final Map<StreamPlatform, PlatformIngestConnector> connectors;

    public ConnectorRegistry(MobileIngestConnector mobileConnector,
                              YouTubeIngestConnector youTubeConnector,
                              TwitchIngestConnector twitchConnector,
                              GenericRtmpConnector genericConnector) {
        connectors = new EnumMap<>(StreamPlatform.class);
        connectors.put(StreamPlatform.MOBILE_IOS,     mobileConnector);
        connectors.put(StreamPlatform.MOBILE_ANDROID, mobileConnector);
        connectors.put(StreamPlatform.YOUTUBE,        youTubeConnector);
        connectors.put(StreamPlatform.TWITCH,         twitchConnector);
        connectors.put(StreamPlatform.RTMP_GENERIC,   genericConnector);
    }

    public PlatformIngestConnector getConnector(StreamPlatform platform) {
        PlatformIngestConnector connector = connectors.get(platform);
        if (connector == null) {
            throw new IllegalArgumentException("No connector registered for platform: " + platform);
        }
        return connector;
    }

    public List<ConnectorStatusDto> getConnectorStatuses() {
        // Deduplicate: MOBILE_IOS and MOBILE_ANDROID share a connector
        Set<PlatformIngestConnector> seen = Collections.newSetFromMap(new IdentityHashMap<>());
        List<ConnectorStatusDto> statuses = new ArrayList<>();
        for (Map.Entry<StreamPlatform, PlatformIngestConnector> entry : connectors.entrySet()) {
            if (seen.add(entry.getValue())) {
                statuses.add(entry.getValue().getStatus());
            }
        }
        return statuses;
    }

    public void setEnabled(StreamPlatform platform, boolean enabled) {
        PlatformIngestConnector connector = getConnector(platform);
        connector.setEnabled(enabled);
        log.info("Platform {} connector {}", platform, enabled ? "enabled" : "disabled");
    }

    public List<ConnectorStatusDto> getQueueStatus() {
        // Returns connector statuses including active connection counts
        return getConnectorStatuses();
    }
}
