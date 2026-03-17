package net.fosterlink.fosterlinkbackend.config;

import net.fosterlink.fosterlinkbackend.security.JwtAuthFilter;
import net.fosterlink.fosterlinkbackend.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.context.request.async.WebAsyncManagerIntegrationFilter;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.ForwardedHeaderFilter;
import jakarta.servlet.http.HttpServletResponse;

@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
@Configuration
public class SecurityConfig {

    // Endpoints accessible without authentication. All other /v1/** paths require at least
    // a valid session token; fine-grained role checks are enforced at the method level via
    // @PreAuthorize. The catch-all rule is .denyAll() so any path not listed here or under
    // /v1/** returns 403 rather than silently passing (GAP-07 / 02/F-01).
    private final String[] publicEndpoints = {
            "/v1/users/register",
            "/v1/users/login",
            "/v1/users/refresh",
            "/v1/users/forgotPassword",
            "/v1/users/resetPassword",
            "/v1/threads/search-by-id",
            "/v1",
            "/v1/threads/getAll",
            "/v1/threads/rand",
            "/v1/threads/search",
            "/v1/threads/replies",
            "/v1/faq/all",
            "/v1/faq/content",
            "/v1/users/isAdmin",
            "/v1/users/isFaqAuthor",
            "/v1/users/privileges",
            "/v1/faq/unapprovedCount",
            "/v1/users/agentInfo",
            "/v1/agencies/all",
            "/v1/users/profileMetadata",
            "/v1/threads/getThreads",
            "/v1/faq/allAuthor",
            "/v1/threads/search-by-user",
            "/v1/token/**",
            "/v1/maps/static"
    };

    @Value("${app.frontendUrl}")
    private String frontendUrl;

    @Autowired private UserService userService;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private JwtAuthFilter authFilter;
    @Autowired private UrlBasedCorsConfigurationSource corsConfigurationSource;
    @Autowired private ForwardedHeaderFilter forwardedHeaderFilter;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                // CSRF is disabled because authentication is stateless (JWT via Authorization
                // header, not browser-managed cookies), so CSRF attacks do not apply.
                .csrf(AbstractHttpConfigurer::disable)
                .cors(cors -> cors.configurationSource(corsConfigurationSource))
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(ex -> ex.authenticationEntryPoint((request, response, authException) -> {
                    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                }))
                .authorizeHttpRequests(auth ->
                            auth
                            .requestMatchers("/actuator/**").hasAuthority("ADMINISTRATOR")
                            // Swagger UI and its backing API-docs are admin-only. springdoc serves
                            // these paths only when springdoc.swagger-ui.enabled=true (local dev).
                            // In production both properties remain false so these paths return 404
                            // before Spring Security even evaluates them, but the explicit
                            // hasAuthority rule ensures they can never be reached without a valid
                            // admin JWT even if springdoc is accidentally re-enabled in prod.
                            .requestMatchers("/swagger-ui/**", "/swagger-ui.html", "/v3/api-docs/**").hasAuthority("ADMINISTRATOR")
                            .requestMatchers(publicEndpoints).permitAll()
                            // All remaining /v1/** paths require a valid session token at minimum.
                            // Fine-grained role checks (ADMINISTRATOR, AGENCY_REP, etc.) are
                            // enforced at the method level via @PreAuthorize.
                            .requestMatchers("/v1/**").authenticated()
                            // Deny everything else — unknown paths must not silently pass (GAP-07).
                            .anyRequest().denyAll()
                        )
                .headers(headers -> headers
                        .frameOptions(frame -> frame.disable())
                        .contentSecurityPolicy(csp -> csp
                                .policyDirectives("frame-ancestors 'self' " + frontendUrl)))
                .addFilterBefore(authFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(forwardedHeaderFilter, WebAsyncManagerIntegrationFilter.class);
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
