package net.fosterlink.fosterlinkbackend.config;

import net.fosterlink.fosterlinkbackend.security.JwtAuthFilter;
import net.fosterlink.fosterlinkbackend.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

@EnableWebSecurity
@Configuration
public class SecurityConfig {

    private final String[] publicEndpoints = {
            "/v1/users/register",
            "/v1/users/login",
            "/swagger-ui/**",
            "/v1/docs/**",
            "/v3/api-docs/**",
            "/v1/threads/search-by-title",
            "/v1/threads/search-by-id",
            "/v1/threads/search-by-date",
            "/swagger-ui.html",
            "/swagger-resources/**",
            "/webjars/**",
            "/v1"
    };
    private final String[] privateEndpoints = {
        "/v1/users/get-all", "/v1/users/delete", "/v1/users/update", "/v1/threads/create", "/v1/threads/update", "/v1/threads/delete", "/v1/users/getInfo"
    };

    @Autowired private UserService userService;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private JwtAuthFilter authFilter;
    @Autowired private UrlBasedCorsConfigurationSource corsConfigurationSource;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http.csrf(AbstractHttpConfigurer::disable)//todo
                .cors(cors -> cors.configurationSource(corsConfigurationSource))
                .authorizeHttpRequests(auth ->
                            auth.requestMatchers(publicEndpoints).permitAll()
                            .requestMatchers(privateEndpoints).authenticated()
                            .anyRequest().authenticated()
                        )
                .addFilterBefore(authFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    @Autowired
    public void configureGlobal(AuthenticationManagerBuilder auth) throws Exception {
        auth.userDetailsService(userService).passwordEncoder(passwordEncoder);
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authenticationConfiguration) throws Exception {
        return authenticationConfiguration.getAuthenticationManager();
    }


}
