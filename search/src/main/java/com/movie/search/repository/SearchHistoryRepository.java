package com.movie.search.repository;

import com.movie.search.model.SearchHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface SearchHistoryRepository extends JpaRepository<SearchHistory, Long> {
    List<SearchHistory> findByUserUserIdOrderBySearchTimestampDesc(Long userId);
    Optional<SearchHistory> findByUserUserIdAndMovieId(Long userId, Integer movieId);
}