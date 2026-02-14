package ru.practicum.moviehub.store;

import ru.practicum.moviehub.model.Movie;
import ru.practicum.moviehub.api.MovieAlreadyExistsException;
import ru.practicum.moviehub.api.MovieNotFoundException;

import java.util.*;

public class MoviesStore {
    private final Map<Integer, Movie> movies = new HashMap<>();

    public List<Movie> getAllMovies() {
        return new ArrayList<>(movies.values());
    }

    public List<Movie> getMoviesByYear(int year) {
        List<Movie> filtered = new ArrayList<>();
        for (Movie movie : movies.values()) {
            if (movie.getYear() == year) {
                filtered.add(movie);
            }
        }
        return filtered;
    }

    public void addMovies(Movie movie) {
        if (movies.containsKey(movie.getId())) {
            throw new MovieAlreadyExistsException("Фильм уже есть в списке");
        }
        movies.put(movie.getId(), movie);
    }

    public Movie findMovie(int id) {
        Movie movie = movies.get(id);
        if (movie == null) throw new MovieNotFoundException("Такого фильма нет в списке");
        return movie;
    }

    public void deleteMovieById(int id) {
        if (!movies.containsKey(id)) throw new MovieNotFoundException("Такого фильма нет в списке");
        movies.remove(id);
    }

    public void clear() {
        movies.clear();
    }
}