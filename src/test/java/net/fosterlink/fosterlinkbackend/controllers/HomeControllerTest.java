package net.fosterlink.fosterlinkbackend.controllers;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.*;

class HomeControllerTest {

    private HomeController homeController;

    @BeforeEach
    void setUp() {
        homeController = new HomeController();
    }

    @Test
    void testHelloWorld_ReturnsOkResponse() {
        // Act
        ResponseEntity<String> response = homeController.helloWorld();

        // Assert
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("Hello World <- changes reflected", response.getBody());
    }

    @Test
    void testHelloWorld_ResponseBodyIsCorrect() {
        // Act
        ResponseEntity<String> response = homeController.helloWorld();

        // Assert
        String body = response.getBody();
        assertTrue(body.contains("Hello World"));
    }
}

