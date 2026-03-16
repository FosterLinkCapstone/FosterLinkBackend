package net.fosterlink.fosterlinkbackend.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Component
public class JwtTokenProvider {

    private static final Logger log = LoggerFactory.getLogger(JwtTokenProvider.class);

    /** JWT claim name for auth token version (session invalidation). */
    public static final String CLAIM_TOKEN_VERSION = "tv";

    @Value("${app.jwtSecret}")
    private String jwtSecret;

    @Value("${app.jwtExpirationInMs}")
    private long jwtExp;

    private SecretKey cachedKey;

    @PostConstruct
    private void initSigningKey() {
        cachedKey = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
    }

    private SecretKey getSigningKey() {
        return cachedKey;
    }

    /**
     * Parses the JWT and returns its Claims, or null if the token is invalid/expired.
     * Callers can extract subject and custom claims from the returned object in one parse.
     */
    public Claims parseClaimsOrNull(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(getSigningKey())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (ExpiredJwtException e) {
            return null;
        } catch (Exception e) {
            log.warn("JWT validation failed [{}]: {}", e.getClass().getSimpleName(), e.getMessage());
            return null;
        }
    }

    /**
     * Generates an access token for a username with the given token version.
     * Used by the refresh endpoint where we have already validated the refresh token
     * and identified the user without re-authenticating their password.
     */
    public String generateTokenForUsername(String username, int authTokenVersion) {
        Date now = new Date();
        Date expiration = new Date(now.getTime() + jwtExp);
        return Jwts.builder()
                .subject(username)
                .claim(CLAIM_TOKEN_VERSION, authTokenVersion)
                .issuedAt(now)
                .expiration(expiration)
                .signWith(getSigningKey(), Jwts.SIG.HS512)
                .compact();
    }

    public String generateToken(Authentication authentication) {
        String username = authentication.getName();
        int authTokenVersion = 0;
        if (authentication.getPrincipal() instanceof net.fosterlink.fosterlinkbackend.models.auth.LoggedInUser loggedIn) {
            authTokenVersion = loggedIn.getAuthTokenVersion();
        }

        Date now = new Date();
        Date expiration = new Date(now.getTime() + jwtExp);

        return Jwts.builder()
                .subject(username)
                .claim(CLAIM_TOKEN_VERSION, authTokenVersion)
                .issuedAt(now)
                .expiration(expiration)
                .signWith(getSigningKey(), Jwts.SIG.HS512)
                .compact();
    }

    public String getUsernameFromJWT(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
        return claims.getSubject();
    }

    /**
     * Returns the token version from the JWT claim, or null if the tv claim is absent or the token
     * is invalid. A null return value must be treated as an authentication failure by the caller.
     */
    public Integer getTokenVersionFromJWT(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(getSigningKey())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            return claims.get(CLAIM_TOKEN_VERSION, Integer.class);
        } catch (Exception e) {
            return null;
        }
    }

    public boolean validateToken(String token) {
        try {
            Jwts.parser().verifyWith(getSigningKey()).build().parseSignedClaims(token);
            return true;
        } catch (Exception ignored) {
        }
        return false;
    }

}
