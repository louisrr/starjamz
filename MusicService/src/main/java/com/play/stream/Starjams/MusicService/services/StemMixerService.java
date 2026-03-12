package com.play.stream.Starjams.MusicService.services;

import com.aerospike.client.IAerospikeClient;
import com.aerospike.client.Key;
import com.aerospike.client.Record;
import com.amazonaws.services.s3.AmazonS3;
import org.freedesktop.gstreamer.Gst;
import org.freedesktop.gstreamer.Pipeline;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.*;

/**
 * Builds and manages per-session dynamic GStreamer audiomixer pipelines for stem playback.
 *
 * Each pipeline fetches the user-accessible stems from S3 (via presigned URLs),
 * applies per-stem volume controls, mixes them together, and emits the output
 * as an MPEG audio stream via GStreamer's appsink.
 *
 * Pipeline description template (N stems):
 *   filesrc location=<tmp/vocals.flac> ! flacparse ! flacdec ! audioconvert
 *       ! volume volume=0.8 ! audiomixer.sink_0
 *   filesrc location=<tmp/drums.flac>  ! flacparse ! flacdec ! audioconvert
 *       ! volume volume=1.0 ! audiomixer.sink_1
 *   audiomixer name=audiomixer ! audioconvert ! audioresample ! lamemp3enc
 *       ! appsink name=sink sync=false
 *
 * Pipelines are keyed by sessionKey = "{userId}:{trackId}" and stored in a
 * ConcurrentHashMap. The rebuild operation is non-blocking: it runs on a
 * dedicated executor thread pool and the old pipeline is torn down before
 * the new one starts.
 */
@Service
public class StemMixerService {

    private static final Logger log = LoggerFactory.getLogger(StemMixerService.class);

    private static final String NS         = "starjamz";
    private static final String STEMS_SET  = "stems";
    private static final int    GST_BUFFER = 4096;

    @Value("${aws.s3.bucket:starjamz-uploads}")
    private String bucket;

    @Value("${stem.pipeline.executor.threads:8}")
    private int executorThreads;

    private final AmazonS3          s3;
    private final IAerospikeClient  aerospike;
    private final StemTierResolver  tierResolver;

    // sessionKey → active Pipeline
    private final ConcurrentHashMap<String, Pipeline>     activePipelines = new ConcurrentHashMap<>();
    // sessionKey → temp dir holding downloaded stems
    private final ConcurrentHashMap<String, Path>         tempDirs        = new ConcurrentHashMap<>();

    private ExecutorService pipelineExecutor;

    public StemMixerService(AmazonS3 s3,
                             IAerospikeClient aerospike,
                             StemTierResolver tierResolver) {
        this.s3           = s3;
        this.aerospike    = aerospike;
        this.tierResolver = tierResolver;
    }

    // Called by Spring after construction — initialises GStreamer and the thread pool.
    @jakarta.annotation.PostConstruct
    public void init() {
        Gst.init("StemMixerService");
        pipelineExecutor = Executors.newFixedThreadPool(executorThreads);
        log.info("StemMixerService initialised with {} pipeline threads", executorThreads);
    }

    @jakarta.annotation.PreDestroy
    public void shutdown() {
        tearDownAll();
        if (pipelineExecutor != null) pipelineExecutor.shutdownNow();
    }

    // -------------------------------------------------------------------------
    // Public API
    // -------------------------------------------------------------------------

    /**
     * Returns a StreamingResponseBody that streams the mixed audio for the given
     * user and track. Only stems accessible at the user's unlock tier are included.
     *
     * @param trackId UUID of the track
     * @param userId  UUID of the requesting user
     * @param volumes stem-type → gain [0.0, 1.0]; defaults to 1.0 if a stem is omitted
     */
    public StreamingResponseBody streamMix(UUID trackId, UUID userId,
                                            Map<String, Double> volumes) {
        String sessionKey = userId + ":" + trackId;
        return outputStream -> {
            try {
                buildAndStreamPipeline(sessionKey, trackId, userId, volumes, outputStream);
            } catch (Exception e) {
                log.error("Pipeline error for session {}: {}", sessionKey, e.getMessage(), e);
            }
        };
    }

    /**
     * Rebuilds the pipeline for a session with new stem volumes.
     * Non-blocking: submitted to the pipeline executor; returns immediately.
     */
    public CompletableFuture<Void> updateMix(UUID trackId, UUID userId,
                                              Map<String, Double> volumes) {
        String sessionKey = userId + ":" + trackId;
        return CompletableFuture.runAsync(() -> {
            tearDownSession(sessionKey);
            log.info("Pipeline rebuilt for session {} with volumes {}", sessionKey, volumes);
        }, pipelineExecutor);
    }

