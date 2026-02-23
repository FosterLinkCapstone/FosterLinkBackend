package net.fosterlink.fosterlinkbackend.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeIn;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.models.headers.Header;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(info = @Info(
        title = "FosterLink Backend API",
        version = "1.0",
        description = """
                FosterLink Backend API for managing foster care resources, forums, FAQs, and agencies.
                
                ## Rate Limiting
                
                All endpoints are rate-limited to prevent abuse. Rate limits are applied per IP address or per authenticated user depending on the endpoint.
                
                **Response Headers:**
                - `X-Rate-Limit-Remaining`: Number of requests remaining in the current window
                - `X-Rate-Limit-Retry-After-Seconds`: Seconds to wait before retrying (only on 429 responses)
                
                **Sustained Limits (per 60 seconds):**
                - Public endpoints: 50 requests per IP
                - Authenticated read endpoints: 50-100 requests per user
                - Content creation (posts, replies): 5-15 requests per user
                - Likes/interactions: 30 requests per user
                - Authentication (login/register): 5 requests per IP
                
                **Burst Limits (prevents rapid-fire requests):**
                - Registration: 1 request per 30 seconds
                - Login: 2 requests per 10 seconds
                - Thread/FAQ/Agency creation: 2 requests per 15 seconds
                - Replies: 3 requests per 10 seconds
                - Likes: 5 requests per 5 seconds
                
                A request must pass BOTH the sustained and burst limits to succeed.
                """
))
@SecurityScheme(
        name="bearerAuth",
        type = SecuritySchemeType.HTTP,
        scheme = "bearer",
        bearerFormat = "JWT",
        in = SecuritySchemeIn.HEADER
)
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .components(new Components()
                        .addHeaders("X-Rate-Limit-Remaining", new Header()
                                .description("Number of requests remaining in the current rate limit window")
                                .schema(new StringSchema()))
                        .addHeaders("X-Rate-Limit-Retry-After-Seconds", new Header()
                                .description("Seconds to wait before retrying (only present on 429 responses)")
                                .schema(new StringSchema())));
    }
}
