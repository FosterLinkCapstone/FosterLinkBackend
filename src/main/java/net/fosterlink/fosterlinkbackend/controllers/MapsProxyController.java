package net.fosterlink.fosterlinkbackend.controllers;

import net.fosterlink.fosterlinkbackend.config.ratelimit.RateLimit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.regex.Pattern;

/**
 * Proxies Google Maps Static API requests server-side so the Maps API key is
 * never exposed to browser clients or bundled into frontend environment variables.
 * <p>
 * The endpoint is intentionally public (no authentication required) because agency
 * map thumbnails are visible on the public agencies page. Rate limiting per IP
 * protects against API-key abuse.
 */
@RestController
@RequestMapping("/v1/maps")
public class MapsProxyController {

    private static final Logger log = LoggerFactory.getLogger(MapsProxyController.class);

    private static final Pattern SAFE_SIZE = Pattern.compile("^\\d{1,4}x\\d{1,4}$");
    private static final int MAX_ZOOM = 21;

    private final String apiKey;
    private final HttpClient httpClient;

    public MapsProxyController(@Value("${google.maps.api.key}") String apiKey) {
        this.apiKey = apiKey;
        this.httpClient = HttpClient.newHttpClient();
    }

    @GetMapping("/static")
    @RateLimit(requests = 30, burstRequests = 10, burstDurationSeconds = 10, keyType = "IP")
    public ResponseEntity<byte[]> staticMap(
            @RequestParam String address,
            @RequestParam(defaultValue = "15") int zoom,
            @RequestParam(defaultValue = "300x200") String size
    ) {
        if (zoom < 0 || zoom > MAX_ZOOM) {
            return ResponseEntity.badRequest().build();
        }
        if (!SAFE_SIZE.matcher(size).matches()) {
            return ResponseEntity.badRequest().build();
        }

        try {
            String encodedAddress = URLEncoder.encode(address, StandardCharsets.UTF_8);
            String url = "https://maps.googleapis.com/maps/api/staticmap"
                    + "?center=" + encodedAddress
                    + "&zoom=" + zoom
                    + "&size=" + size
                    + "&markers=color:red%7C" + encodedAddress
                    + "&key=" + apiKey;

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .GET()
                    .build();

            HttpResponse<byte[]> response = httpClient.send(request, HttpResponse.BodyHandlers.ofByteArray());

            if (response.statusCode() != 200) {
                log.warn("Google Static Maps API returned status {}", response.statusCode());
                return ResponseEntity.status(HttpStatus.BAD_GATEWAY).build();
            }

            String contentType = response.headers()
                    .firstValue("Content-Type")
                    .orElse("image/png");

            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .body(response.body());

        } catch (IOException e) {
            log.error("Failed to fetch static map from Google", e);
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY).build();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).build();
        }
    }
}
