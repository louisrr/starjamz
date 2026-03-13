package com.play.stream.Starjams.MediaIngressService.dto;

public class FanOutProgressDto {

    private String streamKey;
    private long totalFollowers;
    private long batchesTotal;
    private long batchesCompleted;
    private long feedCardsWritten;
    private long failed;
    private String status; // IN_PROGRESS | DONE | PARTIAL

    // --- Getters & Setters ---

    public String getStreamKey() { return streamKey; }
    public void setStreamKey(String streamKey) { this.streamKey = streamKey; }

    public long getTotalFollowers() { return totalFollowers; }
    public void setTotalFollowers(long totalFollowers) { this.totalFollowers = totalFollowers; }

    public long getBatchesTotal() { return batchesTotal; }
    public void setBatchesTotal(long batchesTotal) { this.batchesTotal = batchesTotal; }

    public long getBatchesCompleted() { return batchesCompleted; }
    public void setBatchesCompleted(long batchesCompleted) { this.batchesCompleted = batchesCompleted; }

    public long getFeedCardsWritten() { return feedCardsWritten; }
    public void setFeedCardsWritten(long feedCardsWritten) { this.feedCardsWritten = feedCardsWritten; }

    public long getFailed() { return failed; }
    public void setFailed(long failed) { this.failed = failed; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
