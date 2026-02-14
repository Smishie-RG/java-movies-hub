package ru.practicum.moviehub.http;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.junit.jupiter.api.*;
import ru.practicum.moviehub.model.Movie;
import ru.practicum.moviehub.http.MoviesServer;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class MoviesApiTest {

    private static final String BASE = "http://localhost:8080";
    private MoviesServer server;
    private HttpClient client;
    private Gson gson;

    @BeforeAll
    void beforeAll() {
        server = new MoviesServer();
        server.start();
        client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(2)).build();
        gson = new Gson();
    }

    @AfterAll
    void afterAll() {
        if (server != null) server.stop();
    }

    @BeforeEach
    void clearStore() {
        server.getMoviesStore().clear();
    }

    @Test
    void getMovies_whenEmpty_returnsEmptyArray() throws Exception {
        HttpRequest req = HttpRequest.newBuilder().uri(URI.create(BASE + "/movies")).GET().build();
        HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        assertEquals(200, resp.statusCode());
        List<Movie> movies = gson.fromJson(resp.body(), new TypeToken<List<Movie>>(){}.getType());
        assertTrue(movies.isEmpty());
    }

    @Test
    void postMovie_valid_returns201() throws Exception {
        Movie movie = new Movie(1, "Фильм", 2025);
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(gson.toJson(movie)))
                .build();
        HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        assertEquals(201, resp.statusCode());
        Movie returned = gson.fromJson(resp.body(), Movie.class);
        assertEquals(movie.getTitle(), returned.getTitle());
    }

    @Test
    void postMovie_emptyTitle_returns422() throws Exception {
        Movie movie = new Movie(1, "", 2025);
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(gson.toJson(movie)))
                .build();
        HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        assertEquals(422, resp.statusCode());
        assertTrue(resp.body().contains("название не должно быть пустым"));
    }

    @Test
    void postMovie_titleTooLong_returns422() throws Exception {
        String longTitle = "A".repeat(101);
        Movie movie = new Movie(1, longTitle, 2025);
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(gson.toJson(movie)))
                .build();
        HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        assertEquals(422, resp.statusCode());
        assertTrue(resp.body().contains("название не должно превышать 100 символов"));
    }

    @Test
    void postMovie_yearInvalid_returns422() throws Exception {
        Movie movie = new Movie(1, "Фильм", 1800);
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(gson.toJson(movie)))
                .build();
        HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        assertEquals(422, resp.statusCode());
        assertTrue(resp.body().contains("год должен быть между 1888"));
    }

    @Test
    void postMovie_wrongContentType_returns415() throws Exception {
        Movie movie = new Movie(1, "Фильм", 2025);
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies"))
                .header("Content-Type", "text/plain")
                .POST(HttpRequest.BodyPublishers.ofString(gson.toJson(movie)))
                .build();
        HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        assertEquals(415, resp.statusCode());
    }

    @Test
    void getMovie_byId_success() throws Exception {
        Movie movie = new Movie(1, "Фильм", 2025);
        client.send(HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(gson.toJson(movie)))
                .build(), HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        HttpRequest getReq = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies/1"))
                .GET()
                .build();
        HttpResponse<String> resp = client.send(getReq, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        assertEquals(200, resp.statusCode());
        Movie returned = gson.fromJson(resp.body(), Movie.class);
        assertEquals(movie.getTitle(), returned.getTitle());
    }

    @Test
    void getMovie_byId_notFound() throws Exception {
        HttpRequest getReq = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies/99"))
                .GET()
                .build();
        HttpResponse<String> resp = client.send(getReq, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        assertEquals(404, resp.statusCode());
    }

    @Test
    void getMovie_byId_invalid_returns400() throws Exception {
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies/abc"))
                .GET()
                .build();
        HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        assertEquals(400, resp.statusCode());
    }

    @Test
    void deleteMovie_byId_success() throws Exception {
        Movie movie = new Movie(1, "Фильм", 2025);
        client.send(HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(gson.toJson(movie)))
                .build(), HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        HttpRequest deleteReq = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies/1"))
                .DELETE()
                .build();
        HttpResponse<String> resp = client.send(deleteReq, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        assertEquals(204, resp.statusCode());
    }

    @Test
    void deleteMovie_byId_notFound() throws Exception {
        HttpRequest deleteReq = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies/99"))
                .DELETE()
                .build();
        HttpResponse<String> resp = client.send(deleteReq, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        assertEquals(404, resp.statusCode());
    }

    @Test
    void deleteMovie_byId_invalid_returns400() throws Exception {
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies/abc"))
                .DELETE()
                .build();
        HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        assertEquals(400, resp.statusCode());
    }

    @Test
    void getMoviesByYear_valid_returnsMovies() throws Exception {
        Movie movie1 = new Movie(1, "Фильм1", 2025);
        Movie movie2 = new Movie(2, "Фильм2", 2024);
        client.send(HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(gson.toJson(movie1)))
                .build(), HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        client.send(HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(gson.toJson(movie2)))
                .build(), HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies?year=2025"))
                .GET()
                .build();
        HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        assertEquals(200, resp.statusCode());
        List<Movie> movies = gson.fromJson(resp.body(), new TypeToken<List<Movie>>(){}.getType());
        assertEquals(1, movies.size());
        assertEquals(2025, movies.get(0).getYear());
    }

    @Test
    void getMoviesByYear_invalid_returns400() throws Exception {
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies?year=abc"))
                .GET()
                .build();
        HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        assertEquals(400, resp.statusCode());
    }
}