package com.movie.search.service;

import com.movie.search.model.SearchHistory;
import com.movie.search.model.User;
import com.movie.search.repository.SearchHistoryRepository;
import com.movie.search.repository.UserRepository;
import io.github.cdimascio.dotenv.Dotenv;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.logging.Logger;

@Service
public class MovieService {

    private static final Logger LOGGER = Logger.getLogger(MovieService.class.getName());

    private final String apiKey;
    private final String apiUrl;
    private final TmdbClient tmdbClient;
    private final UserRepository userRepository;
    private final SearchHistoryRepository searchHistoryRepository;

    @Autowired
    public MovieService(TmdbClient tmdbClient, UserRepository userRepository, SearchHistoryRepository searchHistoryRepository) {
        Dotenv dotenv = Dotenv.load();
        this.apiKey = dotenv.get("TMDB_API_KEY");
        this.apiUrl = dotenv.get("TMDB_API_URL");
        this.tmdbClient = tmdbClient;
        this.userRepository = userRepository;
        this.searchHistoryRepository = searchHistoryRepository;

        LOGGER.info("TMDB_API_KEY: " + (this.apiKey != null ? "Set" : "Null"));
        LOGGER.info("TMDB_API_URL: " + (this.apiUrl != null ? this.apiUrl : "Null"));
        if (this.apiKey == null || this.apiUrl == null) {
            throw new IllegalStateException("TMDB_API_KEY or TMDB_API_URL is not set in .env");
        }
    }

    public List<Map<String, Object>> searchMovies(String title, String genre, String director, String year, String auth0Id) {
        LOGGER.info("Searching movies with: title=" + title + ", genre=" + genre + ", director=" + director + ", year=" + year + ", auth0Id=" + auth0Id);

        if (auth0Id != null && !auth0Id.isEmpty()) {
            User user = userRepository.findByAuth0Id(auth0Id)
                    .orElseGet(() -> {
                        User newUser = new User();
                        newUser.setAuth0Id(auth0Id);
                        newUser.setUsername("tempUsername");
                        newUser.setEmail("temp@example.com");
                        return userRepository.save(newUser);
                    });

            String searchQuery = buildSearchQuery(title, genre, director, year);
            SearchHistory history = new SearchHistory();
            history.setUser(user);
            history.setSearchQuery(searchQuery);
            searchHistoryRepository.save(history);
            LOGGER.info("Search history saved for auth0Id=" + auth0Id + ": " + searchQuery);
        }

        if (title != null && !title.trim().isEmpty()) {
            return searchByTitle(title, genre, director, year);
        }
        if (director != null && !director.trim().isEmpty()) {
            return searchByDirector(director, year);
        }
        if (genre != null && !genre.trim().isEmpty()) {
            return searchByGenre(genre, year);
        }
        if (year != null && !year.trim().isEmpty()) {
            return searchByYear(year);
        }

        LOGGER.warning("No valid search parameters provided");
        return new ArrayList<>();
    }

    private String buildSearchQuery(String title, String genre, String director, String year) {
        StringBuilder query = new StringBuilder();
        if (title != null) query.append("title=").append(title);
        if (genre != null) query.append(query.length() > 0 ? ", " : "").append("genre=").append(genre);
        if (director != null) query.append(query.length() > 0 ? ", " : "").append("director=").append(director);
        if (year != null) query.append(query.length() > 0 ? ", " : "").append("year=").append(year);
        return query.toString();
    }

    public Map<String, Object> getMovieDetails(String title) {
        LOGGER.info("Fetching movie details for title: " + title);
        if (title == null || title.trim().isEmpty()) {
            LOGGER.warning("No title provided for movie details");
            return new HashMap<>();
        }

        String searchUrl = String.format("%s/search/movie?api_key=%s&query=%s",
                apiUrl, apiKey, title.replace(" ", "+"));
        try {
            Map<String, Object> searchResponse = tmdbClient.fetchFromTmdb(searchUrl);
            if (searchResponse == null || !searchResponse.containsKey("results")) {
                LOGGER.warning("TMDB API returned null or invalid response for title: " + title);
                return new HashMap<>();
            }

            List<Map<String, Object>> results = (List<Map<String, Object>>) searchResponse.get("results");
            if (results == null || results.isEmpty()) {
                LOGGER.warning("No movies found for title: " + title);
                return new HashMap<>();
            }

            Integer movieId = (Integer) results.get(0).get("id");
            return getMovieDetailsById(movieId);
        } catch (Exception e) {
            LOGGER.severe("Error fetching movie details for title '" + title + "': " + e.getMessage());
            throw new RuntimeException("Failed to get movie details by title", e);
        }
    }

