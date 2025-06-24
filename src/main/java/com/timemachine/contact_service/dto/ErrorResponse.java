package com.timemachine.contact_service.dto;


import org.springframework.http.ResponseEntity;

import java.time.Instant;
import java.util.List;
import java.util.Map;

public record ErrorResponse(
        Instant timestamp,
        int status,
        String error,
        String message,
        String path,
        Map<String, List<String>> fieldErrors, // For validation errors
        String requestId // For distributed tracing
) {
    public ErrorResponse {
        if (timestamp == null) {
            timestamp = Instant.now();
        }
    }

    // Builder-style factory method
    public static ErrorResponseBuilder builder() {
        return new ErrorResponseBuilder();
    }

    // Builder implementation
    public static class ErrorResponseBuilder {
        private Instant timestamp;
        private int status;
        private String error;
        private String message;
        private String path;
        private Map<String, List<String>> fieldErrors;
        private String requestId;

        public ErrorResponseBuilder timestamp(Instant timestamp) {
            this.timestamp = timestamp;
            return this;
        }

        public ErrorResponseBuilder status(int status) {
            this.status = status;
            return this;
        }

        public ErrorResponseBuilder error(String error) {
            this.error = error;
            return this;
        }

        public ErrorResponseBuilder message(String message) {
            this.message = message;
            return this;
        }

        public ErrorResponseBuilder path(String path) {
            this.path = path;
            return this;
        }

        public ErrorResponseBuilder fieldErrors(Map<String, List<String>> fieldErrors) {
            this.fieldErrors = fieldErrors;
            return this;
        }

        public ErrorResponseBuilder requestId(String requestId) {
            this.requestId = requestId;
            return this;
        }

        public ErrorResponse build() {
            return new ErrorResponse(
                    timestamp != null ? timestamp : Instant.now(),
                    status,
                    error,
                    message,
                    path,
                    fieldErrors,
                    requestId
            );
        }
    }
    // Inside the ErrorResponse record
    public ResponseEntity<ErrorResponse> toResponseEntity() {
        return ResponseEntity.status(this.status()).body(this);
    }
}
