package com.play.stream.Starjams.UserService.models;

import com.google.common.base.Objects;
import org.springframework.data.annotation.Transient;
import org.springframework.data.cassandra.core.mapping.Column;
import org.springframework.data.cassandra.core.mapping.PrimaryKey;

import java.util.Arrays;
import java.util.UUID;

public class UserModel {

    @PrimaryKey
    private UUID u;

    @Column
    private String screenName; // shown on the user page

    @Column
    private String userName; // shown in the URL

    @Column
    private String email;

    @Column
    private String emailHash;

    @Column
    private String password;

    @Column
    private String device;

    @Column
    private String userAgent;

    @Column
    private String url;

    @Column
    private String aviAddress;

    @Column
    private String coverAddress;

    @Column
    private UUID lastMusicListen;

    @Column
    private UUID lastAudioStream;

    @Column
    private UUID lastVideoStream;

    @Column
    private String bio;

    @Transient
    private double[] coords; // Geographic location, ScyllaDB coords tuple (lat/lon)

    public UUID getU() {
        return u;
    }

    public String getScreenName() {
        return screenName;
    }

    public String getUserName() {
        return userName;
    }

    public String getEmail() {
        return email;
    }

    public String getEmailHash() {
        return emailHash;
    }

    public String getPassword() {
        return password;
    }

    public String getDevice() {
        return device;
    }

    public String getUserAgent() {
        return userAgent;
    }

    public String getUrl() {
        return url;
    }

    public String getAviAddress() {
        return aviAddress;
    }

    public String getCoverAddress() {
        return coverAddress;
    }

    public UUID getLastMusicListen() {
        return lastMusicListen;
    }

    public UUID getLastAudioStream() {
        return lastAudioStream;
    }

    public UUID getLastVideoStream() {
        return lastVideoStream;
    }

    public String getBio() {
        return bio;
    }

    public double[] getCoords() {
        return coords;
    }

    public void setU(UUID u) {
        this.u = u;
    }

    public void setScreenName(String screenName) {
        this.screenName = screenName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public void setEmailHash(String emailHash) {
        this.emailHash = emailHash;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public void setDevice(String device) {
        this.device = device;
    }

    public void setUserAgent(String userAgent) {
        this.userAgent = userAgent;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public void setAviAddress(String aviAddress) {
        this.aviAddress = aviAddress;
    }

    public void setCoverAddress(String coverAddress) {
        this.coverAddress = coverAddress;
    }

    public void setLastMusicListen(UUID lastMusicListen) {
        this.lastMusicListen = lastMusicListen;
    }

    public void setLastAudioStream(UUID lastAudioStream) {
        this.lastAudioStream = lastAudioStream;
    }

    public void setLastVideoStream(UUID lastVideoStream) {
        this.lastVideoStream = lastVideoStream;
    }

    public void setBio(String bio) {
        this.bio = bio;
    }

    public void setCoords(double[] coords) {
        this.coords = coords;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof UserModel)) return false;
        UserModel userModel = (UserModel) o;
        return Objects.equal(getU(), userModel.getU()) && Objects.equal(getScreenName(), userModel.getScreenName()) && Objects.equal(getUserName(), userModel.getUserName()) && Objects.equal(getEmail(), userModel.getEmail()) && Objects.equal(getEmailHash(), userModel.getEmailHash()) && Objects.equal(getPassword(), userModel.getPassword()) && Objects.equal(getDevice(), userModel.getDevice()) && Objects.equal(getUserAgent(), userModel.getUserAgent()) && Objects.equal(getUrl(), userModel.getUrl()) && Objects.equal(getAviAddress(), userModel.getAviAddress()) && Objects.equal(getCoverAddress(), userModel.getCoverAddress()) && Objects.equal(getLastMusicListen(), userModel.getLastMusicListen()) && Objects.equal(getLastAudioStream(), userModel.getLastAudioStream()) && Objects.equal(getLastVideoStream(), userModel.getLastVideoStream()) && Objects.equal(getBio(), userModel.getBio()) && Objects.equal(getCoords(), userModel.getCoords());
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(getU(), getScreenName(), getUserName(), getEmail(), getEmailHash(), getPassword(), getDevice(), getUserAgent(), getUrl(), getAviAddress(), getCoverAddress(), getLastMusicListen(), getLastAudioStream(), getLastVideoStream(), getBio(), getCoords());
    }

    @Override
    public String toString() {
        return "UserModel{" +
                "u=" + u +
                ", screenName='" + screenName + '\'' +
                ", userName='" + userName + '\'' +
                ", email='" + email + '\'' +
                ", emailHash='" + emailHash + '\'' +
                ", password='" + password + '\'' +
                ", device='" + device + '\'' +
                ", userAgent='" + userAgent + '\'' +
                ", url='" + url + '\'' +
                ", aviAddress='" + aviAddress + '\'' +
                ", coverAddress='" + coverAddress + '\'' +
                ", lastMusicListen=" + lastMusicListen +
                ", lastAudioStream=" + lastAudioStream +
                ", lastVideoStream=" + lastVideoStream +
                ", bio='" + bio + '\'' +
                ", coords=" + Arrays.toString(coords) +
                '}';
    }

    public UUID getId() {
        return u;
    }
}
