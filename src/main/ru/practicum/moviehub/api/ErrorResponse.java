package ru.practicum.moviehub.api;

import java.util.List;

public class ErrorResponse {
    private final int status;
    private final String error;
    private final List<String> details;

    public ErrorResponse(int status, String error) {
        this(status, error, null);
    }

    public ErrorResponse(int status, String error, List<String> details) {
        this.status = status;
        this.error = error;
        this.details = details;
    }

    public int getStatus() {
        return status;
    }

    public String getError() {
        return error;
    }

    public List<String> getDetails() {
        return details;
    }
}