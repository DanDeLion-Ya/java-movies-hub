package ru.practicum.moviehub.http;

import com.google.gson.Gson;

import com.google.gson.JsonSyntaxException;
import com.sun.net.httpserver.HttpExchange;
import ru.practicum.moviehub.model.Movie;
import ru.practicum.moviehub.store.MoviesStore;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class MoviesHandler extends BaseHttpHandler {
    private final MoviesStore storage;

    public MoviesHandler(MoviesStore storage) {
        this.storage = storage;
    }

    @Override
    public void handle(HttpExchange ex) throws IOException {
        int currentYear = LocalDate.now().getYear();
        Gson gson = new Gson();
        String method = ex.getRequestMethod();
        String path = ex.getRequestURI().getPath();
        String[] parts = path.split("/");

        if (method.equalsIgnoreCase("GET")) {
            String query = ex.getRequestURI().getQuery();

            if (query != null && query.startsWith("year=")) {
                try {
                    int year = Integer.parseInt(query.substring(5));
                    List<Movie> movies = storage.getMoviesByYear(year);
                    String json = gson.toJson(movies);
                    sendJson(ex, 200, json);
                } catch (NumberFormatException e) {
                    sendError(ex, 400, "Некорректный параметр запроса - 'year'", List.of());
                }
                return;
            }

            if (path.equals("/movies")) {
                List<Movie> movies = storage.getAllMovies();
                String json = gson.toJson(movies);
                sendJson(ex, 200, json);
                return;
            }
            if (parts.length == 3 && parts[1].equals("movies")) {
                try {
                    int id = Integer.parseInt(parts[2]);
                    Movie movie = storage.getMovieById(id);
                    if (movie == null) {
                        sendError(ex, 404, "Фильм не найден", List.of());
                        return;
                    }
                    String json = gson.toJson(movie);
                    sendJson(ex, 200, json);
                } catch (NumberFormatException e) {
                    sendError(ex, 400, "Некорректный ID", List.of());
                }
                return;
            }
            sendError(ex, 404, "Фильм не найден", List.of());
            return;

        } else if (method.equalsIgnoreCase("POST")) {
            String contentType = ex.getRequestHeaders().getFirst("Content-Type");

            if (contentType == null || !contentType.startsWith("application/json")) {
                ex.sendResponseHeaders(415, 0);
                ex.getResponseBody().close();
                return;
            }
            InputStream inputStream = ex.getRequestBody();
            String body = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
            Movie inputMovie;
            try {
                inputMovie = gson.fromJson(body, Movie.class);
            } catch (JsonSyntaxException e) {
                sendError(ex, 422, "Ошибка валидации", List.of());
                return;
            }
            String title = inputMovie.getTitle();
            int year = inputMovie.getYear();
            List<String> details = new ArrayList<>();

            if (title == null || title.trim().isEmpty()) {
                details.add("Название не должно быть пустым");
            }

            if (title.length() > 100) {
                details.add("Название не должно превышать 100 символов");
            }

            if (year < 1888) {
                details.add("Год должен быть не меньше 1888");
            }

            if (year > currentYear + 1) {
                details.add("Год не должен быть больше " + (currentYear + 1));
            }

            if (!details.isEmpty()) {
                sendError(ex, 422, "Ошибка валидации", details);
                return;
            }
            Movie storeMovie = storage.addMovie(title, year);

            if (storeMovie == null) {
                details.add("Фильм с таким названием и годом уже есть");
                sendError(ex, 422, "Ошибка валидации", details);
                return;
            }
            String responseJson = gson.toJson(storeMovie);
            sendJson(ex, 201, responseJson);
        }
        else if (method.equalsIgnoreCase("DELETE")) {

            if (path.equals("/movies")) {
                ex.sendResponseHeaders(405, 0);
                ex.getResponseBody().close();
                return;
            } else if (parts.length == 3 && parts[1].equals("movies")) {
                try {
                    int id = Integer.parseInt(parts[2]);
                    boolean deleted = storage.deleteMovie(id);
                    if (deleted) {
                        sendNoContent(ex);
                        return;
                    } else {
                        ex.sendResponseHeaders(404, 0);
                        ex.getResponseBody().close();
                        return;
                    }
                } catch (NumberFormatException e) {
                    sendError(ex, 400, "Некорректный ID", List.of());
                    return;
                }
            } else {
                ex.sendResponseHeaders(405, 0);
                ex.getResponseBody().close();
                return;
            }
        } else {
            ex.sendResponseHeaders(405, 0);
            ex.getResponseBody().close();
        }
    }
}