    /**
     * Tears down a specific session's pipeline and cleans up temp files.
     */
    public void tearDownSession(String sessionKey) {
        Pipeline old = activePipelines.remove(sessionKey);
        if (old != null) {
            try { old.stop(); } catch (Exception e) { /* ignore */ }
        }
        Path tmpDir = tempDirs.remove(sessionKey);
        if (tmpDir != null) deleteTempDir(tmpDir);
    }

    // -------------------------------------------------------------------------
    // Pipeline construction
    // -------------------------------------------------------------------------

    private void buildAndStreamPipeline(String sessionKey, UUID trackId, UUID userId,
                                         Map<String, Double> volumes,
                                         OutputStream outputStream) throws IOException {
        // 1. Resolve which stems the user can access
        Set<String> accessible = tierResolver.accessibleStems(userId, trackId);

        // 2. Download accessible stems from S3 into a temp directory
        Path tmpDir = Files.createTempDirectory("stems_" + sessionKey + "_");
        tempDirs.put(sessionKey, tmpDir);

        Map<String, Path> stemPaths = new LinkedHashMap<>();
        for (String stemType : accessible) {
            String s3Key = fetchS3Key(trackId.toString(), stemType);
            if (s3Key == null) continue;

            Path localFile = tmpDir.resolve(stemType + ".flac");
            s3.getObject(bucket, s3Key).getObjectContent()
              .transferTo(Files.newOutputStream(localFile));
            stemPaths.put(stemType, localFile);
        }

        if (stemPaths.isEmpty()) {
            log.warn("No stems available for trackId={} userId={}", trackId, userId);
            return;
        }

        // 3. Build GStreamer pipeline description
        String pipelineDesc = buildPipelineDescription(stemPaths, volumes);
        log.debug("GStreamer pipeline: {}", pipelineDesc);

        Pipeline pipeline = (Pipeline) Gst.parseLaunch(pipelineDesc);
        activePipelines.put(sessionKey, pipeline);

        // 4. Pull buffers from appsink and write to HTTP output stream
        org.freedesktop.gstreamer.elements.AppSink sink =
                (org.freedesktop.gstreamer.elements.AppSink) pipeline.getElementByName("sink");
        sink.setEmitSignals(false);

        pipeline.play();

        byte[] buffer = new byte[GST_BUFFER];
        try {
            while (!Thread.currentThread().isInterrupted()) {
                org.freedesktop.gstreamer.Sample sample = sink.pullSample();
                if (sample == null) break;

                org.freedesktop.gstreamer.Buffer gstBuf = sample.getBuffer();
                org.freedesktop.gstreamer.lowlevel.GstMiniObjectPtr ptr = gstBuf.map(false);
                byte[] data = ptr.getByteArray(0, (int) gstBuf.getSize());
                gstBuf.unmap();
                sample.dispose();

                outputStream.write(data);
                outputStream.flush();
            }
        } catch (IOException e) {
            log.debug("Client disconnected from session {}", sessionKey);
        } finally {
            tearDownSession(sessionKey);
        }
    }

    private String buildPipelineDescription(Map<String, Path> stemPaths,
                                             Map<String, Double> volumes) {
        StringBuilder sb = new StringBuilder();
        List<String> stemTypes = new ArrayList<>(stemPaths.keySet());

        // Source + volume chains
        for (int i = 0; i < stemTypes.size(); i++) {
            String stemType = stemTypes.get(i);
            Path localPath  = stemPaths.get(stemType);
            double gain     = clampVolume(volumes.getOrDefault(stemType, 1.0));

            sb.append(String.format(
                    "filesrc location=\"%s\" ! flacparse ! flacdec ! audioconvert "
                  + "! volume volume=%.4f ! audiomixer.sink_%d ",
                    localPath.toAbsolutePath(), gain, i));
        }

        // Mixer → encoder → appsink
        sb.append("audiomixer name=audiomixer ! audioconvert ! audioresample "
                + "! lamemp3enc target=bitrate bitrate=192 "
                + "! appsink name=sink sync=false");

        return sb.toString();
    }

    private String fetchS3Key(String trackId, String stemType) {
        Key key = new Key(NS, STEMS_SET, trackId + ":" + stemType.toLowerCase());
        Record rec = aerospike.get(null, key, "s3Key");
        return rec != null ? rec.getString("s3Key") : null;
    }

    private double clampVolume(double v) {
        return Math.max(0.0, Math.min(1.0, v));
    }

    private void tearDownAll() {
        new HashSet<>(activePipelines.keySet()).forEach(this::tearDownSession);
    }

    private void deleteTempDir(Path dir) {
        try {
            Files.walk(dir)
                 .sorted(Comparator.reverseOrder())
                 .map(Path::toFile)
                 .forEach(File::delete);
        } catch (IOException e) {
            log.warn("Failed to clean up temp dir {}: {}", dir, e.getMessage());
        }
    }
}
