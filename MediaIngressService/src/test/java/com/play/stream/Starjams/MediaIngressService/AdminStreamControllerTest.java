package com.play.stream.Starjams.MediaIngressService;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.play.stream.Starjams.MediaIngressService.controller.AdminStreamController;
import com.play.stream.Starjams.MediaIngressService.dto.FanOutProgressDto;
import com.play.stream.Starjams.MediaIngressService.dto.LiveSessionDto;
import com.play.stream.Starjams.MediaIngressService.model.StreamPlatform;
import com.play.stream.Starjams.MediaIngressService.model.StreamStatus;
import com.play.stream.Starjams.MediaIngressService.services.AdminStreamService;
import com.play.stream.Starjams.MediaIngressService.services.FanOutProgressService;
import com.play.stream.Starjams.MediaIngressService.services.PipelineHealthService;
import com.play.stream.Starjams.MediaIngressService.services.ingest.ConnectorRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AdminStreamController.class)
@Import(com.play.stream.Starjams.MediaIngressService.config.SecurityConfig.class)
class AdminStreamControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AdminStreamService adminStreamService;

    @MockBean
    private FanOutProgressService fanOutProgressService;

    @MockBean
    private PipelineHealthService pipelineHealthService;

    @MockBean
    private ConnectorRegistry connectorRegistry;

    // -------------------------------------------------------------------------
    // Security: unauthenticated requests should be rejected
    // -------------------------------------------------------------------------

    @Test
    void listLiveStreams_unauthenticated_returns401() throws Exception {
        mockMvc.perform(get("/api/v1/admin/streams/live"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void forceTerminate_unauthenticated_returns401() throws Exception {
        mockMvc.perform(delete("/api/v1/admin/streams/live/testkey"))
            .andExpect(status().isUnauthorized());
    }

    // -------------------------------------------------------------------------
    // Authenticated requests
    // -------------------------------------------------------------------------

    @Test
    @WithMockUser(roles = "ADMIN")
    void listLiveStreams_authenticated_returns200() throws Exception {
        LiveSessionDto session = buildMockSession("key123");
        when(adminStreamService.listLiveStreams()).thenReturn(List.of(session));

        mockMvc.perform(get("/api/v1/admin/streams/live"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].streamKey").value("key123"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void forceTerminate_authenticated_returns200() throws Exception {
        doNothing().when(adminStreamService).forceTerminate(eq("testkey"), any());

        mockMvc.perform(delete("/api/v1/admin/streams/live/testkey"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("TERMINATED"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void getFanOutProgress_returns200() throws Exception {
        FanOutProgressDto progress = new FanOutProgressDto();
        progress.setStreamKey("testkey");
        progress.setTotalFollowers(12000);
        progress.setBatchesTotal(12);
        progress.setBatchesCompleted(5);
        progress.setFeedCardsWritten(5000);
        progress.setStatus("IN_PROGRESS");

        when(fanOutProgressService.getProgress("testkey")).thenReturn(progress);

        mockMvc.perform(get("/api/v1/admin/streams/testkey/fanout"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalFollowers").value(12000))
            .andExpect(jsonPath("$.batchesTotal").value(12))
            .andExpect(jsonPath("$.status").value("IN_PROGRESS"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void analyticsSummary_returns200() throws Exception {
        when(adminStreamService.getSummary()).thenReturn(Map.of(
            "streamsToday", 42,
            "peakConcurrentViewers", 1200,
            "totalWatchMinutes", 3600,
            "vodStorageBytes", 1073741824L
        ));

        mockMvc.perform(get("/api/v1/admin/streams/analytics/summary"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.streamsToday").value(42));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void warnBroadcaster_missingMessage_returns400() throws Exception {
        mockMvc.perform(post("/api/v1/admin/streams/live/testkey/warn")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
            .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void setVisibility_invalidValue_returns400() throws Exception {
        mockMvc.perform(put("/api/v1/admin/streams/live/testkey/visibility")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"visibility\":\"INVALID\"}"))
            .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void enableConnector_returns200() throws Exception {
        doNothing().when(connectorRegistry).setEnabled(StreamPlatform.YOUTUBE, true);

        mockMvc.perform(put("/api/v1/admin/streams/connectors/YOUTUBE/enable"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.enabled").value(true));
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private LiveSessionDto buildMockSession(String streamKey) {
        LiveSessionDto dto = new LiveSessionDto();
        dto.setStreamKey(streamKey);
        dto.setUserId(UUID.randomUUID());
        dto.setPlatform(StreamPlatform.MOBILE_IOS);
        dto.setStatus(StreamStatus.LIVE);
        dto.setViewerCount(42);
        dto.setStartedAt(Instant.now());
        dto.setHlsManifestUrl("https://cdn.starjamz.com/hls/" + streamKey + "/playlist.m3u8");
        return dto;
    }
}
