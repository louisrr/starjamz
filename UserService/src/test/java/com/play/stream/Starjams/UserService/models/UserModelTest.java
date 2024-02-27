package com.play.stream.Starjams.UserService.models;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;

class UserModelTest {

    private UserModel userModel;
    private final UUID uuid = UUID.randomUUID();
    private final double[] coords = {40.712776, -74.005974};

    @BeforeEach
    void setUp() {
        userModel = new UserModel();
        userModel.setU(uuid);
        userModel.setScreenName("JohnDoe");
        userModel.setUserName("john_doe");
        userModel.setEmail("johndoe@example.com");
        userModel.setEmailHash("hashedemail");
        userModel.setPassword("password");
        userModel.setDevice("device");
        userModel.setUserAgent("userAgent");
        userModel.setUrl("http://example.com");
        userModel.setAviAddress("aviAddress");
        userModel.setCoverAddress("coverAddress");
        userModel.setLastMusicListen(UUID.randomUUID());
        userModel.setLastAudioStream(UUID.randomUUID());
        userModel.setLastVideoStream(UUID.randomUUID());
        userModel.setBio("This is a bio");
        userModel.setCoords(coords);
    }

    @Test
    void testGetters() {
        assertEquals(uuid, userModel.getU());
        assertEquals("JohnDoe", userModel.getScreenName());
        assertEquals("john_doe", userModel.getUserName());
        assertEquals("johndoe@example.com", userModel.getEmail());
        assertEquals("hashedemail", userModel.getEmailHash());
        assertEquals("password", userModel.getPassword());
        assertEquals("device", userModel.getDevice());
        assertEquals("userAgent", userModel.getUserAgent());
        assertEquals("http://example.com", userModel.getUrl());
        assertEquals("aviAddress", userModel.getAviAddress());
        assertEquals("coverAddress", userModel.getCoverAddress());
        assertNotNull(userModel.getLastMusicListen());
        assertNotNull(userModel.getLastAudioStream());
        assertNotNull(userModel.getLastVideoStream());
        assertEquals("This is a bio", userModel.getBio());
        assertArrayEquals(coords, userModel.getCoords());
    }

    @Test
    void testEquals() {
        UserModel anotherUser = new UserModel();
        anotherUser.setU(uuid); // Set the same UUID to ensure equality
        assertEquals(userModel, anotherUser);
    }

    @Test
    void testHashCode() {
        UserModel anotherUser = new UserModel();
        anotherUser.setU(uuid); // Set the same UUID to ensure same hashcode
        assertEquals(userModel.hashCode(), anotherUser.hashCode());
    }

    @Test
    void testToString() {
        String expected = "UserModel{u=" + userModel.getU() +
                ", screenName='JohnDoe'" +
                ", userName='john_doe'" +
                ", email='johndoe@example.com'" +
                ", emailHash='hashedemail'" +
                ", password='password'" +
                ", device='device'" +
                ", userAgent='userAgent'" +
                ", url='http://example.com'" +
                ", aviAddress='aviAddress'" +
                ", coverAddress='coverAddress'" +
                ", lastMusicListen=" + userModel.getLastMusicListen() +
                ", lastAudioStream=" + userModel.getLastAudioStream() +
                ", lastVideoStream=" + userModel.getLastVideoStream() +
                ", bio='This is a bio'" +
                ", coords=" + Arrays.toString(userModel.getCoords()) +
                '}';
        assertEquals(expected, userModel.toString());
    }
}
