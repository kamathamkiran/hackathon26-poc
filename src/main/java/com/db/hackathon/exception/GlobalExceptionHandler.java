package com.db.hackathon.exception;

import com.google.api.gax.rpc.InvalidArgumentException;
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

    @ExceptionHandler(InvalidArgumentException.class)
    public ResponseEntity<String> handleInvalidArgumentException(
            InvalidArgumentException ex) {

        log.error("Google Document AI invalid argument error", ex);

        String errorMessage = buildDocumentAiErrorMessage(ex);

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(errorMessage);
    }

    @ExceptionHandler(StatusRuntimeException.class)
    public ResponseEntity<String> handleStatusRuntimeException(
            StatusRuntimeException ex) {

        log.error("gRPC status error: {}", ex.getStatus(), ex);

        if (ex.getStatus().getCode().name().equals("INVALID_ARGUMENT")) {
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body("Invalid request to Google Document AI. " +
                            "Please verify: " +
                            "1) File is a valid PDF " +
                            "2) File is not empty " +
                            "3) File size is under 500MB. " +
                            "Error details: " + ex.getStatus().getDescription());
        }

        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Processing failed: " + ex.getStatus().getDescription());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<String> handleException(Exception ex) {

        log.error("Unhandled exception", ex);

        // Check if it's a wrapped document AI error
        if (ex.getCause() instanceof InvalidArgumentException) {
            return handleInvalidArgumentException(
                    (InvalidArgumentException) ex.getCause());
        }

        if (ex.getCause() instanceof StatusRuntimeException) {
            return handleStatusRuntimeException(
                    (StatusRuntimeException) ex.getCause());
        }

        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("An error occurred: " + ex.getMessage());

    }

    private String buildDocumentAiErrorMessage(InvalidArgumentException ex) {
        String baseMessage = "Document AI processing failed with invalid argument error. ";

        if (ex.getMessage() != null && ex.getMessage().contains("INVALID_ARGUMENT")) {
            return baseMessage +
                    "This typically means: " +
                    "1) The PDF file is corrupted or not a valid PDF " +
                    "2) The file is empty " +
                    "3) The processor ID is incorrect " +
                    "4) The processor is not deployed in the specified location " +
                    "5) Your credentials lack permission to access the processor. " +
                    "Please check your configuration and file.";
        }

        return baseMessage + ex.getMessage();
    }

}
