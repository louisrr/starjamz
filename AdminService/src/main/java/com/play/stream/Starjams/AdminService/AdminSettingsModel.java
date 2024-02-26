package com.play.stream.Starjams.AdminService;

import org.springframework.data.cassandra.core.mapping.Column;
import org.springframework.data.cassandra.core.mapping.PrimaryKey;
import org.springframework.data.cassandra.core.mapping.Table;
import java.util.UUID;

// Scylla is NoSQL, we don't need JPA, or Hibernate here. Google the CAP Theorem 👍

@Table
public class AdminSettingsModel {

    @PrimaryKey
    private UUID id;

    @Column
    private String serviceName;

    @Column
    private String serviceOwner;

    @Column
    private String charset;

    @Column
    private String viewport;

    @Column
    private String description;

    @Column
    private String keywords;

    @Column
    private String author;

    @Column
    private String refresh;

    @Column
    private String robots;

    @Column
    private String contentType;

    @Column
    private String xUaCompatible;

    @Column
    private String ogTitle;

    @Column
    private String ogDescription;

    @Column
    private String ogImage;

    @Column
    private String ogUrl;

    @Column
    private String twitterCard;

    @Column
    private String twitterTitle;

    @Column
    private String twitterDescription;

    @Column
    private String twitterImage;

    @Column
    private boolean iosOnline; // determines whether the iOS app is accessible or not

    @Column
    private boolean androidOnline; // determines whether the Android app is accessible or not

    @Column
    private boolean webOnline; // determines whether the web app is accessible or not

    @Column
    private boolean carousel; // carousel on landing page or nah

    @Column
    private int tracksPromotedOnHomePage;

    @Column
    private int videosPromotedOnLandingPage;

    @Column
    private boolean searchBoxOnLandingPage;

    @Column
    private boolean comingSoonMode; // iOS, Android, Web retained to a coming soon view

    public UUID getId() {
        return id;
    }

    public String getServiceName() {
        return serviceName;
    }

    public String getServiceOwner() {
        return serviceOwner;
    }

    public String getCharset() {
        return charset;
    }

    public String getViewport() {
        return viewport;
    }

    public String getDescription() {
        return description;
    }

    public String getKeywords() {
        return keywords;
    }

    public String getAuthor() {
        return author;
    }

    public String getRefresh() {
        return refresh;
    }

    public String getRobots() {
        return robots;
    }

    public String getContentType() {
        return contentType;
    }

    public String getxUaCompatible() {
        return xUaCompatible;
    }

    public String getOgTitle() {
        return ogTitle;
    }

    public String getOgDescription() {
        return ogDescription;
    }

    public String getOgImage() {
        return ogImage;
    }

    public String getOgUrl() {
        return ogUrl;
    }

    public String getTwitterCard() {
        return twitterCard;
    }

    public String getTwitterTitle() {
        return twitterTitle;
    }

    public String getTwitterDescription() {
        return twitterDescription;
    }

    public String getTwitterImage() {
        return twitterImage;
    }

    public boolean isIosOnline() {
        return iosOnline;
    }

    public boolean isAndroidOnline() {
        return androidOnline;
    }

    public boolean isWebOnline() {
        return webOnline;
    }

    public boolean isCarousel() {
        return carousel;
    }

    public int getTracksPromotedOnHomePage() {
        return tracksPromotedOnHomePage;
    }

    public int getVideosPromotedOnLandingPage() {
        return videosPromotedOnLandingPage;
    }

    public boolean isSearchBoxOnLandingPage() {
        return searchBoxOnLandingPage;
    }

    public boolean isComingSoonMode() {
        return comingSoonMode;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }

    public void setServiceOwner(String serviceOwner) {
        this.serviceOwner = serviceOwner;
    }

    public void setCharset(String charset) {
        this.charset = charset;
    }

    public void setViewport(String viewport) {
        this.viewport = viewport;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setKeywords(String keywords) {
        this.keywords = keywords;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public void setRefresh(String refresh) {
        this.refresh = refresh;
    }

    public void setRobots(String robots) {
        this.robots = robots;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public void setxUaCompatible(String xUaCompatible) {
        this.xUaCompatible = xUaCompatible;
    }

    public void setOgTitle(String ogTitle) {
        this.ogTitle = ogTitle;
    }

    public void setOgDescription(String ogDescription) {
        this.ogDescription = ogDescription;
    }

    public void setOgImage(String ogImage) {
        this.ogImage = ogImage;
    }

    public void setOgUrl(String ogUrl) {
        this.ogUrl = ogUrl;
    }

    public void setTwitterCard(String twitterCard) {
        this.twitterCard = twitterCard;
    }

    public void setTwitterTitle(String twitterTitle) {
        this.twitterTitle = twitterTitle;
    }

    public void setTwitterDescription(String twitterDescription) {
        this.twitterDescription = twitterDescription;
    }

    public void setTwitterImage(String twitterImage) {
        this.twitterImage = twitterImage;
    }

    public void setIosOnline(boolean iosOnline) {
        this.iosOnline = iosOnline;
    }

    public void setAndroidOnline(boolean androidOnline) {
        this.androidOnline = androidOnline;
    }

    public void setWebOnline(boolean webOnline) {
        this.webOnline = webOnline;
    }

    public void setCarousel(boolean carousel) {
        this.carousel = carousel;
    }

    public void setTracksPromotedOnHomePage(int tracksPromotedOnHomePage) {
        this.tracksPromotedOnHomePage = tracksPromotedOnHomePage;
    }

    public void setVideosPromotedOnLandingPage(int videosPromotedOnLandingPage) {
        this.videosPromotedOnLandingPage = videosPromotedOnLandingPage;
    }

    public void setSearchBoxOnLandingPage(boolean searchBoxOnLandingPage) {
        this.searchBoxOnLandingPage = searchBoxOnLandingPage;
    }

    public void setComingSoonMode(boolean comingSoonMode) {
        this.comingSoonMode = comingSoonMode;
    }
}
