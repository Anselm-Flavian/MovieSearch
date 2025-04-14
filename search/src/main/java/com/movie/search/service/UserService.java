package com.movie.search.service;

import com.movie.search.model.User;
import com.movie.search.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.Map;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

@Service
public class UserService {

    private static final Logger LOGGER = Logger.getLogger(UserService.class.getName());

    @Autowired
    private UserRepository userRepository;

    private final RestTemplate restTemplate = new RestTemplate();

    public User updateUserFromAuth0(String auth0Id, String accessToken) {
        String userInfoUrl = "https://{your-auth0-domain}/userinfo";

        try {
            // Set Authorization header correctly
            Map<String, Object> userInfo = restTemplate.getForObject(userInfoUrl, Map.class);

            if (userInfo == null || !userInfo.containsKey("email")) {
                throw new RuntimeException("Invalid response from Auth0");
            }

            String email = (String) userInfo.get("email");
            String username = (String) userInfo.getOrDefault("nickname", email);

            // Check if user exists by auth0Id
            Optional<User> existingUser = userRepository.findByAuth0Id(auth0Id);
            if (existingUser.isPresent()) {
                return existingUser.get();
            }

            // Check if user exists by email
            Optional<User> existingUserByEmail = userRepository.findByEmail(email);
            if (existingUserByEmail.isPresent()) {
                throw new RuntimeException("User with this email already exists.");
            }

            // Create new user
            User newUser = new User();
            newUser.setAuth0Id(auth0Id);
            newUser.setUsername(username);
            newUser.setEmail(email);

            User savedUser = userRepository.save(newUser);
            LOGGER.info("Created new user with auth0Id=" + auth0Id);
            return savedUser;

        } catch (HttpClientErrorException e) {
            LOGGER.log(Level.SEVERE, "Auth0 API error: " + e.getMessage(), e);
            throw new RuntimeException("Failed to fetch user data from Auth0");
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Unexpected error while creating user: " + e.getMessage(), e);
            throw new RuntimeException("Failed to create user");
        }
    }
}
