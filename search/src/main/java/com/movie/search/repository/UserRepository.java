package com.movie.search.repository;

import com.movie.search.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    Optional<User> findByAuth0Id(String auth0Id);

    List<User> findByUsername(String tempUsername);
}