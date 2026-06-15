package ru.practicum.moviehub.http;

import com.google.gson.Gson;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ru.practicum.moviehub.api.ErrorResponse;
import ru.practicum.moviehub.model.Movie;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class MoviesApiTest {
    private static final String BASE = "http://localhost:8080";
    private static MoviesServer server;
    private static HttpClient client;
    private static Gson gson;

    @BeforeAll
    static void beforeAll() {
        server = new MoviesServer();
        server.start();
        client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(2))
                .build();
        gson = new Gson();
    }

    @BeforeEach
    void beforeEach() {
        server.getStorage().clearListMovies();
    }

    @AfterAll
    static void afterAll() {
        if (server != null) {
            server.stop();
        }
    }

    @Test
    void getMovies_whenEmpty_returnsEmptyArray() throws Exception {
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies"))
                .GET()
                .build();

        HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        assertEquals(200, resp.statusCode(), "GET /movies должен вернуть 200");

        String contentTypeHeaderValue = resp.headers().firstValue("Content-Type").orElse("");
        assertEquals("application/json; charset=UTF-8", contentTypeHeaderValue,
                "Content-Type должен содержать формат данных и кодировку");

        String body = resp.body().trim();
        assertTrue(body.startsWith("[") && body.endsWith("]"),
                "Ожидается JSON-массив");
    }

    @Test
    void getMovies_thereMovies_returnsMovieArray() throws Exception {
        server.getStorage().addMovie("Начало", 2010);
        server.getStorage().addMovie("Реальные упыри", 2014);
        server.getStorage().addMovie("Конец", 2024);

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies"))
                .GET()
                .build();

        HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, resp.statusCode(), "GET /movies должен вернуть 200");

        String contentTypeHeaderValue = resp.headers().firstValue("Content-Type").orElse("");
        assertEquals("application/json; charset=UTF-8", contentTypeHeaderValue,
                "Content-Type должен содержать формат данных и кодировку");

        String body = resp.body().trim();
        List<Movie> movieList = gson.fromJson(body, new ListOfMoviesTypeToken().getType());
        assertEquals(3, movieList.size());

        assertEquals(1, movieList.get(0).getId());
        assertEquals("Начало", movieList.get(0).getTitle());
        assertEquals(2010, movieList.get(0).getYear());

        assertEquals(2, movieList.get(1).getId());
        assertEquals("Реальные упыри", movieList.get(1).getTitle());
        assertEquals(2014, movieList.get(1).getYear());

        assertEquals(3, movieList.get(2).getId());
        assertEquals("Конец", movieList.get(2).getTitle());
        assertEquals(2024, movieList.get(2).getYear());
    }

    @Test
    void test_successfulAdd() throws Exception {
        Movie movie = new Movie(0, "Престиж", 2006);
        String json = gson.toJson(movie);

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();

        HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        assertEquals(201, resp.statusCode(), "POST/movies должен вернуть 201");

        String contentType = resp.headers().firstValue("Content-Type").orElse("");
        assertEquals("application/json; charset=UTF-8", contentType);

        Movie deserial = gson.fromJson(resp.body(), Movie.class);
        assertEquals("Престиж", deserial.getTitle());
        assertEquals(2006, deserial.getYear());
        assertTrue(deserial.getId() > 0);
    }

    @Test
    void test_addMovie_emptyTitle() throws Exception {
        Movie movie = new Movie(0, "", 2006);
        String json = gson.toJson(movie);

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();

        HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        assertEquals(422, resp.statusCode(), "POST/movies должен вернуть 422");

        String contentType = resp.headers().firstValue("Content-Type").orElse("");
        assertEquals("application/json; charset=UTF-8", contentType);

        ErrorResponse deserial = gson.fromJson(resp.body(), ErrorResponse.class);
        assertEquals("Ошибка валидации", deserial.getError());
        assertEquals("Название не должно быть пустым", deserial.getDetails().get(0));
    }

    @Test
    void test_addMovie_longTitle() throws Exception {
        String longTitle = "БА".repeat(50) + "Х";
        Movie movie = new Movie(0, longTitle, 2006);
        String json = gson.toJson(movie);

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();

        HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        assertEquals(422, resp.statusCode(), "POST/movies должен вернуть 422");

        String contentType = resp.headers().firstValue("Content-Type").orElse("");
        assertEquals("application/json; charset=UTF-8", contentType);

        ErrorResponse deserial = gson.fromJson(resp.body(), ErrorResponse.class);
        assertEquals("Ошибка валидации", deserial.getError());
        assertEquals(1, deserial.getDetails().size());
        assertEquals("Название не должно превышать 100 символов", deserial.getDetails().get(0));
    }

    @Test
    void test_addMovie_yearLess() throws Exception {
        Movie movie = new Movie(0, "Колизей", 80);
        String json = gson.toJson(movie);

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();

        HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        assertEquals(422, resp.statusCode(), "POST/movies должен вернуть 422");

        String contentType = resp.headers().firstValue("Content-Type").orElse("");
        assertEquals("application/json; charset=UTF-8", contentType);

        ErrorResponse deserial = gson.fromJson(resp.body(), ErrorResponse.class);
        assertEquals("Ошибка валидации", deserial.getError());
        assertEquals(1, deserial.getDetails().size());
        assertEquals("Год должен быть не меньше 1888", deserial.getDetails().get(0));
    }

    @Test
    void test_addMovie_yearMore() throws Exception {
        int currentYear = LocalDate.now().getYear();
        Movie movie = new Movie(0, "КиберПанк", 2077);
        String json = gson.toJson(movie);

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();

        HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        assertEquals(422, resp.statusCode(), "POST/movies должен вернуть 422");

        String contentType = resp.headers().firstValue("Content-Type").orElse("");
        assertEquals("application/json; charset=UTF-8", contentType);

        ErrorResponse deserial = gson.fromJson(resp.body(), ErrorResponse.class);
        assertEquals("Ошибка валидации", deserial.getError());
        assertEquals(1, deserial.getDetails().size());
        assertEquals("Год не должен быть больше " + (currentYear + 1), deserial.getDetails().get(0));
    }

    @Test
    void test_addMovie_requestIncorrectValue() throws Exception {
        Movie movie = new Movie(0, "Переломный момент", 1943);
        String json = gson.toJson(movie);

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies"))
                .header("Content-Type", "apchxi/txt")
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();

        HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        assertEquals(415, resp.statusCode(), "POST/movies должен вернуть 415");
    }

    @Test
    void test_addMovie_invalidJson() throws Exception {
        String invalidJson = "{\"title\": \"Тестовый фильм\", \"year\": \"а чёй это\"}";

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(invalidJson))
                .build();

        HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        assertEquals(422, resp.statusCode(), "Некорректный JSON должен возвращать 422");

        ErrorResponse deserial = gson.fromJson(resp.body(), ErrorResponse.class);
        assertEquals("Ошибка валидации", deserial.getError());
    }

    @Test
    void test_getMovieById_returnMovie() throws Exception {
        server.getStorage().addMovie("Человек-Паук: Тестовый дом", 2026);

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies/1"))
                .GET()
                .build();

        HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        assertEquals(200, resp.statusCode(), "GET/movies/1 должен вернуть 200");

        String contentType = resp.headers().firstValue("Content-Type").orElse("");
        assertEquals("application/json; charset=UTF-8", contentType);

        Movie deserial = gson.fromJson(resp.body(), Movie.class);
        assertEquals(1, deserial.getId());
        assertEquals("Человек-Паук: Тестовый дом", deserial.getTitle());
        assertEquals(2026, deserial.getYear());
    }

    @Test
    void test_getMovieById_notFound() throws Exception {
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies/100500"))
                .GET()
                .build();

        HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        assertEquals(404, resp.statusCode(), "GET/movies/100500 должен вернуть 404");

        String contentType = resp.headers().firstValue("Content-Type").orElse("");
        assertEquals("application/json; charset=UTF-8", contentType);

        ErrorResponse deserial = gson.fromJson(resp.body(), ErrorResponse.class);
        assertEquals("Фильм не найден", deserial.getError());
    }

    @Test
    void test_getMovieById_invalidId() throws Exception {
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies/qwerty"))
                .GET()
                .build();

        HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        assertEquals(400, resp.statusCode(), "GET/movies/qwerty должен вернуть 400");

        String contentType = resp.headers().firstValue("Content-Type").orElse("");
        assertEquals("application/json; charset=UTF-8", contentType);

        ErrorResponse deserial = gson.fromJson(resp.body(), ErrorResponse.class);
        assertEquals("Некорректный ID", deserial.getError());
    }

    @Test
    void test_deleteMovie_delete() throws Exception {
        server.getStorage().addMovie("Человек-Паук: Выгнать из тестового дома", 2026);

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies/1"))
                .DELETE()
                .build();

        HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        assertEquals(204, resp.statusCode(), "DELETE/movies/1 должен вернуть 204");

        HttpRequest getReq = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies/1"))
                .GET()
                .build();

        HttpResponse<String> getResp = client.send(getReq, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        assertEquals(404, getResp.statusCode(), "GET/movies/0 должен вернуть 404");
    }

    @Test
    void test_deleteMovie_notFound() throws Exception {
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies/100500"))
                .DELETE()
                .build();

        HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        assertEquals(404, resp.statusCode(), "DELETE/movies/100500 должен вернуть 404");
    }

    @Test
    void test_deleteMovie_invalidId() throws Exception {
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies/qwerty"))
                .DELETE()
                .build();

        HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        assertEquals(400, resp.statusCode(), "DELETE/movies/qwerty должен вернуть 400");
    }

    @Test
    void test_getMovieByYear_withMovies() throws Exception {
        server.getStorage().addMovie("Человек-Паук: Тестовый дом", 2026);
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies?year=2026"))
                .GET()
                .build();

        HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        assertEquals(200, resp.statusCode(), "GET/movies?year=2026 должен вернуть 200");

        List<Movie> movies = gson.fromJson(resp.body(), new ListOfMoviesTypeToken().getType());
        assertEquals(1, movies.size());
        assertEquals(2026, movies.get(0).getYear());
    }

    @Test
    void test_getMovieByYear_returnEmptyList() throws Exception {
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies?year=1987"))
                .GET()
                .build();

        HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        assertEquals(200, resp.statusCode(), "GET/movies?year=1987 должен вернуть 200");

        List<Movie> movies = gson.fromJson(resp.body(), new ListOfMoviesTypeToken().getType());
        assertTrue(movies.isEmpty());
    }

    @Test
    void test_getMovieByYear_invalidYear() throws Exception {
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies?year=qwerty"))
                .GET()
                .build();

        HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        assertEquals(400, resp.statusCode(), "GET/movies?year=qwerty должен вернуть 400");

        ErrorResponse deserial = gson.fromJson(resp.body(), ErrorResponse.class);
        assertEquals("Некорректный параметр запроса - 'year'", deserial.getError());
    }
}