package com.play.stream.Starjams.GatewayService.controllers;

import com.play.stream.Starjams.GatewayService.services.GatewayService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Enumeration;

@RestController
@RequestMapping("/gateway")
public class PlaylistGatewayController {

    private static final String GATEWAY_PREFIX = "/gateway/";

    private final GatewayService gatewayService;

    public PlaylistGatewayController(GatewayService gatewayService) {
        this.gatewayService = gatewayService;
    }

    @RequestMapping(value = "/{service}/**", method = {
            RequestMethod.GET,
            RequestMethod.POST,
            RequestMethod.PUT,
            RequestMethod.PATCH,
            RequestMethod.DELETE
    })
    public ResponseEntity<String> proxyRequest(
            @PathVariable String service,
            HttpMethod method,
            HttpServletRequest request,
            @RequestBody(required = false) String body) {

        String requestUri = request.getRequestURI();
        String servicePrefix = GATEWAY_PREFIX + service;
        String path = requestUri.startsWith(servicePrefix)
                ? requestUri.substring(servicePrefix.length())
                : "/";

        HttpHeaders headers = new HttpHeaders();
        Enumeration<String> headerNames = request.getHeaderNames();
        while (headerNames.hasMoreElements()) {
            String headerName = headerNames.nextElement();
            headers.add(headerName, request.getHeader(headerName));
        }

        return gatewayService.proxyRequest(
                service,
                method,
                path,
                request.getQueryString(),
                headers,
                body);
    }
}
