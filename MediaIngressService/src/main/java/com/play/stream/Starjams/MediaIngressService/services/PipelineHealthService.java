package com.play.stream.Starjams.MediaIngressService.services;

import com.play.stream.Starjams.MediaIngressService.model.PipelineHealthRecord;
import com.play.stream.Starjams.MediaIngressService.model.StreamPlatform;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * In-memory store of GStreamer pipeline health metrics.
 *
 * <p>All GStreamer errors (Bus.ERROR) and EOS events are funnelled here by connectors
 * and the StreamRouter. The admin {@code GET /ingest/health} endpoint reads from this store.
 *
 * <p>Metrics are reset when a pipeline is removed (stream ended or terminated).
 */
@Service
public class PipelineHealthService {

    private final ConcurrentMap<String, PipelineHealthRecord> pipelines = new ConcurrentHashMap<>();

    public void registerPipeline(String streamKey, StreamPlatform platform) {
        pipelines.put(streamKey, new PipelineHealthRecord(streamKey, platform));
    }

    public void recordError(String streamKey, String errorMessage) {
        PipelineHealthRecord rec = pipelines.get(streamKey);
        if (rec != null) {
            rec.recordError(errorMessage);
        }
    }

    public void updateBitrate(String streamKey, long bitrateKbps) {
        PipelineHealthRecord rec = pipelines.get(streamKey);
        if (rec != null) {
            rec.setBitrateKbps(bitrateKbps);
        }
    }

    public void recordDroppedFrame(String streamKey) {
        PipelineHealthRecord rec = pipelines.get(streamKey);
        if (rec != null) {
            rec.incrementDroppedFrames();
        }
    }

    public void deactivatePipeline(String streamKey) {
        PipelineHealthRecord rec = pipelines.get(streamKey);
        if (rec != null) {
            rec.setActive(false);
        }
    }

    public void removePipeline(String streamKey) {
        pipelines.remove(streamKey);
    }

    public PipelineHealthRecord getHealth(String streamKey) {
        return pipelines.get(streamKey);
    }

    public List<PipelineHealthRecord> getAllActive() {
        List<PipelineHealthRecord> result = new ArrayList<>();
        for (PipelineHealthRecord rec : pipelines.values()) {
            if (rec.isActive()) result.add(rec);
        }
        return result;
    }

    public List<PipelineHealthRecord> getAll() {
        return new ArrayList<>(pipelines.values());
    }
}
