package com.play.stream.Starjams.UserService.models;

import com.google.common.base.Objects;

import java.util.Arrays;
import java.util.UUID;

public class UserModel {

    private UUID u;
    private String screenName; // shown on the user page
    private String userName;   // shown in the URL
    private String email;
    private String emailHash;
    private String phoneNumber;
    private String password;
    private String device;
    private String userAgent;
    private String url;
    private String aviAddress;
    private String coverAddress;
    private UUID lastMusicListen;
    private UUID lastAudioStream;
    private UUID lastVideoStream;
    private String bio;
    private double[] coords; // Geographic location (lat/lon)

    // Registration / analytics fields
    private long signupTimestamp;  // epoch seconds
    private long lastOnline;       // epoch seconds
    private int following;
    private int likes;
    private int shares;
    private String ipAddress;
    private String hostname;
    private String geolocation;
    private int mostViewedGenre;
    private int followerCount;
    private Gender gender;

    // Transient signup fields (not persisted long-term)
    private boolean confirmed;

    // Password reset fields
    private int     authCode;          // 6-digit OTP stored during forgot-password flow
    private long    authCodeDate;      // epoch milliseconds when authCode was issued
    private boolean passwordChanged;   // set true after a successful password reset

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

    public String getPhoneNumber() { return phoneNumber; }
    public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }

    public long getSignupTimestamp() { return signupTimestamp; }
    public void setSignupTimestamp(long signupTimestamp) { this.signupTimestamp = signupTimestamp; }

    public long getLastOnline() { return lastOnline; }
    public void setLastOnline(long lastOnline) { this.lastOnline = lastOnline; }

    public int getFollowing() { return following; }
    public void setFollowing(int following) { this.following = following; }

    public int getLikes() { return likes; }
    public void setLikes(int likes) { this.likes = likes; }

    public int getShares() { return shares; }
    public void setShares(int shares) { this.shares = shares; }

    public String getIpAddress() { return ipAddress; }
    public void setIpAddress(String ipAddress) { this.ipAddress = ipAddress; }

    public String getHostname() { return hostname; }
    public void setHostname(String hostname) { this.hostname = hostname; }

    public String getGeolocation() { return geolocation; }
    public void setGeolocation(String geolocation) { this.geolocation = geolocation; }

    public int getMostViewedGenre() { return mostViewedGenre; }
    public void setMostViewedGenre(int mostViewedGenre) { this.mostViewedGenre = mostViewedGenre; }

    public int getFollowerCount() { return followerCount; }
    public void setFollowerCount(int followerCount) { this.followerCount = followerCount; }

    public Gender getGender() { return gender; }
    public void setGender(Gender gender) { this.gender = gender; }

    public boolean isConfirmed() { return confirmed; }
    public void setConfirmed(boolean confirmed) { this.confirmed = confirmed; }

    public int getAuthCode() { return authCode; }
    public void setAuthCode(int authCode) { this.authCode = authCode; }

    public long getAuthCodeDate() { return authCodeDate; }
    public void setAuthCodeDate(long authCodeDate) { this.authCodeDate = authCodeDate; }

    public boolean isPasswordChanged() { return passwordChanged; }
    public void setPasswordChanged(boolean passwordChanged) { this.passwordChanged = passwordChanged; }

    public UUID getId() {
        return u;
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
}
