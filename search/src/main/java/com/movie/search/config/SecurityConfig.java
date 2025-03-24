package com.movie.search.config;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.auth0.jwt.interfaces.RSAKeyProvider;
import com.auth0.jwk.Jwk;
import com.auth0.jwk.JwkException;
import com.auth0.jwk.UrlJwkProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Collections;
import java.util.logging.Logger;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final String auth0Domain;
    private final String audience;

    public SecurityConfig(
            @Value("${auth0.domain:dev-opqvt1nsdwq040ox.us.auth0.com}") String auth0Domain,
            @Value("${auth0.audience:https://dev-opqvt1nsdwq040ox.us.auth0.com/api/v2/}") String audience) {
        this.auth0Domain = auth0Domain;
        this.audience = audience;
        System.out.println("Loaded auth0.domain: " + auth0Domain);
        System.out.println("Loaded auth0.audience: " + audience);
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.OPTIONS, "/api/**").permitAll()
                        .requestMatchers("/api/movies/**").permitAll() // Temporarily allow all for testing
                        .anyRequest().permitAll()
                );
//                .addFilterBefore(new JwtAuthenticationFilter(auth0Domain, audience), UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(Collections.singletonList("http://localhost:3000"));
        configuration.setAllowedMethods(Collections.singletonList("*"));
        configuration.setAllowedHeaders(Collections.singletonList("*"));
        configuration.setAllowCredentials(true);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", configuration);
        return source;
    }
}

class JwtAuthenticationFilter implements Filter {

    private final String auth0Domain;
    private final String audience;
    private static final Logger LOGGER = Logger.getLogger(JwtAuthenticationFilter.class.getName());

    public JwtAuthenticationFilter(String auth0Domain, String audience) {
        this.auth0Domain = auth0Domain;
        this.audience = audience;
    }

    @Override
    public void doFilter(jakarta.servlet.ServletRequest request, jakarta.servlet.ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        LOGGER.info("Processing request: " + httpRequest.getMethod() + " " + httpRequest.getRequestURI());

        if ("OPTIONS".equalsIgnoreCase(httpRequest.getMethod())) {
            LOGGER.info("Skipping OPTIONS request");
            chain.doFilter(request, response);
            return;
        }

        String header = httpRequest.getHeader("Authorization");
        LOGGER.info("Authorization Header: " + header);

        if (header == null || !header.startsWith("Bearer ")) {
            LOGGER.warning("No valid Bearer token found, proceeding without authentication");
            chain.doFilter(request, response);
            return;
        }

        String token = header.substring(7);
        LOGGER.info("Extracted token: " + token);
        try {
            LOGGER.info("Fetching JWKS from: https://" + auth0Domain + "/.well-known/jwks.json");
            Algorithm algorithm = Algorithm.RSA256(new JwksKeyProvider(auth0Domain));
            LOGGER.info("Building JWT verifier with issuer: https://" + auth0Domain + "/ and audience: " + audience);
            JWTVerifier verifier = JWT.require(algorithm)
                    .withIssuer("https://" + auth0Domain + "/")
                    .withAudience(audience)
                    .build();
            LOGGER.info("Verifying token...");
            DecodedJWT jwt = verifier.verify(token);
            LOGGER.info("JWT verified successfully for subject: " + jwt.getSubject());

            UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                    jwt.getSubject(), null, Collections.emptyList());
            SecurityContextHolder.getContext().setAuthentication(auth);

            chain.doFilter(request, response);
        } catch (JWTVerificationException e) {
            LOGGER.warning("JWT Verification Failed: " + e.getMessage());
            httpResponse.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            httpResponse.getWriter().write("Invalid Token: " + e.getMessage());
            httpResponse.getWriter().flush();
            return;
        } catch (Exception e) {
            LOGGER.severe("Unexpected error in JWT filter: " + e.getMessage());
            e.printStackTrace();
            httpResponse.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            httpResponse.getWriter().write("Unexpected error in JWT filter: " + e.getMessage());
            httpResponse.getWriter().flush();
            return;
        }
    }

    @Override
    public void init(jakarta.servlet.FilterConfig filterConfig) throws ServletException {
        LOGGER.info("Initializing JwtAuthenticationFilter");
    }

    @Override
    public void destroy() {
        LOGGER.info("Destroying JwtAuthenticationFilter");
    }
}

class JwksKeyProvider implements RSAKeyProvider {
    private final String domain;
    private static final Logger LOGGER = Logger.getLogger(JwksKeyProvider.class.getName());

    public JwksKeyProvider(String domain) {
        this.domain = domain;
    }

    @Override
    public java.security.interfaces.RSAPublicKey getPublicKeyById(String keyId) {
        try {
            String jwksUrl = "https://" + domain + "/.well-known/jwks.json"; // Define URL explicitly
            LOGGER.info("Fetching JWKS from: " + jwksUrl);
            UrlJwkProvider provider = new UrlJwkProvider(jwksUrl); // Correct single path
            Jwk jwk = provider.get(keyId);
            LOGGER.info("JWKS fetched successfully for keyId: " + keyId);
            return (java.security.interfaces.RSAPublicKey) jwk.getPublicKey();
        } catch (JwkException e) {
            LOGGER.severe("Failed to fetch JWKS: " + e.getMessage());
            throw new RuntimeException("Failed to fetch JWKS: " + e.getMessage(), e);
        }
    }

    @Override
    public java.security.interfaces.RSAPrivateKey getPrivateKey() {
        return null;
    }

    @Override
    public String getPrivateKeyId() {
        return null;
    }
}