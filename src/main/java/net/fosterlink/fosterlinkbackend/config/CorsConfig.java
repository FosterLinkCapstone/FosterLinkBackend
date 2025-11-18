package net.fosterlink.fosterlinkbackend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

@Configuration
public class CorsConfig {

    private final List<String> allowedOrigins = List.of(
            // dev vite IP
            "http://localhost:5173",

            // frontend staging server outbound IPs

            "130.107.159.126",
            "130.107.156.254",
            "130.107.157.44",
            "130.107.157.238",
            "130.107.157.255",
            "130.107.158.152",
            "130.107.159.45",
            "130.107.159.61",
            "130.107.159.66",
            "130.107.159.69",
            "130.107.159.71",
            "130.107.159.75",
            "130.107.159.77",
            "130.107.159.78",
            "130.107.159.81",
            "130.107.159.89",
            "130.107.159.96",
            "130.107.159.99",
            "130.107.159.104",
            "130.107.159.108",
            "130.107.159.112",
            "130.107.159.116",
            "130.107.159.119",
            "130.107.159.121",
            "130.107.159.141",
            "130.107.159.158",
            "130.107.159.174",
            "130.107.159.180",
            "130.107.159.187",
            "130.107.159.201",
            "20.48.204.14",
            "130.107.159.126",
            "130.107.156.254",
            "130.107.157.44",
            "130.107.157.238",
            "130.107.157.255",
            "130.107.158.152",
            "130.107.159.45",
            "130.107.159.61",
            "130.107.159.66",
            "130.107.159.69",
            "130.107.159.71",
            "130.107.159.75",
            "20.48.204.14",

            // staging url

            "https://fosterlink-frontend-staging-f2gcfxcbeqfuasax.canadacentral-01.azurewebsites.net"
    );

    @Bean
    public UrlBasedCorsConfigurationSource urlBasedCorsConfigurationSource() {
        CorsConfiguration corsConfiguration = new CorsConfiguration();
        corsConfiguration.setAllowedOrigins(allowedOrigins); //TODO
        corsConfiguration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE"));
        corsConfiguration.setAllowedHeaders(List.of("*"));
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", corsConfiguration);
        return source;
    }

}
