package net.fosterlink.fosterlinkbackend.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import net.fosterlink.fosterlinkbackend.config.ratelimit.RateLimit;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1")
public class HomeController {

    @Operation(
            summary = "Health check endpoint",
            description = "A simple endpoint to verify that the API is running and accessible. Rate limit: 65 requests per 60 seconds per IP.",
            tags = {"Home"},
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "The API is running and accessible"
                    ),
                    @ApiResponse(
                            responseCode = "429",
                            description = "Rate limit exceeded. Maximum 65 requests per 60 seconds per IP."
                    )
            }
    )
    @GetMapping
    @RateLimit(requests = 65)
    public ResponseEntity<String> helloWorld() {
        return ResponseEntity.ok("Hello World");
    }

}
