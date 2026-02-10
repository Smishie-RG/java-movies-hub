package ru.practicum.moviehub.http;

import com.google.gson.Gson;
import ru.practicum.moviehub.api.ErrorResponse;
import ru.practicum.moviehub.api.MovieAlreadyExistsException;
import ru.practicum.moviehub.api.MovieNotFoundException;
import ru.practicum.moviehub.model.Movie;
import ru.practicum.moviehub.store.MoviesStore;
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

        try {
            if (method.equalsIgnoreCase("GET")) {
                if (path.equals("/movies")) {
                    sendJson(ex, 200, gson.toJson(moviesStore.getAllMovies()));
                } else if (path.matches("/movies/\\d+")) {
                    int id = Integer.parseInt(path.split("/")[2]);
                    sendJson(ex, 200, gson.toJson(moviesStore.findMovie(id)));
                } else {
                    sendJson(ex, 404, gson.toJson(new ErrorResponse(404, "Неизвестный эндпоинт")));
                }
            } else if (method.equalsIgnoreCase("POST")) {
                if (!"application/json".equalsIgnoreCase(ex.getRequestHeaders().getFirst("Content-Type"))) {
                    sendJson(ex, 415, gson.toJson(new ErrorResponse(415, "Unsupported Media Type")));
                    return;
                }

                String requestBody = new String(ex.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
                Movie movie;
                try {
                    movie = gson.fromJson(requestBody, Movie.class);
                } catch (Exception e) {
                    sendJson(ex, 422, gson.toJson(new ErrorResponse(422, "Ошибка парсинга JSON")));
                    return;
                }

                String title = movie.getTitle();
                int year = movie.getYear();
                int currentYear = Year.now().getValue();
                List<String> details = new ArrayList<>();
                boolean valid = true;

                if (title == null || title.isBlank()) {
                    valid = false;
                    details.add("название не должно быть пустым");
                }

                if (title != null && title.length() > 100) {
                    valid = false;
                    details.add("название не должно превышать 100 символов");
                }

                if (year < 1888 || year > currentYear + 1) {
                    valid = false;
                    details.add("год должен быть между 1888 и " + (currentYear + 1));
                }

                if (!valid) {
                    sendJson(ex, 422, gson.toJson(new ErrorResponse(422, "Ошибка валидации", details)));
                    return;
                }

                moviesStore.addMovies(movie);
                sendJson(ex, 201, gson.toJson(movie));
            } else if (method.equalsIgnoreCase("DELETE") && path.matches("/movies/\\d+")) {
                int id = Integer.parseInt(path.split("/")[2]);
                moviesStore.deleteMovieById(id);
                sendNoContent(ex);
            } else {
                sendJson(ex, 405, gson.toJson(new ErrorResponse(405, "Метод не разрешён для этого пути")));
            }
        } catch (MovieAlreadyExistsException e) {
            sendJson(ex, 409, gson.toJson(new ErrorResponse(409, "Фильм уже есть в списке")));
        } catch (MovieNotFoundException e) {
            sendJson(ex, 404, gson.toJson(new ErrorResponse(404, "Такого фильма нет в списке")));
        } catch (Exception e) {
            sendJson(ex, 500, gson.toJson(new ErrorResponse(500, e.getMessage())));
        }
    }
}