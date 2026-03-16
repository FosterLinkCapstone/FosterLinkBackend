package net.fosterlink.fosterlinkbackend.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Global exception handler for the application.
 * Handles validation errors and returns consistent error responses.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    // #region agent log
    private static final Logger dbgLog = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    // #endregion

    /**
     * Handles validation errors from @Valid annotations on request bodies.
     * Returns a 400 Bad Request with detailed field-level error messages.
     *
     * @param ex the MethodArgumentNotValidException thrown when validation fails
     * @return ResponseEntity containing error details
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidationExceptions(MethodArgumentNotValidException ex) {
        Map<String, Object> response = new HashMap<>();
        response.put("status", HttpStatus.BAD_REQUEST.value());
        response.put("error", "Validation Failed");

        List<Map<String, String>> errors = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(this::mapFieldError)
                .collect(Collectors.toList());

        response.put("errors", errors);

        return ResponseEntity.badRequest().body(response);
    }

    /**
     * Maps a FieldError to a simple map containing field name and error message.
     *
     * @param fieldError the field error to map
     * @return a map with "field" and "message" keys
     */
    private Map<String, String> mapFieldError(FieldError fieldError) {
        Map<String, String> error = new HashMap<>();
        error.put("field", fieldError.getField());
        error.put("message", fieldError.getDefaultMessage());
        return error;
    }

    // #region agent log
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleAllUnhandledExceptions(Exception ex) {
        dbgLog.warn("[DBG fc26e1] unhandled_exception type={} message='{}' cause='{}'",
            ex.getClass().getName(), ex.getMessage(),
            ex.getCause() != null ? ex.getCause().getMessage() : "null", ex);
        Map<String, Object> response = new HashMap<>();
        response.put("status", HttpStatus.INTERNAL_SERVER_ERROR.value());
        response.put("error", "Internal Server Error");
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }
    // #endregion
}
