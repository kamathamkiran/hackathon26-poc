package com.db.hackathon.exception;

import io.grpc.StatusRuntimeException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<String> handleIllegalArgumentException(
            IllegalArgumentException ex) {

        log.error("Invalid argument: {}", ex.getMessage());

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body("Invalid request: " + ex.getMessage());
    }

    @ExceptionHandler(StatusRuntimeException.class)
    public ResponseEntity<String> handleStatusRuntimeException(
            StatusRuntimeException ex) {

        log.error("gRPC status error: {}", ex.getStatus(), ex);

        if (ex.getStatus().getCode().name().equals("INVALID_ARGUMENT")) {
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body("Invalid request: " + ex.getStatus().getDescription());
        }

        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Processing failed: " + ex.getStatus().getDescription());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<String> handleException(Exception ex) {

        log.error("Unhandled exception", ex);

        if (ex.getCause() instanceof StatusRuntimeException) {
            return handleStatusRuntimeException(
                    (StatusRuntimeException) ex.getCause());
        }

        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("An error occurred: " + ex.getMessage());

    }

}
