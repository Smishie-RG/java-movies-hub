package ru.practicum.moviehub.http;

import com.google.gson.Gson;
import ru.practicum.moviehub.model.Movie;
import ru.practicum.moviehub.store.MoviesStore;
import ru.practicum.moviehub.api.ErrorResponse;
import ru.practicum.moviehub.api.MovieAlreadyExistsException;
import ru.practicum.moviehub.api.MovieNotFoundException;

import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Year;
import java.util.ArrayList;
import java.util.List;

public class MoviesHandler extends BaseHttpHandler {

    private final MoviesStore moviesStore;
    private final Gson gson = new Gson();

    public MoviesHandler(MoviesStore store) {
        this.moviesStore = store;
    }

    @Override
    public void handle(HttpExchange ex) throws IOException {
        String method = ex.getRequestMethod();
        String path = ex.getRequestURI().getPath();
        String query = ex.getRequestURI().getQuery();

        try {
            if (method.equalsIgnoreCase("GET")) {
                handleGet(ex, path, query);
            } else if (method.equalsIgnoreCase("POST")) {
                handlePost(ex, path);
            } else if (method.equalsIgnoreCase("DELETE")) {
                handleDelete(ex, path);
            } else {
                sendJson(ex, 405, gson.toJson(new ErrorResponse(405, "Метод не разрешён для этого пути")));
            }
        } catch (MovieAlreadyExistsException e) {
            sendJson(ex, 409, gson.toJson(new ErrorResponse(409, "Фильм уже есть в списке")));
        } catch (MovieNotFoundException e) {
            sendJson(ex, 404, gson.toJson(new ErrorResponse(404, "Такого фильма нет в списке")));
        } catch (NumberFormatException e) {
            sendJson(ex, 400, gson.toJson(new ErrorResponse(400, "Некорректный ID")));
        } catch (Exception e) {
            sendJson(ex, 500, gson.toJson(new ErrorResponse(500, e.getMessage())));
        }
    }

    private void handleGet(HttpExchange ex, String path, String query) throws IOException {
        if ("/movies".equals(path)) {
            if (query != null && query.startsWith("year=")) {
                String yearParam = query.substring(5);
                try {
                    int year = Integer.parseInt(yearParam);
                    sendJson(ex, 200, gson.toJson(moviesStore.getMoviesByYear(year)));
                } catch (NumberFormatException e) {
                    sendJson(ex, 400, gson.toJson(new ErrorResponse(400, "Некорректный параметр запроса — 'year'")));
                }
            } else if (query == null) {
                sendJson(ex, 200, gson.toJson(moviesStore.getAllMovies()));
            } else {
                sendJson(ex, 400, gson.toJson(new ErrorResponse(400, "Некорректный параметр запроса")));
            }
        } else if (path.matches("/movies/\\d+")) {
            int id = Integer.parseInt(path.split("/")[2]);
            sendJson(ex, 200, gson.toJson(moviesStore.findMovie(id)));
        } else if (path.matches("/movies/.*")) {
            sendJson(ex, 400, gson.toJson(new ErrorResponse(400, "Некорректный ID")));
        } else {
            sendJson(ex, 404, gson.toJson(new ErrorResponse(404, "Неизвестный эндпоинт")));
        }
    }

    private void handlePost(HttpExchange ex, String path) throws IOException {
        if (!"/movies".equals(path)) {
            sendJson(ex, 405, gson.toJson(new ErrorResponse(405, "Метод не разрешён для этого пути")));
            return;
        }

        if (!"application/json".equalsIgnoreCase(ex.getRequestHeaders().getFirst("Content-Type"))) {
            sendJson(ex, 415, gson.toJson(new ErrorResponse(415, "Unsupported Media Type")));
            return;
        }

        String requestBody = new String(ex.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
        Movie movie;
        try {
            movie = gson.fromJson(requestBody, Movie.class);
        } catch (Exception e) {
            sendJson(ex, 400, gson.toJson(new ErrorResponse(400, "Ошибка парсинга JSON")));
            return;
        }

        List<String> details = new ArrayList<>();
        boolean valid = true;
        int currentYear = Year.now().getValue();

        if (movie.getTitle() == null || movie.getTitle().isBlank()) { valid = false; details.add("название не должно быть пустым"); }
        if (movie.getTitle() != null && movie.getTitle().length() > 100) { valid = false; details.add("название не должно превышать 100 символов"); }
        if (movie.getYear() < 1888 || movie.getYear() > currentYear + 1) { valid = false; details.add("год должен быть между 1888 и " + (currentYear + 1)); }

        if (!valid) {
            sendJson(ex, 422, gson.toJson(new ErrorResponse(422, "Ошибка валидации", details)));
            return;
        }

        moviesStore.addMovies(movie);
        sendJson(ex, 201, gson.toJson(movie));
    }

    private void handleDelete(HttpExchange ex, String path) throws IOException {
        if (path.matches("/movies/\\d+")) {
            int id = Integer.parseInt(path.split("/")[2]);
            moviesStore.deleteMovieById(id);
            sendNoContent(ex);
        } else if (path.matches("/movies/.*")) {
            sendJson(ex, 400, gson.toJson(new ErrorResponse(400, "Некорректный ID")));
        } else {
            sendJson(ex, 405, gson.toJson(new ErrorResponse(405, "Метод не разрешён для этого пути")));
        }
    }
}