    public Map<String, Object> getMovieDetails(Integer movieId) {
        return getMovieDetailsById(movieId);
    }

    private Map<String, Object> getMovieDetailsById(Integer movieId) {
        LOGGER.info("Fetching movie details for ID: " + movieId);
        if (movieId == null || movieId <= 0) {
            LOGGER.warning("Invalid movie ID: " + movieId);
            return new HashMap<>();
        }

        String detailsUrl = String.format("%s/movie/%s?api_key=%s", apiUrl, movieId, apiKey);
        try {
            Map<String, Object> details = tmdbClient.fetchFromTmdb(detailsUrl);
            if (details == null) {
                LOGGER.warning("No details found for movie ID: " + movieId);
                return new HashMap<>();
            }
            LOGGER.info("Movie details retrieved for ID: " + movieId);
            return details;
        } catch (Exception e) {
            LOGGER.severe("Error fetching movie details for ID " + movieId + ": " + e.getMessage());
            throw new RuntimeException("Failed to get movie details by ID", e);
        }
    }

    public List<String> getAutocompleteSuggestions(String prefix) {
        LOGGER.info("Fetching autocomplete suggestions for prefix: " + prefix);
        if (prefix == null || prefix.trim().isEmpty()) {
            LOGGER.warning("No prefix provided for autocomplete");
            return new ArrayList<>();
        }

        String searchUrl = String.format("%s/search/movie?api_key=%s&query=%s",
                apiUrl, apiKey, prefix.replace(" ", "+"));
        try {
            Map<String, Object> searchResponse = tmdbClient.fetchFromTmdb(searchUrl);
            if (searchResponse == null || !searchResponse.containsKey("results")) {
                LOGGER.warning("TMDB API returned null or invalid response for prefix: " + prefix);
                return new ArrayList<>();
            }

            List<Map<String, Object>> results = (List<Map<String, Object>>) searchResponse.get("results");
            if (results == null || results.isEmpty()) {
                LOGGER.warning("No suggestions found for prefix: " + prefix);
                return new ArrayList<>();
            }

            List<String> suggestions = new ArrayList<>();
            for (Map<String, Object> movie : results.subList(0, Math.min(5, results.size()))) {
                String title = (String) movie.get("title");
                if (title != null) {
                    suggestions.add(title);
                }
            }
            LOGGER.info("Returning " + suggestions.size() + " suggestions");
            return suggestions;
        } catch (Exception e) {
            LOGGER.severe("Error fetching autocomplete suggestions for prefix '" + prefix + "': " + e.getMessage());
            throw new RuntimeException("Failed to get autocomplete suggestions", e);
        }
    }

