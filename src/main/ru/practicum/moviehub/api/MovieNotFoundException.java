package ru.practicum.moviehub.api;

public class MovieNotFoundException extends RuntimeException {
    public MovieNotFoundException(String message) { super(message); }
}