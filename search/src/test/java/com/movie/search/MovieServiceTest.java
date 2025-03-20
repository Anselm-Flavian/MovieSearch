package com.movie.search;

import com.movie.search.service.MovieService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest
class MovieServiceTest {

    @Test
    void contextLoads() {
        assertNotNull(new MovieService());
    }
}
