package com.stream.Starjams.GatewayService.controllers;

import com.stream.Starjams.GatewayService.services.GatewayService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
@CrossOrigin // Configure the appropriate origins as per your security requirements
public class GatewayController {

    @Autowired
    private GatewayService gatewayService;

    @PostMapping("/signup")
    public ResponseEntity<?> signUp(@RequestBody Object signupDetails) {
        return gatewayService.handleSignup(signupDetails);
    }

    @GetMapping("/playlist")
    public ResponseEntity<?> getPlaylists() {
        return gatewayService.fetchPlaylists();
    }

    @PostMapping("/playlist")
    public ResponseEntity<?> createPlaylist(@RequestBody Object playlistDetails) {
        return gatewayService.createPlaylist(playlistDetails);
    }

    /**
    @GetMapping("/friendslist")
    public ResponseEntity<?> getFriendsList() {
        return gatewayService.fetchFriendsList();
    }

    @GetMapping("/followers")
    public ResponseEntity<?> getFollowers() {
        return gatewayService.fetchFollowers();
    }

    @GetMapping("/stream")
    public ResponseEntity<?> streamContent(@RequestParam String contentId) {
        return gatewayService.streamContent(contentId);
    }

    @PostMapping("/upload")
    public ResponseEntity<?> uploadContent(@RequestBody Object contentData) {
        return gatewayService.uploadContent(contentData);
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<?> forgotPassword(@RequestBody Object userData) {
        return gatewayService.handleForgotPassword(userData);
    }

    @PutMapping("/new-password")
    public ResponseEntity<?> newPassword(@RequestBody Object newPasswordData) {
        return gatewayService.updateNewPassword(newPasswordData);
    }

    @GetMapping("/about")
    public ResponseEntity<?> about() {
        return gatewayService.getAboutInfo();
    }

    @GetMapping("/legal")
    public ResponseEntity<?> legal() {
        return gatewayService.getLegalInfo();
    }

    @GetMapping("/contact")
    public ResponseEntity<?> contact() {
        return gatewayService.getContactInfo();
    }

    @GetMapping("/search")
    public ResponseEntity<?> search(@RequestParam String query) {
        return gatewayService.search(query);
    }

    @PostMapping("/comment")
    public ResponseEntity<?> postComment(@RequestBody Object commentData) {
        return gatewayService.postComment(commentData);
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<?> getUser(@PathVariable String userId) {
        return gatewayService.getUserInfo(userId);
    }
    */
    // Add more methods as needed
}

