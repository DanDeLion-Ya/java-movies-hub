package ru.practicum.moviehub.store;

import ru.practicum.moviehub.model.Movie;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MoviesStore {
    private final Map<Integer, Movie> movies = new HashMap<>();
    private int nextId = 0;

    public Movie addMovie(String title, int year) {
        for (Movie movie : movies.values()) {
            if (movie.getTitle().equals(title) && movie.getYear() == year) {
                return null;
            }
        }
        nextId += 1;
        Movie newMovie = new Movie(nextId, title, year);
        movies.put(nextId, newMovie);
        return newMovie;
    }

    public List<Movie> getAllMovies() {
        List<Movie> listMovies = new ArrayList<>();
        for (Movie movie : movies.values()) {
            listMovies.add(movie);
        }
        return listMovies;
    }

    public void clearListMovies() {
        movies.clear();
        nextId = 0;
    }

    public Movie getMovieById(int id) {
        return movies.get(id);
    }

    public boolean deleteMovie(int id) {
        if (movies.containsKey(id)) {
            movies.remove(id);
            return true;
        }
        return false;
    }

    public List<Movie> getMoviesByYear(int year) {
        List<Movie> result = new ArrayList<>();
        for (Movie movie : movies.values()) {
            if (movie.getYear() == year) {
                result.add(movie);
            }
        }
        return result;
    }
}