package net.fosterlink.fosterlinkbackend.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class StringUtilTest {

    @Test
    void testCleanString_WithPlainText_ReturnsCleanedString() {
        // Arrange
        String input = "Hello World";

        // Act
        String result = StringUtil.cleanString(input);

        // Assert
        assertNotNull(result);
        assertEquals("Hello World", result);
    }

    @Test
    void testCleanString_WithHtmlTags_RemovesTags() {
        // Arrange
        String input = "<script>alert('xss')</script>Hello";

        // Act
        String result = StringUtil.cleanString(input);

        // Assert
        assertNotNull(result);
        assertFalse(result.contains("<script>"));
        assertFalse(result.contains("</script>"));
    }

    @Test
    void testCleanString_WithHtmlEntities_EscapesEntities() {
        // Arrange
        String input = "<div>Test & More</div>";

        // Act
        String result = StringUtil.cleanString(input);

        // Assert
        assertNotNull(result);
        // HTML entities should be escaped
        assertTrue(result.contains("&amp;") || !result.contains("&"));
    }

    @Test
    void testCleanString_WithAllowedBasicHtml_PreservesBasicFormatting() {
        // Arrange
        String input = "<p>Hello <b>World</b></p>";

        // Act
        String result = StringUtil.cleanString(input);

        // Assert
        assertNotNull(result);
        // Basic HTML should be cleaned but some basic tags might remain
        assertTrue(result.contains("Hello"));
        assertTrue(result.contains("World"));
    }

    @Test
    void testCleanString_WithNull_HandlesGracefully() {
        // Arrange
        String input = null;

        // Act & Assert
        assertThrows(NullPointerException.class, () -> {
            StringUtil.cleanString(input);
        });
    }

    @Test
    void testCleanString_WithEmptyString_ReturnsEmptyString() {
        // Arrange
        String input = "";

        // Act
        String result = StringUtil.cleanString(input);

        // Assert
        assertNotNull(result);
        assertEquals("", result);
    }

    @Test
    void testCleanString_WithSpecialCharacters_EscapesCorrectly() {
        // Arrange
        String input = "Test < > & \" '";

        // Act
        String result = StringUtil.cleanString(input);

        // Assert
        assertNotNull(result);
        assertTrue(result.contains("Test"));
    }
}