    private List<Map<String, Object>> searchByTitle(String title, String genre, String director, String year) {
        LOGGER.info("Searching by title: " + title + ", genre=" + genre + ", director=" + director + ", year=" + year);
        String query = title.replace(" ", "+");
        String searchUrl = String.format("%s/search/movie?api_key=%s&query=%s",
                apiUrl, apiKey, query);
        if (year != null && !year.trim().isEmpty()) {
            searchUrl += "&year=" + year;
        }

        try {
            Map<String, Object> searchResponse = tmdbClient.fetchFromTmdb(searchUrl);
            if (!searchResponse.containsKey("results")) {
                LOGGER.warning("TMDB API returned invalid response for title: " + title + ", response: " + searchResponse);
                return new ArrayList<>();
            }

            List<Map<String, Object>> results = (List<Map<String, Object>>) searchResponse.get("results");
            if (results == null || results.isEmpty()) {
                LOGGER.warning("No results found for title: " + title);
                return new ArrayList<>();
            }

            List<Map<String, Object>> filtered = new ArrayList<>();
            for (Map<String, Object> movie : results) {
                Integer movieId = (Integer) movie.get("id");
                String detailsUrl = String.format("%s/movie/%s?api_key=%s", apiUrl, movieId, apiKey);
                Map<String, Object> details = tmdbClient.fetchFromTmdb(detailsUrl);
                if (details == null) continue;

                boolean matches = true;
                if (genre != null && !genre.trim().isEmpty()) {
                    List<Map<String, Object>> genres = (List<Map<String, Object>>) details.get("genres");
                    boolean genreMatch = false;
                    if (genres != null) {
                        for (Map<String, Object> g : genres) {
                            String gName = (String) g.get("name");
                            if (gName != null && gName.toLowerCase().contains(genre.toLowerCase())) {
                                genreMatch = true;
                                break;
                            }
                        }
                    }
                    if (!genreMatch) matches = false;
                }
                if (director != null && !director.trim().isEmpty()) {
                    String creditsUrl = String.format("%s/movie/%s/credits?api_key=%s", apiUrl, movieId, apiKey);
                    Map<String, Object> credits = tmdbClient.fetchFromTmdb(creditsUrl);
                    List<Map<String, Object>> crew = (List<Map<String, Object>>) credits.get("crew");
                    boolean directorMatch = false;
                    if (crew != null) {
                        for (Map<String, Object> crewMember : crew) {
                            String job = (String) crewMember.get("job");
                            String name = (String) crewMember.get("name");
                            if ("Director".equalsIgnoreCase(job) && name != null &&
                                    name.toLowerCase().contains(director.toLowerCase())) {
                                directorMatch = true;
                                break;
                            }
                        }
                    }
                    if (!directorMatch) matches = false;
                }
                if (matches) {
                    filtered.add(details);
                }
            }
            LOGGER.info("Found " + filtered.size() + " movies for title: " + title);
            return filtered;
        } catch (Exception e) {
            LOGGER.severe("Error searching movies by title '" + title + "': " + e.getMessage() + ", cause: " + (e.getCause() != null ? e.getCause().getMessage() : "none"));
            throw new RuntimeException("Failed to search movies by title", e);
        }
    }

    private List<Map<String, Object>> searchByDirector(String director, String year) {
        LOGGER.info("Searching by director: " + director + ", year=" + year);
        String personUrl = String.format("%s/search/person?api_key=%s&query=%s",
                apiUrl, apiKey, director.replace(" ", "+"));

        try {
            Map<String, Object> personResponse = tmdbClient.fetchFromTmdb(personUrl);
            if (personResponse == null || !personResponse.containsKey("results")) {
                LOGGER.warning("TMDB API returned null or invalid response for director: " + director);
                return new ArrayList<>();
            }

            List<Map<String, Object>> persons = (List<Map<String, Object>>) personResponse.get("results");
            if (persons == null || persons.isEmpty()) {
                LOGGER.warning("No persons found for director: " + director);
                return new ArrayList<>();
            }

            Integer directorId = null;
            for (Map<String, Object> p : persons) {
                String name = (String) p.get("name");
                String dept = (String) p.get("known_for_department");
                if (dept != null && dept.equalsIgnoreCase("Directing")) {
                    if (name.equalsIgnoreCase(director)) {
                        directorId = (Integer) p.get("id");
                        break;
                    }
                    if (directorId == null) {
                        directorId = (Integer) p.get("id");
                    }
                }
            }

            if (directorId == null) {
                LOGGER.warning("No director ID found for: " + director);
                return new ArrayList<>();
            }

            String creditsUrl = String.format("%s/person/%s/movie_credits?api_key=%s",
                    apiUrl, directorId, apiKey);
            Map<String, Object> creditsResponse = tmdbClient.fetchFromTmdb(creditsUrl);
            if (creditsResponse == null || !creditsResponse.containsKey("crew")) {
                LOGGER.warning("No credits found for director ID: " + directorId);
                return new ArrayList<>();
            }

            List<Map<String, Object>> crew = (List<Map<String, Object>>) creditsResponse.get("crew");
            List<Map<String, Object>> movies = new ArrayList<>();
            if (crew != null) {
                for (Map<String, Object> movie : crew) {
                    String job = (String) movie.get("job");
                    if (!"Director".equalsIgnoreCase(job)) continue;

                    if (year != null && !year.trim().isEmpty()) {
                        String releaseDate = (String) movie.get("release_date");
                        if (releaseDate == null || !releaseDate.startsWith(year)) {
                            continue;
                        }
                    }
                    Integer movieId = (Integer) movie.get("id");
                    movies.add(getMovieDetailsById(movieId));
                }
            }
            LOGGER.info("Found " + movies.size() + " movies for director: " + director);
            return movies;
        } catch (Exception e) {
            LOGGER.severe("Error searching movies by director '" + director + "': " + e.getMessage());
            throw new RuntimeException("Failed to search movies by director", e);
        }
    }

