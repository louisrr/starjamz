package com.play.stream.Starjams.MediaIngressService.dto;

import com.play.stream.Starjams.MediaIngressService.model.StreamPlatform;

public class ConnectorStatusDto {

    private StreamPlatform platform;
    private boolean enabled;
    private int activeConnections;
    private String healthStatus; // ACTIVE | DEGRADED | DISABLED

    // --- Getters & Setters ---

    public StreamPlatform getPlatform() { return platform; }
    public void setPlatform(StreamPlatform platform) { this.platform = platform; }

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }

    public int getActiveConnections() { return activeConnections; }
    public void setActiveConnections(int activeConnections) { this.activeConnections = activeConnections; }

    public String getHealthStatus() { return healthStatus; }
    public void setHealthStatus(String healthStatus) { this.healthStatus = healthStatus; }
}
