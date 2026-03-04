package com.play.stream.Starjams.UserService.controller;

import com.play.stream.Starjams.UserService.dto.SignupRequest;
import com.play.stream.Starjams.UserService.dto.SignupResponse;
import com.play.stream.Starjams.UserService.services.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * POST /signup
 *
 * Accepts { "contact": "...", "password": "..." }
 * Returns 202 on success, 409 on duplicate, 400 on bad input.
 */
@RestController
@RequestMapping("/signup")
public class SignupController {

    private final UserService userService;

    public SignupController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping
    public ResponseEntity<SignupResponse> signup(
            @Valid @RequestBody SignupRequest request,
            HttpServletRequest httpRequest) {

        String ip        = resolveIp(httpRequest);
        String userAgent = httpRequest.getHeader("User-Agent");
        String hostname  = resolveHostname(ip);

        SignupResponse response = userService.signup(request, ip, userAgent, hostname);

        if (response.userId() == null) {
            // Duplicate or invalid — determine correct status code from message
            boolean isDuplicate = response.message().startsWith("You cannot");
            HttpStatus status = isDuplicate ? HttpStatus.CONFLICT : HttpStatus.BAD_REQUEST;
            return ResponseEntity.status(status).body(response);
        }

        return ResponseEntity.status(HttpStatus.ACCEPTED).body(response);
    }

    // -------------------------------------------------------------------------
    // HTTP helpers
    // -------------------------------------------------------------------------

    /**
     * Extract the real client IP, honouring X-Forwarded-For when behind a proxy/load-balancer.
     */
    private String resolveIp(HttpServletRequest req) {
        String forwarded = req.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            // X-Forwarded-For can be a comma-separated list; the first entry is the client.
            return forwarded.split(",")[0].trim();
        }
        return req.getRemoteAddr();
    }

    /**
     * Attempt a reverse-DNS lookup of the client IP.
     * Falls back to the IP string itself if the lookup fails or takes too long.
     * In production this should be done off the hot path (e.g., async worker).
     */
    private String resolveHostname(String ip) {
        try {
            return InetAddress.getByName(ip).getCanonicalHostName();
        } catch (UnknownHostException e) {
            return ip;
        }
    }
}
