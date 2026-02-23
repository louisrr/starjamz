package com.stream.Starjams.GatewayService.datatransferobjects;

import com.google.common.base.Objects;

import java.util.LinkedHashMap;
import java.util.UUID;

public class PlaylistDTO {
    private UUID id;
    private String username;
    private LinkedHashMap<UUID, String> tracks;

    public PlaylistDTO(UUID id, String username, LinkedHashMap<UUID, String> tracks) {
        this.id = id;
        this.username = username;
        this.tracks = tracks;
    }

    public PlaylistDTO(UUID id, String username) {
        this.id = id;
        this.username = username;
    }

    public PlaylistDTO(UUID id) {
        this.id = id;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public LinkedHashMap<UUID, String> getTracks() {
        return tracks;
    }

    public void setTracks(LinkedHashMap<UUID, String> tracks) {
        this.tracks = tracks;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PlaylistDTO)) return false;
        PlaylistDTO that = (PlaylistDTO) o;
        return Objects.equal(getId(), that.getId()) && Objects.equal(getUsername(), that.getUsername()) && Objects.equal(getTracks(), that.getTracks());
    }

    public int compareTo(UUID val) {
        return id.compareTo(val);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(getId(), getUsername(), getTracks());
    }

    @Override
    public String toString() {
        return "PlaylistDTO{" +
                "id=" + id +
                ", username='" + username + '\'' +
                ", tracks=" + tracks +
                '}';
    }

    public static UUID randomUUID() {
        return UUID.randomUUID();
    }

    public static UUID nameUUIDFromBytes(byte[] name) {
        return UUID.nameUUIDFromBytes(name);
    }

    public static UUID fromString(String name) {
        return UUID.fromString(name);
    }

    public long getLeastSignificantBits() {
        return id.getLeastSignificantBits();
    }

    public long getMostSignificantBits() {
        return id.getMostSignificantBits();
    }

    public int version() {
        return id.version();
    }

    public int variant() {
        return id.variant();
    }

    public long timestamp() {
        return id.timestamp();
    }

    public int clockSequence() {
        return id.clockSequence();
    }

    public long node() {
        return id.node();
    }


}