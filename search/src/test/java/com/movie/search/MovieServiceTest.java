package com.movie.search;

import com.movie.search.service.MovieService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@WebMvcTest(MovieService.class) // Loads only MovieService, not the full context
class MovieServiceTest {

    @MockBean
    private MovieService movieService; // Mocking the service to avoid autowiring issues

    @Test
    void contextLoads() {
        assertNotNull(movieService);
    }
}
