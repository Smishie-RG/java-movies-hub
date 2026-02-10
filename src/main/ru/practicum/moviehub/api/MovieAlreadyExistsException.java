package ru.practicum.moviehub.api;

public class MovieAlreadyExistsException extends RuntimeException {
    public MovieAlreadyExistsException(String message) { super(message); }
}