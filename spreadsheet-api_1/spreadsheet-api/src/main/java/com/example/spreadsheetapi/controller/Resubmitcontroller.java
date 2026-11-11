package com.example.spreadsheetapi.controller;

import com.example.spreadsheetapi.dto.FieldValue;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;

/**
 * Passes the approved fields on to the RISE "resubmit claim tax file" API.
 * The browser can't call RISE directly (CORS), and the bearer token should never sit in the page,
 * so the call is made from here.
 *
 * Settings (environment variables on Render):
 *   RISE_TOKEN           bearer token (without the word "Bearer")
 *   RISE_RESUBMIT_URL    default https://api.rise.co.za/resubmit-claim-tax-file
 *   RISE_RESUBMIT_METHOD default POST
 */
@RestController
@RequestMapping("/api/spreadsheet")
public class Resubmitcontroller {

    private final ObjectMapper mapper;
    private final String url;
    private final String method;
    private final String defaultToken;
    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(15)).build();

    public Resubmitcontroller(ObjectMapper mapper,
                              @Value("${rise.resubmit-url}") String url,
                              @Value("${rise.resubmit-method}") String method,
                              @Value("${rise.token:}") String defaultToken) {
        this.mapper = mapper;
        this.url = url;
        this.method = method;
        this.defaultToken = defaultToken;
    }

    @PostMapping(value = "/resubmit", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> resubmit(@RequestParam("claimId") long claimId,
                                           @RequestBody List<FieldValue> changes,
                                           @RequestHeader(value = "X-Rise-Token", required = false) String pageToken)
            throws IOException, InterruptedException {

        if (changes == null || changes.isEmpty()) {
            return ResponseEntity.badRequest().body("No fields to resubmit.");
        }
        String token = (pageToken != null && !pageToken.isBlank()) ? pageToken.trim() : defaultToken;
        if (token == null || token.isBlank()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("No RISE token. Set RISE_TOKEN on the server or enter one under Request details.");
        }

        HttpRequest request = HttpRequest.newBuilder(URI.create(url + "?claimId=" + claimId))
                .timeout(Duration.ofSeconds(60))
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .header("Authorization", "Bearer " + token.replaceFirst("(?i)^bearer\\s+", ""))
                .method(method.toUpperCase(), HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(changes)))
                .build();

        HttpResponse<String> upstream = http.send(request, HttpResponse.BodyHandlers.ofString());
        MediaType type = upstream.headers().firstValue("Content-Type")
                .map(MediaType::parseMediaType).orElse(MediaType.TEXT_PLAIN);
        // Same status and body RISE gave, so the page can show RISE's own error message.
        return ResponseEntity.status(upstream.statusCode()).contentType(type).body(upstream.body());
    }
}