    private List<Map<String, Object>> searchByGenre(String genre, String year) {
        LOGGER.info("Searching by genre: " + genre + ", year=" + year);
        String normalizedGenre = genre.toLowerCase();
        String genreId = genreMap.get(normalizedGenre);
        if (genreId == null) {
            LOGGER.warning("Invalid genre: " + genre);
            return new ArrayList<>();
        }

        String discoverUrl = String.format("%s/discover/movie?api_key=%s&with_genres=%s",
                apiUrl, apiKey, genreId);
        if (year != null && !year.trim().isEmpty()) {
            discoverUrl += "&primary_release_year=" + year;
        }

        try {
            Map<String, Object> discoverResponse = tmdbClient.fetchFromTmdb(discoverUrl);
            if (discoverResponse == null || !discoverResponse.containsKey("results")) {
                LOGGER.warning("TMDB API returned null or invalid response for genre: " + genre);
                return new ArrayList<>();
            }

            List<Map<String, Object>> results = (List<Map<String, Object>>) discoverResponse.get("results");
            if (results == null || results.isEmpty()) {
                LOGGER.warning("No movies found for genre: " + genre);
                return new ArrayList<>();
            }

            List<Map<String, Object>> movies = new ArrayList<>();
            for (Map<String, Object> movie : results) {
                Integer movieId = (Integer) movie.get("id");
                movies.add(getMovieDetailsById(movieId));
            }
            LOGGER.info("Found " + movies.size() + " movies for genre: " + genre);
            return movies;
        } catch (Exception e) {
            LOGGER.severe("Error searching movies by genre '" + genre + "': " + e.getMessage());
            throw new RuntimeException("Failed to search movies by genre", e);
        }
    }

    private List<Map<String, Object>> searchByYear(String year) {
        LOGGER.info("Searching by year: " + year);
        String discoverUrl = String.format("%s/discover/movie?api_key=%s&primary_release_year=%s",
                apiUrl, apiKey, year);

        try {
            Map<String, Object> discoverResponse = tmdbClient.fetchFromTmdb(discoverUrl);
            if (discoverResponse == null || !discoverResponse.containsKey("results")) {
                LOGGER.warning("TMDB API returned null or invalid response for year: " + year);
                return new ArrayList<>();
            }

            List<Map<String, Object>> results = (List<Map<String, Object>>) discoverResponse.get("results");
            if (results == null || results.isEmpty()) {
                LOGGER.warning("No movies found for year: " + year);
                return new ArrayList<>();
            }

            List<Map<String, Object>> movies = new ArrayList<>();
            for (Map<String, Object> movie : results) {
                Integer movieId = (Integer) movie.get("id");
                movies.add(getMovieDetailsById(movieId));
            }
            LOGGER.info("Found " + movies.size() + " movies for year: " + year);
            return movies;
        } catch (Exception e) {
            LOGGER.severe("Error searching movies by year '" + year + "': " + e.getMessage());
            throw new RuntimeException("Failed to search movies by year", e);
        }
    }

    private static final Map<String, String> genreMap = new HashMap<>();
    static {
        genreMap.put("action", "28");
        genreMap.put("adventure", "12");
        genreMap.put("animation", "16");
        genreMap.put("comedy", "35");
        genreMap.put("crime", "80");
        genreMap.put("documentary", "99");
        genreMap.put("drama", "18");
        genreMap.put("family", "10751");
        genreMap.put("fantasy", "14");
        genreMap.put("history", "36");
        genreMap.put("horror", "27");
        genreMap.put("music", "10402");
        genreMap.put("mystery", "9648");
        genreMap.put("romance", "10749");
        genreMap.put("science fiction", "878");
        genreMap.put("tv movie", "10770");
        genreMap.put("thriller", "53");
        genreMap.put("war", "10752");
        genreMap.put("western", "37");
    }
}