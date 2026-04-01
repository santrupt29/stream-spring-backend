package com.stream.app.exceptions;

import com.stream.app.payload.CustomMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.util.NoSuchElementException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * 404 — Video/resource not found
     */
    @ExceptionHandler({NoSuchElementException.class})
    public ResponseEntity<CustomMessage> handleNotFound(NoSuchElementException ex) {
        log.warn("Resource not found: {}", ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(CustomMessage.builder()
                        .message("Resource not found: " + ex.getMessage())
                        .success(false)
                        .build());
    }

    /**
     * 401 — Bad credentials on login
     */
    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<CustomMessage> handleBadCredentials(BadCredentialsException ex) {
        return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .body(CustomMessage.builder()
                        .message("Invalid email or password")
                        .success(false)
                        .build());
    }

    /**
     * 404 — User not found during authentication
     */
    @ExceptionHandler(UsernameNotFoundException.class)
    public ResponseEntity<CustomMessage> handleUserNotFound(UsernameNotFoundException ex) {
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(CustomMessage.builder()
                        .message(ex.getMessage())
                        .success(false)
                        .build());
    }

    /**
     * 413 — File too large
     */
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<CustomMessage> handleMaxUploadSize(MaxUploadSizeExceededException ex) {
        log.warn("Upload too large: {}", ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.PAYLOAD_TOO_LARGE)
                .body(CustomMessage.builder()
                        .message("File too large. Maximum upload size is 500MB.")
                        .success(false)
                        .build());
    }

    /**
     * 400 — Illegal argument (validation errors)
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<CustomMessage> handleIllegalArgument(IllegalArgumentException ex) {
        log.warn("Bad request: {}", ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(CustomMessage.builder()
                        .message(ex.getMessage())
                        .success(false)
                        .build());
    }

    /**
     * 500 — Catch-all for unexpected errors.
     * Logs the full stack trace but returns a safe message to the client.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<CustomMessage> handleGenericException(Exception ex) {
        log.error("Unhandled exception", ex);
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(CustomMessage.builder()
                        .message("An internal error occurred. Please try again later.")
                        .success(false)
                        .build());
    }
}
