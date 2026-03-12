package net.fosterlink.fosterlinkbackend.config;

import net.fosterlink.fosterlinkbackend.security.JwtAuthFilter;
import net.fosterlink.fosterlinkbackend.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
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
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.context.request.async.WebAsyncManagerIntegrationFilter;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.ForwardedHeaderFilter;
import org.springframework.http.HttpStatus;

@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
@Configuration
public class SecurityConfig {

    private final String[] publicEndpoints = {
            "/v1/users/register",
            "/v1/users/login",
            "/v1/users/refresh",
            "/v1/users/forgotPassword",
            "/v1/users/resetPassword",
            "/swagger-ui/**",
            "/v1/docs/**",
            "/v3/api-docs/**",
            "/v1/threads/search-by-id",
            "/swagger-ui.html",
            "/swagger-resources/**",
            "/webjars/**",
            "/v1",
            "/v1/threads/getAll",
            "/actuator/**",
            "/v1/threads/rand",
            "/v1/threads/search",
            "/v1/threads/replies",
            "/v1/faq/all",
            "/v1/faq/content",
            "/v1/users/isAdmin",
            "/v1/users/isFaqAuthor",
            "/v1/faq/unapprovedCount",
            "/v1/users/agentInfo",
            "/v1/agencies/all",
            "/v1/users/profileMetadata",
            "/v1/threads/getThreads",
            "/v1/faq/allAuthor",
            "/v1/threads/search-by-user",
            "/v1/token/**"
    };
    private final String[] privateEndpoints = {
        "/v1/users/get-all", "/v1/users/delete", "/v1/users/update", "/v1/threads/create", "/v1/threads/update", "/v1/threads/delete", "/v1/users/getInfo",
            "/v1/threads/replies/like", "/v1/threads/create", "/v1/threads/replies/update", "/v1/threads/replies/delete",
            "/v1/threads/replies/hide", "/v1/threads/replies/hidden/delete",
            "/v1/faq/delete", "/v1/agencies/delete",
            "/v1/account-deletion/request", "/v1/account-deletion/cancel", "/v1/account-deletion/my-request",
            "/v1/account-deletion/requests", "/v1/account-deletion/approve", "/v1/account-deletion/delay",
            "/v1/admin/users/search", "/v1/admin/users/setRole",
            "/v1/users/logout-all",
            "/v1/mail/emailPreferences",
            "/v1/mail/unsubscribeAll",
            "/v1/mail/resubscribe"
    };

    @Autowired private UserService userService;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private JwtAuthFilter authFilter;
    @Autowired private UrlBasedCorsConfigurationSource corsConfigurationSource;
    @Autowired private ForwardedHeaderFilter forwardedHeaderFilter;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> {
                    CookieCsrfTokenRepository tokenRepository = CookieCsrfTokenRepository.withHttpOnlyFalse();
                    CsrfTokenRequestAttributeHandler requestHandler = new CsrfTokenRequestAttributeHandler();
                    requestHandler.setCsrfRequestAttributeName(null);
                    csrf.csrfTokenRepository(tokenRepository)
                            .csrfTokenRequestHandler(requestHandler)
                            .ignoringRequestMatchers("/v1/users/login", "/v1/users/refresh")
                            .ignoringRequestMatchers("/v1/token/**")
                            .ignoringRequestMatchers("/v1/users/forgotPassword", "/v1/users/resetPassword");
                })
                .cors(cors -> cors.configurationSource(corsConfigurationSource))
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(ex -> ex.authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)))
                .authorizeHttpRequests(auth ->
                            auth
                            .requestMatchers(publicEndpoints).permitAll()
                            .requestMatchers(privateEndpoints).authenticated()
                            .anyRequest().authenticated()
                        )
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
