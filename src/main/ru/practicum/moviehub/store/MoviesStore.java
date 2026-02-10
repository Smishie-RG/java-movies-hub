package ru.practicum.moviehub.store;

import ru.practicum.moviehub.api.MovieAlreadyExistsException;
import ru.practicum.moviehub.api.MovieNotFoundException;
import ru.practicum.moviehub.model.Movie;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MoviesStore {
    private final Map<Integer, Movie> movies = new HashMap<>();

    public List<Movie> getAllMovies() {
        return new ArrayList<>(movies.values());
    }

    public void addMovies(Movie movie) {
        if (movies.containsKey(movie.getId())) {
            throw new MovieAlreadyExistsException("Фильм уже есть в списке");
        }
        movies.put(movie.getId(), movie);
    }

    public Movie findMovie(int id) {
        Movie movie = movies.get(id);
        if (movie == null) {
            throw new MovieNotFoundException("Такого фильма нет в списке");
        }
        return movie;
    }

    public void deleteMovieById(int id) {
        if (!movies.containsKey(id)) {
            throw new MovieNotFoundException("Такого фильма нет в списке");
        }
        movies.remove(id);
    }

    public void clear() {
        movies.clear();
    }
}