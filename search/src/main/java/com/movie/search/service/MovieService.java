package com.movie.search.service;

import io.github.cdimascio.dotenv.Dotenv;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import com.movie.search.util.FuzzySearchUtil;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

@Service
public class MovieService {

    private final String apiKey;
    private final String apiUrl;
    private final RestTemplate restTemplate = new RestTemplate();

    public MovieService() {
        Dotenv dotenv = Dotenv.load();
        this.apiKey = dotenv.get("TMDB_API_KEY");
        this.apiUrl = dotenv.get("TMDB_API_URL");
    }

    public Map<String, Object> getMovieDetails(String title) {
        if (title == null || title.trim().isEmpty()) {
            throw new IllegalArgumentException("Movie title cannot be empty");
        }
        String searchUrl = String.format("%s/search/movie?api_key=%s&query=%s",
                apiUrl, apiKey, title.replace(" ", "+"));
        System.out.println("Searching with URL: " + searchUrl);
        Map<String, Object> searchResponse = restTemplate.getForObject(searchUrl, Map.class);
        List<Map<String, Object>> results = (List<Map<String, Object>>) searchResponse.get("results");
        if (results == null || results.isEmpty()) {
            return Collections.emptyMap();
        }
        Integer movieId = (Integer) results.get(0).get("id");
        String detailsUrl = String.format("%s/movie/%s?api_key=%s", apiUrl, movieId, apiKey);
        System.out.println("Fetching details from URL: " + detailsUrl);
        return restTemplate.getForObject(detailsUrl, Map.class);
    }

    private final static Map<String, String> genreMap = new HashMap<>();
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
    public Map<String, Object> getMovieDetails(Integer movieId) {
        String detailsUrl = String.format("%s/movie/%s?api_key=%s", apiUrl, movieId, apiKey);
        Map<String, Object> details = restTemplate.getForObject(detailsUrl, Map.class);
        if (details != null) {
            Map<String, Object> response = new HashMap<>();
            response.put("id", movieId);
            response.put("title", details.get("title"));
            response.put("poster_path", details.get("poster_path"));
            response.put("release_date", details.get("release_date"));
            response.put("overview", details.get("overview"));
            response.put("genres", details.get("genres"));
            response.put("runtime", details.get("runtime"));
            response.put("vote_average", details.get("vote_average"));
            return response;
        }
        return new HashMap<>();
    }
    public List<Map<String, Object>> searchMovies(String title, String genre, String director, String year) {
        if (title != null && !title.trim().isEmpty()) {
            return searchByTitle(title, genre, director, year);
        } else if (director != null && !director.trim().isEmpty()) {
            return searchByDirector(director, year);
        } else if (genre != null && !genre.trim().isEmpty()) {
            return searchByGenre(genre, year);
        } else if (year != null && !year.trim().isEmpty()) {
            return searchByYear(year);
        }
        System.out.println("No search criteria provided, returning empty list");
        return new ArrayList<>();
    }

