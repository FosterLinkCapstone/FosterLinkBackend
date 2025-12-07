package net.fosterlink.fosterlinkbackend.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
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
            description = "A simple endpoint to verify that the API is running and accessible",
            tags = {"Home"},
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "The API is running and accessible"
                    )
            }
    )
    @GetMapping
    public ResponseEntity<String> helloWorld() {
        return ResponseEntity.ok("Hello World <- changes reflected");
    }

}
