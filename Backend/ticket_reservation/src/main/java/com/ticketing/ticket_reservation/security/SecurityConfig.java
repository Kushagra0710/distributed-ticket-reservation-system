package com.ticketing.ticket_reservation.security;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ticketing.ticket_reservation.dto.ApiResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import jakarta.servlet.http.HttpServletResponse;

@Configuration 
@EnableWebSecurity
public class SecurityConfig {

    private final ObjectMapper objectMapper;
    private final JwtUtils jwtUtils;

    public SecurityConfig(ObjectMapper objectMapper, JwtUtils jwtUtils) {
        this.objectMapper = objectMapper;
        this.jwtUtils = jwtUtils;
    }
    
    @Bean 
    public PasswordEncoder passwordEncoder(){
        return new BCryptPasswordEncoder();
    }

    @Bean 
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception{
        http
            .cors(cors -> cors.configurationSource(corsConfigurationSource())) // <-- MUST HAVE THIS LINE
            .csrf(csrf -> csrf.disable())
            .sessionManagement(session-> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/auth/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/events/**").permitAll()
                .requestMatchers("/api/admin/events/**", "/api/admin/users/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.GET, "/api/admin/venue").hasAnyRole("ADMIN", "OWNER")
                .requestMatchers(HttpMethod.POST, "/api/admin/venue").hasRole("ADMIN")
                .requestMatchers(HttpMethod.PUT, "/api/admin/venue/*").hasRole("ADMIN")
                .requestMatchers(HttpMethod.DELETE, "/api/admin/venue/*").hasRole("ADMIN")
                .requestMatchers(HttpMethod.DELETE, "/api/admin/venue/*/delete").hasRole("ADMIN")
                .requestMatchers("/api/admin/venue/**", "/api/owner/**").hasAnyRole("ADMIN", "OWNER")
                .requestMatchers("/api/admin/**").hasRole("ADMIN")
                .anyRequest().authenticated()
            )
            .exceptionHandling(exceptions -> exceptions
                .authenticationEntryPoint((request, response, exception) ->
                    writeError(response, HttpStatus.UNAUTHORIZED, "Authentication is required"))
                .accessDeniedHandler((request, response, exception) ->
                    writeError(response, HttpStatus.FORBIDDEN, "You do not have permission to access this resource"))
            )
            .addFilterBefore(new JwtAuthenticationFilter(jwtUtils), UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    private void writeError(HttpServletResponse response, HttpStatus status, String message) throws IOException {
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getOutputStream(), new ApiResponse(false, message, Map.of()));
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(List.of("http://localhost:5173")); // Tell Spring to allow Vite
            configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