    private List<Map<String, Object>> searchByTitle(String title, String genre, String director, String year) {
        System.out.println("searchByTitle called with: title=" + title + ", genre=" + genre + ", director=" + director + ", year=" + year);
        String query = title.length() > 4 ? title : title.substring(0, Math.min(title.length(), 4));
        String searchUrl = String.format("%s/search/movie?api_key=%s&query=%s",
                apiUrl, apiKey, query.replace(" ", "+"));
        if (year != null && !year.trim().isEmpty()) {
            searchUrl += "&year=" + year;
        }
        System.out.println("Searching movies by title with URL: " + searchUrl);
        try {
            Map<String, Object> searchResponse = restTemplate.getForObject(searchUrl, Map.class);
            if (searchResponse == null) {
                System.out.println("TMDb returned null response");
                return new ArrayList<>();
            }
            List<Map<String, Object>> results = (List<Map<String, Object>>) searchResponse.get("results");
            if (results == null || results.isEmpty()) {
                System.out.println("No results from TMDb");
                return new ArrayList<>();
            }

            // Apply fuzzy filtering
            List<Map<String, Object>> fuzzyResults = FuzzySearchUtil.fuzzyFilterMovies(title, results);
            System.out.println("Fuzzy filtered results: " + fuzzyResults);
            if (fuzzyResults.isEmpty()) {
                System.out.println("No fuzzy matches found");
                return new ArrayList<>();
            }

            // Fetch details only for fuzzy-filtered results and apply additional filters
            List<Map<String, Object>> filtered = new ArrayList<>();
            for (Map<String, Object> movie : fuzzyResults) {
                Integer movieId = (Integer) movie.get("id");
                String detailsUrl = String.format("%s/movie/%s?api_key=%s", apiUrl, movieId, apiKey);
                Map<String, Object> details = restTemplate.getForObject(detailsUrl, Map.class);
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
                    Map<String, Object> credits = restTemplate.getForObject(creditsUrl, Map.class);
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
            System.out.println("Final filtered results: " + filtered);
            return filtered;
        } catch (Exception e) {
            System.out.println("Error calling TMDb API: " + e.getMessage());
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    private List<Map<String, Object>> searchByDirector(String director, String year) {
        // Construct search URL for the director
        String personUrl = String.format("%s/search/person?api_key=%s&query=%s",
                apiUrl, apiKey, director.replace(" ", "+"));
        System.out.println("Searching director with URL: " + personUrl);

        // Fetch response
        Map<String, Object> personResponse = restTemplate.getForObject(personUrl, Map.class);
        if (personResponse == null || !personResponse.containsKey("results")) {
            return new ArrayList<>();
        }

        // Extract list of potential directors
        List<Map<String, Object>> persons = (List<Map<String, Object>>) personResponse.get("results");
        if (persons == null || persons.isEmpty()) {
            return new ArrayList<>();
        }

        // Find the best matching director
        Integer directorId = null;
        for (Map<String, Object> p : persons) {
            String name = (String) p.get("name");
            String dept = (String) p.get("known_for_department");

            if (dept != null && dept.equalsIgnoreCase("Directing")) {
                // If an exact match is found, use it immediately
                if (name.equalsIgnoreCase(director)) {
                    directorId = (Integer) p.get("id");
                    break;
                }
                // Store the first match if an exact match isn't found
                if (directorId == null) {
                    directorId = (Integer) p.get("id");
                }
            }
        }

        // If no director ID found, return empty result
        if (directorId == null) {
            return new ArrayList<>();
        }

        // Fetch director's movie credits
        String creditsUrl = String.format("%s/person/%s/movie_credits?api_key=%s",
                apiUrl, directorId, apiKey);
        Map<String, Object> creditsResponse = restTemplate.getForObject(creditsUrl, Map.class);
        if (creditsResponse == null || !creditsResponse.containsKey("crew")) {
            return new ArrayList<>();
        }

        // Filter movies directed by the selected director
        List<Map<String, Object>> crew = (List<Map<String, Object>>) creditsResponse.get("crew");
        List<Map<String, Object>> movies = new ArrayList<>();

        if (crew != null) {
            for (Map<String, Object> movie : crew) {
                String job = (String) movie.get("job");

                // Ensure only movies directed by the person are added
                if (!"Director".equalsIgnoreCase(job)) continue;

                // Filter by release year if provided
                if (year != null && !year.trim().isEmpty()) {
                    String releaseDate = (String) movie.get("release_date");
                    if (releaseDate == null || !releaseDate.startsWith(year)) {
                        continue;
                    }
                }
                movies.add(movie);
            }
        }

        return movies;
    }


    private List<Map<String, Object>> searchByGenre(String genre, String year) {
        String normalizedGenre = genre.toLowerCase();
        String genreId = genreMap.get(normalizedGenre);
        if (genreId == null) {
            System.out.println("Invalid genre: " + genre);
            return new ArrayList<>();
        }
        String discoverUrl = String.format("%s/discover/movie?api_key=%s&with_genres=%s", apiUrl, apiKey, genreId);
        if (year != null && !year.trim().isEmpty()) {
            discoverUrl += "&primary_release_year=" + year;
        }
        System.out.println("Discovering movies by genre with URL: " + discoverUrl);
        try {
            Map<String, Object> discoverResponse = restTemplate.getForObject(discoverUrl, Map.class);
            if (discoverResponse == null || !discoverResponse.containsKey("results")) {
                return new ArrayList<>();
            }
            List<Map<String, Object>> discovered = (List<Map<String, Object>>) discoverResponse.get("results");
            return discovered != null ? discovered : new ArrayList<>();
        } catch (Exception e) {
            System.out.println("Error calling TMDb API: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    private List<Map<String, Object>> searchByYear(String year) {
        String discoverUrl = String.format("%s/discover/movie?api_key=%s&primary_release_year=%s",
                apiUrl, apiKey, year);
        System.out.println("Discovering movies by year with URL: " + discoverUrl);
        Map<String, Object> discoverResponse = restTemplate.getForObject(discoverUrl, Map.class);
        List<Map<String, Object>> discovered = (List<Map<String, Object>>) discoverResponse.get("results");
        return discovered != null ? discovered : new ArrayList<>();
    }
}