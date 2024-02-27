package com.play.stream.Starjams.UserService.services;

import com.datastax.oss.driver.api.core.CqlSession;
import com.play.stream.Starjams.UserService.models.UserModel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class UserServiceImplTest {

    @Mock
    private CqlSession cqlSession;

    @InjectMocks
    private UserServiceImpl userService;

    private final UUID userId = UUID.randomUUID();
    private UserModel testUser;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        testUser = new UserModel();
        testUser.setEmail("test@example.com");
        testUser.setAviAddress("http://example.com/avatar.jpg");
        testUser.setBio("Test Bio");
        // Mocking the CqlSession behavior could go here if necessary
    }

    @Test
    void createUser() {
        // Assume the createUser method just executes a query without checking the result
        userService.createUser(testUser);
        verify(cqlSession, times(1)).execute(anyString());
    }

    @Test
    void findByIdNotFound() {
        // Assuming findById returns an empty Optional if the user is not found
        when(cqlSession.execute(anyString())).thenReturn(null); // Simplified for demonstration
        Optional<UserModel> result = userService.findById(userId);
        assertTrue(result.isEmpty());
    }

    @Test
    void updateUser() {
        // Assuming the updateUser just executes an update query
        userService.updateUser(testUser);
        verify(cqlSession, times(1)).execute(anyString());
    }

    @Test
    void addAvatarAddress() {
        // This test assumes that after adding an avatar address, the user is "found"
        userService.addAvatarAddress(userId, "http://example.com/new-avatar.jpg");
        verify(cqlSession, times(1)).execute(anyString());
    }

    @Test
    void deleteUser() {
        userService.deleteUser(userId);
        verify(cqlSession, times(1)).execute(anyString());
    }

    // Additional tests for changeEmail, addSecondaryEmail, and addBio would follow a similar pattern.
}
